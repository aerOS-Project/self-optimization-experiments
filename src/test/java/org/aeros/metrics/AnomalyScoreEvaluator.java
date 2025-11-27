package org.aeros.metrics;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.aeros.domain.MetricType.ANOMALY_SCORE_CPU;
import static org.aeros.domain.MetricType.ANOMALY_SCORE_DISK;
import static org.aeros.domain.MetricType.ANOMALY_SCORE_NAB;
import static org.aeros.domain.MetricType.ANOMALY_SCORE_RAM;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.aeros.domain.AnomalyScoringParameters;
import org.aeros.domain.AnomalyWindow;
import org.aeros.domain.ScenarioDescription;

/**
 * Class contains methods that compute the Anomaly Score (S)
 */
public class AnomalyScoreEvaluator {

	private final Map<Integer, List<String>> detectedAnomalies;
	private final Double anomalyRawScore;

	/**
	 * Default constructor.
	 *
	 * @param detectedAnomalies a map consisting of anomaly indexes and types of anomalies
	 */
	public AnomalyScoreEvaluator(final Map<Integer, List<String>> detectedAnomalies) {
		this.detectedAnomalies = detectedAnomalies;
		this.anomalyRawScore = null;
	}

	/**
	 * Default constructor.
	 *
	 * @param anomalyRawScore raw anomaly score obtained by running NAB
	 */
	public AnomalyScoreEvaluator(final Double anomalyRawScore) {
		this.anomalyRawScore = anomalyRawScore;
		this.detectedAnomalies = null;
	}

	/**
	 * @param scenarioDescription description of test scenario
	 * @return score for all anomalies
	 */
	public double computeAnomalyScoreForNAB(final ScenarioDescription scenarioDescription) {
		final AnomalyScoringParameters parameters =
				(AnomalyScoringParameters) scenarioDescription.getEvaluationMetrics().get(ANOMALY_SCORE_NAB);
		return computeNormalizedScore(parameters.getAnomalyWindows(), parameters.getTruePositiveWeight(),
				parameters.getBaseline(), anomalyRawScore);
	}

	/**
	 * @param scenarioDescription description of test scenario
	 * @return score for all detected CPU anomalies
	 */
	public double computeAnomalyScoreForCPU(final ScenarioDescription scenarioDescription) {
		final AnomalyScoringParameters parameters =
				(AnomalyScoringParameters) scenarioDescription.getEvaluationMetrics().get(ANOMALY_SCORE_CPU);
		return computeAnomalyScore(parameters, List.of("CPU_USAGE_INCREASE", "CPU_USAGE_DECREASE"));
	}

	/**
	 * @param scenarioDescription description of test scenario
	 * @return score for all detected RAM anomalies
	 */
	public double computeAnomalyScoreForRAM(final ScenarioDescription scenarioDescription) {
		final AnomalyScoringParameters parameters =
				(AnomalyScoringParameters) scenarioDescription.getEvaluationMetrics().get(ANOMALY_SCORE_RAM);
		return computeAnomalyScore(parameters, List.of("RAM_USAGE_INCREASE", "RAM_USAGE_DECREASE"));
	}

	/**
	 * @param scenarioDescription description of test scenario
	 * @return score for all detected disk anomalies
	 */
	public double computeAnomalyScoreForDisk(final ScenarioDescription scenarioDescription) {
		final AnomalyScoringParameters parameters =
				(AnomalyScoringParameters) scenarioDescription.getEvaluationMetrics().get(ANOMALY_SCORE_DISK);
		return computeAnomalyScore(parameters, List.of("DISK_USAGE_INCREASE", "DISK_USAGE_DECREASE"));
	}

	/**
	 * Formats information about computed anomaly score that is to be displayed.
	 *
	 * @param type  type of computed score
	 * @param value anomaly score value;
	 * @return formatted message
	 */
	public String formatLog(final double value, final String type) {
		return format("[S] Anomaly Score (%s): %f%%", type, value);
	}

	private double computeAnomalyScore(final AnomalyScoringParameters parameters, final List<String> anomalyTypes) {
		final List<Integer> anomalyIndexes = detectedAnomalies.entrySet().stream()
				.filter(entry -> entry.getValue().stream().anyMatch(anomalyTypes::contains))
				.map(Map.Entry::getKey)
				.sorted()
				.toList();

		final List<AnomalyWindow> anomalyWindows = parameters.getAnomalyWindows();
		final List<AnomalyWindow> detectedWindows = new ArrayList<>();

		final AtomicInteger windowIdx = new AtomicInteger(0);
		final AtomicReference<AnomalyWindow> currWindow = new AtomicReference<>(anomalyWindows.getFirst());
		final AtomicInteger windowSize = new AtomicInteger(currWindow.get().getWindowSize());

		final double totalDetectedScore = anomalyIndexes.stream()
				.map(anomaly -> computeScoreForAnomaly(detectedWindows, anomalyWindows, currWindow, windowSize,
						windowIdx, anomaly, parameters))
				.filter(Optional::isPresent)
				.map(Optional::get)
				.mapToDouble(Double.class::cast)
				.sum();

		final double noDetectionsScore = (anomalyWindows.size() - detectedWindows.size()) *
										 parameters.getFalseNegativeWeight();
		final double anomalyScore = (totalDetectedScore - noDetectionsScore);

		return computeNormalizedScore(anomalyWindows, parameters.getTruePositiveWeight(), parameters.getBaseline(),
				anomalyScore);
	}

	private Optional<Double> computeScoreForAnomaly(final List<AnomalyWindow> detectedWindows,
			final List<AnomalyWindow> expectedAnomalyWindows,
			final AtomicReference<AnomalyWindow> currWindow,
			final AtomicInteger windowSize,
			final AtomicInteger windowIdx,
			final int anomalyIdx,
			final AnomalyScoringParameters parameters) {
		if (currWindow.get().isAfterWindow(anomalyIdx)) {
			return handleAnomalyAfterWindow(anomalyIdx, currWindow, windowSize, windowIdx, expectedAnomalyWindows,
					parameters);
		}

		if (detectedWindows.contains(currWindow.get()))
			return empty(); // No result if anomaly was already detected for window

		if (currWindow.get().isWithinWindow(anomalyIdx)) {
			return handleAnomalyWithinWindow(anomalyIdx, currWindow.get(), windowSize.get(), detectedWindows,
					parameters);
		}
		return of(-1 * parameters.getFalsePositiveWeight());
	}

	private Optional<Double> handleAnomalyWithinWindow(final int anomaly,
			final AnomalyWindow currentWindow,
			final int windowSize,
			final List<AnomalyWindow> windowsWithDetectedAnomalies,
			final AnomalyScoringParameters parameters) {
		final int positionInWindow = anomaly - currentWindow.getStartIdx();
		final double relativePosition = ((double) positionInWindow) / windowSize - 1;

		windowsWithDetectedAnomalies.add(currentWindow);
		return of(computeSigmoid(relativePosition) * parameters.getTruePositiveWeight());
	}

	private Optional<Double> handleAnomalyAfterWindow(final int anomaly,
			final AtomicReference<AnomalyWindow> currentWindow,
			final AtomicInteger windowSize,
			final AtomicInteger windowIdx,
			final List<AnomalyWindow> anomalyWindows,
			final AnomalyScoringParameters parameters) {
		if (isAnomalyCloserToNextWindow(currentWindow.get(), anomaly, windowIdx.get(), anomalyWindows)) {
			currentWindow.set(anomalyWindows.get(windowIdx.incrementAndGet()));
			windowSize.set(currentWindow.get().getWindowSize());
			return empty();
		}

		final double relativePosition = (anomaly - (double) currentWindow.get().getEndIdx()) / windowSize.get();
		return of(computeSigmoid(relativePosition) * parameters.getFalsePositiveWeight());
	}

	private boolean isAnomalyCloserToNextWindow(final AnomalyWindow currentWindow, final int anomalyIdx,
			final int windowIdx, final List<AnomalyWindow> expectedAnomalyWindows) {
		final double distanceToCurrWindowEnd = currentWindow.getEndIdx() - anomalyIdx;
		final double distanceToNextWindowStart = windowIdx + 1 < expectedAnomalyWindows.size()
				? expectedAnomalyWindows.get(windowIdx + 1).getStartIdx() - anomalyIdx
				: -1;

		return distanceToCurrWindowEnd > distanceToNextWindowStart;
	}

	private double computeNormalizedScore(final List<AnomalyWindow> anomalyWindows,
			final Double truePositiveWeight, final Double baseline, final Double anomalyScore) {
		final double perfectScore = anomalyWindows.size() * computeSigmoid(-1) * truePositiveWeight;

		return 100 * ((anomalyScore - baseline) / (perfectScore - baseline));
	}

	private double computeSigmoid(final double relativePosition) {
		return 2 * (1 / (1 + Math.exp(5 * relativePosition))) - 1;
	}
}
