package org.aeros.algorithms.parameters;

import org.aeros.domain.AlgorithmParameters;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Parameters of the UDASA adaptive sampling algorithm
 *
 * @see UDASAParameters
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UDASAParameters extends AlgorithmParameters {

	private int windowSize;
	private int savingSize;
	private int baseSamplingPeriod;
}
