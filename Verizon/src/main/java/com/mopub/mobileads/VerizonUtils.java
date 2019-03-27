package com.mopub.mobileads;

import android.os.Handler;
import android.os.Looper;

public final class VerizonUtils {

    private static final Handler handler = new Handler(Looper.getMainLooper());

    // VASAds error codes
    private static final int ERROR_NO_FILL = -1;
    private static final int ERROR_AD_REQUEST_TIMED_OUT = -2;
    private static final int ERROR_AD_REQUEST_FAILED = -3;

    public static void postOnUiThread(final Runnable runnable) {
        handler.post(runnable);
    }

    static MoPubErrorCode convertErrorCodeToMoPub(final int vasErrorCode) {
        switch (vasErrorCode) {
            case ERROR_NO_FILL:
                return MoPubErrorCode.NETWORK_NO_FILL;
            case ERROR_AD_REQUEST_TIMED_OUT:
                return MoPubErrorCode.NETWORK_TIMEOUT;
            case ERROR_AD_REQUEST_FAILED:
            default:
                return MoPubErrorCode.UNSPECIFIED;
        }
    }
}
