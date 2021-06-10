// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.BaseAdapterConfiguration;
import com.mopub.common.OnNetworkInitializationFinishedListener;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.referencenetwork.BuildConfig;
import com.mopub.sdk.ReferenceSdk;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM_WITH_THROWABLE;

/**
 * This is a reference adapter implementation designed for testing the MoPub mediation protocol ONLY.
 * For that purpose, it leverages a simple network SDK implementation and does not constitute any real-world
 * testing as far as a live ad experience goes.
 * <p>
 * Future MoPub adapter implementations may refer to the implementation here for best practices.
 */
public class ReferenceAdapterConfiguration extends BaseAdapterConfiguration {

    // Declare any global constants/keys needed by the adapter here for readability. Ensure you
    // are alphabetizing and grouping your constants based on names and visibility identifiers.
    private static final String ADAPTER_NAME = ReferenceAdapterConfiguration.class.getSimpleName();
    private static final String ADAPTER_VERSION = BuildConfig.VERSION_NAME;
    private static final String MOPUB_NETWORK_NAME = BuildConfig.NETWORK_NAME;

    /**
     * Gets the adapter version.
     *
     * @return String representing the adapter version.
     */
    @NonNull
    @Override
    public String getAdapterVersion() {
        return ADAPTER_VERSION;
    }

    /**
     * If this adapter has advanced bidding enabled, return an advanced bidding token. Otherwise,
     * it's okay to return null.
     *
     * @param context Context to reach Android resources.
     * @return String representing an advanced bidding token.
     */
    @Nullable
    @Override
    public String getBiddingToken(@NonNull final Context context) {
        Preconditions.checkNotNull(context);

        // Retrieval of the Advanced Bidding token should be synchronous.
        return "good_token";
    }

    /**
     * The MoPub-internal name for this particular adapter.
     *
     * @return String representing the MoPub network name.
     */
    @NonNull
    @Override
    public String getMoPubNetworkName() {
        return MOPUB_NETWORK_NAME;
    }

    // If the network SDK doesn't have an API to query its version, this method may extract
    // a version from the adapter version string by dropping the last digit. For example, if the
    // adapter version is 1.0.0.0, the network SDK version is 1.0.0.
    @NonNull
    @Override
    public String getNetworkSdkVersion() {
        final String adapterVersion = getAdapterVersion();

        return (!TextUtils.isEmpty(adapterVersion)) ?
                adapterVersion.substring(0, adapterVersion.lastIndexOf('.')) : "";
    }

    /**
     * Initialize the network SDK. Called when the MoPub SDK initializes, typically once per app session.
     * Ensure you notify MoPub of onNetworkInitializationFinished().
     *
     * @param context       Context to init with.
     * @param configuration Map of network initialization parameters.
     * @param listener      Callback for the SDK to continue initialization.
     */
    @Override
    public void initializeNetwork(@NonNull final Context context,
                                  @Nullable final Map<String, String> configuration,
                                  @NonNull final OnNetworkInitializationFinishedListener listener) {

        // It is recommended to fail fast if required parameters are invalid
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(listener);

        // Initialize the network SDK in a fire-and-forget manner. The operation should not block
        // the thread and ideally should complete in 100ms. Any completion callback here is not
        // exposed to external implementations.
        synchronized (ReferenceAdapterConfiguration.class) {
            try {
                ReferenceSdk.initialize(new ReferenceSdk.ReferenceInitializationListener() {
                    @Override
                    public void onInitializationFinished() {
                        MoPubLog.log(CUSTOM, ADAPTER_NAME, "Reference Network initialized.");

                        // Always invoke onNetworkInitializationFinished() even if initialization is
                        // not attempted or doesn't complete. The adapter may pass in a success or
                        // a failure error code to indicate the result.
                        listener.onNetworkInitializationFinished(ReferenceAdapterConfiguration.class,
                                MoPubErrorCode.ADAPTER_INITIALIZATION_SUCCESS);
                    }
                });
            } catch (Exception exception) {
                MoPubLog.log(CUSTOM_WITH_THROWABLE, ADAPTER_NAME, "Initializing the Reference " +
                        "Network has encountered an exception.", exception);

                listener.onNetworkInitializationFinished(ReferenceAdapterConfiguration.class,
                        MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            }
        }
    }
}
