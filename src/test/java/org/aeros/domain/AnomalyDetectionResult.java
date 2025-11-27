package org.aeros.domain;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AnomalyDetectionResult {

	private Map<Integer, List<String>> detectedAnomalies;
	private Double score;
}
