package org.aeros.domain;

import org.aeros.algorithms.parameters.AWBSParameters;
import org.aeros.algorithms.parameters.NABAnomalyParameters;
import org.aeros.algorithms.parameters.UDASAParameters;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.AllArgsConstructor;

/**
 * Abstract class that must be extended by all classes, which will specify parameters of individual algorithms.
 */
@JsonTypeInfo(
		use = JsonTypeInfo.Id.NAME,
		property = "type"
)
@JsonSubTypes({
		@JsonSubTypes.Type(value = UDASAParameters.class, name = "UDASA"),
		@JsonSubTypes.Type(value = AWBSParameters.class, name = "AWBS"),
		@JsonSubTypes.Type(value = NABAnomalyParameters.class, name = "ART"),
		@JsonSubTypes.Type(value = NABAnomalyParameters.class, name = "CONTEXTOSE")
})
@AllArgsConstructor
public abstract class AlgorithmParameters {

}
