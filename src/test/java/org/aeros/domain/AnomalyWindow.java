package org.aeros.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Attributes of a single anomaly window (i.e. timeframe within which the anomaly is supposed to be detected).
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AnomalyWindow {

	private int startIdx;
	private int endIdx;

	/**
	 * Method evaluates if a given anomaly is within window.
	 *
	 * @param anomalyIdx index of anomaly
	 * @return information if anomaly is within window
	 */
	public boolean isWithinWindow(final int anomalyIdx) {
		return anomalyIdx >= startIdx && anomalyIdx <= endIdx;
	}

	/**
	 * Method evaluates if a given anomaly is after window.
	 *
	 * @param anomalyIdx index of anomaly
	 * @return information if anomaly is within window
	 */
	public boolean isAfterWindow(final int anomalyIdx) {
		return anomalyIdx > endIdx;
	}

	/**
	 * @return size of the window
	 */
	public int getWindowSize() {
		return endIdx - startIdx;
	}
}
