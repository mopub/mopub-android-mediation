// Copyright 2018-2019 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/
// 2020.3.3- add class HuaweiInterstitial
// Huawei Technologies Co., Ltd.

package com.mopub.mobileads;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.huawei.hms.ads.AdListener;
import com.huawei.hms.ads.AdParam;
import com.huawei.hms.ads.HwAds;
import com.huawei.hms.ads.InterstitialAd;
import com.huawei.hms.ads.RequestOptions;
import com.mopub.common.logging.MoPubLog;

import java.util.Map;

import static com.huawei.hms.ads.TagForChild.TAG_FOR_CHILD_PROTECTION_FALSE;
import static com.huawei.hms.ads.TagForChild.TAG_FOR_CHILD_PROTECTION_TRUE;
import static com.huawei.hms.ads.TagForChild.TAG_FOR_CHILD_PROTECTION_UNSPECIFIED;
import static com.huawei.hms.ads.UnderAge.PROMISE_FALSE;
import static com.huawei.hms.ads.UnderAge.PROMISE_TRUE;
import static com.huawei.hms.ads.UnderAge.PROMISE_UNSPECIFIED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;

public class HuaweiInterstitial extends CustomEventInterstitial {
    /*
     * These keys are intended for MoPub internal use. Do not modify.
     */
    public static final String AD_UNIT_ID_KEY = "adUnitID";
    public static final String CONTENT_URL_KEY = "contentUrl";
    public static final String TAG_FOR_CHILD_DIRECTED_KEY = "tagForChildDirectedTreatment";
    public static final String TAG_FOR_UNDER_AGE_OF_CONSENT_KEY = "tagForUnderAgeOfConsent";

    @NonNull
    private static final String ADAPTER_NAME = HuaweiInterstitial.class.getSimpleName();
    private HuaweiAdapterConfiguration mHuaweiAdsAdapterConfiguration;
    private CustomEventInterstitialListener mInterstitialListener;
    private InterstitialAd mHuaweiInterstitialAd;
    private static String mAdUnitId;

    public HuaweiInterstitial() {
        mHuaweiAdsAdapterConfiguration = new HuaweiAdapterConfiguration();
    }

    @Override
    protected void loadInterstitial(
            final Context context,
            final CustomEventInterstitialListener customEventInterstitialListener,
            final Map<String, Object> localExtras,
            final Map<String, String> serverExtras) {

        setAutomaticImpressionAndClickTracking(false);

        mInterstitialListener = customEventInterstitialListener;

        if (extrasAreValid(serverExtras)) {
            mAdUnitId = serverExtras.get(AD_UNIT_ID_KEY);

            mHuaweiAdsAdapterConfiguration.setCachedInitializationParameters(context, serverExtras);
        } else {
            MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                    MoPubErrorCode.NETWORK_NO_FILL);

            if (mInterstitialListener != null) {
                mInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
            }

            return;
        }

        mHuaweiInterstitialAd = new InterstitialAd(context);
        mHuaweiInterstitialAd.setAdListener(new InterstitialAdListener());
        mHuaweiInterstitialAd.setAdId(mAdUnitId);

        AdParam.Builder builder = new AdParam.Builder();
        builder.setRequestOrigin("MoPub");

        // Publishers may append a content URL by passing it to the MoPubInterstitial.setLocalExtras() call.
        final String contentUrl = (String) localExtras.get(CONTENT_URL_KEY);

        if (!TextUtils.isEmpty(contentUrl)) {
            builder.setTargetingContentUrl(contentUrl);
        }

        final RequestOptions.Builder requestConfigurationBuilder = new RequestOptions.Builder();

        // Publishers may want to indicate that their content is child-directed and forward this
        // information to Huawei.
        final Boolean childDirected = (Boolean) localExtras.get(TAG_FOR_CHILD_DIRECTED_KEY);

        if (childDirected != null) {
            if (childDirected) {
                requestConfigurationBuilder.setTagForChildProtection(TAG_FOR_CHILD_PROTECTION_TRUE);
            } else {
                requestConfigurationBuilder.setTagForChildProtection(TAG_FOR_CHILD_PROTECTION_FALSE);
            }
        } else {
            requestConfigurationBuilder.setTagForChildProtection(TAG_FOR_CHILD_PROTECTION_UNSPECIFIED);
        }

        // Publishers may want to mark their requests to receive treatment for users in the
        // European Economic Area (EEA) under the age of consent.
        final Boolean underAgeOfConsent = (Boolean) localExtras.get(TAG_FOR_UNDER_AGE_OF_CONSENT_KEY);

        if (underAgeOfConsent != null) {
            if (underAgeOfConsent) {
                requestConfigurationBuilder.setTagForUnderAgeOfPromise(PROMISE_TRUE);
            } else {
                requestConfigurationBuilder.setTagForUnderAgeOfPromise(PROMISE_FALSE);
            }
        } else {
            requestConfigurationBuilder.setTagForUnderAgeOfPromise(PROMISE_UNSPECIFIED);
        }

        final RequestOptions requestConfiguration = requestConfigurationBuilder.build();
        HwAds.setRequestOptions(requestConfiguration);

        final AdParam adRequest = builder.build();

        try {
            mHuaweiInterstitialAd.loadAd(adRequest);

            MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);
        } catch (NoClassDefFoundError e) {
            // This can be thrown by Huawei.
            MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                    MoPubErrorCode.NETWORK_NO_FILL);

            if (mInterstitialListener != null) {
                mInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
            }
        }
    }

    @Override
    protected void showInterstitial() {
        MoPubLog.log(getAdNetworkId(), SHOW_ATTEMPTED, ADAPTER_NAME);

        if (mHuaweiInterstitialAd.isLoaded()) {
            mHuaweiInterstitialAd.show();
        } else {
            MoPubLog.log(getAdNetworkId(), SHOW_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                    MoPubErrorCode.NETWORK_NO_FILL);

            if (mInterstitialListener != null) {
                mInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
            }
        }
    }

    @Override
    protected void onInvalidate() {
        if (mHuaweiInterstitialAd != null) {
            mHuaweiInterstitialAd.setAdListener(null);
        }
    }

    private boolean extrasAreValid(Map<String, String> serverExtras) {
        return serverExtras.containsKey(AD_UNIT_ID_KEY);
    }

    private static String getAdNetworkId() {
        return mAdUnitId;
    }

    private class InterstitialAdListener extends AdListener {
        /*
         * Huawei AdListener implementation
         */
        @Override
        public void onAdClosed() {
            if (mInterstitialListener != null) {
                mInterstitialListener.onInterstitialDismissed();
            }
        }

        @Override
        public void onAdFailed(int errorCode) {
            MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                    getMoPubErrorCode(errorCode).getIntCode(),
                    getMoPubErrorCode(errorCode));

            if (mInterstitialListener != null) {
                mInterstitialListener.onInterstitialFailed(getMoPubErrorCode(errorCode));
            }
        }

        @Override
        public void onAdLeave() {
            if (mInterstitialListener != null) {
                mInterstitialListener.onInterstitialClicked();
            }
        }

        @Override
        public void onAdLoaded() {
            MoPubLog.log(getAdNetworkId(), LOAD_SUCCESS, ADAPTER_NAME);

            if (mInterstitialListener != null) {
                mInterstitialListener.onInterstitialLoaded();
            }
        }

        @Override
        public void onAdOpened() {
            MoPubLog.log(getAdNetworkId(), SHOW_SUCCESS, ADAPTER_NAME);

            if (mInterstitialListener != null) {
                mInterstitialListener.onInterstitialShown();
                mInterstitialListener.onInterstitialImpression();
            }
        }

        /**
         * Converts a given Huawei Mobile Ads SDK error code into {@link MoPubErrorCode}.
         *
         * @param error Huawei Mobile Ads SDK error code.
         * @return an equivalent MoPub SDK error code for the given Huawei Mobile Ads SDK error
         * code.
         */
        private MoPubErrorCode getMoPubErrorCode(int error) {
            MoPubErrorCode errorCode;
            switch (error) {
                case AdParam.ErrorCode.INNER:
                    errorCode = MoPubErrorCode.INTERNAL_ERROR;
                    break;
                case AdParam.ErrorCode.INVALID_REQUEST:
                    errorCode = MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR;
                    break;
                case AdParam.ErrorCode.NETWORK_ERROR:
                    errorCode = MoPubErrorCode.NO_CONNECTION;
                    break;
                case AdParam.ErrorCode.NO_AD:
                    errorCode = MoPubErrorCode.NO_FILL;
                    break;
                default:
                    errorCode = MoPubErrorCode.UNSPECIFIED;
            }
            return errorCode;
        }
    }
}
