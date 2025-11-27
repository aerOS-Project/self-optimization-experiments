package org.aeros.base;

import java.util.concurrent.atomic.AtomicInteger;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class DensityBasedAnomalyCache {

	private boolean isInAnomalousState;
	private AtomicInteger currentStateCounter;
	private AtomicInteger changeIndicationCounter;
	private AtomicInteger dataSampleSize;

	private Double sampleDensity;
	private Double averageDensity;
	private Double sampleMean;
	private Double scalarProduct;

	public DensityBasedAnomalyCache() {
		this.isInAnomalousState = false;
		this.dataSampleSize = new AtomicInteger(0);
		this.currentStateCounter = new AtomicInteger(0);
		this.changeIndicationCounter = new AtomicInteger(0);
	}

	public void update(final double newMean, final double newScalarProduct, final double newDensity) {
		sampleDensity = newDensity;
		sampleMean = newMean;
		scalarProduct = newScalarProduct;
	}

	public void update(final double newAverageDensity) {
		averageDensity = newAverageDensity;
	}

	public void switchAnomalyState() {
		isInAnomalousState = !isInAnomalousState;
	}

}
