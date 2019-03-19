package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;

import com.mopub.common.logging.MoPubLog;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.mediation.IUnityAdsExtendedListener;
import com.unity3d.ads.metadata.MetaData;
import com.unity3d.services.banners.IUnityBannerListener;
import com.unity3d.services.banners.UnityBanners;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;

public class UnityBanner extends CustomEventBanner implements IUnityBannerListener, IUnityAdsExtendedListener {

    private static final String ADAPTER_NAME = UnityBanner.class.getSimpleName();

    private Context context;
    private String placementId = "banner";
    private CustomEventBannerListener customEventBannerListener;
    private View bannerView;
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

        initNoRefreshMetaData(context);

        mUnityAdsAdapterConfiguration.setCachedInitializationParameters(context, serverExtras);

        placementId = UnityRouter.placementIdForServerExtras(serverExtras, placementId);
        this.customEventBannerListener = customEventBannerListener;
        this.context = context;
        UnityRouter.getBannerRouter().setCurrentPlacementId(placementId);

        if (UnityRouter.initUnityAds(serverExtras, (Activity) context)) {
            UnityRouter.getBannerRouter().addListener(placementId, this);
            // Bug: banner ready events go through the interstitial router atm.
            UnityRouter.getInterstitialRouter().addListener(placementId, this);

            if (UnityAds.isReady(placementId)) {
                UnityBanners.loadBanner((Activity) context, placementId);

                MoPubLog.log(placementId, LOAD_ATTEMPTED, ADAPTER_NAME);
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

    private void initNoRefreshMetaData(Context context) {
        MetaData metaData = new MetaData(context);
        metaData.set("banner.refresh", false);
        metaData.commit();
    }

    @Override
    protected void onInvalidate() {
        UnityRouter.getBannerRouter().removeListener(placementId);
        UnityRouter.getInterstitialRouter().removeListener(placementId);
        UnityBanners.destroy();

        bannerView = null;
        customEventBannerListener = null;
    }

    @Override
    public void onUnityBannerLoaded(String placementId, View view) {
        MoPubLog.log(LOAD_SUCCESS, ADAPTER_NAME);
        MoPubLog.log(SHOW_ATTEMPTED, ADAPTER_NAME);
        MoPubLog.log(SHOW_SUCCESS, ADAPTER_NAME);

        if (customEventBannerListener != null) {
            customEventBannerListener.onBannerLoaded(view);
            this.bannerView = view;
        }
    }

    @Override
    public void onUnityBannerUnloaded(String placementId) {
        MoPubLog.log(CUSTOM, ADAPTER_NAME, String.format("Banner did unload for placement %s", placementId));
    }

    @Override
    public void onUnityBannerShow(String placementId) {
        MoPubLog.log(CUSTOM, ADAPTER_NAME, String.format("Banner did show for placement %s", placementId));

        if (customEventBannerListener != null) {
            customEventBannerListener.onBannerImpression();
        }
    }

    @Override
    public void onUnityBannerClick(String placementId) {
        MoPubLog.log(CLICKED, ADAPTER_NAME);

        if (customEventBannerListener != null) {
            customEventBannerListener.onBannerClicked();
        }
    }

    @Override
    public void onUnityBannerHide(String placementIds) {
        MoPubLog.log(CUSTOM, ADAPTER_NAME, String.format("Banner did hide for placement %s", placementIds));
    }

    @Override
    public void onUnityBannerError(String message) {
        MoPubLog.log(CUSTOM, ADAPTER_NAME, String.format("Banner did error for placement %s with error %s",
                placementId, message));

        if (customEventBannerListener != null) {
            customEventBannerListener.onBannerFailed(MoPubErrorCode.NETWORK_NO_FILL);
        }
    }

    @Override
    public void onUnityAdsClick(String placementId) {

    }

    @Override
    public void onUnityAdsPlacementStateChanged(String placementId, UnityAds.PlacementState placementState, UnityAds.PlacementState placementState1) {

    }

    @Override
    public void onUnityAdsReady(String placementId) {
        if (bannerView == null) {
            UnityBanners.loadBanner((Activity) context, placementId);

            MoPubLog.log(placementId, LOAD_ATTEMPTED, ADAPTER_NAME);
        }
    }

    @Override
    public void onUnityAdsStart(String s) {

    }

    @Override
    public void onUnityAdsFinish(String s, UnityAds.FinishState finishState) {

    }

    @Override
    public void onUnityAdsError(UnityAds.UnityAdsError unityAdsError, String s) {

    }
}
