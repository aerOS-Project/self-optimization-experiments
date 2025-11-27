package org.aeros.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Attributes used to define base algorithms that will be used in experiments.
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AlgorithmConfigDescription {

	private AlgorithmType type;
	private AlgorithmConfig config;
}
