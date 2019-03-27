package com.mopub.mobileads;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.mopub.common.MediationSettings;
import com.mopub.common.logging.MoPubLog;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;

public class GooglePlayServicesInterstitial extends CustomEventInterstitial {
    /*
     * These keys are intended for MoPub internal use. Do not modify.
     */
    private static final String AD_UNIT_ID_KEY = "adUnitID";
    private static final String ADAPTER_NAME = GooglePlayServicesInterstitial.class.getSimpleName();
    private static final String CONTENT_URL_KEY = "contentUrl";
    private static final String TEST_DEVICES_KEY = "testDevices";

    @NonNull
    private GooglePlayServicesAdapterConfiguration mGooglePlayServicesAdapterConfiguration;
    private CustomEventInterstitialListener mInterstitialListener;
    private InterstitialAd mGoogleInterstitialAd;

    public GooglePlayServicesInterstitial() {
        mGooglePlayServicesAdapterConfiguration = new GooglePlayServicesAdapterConfiguration();
    }

    @Override
    protected void loadInterstitial(
            final Context context,
            final CustomEventInterstitialListener customEventInterstitialListener,
            final Map<String, Object> localExtras,
            final Map<String, String> serverExtras) {

        setAutomaticImpressionAndClickTracking(false);

        mInterstitialListener = customEventInterstitialListener;
        final String adUnitId;

        if (extrasAreValid(serverExtras)) {
            adUnitId = serverExtras.get(AD_UNIT_ID_KEY);

            mGooglePlayServicesAdapterConfiguration.setCachedInitializationParameters(context, serverExtras);
        } else {
            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                    MoPubErrorCode.NETWORK_NO_FILL);

            if (mInterstitialListener != null) {
                mInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
            }

            return;
        }

        mGoogleInterstitialAd = new InterstitialAd(context);
        mGoogleInterstitialAd.setAdListener(new InterstitialAdListener());
        mGoogleInterstitialAd.setAdUnitId(adUnitId);

        AdRequest.Builder builder = new AdRequest.Builder();
        builder.setRequestAgent("MoPub");

        // Publishers may append a content URL by passing it to the MoPubInterstitial.setLocalExtras() call.
        if (localExtras.get(CONTENT_URL_KEY) != null) {
            String contentUrl = localExtras.get(CONTENT_URL_KEY).toString();
            if (!TextUtils.isEmpty(contentUrl)) {
                builder.setContentUrl(contentUrl);
            }
        }

        // Publishers may request for test ads by passing test device IDs to the MoPubInterstitial.setLocalExtras() call.
        if (localExtras.get(TEST_DEVICES_KEY) != null) {
            String testDeviceId = localExtras.get(TEST_DEVICES_KEY).toString();
            if (!TextUtils.isEmpty(testDeviceId)) {
                builder.addTestDevice(testDeviceId);
            }
        }

        // Consent collected from the MoPub’s consent dialogue should not be used to set up
        // Google's personalization preference. Publishers should work with Google to be GDPR-compliant.
        forwardNpaIfSet(builder);

        AdRequest adRequest = builder.build();

        try {
            mGoogleInterstitialAd.loadAd(adRequest);

            MoPubLog.log(adUnitId, LOAD_ATTEMPTED, ADAPTER_NAME);
        } catch (NoClassDefFoundError e) {
            // This can be thrown by Play Services on Honeycomb.
            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                    MoPubErrorCode.NETWORK_NO_FILL);

            if (mInterstitialListener != null) {
                mInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
            }
        }
    }

    private void forwardNpaIfSet(AdRequest.Builder builder) {

        // Only forward the "npa" bundle if it is explicitly set. Otherwise, don't attach it with the ad request.
        if (GooglePlayServicesMediationSettings.getNpaBundle() != null &&
                !GooglePlayServicesMediationSettings.getNpaBundle().isEmpty()) {
            builder.addNetworkExtrasBundle(AdMobAdapter.class, GooglePlayServicesMediationSettings.getNpaBundle());
        }
    }

    @Override
    protected void showInterstitial() {
        MoPubLog.log(SHOW_ATTEMPTED, ADAPTER_NAME);

        if (mGoogleInterstitialAd.isLoaded()) {
            mGoogleInterstitialAd.show();
        } else {
            MoPubLog.log(SHOW_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                    MoPubErrorCode.NETWORK_NO_FILL);

            if (mInterstitialListener != null) {
                mInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
            }
        }
    }

    @Override
    protected void onInvalidate() {
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
            if (mInterstitialListener != null) {
                mInterstitialListener.onInterstitialDismissed();
            }
        }

        @Override
        public void onAdFailedToLoad(int errorCode) {
            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                    getMoPubErrorCode(errorCode).getIntCode(),
                    getMoPubErrorCode(errorCode));

            if (mInterstitialListener != null) {
                mInterstitialListener.onInterstitialFailed(getMoPubErrorCode(errorCode));
            }
        }

        @Override
        public void onAdLeftApplication() {
            if (mInterstitialListener != null) {
                mInterstitialListener.onInterstitialClicked();
            }
        }

        @Override
        public void onAdLoaded() {
            MoPubLog.log(LOAD_SUCCESS, ADAPTER_NAME);

            if (mInterstitialListener != null) {
                mInterstitialListener.onInterstitialLoaded();
            }
        }

        @Override
        public void onAdOpened() {
            MoPubLog.log(SHOW_SUCCESS, ADAPTER_NAME);

            if (mInterstitialListener != null) {
                mInterstitialListener.onInterstitialShown();
                mInterstitialListener.onInterstitialImpression();
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

    public static final class GooglePlayServicesMediationSettings implements MediationSettings {
        private static Bundle npaBundle;

        public GooglePlayServicesMediationSettings() {
        }

        public GooglePlayServicesMediationSettings(Bundle bundle) {
            npaBundle = bundle;
        }

        public void setNpaBundle(Bundle bundle) {
            npaBundle = bundle;
        }

        /* The MoPub Android SDK queries MediationSettings from the rewarded video code
        (MoPubRewardedVideoManager.getGlobalMediationSettings). That API might not always be
        available to publishers importing the modularized SDK(s) based on select ad formats.
        This is a workaround to statically get the "npa" Bundle passed to us via the constructor. */
        private static Bundle getNpaBundle() {
            return npaBundle;
        }
    }

    @Deprecated
        // for testing
    InterstitialAd getGoogleInterstitialAd() {
        return mGoogleInterstitialAd;
    }
}
