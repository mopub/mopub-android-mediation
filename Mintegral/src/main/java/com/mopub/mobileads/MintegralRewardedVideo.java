package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mintegral.msdk.out.MIntegralSDKFactory;
import com.mintegral.msdk.out.MTGBidRewardVideoHandler;
import com.mintegral.msdk.out.MTGRewardVideoHandler;
import com.mintegral.msdk.out.RewardVideoListener;
import com.mopub.common.LifecycleListener;
import com.mopub.common.MoPubReward;
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
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOULD_REWARD;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;
import static com.mopub.mobileads.MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR;
import static com.mopub.mobileads.MoPubErrorCode.NETWORK_NO_FILL;
import static com.mopub.mobileads.MoPubErrorCode.UNSPECIFIED;
import static com.mopub.mobileads.MoPubErrorCode.VIDEO_PLAYBACK_ERROR;

public class MintegralRewardedVideo extends BaseAd implements RewardVideoListener {

    private final String ADAPTER_NAME = this.getClass().getSimpleName();

    private Context mContext;
    private MTGRewardVideoHandler mMtgRewardVideoHandler;
    private MTGBidRewardVideoHandler mtgBidRewardVideoHandler;

    private static boolean isInitialized = false;
    private String mAdUnitId;
    private String mPlacementId;
    private String mUserId;
    private String mRewardId;

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
    protected boolean checkAndInitializeSdk(@NonNull final Activity launcherActivity,
                                            @NonNull final AdData adData) {
        Preconditions.checkNotNull(launcherActivity);
        Preconditions.checkNotNull(adData);

        mContext = launcherActivity.getApplicationContext();
        mUserId = MintegralAdapterConfiguration.getUserId();
        mRewardId = MintegralAdapterConfiguration.getRewardId();

        if (!serverDataIsValid(adData.getExtras(), mContext)) {
            failAdapter(LOAD_FAILED, ADAPTER_CONFIGURATION_ERROR, "One or " +
                    "more keys used for Mintegral's ad requests are empty. Failing adapter. " +
                    "Please ensure you have populated all the required keys on the MoPub " +
                    "dashboard", true);

            return false;
        }

        return isInitialized;
    }

    @Override
    protected void load(@NonNull final Context context,
                        @NonNull final AdData adData) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(adData);

        setAutomaticImpressionAndClickTracking(false);

        final Map<String, String> extras = adData.getExtras();
        if (!serverDataIsValid(extras, mContext)) {
            failAdapter(LOAD_FAILED, ADAPTER_CONFIGURATION_ERROR, "One or " +
                            "more keys used for Mintegral's ad requests are empty. Failing adapter. Please " +
                            "ensure you have populated all the required keys on the MoPub dashboard",
                    true);

            return;
        }

        MintegralAdapterConfiguration.addChannel();
        MintegralAdapterConfiguration.setTargeting(MIntegralSDKFactory.getMIntegralSDK());

        final String adMarkup = extras.get(ADM_KEY);

        if (TextUtils.isEmpty(adMarkup)) {
            mMtgRewardVideoHandler = new MTGRewardVideoHandler(mPlacementId, mAdUnitId);
            mMtgRewardVideoHandler.setRewardVideoListener(this);
            mMtgRewardVideoHandler.load();

            handleAudio();
        } else {
            mtgBidRewardVideoHandler = new MTGBidRewardVideoHandler(mPlacementId, mAdUnitId);
            mtgBidRewardVideoHandler.setRewardVideoListener(this);
            mtgBidRewardVideoHandler.loadFromBid(adMarkup);

            handleAudio();
        }

        MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);
    }

    @Override
    protected void show() {

        if (mMtgRewardVideoHandler != null && mMtgRewardVideoHandler.isReady()) {
            handleAudio();
            mMtgRewardVideoHandler.show(mRewardId, mUserId);

            MoPubLog.log(getAdNetworkId(), SHOW_ATTEMPTED, ADAPTER_NAME);
        } else if (mtgBidRewardVideoHandler != null && mtgBidRewardVideoHandler.isBidReady()) {
            handleAudio();
            mtgBidRewardVideoHandler.showFromBid(mRewardId, mUserId);

            MoPubLog.log(getAdNetworkId(), SHOW_ATTEMPTED, ADAPTER_NAME);
        } else {
            failAdapter(SHOW_FAILED, NETWORK_NO_FILL, "There is no Mintegral rewarded " +
                    "video available. Please make a new ad request.", false);
        }
    }

    @Override
    protected void onInvalidate() {
        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Finished showing Mintegral " +
                "rewarded video. Invalidating adapter...");

        if (mMtgRewardVideoHandler != null) {
            mMtgRewardVideoHandler.setRewardVideoListener(null);
            mMtgRewardVideoHandler = null;
        }

        if (mtgBidRewardVideoHandler != null) {
            mtgBidRewardVideoHandler.setRewardVideoListener(null);
            mtgBidRewardVideoHandler = null;
        }
    }

    private boolean serverDataIsValid(final Map<String, String> extras, Context context) {

        if (extras != null && !extras.isEmpty()) {
            mAdUnitId = extras.get(MintegralAdapterConfiguration.UNIT_ID_KEY);
            mPlacementId = extras.get(MintegralAdapterConfiguration.PLACEMENT_ID_KEY);

            final String appId = extras.get(MintegralAdapterConfiguration.APP_ID_KEY);
            final String appKey = extras.get(MintegralAdapterConfiguration.APP_KEY);

            if (!TextUtils.isEmpty(appId) && !TextUtils.isEmpty(appKey) && !TextUtils.isEmpty(mAdUnitId)) {
                if (!isInitialized) {
                    MintegralAdapterConfiguration.configureMintegral(appId, appKey, context);
                    isInitialized = true;
                }

                return true;
            }
        }
        return false;
    }

    private void failAdapter(final MoPubLog.AdapterLogEvent event, final MoPubErrorCode errorCode,
                             final String errorMsg, final boolean loadRelated) {

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
        boolean isMute = MintegralAdapterConfiguration.isMute();
        int muteStatus = isMute ? REWARD_VIDEO_PLAY_MUTE : REWARD_VIDEO_PLAY_NOT_MUTE;

        if (mMtgRewardVideoHandler != null) {
            mMtgRewardVideoHandler.playVideoMute(muteStatus);
        } else if (mtgBidRewardVideoHandler != null) {
            mtgBidRewardVideoHandler.playVideoMute(muteStatus);
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
        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "onLoadSuccess: " + placementId
                + "  " + unitId);
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
        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "onEndcardShow: " + placementId
                + ", " + unitId);
    }

    @Override
    public void onVideoComplete(String placementId, String unitId) {
        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "onVideoComplete: " +
                placementId + ", " + unitId);
    }
}
