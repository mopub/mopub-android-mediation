package com.mopub.mobileads;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.AdSettings;
import com.facebook.ads.AudienceNetworkAds;
import com.facebook.ads.InterstitialAd;
import com.facebook.ads.InterstitialAdListener;
import com.mopub.common.DataKeys;
import com.mopub.common.MoPub;
import com.mopub.common.logging.MoPubLog;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.mopub.mobileads.MoPubErrorCode.EXPIRED;

public class FacebookInterstitial extends CustomEventInterstitial implements InterstitialAdListener {
    private static final int ONE_HOURS_MILLIS = 60 * 60 * 1000;
    private static final String PLACEMENT_ID_KEY = "placement_id";
    private InterstitialAd mFacebookInterstitial;
    private CustomEventInterstitialListener mInterstitialListener;
    @NonNull
    private Handler mHandler;
    private Runnable mAdExpiration;
    private static AtomicBoolean sIsInitialized = new AtomicBoolean(false);

    public FacebookInterstitial() {
        mHandler = new Handler();
        mAdExpiration = new Runnable() {
            @Override
            public void run() {
                if (mInterstitialListener != null) {
                    MoPubLog.d("Expiring unused Facebook Interstitial ad due to Facebook's 60-minute expiration policy.");
                    mInterstitialListener.onInterstitialFailed(EXPIRED);

                    /* Can't get a direct handle to adFailed() to set the interstitial's state to IDLE: https://github.com/mopub/mopub-android-sdk/blob/4199080a1efd755641369715a4de5031d6072fbc/mopub-sdk/mopub-sdk-interstitial/src/main/java/com/mopub/mobileads/MoPubInterstitial.java#L91.
                    So, invalidating the interstitial (destroying & nulling) instead. */
                    onInvalidate();
                }
            }
        };
    }

    /**
     * CustomEventInterstitial implementation
     */

    @Override
    protected void loadInterstitial(final Context context,
                                    final CustomEventInterstitialListener customEventInterstitialListener,
                                    final Map<String, Object> localExtras,
                                    final Map<String, String> serverExtras) {
        if(!sIsInitialized.getAndSet(true)) {
            AudienceNetworkAds.initialize(context);
        }
        setAutomaticImpressionAndClickTracking(false);

        MoPubLog.d("Loading Facebook interstitial");
        mInterstitialListener = customEventInterstitialListener;

        final String placementId;
        if (extrasAreValid(serverExtras)) {
            placementId = serverExtras.get(PLACEMENT_ID_KEY);
        } else {
            if (mInterstitialListener != null) {
                mInterstitialListener.onInterstitialFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            }
            return;
        }

        AdSettings.setMediationService("MOPUB_" + MoPub.SDK_VERSION);

        mFacebookInterstitial = new InterstitialAd(context, placementId);
        mFacebookInterstitial.setAdListener(this);

        final String adm = serverExtras.get(DataKeys.ADM_KEY);
        if (!TextUtils.isEmpty(adm)) {
            mFacebookInterstitial.loadAdFromBid(adm);
        } else {
            mFacebookInterstitial.loadAd();
        }
    }

    @Override
    protected void showInterstitial() {
        if (mFacebookInterstitial != null && mFacebookInterstitial.isAdLoaded()) {
            mFacebookInterstitial.show();
            cancelExpirationTimer();
        } else {
            MoPubLog.d("Tried to show a Facebook interstitial ad when it's not ready. Please try again.");
            if (mInterstitialListener != null) {
                onError(mFacebookInterstitial, AdError.INTERNAL_ERROR);
            } else {
                MoPubLog.d("Interstitial listener not instantiated. Please load interstitial again.");
            }
        }
    }

    @Override
    protected void onInvalidate() {
        cancelExpirationTimer();
        if (mFacebookInterstitial != null) {
            mFacebookInterstitial.destroy();
            mFacebookInterstitial = null;
            mInterstitialListener = null;
        }
    }

    /**
     * InterstitialAdListener implementation
     */

    @Override
    public void onAdLoaded(final Ad ad) {
        cancelExpirationTimer();
        MoPubLog.d("Facebook interstitial ad loaded successfully.");
        if (mInterstitialListener != null) {
            mInterstitialListener.onInterstitialLoaded();
        }
        mHandler.postDelayed(mAdExpiration, ONE_HOURS_MILLIS);
    }

    @Override
    public void onError(final Ad ad, final AdError error) {
        cancelExpirationTimer();
        MoPubLog.d("Facebook interstitial ad failed to load.");
        if (mInterstitialListener != null) {
            if (error == AdError.NO_FILL) {
                mInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
            } else if (error == AdError.INTERNAL_ERROR) {
                mInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_INVALID_STATE);
            } else {
                mInterstitialListener.onInterstitialFailed(MoPubErrorCode.UNSPECIFIED);
            }
        }
    }

    @Override
    public void onInterstitialDisplayed(final Ad ad) {
        cancelExpirationTimer();
        MoPubLog.d("Showing Facebook interstitial ad.");
        if (mInterstitialListener != null) {
            mInterstitialListener.onInterstitialShown();
        }
    }

    @Override
    public void onAdClicked(final Ad ad) {
        MoPubLog.d("Facebook interstitial ad clicked.");
        if (mInterstitialListener != null) {
            mInterstitialListener.onInterstitialClicked();
        }
    }

    @Override
    public void onLoggingImpression(Ad ad) {
        MoPubLog.d("Facebook interstitial ad logged impression.");
        if (mInterstitialListener != null) {
            mInterstitialListener.onInterstitialImpression();
        }
    }

    @Override
    public void onInterstitialDismissed(final Ad ad) {
        MoPubLog.d("Facebook interstitial ad dismissed.");
        if (mInterstitialListener != null) {
            mInterstitialListener.onInterstitialDismissed();
        }
    }

    private boolean extrasAreValid(final Map<String, String> serverExtras) {
        final String placementId = serverExtras.get(PLACEMENT_ID_KEY);
        return (placementId != null && placementId.length() > 0);
    }

    private void cancelExpirationTimer() {
        mHandler.removeCallbacks(mAdExpiration);
    }
}
