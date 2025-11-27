package org.aeros.domain;

import static java.util.Optional.ofNullable;
import static java.util.function.Predicate.not;

import java.util.Optional;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * A REST object that represents current performance of IE monitored and reported by self-awareness service
 */
@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TestInfrastructureElementStateREST {

	@NotNull
	private String id;
	private Integer cpuCores;
	private Integer currentCpuUsage;
	private Integer ramCapacity;
	private Integer availableRam;
	private Integer currentRamUsage;
	private Integer currentRamUsagePct;
	private Integer diskCapacity;
	private Integer availableDisk;
	private Integer currentDiskUsage;
	private Integer currentDiskUsagePct;
	private Boolean realTimeCapable;

	/**
	 * @implNote The currentDiskUsage and currentRamUsage represent precise amounts of Disk and RAM usage, while
	 * currentCpuUsage represents percentage usage value. Therefore, to uphold coherence in further  computations,
	 * currentCpuUsage is converted to the amount of used CPU cores.
	 */
	public static Double getAmountOfUsedCores(final TestInfrastructureElementStateREST state) {
		if (Stream.of(ofNullable(state.getCpuCores()), ofNullable(state.getCurrentCpuUsage()))
				.anyMatch(not(Optional::isPresent))) {
			return null;
		}
		return (state.getCpuCores().doubleValue() * state.getCurrentCpuUsage()) / 100;
	}

}
