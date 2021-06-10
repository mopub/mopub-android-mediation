package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mbridge.msdk.MBridgeConstans;
import com.mbridge.msdk.interstitialvideo.out.InterstitialVideoListener;
import com.mbridge.msdk.interstitialvideo.out.MBBidInterstitialVideoHandler;
import com.mbridge.msdk.interstitialvideo.out.MBInterstitialVideoHandler;
import com.mbridge.msdk.out.MBridgeSDKFactory;
import com.mopub.common.LifecycleListener;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;

import java.util.Map;

import static com.mopub.common.DataKeys.ADM_KEY;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.DID_DISAPPEAR;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;
import static com.mopub.mobileads.MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR;
import static com.mopub.mobileads.MoPubErrorCode.NETWORK_NO_FILL;
import static com.mopub.mobileads.MoPubErrorCode.UNSPECIFIED;

public class MintegralInterstitial extends BaseAd implements InterstitialVideoListener {
    private final String ADAPTER_NAME = this.getClass().getSimpleName();
    private final MintegralAdapterConfiguration mMintegralAdapterConfiguration;

    private MBInterstitialVideoHandler mInterstitial;
    private MBBidInterstitialVideoHandler mBiddingInterstitial;

    private String mAdUnitId;
    private String mAppId;
    private String mAppKey;
    private String mPlacementId;

    public MintegralInterstitial() {
        mMintegralAdapterConfiguration = new MintegralAdapterConfiguration();
    }

    @Override
    protected boolean checkAndInitializeSdk(@NonNull final Activity activity,
                                            @NonNull final AdData adData) {
        Preconditions.checkNotNull(activity);
        Preconditions.checkNotNull(adData);

        if (!serverDataIsValid(adData.getExtras())) {
            failAdapter(LOAD_FAILED, ADAPTER_CONFIGURATION_ERROR, "One or " +
                    "more keys used for Mintegral's ad requests are empty. Failing adapter. " +
                    "Please ensure you have populated all the required keys on the MoPub " +
                    "dashboard", true);

            return false;
        }

        return true;
    }

    @Override
    protected void load(@NonNull final Context context, @NonNull final AdData adData) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(adData);

        setAutomaticImpressionAndClickTracking(false);

        final Map<String, String> extras = adData.getExtras();

        if (!serverDataIsValid(extras)) {
            failAdapter(LOAD_FAILED, ADAPTER_CONFIGURATION_ERROR, "One or more keys used " +
                    "for Mintegral's ad requests are empty. Failing adapter. Please ensure you have " +
                    "populated all the required keys on the MoPub dashboard.", true);
            return;
        }

        MintegralAdapterConfiguration.addChannel();
        MintegralAdapterConfiguration.setTargeting(MBridgeSDKFactory.getMBridgeSDK());
        MintegralAdapterConfiguration.configureMintegralSdk(mAppId, mAppKey, context,
                new MintegralSdkManager.MBSDKInitializeListener() {
                    @Override
                    public void onInitializeSuccess(String appKey, String appID) {
                        loadInterstitialVideo(context, extras);
                    }

                    @Override
                    public void onInitializeFailure(String message) {
                        failAdapter(LOAD_FAILED, ADAPTER_CONFIGURATION_ERROR, "Mintegral SDK " +
                                "initialization failed: " + message + ". Failing ad request.", true);
                    }
                });
    }

    private void loadInterstitialVideo(@NonNull Context context, Map<String, String> extras) {
        Preconditions.checkNotNull(extras);

        if (context instanceof Activity) {
            final String adMarkup = extras.get(ADM_KEY);

            if (TextUtils.isEmpty(adMarkup)) {
                mInterstitial = new MBInterstitialVideoHandler(context, mPlacementId, mAdUnitId);
                mInterstitial.setRewardVideoListener(MintegralInterstitial.this);
                mInterstitial.load();
            } else {
                mBiddingInterstitial = new MBBidInterstitialVideoHandler(context, mPlacementId, mAdUnitId);
                mBiddingInterstitial.setRewardVideoListener(MintegralInterstitial.this);
                mBiddingInterstitial.loadFromBid(adMarkup);
            }

            handleAudio();

            mMintegralAdapterConfiguration.setCachedInitializationParameters(context, extras);

            MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);
        } else {
            failAdapter(LOAD_FAILED, ADAPTER_CONFIGURATION_ERROR, "Context is not an instance " +
                    "of Activity. Failing ad request.", true);
        }
    }

    @Override
    protected void show() {
        if (mInterstitial != null && mInterstitial.isReady()) {
            mInterstitial.show();
        } else if (mBiddingInterstitial != null && mBiddingInterstitial.isBidReady()) {
            mBiddingInterstitial.showFromBid();
        } else {
            failAdapter(SHOW_FAILED, NETWORK_NO_FILL, "Failed to show Mintegral interstitial " +
                    "because it is not ready.", false);
        }

        handleAudio();

        MoPubLog.log(getAdNetworkId(), SHOW_ATTEMPTED, ADAPTER_NAME);
    }

    @Override
    protected void onInvalidate() {
        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Finished showing Mintegral " +
                "interstitial. Invalidating adapter...");

        if (mInterstitial != null) {
            mInterstitial.setInterstitialVideoListener(null);
            mInterstitial = null;
        }

        if (mBiddingInterstitial != null) {
            mBiddingInterstitial.setInterstitialVideoListener(null);
            mBiddingInterstitial = null;
        }
    }

    @Nullable
    @Override
    protected LifecycleListener getLifecycleListener() {
        return null;
    }

    private void failAdapter(final MoPubLog.AdapterLogEvent event, final MoPubErrorCode errorCode,
                             final String errorMsg, final boolean loadRelated) {
        MoPubLog.log(getAdNetworkId(), event, ADAPTER_NAME, errorCode.getIntCode(), errorCode);
        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, errorMsg);

        if (loadRelated && mLoadListener != null) {
            mLoadListener.onAdLoadFailed(errorCode);
        } else if (!loadRelated && mInteractionListener != null) {
            mInteractionListener.onAdFailed(errorCode);
        }
    }

    private void handleAudio() {
        final boolean isMute = MintegralAdapterConfiguration.isMute();
        final int muteStatus = isMute ? MBridgeConstans.INTER_ACTIVE_VIDEO_PLAY_MUTE :
                MBridgeConstans.INTER_ACTIVE_VIDEO_PLAY_NOT_MUTE;

        if (mInterstitial != null) {
            mInterstitial.playVideoMute(muteStatus);
        } else if (mBiddingInterstitial != null) {
            mBiddingInterstitial.playVideoMute(muteStatus);
        }
    }

    private boolean serverDataIsValid(final Map<String, String> extras) {
        if (extras != null && !extras.isEmpty()) {
            mAdUnitId = extras.get(MintegralAdapterConfiguration.UNIT_ID_KEY);
            mAppId = extras.get(MintegralAdapterConfiguration.APP_ID_KEY);
            mAppKey = extras.get(MintegralAdapterConfiguration.APP_KEY);
            mPlacementId = extras.get(MintegralAdapterConfiguration.PLACEMENT_ID_KEY);

            return !TextUtils.isEmpty(mAppId) && !TextUtils.isEmpty(mAppKey) && !TextUtils.isEmpty(mAdUnitId);
        }

        return false;
    }

    @NonNull
    protected String getAdNetworkId() {
        return mAdUnitId != null ? mAdUnitId : "";
    }

    @Override
    public void onVideoLoadSuccess(String placementId, String s) {
        MoPubLog.log(getAdNetworkId(), LOAD_SUCCESS, ADAPTER_NAME);

        if (mLoadListener != null) {
            mLoadListener.onAdLoaded();
        }
    }

    @Override
    public void onVideoLoadFail(String errorMsg) {
        failAdapter(LOAD_FAILED, UNSPECIFIED, errorMsg, true);
    }

    @Override
    public void onAdShow() {
        MoPubLog.log(getAdNetworkId(), SHOW_SUCCESS, ADAPTER_NAME);

        if (mInteractionListener != null) {
            mInteractionListener.onAdShown();
            mInteractionListener.onAdImpression();
        }
    }

    @Override
    public void onShowFail(String errorMsg) {
        failAdapter(SHOW_FAILED, MoPubErrorCode.AD_SHOW_ERROR, "Failed to show Mintegral interstitial: "
                + errorMsg, false);
    }

    @Override
    public void onAdClose(boolean b) {
        MoPubLog.log(getAdNetworkId(), DID_DISAPPEAR, ADAPTER_NAME);

        if (mInteractionListener != null) {
            mInteractionListener.onAdDismissed();
        }
    }

    @Override
    public void onVideoAdClicked(String placementId, String message) {
        MoPubLog.log(getAdNetworkId(), CLICKED, ADAPTER_NAME);

        if (mInteractionListener != null) {
            mInteractionListener.onAdClicked();
        }
    }

    @Override
    public void onEndcardShow(String placementId, String message) {
        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "onEndcardShow");
    }

    @Override
    public void onVideoComplete(String placementId, String message) {
        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "onVideoComplete: " + message);
    }

    @Override
    public void onAdCloseWithIVReward(boolean isComplete, int rewardAlertStatus) {
        String rewardStatus = null;

        if (rewardAlertStatus == MBridgeConstans.IVREWARDALERT_STATUS_NOTSHOWN) {
            rewardStatus = "The dialog was not shown.";
        } else if (rewardAlertStatus == MBridgeConstans.IVREWARDALERT_STATUS_CLICKCONTINUE) {
            rewardStatus = "The dialog's continue button was clicked.";
        } else if (rewardAlertStatus == MBridgeConstans.IVREWARDALERT_STATUS_CLICKCANCEL) {
            rewardStatus = "The dialog's cancel button was clicked.";
        }

        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, isComplete ? "Video playback is " +
                "complete." : "Video playback is not complete. " + rewardStatus);
    }

    @Override
    public void onLoadSuccess(String placementId, String message) {
        // NO-OP
    }
}
