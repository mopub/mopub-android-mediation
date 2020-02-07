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
import com.mopub.common.DataKeys;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.Views;

import java.util.Map;

import static com.google.android.gms.ads.AdSize.BANNER;
import static com.google.android.gms.ads.AdSize.FULL_BANNER;
import static com.google.android.gms.ads.AdSize.LARGE_BANNER;
import static com.google.android.gms.ads.AdSize.LEADERBOARD;
import static com.google.android.gms.ads.AdSize.MEDIUM_RECTANGLE;
import static com.google.android.gms.ads.AdSize.WIDE_SKYSCRAPER;
import static com.google.android.gms.ads.RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE;
import static com.google.android.gms.ads.RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE;
import static com.google.android.gms.ads.RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED;
import static com.google.android.gms.ads.RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE;
import static com.google.android.gms.ads.RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE;
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
    public static final String AD_UNIT_ID_KEY = "adUnitID";
    public static final String CONTENT_URL_KEY = "contentUrl";
    public static final String TAG_FOR_CHILD_DIRECTED_KEY = "tagForChildDirectedTreatment";
    public static final String TAG_FOR_UNDER_AGE_OF_CONSENT_KEY = "tagForUnderAgeOfConsent";
    public static final String TEST_DEVICES_KEY = "testDevices";

    private static final String ADAPTER_NAME = GooglePlayServicesBanner.class.getSimpleName();
    private CustomEventBannerListener mBannerListener;
    private AdView mGoogleAdView;
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

        mGoogleAdView = new AdView(context);
        mGoogleAdView.setAdListener(new AdViewListener());
        mGoogleAdView.setAdUnitId(mAdUnitId);

        final AdSize adSize = (adWidth == null || adHeight == null)
                ? null
                : calculateAdSize(adWidth, adHeight);

        if (adSize != null) {
            mGoogleAdView.setAdSize(adSize);
        } else {
            MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                    MoPubErrorCode.NETWORK_NO_FILL);

            mBannerListener.onBannerFailed(MoPubErrorCode.NETWORK_NO_FILL);
            return;
        }

        final AdRequest.Builder builder = new AdRequest.Builder();
        builder.setRequestAgent("MoPub");

        // Publishers may append a content URL by passing it to the MoPubView.setLocalExtras() call.
        final String contentUrl = (String) localExtras.get(CONTENT_URL_KEY);

        if (!TextUtils.isEmpty(contentUrl)) {
            builder.setContentUrl(contentUrl);
        }

        // Publishers may request for test ads by passing test device IDs to the MoPubView.setLocalExtras() call.
        final String testDeviceId = (String) localExtras.get(TEST_DEVICES_KEY);

        if (!TextUtils.isEmpty(testDeviceId)) {
            builder.addTestDevice(testDeviceId);
        }

        // Consent collected from the MoPub’s consent dialogue should not be used to set up
        // Google's personalization preference. Publishers should work with Google to be GDPR-compliant.
        forwardNpaIfSet(builder);

        final RequestConfiguration.Builder requestConfigurationBuilder = new RequestConfiguration.Builder();

        // Publishers may want to indicate that their content is child-directed and forward this
        // information to Google.
        final Boolean childDirected = (Boolean) localExtras.get(TAG_FOR_CHILD_DIRECTED_KEY);

        if (childDirected != null) {
            if (childDirected) {
                requestConfigurationBuilder.setTagForChildDirectedTreatment(TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE);
            } else {
                requestConfigurationBuilder.setTagForChildDirectedTreatment(TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE);
            }
        } else {
            requestConfigurationBuilder.setTagForChildDirectedTreatment(TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED);
        }

        // Publishers may want to mark their requests to receive treatment for users in the
        // European Economic Area (EEA) under the age of consent.
        final Boolean underAgeOfConsent = (Boolean) localExtras.get(TAG_FOR_UNDER_AGE_OF_CONSENT_KEY);

        if (underAgeOfConsent != null) {
            if (underAgeOfConsent) {
                requestConfigurationBuilder.setTagForUnderAgeOfConsent(TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE);
            } else {
                requestConfigurationBuilder.setTagForUnderAgeOfConsent(TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE);
            }
        } else {
            requestConfigurationBuilder.setTagForUnderAgeOfConsent(TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED);
        }

        final RequestConfiguration requestConfiguration = requestConfigurationBuilder.build();
        MobileAds.setRequestConfiguration(requestConfiguration);

        final AdRequest adRequest = builder.build();

        try {
            mGoogleAdView.loadAd(adRequest);

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
        Views.removeFromParent(mGoogleAdView);

        if (mGoogleAdView != null) {
            mGoogleAdView.setAdListener(null);
            mGoogleAdView.destroy();
        }
    }

    private void forwardNpaIfSet(AdRequest.Builder builder) {

        // Only forward the "npa" bundle if it is explicitly set. Otherwise, don't attach it with the ad request.
        final Bundle npaBundle = GooglePlayServicesAdapterConfiguration.getNpaBundle();

        if (npaBundle != null && !npaBundle.isEmpty()) {
            builder.addNetworkExtrasBundle(AdMobAdapter.class, npaBundle);
        }
    }

    private static AdSize calculateAdSize(int width, int height) {
        // Use the largest AdSize that fits into MoPubView
        if (height >= WIDE_SKYSCRAPER.getHeight() && width >= WIDE_SKYSCRAPER.getWidth()) {
            return WIDE_SKYSCRAPER;
        } else if (height >= MEDIUM_RECTANGLE.getHeight() && width >= MEDIUM_RECTANGLE.getWidth()) {
            return MEDIUM_RECTANGLE;
        } else if (height >= LARGE_BANNER.getHeight() && width >= LARGE_BANNER.getWidth()) {
            return LARGE_BANNER;
        } else if (height >= LEADERBOARD.getHeight() && width >= LEADERBOARD.getWidth()) {
            return LEADERBOARD;
        } else if (height >= FULL_BANNER.getHeight() && width >= FULL_BANNER.getWidth()) {
            return FULL_BANNER;
        } else if (height >= BANNER.getHeight() && width >= BANNER.getWidth()) {
            return BANNER;
        } else {
            return null;
        }
    }

    private static String getAdNetworkId() {
        return mAdUnitId;
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
            MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
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
            MoPubLog.log(getAdNetworkId(), LOAD_SUCCESS, ADAPTER_NAME);
            MoPubLog.log(getAdNetworkId(), SHOW_ATTEMPTED, ADAPTER_NAME);
            MoPubLog.log(getAdNetworkId(), SHOW_SUCCESS, ADAPTER_NAME);

            if (mBannerListener != null) {
                mBannerListener.onBannerLoaded(mGoogleAdView);
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
