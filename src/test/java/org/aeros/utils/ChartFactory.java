package org.aeros.utils;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.knowm.xchart.XYSeries.XYSeriesRenderStyle.Line;
import static org.knowm.xchart.style.Styler.ChartTheme.GGPlot2;

import java.awt.Color;

import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.style.Styler;
import org.knowm.xchart.style.XYStyler;

/**
 * Class contains methods that can be used to create charts.
 */
public class ChartFactory {

	private static final Color RED_COLOR = new Color(186, 18, 18);
	private static final Color YELLOW_COLOR = new Color(228, 180, 30);
	private static final Color ORANGE_COLOR = new Color(228, 99, 30);
	private static final Color BLUE_COLOR = new Color(20, 105, 189);

	/**
	 * Method generates chart of a monitored data sample.
	 *
	 * @param metricType          type of metric for which the chart is plotted
	 * @param yAxisTitle          title placed over the y-Axis
	 * @param methodName          name of the method used in sampling
	 * @param maxYValue           maximal value of the y-Axis
	 * @param monitoredSampleData monitored sample data that is to be plotted
	 * @param timeStamps          time intervals over which data is to be plotted
	 * @return chart that presents real data sample
	 */
	public static XYChart crateMonitoredSampleChart(final String metricType,
			final String yAxisTitle,
			final String methodName,
			final Double maxYValue,
			final double[] timeStamps,
			final double[] monitoredSampleData) {
		final String title = format("Monitored %s usage using %s over time", metricType, methodName);

		final XYChart monitoredChart = createTimeSeriesChart(title, yAxisTitle, 1000);
		applyDefaultStylingForColors(new Color[] { BLUE_COLOR }, maxYValue, monitoredChart.getStyler());
		monitoredChart.addSeries("Monitored Samples", timeStamps, monitoredSampleData);
		monitoredChart.getSeriesMap().get("Monitored Samples").setLineWidth(1);

		return monitoredChart;
	}

	/**
	 * Method generates chart of a real data sample.
	 *
	 * @param metricType     type of metric for which the chart is plotted
	 * @param yAxisTitle     title placed over the y-Axis
	 * @param maxYValue      maximal value of the y-Axis
	 * @param realSampleData real sample data that is to be plotted
	 * @param timeStamps     time intervals over which data is to be plotted
	 * @return chart that presents real data sample
	 */
	public static XYChart crateRealSampleChart(final String metricType,
			final String yAxisTitle,
			final Double maxYValue,
			final double[] timeStamps,
			final double[] realSampleData) {
		final String title = format("%s usage over time", capitalize(metricType));

		final XYChart realChart = createTimeSeriesChart(title, yAxisTitle, 1000);
		applyDefaultStylingForColors(new Color[] { RED_COLOR }, maxYValue, realChart.getStyler());
		realChart.addSeries("Real Samples", timeStamps, realSampleData);
		realChart.getSeriesMap().get("Real Samples").setLineWidth(1);

		return realChart;
	}

	/**
	 * Method generates chart that can be used as a basis to add anomaly points.
	 *
	 * @param metricType type of metric for which the chart is plotted
	 * @param yAxisTitle title placed over the y-Axis
	 * @param maxYValue  maximal value of the y-Axis
	 * @param sampleData sample data that is to be plotted
	 * @param timeStamps time intervals over which data is to be plotted
	 * @return chart that presents real data sample
	 */
	public static XYChart createAnomalyChart(final String metricType,
			final String yAxisTitle,
			final String methodName,
			final Double maxYValue,
			final double[] timeStamps,
			final double[] sampleData) {
		final String title = format("%s anomalies detected using %s over time", capitalize(metricType), methodName);

		final XYChart chart = createTimeSeriesChart(title, yAxisTitle, 1500);
		chart.getStyler().setSeriesColors(new Color[] { BLUE_COLOR, RED_COLOR, YELLOW_COLOR, ORANGE_COLOR });
		chart.getStyler().setLegendPosition(Styler.LegendPosition.InsideNE);

		chart.addSeries("Data Samples", timeStamps, sampleData);
		chart.getSeriesMap().get("Data Samples").setXYSeriesRenderStyle(Line);
		chart.getSeriesMap().get("Data Samples").setLineWidth(1);
		chart.getStyler().setYAxisMax(maxYValue);
		chart.getStyler().setYAxisMin(0.0);
		chart.getStyler().setXAxisTickMarkSpacingHint(500);

		return chart;
	}

	private static void applyDefaultStylingForColors(final Color[] colors, final double maxYVal,
			final XYStyler styler) {
		styler.setLegendVisible(false);
		styler.setDefaultSeriesRenderStyle(Line);
		styler.setMarkerSize(1);
		styler.setYAxisMax(maxYVal);
		styler.setYAxisMin(0.0);
		styler.setSeriesColors(colors);
	}

	private static XYChart createTimeSeriesChart(final String title, final String yTitle, final int width) {
		return new XYChartBuilder()
				.width(width)
				.height(400)
				.title(title)
				.xAxisTitle("Time [step]")
				.yAxisTitle(yTitle)
				.theme(GGPlot2)
				.build();
	}
}
