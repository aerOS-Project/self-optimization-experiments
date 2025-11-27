package org.aeros.utils;

import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.Optional.ofNullable;
import static java.util.function.Predicate.not;
import static org.aeros.utils.ChartFactory.crateMonitoredSampleChart;
import static org.aeros.utils.ChartFactory.crateRealSampleChart;
import static org.aeros.utils.ChartFactory.createAnomalyChart;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.math.IEEE754rUtils.max;
import static org.knowm.xchart.BitmapEncoder.BitmapFormat.PNG;
import static org.knowm.xchart.XYSeries.XYSeriesRenderStyle.Scatter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;
import java.util.stream.IntStream;

import org.aeros.domain.TestInfrastructureElementStateREST;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.XYChart;

/**
 * Class with methods that support visualization and storage of obtained test results
 */
public class ResultVisualization {

	private static final String TEST_PATH = "src/test/resources/test-scenarios/results";

	/**
	 * Method plots a chart with anomalous observations, which is then saved.
	 *
	 * @param realSamples          data of real sample that is to be visualized
	 * @param anomalies            a map consisting of anomaly indexes and types of anomalies
	 * @param getMetricUtilization function applied to retrieve relevant metric  data
	 * @param getMetricCapacity    function applied to retrieve capacity of the metric
	 * @param testTitle            details (i.e. type of conducted test) attached to the chart title
	 * @param methodName           name of the method used in sampling
	 * @param metricType           type of the metric for which the chart is plotted
	 * @param unit                 unit of the metric for which the chart is plotted
	 */
	public static void plotAndSaveDataSampleChartWithAnomalies(final List<TestInfrastructureElementStateREST> realSamples,
			final Map<Integer, List<String>> anomalies,
			final ToDoubleFunction<TestInfrastructureElementStateREST> getMetricUtilization,
			final ToDoubleFunction<TestInfrastructureElementStateREST> getMetricCapacity,
			final String testTitle,
			final String methodName,
			final String metricType,
			final String unit) {
		final double[] sampleData = realSamples.stream().mapToDouble(getMetricUtilization).toArray();
		final double[] timeStamps = getTimeInterval(sampleData.length);
		final List<Integer> anomaliesIncrease = filterAnomaliesByType("INCREASE", anomalies);
		final List<Integer> anomaliesDecrease = filterAnomaliesByType("DECREASE", anomalies);
		final List<Integer> anomaliesOther = filterAnomaliesByType("OTHER", anomalies);

		final double maxYValue = getMaxYValue(realSamples, getMetricCapacity, sampleData);
		final String yAxisTitle = format("%s usage [%s]", capitalize(metricType), unit);

		final XYChart anomalyChart = createAnomalyChart(metricType, yAxisTitle, methodName, maxYValue, timeStamps,
				sampleData);

		ofNullable(anomaliesIncrease).filter(not(List::isEmpty))
				.ifPresent(indexes -> createAnomalySeries(sampleData, indexes, anomalyChart, "Increase"));
		ofNullable(anomaliesDecrease).filter(not(List::isEmpty))
				.ifPresent(indexes -> createAnomalySeries(sampleData, indexes, anomalyChart, "Decrease"));
		ofNullable(anomaliesOther).filter(not(List::isEmpty))
				.ifPresent(indexes -> createAnomalySeries(sampleData, indexes, anomalyChart));

		saveChart(anomalyChart, "anomalies", testTitle);
	}

	/**
	 * Method plots 2 charts:
	 * (1) Representing monitored data sample
	 * (2) Representing real data sample
	 * <p>
	 * Both charts are saved as pictures in separate files.
	 *
	 * @param realSamples          data of real sample that is to be visualized
	 * @param monitoredSamples     data of monitored sample that is to be visualized
	 * @param getMetricUtilization function applied to retrieve relevant metric  data
	 * @param getMetricCapacity    function applied to retrieve capacity of the metric
	 * @param testTitle            details (i.e. type of conducted test) attached to the chart title
	 * @param methodName           name of the method used in sampling
	 * @param metricType           type of the metric for which the chart is plotted
	 * @param unit                 unit of the metric for which the chart is plotted
	 */
	public static void plotAndSaveSamplingCharts(final List<TestInfrastructureElementStateREST> realSamples,
			final List<TestInfrastructureElementStateREST> monitoredSamples,
			final ToDoubleFunction<TestInfrastructureElementStateREST> getMetricUtilization,
			final ToDoubleFunction<TestInfrastructureElementStateREST> getMetricCapacity,
			final String testTitle,
			final String methodName,
			final String metricType,
			final String unit) {
		final double[] monitoredSampleData = monitoredSamples.stream().mapToDouble(getMetricUtilization).toArray();
		final double[] realSampleData = realSamples.stream().mapToDouble(getMetricUtilization).toArray();
		final double[] timeStamps = getTimeInterval(realSampleData.length);

		final double maxYValue = getMaxYValue(realSamples, getMetricCapacity, realSampleData);
		final String yAxisTitle = format("%s usage [%s]", capitalize(metricType), unit);

		final XYChart monitoredChart = crateMonitoredSampleChart(metricType, yAxisTitle, methodName, maxYValue,
				timeStamps, monitoredSampleData);
		final XYChart realChart = crateRealSampleChart(metricType, yAxisTitle, maxYValue, timeStamps, realSampleData);

		saveChart(monitoredChart, "monitoredsample", testTitle);
		saveChart(realChart, "realsample", testTitle);
	}

	private static void createAnomalySeries(final double[] sampleData, final List<Integer> anomalies,
			final XYChart chart, final String type) {
		final double[] anomalyPoints = getAnomaliesValues(sampleData.length, anomalies, sampleData);
		final double[] anomalyTimeStamps = anomalies.stream().mapToDouble(Integer::doubleValue).toArray();

		chart.addSeries(format("Anomalous State (%s)", type), anomalyTimeStamps, anomalyPoints);
		chart.getSeriesMap().get(format("Anomalous State (%s)", type)).setXYSeriesRenderStyle(Scatter);
	}

	private static void createAnomalySeries(final double[] sampleData, final List<Integer> anomalies,
			final XYChart chart) {
		final double[] anomalyPoints = getAnomaliesValues(sampleData.length, anomalies, sampleData);
		final double[] anomalyTimeStamps = anomalies.stream().mapToDouble(Integer::doubleValue).toArray();

		chart.addSeries(("Anomalous State"), anomalyTimeStamps, anomalyPoints);
		chart.getSeriesMap().get("Anomalous State").setXYSeriesRenderStyle(Scatter);
	}

	private static double getMaxYValue(final List<TestInfrastructureElementStateREST> realSamples,
			final ToDoubleFunction<TestInfrastructureElementStateREST> getMetricCapacity, final double[] realSampleData) {
		final double maxCapacity = getMetricCapacity.applyAsDouble(realSamples.getFirst());
		return max(Arrays.stream(realSampleData).max().orElse(maxCapacity), maxCapacity);
	}

	private static List<Integer> filterAnomaliesByType(final String anomalyType, final Map<Integer, List<String>> detectedAnomalies) {
		return detectedAnomalies.entrySet().stream()
				.filter(anomaly -> anomaly.getValue().stream().anyMatch(name -> name.contains(anomalyType)))
				.map(Map.Entry::getKey)
				.sorted()
				.toList();
	}

	private static double[] getAnomaliesValues(final int dataSize, final List<Integer> filteredAnomalies,
			final double[] sampleData) {
		return IntStream.rangeClosed(1, dataSize).boxed()
				.filter(filteredAnomalies::contains)
				.mapToDouble(idx -> sampleData[idx - 1])
				.toArray();
	}

	private static double[] getTimeInterval(final int dataSize) {
		return IntStream.rangeClosed(1, dataSize).boxed().mapToDouble(Integer::doubleValue).toArray();
	}

	private static void saveChart(final XYChart chart, final String type, final String fileName) {
		try {
			final String outputPath = join("/", TEST_PATH, type);
			Files.createDirectories(Paths.get(outputPath));
			BitmapEncoder.saveBitmap(chart, join("/", outputPath, fileName), PNG);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}
}
