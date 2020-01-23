package com.mopub.mobileads;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.chartboost.sdk.Chartboost;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_ATTEMPTED;

class ChartboostInterstitial extends CustomEventInterstitial {

    private static final String ADAPTER_NAME = ChartboostInterstitial.class.getSimpleName();

    @NonNull
    private String mLocation = ChartboostShared.LOCATION_DEFAULT;

    @NonNull
    private ChartboostAdapterConfiguration mChartboostAdapterConfiguration;

    /*
     * Note: Chartboost recommends implementing their specific Activity lifecycle callbacks in your
     * Activity's onStart(), onStop(), onBackPressed() methods for proper results. Please see their
     * documentation for more information.
     */

    /*
     * Abstract methods from CustomEventInterstitial
     */

    public ChartboostInterstitial() {
        mChartboostAdapterConfiguration = new ChartboostAdapterConfiguration();
    }

    @Override
    protected void loadInterstitial(@NonNull Context context,
                                    @NonNull CustomEventInterstitialListener interstitialListener,
                                    @NonNull Map<String, Object> localExtras, @NonNull Map<String, String> serverExtras) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(interstitialListener);
        Preconditions.checkNotNull(localExtras);
        Preconditions.checkNotNull(serverExtras);

        if (serverExtras.containsKey(ChartboostShared.LOCATION_KEY)) {
            String location = serverExtras.get(ChartboostShared.LOCATION_KEY);
            mLocation = TextUtils.isEmpty(location) ? mLocation : location;
        }

        // If there's already a listener for this location, then another instance of
        // CustomEventInterstitial is still active and we should fail.
        if (ChartboostShared.getDelegate().hasInterstitialLocation(mLocation) &&
                ChartboostShared.getDelegate().getInterstitialListener(mLocation) != interstitialListener) {
            interstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);

            MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                    MoPubErrorCode.NETWORK_NO_FILL);
            return;
        }

        try {
            ChartboostShared.initializeSdk(context, serverExtras);
            ChartboostShared.getDelegate().registerInterstitialListener(mLocation, interstitialListener);

            mChartboostAdapterConfiguration.setCachedInitializationParameters(context, serverExtras);
        } catch (NullPointerException e) {
            interstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);

            MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                    MoPubErrorCode.NETWORK_NO_FILL);
            return;
        } catch (IllegalStateException e) {
            interstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);

            MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                    MoPubErrorCode.NETWORK_NO_FILL);
            return;
        }

        if (Chartboost.hasInterstitial(mLocation)) {
            ChartboostShared.getDelegate().didCacheInterstitial(mLocation);
        } else {
            Chartboost.cacheInterstitial(mLocation);
            MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);
        }
    }

    @Override
    protected void showInterstitial() {
        MoPubLog.log(getAdNetworkId(), SHOW_ATTEMPTED, ADAPTER_NAME);
        Chartboost.showInterstitial(mLocation);
    }

    @Override
    protected void onInvalidate() {
        ChartboostShared.getDelegate().unregisterInterstitialListener(mLocation);
    }

    private String getAdNetworkId() {
        return mLocation;
    }
}
