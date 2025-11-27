package org.aeros.metrics;

import static java.lang.String.format;

import java.util.List;

import org.aeros.domain.TestInfrastructureElementStateREST;

/**
 * Class contains methods that calculates different ratios among monitored sample and a true one
 */
public class RatioEvaluator {

	private final int monitoredSamplesSize;
	private final List<TestInfrastructureElementStateREST> trueSample;

	/**
	 * Default constructor.
	 *
	 * @param monitoredSamplesSize number of monitored samples
	 * @param trueSample           a list encompassing all observations
	 */
	public RatioEvaluator(final int monitoredSamplesSize,
			final List<TestInfrastructureElementStateREST> trueSample) {
		this.monitoredSamplesSize = monitoredSamplesSize;
		this.trueSample = trueSample;
	}

	/**
	 * Formats information about computed ratio that is to be displayed.
	 *
	 * @param value ratio value;
	 * @return formatted message
	 */
	public String formatLog(final double value) {
		return format("[Ratio] Percentage of monitored samples: %f%%", value);
	}

	/**
	 * @return ratio between monitored and true data samples
	 */
	public double computeDataSamplesVolumeRatio() {
		return ((double) monitoredSamplesSize / trueSample.size()) * 100;
	}
}
