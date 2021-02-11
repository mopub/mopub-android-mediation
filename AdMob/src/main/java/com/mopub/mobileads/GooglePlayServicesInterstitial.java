package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.mopub.common.LifecycleListener;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;

import java.util.Collections;
import java.util.Map;

import static com.google.android.gms.ads.RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE;
import static com.google.android.gms.ads.RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE;
import static com.google.android.gms.ads.RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED;
import static com.google.android.gms.ads.RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE;
import static com.google.android.gms.ads.RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE;
import static com.google.android.gms.ads.RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;
import static com.mopub.mobileads.GooglePlayServicesAdapterConfiguration.forwardNpaIfSet;

public class GooglePlayServicesInterstitial extends BaseAd {
    /*
     * These keys are intended for MoPub internal use. Do not modify.
     */
    public static final String AD_UNIT_ID_KEY = "adUnitID";
    public static final String CONTENT_URL_KEY = "contentUrl";
    public static final String TAG_FOR_CHILD_DIRECTED_KEY = "tagForChildDirectedTreatment";
    public static final String TAG_FOR_UNDER_AGE_OF_CONSENT_KEY = "tagForUnderAgeOfConsent";
    public static final String TEST_DEVICES_KEY = "testDevices";

    @NonNull
    private static final String ADAPTER_NAME = GooglePlayServicesInterstitial.class.getSimpleName();
    private final GooglePlayServicesAdapterConfiguration mGooglePlayServicesAdapterConfiguration;
    private InterstitialAd mGoogleInterstitialAd;
    private Context mContext;
    @Nullable
    private String mAdUnitId;

    public GooglePlayServicesInterstitial() {
        mGooglePlayServicesAdapterConfiguration = new GooglePlayServicesAdapterConfiguration();
    }

    @Override
    protected void load(@NonNull final Context context, @NonNull final AdData adData) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(adData);

        mContext = context;

        setAutomaticImpressionAndClickTracking(false);

        final Map<String, String> extras = adData.getExtras();
        if (extrasAreValid(extras)) {
            mAdUnitId = extras.get(AD_UNIT_ID_KEY);

            mGooglePlayServicesAdapterConfiguration.setCachedInitializationParameters(context, extras);
        } else {
            MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                    MoPubErrorCode.NETWORK_NO_FILL);

            if (mLoadListener != null) {
                mLoadListener.onAdLoadFailed(MoPubErrorCode.NETWORK_NO_FILL);
            }

            return;
        }

        final AdRequest.Builder builder = new AdRequest.Builder();
        builder.setRequestAgent("MoPub");

        // Publishers may append a content URL by passing it to the MoPubInterstitial.setLocalExtras() call.
        final String contentUrl = extras.get(CONTENT_URL_KEY);

        if (!TextUtils.isEmpty(contentUrl)) {
            builder.setContentUrl(contentUrl);
        }

        forwardNpaIfSet(builder);

        final RequestConfiguration.Builder requestConfigurationBuilder = new RequestConfiguration.Builder();

        // Publishers may request for test ads by passing test device IDs to the MoPubView.setLocalExtras() call.
        final String testDeviceId = extras.get(TEST_DEVICES_KEY);

        if (!TextUtils.isEmpty(testDeviceId)) {
            requestConfigurationBuilder.setTestDeviceIds(Collections.singletonList(testDeviceId));
        }

        // Publishers may want to indicate that their content is child-directed and forward this
        // information to Google.
        final String childDirected = extras.get(TAG_FOR_CHILD_DIRECTED_KEY);

        if (childDirected != null) {
            if (Boolean.parseBoolean(childDirected)) {
                requestConfigurationBuilder.setTagForChildDirectedTreatment(TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE);
            } else {
                requestConfigurationBuilder.setTagForChildDirectedTreatment(TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE);
            }
        } else {
            requestConfigurationBuilder.setTagForChildDirectedTreatment(TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED);
        }

        // Publishers may want to mark their requests to receive treatment for users in the
        // European Economic Area (EEA) under the age of consent.
        final String underAgeOfConsent = extras.get(TAG_FOR_UNDER_AGE_OF_CONSENT_KEY);

        if (underAgeOfConsent != null) {
            if (Boolean.parseBoolean(underAgeOfConsent)) {
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

        InterstitialAd.load(context, mAdUnitId, adRequest, new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                mGoogleInterstitialAd = interstitialAd;

                MoPubLog.log(getAdNetworkId(), LOAD_SUCCESS, ADAPTER_NAME);

                if (mLoadListener != null) {
                    mLoadListener.onAdLoaded();
                }

                mGoogleInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        if (mInteractionListener != null) {
                            mInteractionListener.onAdDismissed();
                        }

                        mGoogleInterstitialAd = null;
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(AdError adError) {
                        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Failed to show " +
                                "Google interstitial. " + adError.getMessage());

                        MoPubLog.log(getAdNetworkId(), SHOW_FAILED, ADAPTER_NAME,
                                MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                                MoPubErrorCode.NETWORK_NO_FILL);

                        if (mInteractionListener != null) {
                            mInteractionListener.onAdFailed(MoPubErrorCode.NETWORK_NO_FILL);
                        }

                        mGoogleInterstitialAd = null;
                    }

                    @Override
                    public void onAdShowedFullScreenContent() {
                        MoPubLog.log(getAdNetworkId(), SHOW_SUCCESS, ADAPTER_NAME);

                        if (mInteractionListener != null) {
                            mInteractionListener.onAdShown();
                            mInteractionListener.onAdImpression();
                        }
                    }
                });
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                        MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR.getIntCode(),
                        MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
                MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Failed to load Google " +
                        "interstitial. " + loadAdError.getMessage());

                if (mLoadListener != null) {
                    mLoadListener.onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
                }
            }
        });

        MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);
    }

    @Override
    protected void show() {
        MoPubLog.log(getAdNetworkId(), SHOW_ATTEMPTED, ADAPTER_NAME);

        if (mGoogleInterstitialAd != null) {
            if (mContext instanceof Activity) {
                mGoogleInterstitialAd.show((Activity) mContext);
            } else {
                MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Failed to show Google" +
                        " interstitial because context is not an Activity.");

                MoPubLog.log(getAdNetworkId(), SHOW_FAILED, ADAPTER_NAME,
                        MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR.getIntCode(),
                        MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);

                if (mInteractionListener != null) {
                    mInteractionListener.onAdFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
                }
            }
        } else {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Failed to show Google" +
                    " interstitial because it wasn't ready yet.");

            MoPubLog.log(getAdNetworkId(), SHOW_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                    MoPubErrorCode.NETWORK_NO_FILL);

            if (mInteractionListener != null) {
                mInteractionListener.onAdFailed(MoPubErrorCode.NETWORK_NO_FILL);
            }
        }
    }

    @Override
    protected void onInvalidate() {
        if (mGoogleInterstitialAd != null) {
            mGoogleInterstitialAd = null;
        }
    }

    @Nullable
    @Override
    protected LifecycleListener getLifecycleListener() {
        return null;
    }

    private boolean extrasAreValid(Map<String, String> extras) {
        return extras.containsKey(AD_UNIT_ID_KEY);
    }

    @NonNull
    public String getAdNetworkId() {
        return mAdUnitId == null ? "" : mAdUnitId;
    }

    @Override
    protected boolean checkAndInitializeSdk(@NonNull Activity launcherActivity,
                                            @NonNull AdData adData) {
        return false;
    }
}
