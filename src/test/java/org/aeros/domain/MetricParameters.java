package org.aeros.domain;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.AllArgsConstructor;

/**
 * Abstract class that must be extended by all classes, which will specify parameters of quality assessment metrics.
 */

@JsonTypeInfo(
		use = JsonTypeInfo.Id.NAME,
		property = "name"
)
@JsonSubTypes({
		@JsonSubTypes.Type(value = AnomalyScoringParameters.class, name = "SCORING")
})
@AllArgsConstructor
public abstract class MetricParameters {
}
