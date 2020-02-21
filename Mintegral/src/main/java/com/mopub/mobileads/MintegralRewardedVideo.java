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

public class MintegralRewardedVideo extends CustomEventRewardedVideo implements RewardVideoListener {

    private final String ADAPTER_NAME = this.getClass().getSimpleName();

    private Context mContext;
    private MTGRewardVideoHandler mMtgRewardVideoHandler;
    private MTGBidRewardVideoHandler mtgBidRewardVideoHandler;

    private static boolean isInitialized = false;
    private String mAdUnitId;
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
                                            @NonNull final Map<String, Object> localExtras,
                                            @NonNull final Map<String, String> serverExtras) throws Exception {

        Preconditions.checkNotNull(launcherActivity);
        Preconditions.checkNotNull(localExtras);
        Preconditions.checkNotNull(serverExtras);

        mContext = launcherActivity.getApplicationContext();
        mUserId = MintegralAdapterConfiguration.getUserId();
        mRewardId = MintegralAdapterConfiguration.getRewardId();

        if (!serverDataIsValid(serverExtras, mContext)) {
            failAdapter(LOAD_FAILED, ADAPTER_CONFIGURATION_ERROR, "One or " +
                    "more keys used for Mintegral's ad requests are empty. Failing adapter. " +
                    "Please ensure you have populated all the required keys on the MoPub " +
                    "dashboard", true);

            return false;
        }

        return isInitialized;
    }

    @Override
    protected void loadWithSdkInitialized(@NonNull final Activity activity,
                                          @NonNull final Map<String, Object> localExtras,
                                          @NonNull final Map<String, String> serverExtras) throws Exception {

        Preconditions.checkNotNull(activity);
        Preconditions.checkNotNull(localExtras);
        Preconditions.checkNotNull(serverExtras);

        if (!serverDataIsValid(serverExtras, mContext)) {
            failAdapter(LOAD_FAILED, ADAPTER_CONFIGURATION_ERROR, "One or " +
                            "more keys used for Mintegral's ad requests are empty. Failing adapter. Please " +
                            "ensure you have populated all the required keys on the MoPub dashboard",
                    true);

            return;
        }

        MintegralAdapterConfiguration.addChannel();
        MintegralAdapterConfiguration.setTargeting(MIntegralSDKFactory.getMIntegralSDK());

        final String adm = serverExtras.get(ADM_KEY);

        if (TextUtils.isEmpty(adm)) {
            mMtgRewardVideoHandler = new MTGRewardVideoHandler(mAdUnitId);

            mMtgRewardVideoHandler.setRewardVideoListener(this);
            mMtgRewardVideoHandler.load();
        } else {
            mtgBidRewardVideoHandler = new MTGBidRewardVideoHandler(mAdUnitId);

            mtgBidRewardVideoHandler.setRewardVideoListener(this);
            mtgBidRewardVideoHandler.loadFromBid(adm);
        }

        MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);
    }

    @Override
    protected void showVideo() {

        if (mMtgRewardVideoHandler != null && mMtgRewardVideoHandler.isReady()) {
            mMtgRewardVideoHandler.show(mRewardId, mUserId);

            MoPubLog.log(getAdNetworkId(), SHOW_ATTEMPTED, ADAPTER_NAME);
        } else if (mtgBidRewardVideoHandler != null && mtgBidRewardVideoHandler.isBidReady()) {
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
            mMtgRewardVideoHandler.clearVideoCache();
            mMtgRewardVideoHandler = null;
        }

        if (mtgBidRewardVideoHandler != null) {
            mtgBidRewardVideoHandler.clearVideoCache();
            mtgBidRewardVideoHandler = null;
        }
    }

    @Override
    protected boolean hasVideoAvailable() {
        if (mMtgRewardVideoHandler != null) {
            return mMtgRewardVideoHandler.isReady();
        }

        if (mtgBidRewardVideoHandler != null) {
            return mtgBidRewardVideoHandler.isBidReady();
        }

        return false;
    }

    private boolean serverDataIsValid(final Map<String, String> serverExtras, Context context) {

        if (serverExtras != null && !serverExtras.isEmpty()) {
            mAdUnitId = serverExtras.get(MintegralAdapterConfiguration.UNIT_ID_KEY);
            final String appId = serverExtras.get(MintegralAdapterConfiguration.APP_ID_KEY);
            final String appKey = serverExtras.get(MintegralAdapterConfiguration.APP_KEY);

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

        if (loadRelated) {
            MoPubRewardedVideoManager.onRewardedVideoLoadFailure(this.getClass(),
                    mAdUnitId, errorCode);
        } else {
            MoPubRewardedVideoManager.onRewardedVideoPlaybackError(this.getClass(),
                    mAdUnitId, errorCode);
        }
    }

    @Override
    public void onLoadSuccess(String message) {
        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "onLoadSuccess: " + message);
    }

    @Override
    public void onAdClose(boolean b, String label, float amount) {
        if (b) {
            MoPubRewardedVideoManager.onRewardedVideoCompleted(this.getClass(), mAdUnitId,
                    MoPubReward.success(label, (int) amount));

            MoPubLog.log(getAdNetworkId(), SHOULD_REWARD, ADAPTER_NAME, amount, label);
        }

        MoPubRewardedVideoManager.onRewardedVideoClosed(this.getClass(), mAdUnitId);
        MoPubLog.log(getAdNetworkId(), DID_DISAPPEAR, ADAPTER_NAME);
    }

    @Override
    public void onVideoLoadFail(String errorMsg) {
        failAdapter(LOAD_FAILED, UNSPECIFIED, errorMsg, true);
    }

    @Override
    public void onVideoLoadSuccess(String s) {
        MoPubRewardedVideoManager.onRewardedVideoLoadSuccess(this.getClass(), mAdUnitId);
        MoPubLog.log(getAdNetworkId(), LOAD_SUCCESS, ADAPTER_NAME);
    }

    @Override
    public void onAdShow() {
        MoPubRewardedVideoManager.onRewardedVideoStarted(this.getClass(), mAdUnitId);
        MoPubLog.log(getAdNetworkId(), SHOW_SUCCESS, ADAPTER_NAME);
    }

    @Override
    public void onShowFail(String errorMsg) {
        failAdapter(SHOW_FAILED, VIDEO_PLAYBACK_ERROR, errorMsg, false);
    }

    @Override
    public void onVideoAdClicked(String s) {
        MoPubRewardedVideoManager.onRewardedVideoClicked(this.getClass(), mAdUnitId);
        MoPubLog.log(getAdNetworkId(), CLICKED, ADAPTER_NAME);
    }

    @Override
    public void onEndcardShow(String message) {
        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "onEndcardShow: " + message);
    }

    @Override
    public void onVideoComplete(String message) {
        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "onVideoComplete: " + message);
    }
}
