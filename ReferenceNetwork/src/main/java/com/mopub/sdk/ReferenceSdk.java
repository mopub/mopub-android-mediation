// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.sdk;

import android.os.Handler;

import androidx.annotation.Nullable;

/**
 * A simple network SDK implementation that works with the reference adapters.
 * <p>
 * INTERNAL USE ONLY. DO NOT REFERENCE OTHERWISE.
 */
public class ReferenceSdk {
    // Simulate a network SDK initialization that does nothing and completes after n milliseconds
    public static void initialize(@Nullable final ReferenceInitializationListener listener) {
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (listener != null) {
                    listener.onInitializationFinished();
                }
            }
        }, 100);
    }

    public interface ReferenceInitializationListener {
        void onInitializationFinished();
    }
}
