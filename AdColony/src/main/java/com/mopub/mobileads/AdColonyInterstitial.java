package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.mopub.common.logging.MoPubLog;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;

import com.adcolony.sdk.AdColony;
import com.adcolony.sdk.AdColonyAppOptions;
import com.adcolony.sdk.AdColonyInterstitialListener;
import com.adcolony.sdk.AdColonyZone;
import com.mopub.common.MoPub;
import com.mopub.common.privacy.ConsentStatus;
import com.mopub.common.privacy.PersonalInfoManager;
import com.mopub.common.util.Json;

import java.util.Arrays;
import java.util.Map;

public class AdColonyInterstitial extends CustomEventInterstitial {

    public static final String ADAPTER_NAME = AdColonyInterstitial.class.getSimpleName();

    private CustomEventInterstitialListener mCustomEventInterstitialListener;
    private AdColonyInterstitialListener mAdColonyInterstitialListener;
    private final Handler mHandler;
    private com.adcolony.sdk.AdColonyInterstitial mAdColonyInterstitial;

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
            customEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME, MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR.getIntCode(), MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            return;
        }

        String clientOptions = AdColonyUtils.DEFAULT_CLIENT_OPTIONS;
        String appId = AdColonyUtils.DEFAULT_APP_ID;
        String[] allZoneIds = AdColonyUtils.DEFAULT_ALL_ZONE_IDS;
        String zoneId = AdColonyUtils.DEFAULT_ZONE_ID;

        mCustomEventInterstitialListener = customEventInterstitialListener;

        if (AdColonyUtils.extrasAreValid(serverExtras)) {
            clientOptions = serverExtras.get(AdColonyUtils.CLIENT_OPTIONS_KEY);
            appId = serverExtras.get(AdColonyUtils.APP_ID_KEY);
            allZoneIds = AdColonyUtils.extractAllZoneIds(serverExtras);
            zoneId = serverExtras.get(AdColonyUtils.ZONE_ID_KEY);
            mAdColonyAdapterConfiguration.setCachedInitializationParameters(context, serverExtras);
        }

        // Check to see if app ID parameter is present. If not AdColony will not return an ad.
        // So there's no need to make a request. If so, must fail and log the flow.
        if (TextUtils.isEmpty(appId) || TextUtils.equals(appId, AdColonyUtils.DEFAULT_APP_ID)) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "AppId parameter cannot be empty. " +
                    "Please make sure you enter correct AppId on the MoPub Dashboard " +
                    "for AdColony.");

            customEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            return;
        }

        mAdColonyInterstitialListener = getAdColonyInterstitialListener();

        AdColonyUtils.checkAndConfigureAdColony(context, clientOptions, appId, allZoneIds);

        if (!TextUtils.isEmpty(zoneId)) {
            AdColony.requestInterstitial(zoneId, mAdColonyInterstitialListener);
            MoPubLog.log(zoneId, LOAD_ATTEMPTED, ADAPTER_NAME);
        }
    }

    @Override
    protected void showInterstitial() {
        MoPubLog.log(SHOW_ATTEMPTED, ADAPTER_NAME);
        if (mAdColonyInterstitial == null || mAdColonyInterstitial.isExpired()) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCustomEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
                    MoPubLog.log(SHOW_FAILED, ADAPTER_NAME, MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
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
            mAdColonyInterstitialListener = null;
            mAdColonyInterstitial.destroy();
            mAdColonyInterstitial = null;
        }
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
                            MoPubLog.log(LOAD_SUCCESS, ADAPTER_NAME);
                        }
                    });
                }

                @Override
                public void onRequestNotFilled(@NonNull AdColonyZone zone) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mCustomEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
                            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME, MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                                    MoPubErrorCode.NETWORK_NO_FILL);
                        }
                    });
                }

                @Override
                public void onClosed(@NonNull com.adcolony.sdk.AdColonyInterstitial ad) {
                    MoPubLog.log(CUSTOM, ADAPTER_NAME, "AdColony interstitial ad has been dismissed");
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
                            MoPubLog.log(SHOW_SUCCESS, ADAPTER_NAME);
                        }
                    });
                }

                @Override
                public void onExpiring(@NonNull com.adcolony.sdk.AdColonyInterstitial ad) {
                    MoPubLog.log(CUSTOM, ADAPTER_NAME, "AdColony interstitial is expiring; requesting new ad" + ad.getZoneID());
                    AdColony.requestInterstitial(ad.getZoneID(), mAdColonyInterstitialListener);
                }

                @Override
                public void onClicked(@NonNull com.adcolony.sdk.AdColonyInterstitial ad) {
                    mCustomEventInterstitialListener.onInterstitialClicked();
                    MoPubLog.log(CLICKED, ADAPTER_NAME);
                }
            };
        }
    }
}
