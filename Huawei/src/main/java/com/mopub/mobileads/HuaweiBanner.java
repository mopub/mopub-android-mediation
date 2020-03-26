// Copyright 2018-2019 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/
// 2020.3.3- add class HuaweiBanner
// Huawei Technologies Co., Ltd.

package com.mopub.mobileads;

import android.content.Context;
import android.text.TextUtils;

import com.huawei.hms.ads.AdListener;
import com.huawei.hms.ads.AdParam;
import com.huawei.hms.ads.BannerAdSize;
import com.huawei.hms.ads.HwAds;
import com.huawei.hms.ads.RequestOptions;
import com.huawei.hms.ads.banner.BannerView;
import com.mopub.common.DataKeys;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.Views;

import java.util.Map;

import static com.huawei.hms.ads.BannerAdSize.BANNER_SIZE_320_50;
import static com.huawei.hms.ads.BannerAdSize.BANNER_SIZE_468_60;
import static com.huawei.hms.ads.BannerAdSize.BANNER_SIZE_320_100;
import static com.huawei.hms.ads.BannerAdSize.BANNER_SIZE_728_90;
import static com.huawei.hms.ads.BannerAdSize.BANNER_SIZE_300_250;
import static com.huawei.hms.ads.BannerAdSize.BANNER_SIZE_160_600;

import static com.huawei.hms.ads.TagForChild.TAG_FOR_CHILD_PROTECTION_FALSE;
import static com.huawei.hms.ads.TagForChild.TAG_FOR_CHILD_PROTECTION_TRUE;
import static com.huawei.hms.ads.TagForChild.TAG_FOR_CHILD_PROTECTION_UNSPECIFIED;
import static com.huawei.hms.ads.UnderAge.PROMISE_FALSE;
import static com.huawei.hms.ads.UnderAge.PROMISE_TRUE;
import static com.huawei.hms.ads.UnderAge.PROMISE_UNSPECIFIED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;

public class HuaweiBanner extends CustomEventBanner {
    /*
     * These keys are intended for MoPub internal use. Do not modify.
     */
    public static final String AD_UNIT_ID_KEY = "adUnitID";
    public static final String CONTENT_URL_KEY = "contentUrl";
    public static final String TAG_FOR_CHILD_DIRECTED_KEY = "tagForChildDirectedTreatment";
    public static final String TAG_FOR_UNDER_AGE_OF_CONSENT_KEY = "tagForUnderAgeOfConsent";

    private static final String ADAPTER_NAME = HuaweiBanner.class.getSimpleName();
    private CustomEventBannerListener mBannerListener;
    private BannerView mBannerView;
    private static String mAdUnitId;

    @Override
    protected void loadBanner(
            final Context context,
            final CustomEventBannerListener customEventBannerListener,
            final Map<String, Object> localExtras,
            final Map<String, String> serverExtras) {
        mBannerListener = customEventBannerListener;

        final Integer adWidth;
        final Integer adHeight;

        if (localExtras != null && !localExtras.isEmpty()) {
            mAdUnitId = serverExtras.get(AD_UNIT_ID_KEY);
            adWidth = (Integer) localExtras.get(DataKeys.AD_WIDTH);
            adHeight = (Integer) localExtras.get(DataKeys.AD_HEIGHT);
        } else {
            MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                    MoPubErrorCode.NETWORK_NO_FILL);

            mBannerListener.onBannerFailed(MoPubErrorCode.NETWORK_NO_FILL);
            return;
        }

        mBannerView = new BannerView(context);
        mBannerView.setAdListener(new AdViewListener());
        mBannerView.setAdId(mAdUnitId);

        final BannerAdSize adSize = (adWidth == null || adHeight == null)
                ? null
                : calculateAdSize(adWidth, adHeight);

        if (adSize != null) {
            mBannerView.setBannerAdSize(adSize);
        } else {
            MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                    MoPubErrorCode.NETWORK_NO_FILL);

            mBannerListener.onBannerFailed(MoPubErrorCode.NETWORK_NO_FILL);
            return;
        }

        AdParam.Builder builder = new AdParam.Builder();
        builder.setRequestOrigin("MoPub");

        // Publishers may append a content URL by passing it to the MoPubView.setLocalExtras() call.
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
            requestConfigurationBuilder.setTagForChildProtection(TAG_FOR_CHILD_PROTECTION_UNSPECIFIED );
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
            mBannerView.loadAd(adRequest);

            MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);
        } catch (NoClassDefFoundError e) {
            // This can be thrown by Play Services on Honeycomb.
            MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                    MoPubErrorCode.NETWORK_NO_FILL);

            mBannerListener.onBannerFailed(MoPubErrorCode.NETWORK_NO_FILL);
        }
    }

    @Override
    protected void onInvalidate() {
        Views.removeFromParent(mBannerView);

        if (mBannerView != null) {
            mBannerView.setAdListener(null);
            mBannerView.destroy();
        }
    }

    private static BannerAdSize calculateAdSize(int width, int height) {
        // Use the largest AdSize that fits into MoPubView
        if (height >= BANNER_SIZE_160_600.getHeight() && width >= BANNER_SIZE_160_600.getWidth()) {
            return BANNER_SIZE_160_600;
        } else if (height >= BANNER_SIZE_300_250.getHeight() && width >= BANNER_SIZE_300_250.getWidth()) {
            return BANNER_SIZE_300_250;
        } else if (height >= BANNER_SIZE_320_100.getHeight() && width >= BANNER_SIZE_320_100.getWidth()) {
            return BANNER_SIZE_320_100;
        } else if (height >= BANNER_SIZE_728_90.getHeight() && width >= BANNER_SIZE_728_90.getWidth()) {
            return BANNER_SIZE_728_90;
        } else if (height >= BANNER_SIZE_468_60.getHeight() && width >= BANNER_SIZE_468_60.getWidth()) {
            return BANNER_SIZE_468_60;
        } else if (height >= BANNER_SIZE_320_50.getHeight() && width >= BANNER_SIZE_320_50.getWidth()) {
            return BANNER_SIZE_320_50;
        } else {
            return null;
        }
    }

    private static String getAdNetworkId() {
        return mAdUnitId;
    }

    private class AdViewListener extends AdListener {
        /*
         * Huawei AdListener implementation
         */

        @Override
        public void onAdClosed() {

        }

        @Override
        public void onAdFailed(int errorCode) {
            MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                    getMoPubErrorCode(errorCode).getIntCode(),
                    getMoPubErrorCode(errorCode));

            if (mBannerListener != null) {
                mBannerListener.onBannerFailed(getMoPubErrorCode(errorCode));
            }
        }

        @Override
        public void onAdLeave() {
        }

        @Override
        public void onAdLoaded() {
            MoPubLog.log(getAdNetworkId(), LOAD_SUCCESS, ADAPTER_NAME);
            MoPubLog.log(getAdNetworkId(), SHOW_ATTEMPTED, ADAPTER_NAME);
            MoPubLog.log(getAdNetworkId(), SHOW_SUCCESS, ADAPTER_NAME);

            if (mBannerListener != null) {
                mBannerListener.onBannerLoaded(mBannerView);
            }
        }

        @Override
        public void onAdOpened() {
            MoPubLog.log(getAdNetworkId(), CLICKED, ADAPTER_NAME);

            if (mBannerListener != null) {
                mBannerListener.onBannerClicked();
            }
        }

        /**
         * Converts a given Huawei Mobile Ads SDK error code into {@link MoPubErrorCode}.
         *  for more detail,you can click
         *  https://developer.huawei.com/consumer/cn/doc/development/HMS-References/ads-api-adparam-errorcode
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
