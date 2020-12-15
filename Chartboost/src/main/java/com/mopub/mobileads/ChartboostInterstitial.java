package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.chartboost.sdk.Chartboost;
import com.mopub.common.LifecycleListener;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM_WITH_THROWABLE;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_ATTEMPTED;

public class ChartboostInterstitial extends BaseAd {

    private static String ADAPTER_NAME = ChartboostInterstitial.class.getSimpleName();

    @NonNull
    private String mLocation = ChartboostShared.LOCATION_DEFAULT;

    @NonNull
    private ChartboostAdapterConfiguration mChartboostAdapterConfiguration;

    /*
     * Note: Chartboost recommends implementing their specific Activity lifecycle callbacks in your
     * Activity's onStart(), onStop(), onBackPressed() methods for proper results. Please see their
     * documentation for more information.
     */

    public ChartboostInterstitial() {
        mChartboostAdapterConfiguration = new ChartboostAdapterConfiguration();
    }

    @Nullable
    @Override
    protected LifecycleListener getLifecycleListener() {
        return null;
    }

    @NonNull
    public String getAdNetworkId() {
        return mLocation;
    }

    @Override
    protected boolean checkAndInitializeSdk(@NonNull final Activity launcherActivity,
                                            @NonNull final AdData adData) {
        Preconditions.checkNotNull(launcherActivity);
        Preconditions.checkNotNull(adData);

        // Always return true so that the lifecycle listener is registered even if an interstitial
        // did the initialization.
        return true;
    }

    @Override
    protected void load(@NonNull final Context context,
                        @NonNull final AdData adData) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(adData);

        setAutomaticImpressionAndClickTracking(false);

        final Map<String, String> extras = adData.getExtras();
        if (extras.containsKey(ChartboostShared.LOCATION_KEY)) {
            String location = extras.get(ChartboostShared.LOCATION_KEY);
            mLocation = TextUtils.isEmpty(location) ? mLocation : location;
        }

        if (extras != null && !extras.isEmpty()) {
            mChartboostAdapterConfiguration.setCachedInitializationParameters(context, extras);
        }

        try {
            ChartboostAdapterConfiguration.initializeChartboostSdk(context, extras);
        } catch (Exception initializationError) {
            MoPubLog.log(CUSTOM_WITH_THROWABLE, ADAPTER_NAME,
                    "Chartboost initialization called by adapter " + ADAPTER_NAME +
                            " has failed because of an exception", initializationError.getMessage());
        }

        // Chartboost delegation can be set to null on some cases in Chartboost SDK 8.0+.
        // We should set the delegation on each load request to prevent this.
        Chartboost.setDelegate(ChartboostShared.getDelegate());

        // If there's already a listener for this location, we should fail.
        if (ChartboostShared.getDelegate().hasLoadLocation(mLocation) &&
                ChartboostShared.getDelegate().getLoadListener(mLocation) != mLoadListener) {
            mLoadListener.onAdLoadFailed(MoPubErrorCode.NETWORK_NO_FILL);

            MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                    MoPubErrorCode.NETWORK_NO_FILL);
            return;
        }

        try {
            ChartboostShared.getDelegate().registerLoadListener(mLocation, mLoadListener);
        } catch (NullPointerException | IllegalStateException e) {
            mLoadListener.onAdLoadFailed(MoPubErrorCode.NETWORK_NO_FILL);

            MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                    MoPubErrorCode.NETWORK_NO_FILL);
            return;
        }

        if (Chartboost.hasInterstitial(mLocation)) {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME,
                    "Chartboost already has the interstitial ready. Calling didCacheInterstitial.");
            ChartboostShared.getDelegate().didCacheInterstitial(mLocation);
        } else {
            MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);
            Chartboost.cacheInterstitial(mLocation);
        }
    }

    @Override
    protected void show() {
        ChartboostShared.getDelegate().registerInteractionListener(mLocation, mInteractionListener);
        MoPubLog.log(getAdNetworkId(), SHOW_ATTEMPTED, ADAPTER_NAME);
        Chartboost.showInterstitial(mLocation);
    }

    @Override
    protected void onInvalidate() {
        ChartboostShared.getDelegate().unregisterLoadListener(mLocation);
        ChartboostShared.getDelegate().unregisterInteractionListener(mLocation);
    }
}
