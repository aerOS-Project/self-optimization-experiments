package org.aeros.metrics;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;
import java.util.Map;

import org.aeros.domain.TestInfrastructureElementStateREST;
import org.aeros.domain.MetricParameters;
import org.aeros.domain.MetricType;
import org.aeros.domain.ScenarioDescription;
import org.slf4j.Logger;

/**
 * Class contains methods that logs quality metrics
 */
public class MetricLogger {

	private static final Logger logger = getLogger(MetricLogger.class);

	private final boolean isForAnomaly;
	private ScenarioDescription description;
	private AnomalyScoreEvaluator scoreEvaluator;
	private JPMEvaluator jpmEvaluator;
	private MAPEEvaluator mapeEvaluator;
	private RatioEvaluator ratioEvaluator;

	/**
	 * Constructor used for printing anomaly detection metrics.
	 *
	 * @param scenarioDescription description of executed tests
	 * @param detectedAnomalies   a map consisting of anomaly indexes and types of anomalies
	 */
	public MetricLogger(final ScenarioDescription scenarioDescription,
			final Map<Integer, List<String>> detectedAnomalies) {
		this.description = scenarioDescription;
		this.scoreEvaluator = new AnomalyScoreEvaluator(detectedAnomalies);
		this.isForAnomaly = true;
	}

	/**
	 * Constructor used for printing anomaly detection for pre-computed anomalies.
	 *
	 * @param scenarioDescription description of executed tests
	 * @param rawAnomalyScore     raw anomaly detection score
	 */
	public MetricLogger(final ScenarioDescription scenarioDescription, final Double rawAnomalyScore) {
		this.description = scenarioDescription;
		this.scoreEvaluator = new AnomalyScoreEvaluator(rawAnomalyScore);
		this.isForAnomaly = true;
	}

	/**
	 * Constructor used for printing adaptive sampling metrics.
	 *
	 * @param monitoredSample      list of monitored observations
	 * @param realSample           list of original observations
	 * @param monitoredSampleCount size of monitored sample
	 */
	public MetricLogger(final List<TestInfrastructureElementStateREST> monitoredSample,
			final List<TestInfrastructureElementStateREST> realSample, final int monitoredSampleCount) {
		this.mapeEvaluator = new MAPEEvaluator(monitoredSample, realSample);
		this.ratioEvaluator = new RatioEvaluator(monitoredSampleCount, monitoredSample);
		this.jpmEvaluator = new JPMEvaluator(mapeEvaluator, ratioEvaluator);
		this.isForAnomaly = false;
	}

	/**
	 * Method computes and logs information of relevant NAB pre-computed results
	 *
	 * @param metricsMap map of all relevant metrics
	 */
	public void printMetricsForNAB(final Map<MetricType, MetricParameters> metricsMap) {
		metricsMap.forEach((metric, _) -> {
			switch (metric) {
				case ANOMALY_SCORE_NAB -> logger.info(
						scoreEvaluator.formatLog(scoreEvaluator.computeAnomalyScoreForNAB(description), "NAB"));
			}
		});
	}

	/**
	 * Method computes and logs information of relevant quality metrics.
	 *
	 * @param metricsMap map of all relevant metrics
	 */
	public void printMetrics(final Map<MetricType, MetricParameters> metricsMap) {
		if (isForAnomaly) {
			metricsMap.forEach((metric, _) -> {
				switch (metric) {
					case ANOMALY_SCORE_CPU -> logger.info(
							scoreEvaluator.formatLog(scoreEvaluator.computeAnomalyScoreForCPU(description), "CPU"));
					case ANOMALY_SCORE_RAM ->
							scoreEvaluator.formatLog(scoreEvaluator.computeAnomalyScoreForRAM(description), "RAM");
					case ANOMALY_SCORE_DISK ->
							scoreEvaluator.formatLog(scoreEvaluator.computeAnomalyScoreForDisk(description), "DISK");
				}
			});
		} else {
			metricsMap.forEach((metric, _) -> {
				switch (metric) {
					case MAPE_CPU -> logger.info(mapeEvaluator.formatLog(mapeEvaluator.computeMAPEForCPU(), "CPU"));
					case MAPE_RAM -> logger.info(mapeEvaluator.formatLog(mapeEvaluator.computeMAPEForRAM(), "RAM"));
					case MAPE_DISK -> logger.info(mapeEvaluator.formatLog(mapeEvaluator.computeMAPEForDisk(), "DISK"));
					case MAPE_AVG -> logger.info(mapeEvaluator.formatLog(mapeEvaluator.computeAvgMAPE(), "AVG"));
					case JPM_CPU -> logger.info(jpmEvaluator.formatLog(jpmEvaluator.computeJPMForCPU(), "CPU"));
					case JPM_RAM -> logger.info(jpmEvaluator.formatLog(jpmEvaluator.computeJPMForRAM(), "RAM"));
					case JPM_DISK -> logger.info(jpmEvaluator.formatLog(jpmEvaluator.computeJPMForDisk(), "DISK"));
					case JPM_AVG -> logger.info(jpmEvaluator.formatLog(jpmEvaluator.computeAvgJPM(), "AVG"));
					case SAMPLE_RATIO ->
							logger.info(ratioEvaluator.formatLog(ratioEvaluator.computeDataSamplesVolumeRatio()));
				}
			});
		}

	}
}
