// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.LifecycleListener;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.mopub.sdk.ReferenceInlineAd;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;

/**
 * This is a reference adapter implementation designed for testing the MoPub mediation protocol ONLY.
 * For that purpose, it leverages a simple network SDK implementation and does not constitute any real-world
 * testing as far as a live ad experience goes.
 * <p>
 * Future MoPub adapter implementations may refer to the implementation here for best practices.
 */
public class ReferenceInlineAdapter extends BaseAd
        implements ReferenceInlineAd.ReferenceInlineAdListener {

    // Declare any global constants/keys needed by the adapter here for readability. Ensure you
    // are alphabetizing and grouping your constants based on names and visibility identifiers.
    private static final String ADAPTER_NAME = ReferenceInlineAdapter.class.getSimpleName();
    private static final String AD_UNIT_ID_KEY = "adUnitId";

    @Nullable
    private ReferenceInlineAd mInlineAd;
    private String mAdUnitId;

    @NonNull
    private final ReferenceAdapterConfiguration mReferenceAdapterConfiguration;

    public ReferenceInlineAdapter() {
        mReferenceAdapterConfiguration = new ReferenceAdapterConfiguration();
    }

    /**
     * When the MoPub SDK receives a response indicating it should load a base ad, it will send
     * this message to your base ad class. Your implementation of this method can either load
     * an interstitial ad from a third-party ad network, or execute any application code.
     * It must also notify the provided FullscreenAdListener of certain lifecycle
     * events.
     * <p>
     * The adData parameter is an object containing additional data required by the subclass as well
     * as data configurable on the MoPub website that you want to associate with a given base
     * ad request. This data may be used to pass dynamic information, such as publisher IDs, without
     * changes in application code.
     * <p/>
     * Implementers should also use this method (or checkAndInitializeSdk)
     * to register a listener for their SDK.
     * <p/>
     * This method should not call any MoPubRewardedAdManager event methods directly
     * (onAdLoadSuccess, etc). Instead the SDK delegate/listener should call these methods.
     *
     * @param context a context from the calling application.
     * @param adData  a collection of ad data.
     */
    @Override
    protected void load(@NonNull final Context context, @NonNull final AdData adData) {
        // It is recommended to fail fast if required parameters are invalid
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(adData);

        /*
         * Set auto impression and click tracking to true if the adapter is to let the MoPub SDK
         * decide when to track impression and click for each ad. This is the default behavior if
         * not set.
         *
         * Set auto impression and click tracking to false if the adapter is to rely on callbacks
         * from the network SDK for those events. When the callback is fired, the adapter is to
         * call the equivalent MoPub tracking callbacks for that event.
         */
        setAutomaticImpressionAndClickTracking(false);

        /*
         * AdData contains network-specific data that you have entered on the MoPub dashboard.
         * Parse for the data needed by the network SDK to request ads and handle the ad experience.
         */
        final Map<String, String> extras = adData.getExtras();

        if (!extras.isEmpty()) {
            mAdUnitId = extras.get(AD_UNIT_ID_KEY);

            mReferenceAdapterConfiguration.setCachedInitializationParameters(context, extras);
        } else {
            failAdRequest("Reference inline ad failed to load because no data is provided " +
                    "for the ad request. Make sure your ad unit setup on the MoPub dashboard is " +
                    "correct.");

            return;
        }

        // Request ad
        mInlineAd = new ReferenceInlineAd(context, this);
        mInlineAd.loadAd();

        MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);
    }

    /**
     * Provides the {@link View} of the base ad's ad network. This is required for Inline ads to
     * show correctly, but is otherwise optional.
     *
     * @return a View. Default implementation returns null.
     */
    @Override
    public View getAdView() {
        if (mInlineAd != null) {
            return mInlineAd.getAdView();
        }

        return null;
    }

    /*
     * Called when a BaseAd is being invalidated or destroyed. Perform any final cleanup here.
     */
    @Override
    protected void onInvalidate() {
        if (mInlineAd != null) {
            mInlineAd.destroy();
            mInlineAd = null;
        }
    }

    /**
     * Provides a {@link LifecycleListener} if the base ad's ad network wishes to be notified of
     * activity lifecycle events in the application.
     *
     * @return a LifecycleListener. May be null.
     */
    @Nullable
    @Override
    protected LifecycleListener getLifecycleListener() {
        return null;
    }

    /**
     * Called by the MoPubRewardedAdManager after loading the base ad.
     * This should return the "ad unit id", "zone id" or similar identifier for the network.
     * May be empty if the network does not have anything more specific than an application ID.
     *
     * @return the id string for this ad unit with the ad network.
     */
    @NonNull
    public String getAdNetworkId() {
        return mAdUnitId == null ? "" : mAdUnitId;
    }

    /**
     * Sets up the 3rd party ads SDK if it needs configuration. Extenders should use this
     * to do any static initialization the first time this method is run by any class instance.
     * From then on, the SDK should be reused without initialization.
     *
     * @return true if the SDK performed initialization, false if the SDK was already initialized.
     */
    @Override
    protected boolean checkAndInitializeSdk(@NonNull Activity launcherActivity, @NonNull AdData adData) {
        // It is recommended to fail fast if required parameters are invalid
        Preconditions.checkNotNull(launcherActivity);
        Preconditions.checkNotNull(adData);

        return false;
    }

    // Override the network SDK's loaded callback to signal the ad has loaded
    @Override
    public void onAdLoaded() {
        MoPubLog.log(getAdNetworkId(), LOAD_SUCCESS, ADAPTER_NAME);

        if (mLoadListener != null) {
            mLoadListener.onAdLoaded();
        }
    }

    // Override the network SDK's impression failure callback to signal the ad has registered an impression
    @Override
    public void onAdImpression() {
        MoPubLog.log(getAdNetworkId(), SHOW_SUCCESS, ADAPTER_NAME);

        if (mInteractionListener != null) {
            mInteractionListener.onAdImpression();
        }
    }

    // Override the network SDK's load failure callback to signal the ad has failed to load
    @Override
    public void onAdFailedToLoad() {
        failAdRequest("Reference inline ad failed to load.");
    }

    // Override the network SDK's clicked callback to signal the ad has been clicked
    @Override
    public void onAdClicked() {
        MoPubLog.log(getAdNetworkId(), CLICKED, ADAPTER_NAME);

        if (mInteractionListener != null) {
            mInteractionListener.onAdClicked();
        }
    }

    private void failAdRequest(String errorMsg) {
        MoPubLog.log(CUSTOM, ADAPTER_NAME, errorMsg);
        MoPubLog.log(LOAD_FAILED, ADAPTER_NAME, MoPubErrorCode.NO_FILL.getIntCode(),
                MoPubErrorCode.NO_FILL);

        // Use the appropriate listener depending on whether the ad fails to load or to show.
        // Use mLoadListener for load-related events. Use mInteractionListener for show-related events.
        if (mInteractionListener == null && mLoadListener != null) {
            mLoadListener.onAdLoadFailed(MoPubErrorCode.NO_FILL);
        } else if (mInteractionListener != null) {
            mInteractionListener.onAdFailed(MoPubErrorCode.NO_FILL);
        }
    }
}
