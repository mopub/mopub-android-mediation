package com.mintegral.adapter.interstitialvideonative.interstitialvideonativeadapter;

import android.app.Activity;
import android.app.Application;
import android.content.Context;

import android.text.TextUtils;
import android.util.Log;

import com.mintegral.adapter.common.AdapterCommonUtil;
import com.mintegral.adapter.common.AdapterTools;

import com.mintegral.adapter.reward.rewardadapter.MintegralRewardVideo;
import com.mintegral.msdk.MIntegralConstans;
import com.mintegral.msdk.MIntegralSDK;
import com.mintegral.msdk.interstitialvideo.out.InterstitialVideoListener;
import com.mintegral.msdk.interstitialvideo.out.MTGBidInterstitialVideoHandler;
import com.mintegral.msdk.interstitialvideo.out.MTGInterstitialVideoHandler;
import com.mintegral.msdk.out.InterstitialListener;
import com.mintegral.msdk.out.MIntegralSDKFactory;
import com.mintegral.msdk.out.MTGInterstitialHandler;
import com.mintegral.msdk.out.MTGRewardVideoHandler;
import com.mintegral.msdk.out.RewardVideoListener;


import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.CustomEventInterstitial;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.mobileads.MoPubRewardedVideoManager;


import java.util.HashMap;
import java.util.Map;


/**
 * Created by songjunjun on 17/8/31.
 */

public class MIntegralInterstititalVideoNativeAdapter extends CustomEventInterstitial implements InterstitialVideoListener {
    private static final String ADAPTER_NAME = MintegralRewardVideo.class.getName();
    MTGInterstitialVideoHandler mInterstitialHandler;
    MTGBidInterstitialVideoHandler mBidInterstitialVideoHandler;
    CustomEventInterstitialListener mCustomEventInterstitialListener;
    private String appId = "";
    private String appKey = "";
    private String unitId = "";


    @Override
    protected void loadInterstitial(Context context, CustomEventInterstitialListener customEventInterstitialListener, Map<String, Object> localExtras, Map<String, String> serverExtras) {
        HashMap<String, Object> hashMap = new HashMap<String, Object>();
        mCustomEventInterstitialListener = customEventInterstitialListener;
        try {
            appId = serverExtras.get("appId");
            appKey = serverExtras.get("appKey");
            unitId = serverExtras.get("unitId");
            AdapterCommonUtil.addChannel();
        } catch (Throwable e1) {
            e1.printStackTrace();
        }

        if (!TextUtils.isEmpty(appId) && !TextUtils.isEmpty(appKey) && !TextUtils.isEmpty(unitId)) {
            MIntegralSDK sdk = MIntegralSDKFactory.getMIntegralSDK();
            if (!AdapterTools.canCollectPersonalInformation()) {
                sdk.setUserPrivateInfoType(context, MIntegralConstans.AUTHORITY_ALL_INFO, MIntegralConstans.IS_SWITCH_OFF);
            } else {
                sdk.setUserPrivateInfoType(context, MIntegralConstans.AUTHORITY_ALL_INFO, MIntegralConstans.IS_SWITCH_ON);
            }
            Map<String, String> map = sdk.getMTGConfigurationMap(appId,
                    appKey);
            if (context instanceof Activity) {
                sdk.init(map, ((Activity) context).getApplication());
            } else if (context instanceof Application) {
                sdk.init(map, context);
            }
            AdapterCommonUtil.parseLocalExtras(localExtras, sdk);
        } else {
            if (mCustomEventInterstitialListener != null) {
                mCustomEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            }
        }

        if (context instanceof Activity) {
            String adm = (String) serverExtras.get("adm");
            adm = "cb3ecd2d-e199-4a24-b625-6046eccf84a4_hk";
            if (TextUtils.isEmpty(adm)) {
                mInterstitialHandler = new MTGInterstitialVideoHandler((Activity) context, unitId);
                mInterstitialHandler.setRewardVideoListener(this);
                mInterstitialHandler.load();
            } else {
                mBidInterstitialVideoHandler = new MTGBidInterstitialVideoHandler((Activity) context, unitId);
                mBidInterstitialVideoHandler.setRewardVideoListener(this);
                mBidInterstitialVideoHandler.loadFromBid(adm);
            }
            MoPubLog.log(unitId, MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED, new Object[]{ADAPTER_NAME});
        } else {
            if (mCustomEventInterstitialListener != null) {
                mCustomEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            }
        }

    }


    @Override
    public void onVideoLoadSuccess(String s) {
        Log.e(ADAPTER_NAME, "onVideoLoadSuccess");
        MoPubLog.log(unitId, MoPubLog.AdapterLogEvent.LOAD_SUCCESS, new Object[]{ADAPTER_NAME});
        if (mCustomEventInterstitialListener != null) {
            mCustomEventInterstitialListener.onInterstitialLoaded();
        }
    }

    @Override
    public void onVideoLoadFail(String s) {
        Log.e(ADAPTER_NAME, "onVideoLoadFail:" + s);
        MoPubLog.log(unitId, MoPubLog.AdapterLogEvent.LOAD_FAILED, new Object[]{ADAPTER_NAME});
        if (mCustomEventInterstitialListener != null) {
            mCustomEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.UNSPECIFIED);
        }
        Log.e(ADAPTER_NAME, "onInterstitialLoadFail");
    }

    @Override
    public void onAdShow() {

        if (mCustomEventInterstitialListener != null) {
            mCustomEventInterstitialListener.onInterstitialShown();
        }
        Log.e(ADAPTER_NAME, "onInterstitialShowSuccess");
    }

    @Override
    public void onShowFail(String s) {
        if (mCustomEventInterstitialListener != null) {
            mCustomEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.UNSPECIFIED);
        }
        Log.e(ADAPTER_NAME, "onInterstitialShowFail");
    }

    @Override
    public void onAdClose(boolean b) {
        if (mCustomEventInterstitialListener != null) {
            mCustomEventInterstitialListener.onInterstitialDismissed();
        }
        Log.e(ADAPTER_NAME, "onInterstitialClosed");
    }

    @Override
    public void onVideoAdClicked(String s) {
        if (mCustomEventInterstitialListener != null) {
            mCustomEventInterstitialListener.onInterstitialClicked();
        }
        Log.e(ADAPTER_NAME, "onInterstitialAdClick");
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
    protected void showInterstitial() {
        Log.e(ADAPTER_NAME, "showInterstitial");
        MoPubLog.log(MoPubLog.AdapterLogEvent.SHOW_ATTEMPTED, new Object[]{ADAPTER_NAME});
        if (mInterstitialHandler != null) {
            mInterstitialHandler.show();
        } else if (mBidInterstitialVideoHandler != null) {
            mBidInterstitialVideoHandler.showFromBid();
        }

    }

    @Override
    protected void onInvalidate() {
    }

    @Override
    public void onLoadSuccess(String s) {
        Log.e(ADAPTER_NAME, "onLoadSuccess");

    }
}
