package com.mopub.mobileads;

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

public class MintegralRewardedVideo extends BaseAd implements RewardVideoListener {
    private final String ADAPTER_NAME = this.getClass().getSimpleName();
    private final MintegralAdapterConfiguration mMintegralAdapterConfiguration;

    private MBBidRewardVideoHandler mBiddingRewardedVideo;
    private MBRewardVideoHandler mRewardedVideo;

    private String mAdUnitId;
    private String mAppId;
    private String mAppKey;
    private String mPlacementId;
    private String mRewardId;
    private String mUserId;

    public MintegralRewardedVideo() {
        mMintegralAdapterConfiguration = new MintegralAdapterConfiguration();
    }

    @Nullable
    @Override
    protected LifecycleListener getLifecycleListener() {
        return null;
    }

    @NonNull
    @Override
    protected String getAdNetworkId() {
        return mAdUnitId != null ? mAdUnitId : "";
    }

    @Override
    protected boolean checkAndInitializeSdk(@NonNull final Activity launcherActivity,
                                            @NonNull final AdData adData) {
        Preconditions.checkNotNull(launcherActivity);
        Preconditions.checkNotNull(adData);

        mUserId = MintegralAdapterConfiguration.getUserId();
        mRewardId = MintegralAdapterConfiguration.getRewardId();

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
                    "populated all the required keys on the MoPub dashboard", true);
            return;
        }

        MintegralAdapterConfiguration.addChannel();
        MintegralAdapterConfiguration.setTargeting(MBridgeSDKFactory.getMBridgeSDK());
        MintegralAdapterConfiguration.configureMintegralSdk(mAppId, mAppKey, context,
                new MintegralSdkManager.MBSDKInitializeListener() {
                    @Override
                    public void onInitializeSuccess(String appKey, String appID) {
                        loadRewardVideo(context, extras);

                        MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);
                    }

                    @Override
                    public void onInitializeFailure(String message) {
                        failAdapter(LOAD_FAILED, ADAPTER_CONFIGURATION_ERROR, "Mintegral " +
                                "SDK initialization failed: " + message + ". Failing ad request", true);
                    }
                });
    }

    private void loadRewardVideo(Context context, Map<String, String> extras) {
        final String adMarkup = extras.get(ADM_KEY);

        if (TextUtils.isEmpty(adMarkup)) {
            mRewardedVideo = new MBRewardVideoHandler(mPlacementId, mAdUnitId);
            mRewardedVideo.setRewardVideoListener(MintegralRewardedVideo.this);
            mRewardedVideo.load();
        } else {
            mBiddingRewardedVideo = new MBBidRewardVideoHandler(mPlacementId, mAdUnitId);
            mBiddingRewardedVideo.setRewardVideoListener(MintegralRewardedVideo.this);
            mBiddingRewardedVideo.loadFromBid(adMarkup);
        }

        handleAudio();

        mMintegralAdapterConfiguration.setCachedInitializationParameters(context, extras);
    }

    @Override
    protected void show() {
        if (mRewardedVideo != null && mRewardedVideo.isReady()) {
            mRewardedVideo.show(mRewardId, mUserId);
        } else if (mBiddingRewardedVideo != null && mBiddingRewardedVideo.isBidReady()) {
            mBiddingRewardedVideo.showFromBid(mRewardId, mUserId);
        } else {
            failAdapter(SHOW_FAILED, NETWORK_NO_FILL, "There is no Mintegral rewarded video " +
                    "available. Please make a new ad request.", false);
        }

        handleAudio();
        MoPubLog.log(getAdNetworkId(), SHOW_ATTEMPTED, ADAPTER_NAME);
    }

    @Override
    protected void onInvalidate() {
        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Finished showing Mintegral rewarded ad. " +
                "Invalidating adapter.");

        if (mRewardedVideo != null) {
            mRewardedVideo.setRewardVideoListener(null);
            mRewardedVideo = null;
        }

        if (mBiddingRewardedVideo != null) {
            mBiddingRewardedVideo.setRewardVideoListener(null);
            mBiddingRewardedVideo = null;
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
        final boolean isMute = MintegralAdapterConfiguration.isMute();
        final int muteStatus = isMute ? MBridgeConstans.REWARD_VIDEO_PLAY_MUTE :
                MBridgeConstans.REWARD_VIDEO_PLAY_NOT_MUTE;

        if (mRewardedVideo != null) {
            mRewardedVideo.playVideoMute(muteStatus);
        } else if (mBiddingRewardedVideo != null) {
            mBiddingRewardedVideo.playVideoMute(muteStatus);
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
        // NO-OP
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
        failAdapter(SHOW_FAILED, MoPubErrorCode.AD_SHOW_ERROR, errorMsg, false);
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
        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "onVideoComplete: " + placementId
                + ", " + unitId);
    }
}
