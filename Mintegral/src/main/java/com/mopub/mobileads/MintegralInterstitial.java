package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import com.mintegral.msdk.interstitialvideo.out.InterstitialVideoListener;
import com.mintegral.msdk.interstitialvideo.out.MTGBidInterstitialVideoHandler;
import com.mintegral.msdk.interstitialvideo.out.MTGInterstitialVideoHandler;
import com.mintegral.msdk.out.MIntegralSDKFactory;
import com.mopub.common.logging.MoPubLog;

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

public class MintegralInterstitial extends CustomEventInterstitial implements InterstitialVideoListener {

    private static final String ADAPTER_NAME = MintegralInterstitial.class.getSimpleName();

    private MTGInterstitialVideoHandler mInterstitialHandler;
    private MTGBidInterstitialVideoHandler mBidInterstitialVideoHandler;
    private CustomEventInterstitialListener mCustomEventInterstitialListener;

    private static String mAdUnitId;

    @Override
    protected void loadInterstitial(final Context context,
                                    final CustomEventInterstitialListener customEventInterstitialListener,
                                    final Map<String, Object> localExtras, Map<String, String> serverExtras) {

        mCustomEventInterstitialListener = customEventInterstitialListener;

        if (!serverDataIsValid(serverExtras, context)) {
            failAdapter(LOAD_FAILED, ADAPTER_CONFIGURATION_ERROR, "One or " +
                    "more keys used for Mintegral's ad requests are empty. Failing adapter. Please " +
                    "ensure you have populated all the required keys on the MoPub dashboard.");

            return;
        }

        MintegralAdapterConfiguration.addChannel();
        MintegralAdapterConfiguration.setTargeting(MIntegralSDKFactory.getMIntegralSDK());

        if (context instanceof Activity) {
            final String adm = serverExtras.get(ADM_KEY);

            if (TextUtils.isEmpty(adm)) {
                mInterstitialHandler = new MTGInterstitialVideoHandler(context, mAdUnitId);
                mInterstitialHandler.setRewardVideoListener(this);
                mInterstitialHandler.load();
            } else {
                mBidInterstitialVideoHandler = new MTGBidInterstitialVideoHandler(context, mAdUnitId);
                mBidInterstitialVideoHandler.setRewardVideoListener(this);
                mBidInterstitialVideoHandler.loadFromBid(adm);
            }

            MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);
        } else {
            failAdapter(LOAD_FAILED, ADAPTER_CONFIGURATION_ERROR, "Context is not an instance " +
                    "of Activity. Aborting ad request, and failing adapter.");
        }
    }

    @Override
    protected void showInterstitial() {
        if (mInterstitialHandler != null && mInterstitialHandler.isReady()) {
            mInterstitialHandler.show();
        } else if (mBidInterstitialVideoHandler != null && mBidInterstitialVideoHandler.isBidReady()) {
            mBidInterstitialVideoHandler.showFromBid();
        } else {
            failAdapter(SHOW_FAILED, NETWORK_NO_FILL, "Failed to show Mintegral interstitial " +
                    "because it is not ready. Please make a new ad request.");
        }

        MoPubLog.log(getAdNetworkId(), SHOW_ATTEMPTED, ADAPTER_NAME);
    }

    @Override
    protected void onInvalidate() {
        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Finished showing Mintegral " +
                "interstitial. Invalidating adapter...");

        if (mInterstitialHandler != null) {
            mInterstitialHandler.clearVideoCache();
            mInterstitialHandler = null;
        }

        if (mBidInterstitialVideoHandler != null) {
            mBidInterstitialVideoHandler.clearVideoCache();
            mBidInterstitialVideoHandler = null;
        }

        mCustomEventInterstitialListener = null;
    }

    private void failAdapter(final MoPubLog.AdapterLogEvent event, final MoPubErrorCode errorCode,
                             final String errorMsg) {

        MoPubLog.log(getAdNetworkId(), event, ADAPTER_NAME, errorCode.getIntCode(), errorCode);

        if (!TextUtils.isEmpty(errorMsg)) {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, errorMsg);
        }

        if (mCustomEventInterstitialListener != null) {
            mCustomEventInterstitialListener.onInterstitialFailed(errorCode);
        }
    }

    private boolean serverDataIsValid(final Map<String, String> serverExtras, Context context) {

        if (serverExtras != null && !serverExtras.isEmpty()) {
            mAdUnitId = serverExtras.get(MintegralAdapterConfiguration.UNIT_ID_KEY);
            final String appId = serverExtras.get(MintegralAdapterConfiguration.APP_ID_KEY);
            final String appKey = serverExtras.get(MintegralAdapterConfiguration.APP_KEY);

            if (!TextUtils.isEmpty(appId) && !TextUtils.isEmpty(appKey) && !TextUtils.isEmpty(mAdUnitId)) {
                MintegralAdapterConfiguration.configureMintegral(appId, appKey, context);

                return true;
            }
        }
        return false;
    }

    private static String getAdNetworkId() {
        return mAdUnitId;
    }

    @Override
    public void onVideoLoadSuccess(String s) {
        MoPubLog.log(getAdNetworkId(), LOAD_SUCCESS, ADAPTER_NAME);

        if (mCustomEventInterstitialListener != null) {
            mCustomEventInterstitialListener.onInterstitialLoaded();
        }
    }

    @Override
    public void onVideoLoadFail(String errorMsg) {
        failAdapter(LOAD_FAILED, UNSPECIFIED, errorMsg);
    }

    @Override
    public void onAdShow() {
        MoPubLog.log(getAdNetworkId(), SHOW_SUCCESS, ADAPTER_NAME);

        if (mCustomEventInterstitialListener != null) {
            mCustomEventInterstitialListener.onInterstitialShown();
        }
    }

    @Override
    public void onShowFail(String errorMsg) {
        failAdapter(SHOW_FAILED, UNSPECIFIED, "Failed to show Mintegral interstitial: "
                + errorMsg);
    }

    @Override
    public void onAdClose(boolean b) {
        MoPubLog.log(getAdNetworkId(), DID_DISAPPEAR, ADAPTER_NAME);
        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "onAdClose");

        if (mCustomEventInterstitialListener != null) {
            mCustomEventInterstitialListener.onInterstitialDismissed();
        }
    }

    @Override
    public void onVideoAdClicked(String message) {
        MoPubLog.log(getAdNetworkId(), CLICKED, ADAPTER_NAME);

        if (mCustomEventInterstitialListener != null) {
            mCustomEventInterstitialListener.onInterstitialClicked();
        }
    }

    @Override
    public void onEndcardShow(String message) {
        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "onEndcardShow");
    }

    @Override
    public void onVideoComplete(String message) {
        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "onVideoComplete: " + message);
    }

    @Override
    public void onLoadSuccess(String message) {
        MoPubLog.log(getAdNetworkId(), LOAD_SUCCESS, ADAPTER_NAME);
    }
}
