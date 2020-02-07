package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.adcolony.sdk.AdColony;
import com.adcolony.sdk.AdColonyInterstitialListener;
import com.adcolony.sdk.AdColonyZone;
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

public class AdColonyInterstitial extends CustomEventInterstitial {

    private static final String ADAPTER_NAME = AdColonyInterstitial.class.getSimpleName();

    private CustomEventInterstitialListener mCustomEventInterstitialListener;
    private AdColonyInterstitialListener mAdColonyInterstitialListener;
    private final Handler mHandler;
    private com.adcolony.sdk.AdColonyInterstitial mAdColonyInterstitial;

    @NonNull
    private String mZoneId = AdColonyAdapterConfiguration.DEFAULT_ZONE_ID;

    @NonNull
    public String getAdNetworkId() {
        return mZoneId;
    }

    @NonNull
    private AdColonyAdapterConfiguration mAdColonyAdapterConfiguration;

    public AdColonyInterstitial() {
        mHandler = new Handler();
        mAdColonyAdapterConfiguration = new AdColonyAdapterConfiguration();
    }

    @Override
    protected void loadInterstitial(@NonNull Context context,
                                    @NonNull CustomEventInterstitialListener customEventInterstitialListener,
                                    @Nullable Map<String, Object> localExtras,
                                    @NonNull Map<String, String> serverExtras) {
        if (!(context instanceof Activity)) {
            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME, MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR.getIntCode(), MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            customEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            return;
        }

        String clientOptions = serverExtras.get(AdColonyAdapterConfiguration.CLIENT_OPTIONS_KEY);
        if (clientOptions == null)
            clientOptions = "";

        mCustomEventInterstitialListener = customEventInterstitialListener;


        // Set mandatory parameters
        final String appId = AdColonyAdapterConfiguration.getAdColonyParameter(AdColonyAdapterConfiguration.APP_ID_KEY, serverExtras);
        final String zoneId = AdColonyAdapterConfiguration.getAdColonyParameter(AdColonyAdapterConfiguration.ZONE_ID_KEY, serverExtras);

        String[] allZoneIds;
        String allZoneIdsString = AdColonyAdapterConfiguration.getAdColonyParameter(AdColonyAdapterConfiguration.ALL_ZONE_IDS_KEY, serverExtras);
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

        mAdColonyAdapterConfiguration.setCachedInitializationParameters(context, serverExtras);
        mAdColonyInterstitialListener = getAdColonyInterstitialListener();

        AdColonyAdapterConfiguration.checkAndConfigureAdColonyIfNecessary(context, clientOptions, appId, allZoneIds);
        AdColony.requestInterstitial(mZoneId, mAdColonyInterstitialListener);
        MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);
    }

    private void abortRequestForIncorrectParameter(String parameterName) {
        AdColonyAdapterConfiguration.logAndFail("interstitial request", parameterName);
        mCustomEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
    }

    @Override
    protected void showInterstitial() {
        MoPubLog.log(getAdNetworkId(), SHOW_ATTEMPTED, ADAPTER_NAME);
        if (mAdColonyInterstitial == null || mAdColonyInterstitial.isExpired()) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCustomEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
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

    private AdColonyInterstitialListener getAdColonyInterstitialListener() {
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
                            mCustomEventInterstitialListener.onInterstitialLoaded();
                            MoPubLog.log(getAdNetworkId(), LOAD_SUCCESS, ADAPTER_NAME);
                        }
                    });
                }

                @Override
                public void onRequestNotFilled(@NonNull AdColonyZone zone) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mCustomEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
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
                            mCustomEventInterstitialListener.onInterstitialDismissed();
                        }
                    });
                }

                @Override
                public void onOpened(@NonNull com.adcolony.sdk.AdColonyInterstitial ad) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mCustomEventInterstitialListener.onInterstitialShown();
                            MoPubLog.log(getAdNetworkId(), SHOW_SUCCESS, ADAPTER_NAME);
                        }
                    });
                }

                @Override
                public void onExpiring(@NonNull com.adcolony.sdk.AdColonyInterstitial ad) {
                    MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "AdColony interstitial is expiring; requesting new ad.");
                    AdColony.requestInterstitial(ad.getZoneID(), mAdColonyInterstitialListener);
                }

                @Override
                public void onClicked(@NonNull com.adcolony.sdk.AdColonyInterstitial ad) {
                    mCustomEventInterstitialListener.onInterstitialClicked();
                    MoPubLog.log(getAdNetworkId(), CLICKED, ADAPTER_NAME);
                }
            };
        }
    }
}
