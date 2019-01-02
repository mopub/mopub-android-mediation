package com.mopub.mobileads;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.mopub.common.MediationSettings;
import com.mopub.common.util.Views;

import java.util.Map;

import static com.google.android.gms.ads.AdSize.BANNER;
import static com.google.android.gms.ads.AdSize.FULL_BANNER;
import static com.google.android.gms.ads.AdSize.LEADERBOARD;
import static com.google.android.gms.ads.AdSize.MEDIUM_RECTANGLE;

public class GooglePlayServicesBanner extends CustomEventBanner {
    /*
     * These keys are intended for MoPub internal use. Do not modify.
     */
    public static final String AD_UNIT_ID_KEY = "adUnitID";
    public static final String AD_WIDTH_KEY = "adWidth";
    public static final String AD_HEIGHT_KEY = "adHeight";
    public static final String CONTENT_URL_KEY = "contentUrl";
    public static final String TEST_DEVICES_KEY = "testDevices";

    private CustomEventBannerListener mBannerListener;
    private AdView mGoogleAdView;

    @Override
    protected void loadBanner(
            final Context context,
            final CustomEventBannerListener customEventBannerListener,
            final Map<String, Object> localExtras,
            final Map<String, String> serverExtras) {
        mBannerListener = customEventBannerListener;
        final String adUnitId;
        final int adWidth;
        final int adHeight;


        if (extrasAreValid(serverExtras)) {
            adUnitId = serverExtras.get(AD_UNIT_ID_KEY);
            adWidth = Integer.parseInt(serverExtras.get(AD_WIDTH_KEY));
            adHeight = Integer.parseInt(serverExtras.get(AD_HEIGHT_KEY));
        } else {
            mBannerListener.onBannerFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            return;
        }

        mGoogleAdView = new AdView(context);
        mGoogleAdView.setAdListener(new AdViewListener());
        mGoogleAdView.setAdUnitId(adUnitId);

        final AdSize adSize = calculateAdSize(adWidth, adHeight);
        if (adSize == null) {
            mBannerListener.onBannerFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            return;
        }

        mGoogleAdView.setAdSize(adSize);

        AdRequest.Builder builder = new AdRequest.Builder();
        builder.setRequestAgent("MoPub");

        // Publishers may append a content URL by passing it to the MoPubView.setLocalExtras() call.
        if (localExtras.get(CONTENT_URL_KEY) != null) {
            String contentUrl = localExtras.get(CONTENT_URL_KEY).toString();
            if (!TextUtils.isEmpty(contentUrl)) {
                builder.setContentUrl(contentUrl);
            }
        }

        // Publishers may request for test ads by passing test device IDs to the MoPubView.setLocalExtras() call.
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
            mGoogleAdView.loadAd(adRequest);
        } catch (NoClassDefFoundError e) {
            // This can be thrown by Play Services on Honeycomb.
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
        if (GooglePlayServicesMediationSettings.getNpaBundle() != null &&
                !GooglePlayServicesMediationSettings.getNpaBundle().isEmpty()) {
            builder.addNetworkExtrasBundle(AdMobAdapter.class, GooglePlayServicesMediationSettings.getNpaBundle());
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
            Log.d("MoPub", "Google Play Services banner ad failed to load.");
            if (mBannerListener != null) {
                mBannerListener.onBannerFailed(getMoPubErrorCode(errorCode));
            }
        }

        @Override
        public void onAdLeftApplication() {

        }

        @Override
        public void onAdLoaded() {
            Log.d("MoPub", "Google Play Services banner ad loaded successfully. Showing ad...");
            if (mBannerListener != null) {
                mBannerListener.onBannerLoaded(mGoogleAdView);
            }
        }

        @Override
        public void onAdOpened() {
            Log.d("MoPub", "Google Play Services banner ad clicked.");
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
    AdView getGoogleAdView() {
        return mGoogleAdView;
    }
}