package org.aeros.base.parameters;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Parameters of the density-based anomaly detection algorithm
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class DensityBasedAnomaliesParameters {

	private String name;
	private Double toleranceThresholdAnomaly;
	private Double toleranceThresholdNormal;
	private Integer windowAnomaly;
	private Integer windowNormal;

}
