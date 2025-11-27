package org.aeros.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Attributes used to define additional algorithm that will be used in comparison.
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AlgorithmDescription {

	private AlgorithmType type;
	private AlgorithmParameters params;
}
