package org.aeros.utils;

import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static java.lang.Math.exp;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import static java.util.Optional.ofNullable;

/**
 * Utility service that contains method performing statistical/mathematical operations
 */
public class StatisticalOperations {

	/**
	 * Method computes sample mean value.
	 *
	 * @param sampleSize   size of the sample
	 * @param previousMean previous mean value
	 * @param currentValue new observation value
	 * @return updated mean value
	 */
	public static double computeMeanValue(final int sampleSize, final Double previousMean,
			final Double currentValue) {
		return ofNullable(previousMean)
				.map(mean -> mean + (currentValue - mean) / sampleSize)
				.orElse(currentValue);
	}

	/**
	 * Method computes updated scalar product value.
	 *
	 * @param sampleSize            updated size of the sample
	 * @param previousScalarProduct previous scalar product value
	 * @param currentValue          new observation value
	 * @return updated scalar product value
	 */
	public static double computeScalarProduct(final int sampleSize, final Double previousScalarProduct,
			final Double currentValue) {
		final double squaredValue = pow(currentValue, 2);
		return ofNullable(previousScalarProduct)
				.map(scalarProduct -> scalarProduct + (squaredValue - scalarProduct) / sampleSize)
				.orElse(squaredValue);
	}

	/**
	 * Method computes data density based on current sample mean value and scalar product.
	 *
	 * @param sampleMean    mean of data sample
	 * @param scalarProduct data sample scalar product
	 * @param currentValue  new observation value
	 * @return data density
	 */
	public static double computeDensity(final double sampleMean, final double scalarProduct,
			final Double currentValue) {
		final double meanDeviation = currentValue - sampleMean;
		final double squaredMean = pow(sampleMean, 2);

		return 1 / (1 + pow(meanDeviation, 2) + scalarProduct - squaredMean);
	}

	/**
	 * Method computes mean data density.
	 *
	 * @param previousDensity  previous data density value
	 * @param occurrenceNumber number of consecutive observations for which the density remained unchanged
	 * @param newDensity       the latest density value
	 * @return mean density
	 */
	public static double computeMeanDensity(final Double previousDensity, final int occurrenceNumber,
			final double newDensity) {
		return ofNullable(previousDensity)
				.map(density -> getUpdatedDensity(density, newDensity, occurrenceNumber))
				.orElse(1D);
	}

	/**
	 * Method calculates the PEWMA probability using std and sample distance.
	 *
	 * @param distance                  new sample distance
	 * @param previousEstimatedDistance previous estimated PEWMA distance
	 * @param standardDeviation         previous estimated standard deviation
	 * @return PEWMA probability
	 */
	public static double computePEWMAProbability(final double distance, final double previousEstimatedDistance,
			final double standardDeviation) {
		if (standardDeviation == 0) {
			return distance == 0 ? 1.0 : 0.0;
		}

		final double observedDistanceDiff = distance - previousEstimatedDistance;
		final double stdRandomVariable = observedDistanceDiff / standardDeviation;
		final double stdRandomVariableSquare = pow(stdRandomVariable, 2);

		return exp(-stdRandomVariableSquare / 2) / sqrt(2 * PI);
	}

	/**
	 * Method computes standard deviation of distance measurement.
	 *
	 * @param distance distance measurement
	 * @return standard deviation
	 */
	public static double computeStandardDeviation(final double distance) {
		return distance / (2 * sqrt(2));
	}

	private static double getUpdatedDensity(final Double previousDensity, final double newDensity,
			final int occurrenceNumber) {
		final double densityDifference = abs(newDensity - previousDensity);
		return (previousDensity + (newDensity - previousDensity) / occurrenceNumber) * (1 - densityDifference) +
			   newDensity * densityDifference;
	}
}
