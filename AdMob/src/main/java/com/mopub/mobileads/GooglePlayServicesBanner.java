package com.mopub.mobileads;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;

import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.Views;

import java.util.Map;

import static com.google.android.gms.ads.AdSize.BANNER;
import static com.google.android.gms.ads.AdSize.FULL_BANNER;
import static com.google.android.gms.ads.AdSize.LEADERBOARD;
import static com.google.android.gms.ads.AdSize.MEDIUM_RECTANGLE;
import static com.google.android.gms.ads.RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE;
import static com.google.android.gms.ads.RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE;
import static com.google.android.gms.ads.RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE;
import static com.google.android.gms.ads.RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE;
import static com.google.android.gms.ads.RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED;
import static com.google.android.gms.ads.RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;

public class GooglePlayServicesBanner extends CustomEventBanner {
    /*
     * These keys are intended for MoPub internal use. Do not modify.
     */
    private static final String AD_UNIT_ID_KEY = "adUnitID";
    private static final String AD_WIDTH_KEY = "adWidth";
    private static final String AD_HEIGHT_KEY = "adHeight";
    private static final String ADAPTER_NAME = GooglePlayServicesBanner.class.getSimpleName();
    private static final String CONTENT_URL_KEY = "contentUrl";
    private static final String TAG_FOR_CHILD_DIRECTED_KEY = "tagForChildDirectedTreatment";
    private static final String TAG_FOR_UNDER_AGE_OF_CONSENT_KEY = "tagForUnderAgeOfConsent";
    private static final String TEST_DEVICES_KEY = "testDevices";

    private CustomEventBannerListener mBannerListener;
    private AdView mGoogleAdView;

    @Override
    protected void loadBanner(
            final Context context,
            final CustomEventBannerListener customEventBannerListener,
            final Map<String, Object> localExtras,
            final Map<String, String> serverExtras) {
        mBannerListener = customEventBannerListener;

        final int adWidth;
        final int adHeight;

        String adUnitId = "";
        if (extrasAreValid(serverExtras)) {
            adUnitId = serverExtras.get(AD_UNIT_ID_KEY);
            adWidth = Integer.parseInt(serverExtras.get(AD_WIDTH_KEY));
            adHeight = Integer.parseInt(serverExtras.get(AD_HEIGHT_KEY));
        } else {
            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                    MoPubErrorCode.NETWORK_NO_FILL);

            mBannerListener.onBannerFailed(MoPubErrorCode.NETWORK_NO_FILL);
            return;
        }

        mGoogleAdView = new AdView(context);
        mGoogleAdView.setAdListener(new AdViewListener());
        mGoogleAdView.setAdUnitId(adUnitId);

        final AdSize adSize = calculateAdSize(adWidth, adHeight);
        if (adSize == null) {
            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                    MoPubErrorCode.NETWORK_NO_FILL);

            mBannerListener.onBannerFailed(MoPubErrorCode.NETWORK_NO_FILL);
            return;
        }

        mGoogleAdView.setAdSize(adSize);

        AdRequest.Builder builder = new AdRequest.Builder();
        builder.setRequestAgent("MoPub");

        // Publishers may append a content URL by passing it to the MoPubView.setLocalExtras() call.
        String contentUrl = (String) localExtras.get(CONTENT_URL_KEY);

        if (!TextUtils.isEmpty(contentUrl)) {
            builder.setContentUrl(contentUrl);
        }

        // Publishers may request for test ads by passing test device IDs to the MoPubView.setLocalExtras() call.
        String testDeviceId = (String) localExtras.get(TEST_DEVICES_KEY);

        if (!TextUtils.isEmpty(testDeviceId)) {
            builder.addTestDevice(testDeviceId);
        }

        // Consent collected from the MoPub’s consent dialogue should not be used to set up
        // Google's personalization preference. Publishers should work with Google to be GDPR-compliant.
        forwardNpaIfSet(builder);

        RequestConfiguration.Builder requestConfigurationBuilder = new RequestConfiguration.Builder();

        // Publishers may want to indicate that their content is child-directed and forward this
        // information to Google.
        Boolean childDirected = (Boolean) localExtras.get(TAG_FOR_CHILD_DIRECTED_KEY);

        if (childDirected != null) {
            if (childDirected) {
                requestConfigurationBuilder.
                        setTagForChildDirectedTreatment(TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE);
            } else {
                requestConfigurationBuilder.
                        setTagForChildDirectedTreatment(TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE);
            }
        } else {
            requestConfigurationBuilder.
                    setTagForChildDirectedTreatment(TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED);
        }

        // Publishers may want to mark their requests to receive treatment for users in the
        // European Economic Area (EEA) under the age of consent.
        Boolean underAgeOfConsent = (Boolean) localExtras.get(TAG_FOR_UNDER_AGE_OF_CONSENT_KEY);

        if (underAgeOfConsent != null) {
            if (underAgeOfConsent) {
                requestConfigurationBuilder.
                        setTagForUnderAgeOfConsent(TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE);
            } else {
                requestConfigurationBuilder.
                        setTagForUnderAgeOfConsent(TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE);
            }
        } else {
            requestConfigurationBuilder.
                    setTagForUnderAgeOfConsent(TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED);
        }

        RequestConfiguration requestConfiguration = requestConfigurationBuilder.build();
        MobileAds.setRequestConfiguration(requestConfiguration);

        AdRequest adRequest = builder.build();

        try {
            mGoogleAdView.loadAd(adRequest);

            MoPubLog.log(adUnitId, LOAD_ATTEMPTED, ADAPTER_NAME);
        } catch (NoClassDefFoundError e) {
            // This can be thrown by Play Services on Honeycomb.
            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                    MoPubErrorCode.NETWORK_NO_FILL);

            mBannerListener.onBannerFailed(MoPubErrorCode.NETWORK_NO_FILL);
        }
    }

    @Override
    protected void onInvalidate() {
        Views.removeFromParent(mGoogleAdView);

        if (mGoogleAdView != null) {
            mGoogleAdView.setAdListener(null);
            mGoogleAdView.destroy();
        }
    }

    private void forwardNpaIfSet(AdRequest.Builder builder) {

        // Only forward the "npa" bundle if it is explicitly set. Otherwise, don't attach it with the ad request.
        Bundle npaBundle = GooglePlayServicesAdapterConfiguration.getNpaBundle();

        if (npaBundle != null && !npaBundle.isEmpty()) {
            builder.addNetworkExtrasBundle(AdMobAdapter.class, npaBundle);
        }
    }

    private boolean extrasAreValid(Map<String, String> serverExtras) {
        try {
            Integer.parseInt(serverExtras.get(AD_WIDTH_KEY));
            Integer.parseInt(serverExtras.get(AD_HEIGHT_KEY));
        } catch (NumberFormatException e) {
            return false;
        }

        return serverExtras.containsKey(AD_UNIT_ID_KEY);
    }

    private AdSize calculateAdSize(int width, int height) {
        // Use the smallest AdSize that will properly contain the adView
        if (width <= BANNER.getWidth() && height <= BANNER.getHeight()) {
            return BANNER;
        } else if (width <= MEDIUM_RECTANGLE.getWidth() && height <= MEDIUM_RECTANGLE.getHeight()) {
            return MEDIUM_RECTANGLE;
        } else if (width <= FULL_BANNER.getWidth() && height <= FULL_BANNER.getHeight()) {
            return FULL_BANNER;
        } else if (width <= LEADERBOARD.getWidth() && height <= LEADERBOARD.getHeight()) {
            return LEADERBOARD;
        } else {
            return null;
        }
    }

    private class AdViewListener extends AdListener {
        /*
         * Google Play Services AdListener implementation
         */

        @Override
        public void onAdClosed() {

        }

        @Override
        public void onAdFailedToLoad(int errorCode) {
            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                    getMoPubErrorCode(errorCode).getIntCode(),
                    getMoPubErrorCode(errorCode));

            if (mBannerListener != null) {
                mBannerListener.onBannerFailed(getMoPubErrorCode(errorCode));
            }
        }

        @Override
        public void onAdLeftApplication() {
        }

        @Override
        public void onAdLoaded() {
            MoPubLog.log(LOAD_SUCCESS, ADAPTER_NAME);
            MoPubLog.log(SHOW_ATTEMPTED, ADAPTER_NAME);
            MoPubLog.log(SHOW_SUCCESS, ADAPTER_NAME);

            if (mBannerListener != null) {
                mBannerListener.onBannerLoaded(mGoogleAdView);
            }
        }

        @Override
        public void onAdOpened() {
            MoPubLog.log(CLICKED, ADAPTER_NAME);

            if (mBannerListener != null) {
                mBannerListener.onBannerClicked();
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
}
