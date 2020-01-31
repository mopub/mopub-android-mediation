package com.mopub.mobileads;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.AdListener;
import com.facebook.ads.AdSize;
import com.facebook.ads.AdView;
import com.facebook.ads.AudienceNetworkAds;
import com.mopub.common.DataKeys;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.Views;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.facebook.ads.AdError.BROKEN_MEDIA_ERROR_CODE;
import static com.facebook.ads.AdError.CACHE_ERROR_CODE;
import static com.facebook.ads.AdError.INTERNAL_ERROR_CODE;
import static com.facebook.ads.AdError.LOAD_TOO_FREQUENTLY_ERROR_CODE;
import static com.facebook.ads.AdError.MEDIATION_ERROR_CODE;
import static com.facebook.ads.AdError.NETWORK_ERROR_CODE;
import static com.facebook.ads.AdError.NO_FILL_ERROR_CODE;
import static com.facebook.ads.AdError.SERVER_ERROR_CODE;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;
import static com.mopub.mobileads.MoPubErrorCode.CANCELLED;
import static com.mopub.mobileads.MoPubErrorCode.NETWORK_INVALID_STATE;
import static com.mopub.mobileads.MoPubErrorCode.NETWORK_NO_FILL;
import static com.mopub.mobileads.MoPubErrorCode.NO_CONNECTION;
import static com.mopub.mobileads.MoPubErrorCode.UNSPECIFIED;
import static com.mopub.mobileads.MoPubErrorCode.VIDEO_CACHE_ERROR;
import static com.mopub.mobileads.MoPubErrorCode.VIDEO_PLAYBACK_ERROR;

public class FacebookBanner extends CustomEventBanner implements AdListener {
    private static final String PLACEMENT_ID_KEY = "placement_id";
    private static final String ADAPTER_NAME = FacebookBanner.class.getSimpleName();
    private static AtomicBoolean sIsInitialized = new AtomicBoolean(false);

    private AdView mFacebookBanner;
    private CustomEventBannerListener mBannerListener;
    @NonNull
    private FacebookAdapterConfiguration mFacebookAdapterConfiguration;
    private static String mPlacementId;

    /**
     * CustomEventBanner implementation
     */

    public FacebookBanner() {
        mFacebookAdapterConfiguration = new FacebookAdapterConfiguration();
    }

    @Override
    protected void loadBanner(final Context context,
                              final CustomEventBannerListener customEventBannerListener,
                              final Map<String, Object> localExtras,
                              final Map<String, String> serverExtras) {
        if (!sIsInitialized.getAndSet(true)) {
            AudienceNetworkAds.initialize(context);
        }

        setAutomaticImpressionAndClickTracking(false);

        mBannerListener = customEventBannerListener;

        if (serverExtrasAreValid(serverExtras)) {
            mPlacementId = serverExtras.get(PLACEMENT_ID_KEY);
            mFacebookAdapterConfiguration.setCachedInitializationParameters(context, serverExtras);
        } else {
            MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(), MoPubErrorCode.NETWORK_NO_FILL);
            if (mBannerListener != null) {
                mBannerListener.onBannerFailed(NETWORK_NO_FILL);
            }
            return;
        }
        
        int height = 0;

        if (localExtrasAreValid(localExtras)) {
            Object heightObj = localExtras.get(DataKeys.AD_HEIGHT);

            if (heightObj instanceof Integer) {
                height = (Integer) heightObj;
            }
        } else {
            MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(), MoPubErrorCode.NETWORK_NO_FILL);
            if (mBannerListener != null) {
                mBannerListener.onBannerFailed(NETWORK_NO_FILL);
            }
            return;
        }

        AdSize adSize = calculateAdSize(height);

        mFacebookBanner = new AdView(context, mPlacementId, adSize);

        AdView.AdViewLoadConfigBuilder bannerConfigBuilder = mFacebookBanner.buildLoadAdConfig()
                .withAdListener(this);

        final String adm = serverExtras.get(DataKeys.ADM_KEY);
        if (!TextUtils.isEmpty(adm)) {
            mFacebookBanner.loadAd(bannerConfigBuilder.withBid(adm).build());
            MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);
        } else {
            mFacebookBanner.loadAd(bannerConfigBuilder.build());
            MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);
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
        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Facebook banner ad loaded " +
                "successfully. Showing ad...");

        if (mBannerListener != null) {
            mBannerListener.onBannerLoaded(mFacebookBanner);
            MoPubLog.log(getAdNetworkId(), LOAD_SUCCESS, ADAPTER_NAME);
            MoPubLog.log(getAdNetworkId(), SHOW_ATTEMPTED, ADAPTER_NAME);
            MoPubLog.log(getAdNetworkId(), SHOW_SUCCESS, ADAPTER_NAME);
        }
    }

    @Override
    public void onError(final Ad ad, final AdError error) {
        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Facebook banner ad failed " +
                "to load.");
        MoPubErrorCode errorCode;

        switch (error.getErrorCode()) {
            case NO_FILL_ERROR_CODE:
                errorCode = NETWORK_NO_FILL;
                break;
            case INTERNAL_ERROR_CODE:
                errorCode = MoPubErrorCode.INTERNAL_ERROR;
                break;
            case NETWORK_ERROR_CODE:
                errorCode = NO_CONNECTION;
                break;
            case LOAD_TOO_FREQUENTLY_ERROR_CODE:
                errorCode = CANCELLED;
                break;
            case SERVER_ERROR_CODE:
                errorCode = MoPubErrorCode.SERVER_ERROR;
                break;
            case CACHE_ERROR_CODE:
                errorCode = VIDEO_CACHE_ERROR;
                break;
            case MEDIATION_ERROR_CODE:
                errorCode = NETWORK_INVALID_STATE;
                break;
            case BROKEN_MEDIA_ERROR_CODE:
                errorCode = VIDEO_PLAYBACK_ERROR;
                break;
            default:
                errorCode = UNSPECIFIED;
        }

        MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME, errorCode.getIntCode(), errorCode);

        if (mBannerListener != null) {
            mBannerListener.onBannerFailed(errorCode);
        }
    }

    @Override
    public void onAdClicked(Ad ad) {
        if (mBannerListener != null) {
            mBannerListener.onBannerClicked();
            MoPubLog.log(getAdNetworkId(), CLICKED, ADAPTER_NAME);
        }
    }

    @Override
    public void onLoggingImpression(Ad ad) {
        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Facebook banner ad logged " +
                "impression.");
        if (mBannerListener != null) {
            mBannerListener.onBannerImpression();
        }
    }

    private boolean serverExtrasAreValid(final Map<String, String> serverExtras) {
        final String placementId = serverExtras.get(PLACEMENT_ID_KEY);
        return (placementId != null && placementId.length() > 0);
    }

    private boolean localExtrasAreValid(@NonNull final Map<String, Object> localExtras) {
        Preconditions.checkNotNull(localExtras);

        return localExtras.get(DataKeys.AD_WIDTH) instanceof Integer
                && localExtras.get(DataKeys.AD_HEIGHT) instanceof Integer;
    }

    @Nullable
    private AdSize calculateAdSize(int height) {
        if (height >= AdSize.RECTANGLE_HEIGHT_250.getHeight()) {
            return AdSize.RECTANGLE_HEIGHT_250;
        } else if (height >= AdSize.BANNER_HEIGHT_90.getHeight()) {
            return AdSize.BANNER_HEIGHT_90;
        } else {
            // Default to standard banner size
            return AdSize.BANNER_HEIGHT_50;
        }
    }

    private static String getAdNetworkId() {
        return mPlacementId;
    }
}
