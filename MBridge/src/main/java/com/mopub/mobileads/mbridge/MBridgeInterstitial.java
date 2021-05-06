package com.mopub.mobileads.mbridge;

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
import com.mopub.mobileads.AdData;
import com.mopub.mobileads.BaseAd;
import com.mopub.mobileads.MoPubErrorCode;

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

public class MBridgeInterstitial extends BaseAd implements InterstitialVideoListener {

    private final String ADAPTER_NAME = this.getClass().getSimpleName();

    private MBInterstitialVideoHandler mInterstitialHandler;
    private MBBidInterstitialVideoHandler mBidInterstitialVideoHandler;

    private String mAdUnitId;
    private String mPlacementId;
    private String appId;
    private String appKey;
    private Context mContext;
    private String mUserId;
    private String mRewardId;

    @Override
    protected void load(@NonNull final Context context, @NonNull final AdData adData) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(adData);

        setAutomaticImpressionAndClickTracking(false);

        final Map<String, String> extras = adData.getExtras();
        if (!serverDataIsValid(extras, context)) {
            failAdapter(LOAD_FAILED, ADAPTER_CONFIGURATION_ERROR, "One or " + "more keys used for mb's ad requests are empty. Failing adapter. Please " + "ensure you have populated all the required keys on the MoPub dashboard.", true);
            return;
        }


        MBridgeAdapterConfiguration.addChannel();
        MBridgeAdapterConfiguration.setTargeting(MBridgeSDKFactory.getMBridgeSDK());

        MBridgeAdapterConfiguration.configureMBSDK(appId, appKey, context, new MBridgeSDKManager.MBSDKInitializeListener() {
            @Override
            public void onInitializeSuccess(String appKey, String appID) {
                loadInterstitialVideo(context, extras);
            }

            @Override
            public void onInitializeFailure(String message) {
                failAdapter(LOAD_FAILED, ADAPTER_CONFIGURATION_ERROR, "mb SDK init failed: " + message, true);
            }
        });
    }

    private void loadInterstitialVideo(@NonNull Context context, Map<String, String> extras) {
        if (context instanceof Activity) {
            final String adMarkup = extras.get(ADM_KEY);

            if (TextUtils.isEmpty(adMarkup)) {
                mInterstitialHandler = new MBInterstitialVideoHandler(context, mPlacementId, mAdUnitId);
                mInterstitialHandler.setRewardVideoListener(MBridgeInterstitial.this);
                mInterstitialHandler.load();

                handleAudio();
            } else {
                mBidInterstitialVideoHandler = new MBBidInterstitialVideoHandler(context, mPlacementId, mAdUnitId);
                mBidInterstitialVideoHandler.setRewardVideoListener(MBridgeInterstitial.this);
                mBidInterstitialVideoHandler.loadFromBid(adMarkup);

                handleAudio();
            }
            MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);
        } else {
            failAdapter(LOAD_FAILED, ADAPTER_CONFIGURATION_ERROR, "Context is not an instance " + "of Activity. Aborting ad request, and failing adapter.", true);
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
            failAdapter(SHOW_FAILED, NETWORK_NO_FILL, "Failed to show mb interstitial " + "because it is not ready. Please make a new ad request.", false);
        }

        MoPubLog.log(getAdNetworkId(), SHOW_ATTEMPTED, ADAPTER_NAME);
    }

    @Override
    protected void onInvalidate() {
        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Finished showing mb " + "interstitial. Invalidating adapter...");

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

    private void failAdapter(final MoPubLog.AdapterLogEvent event, final MoPubErrorCode errorCode, final String errorMsg, final boolean isLoad) {

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
        final boolean isMute = MBridgeAdapterConfiguration.isMute();
        final int muteStatus = isMute ? MBridgeConstans.REWARD_VIDEO_PLAY_MUTE : MBridgeConstans.REWARD_VIDEO_PLAY_NOT_MUTE;

        if (mInterstitialHandler != null) {
            mInterstitialHandler.playVideoMute(muteStatus);
        } else if (mBidInterstitialVideoHandler != null) {
            mBidInterstitialVideoHandler.playVideoMute(muteStatus);
        }
    }

    private boolean serverDataIsValid(final Map<String, String> extras, Context context) {

        if (extras != null && !extras.isEmpty()) {
            mAdUnitId = extras.get(MBridgeAdapterConfiguration.UNIT_ID_KEY);
            mPlacementId = extras.get(MBridgeAdapterConfiguration.PLACEMENT_ID_KEY);

            appId = extras.get(MBridgeAdapterConfiguration.APP_ID_KEY);
            appKey = extras.get(MBridgeAdapterConfiguration.APP_KEY);

            if (!TextUtils.isEmpty(appId) && !TextUtils.isEmpty(appKey) && !TextUtils.isEmpty(mAdUnitId)) {
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
    protected boolean checkAndInitializeSdk(@NonNull final Activity activity, @NonNull final AdData adData) {
        Preconditions.checkNotNull(activity);
        Preconditions.checkNotNull(adData);

        mContext = activity.getApplicationContext();
        mUserId = MBridgeAdapterConfiguration.getUserId();
        mRewardId = MBridgeAdapterConfiguration.getRewardId();

        if (!serverDataIsValid(adData.getExtras(), mContext)) {
            failAdapter(LOAD_FAILED, ADAPTER_CONFIGURATION_ERROR, "One or " +
                    "more keys used for mb's ad requests are empty. Failing adapter. " +
                    "Please ensure you have populated all the required keys on the MoPub " +
                    "dashboard", true);

            return false;
        }
        return true;
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
        failAdapter(SHOW_FAILED, UNSPECIFIED, "Failed to show mb interstitial: "
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
        MoPubLog.log(getAdNetworkId(), LOAD_SUCCESS, ADAPTER_NAME);
    }
}
