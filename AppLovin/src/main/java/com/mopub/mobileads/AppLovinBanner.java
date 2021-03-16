package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.applovin.adview.AppLovinAdView;
import com.applovin.adview.AppLovinAdViewDisplayErrorCode;
import com.applovin.adview.AppLovinAdViewEventListener;
import com.applovin.sdk.AppLovinAd;
import com.applovin.sdk.AppLovinAdClickListener;
import com.applovin.sdk.AppLovinAdDisplayListener;
import com.applovin.sdk.AppLovinAdLoadListener;
import com.applovin.sdk.AppLovinAdSize;
import com.applovin.sdk.AppLovinMediationProvider;
import com.applovin.sdk.AppLovinPrivacySettings;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkSettings;
import com.mopub.common.DataKeys;
import com.mopub.common.LifecycleListener;
import com.mopub.common.MoPub;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;

import java.util.Map;

import static com.mopub.common.DataKeys.ADUNIT_FORMAT;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM_WITH_THROWABLE;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;

public class AppLovinBanner extends BaseAd {

    private static final String ADAPTER_NAME = AppLovinBanner.class.getSimpleName();
    private static final Handler UI_HANDLER = new Handler(Looper.getMainLooper());
    private static final String ZONE_ID_EXTRAS_KEY = "zone_id";

    private String mZoneId;
    private AppLovinAdView mAdView;

    @NonNull
    private AppLovinAdapterConfiguration mAppLovinAdapterConfiguration;
    //
    // MoPub Custom Event Methods
    //

    public AppLovinBanner() {
        mAppLovinAdapterConfiguration = new AppLovinAdapterConfiguration();
    }

    @Override
    protected void load(@NonNull final Context context, @NonNull final AdData adData) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(adData);

        final Map<String, String> extras = adData.getExtras();
        if (extras.isEmpty()) {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "No extras provided");
            if (mLoadListener != null) {
                mLoadListener.onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            }
            return;
        }

        mZoneId = extras.get(ZONE_ID_EXTRAS_KEY);
        // Pass the user consent from the MoPub SDK to AppLovin as per GDPR
        boolean canCollectPersonalInfo = MoPub.canCollectPersonalInformation();
        AppLovinPrivacySettings.setHasUserConsent(canCollectPersonalInfo, context);

        String adUnitFormat = extras.get(ADUNIT_FORMAT);
        if (!TextUtils.isEmpty(adUnitFormat)) {
            adUnitFormat = adUnitFormat.toLowerCase();
        }

        final boolean isBannerFormat = "banner".equals(adUnitFormat);
        if (!isBannerFormat) {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME,
                    "AppLovin only supports 320*50 and 728*90 sized ads. " +
                            "Please ensure your MoPub adunit's format is Banner.");
            MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR.getIntCode(),
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);

            if (mLoadListener != null) {
                mLoadListener.onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            }

            return;
        }

        final AppLovinAdSize adSize = appLovinAdSizeFromAdData(adData);
        if (adSize != null) {
            final String adMarkup = extras.get(DataKeys.ADM_KEY);
            final boolean hasAdMarkup = !TextUtils.isEmpty(adMarkup);

            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Requesting AppLovin banner with extras: " +
                    extras + " and has ad markup: " + hasAdMarkup);

            AppLovinSdk sdk = retrieveSdk(context);

            if (sdk == null) {
                MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "AppLovinSdk instance is null likely because " +
                        "no AppLovin SDK key is available. Failing ad request.");
                MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                        MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR.getIntCode(),
                        MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);

                if (mLoadListener != null) {
                    mLoadListener.onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
                }

                return;
            }

            sdk.setMediationProvider(AppLovinMediationProvider.MOPUB);
            sdk.setPluginVersion(AppLovinAdapterConfiguration.APPLOVIN_PLUGIN_VERSION);

            mAppLovinAdapterConfiguration.setCachedInitializationParameters(context, extras);

            mAdView = new AppLovinAdView(sdk, adSize, context);
            mAdView.setAdDisplayListener(new AppLovinAdDisplayListener() {
                @Override
                public void adDisplayed(final AppLovinAd ad) {
                    MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Banner displayed");
                }

                @Override
                public void adHidden(final AppLovinAd ad) {
                    MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Banner dismissed");
                }
            });
            mAdView.setAdClickListener(new AppLovinAdClickListener() {
                @Override
                public void adClicked(final AppLovinAd ad) {
                    MoPubLog.log(getAdNetworkId(), CLICKED, ADAPTER_NAME);

                    if (mInteractionListener != null) {
                        mInteractionListener.onAdClicked();
                    }
                }
            });


            mAdView.setAdViewEventListener(new AppLovinAdViewEventListener() {
                @Override
                public void adOpenedFullscreen(final AppLovinAd appLovinAd, final AppLovinAdView appLovinAdView) {
                    MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Banner opened fullscreen");

                    if (mInteractionListener != null) {
                        mInteractionListener.onAdExpanded();
                    }
                }

                @Override
                public void adClosedFullscreen(final AppLovinAd appLovinAd, final AppLovinAdView appLovinAdView) {
                    MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Banner closed fullscreen");

                    if (mInteractionListener != null) {
                        mInteractionListener.onAdCollapsed();
                    }
                }

                @Override
                public void adLeftApplication(final AppLovinAd appLovinAd, final AppLovinAdView appLovinAdView) {
                    MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Banner left application");
                }

                @Override
                public void adFailedToDisplay(final AppLovinAd appLovinAd, final AppLovinAdView appLovinAdView, final AppLovinAdViewDisplayErrorCode appLovinAdViewDisplayErrorCode) {
                }
            });

            final AppLovinAdLoadListener adLoadListener = new AppLovinAdLoadListener() {
                @Override
                public void adReceived(final AppLovinAd ad) {
                    // Ensure logic is ran on main queue
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            MoPubLog.log(getAdNetworkId(), LOAD_SUCCESS, ADAPTER_NAME);
                            MoPubLog.log(getAdNetworkId(), SHOW_ATTEMPTED, ADAPTER_NAME);

                            mAdView.renderAd(ad);
                            MoPubLog.log(getAdNetworkId(), SHOW_SUCCESS, ADAPTER_NAME);

                            try {
                                if (mLoadListener != null) {
                                    mLoadListener.onAdLoaded();
                                }
                            } catch (Throwable th) {
                                MoPubLog.log(getAdNetworkId(), CUSTOM_WITH_THROWABLE, "Unable to notify listener " +
                                        "of successful ad load.", th);
                            }
                        }
                    });
                }

                @Override
                public void failedToReceiveAd(final int errorCode) {
                    // Ensure logic is ran on main queue
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Failed to load banner ad with code: ",
                                    errorCode);
                            MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                                    AppLovinAdapterConfiguration.getMoPubErrorCode(errorCode).getIntCode(),
                                    AppLovinAdapterConfiguration.getMoPubErrorCode(errorCode));
                            try {
                                if (mLoadListener != null) {
                                    mLoadListener.onAdLoadFailed(AppLovinAdapterConfiguration.getMoPubErrorCode(errorCode));
                                }
                            } catch (Throwable th) {
                                MoPubLog.log(getAdNetworkId(), CUSTOM_WITH_THROWABLE, "Unable to notify " +
                                        "listener of failure to receive ad.", th);
                            }
                        }
                    });
                }
            };

            if (hasAdMarkup) {
                sdk.getAdService().loadNextAdForAdToken(adMarkup, adLoadListener);
                MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);
            } else {
                // Determine zone
                if (!TextUtils.isEmpty(mZoneId)) {
                    sdk.getAdService().loadNextAdForZoneId(mZoneId, adLoadListener);
                    MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);
                } else {
                    sdk.getAdService().loadNextAd(adSize, adLoadListener);
                    MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);
                }
            }
        } else {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Unable to request AppLovin banner");

            MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR.getIntCode(),
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);

            if (mLoadListener != null) {
                mLoadListener.onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            }
        }
    }

    @Nullable
    @Override
    protected View getAdView() {
        return mAdView;
    }

    @Override
    protected void onInvalidate() {
    }

    @Nullable
    @Override
    protected LifecycleListener getLifecycleListener() {
        return null;
    }

    private AppLovinAdSize appLovinAdSizeFromAdData(@NonNull final AdData adData) {
        // Default to standard banner size
        AppLovinAdSize adSize = AppLovinAdSize.BANNER;

        try {
            final int width = adData.getAdWidth() != null ? adData.getAdWidth() : 0;
            final int height = adData.getAdHeight() != null ? adData.getAdHeight() : 0;

            if (width > 0 && height > 0) {
                // Size can contain an AppLovin leaderboard ad size of 728x90
                if (width >= 728 && height >= 90) {
                    adSize = AppLovinAdSize.LEADER;
                }
            } else {
                MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Invalid width (" + width + ") and height " +
                        "(" + height + ") provided");
            }
        } catch (Throwable th) {
            MoPubLog.log(getAdNetworkId(), CUSTOM_WITH_THROWABLE, "Encountered error while parsing width and " +
                    "height from extras", th);
        }

        return adSize;
    }

    @NonNull
    public String getAdNetworkId() {
        return mZoneId == null ? "" : mZoneId;
    }

    @Override
    protected boolean checkAndInitializeSdk(@NonNull final Activity launcherActivity,
                                            @NonNull final AdData adData) {
        return false;
    }

    //
    // Utility Methods
    //

    /**
     * Retrieves the appropriate instance of AppLovin's SDK from the SDK key. This check prioritizes
     * the SDK Key in the AndroidManifest, and only uses the one passed in to the AdapterConfiguration
     * if the former is not available.
     */
    private static AppLovinSdk retrieveSdk(final Context context) {

        if (!AppLovinAdapterConfiguration.androidManifestContainsValidSdkKey(context)) {
            final String sdkKey = AppLovinAdapterConfiguration.getSdkKey();

            return !TextUtils.isEmpty(sdkKey)
                    ? AppLovinSdk.getInstance(sdkKey, new AppLovinSdkSettings(), context)
                    : null;
        } else {
            return AppLovinSdk.getInstance(context);
        }
    }

    /**
     * Performs the given runnable on the main thread.
     */
    private static void runOnUiThread(final Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            UI_HANDLER.post(runnable);
        }
    }
}
