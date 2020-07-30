package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.AudienceNetworkAds;
import com.facebook.ads.InterstitialAd;
import com.facebook.ads.InterstitialAdExtendedListener;
import com.mopub.common.DataKeys;
import com.mopub.common.LifecycleListener;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;

import java.util.Map;

import static com.facebook.ads.AdError.BROKEN_MEDIA_ERROR_CODE;
import static com.facebook.ads.AdError.CACHE_ERROR_CODE;
import static com.facebook.ads.AdError.INTERNAL_ERROR_CODE;
import static com.facebook.ads.AdError.INTERSTITIAL_AD_TIMEOUT;
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
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;
import static com.mopub.mobileads.MoPubErrorCode.CANCELLED;
import static com.mopub.mobileads.MoPubErrorCode.FULLSCREEN_SHOW_ERROR;
import static com.mopub.mobileads.MoPubErrorCode.NETWORK_INVALID_STATE;
import static com.mopub.mobileads.MoPubErrorCode.NETWORK_NO_FILL;
import static com.mopub.mobileads.MoPubErrorCode.NETWORK_TIMEOUT;
import static com.mopub.mobileads.MoPubErrorCode.NO_CONNECTION;
import static com.mopub.mobileads.MoPubErrorCode.UNSPECIFIED;
import static com.mopub.mobileads.MoPubErrorCode.VIDEO_CACHE_ERROR;
import static com.mopub.mobileads.MoPubErrorCode.VIDEO_PLAYBACK_ERROR;

public class FacebookInterstitial extends BaseAd implements InterstitialAdExtendedListener {
    private static final int ONE_HOURS_MILLIS = 60 * 60 * 1000;
    private static final String PLACEMENT_ID_KEY = "placement_id";
    private InterstitialAd mFacebookInterstitial;
    private static final String ADAPTER_NAME = FacebookInterstitial.class.getSimpleName();
    @NonNull
    private Handler mHandler;
    private Runnable mAdExpiration;
    @NonNull
    private FacebookAdapterConfiguration mFacebookAdapterConfiguration;
    private String mPlacementId;

    public FacebookInterstitial() {
        mHandler = new Handler();
        mFacebookAdapterConfiguration = new FacebookAdapterConfiguration();

        mAdExpiration = new Runnable() {
            @Override
            public void run() {
                // Add custom cleanup logic following an expiration. To request a new ad, call
                // MoPubInterstitial.destroy() first.
                if (mLoadListener != null) {
                    MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Expiring unused " +
                            "Facebook Interstitial ad due to Facebook's 60-minute expiration policy.");
                    mLoadListener.onAdLoadFailed(FULLSCREEN_SHOW_ERROR);
                    MoPubLog.log(getAdNetworkId(), SHOW_FAILED, ADAPTER_NAME,
                            MoPubErrorCode.EXPIRED.getIntCode(), MoPubErrorCode.EXPIRED);

                    onInvalidate();
                }
            }
        };
    }

    /**
     * CustomEventInterstitial implementation
     */

    @Override
    protected void load(@NonNull final Context context, @NonNull final AdData adData) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(adData);

        if (!AudienceNetworkAds.isInitialized(context)) {
            AudienceNetworkAds.initialize(context);
        }

        setAutomaticImpressionAndClickTracking(false);

        final Map<String, String> extras = adData.getExtras();
        if (extrasAreValid(extras)) {
            mPlacementId = extras.get(PLACEMENT_ID_KEY);
            mFacebookAdapterConfiguration.setCachedInitializationParameters(context, extras);
        } else {
            if (mLoadListener != null) {
                mLoadListener.onAdLoadFailed(MoPubErrorCode.NETWORK_NO_FILL);
                MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                        MoPubErrorCode.NETWORK_NO_FILL.getIntCode(), MoPubErrorCode.NETWORK_NO_FILL);
            }
            return;
        }

        mFacebookInterstitial = new InterstitialAd(context, mPlacementId);

        final String adMarkup = extras.get(DataKeys.ADM_KEY);

        InterstitialAd.InterstitialAdLoadConfigBuilder interstitialLoadAdConfigBuilder =
                mFacebookInterstitial.buildLoadAdConfig().withAdListener(this);

        if (!TextUtils.isEmpty(adMarkup)) {
            mFacebookInterstitial.loadAd(interstitialLoadAdConfigBuilder.withBid(adMarkup).build());
            MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);
        } else {
            mFacebookInterstitial.loadAd(interstitialLoadAdConfigBuilder.build());
            MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);
        }
    }

    @Override
    protected void show() {
        MoPubLog.log(getAdNetworkId(), SHOW_ATTEMPTED, ADAPTER_NAME);
        if (mFacebookInterstitial != null && mFacebookInterstitial.isAdLoaded() &&
                !mFacebookInterstitial.isAdInvalidated()) {
            mFacebookInterstitial.show();
            cancelExpirationTimer();
        } else {
            MoPubLog.log(getAdNetworkId(), SHOW_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(), MoPubErrorCode.NETWORK_NO_FILL);
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Tried to show a Facebook " +
                    "interstitial ad when it's not ready. Please try again.");
            if (mInteractionListener != null) {
                onError(mFacebookInterstitial, AdError.INTERNAL_ERROR);
            } else {
                MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Interstitial listener " +
                        "not instantiated. Please load interstitial again.");
            }
        }
    }

    @Override
    protected void onInvalidate() {
        cancelExpirationTimer();
        if (mFacebookInterstitial != null) {
            mFacebookInterstitial.destroy();
            mFacebookInterstitial = null;
        }
    }

    @Nullable
    @Override
    protected LifecycleListener getLifecycleListener() {
        return null;
    }

    /**
     * InterstitialAdListener implementation
     */

    @Override
    public void onAdLoaded(final Ad ad) {
        cancelExpirationTimer();
        if (mLoadListener != null) {
            mLoadListener.onAdLoaded();
            MoPubLog.log(getAdNetworkId(), LOAD_SUCCESS, ADAPTER_NAME);
        }
        mHandler.postDelayed(mAdExpiration, ONE_HOURS_MILLIS);
    }

    @Override
    public void onError(final Ad ad, final AdError error) {
        cancelExpirationTimer();
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
            case INTERSTITIAL_AD_TIMEOUT:
                errorCode = NETWORK_TIMEOUT;
                break;
            case BROKEN_MEDIA_ERROR_CODE:
                errorCode = VIDEO_PLAYBACK_ERROR;
                break;
            default:
                errorCode = UNSPECIFIED;
        }

        MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME, errorCode.getIntCode(), errorCode);

        if (mInteractionListener == null && mLoadListener != null) {
            mLoadListener.onAdLoadFailed(errorCode);
        } else if (mInteractionListener != null) {
            mInteractionListener.onAdFailed(errorCode);
        }
    }

    @Override
    public void onInterstitialDisplayed(final Ad ad) {
        cancelExpirationTimer();
        MoPubLog.log(getAdNetworkId(), SHOW_SUCCESS, ADAPTER_NAME);
        if (mInteractionListener != null) {
            mInteractionListener.onAdShown();
        }
    }

    @Override
    public void onAdClicked(final Ad ad) {
        MoPubLog.log(getAdNetworkId(), CLICKED, ADAPTER_NAME);
        if (mInteractionListener != null) {
            mInteractionListener.onAdClicked();
        }
    }

    @Override
    public void onLoggingImpression(Ad ad) {
        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Facebook interstitial ad " +
                "logged impression.");
        if (mInteractionListener != null) {
            mInteractionListener.onAdImpression();
        }
    }

    @Override
    public void onInterstitialDismissed(final Ad ad) {
        if (mInteractionListener != null) {
            mInteractionListener.onAdDismissed();
        }
    }

    @Override
    public void onInterstitialActivityDestroyed() {
        if (mInteractionListener != null) {
            mInteractionListener.onAdDismissed();
        }
    }

    @Override
    public void onRewardedAdCompleted() {
        //no-op
    }

    @Override
    public void onRewardedAdServerSucceeded() {
        //no-op
    }

    @Override
    public void onRewardedAdServerFailed() {
        //no-op
    }

    private boolean extrasAreValid(final Map<String, String> extras) {
        final String placementId = extras.get(PLACEMENT_ID_KEY);
        return (placementId != null && placementId.length() > 0);
    }

    private void cancelExpirationTimer() {
        mHandler.removeCallbacks(mAdExpiration);
    }

    @NonNull
    public String getAdNetworkId() {
        return mPlacementId == null ? "" : mPlacementId;
    }

    @Override
    protected boolean checkAndInitializeSdk(@NonNull Activity launcherActivity, @NonNull AdData adData) {
        return false;
    }
}
