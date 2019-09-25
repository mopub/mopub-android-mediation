package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;

import com.mopub.common.logging.MoPubLog;
import com.unity3d.services.banners.BannerErrorInfo;
import com.unity3d.services.banners.BannerView;
import com.unity3d.services.banners.UnityBannerSize;

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

public class UnityBanner extends CustomEventBanner implements BannerView.IListener{

    private static final String ADAPTER_NAME = UnityBanner.class.getSimpleName();

    private String placementId = "banner";
    private CustomEventBannerListener customEventBannerListener;
    private BannerView mBannerView;

    @NonNull
    private UnityAdsAdapterConfiguration mUnityAdsAdapterConfiguration;

    public UnityBanner() {
        mUnityAdsAdapterConfiguration = new UnityAdsAdapterConfiguration();
    }

    @Override
    protected void loadBanner(Context context, CustomEventBannerListener customEventBannerListener,
                              Map<String, Object> localExtras, Map<String, String> serverExtras) {
        if (!(context instanceof Activity)) {
            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                    MoPubErrorCode.NETWORK_NO_FILL);

            customEventBannerListener.onBannerFailed(MoPubErrorCode.NETWORK_NO_FILL);
            return;
        }

        mUnityAdsAdapterConfiguration.setCachedInitializationParameters(context, serverExtras);

        placementId = UnityRouter.placementIdForServerExtras(serverExtras, placementId);
        this.customEventBannerListener = customEventBannerListener;

        if (UnityRouter.initUnityAds(serverExtras, (Activity) context)) {
            if(localExtras.containsKey(AD_WIDTH) && localExtras.containsKey(AD_WIDTH)) {
                UnityBannerSize bannerSize = new UnityBannerSize((int)localExtras.get(AD_WIDTH), (int)localExtras.get(AD_HEIGHT));
                
                if(mBannerView != null) {
                    mBannerView.destroy();
                    mBannerView = null;
                }
                mBannerView = new BannerView((Activity)context, placementId, bannerSize);
                mBannerView.setListener(this);
                mBannerView.load();

                MoPubLog.log(placementId, LOAD_ATTEMPTED, ADAPTER_NAME);
            } else {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "Failed to get banner size");
                if (customEventBannerListener != null) {
                    customEventBannerListener.onBannerFailed(MoPubErrorCode.NETWORK_NO_FILL);
                }
            }

        } else {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Failed to initialize Unity Ads");
            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                    MoPubErrorCode.NETWORK_NO_FILL);

            if (customEventBannerListener != null) {
                customEventBannerListener.onBannerFailed(MoPubErrorCode.NETWORK_NO_FILL);
            }
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
        MoPubLog.log(LOAD_SUCCESS, ADAPTER_NAME);
        MoPubLog.log(SHOW_ATTEMPTED, ADAPTER_NAME);
        MoPubLog.log(SHOW_SUCCESS, ADAPTER_NAME);

        if (customEventBannerListener != null) {
            customEventBannerListener.onBannerLoaded(bannerView);
            mBannerView = bannerView;
        }
    }

    @Override
    public void onBannerClick(BannerView bannerView) {
        MoPubLog.log(CLICKED, ADAPTER_NAME);

        if (customEventBannerListener != null) {
            customEventBannerListener.onBannerClicked();
        }
    }

    @Override
    public void onBannerFailedToLoad(BannerView bannerView, BannerErrorInfo errorInfo) {
        MoPubLog.log(CUSTOM, ADAPTER_NAME, String.format("Banner did error for placement %s with error %s",
                placementId, errorInfo.errorMessage));

        if (customEventBannerListener != null) {
            customEventBannerListener.onBannerFailed(MoPubErrorCode.NETWORK_NO_FILL);
        }
    }

    @Override
    public void onBannerLeftApplication(BannerView bannerView) {
        MoPubLog.log(WILL_LEAVE_APPLICATION, ADAPTER_NAME);
        if (customEventBannerListener != null) {
            customEventBannerListener.onLeaveApplication();
        }
    }
}
