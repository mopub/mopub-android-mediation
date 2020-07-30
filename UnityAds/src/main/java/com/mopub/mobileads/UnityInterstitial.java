package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.LifecycleListener;
import com.mopub.common.logging.MoPubLog;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.mediation.IUnityAdsExtendedListener;
import com.unity3d.ads.metadata.MediationMetaData;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;

public class UnityInterstitial extends BaseAd implements IUnityAdsExtendedListener {

    private static final String ADAPTER_NAME = UnityInterstitial.class.getSimpleName();

    private Context mContext;
    private String mPlacementId = "video";
    private int impressionOrdinal;
    private int missedImpressionOrdinal;
    @NonNull
    private UnityAdsAdapterConfiguration mUnityAdsAdapterConfiguration;

    public UnityInterstitial() {
        mUnityAdsAdapterConfiguration = new UnityAdsAdapterConfiguration();
    }

    @Override
    protected void load(@NonNull final Context context, @NonNull final AdData adData) {

        final Map<String, String> extras = adData.getExtras();
        mPlacementId = UnityRouter.placementIdForServerExtras(extras, mPlacementId);
        mContext = context;

        setAutomaticImpressionAndClickTracking(false);
        UnityAds.load(mPlacementId);

        mUnityAdsAdapterConfiguration.setCachedInitializationParameters(context, extras);

        UnityRouter.getInterstitialRouter().addListener(mPlacementId, this);
        UnityRouter.getInterstitialRouter().setCurrentPlacementId(mPlacementId);
        initializeUnityAdsSdk(extras);
    }

    private void initializeUnityAdsSdk(Map<String, String> serverExtras) {
        if (!UnityAds.isInitialized()) {
            if (!(mContext instanceof Activity)) {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "Context is null or is not an instanceof Activity.");
                return;
            }
            UnityRouter.initUnityAds(serverExtras, (Activity) mContext);
        }
    }

    @Override
    protected void show() {
        MoPubLog.log(SHOW_ATTEMPTED, ADAPTER_NAME);

        if (UnityAds.isReady(mPlacementId) && mContext != null) {
            MediationMetaData metadata = new MediationMetaData(mContext);
            metadata.setOrdinal(++impressionOrdinal);
            metadata.commit();

            UnityAds.show((Activity) mContext, mPlacementId);
        } else {
            // lets Unity Ads know when ads fail to show
            MediationMetaData metadata = new MediationMetaData(mContext);
            metadata.setMissedImpressionOrdinal(++missedImpressionOrdinal);
            metadata.commit();

            MoPubLog.log(SHOW_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                    MoPubErrorCode.NETWORK_NO_FILL);

            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Attempted to show Unity interstitial video before it was available.");
        }
    }

    @Override
    protected void onInvalidate() {
        UnityRouter.getInterstitialRouter().removeListener(mPlacementId);
    }

    @Nullable
    @Override
    protected LifecycleListener getLifecycleListener() {
        return null;
    }

    @NonNull
    @Override
    protected String getAdNetworkId() {
        return mPlacementId != null ? mPlacementId : "";
    }

    @Override
    protected boolean checkAndInitializeSdk(@NonNull final Activity activity,
                                            @NonNull final AdData adData) {
        return false;
    }

    @Override
    public void onUnityAdsReady(String placementId) {
        if (mLoadListener != null) {
            mLoadListener.onAdLoaded();

            MoPubLog.log(LOAD_SUCCESS, ADAPTER_NAME);
        }
    }

    @Override
    public void onUnityAdsStart(String placementId) {
        if (mInteractionListener != null) {
            mInteractionListener.onAdShown();
            mInteractionListener.onAdImpression();
        }

        MoPubLog.log(SHOW_SUCCESS, ADAPTER_NAME);
    }

    @Override
    public void onUnityAdsFinish(String placementId, UnityAds.FinishState finishState) {
        if (mInteractionListener != null) {
            if (finishState == UnityAds.FinishState.ERROR) {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unity interstitial video encountered a playback error for " +
                        "placement " + placementId);
                mInteractionListener.onAdFailed(MoPubErrorCode.NETWORK_NO_FILL);

                MoPubLog.log(SHOW_FAILED, ADAPTER_NAME,
                        MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                        MoPubErrorCode.NETWORK_NO_FILL);
            } else {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unity interstitial video completed for placement " + placementId);
                mInteractionListener.onAdDismissed();
            }
        }
        UnityRouter.getInterstitialRouter().removeListener(placementId);
    }

    @Override
    public void onUnityAdsClick(String placementId) {
        if (mInteractionListener != null) {
            mInteractionListener.onAdClicked();
        }

        MoPubLog.log(CLICKED, ADAPTER_NAME);
    }


    // @Override
    public void onUnityAdsPlacementStateChanged(String placementId, UnityAds.PlacementState oldState, UnityAds.PlacementState newState) {
        if (placementId.equals(mPlacementId) && mLoadListener != null) {
            if (newState == UnityAds.PlacementState.NO_FILL) {
                mLoadListener.onAdLoadFailed(MoPubErrorCode.NETWORK_NO_FILL);
                UnityRouter.getInterstitialRouter().removeListener(mPlacementId);

                MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                        MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                        MoPubErrorCode.NETWORK_NO_FILL);
            }
        }
    }

    @Override
    public void onUnityAdsError(UnityAds.UnityAdsError unityAdsError, String message) {

        if (mLoadListener != null) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unity interstitial video cache failed for placement " +
                    mPlacementId + "." + message);
            MoPubErrorCode errorCode = UnityRouter.UnityAdsUtils.getMoPubErrorCode(unityAdsError);
            mLoadListener.onAdLoadFailed(errorCode);

            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                    errorCode.getIntCode(),
                    errorCode);
        }
    }
}
