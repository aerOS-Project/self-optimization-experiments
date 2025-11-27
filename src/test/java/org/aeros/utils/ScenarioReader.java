package org.aeros.utils;

import static java.lang.Double.parseDouble;
import static java.lang.String.format;
import static java.lang.String.join;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

import org.aeros.domain.AnomalyDetectionResult;
import org.aeros.domain.ScenarioDescription;
import org.aeros.domain.TestInfrastructureElementState;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Class containing methods used to read scenario data.
 */
public class ScenarioReader {

	private static final String TEST_SCENARIO_PATH = "test-scenarios";
	private static final String TEST_SCENARIO_DATA_PATH = join("/", TEST_SCENARIO_PATH, "data");
	private static final String TEST_SCENARIO_BENCHMARK_PATH = join("/", TEST_SCENARIO_PATH, "benchmark");

	private static final int ROW_FILE_NAME = 2;
	private static final int ROW_SCORE = 4;
	private static final int ROW_ANOMALY_RESULT = 2;

	private static final ObjectMapper mapper = new ObjectMapper();

	/**
	 * Method reads JSON file with scenario data.
	 *
	 * @param scenarioDataFileName name of the file that is to be read
	 * @return parsed IE test data
	 */
	public static List<TestInfrastructureElementState> readScenarioData(final String scenarioDataFileName) {
		final String dataFileName = join("/", TEST_SCENARIO_DATA_PATH, scenarioDataFileName);
		final InputStream inputStream = ScenarioReader.class.getClassLoader().getResourceAsStream(dataFileName);

		try {
			return mapper.readValue(inputStream, new TypeReference<>() {
			});
		} catch (final IOException e) {
			throw new RuntimeException("Couldn't read scenario data from the file.", e);
		}
	}

	/**
	 * Method reads JSON file with scenario configuration.
	 *
	 * @param scenarioConfigName name of the file that is to be read
	 * @return parsed scenario configuration
	 */
	public static ScenarioDescription readScenario(final String scenarioConfigName) {
		final String fullConfigName = join("/", TEST_SCENARIO_PATH, scenarioConfigName);
		final InputStream inputStream = ScenarioReader.class.getClassLoader().getResourceAsStream(fullConfigName);

		try {
			return mapper.readValue(inputStream, ScenarioDescription.class);
		} catch (final IOException e) {
			throw new RuntimeException("Couldn't read scenario description from the file.", e);
		}
	}

	/**
	 * Method reads the results of anomaly detection.
	 *
	 * @param detectionResultsName name of the file with detected anomalies
	 * @param detectionFileName    name of the file with detected anomalies referred in CSV file.
	 * @param allResultsName       name of the file with overall anomaly detection results
	 * @param threshold            threshold above which an observation is considered as anomaly
	 * @return combined anomaly detection results
	 */
	public static AnomalyDetectionResult readAnomalyDetectionResult(final String detectionResultsName,
			final String detectionFileName, final String allResultsName, final Double threshold) {
		final String fullDetectionPath = join("/", TEST_SCENARIO_BENCHMARK_PATH, detectionResultsName);
		final String fullResultsPath = join("/", TEST_SCENARIO_BENCHMARK_PATH, allResultsName);

		return AnomalyDetectionResult.builder()
				.detectedAnomalies(readDetectedAnomalies(fullDetectionPath, threshold))
				.score(readAnomalyScore(fullResultsPath, detectionFileName))
				.build();
	}

	/**
	 * Method retrieves complete scenario configuration file name.
	 *
	 * @param scenarioName name of the scenario
	 * @return configuration file name
	 */
	public static String getScenarioConfigName(final String scenarioName) {
		return format("%s-config.json", scenarioName);
	}

	/**
	 * Method creates a log of scenario description.
	 *
	 * @param scenarioDescription scenario description
	 * @return information that can be logged
	 */
	public static String getScenarioName(final ScenarioDescription scenarioDescription) {
		return format("Executing test scenario. %s", scenarioDescription.getDescription());
	}

	private static Map<Integer, List<String>> readDetectedAnomalies(final String fullDetectionPath,
			final Double threshold) {
		final InputStream inputStream = ScenarioReader.class.getClassLoader().getResourceAsStream(fullDetectionPath);

		try (final Scanner scanner = new Scanner(inputStream)) {
			final AtomicInteger rowCount = new AtomicInteger(0);
			final Map<Integer, List<String>> detectedAnomalies = new HashMap<>();

			while (scanner.hasNextLine()) {
				final String[] resultRow = scanner.nextLine().split(",");
				if (rowCount.get() > 0 && parseDouble(resultRow[ROW_ANOMALY_RESULT]) > threshold) {
					detectedAnomalies.put(rowCount.get(), List.of("OTHER"));
				}
				rowCount.incrementAndGet();
			}
			return detectedAnomalies;
		}
	}

	private static Double readAnomalyScore(final String fullResultsPath, final String detectionFileName) {
		final InputStream inputStream = ScenarioReader.class.getClassLoader().getResourceAsStream(fullResultsPath);

		try (final Scanner scanner = new Scanner(inputStream)) {
			final AtomicInteger rowCount = new AtomicInteger(0);

			while (scanner.hasNextLine()) {
				final String[] resultRow = scanner.nextLine().split(",");
				if (rowCount.get() > 0 && resultRow[ROW_FILE_NAME].equals(detectionFileName)) {
					return parseDouble(resultRow[ROW_SCORE]);
				}
				rowCount.incrementAndGet();
			}
		}
		return null;
	}
}
