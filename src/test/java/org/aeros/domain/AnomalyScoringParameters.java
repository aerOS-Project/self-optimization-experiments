package org.aeros.domain;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Parameters used in the Anomaly Scoring quality metric.
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AnomalyScoringParameters extends MetricParameters {

	private List<AnomalyWindow> anomalyWindows;
	private Double truePositiveWeight;
	private Double falsePositiveWeight;
	private Double falseNegativeWeight;
	private Double baseline;
}
