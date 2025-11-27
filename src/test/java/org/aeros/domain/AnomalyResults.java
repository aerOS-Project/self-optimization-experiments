package org.aeros.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Attributes represent results of scoring.
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AnomalyResults {

	private Double value;
	private Double anomalyScore;
	private Integer label;

}
