package org.aeros.base.config;

import java.util.List;

import org.aeros.base.parameters.DensityBasedAnomaliesParameters;
import org.aeros.domain.AlgorithmConfig;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class DensityBasedAnomalyConfiguration extends AlgorithmConfig {

	private List<DensityBasedAnomaliesParameters> modelsProperties;
}
