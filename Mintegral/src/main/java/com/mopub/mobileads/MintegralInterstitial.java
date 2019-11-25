package com.mopub.mobileads;

import android.app.Activity;
import android.app.Application;
import android.content.Context;

import android.text.TextUtils;
import android.util.Log;

import com.mopub.common.MoPub;

import com.mintegral.msdk.MIntegralConstans;
import com.mintegral.msdk.MIntegralSDK;
import com.mintegral.msdk.interstitialvideo.out.InterstitialVideoListener;
import com.mintegral.msdk.interstitialvideo.out.MTGBidInterstitialVideoHandler;
import com.mintegral.msdk.interstitialvideo.out.MTGInterstitialVideoHandler;
import com.mintegral.msdk.out.MIntegralSDKFactory;


import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.mintegral.BuildConfig;


import java.util.Map;


public class MintegralInterstitial extends CustomEventInterstitial implements InterstitialVideoListener {
    private static final String ADAPTER_NAME = MintegralRewardVideo.class.getName();
    MTGInterstitialVideoHandler mInterstitialHandler;
    MTGBidInterstitialVideoHandler mBidInterstitialVideoHandler;
    CustomEventInterstitialListener mCustomEventInterstitialListener;
    private String adUnitId = "";
    static boolean isInitialized = false;


    @Override
    protected void loadInterstitial(Context context, CustomEventInterstitialListener customEventInterstitialListener, Map<String, Object> localExtras, Map<String, String> serverExtras) {
        try {
            mCustomEventInterstitialListener = customEventInterstitialListener;
            String appId = serverExtras.get("appId");
            String appKey = serverExtras.get("appKey");
            adUnitId = serverExtras.get("unitId");
            BuildConfig.addChannel();


            if (!isInitialized && !TextUtils.isEmpty(appId) && !TextUtils.isEmpty(appKey) && !TextUtils.isEmpty(adUnitId)) {
                MIntegralSDK sdk = MIntegralSDKFactory.getMIntegralSDK();
                if (!MoPub.canCollectPersonalInformation()) {
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
                BuildConfig.parseLocalExtras(localExtras, sdk);
                isInitialized = true;
            } else {
                if (mCustomEventInterstitialListener != null) {
                    mCustomEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (context instanceof Activity) {
            String adm = (String) serverExtras.get("adm");
            if (TextUtils.isEmpty(adm)) {
                mInterstitialHandler = new MTGInterstitialVideoHandler((Activity) context, adUnitId);
                mInterstitialHandler.setRewardVideoListener(this);
                mInterstitialHandler.load();
            } else {
                mBidInterstitialVideoHandler = new MTGBidInterstitialVideoHandler((Activity) context, adUnitId);
                mBidInterstitialVideoHandler.setRewardVideoListener(this);
                mBidInterstitialVideoHandler.loadFromBid(adm);
            }
            MoPubLog.log(adUnitId, MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED, new Object[]{ADAPTER_NAME});
        } else {
            if (mCustomEventInterstitialListener != null) {
                mCustomEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            }
        }

    }


    @Override
    public void onVideoLoadSuccess(String s) {
        Log.e(ADAPTER_NAME, "onVideoLoadSuccess");
        MoPubLog.log(adUnitId, MoPubLog.AdapterLogEvent.LOAD_SUCCESS, new Object[]{ADAPTER_NAME});
        if (mCustomEventInterstitialListener != null) {
            mCustomEventInterstitialListener.onInterstitialLoaded();
        }
    }

    @Override
    public void onVideoLoadFail(String s) {
        Log.e(ADAPTER_NAME, "onVideoLoadFail:" + s);
        MoPubLog.log(adUnitId, MoPubLog.AdapterLogEvent.LOAD_FAILED, new Object[]{ADAPTER_NAME});
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
