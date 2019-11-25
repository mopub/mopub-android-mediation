package com.mopub.mobileads;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.mopub.common.MoPub;
import com.mintegral.msdk.MIntegralConstans;
import com.mintegral.msdk.MIntegralSDK;
import com.mintegral.msdk.out.MIntegralSDKFactory;
import com.mintegral.msdk.out.MTGBidRewardVideoHandler;
import com.mintegral.msdk.out.MTGRewardVideoHandler;
import com.mintegral.msdk.out.RewardVideoListener;


import com.mopub.common.LifecycleListener;
import com.mopub.common.MoPubReward;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.mintegral.BuildConfig;

import java.util.Map;


public class MintegralRewardVideo extends CustomEventRewardedVideo implements RewardVideoListener {


    private static final String ADAPTER_NAME = MintegralRewardVideo.class.getName();
    static boolean isInitialized = false;
    private String adUnitId = "";
    private String mUserId = "your user id";

    private MTGRewardVideoHandler mMtgRewardVideoHandler;
    private MTGBidRewardVideoHandler mtgBidRewardVideoHandler;


    @Override
    public void onLoadSuccess(String s) {

    }

    @Override
    public void onAdClose(boolean b, String s, float v) {
        if (b) {
            MoPubRewardedVideoManager.onRewardedVideoCompleted(MintegralRewardVideo.class, null, MoPubReward.success(s, (int) v));
        }
        MoPubRewardedVideoManager.onRewardedVideoClosed(MintegralRewardVideo.class, adUnitId);

    }

    @Override
    public void onVideoLoadFail(String s) {
        Log.e(ADAPTER_NAME, "onVideoLoadFail" + s);
        MoPubRewardedVideoManager.onRewardedVideoLoadFailure(MintegralRewardVideo.class, adUnitId, MoPubErrorCode.UNSPECIFIED);

    }

    @Override
    public void onVideoLoadSuccess(String s) {
        Log.e(ADAPTER_NAME, "onVideoLoadSuccess" + s);
        MoPubRewardedVideoManager.onRewardedVideoLoadSuccess(MintegralRewardVideo.class, adUnitId);

    }

    @Override
    public void onAdShow() {
        Log.e(ADAPTER_NAME, "onAdShow");
        MoPubRewardedVideoManager.onRewardedVideoStarted(MintegralRewardVideo.class, adUnitId);
    }

    @Override
    public void onShowFail(String s) {
        Log.e(ADAPTER_NAME, "onShowFail");
        MoPubRewardedVideoManager.onRewardedVideoPlaybackError(MintegralRewardVideo.class, adUnitId, MoPubErrorCode.VIDEO_PLAYBACK_ERROR);
    }


    @Override
    public void onVideoAdClicked(String s) {
        Log.e(ADAPTER_NAME, "onVideoAdClicked");
        MoPubRewardedVideoManager.onRewardedVideoClicked(MintegralRewardVideo.class, adUnitId);
    }

    @Override
    public void onEndcardShow(String s) {
        Log.e(ADAPTER_NAME, "onEndcardShow");
    }


    @Override
    public void onVideoComplete(String s) {
        Log.e(ADAPTER_NAME, "onVideoComplete");
    }

    @Override
    protected boolean checkAndInitializeSdk(@NonNull Activity launcherActivity, @NonNull Map<String, Object> localExtras, @NonNull Map<String, String> serverExtras) throws Exception {

        if (localExtras != null) {
            Object mCustomer = localExtras.get("Rewarded-Video-Customer-Id");
            if (mCustomer instanceof String) {
                mUserId = mCustomer.toString();
            }
        }
        BuildConfig.addChannel();

        if (launcherActivity == null) {
            MoPubRewardedVideoManager
                    .onRewardedVideoLoadFailure(MintegralRewardVideo.class, getAdNetworkId(), MoPubErrorCode.UNSPECIFIED);
            return false;
        }

        String appId = (String) serverExtras.get("appId");

        String appKey = (String) serverExtras.get("appKey");


        if (!isInitialized && !TextUtils.isEmpty(appId) && !TextUtils.isEmpty(appKey)) {
            MIntegralSDK sdk = MIntegralSDKFactory.getMIntegralSDK();
            if (!MoPub.canCollectPersonalInformation()) {
                sdk.setUserPrivateInfoType(launcherActivity.getApplication(), MIntegralConstans.AUTHORITY_ALL_INFO, MIntegralConstans.IS_SWITCH_OFF);
            } else {
                sdk.setUserPrivateInfoType(launcherActivity.getApplication(), MIntegralConstans.AUTHORITY_ALL_INFO, MIntegralConstans.IS_SWITCH_ON);
            }
            Map<String, String> map = sdk.getMTGConfigurationMap(appId, appKey);
            sdk.init(map, launcherActivity.getApplicationContext());
            BuildConfig.parseLocalExtras(localExtras, sdk);
            isInitialized = true;
        }
        return isInitialized;
    }


    @Nullable
    @Override
    protected LifecycleListener getLifecycleListener() {
        return null;
    }

    @NonNull
    @Override
    protected String getAdNetworkId() {
        return adUnitId;
    }

    @Override
    protected void loadWithSdkInitialized(@NonNull Activity activity, @NonNull Map<String, Object> localExtras, @NonNull Map<String, String> serverExtras) throws Exception {
        if (serverExtras.isEmpty()) {
            MoPubRewardedVideoManager.onRewardedVideoLoadFailure(MintegralRewardVideo.class, this.getAdNetworkId(), MoPubErrorCode.INTERNAL_ERROR);
            MoPubLog.log(MoPubLog.AdapterLogEvent.LOAD_FAILED, new Object[]{ADAPTER_NAME, MoPubErrorCode.NETWORK_NO_FILL.getIntCode(), MoPubErrorCode.NETWORK_NO_FILL});
            MoPubLog.log(MoPubLog.AdapterLogEvent.CUSTOM, new Object[]{ADAPTER_NAME, "serverExtras is null or empty."});
            return;
        }
        adUnitId = (String) serverExtras.get("unitId");
        if (TextUtils.isEmpty(adUnitId)) {
            MoPubRewardedVideoManager.onRewardedVideoLoadFailure(MintegralRewardVideo.class, this.getAdNetworkId(), MoPubErrorCode.INTERNAL_ERROR);
            MoPubLog.log(MoPubLog.AdapterLogEvent.LOAD_FAILED, new Object[]{ADAPTER_NAME, MoPubErrorCode.NETWORK_NO_FILL.getIntCode(), MoPubErrorCode.NETWORK_NO_FILL});
            MoPubLog.log(MoPubLog.AdapterLogEvent.CUSTOM, new Object[]{ADAPTER_NAME, "UnitID is null or empty."});
            return;

        }

        String adm = (String) serverExtras.get("adm");

        if (TextUtils.isEmpty(adm)) {
            mMtgRewardVideoHandler = new MTGRewardVideoHandler(adUnitId);
            mMtgRewardVideoHandler.setRewardVideoListener(this);
            mMtgRewardVideoHandler.load();
        } else {
            mtgBidRewardVideoHandler = new MTGBidRewardVideoHandler(adUnitId);
            mtgBidRewardVideoHandler.setRewardVideoListener(this);
            mtgBidRewardVideoHandler.loadFromBid(adm);
        }

    }

    @Override
    protected void showVideo() {
        if (mMtgRewardVideoHandler != null) {
            if (mMtgRewardVideoHandler.isReady()) {
                mMtgRewardVideoHandler.show("1", mUserId);
            } else {
                MoPubRewardedVideoManager.onRewardedVideoPlaybackError(MintegralRewardVideo.class, adUnitId, MoPubErrorCode.VIDEO_CACHE_ERROR);
            }
        } else if (mtgBidRewardVideoHandler != null) {
            if (mtgBidRewardVideoHandler.isBidReady()) {
                mtgBidRewardVideoHandler.showFromBid("1", mUserId);
            } else {
                MoPubRewardedVideoManager.onRewardedVideoPlaybackError(MintegralRewardVideo.class, adUnitId, MoPubErrorCode.VIDEO_CACHE_ERROR);
            }
        }


    }


    @Override
    protected void onInvalidate() {

    }


    @Override
    protected boolean hasVideoAvailable() {
        return mMtgRewardVideoHandler == null ? mtgBidRewardVideoHandler.isBidReady() : mMtgRewardVideoHandler.isReady();
    }


}
