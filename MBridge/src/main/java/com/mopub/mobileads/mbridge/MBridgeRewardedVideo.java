package com.mopub.mobileads.mbridge;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mbridge.msdk.MBridgeConstans;
import com.mbridge.msdk.out.MBBidRewardVideoHandler;
import com.mbridge.msdk.out.MBRewardVideoHandler;
import com.mbridge.msdk.out.MBridgeSDKFactory;
import com.mbridge.msdk.out.RewardVideoListener;
import com.mopub.common.LifecycleListener;
import com.mopub.common.MoPubReward;
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
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOULD_REWARD;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;
import static com.mopub.mobileads.MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR;
import static com.mopub.mobileads.MoPubErrorCode.NETWORK_NO_FILL;
import static com.mopub.mobileads.MoPubErrorCode.UNSPECIFIED;
import static com.mopub.mobileads.MoPubErrorCode.VIDEO_PLAYBACK_ERROR;

public class MBridgeRewardedVideo extends BaseAd implements RewardVideoListener {

    private final String ADAPTER_NAME = this.getClass().getSimpleName();

    private Context mContext;
    private MBRewardVideoHandler mbRewardVideoHandler;
    private MBBidRewardVideoHandler mbBidRewardVideoHandler;

    private String mAdUnitId;
    private String mPlacementId;
    private String mUserId;
    private String mRewardId;
    private String appId;
    private String appKey;

    @Nullable
    @Override
    protected LifecycleListener getLifecycleListener() {
        return null;
    }

    @NonNull
    @Override
    protected String getAdNetworkId() {
        return !TextUtils.isEmpty(mAdUnitId) ? mAdUnitId : "";
    }

    @Override
    protected boolean checkAndInitializeSdk(@NonNull final Activity launcherActivity, @NonNull final AdData adData) {
        Preconditions.checkNotNull(launcherActivity);
        Preconditions.checkNotNull(adData);

        mContext = launcherActivity.getApplicationContext();
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
    protected void load(@NonNull final Context context, @NonNull final AdData adData) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(adData);

        setAutomaticImpressionAndClickTracking(false);
        final Map<String, String> extras = adData.getExtras();
        if (!serverDataIsValid(extras, mContext)) {
            failAdapter(LOAD_FAILED, ADAPTER_CONFIGURATION_ERROR, "One or more keys used for mb's ad requests are empty. Failing adapter. Please " + "ensure you have populated all the required keys on the MoPub dashboard", true);
            return;
        }

        MBridgeAdapterConfiguration.addChannel();
        MBridgeAdapterConfiguration.setTargeting(MBridgeSDKFactory.getMBridgeSDK());
        MBridgeAdapterConfiguration.configureMBSDK(appId, appKey, context, new MBridgeSDKManager.MBSDKInitializeListener() {
            @Override
            public void onInitializeSuccess(String appKey, String appID) {
                loadRewardVideo(extras);
            }

            @Override
            public void onInitializeFailure(String message) {
                failAdapter(LOAD_FAILED, ADAPTER_CONFIGURATION_ERROR, "mb SDK init failed: " + message, true);
            }
        });
        MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);
    }

    private void loadRewardVideo(Map<String, String> extras) {
        final String adMarkup = extras.get(ADM_KEY);
        if (TextUtils.isEmpty(adMarkup)) {
            mbRewardVideoHandler = new MBRewardVideoHandler(mPlacementId, mAdUnitId);
            mbRewardVideoHandler.setRewardVideoListener(MBridgeRewardedVideo.this);
            mbRewardVideoHandler.load();
            handleAudio();
        } else {
            mbBidRewardVideoHandler = new MBBidRewardVideoHandler(mPlacementId, mAdUnitId);
            mbBidRewardVideoHandler.setRewardVideoListener(MBridgeRewardedVideo.this);
            mbBidRewardVideoHandler.loadFromBid(adMarkup);
            handleAudio();
        }
    }

    @Override
    protected void show() {
        if (mbRewardVideoHandler != null && mbRewardVideoHandler.isReady()) {
            handleAudio();
            mbRewardVideoHandler.show(mRewardId, mUserId);

            MoPubLog.log(getAdNetworkId(), SHOW_ATTEMPTED, ADAPTER_NAME);
        } else if (mbBidRewardVideoHandler != null && mbBidRewardVideoHandler.isBidReady()) {
            handleAudio();
            mbBidRewardVideoHandler.showFromBid(mRewardId, mUserId);

            MoPubLog.log(getAdNetworkId(), SHOW_ATTEMPTED, ADAPTER_NAME);
        } else {
            failAdapter(SHOW_FAILED, NETWORK_NO_FILL, "There is no mb rewarded video available. Please make a new ad request.", false);
        }
    }

    @Override
    protected void onInvalidate() {
        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Finished showing mb rewarded video. Invalidating adapter...");

        if (mbRewardVideoHandler != null) {
            mbRewardVideoHandler.setRewardVideoListener(null);
            mbRewardVideoHandler = null;
        }

        if (mbBidRewardVideoHandler != null) {
            mbBidRewardVideoHandler.setRewardVideoListener(null);
            mbBidRewardVideoHandler = null;
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

    private void failAdapter(final MoPubLog.AdapterLogEvent event, final MoPubErrorCode errorCode, final String errorMsg, final boolean loadRelated) {

        MoPubLog.log(getAdNetworkId(), event, ADAPTER_NAME, errorCode.getIntCode(), errorCode);

        if (!TextUtils.isEmpty(errorMsg)) {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, errorMsg);
        }

        if (loadRelated && mLoadListener != null) {
            mLoadListener.onAdLoadFailed(errorCode);
        } else if (!loadRelated && mInteractionListener != null) {
            mInteractionListener.onAdFailed(errorCode);
        }
    }

    private void handleAudio() {
        boolean isMute = MBridgeAdapterConfiguration.isMute();
        int muteStatus = isMute ? MBridgeConstans.REWARD_VIDEO_PLAY_MUTE : MBridgeConstans.REWARD_VIDEO_PLAY_NOT_MUTE;

        if (mbRewardVideoHandler != null) {
            mbRewardVideoHandler.playVideoMute(muteStatus);
        } else if (mbBidRewardVideoHandler != null) {
            mbBidRewardVideoHandler.playVideoMute(muteStatus);
        }
    }

    @Override
    public void onAdClose(boolean b, String label, float amount) {
        if (b) {
            if (mInteractionListener != null) {
                mInteractionListener.onAdComplete(MoPubReward.success(label, (int) amount));
            }

            MoPubLog.log(getAdNetworkId(), SHOULD_REWARD, ADAPTER_NAME, amount, label);
        }

        if (mInteractionListener != null) {
            mInteractionListener.onAdDismissed();
        }
        MoPubLog.log(getAdNetworkId(), DID_DISAPPEAR, ADAPTER_NAME);
    }

    @Override
    public void onVideoLoadSuccess(String placementId, String unitId) {
        if (mLoadListener != null) {
            mLoadListener.onAdLoaded();
        }
        MoPubLog.log(getAdNetworkId(), LOAD_SUCCESS, ADAPTER_NAME);
    }

    @Override
    public void onLoadSuccess(String placementId, String unitId) {
        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "onLoadSuccess: " + placementId + "  " + unitId);
    }

    @Override
    public void onVideoLoadFail(String errorMsg) {
        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "onVideoLoadFail: " + errorMsg);
        failAdapter(LOAD_FAILED, UNSPECIFIED, errorMsg, true);
    }

    @Override
    public void onAdShow() {
        if (mInteractionListener != null) {
            mInteractionListener.onAdShown();
            mInteractionListener.onAdImpression();
        }
        MoPubLog.log(getAdNetworkId(), SHOW_SUCCESS, ADAPTER_NAME);
    }

    @Override
    public void onShowFail(String errorMsg) {
        failAdapter(SHOW_FAILED, VIDEO_PLAYBACK_ERROR, errorMsg, false);
    }

    @Override
    public void onVideoAdClicked(String placementId, String unitId) {
        if (mInteractionListener != null) {
            mInteractionListener.onAdClicked();
        }
        MoPubLog.log(getAdNetworkId(), CLICKED, ADAPTER_NAME);
    }

    @Override
    public void onEndcardShow(String placementId, String unitId) {
        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "onEndcardShow: " + placementId + ", " + unitId);
    }

    @Override
    public void onVideoComplete(String placementId, String unitId) {
        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "onVideoComplete: " + placementId + ", " + unitId);
    }
}
