package org.aeros.domain;

import org.aeros.base.config.DensityBasedAnomalyConfiguration;
import org.aeros.base.config.PEWMASamplingConfiguration;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.AllArgsConstructor;

/**
 * Abstract class that must be extended by all classes, which will specify configuration of base algorithms.
 */
@JsonTypeInfo(
		use = JsonTypeInfo.Id.NAME,
		property = "type"
)
@JsonSubTypes({
		@JsonSubTypes.Type(value = PEWMASamplingConfiguration.class, name = "SAMPLING"),
		@JsonSubTypes.Type(value = DensityBasedAnomalyConfiguration.class, name = "ANOMALY")
})
@AllArgsConstructor
public abstract class AlgorithmConfig {

}
