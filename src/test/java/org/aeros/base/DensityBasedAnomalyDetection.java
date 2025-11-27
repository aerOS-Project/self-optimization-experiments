package org.aeros.base;

import static java.lang.Math.pow;
import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toMap;
import static org.aeros.utils.StatisticalOperations.computeDensity;
import static org.aeros.utils.StatisticalOperations.computeMeanDensity;
import static org.aeros.utils.StatisticalOperations.computeMeanValue;
import static org.aeros.utils.StatisticalOperations.computeScalarProduct;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.aeros.base.config.DensityBasedAnomalyConfiguration;
import org.aeros.base.parameters.DensityBasedAnomaliesParameters;
import org.aeros.domain.TestInfrastructureElement;
import org.aeros.domain.TestInfrastructureElementState;
import org.slf4j.Logger;

public class DensityBasedAnomalyDetection {

	private static final Logger logger = getLogger(DensityBasedAnomalyDetection.class);

	private final Map<String, DensityBasedAnomalyCache> cacheMap;
	private final DensityBasedAnomalyConfiguration anomalyModelConfiguration;

	public DensityBasedAnomalyDetection(final DensityBasedAnomalyConfiguration anomalyModelConfiguration) {
		this.anomalyModelConfiguration = anomalyModelConfiguration;
		cacheMap = initializeCacheMap();
	}

	public List<String> detectAnomalies(final TestInfrastructureElementState infrastructureElementState,
			final TestInfrastructureElement testIe) {
		return anomalyModelConfiguration.getModelsProperties().stream()
				.map(modelProperties -> detectAnomalyForModel(modelProperties, infrastructureElementState, testIe))
				.filter(Optional::isPresent)
				.map(Optional::get)
				.toList();
	}

	private Optional<String> detectAnomalyForModel(final DensityBasedAnomaliesParameters modelProperties,
			final TestInfrastructureElementState infrastructureElementState,
			final TestInfrastructureElement testIe) {
		final String metricName = modelProperties.getName();
		final Double currentValue = infrastructureElementState.getMetricValue(metricName, testIe);
		final DensityBasedAnomalyCache anomalyCache = cacheMap.get(metricName);

		final int sampleSize = anomalyCache.getDataSampleSize().incrementAndGet();
		final int stateCounter = anomalyCache.getCurrentStateCounter().incrementAndGet();

		final double sampleMean = computeMeanValue(sampleSize, anomalyCache.getSampleMean(), currentValue);
		final double scalarProduct = computeScalarProduct(sampleSize, anomalyCache.getScalarProduct(), currentValue);
		final double density = computeDensity(sampleMean, scalarProduct, currentValue);
		final double averageDensity = computeMeanDensity(anomalyCache.getSampleDensity(), stateCounter, density);
		anomalyCache.update(sampleMean, scalarProduct, density);

		return of(anomalyCache)
				.filter(not(DensityBasedAnomalyCache::isInAnomalousState))
				.map(cache -> handleNormalState(cache, modelProperties, density, averageDensity, currentValue))
				.orElseGet(() -> handleAnomalousState(anomalyCache, modelProperties, density, currentValue));
	}

	private Optional<String> handleNormalState(final DensityBasedAnomalyCache anomalyCache,
			final DensityBasedAnomaliesParameters modelProperties,
			final double density,
			final double averageDensity,
			final double currentValue
	) {
		anomalyCache.update(averageDensity);
		if (isInAnomalousState(density, averageDensity, modelProperties.getToleranceThresholdAnomaly())) {
			if (didStateChangedSufficiently(anomalyCache, modelProperties.getWindowAnomaly())) {
				anomalyCache.switchAnomalyState();
				anomalyCache.getCurrentStateCounter().set(0);

				final String anomalyCategory = currentValue > anomalyCache.getSampleMean() ? "INCREASE" : "DECREASE";
				final String anomaly = format("%s_%s", modelProperties.getName(), anomalyCategory);

				logger.info("Detected anomaly: {}.", anomaly);
				return of(anomaly);
			}
		} else {
			anomalyCache.getChangeIndicationCounter().set(0);
		}
		return empty();
	}

	private Optional<String> handleAnomalousState(final DensityBasedAnomalyCache anomalyCache,
			final DensityBasedAnomaliesParameters modelProperties, final double density, final double currentValue) {
		if (isInNormalState(density, anomalyCache.getAverageDensity(), modelProperties.getToleranceThresholdNormal())) {
			if (didStateChangedSufficiently(anomalyCache, modelProperties.getWindowNormal())) {
				logger.info("Resetting anomalous state...");

				anomalyCache.switchAnomalyState();
				anomalyCache.getCurrentStateCounter().set(0);
				anomalyCache.update(currentValue, pow(currentValue, 2), 1D);
				anomalyCache.update(1D);
			}
		} else {
			anomalyCache.getChangeIndicationCounter().set(0);
		}
		return empty();
	}

	private boolean didStateChangedSufficiently(final DensityBasedAnomalyCache anomalyCache, final int minimalWindow) {
		final int changeStateCounter = anomalyCache.getChangeIndicationCounter().incrementAndGet();
		return changeStateCounter >= minimalWindow;
	}

	private boolean isInNormalState(final double density, final double averageDensity, final double toleranceNormal) {
		return density >= averageDensity * toleranceNormal;
	}

	private boolean isInAnomalousState(final double density, final double averageDensity,
			final double toleranceAnomaly) {
		return density <= averageDensity * toleranceAnomaly;
	}

	private Map<String, DensityBasedAnomalyCache> initializeCacheMap() {
		return anomalyModelConfiguration.getModelsProperties().stream()
				.collect(toMap(DensityBasedAnomaliesParameters::getName, _ -> new DensityBasedAnomalyCache()));
	}
}
