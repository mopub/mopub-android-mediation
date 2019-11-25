// Copyright 2018-2019 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads.testing;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.logging.MoPubLog;
import com.mopub.common.logging.MoPubLogger;
import com.mopub.mobileads.MoPubErrorCode;

import static com.mopub.common.logging.MoPubLog.SdkLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.LogLevel.INFO;

/**
 * Used to intercept logs so that we can view logs at a lower level
 * than Verbose (ie. Level.FINEST). This will show a toast when we
 * receive a matching error from the mopub sdk.
 */
public class LoggingUtils {
    private LoggingUtils() {
    }

    private static boolean sEnabled;

    /**
     * Makes it so that this app can intercept Level.FINEST log messages.
     * This is not thread safe.
     *
     * @param context Needs a context to send toasts.
     */
    static void enableCanaryLogging(@NonNull final Context context) {
        if (sEnabled) {
            return;
        }

        MoPubLog.addLogger(new MoPubLogger() {
            @Override
            public void log(@Nullable String className, @Nullable String methodName,
                            @Nullable String identifier, @Nullable String message) {
                if (MoPubErrorCode.WARMUP.toString().equals(message)) {
                    Utils.logToast(context, MoPubErrorCode.WARMUP.toString());
                }
                // Toasts the no connection message if a native ad failed due to no internet
                if (MoPubErrorCode.NO_CONNECTION.toString().equals(message)) {
                    Utils.logToast(context, MoPubErrorCode.NO_CONNECTION.toString());
                }
            }
        }, INFO);

        MoPubLog.log(CUSTOM, "Setting up MoPubLog");

        sEnabled = true;
    }
}

