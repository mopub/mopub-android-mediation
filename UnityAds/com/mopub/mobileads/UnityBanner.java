package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.view.View;

import com.mopub.common.logging.MoPubLog;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.mediation.IUnityAdsExtendedListener;
import com.unity3d.ads.metadata.MetaData;
import com.unity3d.services.banners.IUnityBannerListener;
import com.unity3d.services.banners.UnityBanners;

import java.util.Map;

public class UnityBanner extends CustomEventBanner implements IUnityBannerListener, IUnityAdsExtendedListener {

	private Context context;
	private String placementId = "banner";
	private CustomEventBannerListener customEventBannerListener;

	@Override
	protected void loadBanner(Context context, CustomEventBannerListener customEventBannerListener, Map<String, Object> localExtras, Map<String, String> serverExtras) {
		if (!(context instanceof Activity)) {
			customEventBannerListener.onBannerFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
			return;
		}

		initNoRefreshMetaData(context);

		placementId = UnityRouter.placementIdForServerExtras(serverExtras, placementId);
		this.customEventBannerListener = customEventBannerListener;
		this.context = context;

		if (UnityRouter.initUnityAds(serverExtras, (Activity)context)) {
			UnityRouter.getBannerRouter().addListener(placementId, this);
			// Bug: banner ready events go through the interstitial router atm.
			UnityRouter.getInterstitialRouter().addListener(placementId, this);

			if (UnityAds.isReady(placementId)) {
				UnityBanners.loadBanner((Activity)context, placementId);
			}
		} else {
			MoPubLog.e("Failed to initialize Unity Ads");
			customEventBannerListener.onBannerFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
		}
	}

	private void initNoRefreshMetaData(Context context) {
		MetaData gdprMetaData = new MetaData(context);
		gdprMetaData.set("banner.refresh", false);
		gdprMetaData.commit();
	}

	@Override
	protected void onInvalidate() {
		UnityRouter.getBannerRouter().removeListener(placementId);
		UnityRouter.getInterstitialRouter().removeListener(placementId);
		UnityBanners.destroy();
	}

	@Override
	public void onUnityBannerLoaded(String placementId, View view) {
		customEventBannerListener.onBannerLoaded(view);
	}

	@Override
	public void onUnityBannerUnloaded(String placementId) {
	}

	@Override
	public void onUnityBannerShow(String placementId) {
		customEventBannerListener.onBannerImpression();
	}

	@Override
	public void onUnityBannerClick(String placementId) {
	}

	@Override
	public void onUnityBannerHide(String placementIds) {
	}

	@Override
	public void onUnityBannerError(String placementId) {
		customEventBannerListener.onBannerFailed(MoPubErrorCode.INTERNAL_ERROR);
	}

	@Override
	public void onUnityAdsClick(String s) {

	}

	@Override
	public void onUnityAdsPlacementStateChanged(String s, UnityAds.PlacementState placementState, UnityAds.PlacementState placementState1) {

	}

	@Override
	public void onUnityAdsReady(String placementId) {
		UnityBanners.loadBanner((Activity)context, placementId);
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
