package org.aeros.algorithms;

import static org.aeros.domain.TestInfrastructureElementStateREST.getAmountOfUsedCores;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.aeros.domain.TestInfrastructureElementStateREST;
import org.aeros.algorithms.parameters.UDASAParameters;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.stat.descriptive.rank.Median;

/**
 * Class implements the user-driven window-based adaptive sampling approach.
 * It used in comparing the efficiency of AdaM adaptive sampling implemented in Sampling Model.
 *
 * @see <a href="https://ieeexplore.ieee.org/document/9146545"> User-Driven Adaptive Sampling
 */
public class AdaptiveSamplingUDASA {

	private final Map<String, List<Double>> windowObservations;
	private final Map<String, Integer> samplingPeriod;

	private final int windowSize;
	private final int savingSize;
	private final int baseSamplingPeriod;

	/**
	 * Default constructor.
	 *
	 * @param udasaParameters parameters of the algorithm
	 */
	public AdaptiveSamplingUDASA(final UDASAParameters udasaParameters) {
		this.windowSize = udasaParameters.getWindowSize();
		this.savingSize = udasaParameters.getSavingSize();
		this.baseSamplingPeriod = udasaParameters.getBaseSamplingPeriod();

		this.windowObservations = new HashMap<>(Map.of(
				"CPU", new ArrayList<>(),
				"RAM", new ArrayList<>(),
				"DISK", new ArrayList<>())
		);
		this.samplingPeriod = new HashMap<>(Map.of(
				"CPU", baseSamplingPeriod,
				"RAM", baseSamplingPeriod,
				"DISK", baseSamplingPeriod)
		);
	}

	/**
	 * Method simulates adaptive sampling using UDASA algorithm.
	 *
	 * @param ieRESTData input data on which sampling is to be simulated
	 * @return Pair that contains the count of monitored sample and the monitored observations
	 */
	public Pair<Integer, List<TestInfrastructureElementStateREST>> simulateSampling(
			final List<TestInfrastructureElementStateREST> ieRESTData) {
		final List<TestInfrastructureElementStateREST> monitoredSamples = new ArrayList<>();
		final AtomicInteger monitoredSamplesCount = new AtomicInteger(0);
		int nextExpectedIdx = 0;

		for (int i = 0; i < ieRESTData.size(); i++) {
			if (nextExpectedIdx != i) {
				monitoredSamples.add(monitoredSamples.getLast());
				continue;
			}

			final long period = estimateSamplingPeriod(ieRESTData.get(i));
			monitoredSamples.add(ieRESTData.get(i));
			nextExpectedIdx = i + (int) (period / 1000);
			monitoredSamplesCount.incrementAndGet();
		}
		return Pair.of(monitoredSamplesCount.get(), monitoredSamples);
	}

	private long estimateSamplingPeriod(final TestInfrastructureElementStateREST nextObs) {
		final long samplingPeriodCPU = applyUDASAAlgorithm("CPU", getAmountOfUsedCores(nextObs));
		final long samplingPeriodRAM = applyUDASAAlgorithm("RAM", nextObs.getCurrentRamUsage());
		final long samplingPeriodDisk = applyUDASAAlgorithm("DISK", nextObs.getCurrentDiskUsage());

		return Stream.of(samplingPeriodCPU, samplingPeriodDisk, samplingPeriodRAM)
				.mapToLong(Long::longValue)
				.min()
				.orElse(baseSamplingPeriod);
	}

	private long applyUDASAAlgorithm(final String type, final double nextObs) {
		windowObservations.get(type).add(nextObs);

		if (windowObservations.get(type).size() == windowSize) {
			final double madSum = IntStream.range(1, windowObservations.get(type).size())
					.mapToDouble(i -> calculateMAD(windowObservations.get(type).subList(0, i)))
					.sum();
			final double meadMAD = madSum / windowSize;
			final double currentMAD = calculateMAD(windowObservations.get(type));
			final double savingRatio = (((double) savingSize + 1) / 2);
			final double changeDeg = currentMAD - savingRatio * meadMAD;

			samplingPeriod.replace(type, (int) Math.round(
					(savingSize + (1 - savingSize) / (1 + Math.exp(-savingSize * changeDeg))) * baseSamplingPeriod));
			windowObservations.get(type).clear();
		}
		return samplingPeriod.get(type);
	}

	private double calculateMAD(final List<Double> sample) {
		final double median = new Median().evaluate(sample.stream().mapToDouble(Double::doubleValue).toArray());
		final double[] distances = sample.stream()
				.map(obs -> Math.abs(obs - median))
				.mapToDouble(Double::doubleValue)
				.toArray();
		return new Median().evaluate(distances);
	}
}
