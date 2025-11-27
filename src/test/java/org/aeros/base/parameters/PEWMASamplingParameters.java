package org.aeros.base.parameters;

import org.aeros.domain.SamplingModelType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Parameters of the PEWMA-based adaptive sampling algorithm
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PEWMASamplingParameters {

	private SamplingModelType type;
	private long minPeriod;
	private long maxPeriod;
	private double valueWeightFactor;
	private double probabilityWeightFactor;
	private double imprecision;
	private long multiplicity;

}
