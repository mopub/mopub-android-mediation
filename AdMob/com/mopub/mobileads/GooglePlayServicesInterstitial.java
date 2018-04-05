package com.mopub.mobileads;

import android.content.Context;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.mopub.MoPubMediationLog;

import static com.mopub.MoPubMediationLog.AdEvent.*;

import java.util.Map;

public class GooglePlayServicesInterstitial extends CustomEventInterstitial {
    /*
     * These keys are intended for MoPub internal use. Do not modify.
     */
    public static final String AD_UNIT_ID_KEY = "adUnitID";
    public static final String LOCATION_KEY = "location";

    private CustomEventInterstitialListener mInterstitialListener;
    private InterstitialAd mGoogleInterstitialAd;

    @Override
    protected void loadInterstitial(
            final Context context,
            final CustomEventInterstitialListener customEventInterstitialListener,
            final Map<String, Object> localExtras,
            final Map<String, String> serverExtras) {

        new MoPubMediationLog(getClass().getSimpleName());

        mInterstitialListener = customEventInterstitialListener;
        final String adUnitId;

        if (extrasAreValid(serverExtras)) {
            adUnitId = serverExtras.get(AD_UNIT_ID_KEY);
        } else {
            MoPubMediationLog.logParamsInvalid(AD_UNIT_ID_KEY, "serverExtras");
            mInterstitialListener.onInterstitialFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            return;
        }

        mGoogleInterstitialAd = new InterstitialAd(context);
        mGoogleInterstitialAd.setAdListener(new InterstitialAdListener());
        mGoogleInterstitialAd.setAdUnitId(adUnitId);

        final AdRequest adRequest = new AdRequest.Builder()
                .setRequestAgent("MoPub")
                .build();

        try {
            mGoogleInterstitialAd.loadAd(adRequest);
            MoPubMediationLog.logAdRequestSent();
        } catch (NoClassDefFoundError e) {
            // This can be thrown by Play Services on Honeycomb.
            mInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
        }
    }

    @Override
    protected void showInterstitial() {
        if (mGoogleInterstitialAd.isLoaded()) {
            mGoogleInterstitialAd.show();
        } else {
            MoPubMediationLog.logAdEvent(UNAVAILABLE);
        }
    }

    @Override
    protected void onInvalidate() {
        MoPubMediationLog.logOnInvalidate();
        if (mGoogleInterstitialAd != null) {
            mGoogleInterstitialAd.setAdListener(null);
        }
    }

    private boolean extrasAreValid(Map<String, String> serverExtras) {
        return serverExtras.containsKey(AD_UNIT_ID_KEY);
    }

    private class InterstitialAdListener extends AdListener {
        /*
         * Google Play Services AdListener implementation
    	 */
        @Override
        public void onAdClosed() {
            MoPubMediationLog.logAdEvent(DISMISSED);
            if (mInterstitialListener != null) {
                mInterstitialListener.onInterstitialDismissed();
            }
        }

        @Override
        public void onAdFailedToLoad(int errorCode) {
            MoPubMediationLog.logAdEvent(ERROR);
            if (mInterstitialListener != null) {
                mInterstitialListener.onInterstitialFailed(getMoPubErrorCode(errorCode));
            }
        }

        @Override
        public void onAdLeftApplication() {
            MoPubMediationLog.logAdEvent(CLICKED);
            if (mInterstitialListener != null) {
                mInterstitialListener.onInterstitialClicked();
            }
        }

        @Override
        public void onAdLoaded() {
            MoPubMediationLog.logAdEvent(LOADED);
            if (mInterstitialListener != null) {
                mInterstitialListener.onInterstitialLoaded();
            }
        }

        @Override
        public void onAdOpened() {
            MoPubMediationLog.logAdEvent(SHOWN);
            if (mInterstitialListener != null) {
                mInterstitialListener.onInterstitialShown();
            }
        }

        /**
         * Converts a given Google Mobile Ads SDK error code into {@link MoPubErrorCode}.
         *
         * @param error Google Mobile Ads SDK error code.
         * @return an equivalent MoPub SDK error code for the given Google Mobile Ads SDK error
         * code.
         */
        private MoPubErrorCode getMoPubErrorCode(int error) {
            MoPubErrorCode errorCode;
            switch (error) {
                case AdRequest.ERROR_CODE_INTERNAL_ERROR:
                    errorCode = MoPubErrorCode.INTERNAL_ERROR;
                    break;
                case AdRequest.ERROR_CODE_INVALID_REQUEST:
                    errorCode = MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR;
                    break;
                case AdRequest.ERROR_CODE_NETWORK_ERROR:
                    errorCode = MoPubErrorCode.NO_CONNECTION;
                    break;
                case AdRequest.ERROR_CODE_NO_FILL:
                    errorCode = MoPubErrorCode.NO_FILL;
                    break;
                default:
                    errorCode = MoPubErrorCode.UNSPECIFIED;
            }
            return errorCode;
        }
    }

    @Deprecated
        // for testing
    InterstitialAd getGoogleInterstitialAd() {
        return mGoogleInterstitialAd;
    }
}
