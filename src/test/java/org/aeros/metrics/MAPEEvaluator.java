package org.aeros.metrics;

import static java.lang.Math.abs;
import static java.lang.String.format;

import java.util.List;
import java.util.function.ToIntFunction;
import java.util.stream.IntStream;

import org.aeros.domain.TestInfrastructureElementStateREST;

/**
 * Class contains methods that compute different Mean Average Percentage Error (MAPE)
 * quality assessment metrics.
 */
public class MAPEEvaluator {

	private final List<TestInfrastructureElementStateREST> monitoredSamples;
	private final List<TestInfrastructureElementStateREST> trueSample;

	/**
	 * Default constructor.
	 *
	 * @param monitoredSamples a list encompassing monitored observations
	 * @param trueSample       a list encompassing all observations
	 */
	public MAPEEvaluator(final List<TestInfrastructureElementStateREST> monitoredSamples,
			final List<TestInfrastructureElementStateREST> trueSample) {
		this.monitoredSamples = monitoredSamples;
		this.trueSample = trueSample;
	}

	/**
	 * @return average MAPE computed based on MAPEs of CPU, RAM and disk utilization
	 */
	public double computeAvgMAPE() {
		return (computeMAPEForCPU() + computeMAPEForRAM() + computeMAPEForDisk()) / 3;
	}

	/**
	 * @return MAPE computed based on CPU utilization
	 */
	public double computeMAPEForCPU() {
		return computeMAPE(TestInfrastructureElementStateREST::getCurrentCpuUsage);
	}

	/**
	 * @return MAPE computed based on RAM utilization
	 */
	public double computeMAPEForRAM() {
		return computeMAPE(TestInfrastructureElementStateREST::getCurrentRamUsage);
	}

	/**
	 * @return MAPE computed based on disk utilization
	 */
	public double computeMAPEForDisk() {
		return computeMAPE(TestInfrastructureElementStateREST::getCurrentDiskUsage);
	}

	/**
	 * Formats information about MAPE that is to be displayed.
	 *
	 * @param valueMAPE MAPE value;
	 * @param type      type of computed MAPE
	 * @return formatted message
	 */
	public String formatLog(final double valueMAPE, final String type) {
		return format("[MAPE] Mean Absolute Percentage Error (%s): %f%%", type, valueMAPE);
	}

	private double computeMAPE(final ToIntFunction<TestInfrastructureElementStateREST> computeForMetric) {
		return IntStream.range(0, monitoredSamples.size()).boxed()
					   .mapToDouble(idx -> {
						   final int trueMetricVal = computeForMetric.applyAsInt(trueSample.get(idx));
						   final int monitoredMetricVal = computeForMetric.applyAsInt(monitoredSamples.get(idx));

						   return trueMetricVal == 0
								   ? monitoredMetricVal
								   : abs((trueMetricVal - monitoredMetricVal) / trueMetricVal) * 100;
					   })
					   .sum() / monitoredSamples.size();
	}
}
