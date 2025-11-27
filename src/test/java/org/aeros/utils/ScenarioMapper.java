package org.aeros.utils;

import java.util.List;
import java.util.Map;

import org.aeros.domain.TestInfrastructureElementStateREST;
import org.aeros.domain.ScenarioDescription;
import org.aeros.domain.TestInfrastructureElementState;

/**
 * Class with methods used to map entities defined in test scenarios.
 */
public class ScenarioMapper {

	private static final String VALUE = "value";

	/**
	 * Methods maps a list of test IE entity into a list of REST IE object.
	 *
	 * @param scenarioDescription         description of the scenario containing domain IE characteristics
	 * @param infrastructureElementStates list of IE states defined in test data
	 * @return list of IE REST objects
	 */
	public static List<TestInfrastructureElementStateREST> mapToIEREST(
			final ScenarioDescription scenarioDescription,
			final List<TestInfrastructureElementState> infrastructureElementStates) {
		return infrastructureElementStates.stream()
				.map(ieState -> new TestInfrastructureElementStateREST(scenarioDescription.getIe().getId(),
						scenarioDescription.getIe().getCpuCores(),
						ieState.getCurrentCpuUsage().get(VALUE),
						scenarioDescription.getIe().getRamCapacity(),
						ieState.getAvailableRam().get(VALUE),
						ieState.getCurrentRamUsage().get(VALUE),
						ieState.getCurrentRamUsagePct().get(VALUE),
						scenarioDescription.getIe().getDiskCapacity(),
						ieState.getAvailableDisk().get(VALUE),
						ieState.getCurrentDiskUsage().get(VALUE),
						ieState.getCurrentDiskUsagePct().get(VALUE),
						ieState.getRealTimeCapable().get(VALUE)))
				.toList();
	}

	/**
	 * Method maps test IE into an IE monitored with adaptive sampling (i.e. with updated utilization values).
	 *
	 * @param ieREST          original IE
	 * @param monitoredValues monitored utilization values
	 * @return updated IE
	 */
	public static TestInfrastructureElementStateREST mapToMonitoredIE(
			final TestInfrastructureElementStateREST ieREST, final Map<String, Double> monitoredValues) {
		final TestInfrastructureElementStateREST updatedIE = new TestInfrastructureElementStateREST(
				ieREST.getId(),
				ieREST.getCpuCores(),
				(int) (monitoredValues.get("CPU") * 100 / ieREST.getCpuCores()),
				ieREST.getRamCapacity(),
				ieREST.getAvailableRam(),
				monitoredValues.get("RAM").intValue(),
				ieREST.getCurrentDiskUsagePct(),
				ieREST.getDiskCapacity(),
				ieREST.getAvailableDisk(),
				monitoredValues.get("DISK").intValue(),
				ieREST.getCurrentDiskUsagePct(),
				ieREST.getRealTimeCapable()
		);
		return updatedIE;
	}
}
