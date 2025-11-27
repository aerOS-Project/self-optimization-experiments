package org.aeros.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A test IE instance used in the test scenario description.
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TestInfrastructureElement {

	private String id;
	private int cpuCores;
	private int ramCapacity;
	private int diskCapacity;
	private String data;
}
