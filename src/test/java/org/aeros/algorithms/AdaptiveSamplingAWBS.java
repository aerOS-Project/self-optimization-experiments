package org.aeros.algorithms;

import static org.aeros.utils.ScenarioMapper.mapToMonitoredIE;
import static org.aeros.domain.TestInfrastructureElementStateREST.getAmountOfUsedCores;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.aeros.algorithms.parameters.AWBSParameters;
import org.aeros.domain.TestInfrastructureElementStateREST;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Class implements the window-based adaptive sampling approach.
 * It used in comparing the efficiency of AdaM adaptive sampling implemented in Sampling Model.
 *
 * @see <a href="https://ieeexplore.ieee.org/document/9249151/"> Adaptive Window Based Sampling
 */
public class AdaptiveSamplingAWBS {

	private final Map<String, List<Double>> windowObservations;
	private final Map<String, Double> lastAverage;
	private final Map<String, Integer> windowSize;

	private final double threshold;
	private final int maxWindowSize;

	/**
	 * Default constructor.
	 *
	 * @param parameters parameters of the algorithm
	 */
	public AdaptiveSamplingAWBS(final AWBSParameters parameters) {
		this.threshold = parameters.getThreshold();
		this.maxWindowSize = parameters.getMaxWindowSize();

		this.windowObservations = new HashMap<>(Map.of(
				"CPU", new ArrayList<>(),
				"RAM", new ArrayList<>(),
				"DISK", new ArrayList<>())
		);
		this.windowSize = new HashMap<>(Map.of(
				"CPU", parameters.getInitialWindowSize(),
				"RAM", parameters.getInitialWindowSize(),
				"DISK", parameters.getInitialWindowSize())
		);
		this.lastAverage = new HashMap<>(Map.of("CPU", -1.0, "RAM", -1.0, "DISK", -1.0));
	}

	/**
	 * Method simulates adaptive sampling using AWBS algorithm.
	 *
	 * @param ieRESTData input data on which sampling is to be simulated
	 * @return Pair that contains the count of monitored sample and the monitored observations
	 */
	public Pair<Integer, List<TestInfrastructureElementStateREST>> simulateSampling(
			final List<TestInfrastructureElementStateREST> ieRESTData) {
		final List<TestInfrastructureElementStateREST> monitoredSamples = new ArrayList<>();
		final AtomicInteger monitoredSamplesCount = new AtomicInteger(0);

		monitoredSamples.add(ieRESTData.getFirst());

		for (TestInfrastructureElementStateREST ieREST : ieRESTData) {
			final Map<String, Double> monitoredUtilization = estimateObservation(ieREST);

			if (monitoredUtilization.isEmpty()) {
				monitoredSamples.add(monitoredSamples.getLast());
				continue;
			}
			monitoredSamples.add(mapToMonitoredIE(ieREST, monitoredUtilization));
			monitoredSamplesCount.incrementAndGet();
		}
		monitoredSamples.removeFirst();
		return Pair.of(monitoredSamples.size(), monitoredSamples);
	}

	/**
	 * Method computes next optimal window size based on the current observation.
	 *
	 * @param nextObs current observation
	 * @return map with monitored values
	 */
	public Map<String, Double> estimateObservation(final TestInfrastructureElementStateREST nextObs) {
		final Pair<Integer, Double> observationCPU = applyAWBSAlgorithm("CPU", getAmountOfUsedCores(nextObs));
		final Pair<Integer, Double> observationRAM = applyAWBSAlgorithm("RAM", nextObs.getCurrentRamUsage());
		final Pair<Integer, Double> observationDisk = applyAWBSAlgorithm("DISK", nextObs.getCurrentDiskUsage());

		if (observationCPU.getValue() != -1 && observationRAM.getValue() != -1 && observationDisk.getValue() != -1) {
			final int commonWindowSize = Stream.of(observationDisk.getKey(), observationRAM.getKey(),
							observationCPU.getKey())
					.mapToInt(Integer::intValue)
					.min()
					.orElse(1);

			windowSize.replace("CPU", commonWindowSize);
			windowSize.replace("RAM", commonWindowSize);
			windowSize.replace("DISK", commonWindowSize);

			return Map.of(
					"CPU", observationCPU.getValue(),
					"RAM", observationRAM.getValue(),
					"DISK", observationDisk.getValue()
			);
		}
		return Collections.emptyMap();
	}

	private Pair<Integer, Double> applyAWBSAlgorithm(final String type, final double nextObs) {
		windowObservations.get(type).add(nextObs);

		if (windowObservations.get(type).size() == windowSize.get(type)) {
			final double sum = windowObservations.get(type).stream().mapToDouble(Double::doubleValue).sum();
			final double average = sum / windowSize.get(type);
			final double prevAverage = lastAverage.get(type);

			lastAverage.replace(type, average);
			windowObservations.get(type).clear();

			if (prevAverage != -1) {
				final double avgDifference =
						prevAverage == 0 ? 0 : Math.abs((prevAverage - average) / prevAverage) * 100;
				final int newWindowSize = avgDifference <= threshold
						? Math.min(windowSize.get(type) + 1, maxWindowSize)
						: Math.max(windowSize.get(type) - 1, 1);
				windowSize.replace(type, newWindowSize);

				return Pair.of(windowSize.get(type), average);
			}
		}
		return Pair.of(windowSize.get(type), -1.0);
	}
}
