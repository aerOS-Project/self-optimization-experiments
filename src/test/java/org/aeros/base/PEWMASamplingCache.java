package org.aeros.base;

import static java.lang.Math.pow;
import static java.util.Objects.isNull;

import org.springframework.stereotype.Service;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Service
public class PEWMASamplingCache {

	private Double lastSampleDistance;
	private Double lastMovingStandardDeviation;
	private Long lastSamplingPeriod;
	private Double lastSampleValue;

	public void setCacheValues(final long samplingPeriod,
			final double samplingDistance,
			final Double sampleValue,
			final double movingAverage) {
		lastSampleDistance = samplingDistance;
		lastSampleValue = sampleValue;
		lastMovingStandardDeviation = movingAverage;
		lastSamplingPeriod = samplingPeriod;
	}

	public boolean isEmpty() {
		return isNull(lastSamplingPeriod);
	}

	public double getLastPEWMAStd() {
		return pow(lastMovingStandardDeviation, 2) + pow(lastSampleDistance, 2);
	}
}
