package com.mopub.mobileads;

import android.os.Handler;
import android.os.Looper;

import com.mopub.nativeads.NativeErrorCode;
import com.verizon.ads.ErrorInfo;

import static com.verizon.ads.VASAds.ERROR_AD_REQUEST_FAILED;
import static com.verizon.ads.VASAds.ERROR_AD_REQUEST_TIMED_OUT;
import static com.verizon.ads.VASAds.ERROR_NO_FILL;

public final class VerizonUtils {

    private static final Handler handler = new Handler(Looper.getMainLooper());

    public static void postOnUiThread(final Runnable runnable) {
        handler.post(runnable);
    }

    static MoPubErrorCode convertErrorInfoToMoPub(final ErrorInfo errorInfo) {
        if (errorInfo == null) {
            return MoPubErrorCode.UNSPECIFIED;
        }

        switch (errorInfo.getErrorCode()) {
            case ERROR_NO_FILL:
                return MoPubErrorCode.NETWORK_NO_FILL;
            case ERROR_AD_REQUEST_TIMED_OUT:
                return MoPubErrorCode.NETWORK_TIMEOUT;
            case ERROR_AD_REQUEST_FAILED:
            default:
                return MoPubErrorCode.NETWORK_INVALID_STATE;
        }
    }

    public static NativeErrorCode convertErrorInfoToMoPubNative(final ErrorInfo errorInfo) {
        if (errorInfo == null) {
            return NativeErrorCode.UNSPECIFIED;
        }

        switch (errorInfo.getErrorCode()) {
            case ERROR_NO_FILL:
                return NativeErrorCode.NETWORK_NO_FILL;
            case ERROR_AD_REQUEST_TIMED_OUT:
                return NativeErrorCode.NETWORK_TIMEOUT;
            case ERROR_AD_REQUEST_FAILED:
            default:
                return NativeErrorCode.NETWORK_INVALID_STATE;
        }
    }
}
