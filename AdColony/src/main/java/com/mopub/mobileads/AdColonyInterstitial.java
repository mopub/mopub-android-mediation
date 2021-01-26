package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.adcolony.sdk.AdColony;
import com.adcolony.sdk.AdColonyAdOptions;
import com.adcolony.sdk.AdColonyInterstitialListener;
import com.adcolony.sdk.AdColonyZone;
import com.mopub.common.LifecycleListener;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.Json;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;

public class AdColonyInterstitial extends BaseAd {

    private static final String ADAPTER_NAME = AdColonyInterstitial.class.getSimpleName();

    private AdColonyInterstitialListener mAdColonyInterstitialListener;
    private final Handler mHandler;
    private com.adcolony.sdk.AdColonyInterstitial mAdColonyInterstitial;

    @NonNull
    private String mZoneId = AdColonyAdapterConfiguration.DEFAULT_ZONE_ID;

    @NonNull
    public String getAdNetworkId() {
        return mZoneId;
    }

    @Override
    protected boolean checkAndInitializeSdk(@NonNull Activity launcherActivity, @NonNull AdData adData) throws Exception {
        return false;
    }

    @NonNull
    private AdColonyAdapterConfiguration mAdColonyAdapterConfiguration;

    public AdColonyInterstitial() {
        mHandler = new Handler(Looper.getMainLooper());
        mAdColonyAdapterConfiguration = new AdColonyAdapterConfiguration();
    }

    @Override
    protected void load(@NonNull final Context context,
                        @NonNull final AdData adData) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(adData);

        setAutomaticImpressionAndClickTracking(false);

        if (!(context instanceof Activity)) {
            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME, MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR.getIntCode(), MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            if (mLoadListener != null) {
                mLoadListener.onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            }
            return;
        }

        String clientOptions = adData.getExtras().get(AdColonyAdapterConfiguration.CLIENT_OPTIONS_KEY);
        if (clientOptions == null)
            clientOptions = "";

        // Set mandatory parameters
        final Map<String, String> extras = adData.getExtras();
        final String appId = AdColonyAdapterConfiguration.getAdColonyParameter(AdColonyAdapterConfiguration.APP_ID_KEY, extras);
        final String zoneId = AdColonyAdapterConfiguration.getAdColonyParameter(AdColonyAdapterConfiguration.ZONE_ID_KEY, extras);

        String[] allZoneIds;
        String allZoneIdsString = AdColonyAdapterConfiguration.getAdColonyParameter(AdColonyAdapterConfiguration.ALL_ZONE_IDS_KEY, extras);
        if (allZoneIdsString != null) {
            allZoneIds = Json.jsonArrayToStringArray(allZoneIdsString);
        } else {
            allZoneIds = null;
        }

        // Check if mandatory parameters are valid, abort otherwise
        if (appId == null) {
            abortRequestForIncorrectParameter(AdColonyAdapterConfiguration.APP_ID_KEY);
            return;
        }

        if (zoneId == null || allZoneIds == null || allZoneIds.length == 0) {
            abortRequestForIncorrectParameter(AdColonyAdapterConfiguration.ZONE_ID_KEY);
            return;
        }

        mZoneId = zoneId;

        final AdColonyAdOptions mAdColonyAdOptions = mAdColonyAdapterConfiguration.getInterstitialAdOptionsFromExtras(extras);

        mAdColonyAdapterConfiguration.setCachedInitializationParameters(context, extras);
        mAdColonyInterstitialListener = getAdColonyInterstitialListener(mAdColonyAdOptions);

        AdColonyAdapterConfiguration.checkAndConfigureAdColonyIfNecessary(context, clientOptions, appId, allZoneIds);
        AdColony.requestInterstitial(mZoneId, mAdColonyInterstitialListener, mAdColonyAdOptions);
        MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);
    }

    private void abortRequestForIncorrectParameter(String parameterName) {
        AdColonyAdapterConfiguration.logAndFail("interstitial request", parameterName);
        if (mInteractionListener != null) {
            mInteractionListener.onAdFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
        }
    }

    @Override
    protected void show() {
        MoPubLog.log(getAdNetworkId(), SHOW_ATTEMPTED, ADAPTER_NAME);
        if (mAdColonyInterstitial == null || mAdColonyInterstitial.isExpired()) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mInteractionListener != null) {
                        mInteractionListener.onAdFailed(MoPubErrorCode.NETWORK_NO_FILL);
                    }
                    MoPubLog.log(getAdNetworkId(), SHOW_FAILED, ADAPTER_NAME, MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                            MoPubErrorCode.NETWORK_NO_FILL);
                }
            });
        } else {
            mAdColonyInterstitial.show();
        }
    }

    @Override
    protected void onInvalidate() {
        if (mAdColonyInterstitial != null) {
            mAdColonyInterstitial.destroy();
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "AdColony interstitial destroyed");
            mAdColonyInterstitial = null;
        }
        mAdColonyInterstitialListener = null;
    }

    @Nullable
    @Override
    protected LifecycleListener getLifecycleListener() {
        return null;
    }

    private AdColonyInterstitialListener getAdColonyInterstitialListener(final AdColonyAdOptions mAdColonyAdOptions) {
        if (mAdColonyInterstitialListener != null) {
            return mAdColonyInterstitialListener;
        } else {
            return new AdColonyInterstitialListener() {
                @Override
                public void onRequestFilled(@NonNull com.adcolony.sdk.AdColonyInterstitial adColonyInterstitial) {
                    mAdColonyInterstitial = adColonyInterstitial;
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mLoadListener != null) {
                                mLoadListener.onAdLoaded();
                            }
                            MoPubLog.log(getAdNetworkId(), LOAD_SUCCESS, ADAPTER_NAME);
                        }
                    });
                }

                @Override
                public void onRequestNotFilled(@NonNull AdColonyZone zone) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mLoadListener != null) {
                                mLoadListener.onAdLoadFailed(MoPubErrorCode.NETWORK_NO_FILL);
                            }
                            MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME, MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                                    MoPubErrorCode.NETWORK_NO_FILL);
                        }
                    });
                }

                @Override
                public void onClosed(@NonNull com.adcolony.sdk.AdColonyInterstitial ad) {
                    MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "AdColony interstitial ad has been dismissed");
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mInteractionListener != null) {
                                mInteractionListener.onAdDismissed();
                            }
                        }
                    });
                }

                @Override
                public void onOpened(@NonNull com.adcolony.sdk.AdColonyInterstitial ad) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mInteractionListener != null) {
                                mInteractionListener.onAdShown();
                                mInteractionListener.onAdImpression();
                            }
                            MoPubLog.log(getAdNetworkId(), SHOW_SUCCESS, ADAPTER_NAME);
                        }
                    });
                }

                @Override
                public void onExpiring(@NonNull com.adcolony.sdk.AdColonyInterstitial ad) {
                    MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "AdColony interstitial is expiring; requesting new ad.");
                    Preconditions.checkNotNull(ad);

                    if (mAdColonyInterstitialListener != null) {
                        AdColony.requestInterstitial(ad.getZoneID(), mAdColonyInterstitialListener, mAdColonyAdOptions);
                    }
                }

                @Override
                public void onClicked(@NonNull com.adcolony.sdk.AdColonyInterstitial ad) {
                    if (mInteractionListener != null) {
                        mInteractionListener.onAdClicked();
                    }
                    MoPubLog.log(getAdNetworkId(), CLICKED, ADAPTER_NAME);
                }
            };
        }
    }
}
