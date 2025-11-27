package org.aeros;

import static java.lang.String.join;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;
import static org.aeros.domain.AlgorithmType.ANOMALY;
import static org.aeros.domain.ScenarioDescription.AEROS_SCENARIO;
import static org.aeros.domain.ScenarioDescription.NUMENTA_JUMPS_SCENARIO;
import static org.aeros.domain.ScenarioDescription.NUMENTA_SPIKES_SCENARIO;
import static org.aeros.domain.TestInfrastructureElementStateREST.getAmountOfUsedCores;
import static org.aeros.utils.ResultVisualization.plotAndSaveDataSampleChartWithAnomalies;
import static org.aeros.utils.ScenarioMapper.mapToIEREST;
import static org.aeros.utils.ScenarioReader.getScenarioName;
import static org.aeros.utils.ScenarioReader.readAnomalyDetectionResult;
import static org.aeros.utils.ScenarioReader.readScenarioData;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;
import java.util.stream.IntStream;

import org.aeros.algorithms.parameters.NABAnomalyParameters;
import org.aeros.base.DensityBasedAnomalyDetection;
import org.aeros.base.config.DensityBasedAnomalyConfiguration;
import org.aeros.domain.AlgorithmConfigDescription;
import org.aeros.domain.AnomalyDetectionResult;
import org.aeros.domain.ScenarioDescription;
import org.aeros.domain.TestInfrastructureElementState;
import org.aeros.domain.TestInfrastructureElementStateREST;
import org.aeros.metrics.MetricLogger;
import org.aeros.utils.ScenarioReader;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.slf4j.Logger;

public class AnomalyDetectionScenarioTest {

	private static final Logger logger = getLogger(AnomalyDetectionScenarioTest.class);
	private static final List<String> scenarioNames = List.of(AEROS_SCENARIO, NUMENTA_SPIKES_SCENARIO, NUMENTA_JUMPS_SCENARIO);

	@TestFactory
	Collection<DynamicTest> prepareTestScenarios() {
		return scenarioNames.stream()
				.map(ScenarioReader::getScenarioConfigName)
				.map(ScenarioReader::readScenario)
				.map(scenario -> dynamicTest(getScenarioName(scenario), () -> executeTestScenario(scenario)))
				.toList();
	}

	private void executeTestScenario(final ScenarioDescription scenarioDescription) {
		final DensityBasedAnomalyConfiguration configuration = scenarioDescription.getBaseAlgorithmsConfig().stream()
				.filter(config -> config.getType().equals(ANOMALY))
				.findFirst()
				.map(AlgorithmConfigDescription::getConfig)
				.map(DensityBasedAnomalyConfiguration.class::cast)
				.orElseThrow();
		final DensityBasedAnomalyDetection densityBasedAnomalyDetection = new DensityBasedAnomalyDetection(
				configuration);

		final List<TestInfrastructureElementState> ieData = readScenarioData(scenarioDescription.getIe().getData());
		final List<TestInfrastructureElementStateREST> ieRESTData = mapToIEREST(scenarioDescription, ieData);

		final Map<Integer, List<String>> anomalies = IntStream.range(0, ieRESTData.size()).boxed()
				.map(idx -> Pair.of(idx + 1,
						densityBasedAnomalyDetection.detectAnomalies(ieData.get(idx), scenarioDescription.getIe())))
				.filter(idxPair -> !idxPair.getValue().isEmpty())
				.collect(toMap(Pair::getKey, Pair::getValue));

		logger.info("Anomalies detected by Density-Based: {}", anomalies);

		plotDiskAnomalies(ieRESTData, anomalies, "Density-Based", join("-", scenarioDescription.getName(), "disk"));
		plotRAMAnomalies(ieRESTData, anomalies, "Density-Based", join("-", scenarioDescription.getName(), "ram"));
		plotCPUAnomalies(ieRESTData, anomalies, "Density-Based", join("-", scenarioDescription.getName(), "cpu"));

		new MetricLogger(scenarioDescription, anomalies).printMetrics(scenarioDescription.getEvaluationMetrics());
		runComparisonAlgorithms(ieRESTData, scenarioDescription);
	}

	private void runComparisonAlgorithms(final List<TestInfrastructureElementStateREST> ieRESTData,
			final ScenarioDescription scenarioDescription) {
		scenarioDescription.getAlgorithmsForComparison().forEach(algorithm -> {
			final NABAnomalyParameters parameters = (NABAnomalyParameters) algorithm.getParams();
			final AnomalyDetectionResult results = readAnomalyDetectionResult(
					parameters.getDetectionResultsFileName(),
					parameters.getDetectionCSVFileName(),
					parameters.getScoreFileName(),
					parameters.getThreshold()
			);
			final String methodName = algorithm.getType().name();
			final Map<Integer, List<String>> anomalies = results.getDetectedAnomalies();
			final String testTitle = join("-", methodName.toLowerCase(), scenarioDescription.getName());

			logger.info("Anomalies detected by {}: {}", methodName, anomalies);

			switch (parameters.getMetricType()) {
				case "CPU" -> plotCPUAnomalies(ieRESTData, anomalies, methodName, join("-", testTitle, "cpu"));
				case "RAM" -> plotRAMAnomalies(ieRESTData, anomalies, methodName, join("-", testTitle, "ram"));
				case "DISK" -> plotDiskAnomalies(ieRESTData, anomalies, methodName, join("-", testTitle, "disk"));
			}

			new MetricLogger(scenarioDescription, results.getScore())
					.printMetricsForNAB(scenarioDescription.getEvaluationMetrics());
		});
	}

	private void plotRAMAnomalies(final List<TestInfrastructureElementStateREST> ieRESTData,
			final Map<Integer, List<String>> detectedAnomalies, final String methodName, final String testTitle) {
		plotAndSaveDataSampleChartWithAnomalies(ieRESTData,
				detectedAnomalies,
				TestInfrastructureElementStateREST::getCurrentRamUsage,
				TestInfrastructureElementStateREST::getRamCapacity,
				testTitle,
				methodName,
				"RAM",
				"MB");
	}

	private void plotCPUAnomalies(final List<TestInfrastructureElementStateREST> ieRESTData,
			final Map<Integer, List<String>> detectedAnomalies, final String methodName, final String testTitle) {
		final ToDoubleFunction<TestInfrastructureElementStateREST> getCPUUtilization = ie ->
				testTitle.contains("aeros") ? requireNonNull(getAmountOfUsedCores(ie)) : ie.getCurrentCpuUsage();

		plotAndSaveDataSampleChartWithAnomalies(ieRESTData,
				detectedAnomalies,
				getCPUUtilization,
				TestInfrastructureElementStateREST::getCpuCores,
				testTitle,
				methodName,
				"CPU",
				"cores");
	}

	private void plotDiskAnomalies(final List<TestInfrastructureElementStateREST> ieRESTData,
			final Map<Integer, List<String>> detectedAnomalies, final String methodName, final String testTitle) {
		plotAndSaveDataSampleChartWithAnomalies(ieRESTData,
				detectedAnomalies,
				TestInfrastructureElementStateREST::getCurrentDiskUsage,
				TestInfrastructureElementStateREST::getDiskCapacity,
				testTitle,
				methodName,
				"disk",
				"MB");
	}

}
