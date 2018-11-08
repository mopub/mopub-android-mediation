package com.mopub.mobileads;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.AdListener;
import com.facebook.ads.AdSettings;
import com.facebook.ads.AdSize;
import com.facebook.ads.AdView;
import com.facebook.ads.AudienceNetworkAds;
import com.mopub.common.DataKeys;
import com.mopub.common.MoPub;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.Views;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class FacebookBanner extends CustomEventBanner implements AdListener {
    private static final String PLACEMENT_ID_KEY = "placement_id";
    private AdView mFacebookBanner;
    private CustomEventBannerListener mBannerListener;
    private static AtomicBoolean sIsInitialized = new AtomicBoolean(false);

    /**
     * CustomEventBanner implementation
     */

    @Override
    protected void loadBanner(final Context context,
                              final CustomEventBannerListener customEventBannerListener,
                              final Map<String, Object> localExtras,
                              final Map<String, String> serverExtras) {
        if(!sIsInitialized.getAndSet(true)) {
            AudienceNetworkAds.initialize(context);
        }
        setAutomaticImpressionAndClickTracking(false);

        mBannerListener = customEventBannerListener;

        final String placementId;
        if (serverExtrasAreValid(serverExtras)) {
            placementId = serverExtras.get(PLACEMENT_ID_KEY);
        } else {
            if (mBannerListener != null) {
                mBannerListener.onBannerFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            }
            return;
        }

        int width;
        int height;
        if (localExtrasAreValid(localExtras)) {
            width = (Integer) localExtras.get(DataKeys.AD_WIDTH);
            height = (Integer) localExtras.get(DataKeys.AD_HEIGHT);
        } else {
            if (mBannerListener != null) {
                mBannerListener.onBannerFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            }
            return;
        }

        AdSize adSize = calculateAdSize(width, height);
        if (adSize == null) {

            if (mBannerListener != null) {
                mBannerListener.onBannerFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            }
            return;
        }
        AdSettings.setMediationService("MOPUB_" + MoPub.SDK_VERSION);

        mFacebookBanner = new AdView(context, placementId, adSize);
        mFacebookBanner.setAdListener(this);

        final String adm = serverExtras.get(DataKeys.ADM_KEY);
        if (!TextUtils.isEmpty(adm)) {
            mFacebookBanner.loadAdFromBid(adm);
        } else {
            mFacebookBanner.loadAd();
        }
    }

    @Override
    protected void onInvalidate() {
        if (mFacebookBanner != null) {
            Views.removeFromParent(mFacebookBanner);
            mFacebookBanner.destroy();
            mFacebookBanner = null;
        }
    }

    /**
     * AdListener implementation
     */

    @Override
    public void onAdLoaded(Ad ad) {
        MoPubLog.d("Facebook banner ad loaded successfully. Showing ad...");

        if (mBannerListener != null) {
            mBannerListener.onBannerLoaded(mFacebookBanner);
        }
    }

    @Override
    public void onError(final Ad ad, final AdError error) {
        MoPubLog.d("Facebook banner ad failed to load.");

        if (mBannerListener != null) {
            if (error == AdError.NO_FILL) {
                mBannerListener.onBannerFailed(MoPubErrorCode.NETWORK_NO_FILL);
            } else if (error == AdError.INTERNAL_ERROR) {
                mBannerListener.onBannerFailed(MoPubErrorCode.NETWORK_INVALID_STATE);
            } else {
                mBannerListener.onBannerFailed(MoPubErrorCode.UNSPECIFIED);
            }
        }
    }

    @Override
    public void onAdClicked(Ad ad) {
        MoPubLog.d("Facebook banner ad clicked.");

        if (mBannerListener != null) {
            mBannerListener.onBannerClicked();
        }
    }

    @Override
    public void onLoggingImpression(Ad ad) {
        MoPubLog.d("Facebook banner ad logged impression.");

        if (mBannerListener != null) {
            mBannerListener.onBannerImpression();
        }
    }

    private boolean serverExtrasAreValid(final Map<String, String> serverExtras) {
        final String placementId = serverExtras.get(PLACEMENT_ID_KEY);
        return (placementId != null && placementId.length() > 0);
    }

    private boolean localExtrasAreValid(@NonNull final Map<String, Object> localExtras) {
        return localExtras.get(DataKeys.AD_WIDTH) instanceof Integer
                && localExtras.get(DataKeys.AD_HEIGHT) instanceof Integer;
    }

    @Nullable
    private AdSize calculateAdSize(int width, int height) {
        // Use the smallest AdSize that will properly contain the adView
        if (height <= AdSize.BANNER_HEIGHT_50.getHeight()) {
            return AdSize.BANNER_HEIGHT_50;
        } else if (height <= AdSize.BANNER_HEIGHT_90.getHeight()) {
            return AdSize.BANNER_HEIGHT_90;
        } else if (height <= AdSize.RECTANGLE_HEIGHT_250.getHeight()) {
            return AdSize.RECTANGLE_HEIGHT_250;
        } else {
            return null;
        }
    }
}
