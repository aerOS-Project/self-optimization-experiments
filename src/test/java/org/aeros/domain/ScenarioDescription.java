package org.aeros.domain;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Class encompasses all attributes used to define a complete testing scenario
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ScenarioDescription {

	public static final String NUMENTA_JUMPS_SCENARIO = "numenta-jumps-scenario";
	public static final String NUMENTA_SPIKES_SCENARIO = "numenta-load-spikes-scenario";
	public static final String RAINMON_SCENARIO = "rainmon-traces-scenario";
	public static final String AEROS_SCENARIO = "aeros-scenario";

	private String name;
	private String description;
	private TestInfrastructureElement ie;
	private Map<MetricType, MetricParameters> evaluationMetrics;
	private List<AlgorithmConfigDescription> baseAlgorithmsConfig;
	private List<AlgorithmDescription> algorithmsForComparison;

}
