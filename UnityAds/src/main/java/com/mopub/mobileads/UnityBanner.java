package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.LifecycleListener;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;

import com.unity3d.ads.IUnityAdsInitializationListener;
import com.unity3d.ads.UnityAds;
import com.unity3d.services.banners.BannerErrorInfo;
import com.unity3d.services.banners.BannerView;
import com.unity3d.services.banners.UnityBannerSize;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.WILL_LEAVE_APPLICATION;

public class UnityBanner extends BaseAd implements BannerView.IListener {

    private static final String ADAPTER_NAME = UnityBanner.class.getSimpleName();

    private String placementId = "banner";
    private BannerView mBannerView;

    @NonNull
    private UnityAdsAdapterConfiguration mUnityAdsAdapterConfiguration;

    public UnityBanner() {
        mUnityAdsAdapterConfiguration = new UnityAdsAdapterConfiguration();
    }

    @Override
    protected void load(@NonNull final Context context, @NonNull final AdData adData) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(adData);

        if (!(context instanceof Activity)) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Failing Unity Ads banner ad request as the context is not an Activity.");
            MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR.getIntCode(),
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);

            if (mLoadListener != null) {
                mLoadListener.onAdLoadFailed(MoPubErrorCode.NETWORK_NO_FILL);
            }
            return;
        }

        setAutomaticImpressionAndClickTracking(true);

        final Map<String, String> extras = adData.getExtras();
        mUnityAdsAdapterConfiguration.setCachedInitializationParameters(context, extras);

        placementId = UnityRouter.placementIdForServerExtras(extras, placementId);

        final String format = extras.get("adunit_format");
        final boolean isMediumRectangleFormat = format.contains("medium_rectangle");

        if (isMediumRectangleFormat) {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Unity Ads does not support medium rectangle ads.");

            if (mLoadListener != null) {
                mLoadListener.onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            }
            return;
        }

        final BannerView.IListener bannerlistener = this;

        if (!UnityAds.isInitialized()) {
            UnityRouter.initUnityAds(extras, context, new IUnityAdsInitializationListener() {
                @Override
                public void onInitializationComplete() {
                    MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unity Ads successfully initialized.");
                }

                @Override
                public void onInitializationFailed(UnityAds.UnityAdsInitializationError unityAdsInitializationError, String errorMessage) {
                    if (errorMessage != null) {
                        MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unity Ads failed to initialize initialize with message: " + errorMessage);
                    }
                }
            });

            if (mLoadListener != null) {
                MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME,
                        "Unity Ads adapter failed to request banner ad, Unity Ads is not initialized yet. " +
                                "Failing this ad request and calling Unity Ads initialization, " +
                                "so it would be available for an upcoming ad request");
                mLoadListener.onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            }

            return;
        }

        final UnityBannerSize bannerSize = unityAdsAdSizeFromAdData(adData);

        if (mBannerView != null) {
            mBannerView.destroy();
            mBannerView = null;
        }

        MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);

        mBannerView = new BannerView((Activity) context, placementId, bannerSize);
        mBannerView.setListener(bannerlistener);
        mBannerView.load();
    }

    @Override
    @Nullable
    public View getAdView() {
        return mBannerView;
    }

    private UnityBannerSize unityAdsAdSizeFromAdData(@NonNull final AdData adData) {

        int adWidth = adData.getAdWidth() != null ? adData.getAdWidth() : 0;
        int adHeight = adData.getAdHeight() != null ? adData.getAdHeight() : 0;

        if (adWidth >= 728 && adHeight >= 90) {
            return new UnityBannerSize(728, 90);
        } else if (adWidth >= 468 && adHeight >= 60) {
            return new UnityBannerSize(468, 60);
        } else {
            return new UnityBannerSize(320, 50);
        }

    }

    @Override
    protected void onInvalidate() {
        if (mBannerView != null) {
            mBannerView.destroy();
        }

        mBannerView = null;
    }

    @Nullable
    @Override
    protected LifecycleListener getLifecycleListener() {
        return null;
    }

    @Override
    public void onBannerLoaded(BannerView bannerView) {
        MoPubLog.log(getAdNetworkId(), LOAD_SUCCESS, ADAPTER_NAME);
        MoPubLog.log(getAdNetworkId(), SHOW_ATTEMPTED, ADAPTER_NAME);
        MoPubLog.log(getAdNetworkId(), SHOW_SUCCESS, ADAPTER_NAME);

        if (mLoadListener != null) {
            mLoadListener.onAdLoaded();
            mBannerView = bannerView;
        }

        if (mInteractionListener != null) {
            mInteractionListener.onAdImpression();
        }
    }

    @Override
    public void onBannerClick(BannerView bannerView) {
        MoPubLog.log(getAdNetworkId(), CLICKED, ADAPTER_NAME);

        if (mInteractionListener != null) {
            mInteractionListener.onAdClicked();
        }
    }

    @Override
    public void onBannerFailedToLoad(BannerView bannerView, BannerErrorInfo errorInfo) {
        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, String.format("Banner did error for placement %s with error %s",
                placementId, errorInfo.errorMessage));

        if (mLoadListener != null) {
            mLoadListener.onAdLoadFailed(MoPubErrorCode.NETWORK_NO_FILL);
        }
    }

    @Override
    public void onBannerLeftApplication(BannerView bannerView) {
        MoPubLog.log(getAdNetworkId(), WILL_LEAVE_APPLICATION, ADAPTER_NAME);
    }

    @NonNull
    @Override
    public String getAdNetworkId() {
        return placementId != null ? placementId : "";
    }

    @Override
    protected boolean checkAndInitializeSdk(@NonNull final Activity launcherActivity,
                                            @NonNull final AdData adData) {
        return false;
    }
}
