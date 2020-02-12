package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;

import android.view.View;

import com.mopub.common.logging.MoPubLog;
import com.unity3d.services.banners.BannerErrorInfo;
import com.unity3d.services.banners.BannerView;
import com.unity3d.services.banners.UnityBannerSize;
import com.unity3d.services.core.misc.ViewUtilities;

import java.util.Map;

import static com.mopub.common.DataKeys.AD_HEIGHT;
import static com.mopub.common.DataKeys.AD_WIDTH;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.WILL_LEAVE_APPLICATION;

public class UnityBanner extends CustomEventBanner implements BannerView.IListener {

    private static final String ADAPTER_NAME = UnityBanner.class.getSimpleName();

    private static String placementId = "banner";
    private CustomEventBannerListener customEventBannerListener;
    private BannerView mBannerView;
    private int adWidth, adHeight;

    @NonNull
    private UnityAdsAdapterConfiguration mUnityAdsAdapterConfiguration;

    public UnityBanner() {
        mUnityAdsAdapterConfiguration = new UnityAdsAdapterConfiguration();
    }

    @Override
    protected void loadBanner(Context context, CustomEventBannerListener customEventBannerListener,
                              Map<String, Object> localExtras, Map<String, String> serverExtras) {
        if (!(context instanceof Activity)) {
            MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR.getIntCode(),
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);

            customEventBannerListener.onBannerFailed(MoPubErrorCode.NETWORK_NO_FILL);
            return;
        }

        mUnityAdsAdapterConfiguration.setCachedInitializationParameters(context, serverExtras);

        placementId = UnityRouter.placementIdForServerExtras(serverExtras, placementId);
        this.customEventBannerListener = customEventBannerListener;

        final String format = serverExtras.get("adunit_format");
        final boolean isMediumRectangleFormat = format.contains("medium_rectangle") ? true : false;

        if (isMediumRectangleFormat) {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Unity Ads does not support medium rectangle ads.");

            if (customEventBannerListener != null) {
                customEventBannerListener.onBannerFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            }

            return;
        }

        if (UnityRouter.initUnityAds(serverExtras, (Activity) context)) {
            if (localExtras == null || localExtras.isEmpty()) {
                MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Failed to get banner size because the " +
                        "localExtras is empty.");

                if (customEventBannerListener != null) {
                    customEventBannerListener.onBannerFailed(MoPubErrorCode.NETWORK_NO_FILL);
                }
            } else {
               final UnityBannerSize bannerSize = unityAdsAdSizeFromLocalExtras(context, localExtras);

                if (mBannerView != null) {
                    mBannerView.destroy();
                    mBannerView = null;
                }
				
                mBannerView = new BannerView((Activity) context, placementId, bannerSize);
                mBannerView.setListener(this);
                mBannerView.load();

                MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);
            }
        } else {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Failed to initialize Unity Ads");
            MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                    MoPubErrorCode.NETWORK_NO_FILL);

            if (customEventBannerListener != null) {
                customEventBannerListener.onBannerFailed(MoPubErrorCode.NETWORK_NO_FILL);
            }
        }
    }

    private UnityBannerSize unityAdsAdSizeFromLocalExtras(Context context, final Map<String, Object> localExtras) {

        final Object adWidthObject = localExtras.get(AD_WIDTH);
        if (adWidthObject instanceof Integer) {
            adWidth = (int) adWidthObject;
        }

        final Object adHeightObject = localExtras.get(AD_HEIGHT);
        if (adHeightObject instanceof Integer) {
            adHeight = (int) adHeightObject;
        }

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
        customEventBannerListener = null;
    }

    @Override
    public void onBannerLoaded(BannerView bannerView) {
        MoPubLog.log(getAdNetworkId(), LOAD_SUCCESS, ADAPTER_NAME);
        MoPubLog.log(getAdNetworkId(), SHOW_ATTEMPTED, ADAPTER_NAME);
        MoPubLog.log(getAdNetworkId(), SHOW_SUCCESS, ADAPTER_NAME);

        if (customEventBannerListener != null) {
            customEventBannerListener.onBannerLoaded(bannerView);
            mBannerView = bannerView;
        }
    }

    @Override
    public void onBannerClick(BannerView bannerView) {
        MoPubLog.log(getAdNetworkId(), CLICKED, ADAPTER_NAME);

        if (customEventBannerListener != null) {
            customEventBannerListener.onBannerClicked();
        }
    }

    @Override
    public void onBannerFailedToLoad(BannerView bannerView, BannerErrorInfo errorInfo) {
        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, String.format("Banner did error for placement %s with error %s",
                placementId, errorInfo.errorMessage));

        if (customEventBannerListener != null) {
            customEventBannerListener.onBannerFailed(MoPubErrorCode.NETWORK_NO_FILL);
        }
    }

    @Override
    public void onBannerLeftApplication(BannerView bannerView) {
        MoPubLog.log(getAdNetworkId(), WILL_LEAVE_APPLICATION, ADAPTER_NAME);
		
        if (customEventBannerListener != null) {
            customEventBannerListener.onLeaveApplication();
        }
    }

    private static String getAdNetworkId() {
        return placementId;
    }
}
