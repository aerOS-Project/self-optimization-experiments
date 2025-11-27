package org.aeros.algorithms.parameters;

import org.aeros.domain.AlgorithmParameters;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Parameters of the NAB anomaly detection algorithm
 *
 * @see NABAnomalyParameters
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class NABAnomalyParameters extends AlgorithmParameters {

	private String detectionResultsFileName;
	private String detectionCSVFileName;
	private String scoreFileName;
	private String metricType;
	private Double threshold;
}
