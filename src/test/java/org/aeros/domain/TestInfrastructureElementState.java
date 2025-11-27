package org.aeros.domain;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A test instance of IE state used in scenario description.
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TestInfrastructureElementState {

	private Map<String, Integer> currentCpuUsage;
	private Map<String, Integer> availableRam;
	private Map<String, Integer> currentRamUsage;
	private Map<String, Integer> currentRamUsagePct;
	private Map<String, Integer> availableDisk;
	private Map<String, Integer> currentDiskUsage;
	private Map<String, Integer> currentDiskUsagePct;
	private Map<String, Boolean> realTimeCapable;

	/**
	 * Method returns value corresponding to particular metric type.
	 *
	 * @param metricName type of the metric
	 * @return Double value
	 */
	public Double getMetricValue(final String metricName, final TestInfrastructureElement ie) {
		return switch (metricName) {
			case "CPU_USAGE" -> (double) (ie.getCpuCores() * currentCpuUsage.get("value") / 100);
			case "RAM_USAGE" -> (double) currentRamUsage.get("value");
			case "DISK_USAGE" -> (double) currentDiskUsage.get("value");
			default -> 0.0d;
		};
	}
}
