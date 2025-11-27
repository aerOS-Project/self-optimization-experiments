package org.aeros.metrics;

import static java.lang.String.format;

/**
 * Class contains methods that compute different Joint Performance Metrics (JPM)
 */
public class JPMEvaluator {

	private final MAPEEvaluator mapeEvaluator;
	private final RatioEvaluator ratioEvaluator;

	/**
	 * Default constructor.
	 *
	 * @param mapeEvaluator  class with methods used to compute MAPE
	 * @param ratioEvaluator class with methods used to compute sample ratio
	 */
	public JPMEvaluator(final MAPEEvaluator mapeEvaluator, final RatioEvaluator ratioEvaluator) {
		this.mapeEvaluator = mapeEvaluator;
		this.ratioEvaluator = ratioEvaluator;
	}

	/**
	 * @return average JPM computed based on JPMs of CPU, RAM and disk utilization
	 */
	public double computeAvgJPM() {
		return (computeJPMForCPU() + computeJPMForRAM() + computeJPMForDisk()) / 3;
	}

	/**
	 * @return JPM computed based on CPU utilization
	 */
	public double computeJPMForCPU() {
		return computeJPM(mapeEvaluator.computeMAPEForCPU());
	}

	/**
	 * @return JPM computed based on RAM utilization
	 */
	public double computeJPMForRAM() {
		return computeJPM(mapeEvaluator.computeMAPEForRAM());
	}

	/**
	 * @return JPM computed based on disk utilization
	 */
	public double computeJPMForDisk() {
		return computeJPM(mapeEvaluator.computeMAPEForDisk());
	}

	/**
	 * Formats information about JPM that is to be displayed.
	 *
	 * @param valueJPM JPM value;
	 * @param type     type of computed JPM
	 * @return formatted message
	 */
	public String formatLog(final double valueJPM, final String type) {
		return format("[JPM] Joint-Performance Metric (%s): %f%%", type, valueJPM);
	}

	private double computeJPM(final Double estimatedMAPE) {
		return 100 - ((estimatedMAPE + ratioEvaluator.computeDataSamplesVolumeRatio()) / 2);
	}
}
