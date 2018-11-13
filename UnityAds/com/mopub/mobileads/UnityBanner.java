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
	private View bannerView;

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
		UnityRouter.getBannerRouter().setCurrentPlacementId(placementId);

		if (UnityRouter.initUnityAds(serverExtras, (Activity)context)) {
			UnityRouter.getBannerRouter().addListener(placementId, this);
			// Bug: banner ready events go through the interstitial router atm.
			UnityRouter.getInterstitialRouter().addListener(placementId, this);

			if (UnityAds.isReady(placementId)) {
				UnityBanners.loadBanner((Activity)context, placementId);
			}
		} else {
			MoPubLog.e("Failed to initialize Unity Ads");
			if (customEventBannerListener != null) {
				customEventBannerListener.onBannerFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
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
		customEventBannerListener = null;
		UnityBanners.destroy();
		bannerView = null;
	}

	@Override
	public void onUnityBannerLoaded(String placementId, View view) {
		MoPubLog.i(String.format("Banner did load for placement %s", placementId));
		if (customEventBannerListener != null) {
			customEventBannerListener.onBannerLoaded(view);
			this.bannerView = view;
		}
	}

	@Override
	public void onUnityBannerUnloaded(String placementId) {
		MoPubLog.i(String.format("Banner did unload for placement %s", placementId));
	}

	@Override
	public void onUnityBannerShow(String placementId) {
		if (customEventBannerListener != null) {
			MoPubLog.i(String.format("Banner did show for placement %s", placementId));
			customEventBannerListener.onBannerImpression();
		}
	}

	@Override
	public void onUnityBannerClick(String placementId) {
		if (customEventBannerListener != null) {
			MoPubLog.i(String.format("Banner did click for placement %s", placementId));
			customEventBannerListener.onBannerClicked();
		}
	}

	@Override
	public void onUnityBannerHide(String placementIds) {
		MoPubLog.i(String.format("Banner did hide for placement %s", placementIds));
	}

	@Override
	public void onUnityBannerError(String message) {
		if (customEventBannerListener != null) {
			MoPubLog.i(String.format("Banner did error for placement %s with error %s", placementId, message));
			customEventBannerListener.onBannerFailed(MoPubErrorCode.INTERNAL_ERROR);
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
			UnityBanners.loadBanner((Activity)context, placementId);
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
