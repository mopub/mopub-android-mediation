package com.mintegral.adapter.reward.rewardadapter;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.mintegral.adapter.common.AdapterCommonUtil;
import com.mintegral.adapter.common.AdapterTools;
import com.mintegral.msdk.MIntegralConstans;
import com.mintegral.msdk.MIntegralSDK;
import com.mintegral.msdk.out.MIntegralSDKFactory;
import com.mintegral.msdk.out.MTGBidRewardVideoHandler;
import com.mintegral.msdk.out.MTGRewardVideoHandler;
import com.mintegral.msdk.out.RewardVideoListener;


import com.mopub.common.LifecycleListener;
import com.mopub.common.MediationSettings;
import com.mopub.common.MoPubReward;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.CustomEventRewardedVideo;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.mobileads.MoPubRewardedVideoManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;


/**
 * Created by songjunjun on 17/3/22.
 */

public class MintegralRewardVideo extends CustomEventRewardedVideo implements RewardVideoListener {


    private static final String ADAPTER_NAME = MintegralRewardVideo.class.getName();
    private JSONObject serverParams;
    boolean isInitialized = false;
    private String appId = "";
    private String appKey = "";
    private String unitId = "";
    private String mRewardId = "";
    private String mUserId = "your user id";


    private MTGRewardVideoHandler mMtgRewardVideoHandler;
    private MTGBidRewardVideoHandler mtgBidRewardVideoHandler;
    private final static int LOAD_CANCEL_TIME = 20 * 1000;
    private final static int TIME_OUT_CODE = 0X001;
    private boolean hasRetrue = false;


    @Override
    public void onLoadSuccess(String s) {

    }

    @Override
    public void onAdClose(boolean b, String s, float v) {
        if (b) {
            MoPubRewardedVideoManager.onRewardedVideoCompleted(MintegralRewardVideo.class, null, MoPubReward.success(s, (int) v));
        }
        MoPubRewardedVideoManager.onRewardedVideoClosed(MintegralRewardVideo.class, unitId);

    }

    @Override
    public void onVideoLoadFail(String s) {
        Log.e(ADAPTER_NAME, "====MTG_onVideoLoadFail" + s);
        if (!hasRetrue) {
            hasRetrue = true;
            MoPubRewardedVideoManager.onRewardedVideoLoadFailure(MintegralRewardVideo.class, unitId, MoPubErrorCode.UNSPECIFIED);

        }
    }

    @Override
    public void onVideoLoadSuccess(String s) {
        Log.e(ADAPTER_NAME, "onVideoLoadSuccess" + s);
        MoPubRewardedVideoManager.onRewardedVideoLoadSuccess(MintegralRewardVideo.class, unitId);

    }

    @Override
    public void onAdShow() {
        Log.e(ADAPTER_NAME, "onAdShow");
        MoPubRewardedVideoManager.onRewardedVideoStarted(MintegralRewardVideo.class, unitId);
    }

    @Override
    public void onShowFail(String s) {
        Log.e(ADAPTER_NAME, "onShowFail");
        MoPubRewardedVideoManager.onRewardedVideoPlaybackError(MintegralRewardVideo.class, unitId, MoPubErrorCode.VIDEO_PLAYBACK_ERROR);
    }


    @Override
    public void onVideoAdClicked(String s) {
        Log.e(ADAPTER_NAME, "onVideoAdClicked");
        MoPubRewardedVideoManager.onRewardedVideoClicked(MintegralRewardVideo.class, unitId);
    }

    @Override
    public void onEndcardShow(String s) {
        Log.e(ADAPTER_NAME, "onEndcardShow");
    }


    @Override
    public void onVideoComplete(String s) {
//        MoPubRewardedVideoManager.onRewardedVideoCompleted(MintegralRewardVideo.class,unitId);
    }

    @Override
    protected boolean checkAndInitializeSdk(@NonNull Activity launcherActivity, @NonNull Map<String, Object> localExtras, @NonNull Map<String, String> serverExtras) throws Exception {


        //获取当前的用户id  get User id
        if (localExtras != null) {
            Object mCustomer = localExtras.get("Rewarded-Video-Customer-Id");
            if (mCustomer instanceof String) {
                mUserId = mCustomer.toString();
            }
        }
        AdapterCommonUtil.addChannel();

        if (launcherActivity == null) {
            MoPubRewardedVideoManager
                    .onRewardedVideoLoadFailure(MintegralRewardVideo.class, getAdNetworkId(), MoPubErrorCode.UNSPECIFIED);
            return false;
        }

        appId = (String) serverExtras.get("appId");

        appKey = (String) serverExtras.get("appKey");


        if (!isInitialized && !TextUtils.isEmpty(appId) && !TextUtils.isEmpty(appKey)) {
            MIntegralSDK sdk = MIntegralSDKFactory.getMIntegralSDK();
            if (!AdapterTools.canCollectPersonalInformation()) {
                sdk.setUserPrivateInfoType(launcherActivity.getApplication(), MIntegralConstans.AUTHORITY_ALL_INFO, MIntegralConstans.IS_SWITCH_OFF);
            } else {
                sdk.setUserPrivateInfoType(launcherActivity.getApplication(), MIntegralConstans.AUTHORITY_ALL_INFO, MIntegralConstans.IS_SWITCH_ON);
            }
            Map<String, String> map = sdk.getMTGConfigurationMap(appId, appKey);

            sdk.init(map, launcherActivity.getApplicationContext());
            isInitialized = true;
            AdapterCommonUtil.parseLocalExtras(localExtras, sdk);
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
        return unitId;
    }

    @Override
    protected void loadWithSdkInitialized(@NonNull Activity activity, @NonNull Map<String, Object> localExtras, @NonNull Map<String, String> serverExtras) throws Exception {
        if (serverExtras.isEmpty()) {
            MoPubRewardedVideoManager.onRewardedVideoLoadFailure(MintegralRewardVideo.class, this.getAdNetworkId(), MoPubErrorCode.INTERNAL_ERROR);
            MoPubLog.log(MoPubLog.AdapterLogEvent.LOAD_FAILED, new Object[]{ADAPTER_NAME, MoPubErrorCode.NETWORK_NO_FILL.getIntCode(), MoPubErrorCode.NETWORK_NO_FILL});
            MoPubLog.log(MoPubLog.AdapterLogEvent.CUSTOM, new Object[]{ADAPTER_NAME, "serverExtras is null or empty."});
            return;
        }
        mRewardId = (String) serverExtras.get("rewardId");
        unitId = (String) serverExtras.get("unitId");
        if (TextUtils.isEmpty(unitId)) {
            MoPubRewardedVideoManager.onRewardedVideoLoadFailure(MintegralRewardVideo.class, this.getAdNetworkId(), MoPubErrorCode.INTERNAL_ERROR);
            MoPubLog.log(MoPubLog.AdapterLogEvent.LOAD_FAILED, new Object[]{ADAPTER_NAME, MoPubErrorCode.NETWORK_NO_FILL.getIntCode(), MoPubErrorCode.NETWORK_NO_FILL});
            MoPubLog.log(MoPubLog.AdapterLogEvent.CUSTOM, new Object[]{ADAPTER_NAME, "UnitID is null or empty."});
            return;

        }
        String adm = (String) serverExtras.get("adm");
//        adm="3cb283c6-5967-4e57-8dd7-5cd4aeff3741_hk";
        AdapterCommonUtil.addChannel();

        Log.e(ADAPTER_NAME, "====load");

        mMtgRewardVideoHandler = null;
        mtgBidRewardVideoHandler = null;

        if (TextUtils.isEmpty(adm)) {
            mMtgRewardVideoHandler = new MTGRewardVideoHandler(unitId);
            mMtgRewardVideoHandler.setRewardVideoListener(this);
            mMtgRewardVideoHandler.load();
        } else {
            mtgBidRewardVideoHandler = new MTGBidRewardVideoHandler(unitId);
            mtgBidRewardVideoHandler.setRewardVideoListener(this);
            mtgBidRewardVideoHandler.loadFromBid(adm);
        }

    }

    @Override
    protected void showVideo() {
        if (mMtgRewardVideoHandler != null) {
            if (mMtgRewardVideoHandler.isReady()) {
                mMtgRewardVideoHandler.show(mRewardId, mUserId);
            } else {
                MoPubRewardedVideoManager.onRewardedVideoPlaybackError(MintegralRewardVideo.class, unitId, MoPubErrorCode.VIDEO_CACHE_ERROR);
            }
        } else if (mtgBidRewardVideoHandler != null) {
            if (mtgBidRewardVideoHandler.isBidReady()) {
                mtgBidRewardVideoHandler.showFromBid(mRewardId, mUserId);
            } else {
                MoPubRewardedVideoManager.onRewardedVideoPlaybackError(MintegralRewardVideo.class, unitId, MoPubErrorCode.VIDEO_CACHE_ERROR);
            }
        }


    }


    @Override
    protected void onInvalidate() {

    }

    @Nullable
    @Override
    protected CustomEventRewardedVideoListener getVideoListenerForSdk() {
        return null;
    }

    @Override
    protected boolean hasVideoAvailable() {
        return mMtgRewardVideoHandler == null ? mtgBidRewardVideoHandler.isBidReady() : mMtgRewardVideoHandler.isReady();
    }


}
