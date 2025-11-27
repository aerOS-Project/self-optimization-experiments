package org.aeros;

import static java.lang.String.join;
import static java.util.Objects.requireNonNull;
import static org.aeros.domain.AlgorithmType.SAMPLING;
import static org.aeros.domain.ScenarioDescription.AEROS_SCENARIO;
import static org.aeros.domain.ScenarioDescription.RAINMON_SCENARIO;
import static org.aeros.domain.TestInfrastructureElementStateREST.getAmountOfUsedCores;
import static org.aeros.utils.ResultVisualization.plotAndSaveSamplingCharts;
import static org.aeros.utils.ScenarioMapper.mapToIEREST;
import static org.aeros.utils.ScenarioReader.getScenarioName;
import static org.aeros.utils.ScenarioReader.readScenarioData;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.ToDoubleFunction;

import org.aeros.algorithms.AdaptiveSamplingAWBS;
import org.aeros.algorithms.AdaptiveSamplingUDASA;
import org.aeros.algorithms.parameters.AWBSParameters;
import org.aeros.algorithms.parameters.UDASAParameters;
import org.aeros.base.PEWMASampling;
import org.aeros.base.config.PEWMASamplingConfiguration;
import org.aeros.domain.AlgorithmConfigDescription;
import org.aeros.domain.ScenarioDescription;
import org.aeros.domain.TestInfrastructureElementState;
import org.aeros.domain.TestInfrastructureElementStateREST;
import org.aeros.metrics.MetricLogger;
import org.aeros.utils.ScenarioReader;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.slf4j.Logger;

public class ResourceSamplingScenarioTest {

	private static final Logger logger = getLogger(ResourceSamplingScenarioTest.class);
	private static final List<String> scenarioNames = List.of(AEROS_SCENARIO, RAINMON_SCENARIO);

	@TestFactory
	Collection<DynamicTest> prepareTestScenarios() {
		return scenarioNames.stream()
				.map(ScenarioReader::getScenarioConfigName)
				.map(ScenarioReader::readScenario)
				.map(scenario -> dynamicTest(getScenarioName(scenario), () -> executeTestScenario(scenario)))
				.toList();
	}

	private void executeTestScenario(final ScenarioDescription scenarioDescription) {
		final PEWMASamplingConfiguration configuration = scenarioDescription.getBaseAlgorithmsConfig().stream()
				.filter(config -> config.getType().equals(SAMPLING))
				.findFirst()
				.map(AlgorithmConfigDescription::getConfig)
				.map(PEWMASamplingConfiguration.class::cast)
				.orElseThrow();
		final PEWMASampling pewmaSamplingAlgorithm = new PEWMASampling(configuration);

		final List<TestInfrastructureElementState> ieData = readScenarioData(scenarioDescription.getIe().getData());
		final List<TestInfrastructureElementStateREST> ieRESTData = mapToIEREST(scenarioDescription, ieData);
		final List<TestInfrastructureElementStateREST> monitoredSamples = new ArrayList<>();
		final AtomicInteger monitoredSamplesCount = new AtomicInteger(0);

		int nextExpectedIdx = 0;

		for (int i = 0; i < ieRESTData.size(); i++) {
			if (nextExpectedIdx != i) {
				monitoredSamples.add(monitoredSamples.getLast());
				continue;
			}

			final long samplingPeriod = pewmaSamplingAlgorithm.estimateSamplingPeriod(ieData.get(i),
					scenarioDescription.getIe());
			monitoredSamples.add(ieRESTData.get(i));
			nextExpectedIdx = i + (int) (samplingPeriod / 1000);
			monitoredSamplesCount.incrementAndGet();
		}

		displaySamplingResults(scenarioDescription, monitoredSamples, ieRESTData, monitoredSamplesCount.get(), "AdaM");
		runComparisonAlgorithms(scenarioDescription, ieRESTData);
	}

	private void runComparisonAlgorithms(final ScenarioDescription scenarioDescription,
			final List<TestInfrastructureElementStateREST> ieRESTData) {
		scenarioDescription.getAlgorithmsForComparison().forEach(algorithm -> {
			final Pair<Integer, List<TestInfrastructureElementStateREST>> result = switch (algorithm.getType()) {
				case UDASA -> {
					final UDASAParameters params = (UDASAParameters) algorithm.getParams();
					final AdaptiveSamplingUDASA udasa = new AdaptiveSamplingUDASA(params);
					yield udasa.simulateSampling(ieRESTData);
				}
				case AWBS -> {
					final AWBSParameters params = (AWBSParameters) algorithm.getParams();
					final AdaptiveSamplingAWBS awbs = new AdaptiveSamplingAWBS(params);
					yield awbs.simulateSampling(ieRESTData);
				}
				default -> throw new IllegalStateException("Unexpected value: " + algorithm.getType());
			};
			displaySamplingResults(scenarioDescription, result.getValue(), ieRESTData, result.getKey(),
					algorithm.getType().name());
		});
	}

	private void displaySamplingResults(final ScenarioDescription scenarioDescription,
			final List<TestInfrastructureElementStateREST> monitoredSamples,
			final List<TestInfrastructureElementStateREST> ieRESTData,
			final Integer monitoredSamplesCount,
			final String methodName) {
		final String testTitle = join("-", methodName.toLowerCase(), scenarioDescription.getName());
		logger.info("Results of {} sampling:", methodName);

		plotDiskMonitoring(monitoredSamples, ieRESTData, methodName, join("-", testTitle, "disk"));
		plotRAMMonitoring(monitoredSamples, ieRESTData, methodName, join("-", testTitle, "ram"));
		plotCPUMonitoring(ieRESTData, monitoredSamples, methodName, join("-", testTitle, "cpu"));

		new MetricLogger(monitoredSamples, ieRESTData, monitoredSamplesCount).printMetrics(
				scenarioDescription.getEvaluationMetrics());
	}

	private void plotDiskMonitoring(final List<TestInfrastructureElementStateREST> monitoredSamples,
			final List<TestInfrastructureElementStateREST> ieRESTData, final String methodName,
			final String testTitle) {
		plotAndSaveSamplingCharts(ieRESTData,
				monitoredSamples,
				TestInfrastructureElementStateREST::getCurrentDiskUsage,
				TestInfrastructureElementStateREST::getDiskCapacity,
				testTitle,
				methodName,
				"disk",
				"MB");
	}

	private void plotRAMMonitoring(final List<TestInfrastructureElementStateREST> monitoredSamples,
			final List<TestInfrastructureElementStateREST> ieRESTData, final String methodName,
			final String testTitle) {
		plotAndSaveSamplingCharts(ieRESTData,
				monitoredSamples,
				TestInfrastructureElementStateREST::getCurrentRamUsage,
				TestInfrastructureElementStateREST::getRamCapacity,
				testTitle,
				methodName,
				"RAM",
				"MB");
	}

	private void plotCPUMonitoring(final List<TestInfrastructureElementStateREST> monitoredSamples,
			final List<TestInfrastructureElementStateREST> ieRESTData, final String methodName,
			final String testTitle) {
		final ToDoubleFunction<TestInfrastructureElementStateREST> getCPUUtilization = ie ->
				testTitle.contains("aeros") ? requireNonNull(getAmountOfUsedCores(ie)) : ie.getCurrentCpuUsage();

		plotAndSaveSamplingCharts(ieRESTData,
				monitoredSamples,
				getCPUUtilization,
				TestInfrastructureElementStateREST::getCpuCores,
				testTitle,
				methodName,
				"CPU",
				"cores");
	}
}
