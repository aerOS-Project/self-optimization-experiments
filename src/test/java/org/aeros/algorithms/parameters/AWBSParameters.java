package org.aeros.algorithms.parameters;

import org.aeros.domain.AlgorithmParameters;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Parameters of the AWBS adaptive sampling algorithm
 *
 * @see AWBSParameters
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AWBSParameters extends AlgorithmParameters {

	private double threshold;
	private int maxWindowSize;
	private int initialWindowSize;
}
