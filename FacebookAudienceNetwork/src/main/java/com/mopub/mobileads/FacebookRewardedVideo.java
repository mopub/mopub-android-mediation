package com.mopub.mobileads;

import android.app.Activity;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.AdSettings;
import com.facebook.ads.AudienceNetworkAds;
import com.facebook.ads.RewardedVideoAd;
import com.facebook.ads.RewardedVideoAdListener;
import com.mopub.common.DataKeys;
import com.mopub.common.LifecycleListener;
import com.mopub.common.MoPub;
import com.mopub.common.MoPubReward;
import com.mopub.common.logging.MoPubLog;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOULD_REWARD;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;
import static com.mopub.mobileads.MoPubErrorCode.EXPIRED;

public class FacebookRewardedVideo extends CustomEventRewardedVideo implements RewardedVideoAdListener {

    private static final int ONE_HOURS_MILLIS = 60 * 60 * 1000;
    private static final String ADAPTER_NAME = FacebookRewardedVideo.class.getSimpleName();
    private static AtomicBoolean sIsInitialized = new AtomicBoolean(false);
    @Nullable
    private RewardedVideoAd mRewardedVideoAd;
    @NonNull
    private String mPlacementId = "";
    @NonNull
    private Handler mHandler;
    private Runnable mAdExpiration;
    @NonNull
    private FacebookAdapterConfiguration mFacebookAdapterConfiguration;

    public FacebookRewardedVideo() {
        mHandler = new Handler();
        mFacebookAdapterConfiguration = new FacebookAdapterConfiguration();

        mAdExpiration = new Runnable() {
            @Override
            public void run() {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "Expiring unused Facebook Rewarded Video ad due to Facebook's 60-minute expiration policy.");
                MoPubRewardedVideoManager.onRewardedVideoLoadFailure(FacebookRewardedVideo.class, mPlacementId, EXPIRED);
                MoPubLog.log(LOAD_FAILED, ADAPTER_NAME, MoPubErrorCode.EXPIRED.getIntCode(), MoPubErrorCode.EXPIRED);

                onInvalidate();
            }
        };
    }

    /**
     * CustomEventRewardedVideo implementation
     */

    @Nullable
    @Override
    protected LifecycleListener getLifecycleListener() {
        return null;
    }

    @Override
    protected boolean checkAndInitializeSdk(@NonNull Activity launcherActivity, @NonNull Map<String, Object> localExtras, @NonNull Map<String, String> serverExtras) throws Exception {
        boolean requiresInitialization = !sIsInitialized.getAndSet(true);
        if (requiresInitialization) {
            AudienceNetworkAds.initialize(launcherActivity);
        }
        return requiresInitialization;
    }

    @Override
    protected void loadWithSdkInitialized(@NonNull Activity activity, @NonNull Map<String, Object> localExtras, @NonNull Map<String, String> serverExtras) throws Exception {
        if (!serverExtras.isEmpty()) {
            mPlacementId = serverExtras.get("placement_id");
            mFacebookAdapterConfiguration.setCachedInitializationParameters(activity.getApplicationContext(), serverExtras);

            if (!TextUtils.isEmpty(mPlacementId)) {
                if (mRewardedVideoAd != null) {
                    mRewardedVideoAd.destroy();
                    mRewardedVideoAd = null;
                }
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "Creating a Facebook Rewarded Video instance, and registering callbacks.");
                mRewardedVideoAd = new RewardedVideoAd(activity, mPlacementId);
                mRewardedVideoAd.setAdListener(this);
            } else {
                MoPubRewardedVideoManager.onRewardedVideoLoadFailure(FacebookRewardedVideo.class, getAdNetworkId(), MoPubErrorCode.NETWORK_NO_FILL);
                MoPubLog.log(LOAD_FAILED, ADAPTER_NAME, MoPubErrorCode.NETWORK_NO_FILL.getIntCode(), MoPubErrorCode.NETWORK_NO_FILL);
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "Placement ID is null or empty.");
                return;
            }
        }

        if (mRewardedVideoAd != null) {
            if (mRewardedVideoAd.isAdLoaded()) {
                MoPubRewardedVideoManager.onRewardedVideoLoadSuccess(FacebookRewardedVideo.class, mPlacementId);
                MoPubLog.log(LOAD_SUCCESS, ADAPTER_NAME);
                return;
            }

            AdSettings.setMediationService("MOPUB_" + MoPub.SDK_VERSION);

            final String adm = serverExtras.get(DataKeys.ADM_KEY);
            if (!TextUtils.isEmpty(adm)) {
                mRewardedVideoAd.loadAdFromBid(adm);
                MoPubLog.log(mPlacementId, LOAD_ATTEMPTED, ADAPTER_NAME);
            } else {
                mRewardedVideoAd.loadAd();
                MoPubLog.log(mPlacementId, LOAD_ATTEMPTED, ADAPTER_NAME);
            }
        }
    }

    @NonNull
    @Override
    protected String getAdNetworkId() {
        return mPlacementId;
    }

    @Override
    protected void onInvalidate() {
        cancelExpirationTimer();
        if (mRewardedVideoAd != null) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Performing cleanup tasks...");
            mRewardedVideoAd.setAdListener(null);
            mRewardedVideoAd.destroy();
            mRewardedVideoAd = null;
        }
    }

    @Override
    protected boolean hasVideoAvailable() {
        return mRewardedVideoAd != null && mRewardedVideoAd.isAdLoaded();
    }

    @Override
    protected void showVideo() {
        MoPubLog.log(SHOW_ATTEMPTED, ADAPTER_NAME);
        if (mRewardedVideoAd != null && hasVideoAvailable()) {
            mRewardedVideoAd.show();
        } else {
            MoPubRewardedVideoManager.onRewardedVideoPlaybackError(FacebookRewardedVideo.class, mPlacementId, MoPubErrorCode.NETWORK_NO_FILL);
            MoPubLog.log(SHOW_FAILED, ADAPTER_NAME, MoPubErrorCode.NETWORK_NO_FILL.getIntCode(), MoPubErrorCode.NETWORK_NO_FILL);
        }
    }

    @Override
    public void onRewardedVideoCompleted() {
        MoPubRewardedVideoManager.onRewardedVideoCompleted(FacebookRewardedVideo.class, mPlacementId, MoPubReward.success(MoPubReward.NO_REWARD_LABEL, MoPubReward.DEFAULT_REWARD_AMOUNT));
        MoPubLog.log(SHOULD_REWARD, ADAPTER_NAME, MoPubReward.DEFAULT_REWARD_AMOUNT, MoPubReward.NO_REWARD_LABEL);
    }

    @Override
    public void onLoggingImpression(Ad ad) {
        cancelExpirationTimer();
        MoPubRewardedVideoManager.onRewardedVideoStarted(FacebookRewardedVideo.class, mPlacementId);
        MoPubLog.log(SHOW_SUCCESS, ADAPTER_NAME);
    }

    @Override
    public void onRewardedVideoClosed() {
        MoPubRewardedVideoManager.onRewardedVideoClosed(FacebookRewardedVideo.class, mPlacementId);
    }

    @Override
    public void onAdLoaded(Ad ad) {
        cancelExpirationTimer();
        mHandler.postDelayed(mAdExpiration, ONE_HOURS_MILLIS);

        MoPubRewardedVideoManager.onRewardedVideoLoadSuccess(FacebookRewardedVideo.class, mPlacementId);
        MoPubLog.log(LOAD_SUCCESS, ADAPTER_NAME);
    }

    @Override
    public void onAdClicked(Ad ad) {
        MoPubRewardedVideoManager.onRewardedVideoClicked(FacebookRewardedVideo.class, mPlacementId);
        MoPubLog.log(CLICKED, ADAPTER_NAME);
    }

    @Override
    public void onError(Ad ad, AdError adError) {
        cancelExpirationTimer();
        MoPubRewardedVideoManager.onRewardedVideoLoadFailure(FacebookRewardedVideo.class, mPlacementId, mapErrorCode(adError.getErrorCode()));
        MoPubLog.log(CUSTOM, ADAPTER_NAME, "Loading/Playing Facebook Rewarded Video creative encountered an error: " + mapErrorCode(adError.getErrorCode()).toString());
        MoPubLog.log(LOAD_FAILED, ADAPTER_NAME, mapErrorCode(adError.getErrorCode()), mapErrorCode(adError.getErrorCode()).toString());
    }

    @NonNull
    private static MoPubErrorCode mapErrorCode(int error) {
        switch (error) {
            case AdError.NO_FILL_ERROR_CODE:
                return MoPubErrorCode.NETWORK_NO_FILL;
            case AdError.INTERNAL_ERROR_CODE:
                return MoPubErrorCode.INTERNAL_ERROR;
            case AdError.NETWORK_ERROR_CODE:
                return MoPubErrorCode.NO_CONNECTION;
            default:
                return MoPubErrorCode.UNSPECIFIED;
        }
    }

    private void cancelExpirationTimer() {
        mHandler.removeCallbacks(mAdExpiration);
    }
}
