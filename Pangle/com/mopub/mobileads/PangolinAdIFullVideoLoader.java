package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import com.bytedance.sdk.openadsdk.AdSlot;
import com.bytedance.sdk.openadsdk.TTAdNative;
import com.bytedance.sdk.openadsdk.TTFullScreenVideoAd;
import com.mopub.common.logging.MoPubLog;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.mobileads.ErrorCode.mapErrorCode;
import static com.mopub.mobileads.PangolinAudienceAdAdapterInterstitial.ADAPTER_NAME;

/**
 * created by wuzejian on 2020/5/11
 */
public class PangolinAdIFullVideoLoader {
    private Context mContext;
    private CustomEventInterstitial.CustomEventInterstitialListener mFullVideoListener;
    private boolean mIsLoaded;
    private TTFullScreenVideoAd mTTFullScreenVideoAd;

    public PangolinAdIFullVideoLoader(Context context, CustomEventInterstitial.CustomEventInterstitialListener fullVideoListener) {
        this.mContext = context;
        this.mFullVideoListener = fullVideoListener;
    }

    public void loadAdFullVideoListener(AdSlot adSlot, TTAdNative ttAdNative) {
        if (ttAdNative == null || mContext == null || adSlot == null || TextUtils.isEmpty(adSlot.getCodeId())) {
            return;
        }
        ttAdNative.loadFullScreenVideoAd(adSlot, mLoadFullVideoAdListener);
    }

    public void showFullVideo(Activity activity) {
        if (mTTFullScreenVideoAd != null && mIsLoaded) {
            mTTFullScreenVideoAd.showFullScreenVideoAd(activity);
        }
    }

    public void destroy() {
        mContext = null;
        mFullVideoListener = null;
        mTTFullScreenVideoAd = null;
        mLoadFullVideoAdListener = null;
        mFullScreenVideoAdInteractionListener = null;
    }


    private TTAdNative.FullScreenVideoAdListener mLoadFullVideoAdListener = new TTAdNative.FullScreenVideoAdListener() {
        @Override
        public void onError(int code, String message) {
            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME, "Loading Full Video creative encountered an error: " + mapErrorCode(code).toString() + ",error message:" + message);
            if (mFullVideoListener != null) {
                mFullVideoListener.onInterstitialFailed(ErrorCode.mapErrorCode(code));
            }
        }

        @Override
        public void onFullScreenVideoAdLoad(TTFullScreenVideoAd ad) {
            if (ad != null) {
                mIsLoaded = true;
                mTTFullScreenVideoAd = ad;
                mTTFullScreenVideoAd.setFullScreenVideoAdInteractionListener(mFullScreenVideoAdInteractionListener);
                if (mFullVideoListener != null) {
                    mFullVideoListener.onInterstitialLoaded();
                }
                MoPubLog.log(LOAD_SUCCESS, ADAPTER_NAME);

            } else {
                if (mFullVideoListener != null) {
                    mFullVideoListener.onInterstitialFailed(ErrorCode.mapErrorCode(ErrorCode.NO_AD));
                }
                MoPubLog.log(LOAD_FAILED, ADAPTER_NAME, " mTTFullScreenVideoAd is null !");
            }
        }

        @Override
        public void onFullScreenVideoCached() {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, " mTTFullScreenVideoAd onFullScreenVideoCached invoke !");
        }
    };

    private TTFullScreenVideoAd.FullScreenVideoAdInteractionListener mFullScreenVideoAdInteractionListener = new TTFullScreenVideoAd.FullScreenVideoAdInteractionListener() {

        @Override
        public void onAdShow() {
            if (mFullVideoListener != null) {
                mFullVideoListener.onInterstitialShown();
            }
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "TTFullScreenVideoAd onAdShow...");
        }

        @Override
        public void onAdVideoBarClick() {
            if (mFullVideoListener != null) {
                mFullVideoListener.onInterstitialClicked();
            }
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "TTFullScreenVideoAd onAdVideoBarClick...");
        }

        @Override
        public void onAdClose() {
            if (mFullVideoListener != null) {
                mFullVideoListener.onInterstitialDismissed();
            }
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "TTFullScreenVideoAd onAdClose...");
        }

        @Override
        public void onVideoComplete() {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "TTFullScreenVideoAd onVideoComplete...");
        }

        @Override
        public void onSkippedVideo() {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "TTFullScreenVideoAd onSkippedVideo...");
        }
    };

}

