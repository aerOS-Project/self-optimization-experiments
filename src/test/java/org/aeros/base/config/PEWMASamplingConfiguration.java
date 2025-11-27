package org.aeros.base.config;

import java.util.List;

import org.aeros.base.parameters.PEWMASamplingParameters;
import org.aeros.domain.AlgorithmConfig;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PEWMASamplingConfiguration extends AlgorithmConfig {

	private List<PEWMASamplingParameters> modelsProperties;
}
