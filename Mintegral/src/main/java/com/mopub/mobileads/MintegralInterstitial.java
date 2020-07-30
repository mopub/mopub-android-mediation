package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mintegral.msdk.MIntegralConstans;
import com.mintegral.msdk.interstitialvideo.out.InterstitialVideoListener;
import com.mintegral.msdk.interstitialvideo.out.MTGBidInterstitialVideoHandler;
import com.mintegral.msdk.interstitialvideo.out.MTGInterstitialVideoHandler;
import com.mintegral.msdk.out.MIntegralSDKFactory;
import com.mopub.common.LifecycleListener;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;

import java.util.Map;

import static com.mintegral.msdk.MIntegralConstans.REWARD_VIDEO_PLAY_MUTE;
import static com.mintegral.msdk.MIntegralConstans.REWARD_VIDEO_PLAY_NOT_MUTE;
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

    private MTGInterstitialVideoHandler mInterstitialHandler;
    private MTGBidInterstitialVideoHandler mBidInterstitialVideoHandler;

    private String mAdUnitId;
    private String mPlacementId;

    @Override
    protected void load(@NonNull final Context context, @NonNull final AdData adData) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(adData);

        setAutomaticImpressionAndClickTracking(false);

        final Map<String, String> extras = adData.getExtras();
        if (!serverDataIsValid(extras, context)) {
            failAdapter(LOAD_FAILED, ADAPTER_CONFIGURATION_ERROR, "One or " +
                    "more keys used for Mintegral's ad requests are empty. Failing adapter. Please " +
                    "ensure you have populated all the required keys on the MoPub dashboard.", true);

            return;
        }

        MintegralAdapterConfiguration.addChannel();
        MintegralAdapterConfiguration.setTargeting(MIntegralSDKFactory.getMIntegralSDK());

        if (context instanceof Activity) {
            final String adMarkup = extras.get(ADM_KEY);

            if (TextUtils.isEmpty(adMarkup)) {
                mInterstitialHandler = new MTGInterstitialVideoHandler(context, mPlacementId, mAdUnitId);
                mInterstitialHandler.setRewardVideoListener(this);
                mInterstitialHandler.load();

                handleAudio();
            } else {
                mBidInterstitialVideoHandler = new MTGBidInterstitialVideoHandler(context,
                        mPlacementId, mAdUnitId);
                mBidInterstitialVideoHandler.setRewardVideoListener(this);
                mBidInterstitialVideoHandler.loadFromBid(adMarkup);

                handleAudio();
            }

            MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);
        } else {
            failAdapter(LOAD_FAILED, ADAPTER_CONFIGURATION_ERROR, "Context is not an instance " +
                    "of Activity. Aborting ad request, and failing adapter.", true);
        }
    }

    @Override
    protected void show() {
        if (mInterstitialHandler != null && mInterstitialHandler.isReady()) {
            handleAudio();
            mInterstitialHandler.show();
        } else if (mBidInterstitialVideoHandler != null && mBidInterstitialVideoHandler.isBidReady()) {
            handleAudio();
            mBidInterstitialVideoHandler.showFromBid();
        } else {
            failAdapter(SHOW_FAILED, NETWORK_NO_FILL, "Failed to show Mintegral interstitial " +
                    "because it is not ready. Please make a new ad request.", false);
        }

        MoPubLog.log(getAdNetworkId(), SHOW_ATTEMPTED, ADAPTER_NAME);
    }

    @Override
    protected void onInvalidate() {
        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Finished showing Mintegral " +
                "interstitial. Invalidating adapter...");

        if (mInterstitialHandler != null) {
            mInterstitialHandler.setInterstitialVideoListener(null);
            mInterstitialHandler = null;
        }

        if (mBidInterstitialVideoHandler != null) {
            mBidInterstitialVideoHandler.setInterstitialVideoListener(null);
            mBidInterstitialVideoHandler = null;
        }
    }

    @Nullable
    @Override
    protected LifecycleListener getLifecycleListener() {
        return null;
    }

    private void failAdapter(final MoPubLog.AdapterLogEvent event, final MoPubErrorCode errorCode,
                             final String errorMsg, final boolean isLoad) {

        MoPubLog.log(getAdNetworkId(), event, ADAPTER_NAME, errorCode.getIntCode(), errorCode);

        if (!TextUtils.isEmpty(errorMsg)) {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, errorMsg);
        }

        if (isLoad && mLoadListener != null) {
            mLoadListener.onAdLoadFailed(errorCode);
        } else if (!isLoad && mInteractionListener != null) {
            mInteractionListener.onAdFailed(errorCode);
        }
    }

    private void handleAudio() {
        final boolean isMute = MintegralAdapterConfiguration.isMute();
        final int muteStatus = isMute ? REWARD_VIDEO_PLAY_MUTE : REWARD_VIDEO_PLAY_NOT_MUTE;

        if (mInterstitialHandler != null) {
            mInterstitialHandler.playVideoMute(muteStatus);
        } else if (mBidInterstitialVideoHandler != null) {
            mBidInterstitialVideoHandler.playVideoMute(muteStatus);
        }
    }

    private boolean serverDataIsValid(final Map<String, String> extras, Context context) {

        if (extras != null && !extras.isEmpty()) {
            mAdUnitId = extras.get(MintegralAdapterConfiguration.UNIT_ID_KEY);
            mPlacementId = extras.get(MintegralAdapterConfiguration.PLACEMENT_ID_KEY);

            final String appId = extras.get(MintegralAdapterConfiguration.APP_ID_KEY);
            final String appKey = extras.get(MintegralAdapterConfiguration.APP_KEY);

            if (!TextUtils.isEmpty(appId) && !TextUtils.isEmpty(appKey) && !TextUtils.isEmpty(mAdUnitId)) {
                MintegralAdapterConfiguration.configureMintegral(appId, appKey, context);

                return true;
            }
        }
        return false;
    }

    @NonNull
    protected String getAdNetworkId() {
        return mAdUnitId != null ? mAdUnitId : "";
    }

    @Override
    protected boolean checkAndInitializeSdk(@NonNull final Activity activity,
                                            @NonNull final AdData adData) {
        return false;
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
        failAdapter(SHOW_FAILED, UNSPECIFIED, "Failed to show Mintegral interstitial: "
                + errorMsg, false);
    }

    @Override
    public void onAdClose(boolean b) {
        MoPubLog.log(getAdNetworkId(), DID_DISAPPEAR, ADAPTER_NAME);
        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "onAdClose");

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

        if (rewardAlertStatus == MIntegralConstans.IVREWARDALERT_STATUS_NOTSHOWN) {
            rewardStatus = "The dialog was not shown.";
        } else if (rewardAlertStatus == MIntegralConstans.IVREWARDALERT_STATUS_CLICKCONTINUE) {
            rewardStatus = "The dialog's continue button was clicked.";
        } else if (rewardAlertStatus == MIntegralConstans.IVREWARDALERT_STATUS_CLICKCANCEL) {
            rewardStatus = "The dialog's cancel button was clicked.";
        }

        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, isComplete ? "Video playback is " +
                "complete." : "Video playback is not complete. " + rewardStatus);
    }

    @Override
    public void onLoadSuccess(String placementId, String message) {
        MoPubLog.log(getAdNetworkId(), LOAD_SUCCESS, ADAPTER_NAME);
    }
}
