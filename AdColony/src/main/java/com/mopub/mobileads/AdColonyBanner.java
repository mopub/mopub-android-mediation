package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.adcolony.sdk.AdColony;
import com.adcolony.sdk.AdColonyAdSize;
import com.adcolony.sdk.AdColonyAdView;
import com.adcolony.sdk.AdColonyAdViewListener;
import com.adcolony.sdk.AdColonyAppOptions;
import com.adcolony.sdk.AdColonyZone;
import com.mopub.common.DataKeys;
import com.mopub.common.MoPub;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.privacy.ConsentStatus;
import com.mopub.common.privacy.PersonalInfoManager;
import com.mopub.common.util.Json;

import java.util.Arrays;
import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.WILL_LEAVE_APPLICATION;

public class AdColonyBanner extends CustomEventBanner {


    public static final String ADAPTER_NAME = AdColonyBanner.class.getSimpleName();

    private CustomEventBannerListener mCustomEventBannerListener;
    private AdColonyAdViewListener mAdColonyBannerListener;
    private final Handler mHandler;

    @NonNull
    private AdColonyAdapterConfiguration mAdColonyAdapterConfiguration;

    private AdColonyAdSize adSize;

    private AdColonyAdView mAdColonyAdView;

    public AdColonyBanner() {
        mHandler = new Handler();
        mAdColonyAdapterConfiguration = new AdColonyAdapterConfiguration();
    }

    @Override
    protected void loadBanner(@NonNull Context context,
                              @NonNull CustomEventBannerListener customEventBannerListener,
                              @NonNull Map<String, Object> localExtras,
                              @NonNull Map<String, String> serverExtras) {
        if (!(context instanceof Activity)) {
            customEventBannerListener.onBannerFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME, MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR.getIntCode(), MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            return;
        }

        String clientOptions = AdColonyUtils.DEFAULT_CLIENT_OPTIONS;
        String appId = AdColonyUtils.DEFAULT_APP_ID;
        String[] allZoneIds = AdColonyUtils.DEFAULT_ALL_ZONE_IDS;
        String zoneId = AdColonyUtils.DEFAULT_ZONE_ID;

        mCustomEventBannerListener = customEventBannerListener;

        adSize = getAdSizeFromLocalExtras(localExtras);
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

            mCustomEventBannerListener.onBannerFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            return;
        }

        mAdColonyBannerListener = getAdColonyBannerListener();

        AdColonyUtils.checkAndConfigureAdColony(context, clientOptions, appId, allZoneIds);

        if (!TextUtils.isEmpty(zoneId)) {
            AdColony.requestAdView(zoneId, mAdColonyBannerListener, adSize);
            MoPubLog.log(zoneId, LOAD_ATTEMPTED, ADAPTER_NAME);
        }

    }

    @Override
    protected void onInvalidate() {
        if (mAdColonyAdView != null) {
            mAdColonyBannerListener = null;
            mAdColonyAdView.destroy();
            mAdColonyAdView = null;
        }
    }

    private AdColonyAdViewListener getAdColonyBannerListener() {
        if(mAdColonyBannerListener != null) {
            return mAdColonyBannerListener;
        } else {
            return new AdColonyAdViewListener() {
                @Override
                public void onRequestFilled(final AdColonyAdView adColonyAdView) {
                    mAdColonyAdView = adColonyAdView;
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mCustomEventBannerListener.onBannerLoaded(adColonyAdView);
                            MoPubLog.log(LOAD_SUCCESS, ADAPTER_NAME);
                        }
                    });
                }
                @Override
                public void onRequestNotFilled(AdColonyZone zone) {
                    super.onRequestNotFilled(zone);
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mCustomEventBannerListener.onBannerFailed(MoPubErrorCode.NETWORK_NO_FILL);
                            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME, MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                                    MoPubErrorCode.NETWORK_NO_FILL);
                        }
                    });
                }
                @Override
                public void onClicked(AdColonyAdView ad) {
                    super.onClicked(ad);
                    mCustomEventBannerListener.onBannerClicked();
                    MoPubLog.log(CLICKED, ADAPTER_NAME);
                }
                @Override
                public void onLeftApplication(AdColonyAdView ad) {
                    super.onLeftApplication(ad);
                    mCustomEventBannerListener.onLeaveApplication();
                    MoPubLog.log(WILL_LEAVE_APPLICATION, ADAPTER_NAME);
                }
                @Override
                public void onOpened(AdColonyAdView ad) {
                    super.onOpened(ad);
                    mCustomEventBannerListener.onBannerExpanded();
                }
                @Override
                public void onClosed(AdColonyAdView ad) {
                    super.onClosed(ad);
                    mCustomEventBannerListener.onBannerCollapsed();
                }
            };
        }
    }

    private AdColonyAdSize getAdSizeFromLocalExtras(Map<String, Object> localExtras) {
        AdColonyAdSize adSize =  AdColonyAdSize.BANNER;

        if (localExtras == null || localExtras.isEmpty()) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "No local extras provided");
            return adSize;
        }

        try {
            final int width = (Integer) localExtras.get(DataKeys.AD_WIDTH);
            final int height = (Integer) localExtras.get(DataKeys.AD_HEIGHT);
            adSize = new AdColonyAdSize(width,height);
            if(adSize == null) {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "Invalid width (" + width + ") and height " +
                        "(" + height + ") provided. Setting Default AdSize.");
            }
        } catch (Throwable th) {

        }

        return adSize;
    }
}
