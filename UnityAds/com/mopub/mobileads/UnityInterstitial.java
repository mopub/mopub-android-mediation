package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;

import com.mopub.common.logging.MoPubLog;
import com.unity3d.ads.mediation.IUnityAdsExtendedListener;
import com.unity3d.ads.UnityAds;

import java.util.Map;

public class UnityInterstitial extends CustomEventInterstitial implements IUnityAdsExtendedListener {

    private CustomEventInterstitialListener mCustomEventInterstitialListener;
    private Context mContext;
    private String mPlacementId = "video";
    private boolean loadRequested = false;

    @Override
    protected void loadInterstitial(Context context,
                                    CustomEventInterstitialListener customEventInterstitialListener,
                                    Map<String, Object> localExtras,
                                    Map<String, String> serverExtras) {

        mPlacementId = UnityRouter.placementIdForServerExtras(serverExtras, mPlacementId);
        mCustomEventInterstitialListener = customEventInterstitialListener;
        mContext = context;
        loadRequested = true;


        UnityRouter.getInterstitialRouter().addListener(mPlacementId, this);
        UnityRouter.getInterstitialRouter().setCurrentPlacementId(mPlacementId);
        initializeUnityAdsSdk(serverExtras);
        if (UnityAds.isReady(mPlacementId)) {
            mCustomEventInterstitialListener.onInterstitialLoaded();
            loadRequested = false;
        } else if (UnityAds.getPlacementState(mPlacementId) == UnityAds.PlacementState.NO_FILL){
            mCustomEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.NO_FILL);
            UnityRouter.getInterstitialRouter().removeListener(mPlacementId);
        }
    }

    private void initializeUnityAdsSdk(Map<String, String> serverExtras) {
        if (!UnityAds.isInitialized()) {
            if (!(mContext instanceof Activity)) {
                MoPubLog.e("Context is null or is not an instanceof Activity.");
                return;
            }
            UnityRouter.initUnityAds(serverExtras, (Activity) mContext);
        }
    }

    @Override
    protected void showInterstitial() {
        if (UnityAds.isReady(mPlacementId) && mContext != null) {
            UnityAds.show((Activity) mContext, mPlacementId);
        } else {
            MoPubLog.d("Attempted to show Unity interstitial video before it was available.");
        }
    }

    @Override
    protected void onInvalidate() {
        UnityRouter.getInterstitialRouter().removeListener(mPlacementId);
        mCustomEventInterstitialListener = null;
    }

    @Override
    public void onUnityAdsReady(String placementId) {
        if (loadRequested && mCustomEventInterstitialListener != null) {
            mCustomEventInterstitialListener.onInterstitialLoaded();
            loadRequested = false;
        }
    }

    @Override
    public void onUnityAdsStart(String placementId) {
        if (mCustomEventInterstitialListener != null) {
            mCustomEventInterstitialListener.onInterstitialShown();
        }
    }

    @Override
    public void onUnityAdsFinish(String placementId, UnityAds.FinishState finishState) {
        if (mCustomEventInterstitialListener != null) {
            if (finishState == UnityAds.FinishState.ERROR) {
                MoPubLog.d("Unity interstitial video encountered a playback error for placement " + placementId);
                mCustomEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.VIDEO_PLAYBACK_ERROR);
            } else {
                MoPubLog.d("Unity interstitial video completed for placement " + placementId);
                mCustomEventInterstitialListener.onInterstitialDismissed();
            }
        }
        UnityRouter.getInterstitialRouter().removeListener(placementId);
    }

    @Override
    public void onUnityAdsClick(String placementId) {
        if (mCustomEventInterstitialListener != null) {
            mCustomEventInterstitialListener.onInterstitialClicked();
        }
    }


    // @Override
    public void onUnityAdsPlacementStateChanged(String placementId, UnityAds.PlacementState oldState, UnityAds.PlacementState newState) {
        if (placementId.equals(mPlacementId) && mCustomEventInterstitialListener != null) {
            if(newState == UnityAds.PlacementState.NO_FILL) {
                mCustomEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.NO_FILL);
                UnityRouter.getInterstitialRouter().removeListener(mPlacementId);
            }
        }
    }

    @Override
    public void onUnityAdsError(UnityAds.UnityAdsError unityAdsError, String message) {
        if (mCustomEventInterstitialListener != null) {
            MoPubLog.d("Unity interstitial video cache failed for placement " + mPlacementId + ".");
            MoPubErrorCode errorCode = UnityRouter.UnityAdsUtils.getMoPubErrorCode(unityAdsError);
            mCustomEventInterstitialListener.onInterstitialFailed(errorCode);
        }
    }
}
