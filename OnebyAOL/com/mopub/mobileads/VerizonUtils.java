package com.mopub.mobileads;

import android.os.Handler;
import android.os.Looper;


/**
 * Compatible with version 1.0.1 of the Verizon Ads SDK StandardEdition.
 */

final public class VerizonUtils {

	private static final Handler handler = new Handler(Looper.getMainLooper());

	// VASAds error codes
	private static final int ERROR_NO_FILL = -1;
	private static final int ERROR_AD_REQUEST_TIMED_OUT = -2;
	private static final int ERROR_AD_REQUEST_FAILED = -3;


	public static void postOnUiThread(final Runnable runnable) {

		handler.post(runnable);
	}


	static boolean isEmpty(final String s) {

		return (s == null || s.trim().isEmpty());
	}


	static MoPubErrorCode convertErrorCodeToMoPub(final int vasErrorCode) {

		MoPubErrorCode errorCode;
		switch (vasErrorCode) {
			case ERROR_NO_FILL:
				errorCode = MoPubErrorCode.NETWORK_NO_FILL;
				break;
			case ERROR_AD_REQUEST_TIMED_OUT:
				errorCode = MoPubErrorCode.NETWORK_TIMEOUT;
				break;
			case ERROR_AD_REQUEST_FAILED:
			default:
				errorCode = MoPubErrorCode.UNSPECIFIED;

		}
		return errorCode;
	}
}
