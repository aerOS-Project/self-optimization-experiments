package org.aeros.base;

import static java.lang.Math.abs;
import static java.lang.Math.ceil;
import static java.lang.Math.clamp;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import static java.lang.String.join;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.aeros.domain.SamplingModelType.RESOURCE;
import static org.aeros.utils.StatisticalOperations.computePEWMAProbability;
import static org.aeros.utils.StatisticalOperations.computeStandardDeviation;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.aeros.base.config.PEWMASamplingConfiguration;
import org.aeros.base.parameters.PEWMASamplingParameters;
import org.aeros.domain.PEWMAEstimation;
import org.aeros.domain.ResourceType;
import org.aeros.domain.SamplingModelType;
import org.aeros.domain.TestInfrastructureElement;
import org.aeros.domain.TestInfrastructureElementState;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

@Service
public class PEWMASampling {

	private static final Logger logger = getLogger(PEWMASampling.class);

	private final Map<String, PEWMASamplingCache> cacheMap;
	private final PEWMASamplingConfiguration samplingModelConfiguration;
	protected TestInfrastructureElementState ieState;

	public PEWMASampling(final PEWMASamplingConfiguration samplingModelConfiguration) {
		this.samplingModelConfiguration = samplingModelConfiguration;
		cacheMap = initializeCacheMap();
	}

	public long estimateSamplingPeriod(final TestInfrastructureElementState currentIEState,
			final TestInfrastructureElement ie) {
		return Stream.of("CPU_USAGE", "RAM_USAGE", "DISK_USAGE")
				.mapToLong(metric -> computeSamplingPeriod(
						getParamsForType(RESOURCE), join("_", RESOURCE.name(), metric),
						currentIEState.getMetricValue(metric, ie)))
				.min()
				.orElse(getParamsForType(RESOURCE).getMinPeriod());
	}

	private long computeSamplingPeriod(final PEWMASamplingParameters properties, final String type,
			final double sampleValue) {
		final PEWMASamplingCache cache = cacheMap.get(type);
		final long minTimePeriod = properties.getMinPeriod();
		final long maxTimePeriod = properties.getMaxPeriod();
		final long optimalMultiplicity = properties.getMultiplicity();
		final double imprecision = properties.getImprecision();
		final double valueWeightFactor = properties.getValueWeightFactor();
		final double probabilityWeightFactor = properties.getProbabilityWeightFactor();

		if (cache.isEmpty()) {
			cache.setCacheValues(minTimePeriod, 0D, sampleValue, 0);
			return minTimePeriod;
		}

		final double distance = abs(sampleValue - cache.getLastSampleValue());
		final double observedStd = computeStandardDeviation(distance);
		final double probability = computePEWMAProbability(distance, cache.getLastSampleDistance(),
				cache.getLastMovingStandardDeviation());

		final PEWMAEstimation estimatedEvolution =
				computeNextPEWMAEstimation(probability, distance, cache, valueWeightFactor, probabilityWeightFactor);
		final Double estimationConfidence = computeCurrentConfidence(cache.getLastMovingStandardDeviation(),
				observedStd);
		final double requiredPrecision = 1 - imprecision;

		final long estimatedSamplingPeriod = Optional.of(estimationConfidence)
				.filter(confidence -> confidence >= requiredPrecision)
				.map(confidence -> getEstimatedSamplingPeriod(confidence, cache, imprecision, maxTimePeriod,
						minTimePeriod, optimalMultiplicity))
				.orElse(minTimePeriod);

		cache.setCacheValues(estimatedSamplingPeriod, estimatedEvolution.getMovingAverage(), sampleValue,
				estimatedEvolution.getMovingStd());
		logger.info("[{}] Optimal estimated sampling period is equal to {}.", type, estimatedSamplingPeriod);

		return estimatedSamplingPeriod;
	}

	private long getEstimatedSamplingPeriod(final double confidence, final PEWMASamplingCache cache,
			final double imprecision, final long maxTimePeriod, final long minTimePeriod,
			final long optimalMultiplicity) {
		final double samplingPeriodEstimation =
				cache.getLastSamplingPeriod() + optimalMultiplicity * (1 + ((confidence - imprecision) / confidence));
		return (long) clamp(ceil(samplingPeriodEstimation), minTimePeriod, maxTimePeriod);
	}

	private PEWMAEstimation computeNextPEWMAEstimation(final double probability, final double distance,
			final PEWMASamplingCache cache, final double valueWeightingFactor, final double probabilityWeightingFactor) {
		final double adaptableFactor = valueWeightingFactor * (1 - probabilityWeightingFactor * probability);
		double distanceSquare = pow(distance, 2);

		final double newPEWMA = adaptableFactor * cache.getLastSampleDistance() + ((1 - adaptableFactor) * distance);
		final double newPEWMAStd = adaptableFactor * cache.getLastPEWMAStd() + ((1 - adaptableFactor) * distanceSquare);
		final double newMovingStd = sqrt(newPEWMAStd - pow(newPEWMA, 2));

		return mapToPEWMAEstimation(newPEWMA, newMovingStd);
	}

	private double computeCurrentConfidence(final double estimatedStd, final double actualStd) {
		final double stdDifference = abs(estimatedStd - actualStd);
		return actualStd == 0 ? 1 : 1 - (stdDifference / actualStd);
	}

	private PEWMASamplingParameters getParamsForType(final SamplingModelType type) {
		return samplingModelConfiguration.getModelsProperties().stream()
				.filter(props -> props.getType().equals(type))
				.findFirst()
				.orElseThrow();
	}

	private Map<String, PEWMASamplingCache> initializeCacheMap() {
		final Set<String> resourceCacheNames = Arrays.stream(ResourceType.values())
				.map(resource -> join("_", RESOURCE.name(), resource.name()))
				.collect(toSet());

		return Arrays.stream(SamplingModelType.values())
				.flatMap(type -> type.equals(RESOURCE) ? resourceCacheNames.stream() : Stream.of(type.name()))
				.collect(toMap(type -> type, _ -> new PEWMASamplingCache()));
	}

	private static PEWMAEstimation mapToPEWMAEstimation(final double movingAverage,
			final double movingStandardDeviation) {
		return PEWMAEstimation.builder()
				.movingAverage(movingAverage)
				.movingStd(movingStandardDeviation)
				.build();
	}
}
