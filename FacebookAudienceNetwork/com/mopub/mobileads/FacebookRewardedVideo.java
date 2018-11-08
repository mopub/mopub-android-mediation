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

import static com.mopub.mobileads.MoPubErrorCode.EXPIRED;

public class FacebookRewardedVideo extends CustomEventRewardedVideo implements RewardedVideoAdListener {

    private static final int ONE_HOURS_MILLIS = 60 * 60 * 1000;
    @Nullable
    private RewardedVideoAd mRewardedVideoAd;
    @NonNull
    private String mPlacementId = "";
    @NonNull
    private Handler mHandler;
    private Runnable mAdExpiration;
    private static AtomicBoolean sIsInitialized = new AtomicBoolean(false);

    public FacebookRewardedVideo() {
        mHandler = new Handler();
        mAdExpiration = new Runnable() {
            @Override
            public void run() {
                MoPubLog.d("Expiring unused Facebook Rewarded Video ad due to Facebook's 60-minute expiration policy.");
                MoPubRewardedVideoManager.onRewardedVideoLoadFailure(FacebookRewardedVideo.class, mPlacementId, EXPIRED);

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
        if(requiresInitialization) {
            AudienceNetworkAds.initialize(launcherActivity);
        }
        return requiresInitialization;
    }

    @Override
    protected void loadWithSdkInitialized(@NonNull Activity activity, @NonNull Map<String, Object> localExtras, @NonNull Map<String, String> serverExtras) throws Exception {
        if (!serverExtras.isEmpty()) {
            mPlacementId = serverExtras.get("placement_id");

            if (!TextUtils.isEmpty(mPlacementId)) {
                if (mRewardedVideoAd != null) {
                    mRewardedVideoAd.destroy();
                    mRewardedVideoAd = null;
                }
                MoPubLog.d("Creating a Facebook Rewarded Video instance, and registering callbacks.");
                mRewardedVideoAd = new RewardedVideoAd(activity, mPlacementId);
                mRewardedVideoAd.setAdListener(this);
            } else {
                MoPubRewardedVideoManager.onRewardedVideoLoadFailure(FacebookRewardedVideo.class, getAdNetworkId(), MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
                MoPubLog.d(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR.toString());
                MoPubLog.d("Placement ID is null or empty.");
                return;
            }
        }

        if (mRewardedVideoAd.isAdLoaded()) {
            MoPubRewardedVideoManager.onRewardedVideoLoadSuccess(FacebookRewardedVideo.class, mPlacementId);
            return;
        }

        if (mRewardedVideoAd != null) {
            MoPubLog.d("Sending Facebook an ad request.");
            AdSettings.setMediationService("MOPUB_" + MoPub.SDK_VERSION);

            final String adm = serverExtras.get(DataKeys.ADM_KEY);
            if (!TextUtils.isEmpty(adm)) {
                mRewardedVideoAd.loadAdFromBid(adm);
            } else {
                mRewardedVideoAd.loadAd();
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
            MoPubLog.d("Performing cleanup tasks...");
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
        if (hasVideoAvailable()) {
            MoPubLog.d("Facebook Rewarded Video creative is available. Showing...");
            mRewardedVideoAd.show();
        } else {
            MoPubRewardedVideoManager.onRewardedVideoPlaybackError(FacebookRewardedVideo.class, mPlacementId, MoPubErrorCode.VIDEO_PLAYBACK_ERROR);
            MoPubLog.d("Facebook Rewarded Video creative is not available. Try re-requesting.");
        }
    }

    @Override
    public void onRewardedVideoCompleted() {
        MoPubLog.d("Facebook Rewarded Video creative is completed. Awarding the user.");
        MoPubRewardedVideoManager.onRewardedVideoCompleted(FacebookRewardedVideo.class, mPlacementId, MoPubReward.success(MoPubReward.NO_REWARD_LABEL, MoPubReward.DEFAULT_REWARD_AMOUNT));
    }

    @Override
    public void onLoggingImpression(Ad ad) {
        cancelExpirationTimer();
        MoPubRewardedVideoManager.onRewardedVideoStarted(FacebookRewardedVideo.class, mPlacementId);
        MoPubLog.d("Facebook Rewarded Video creative started playing.");
    }

    @Override
    public void onRewardedVideoClosed() {
        MoPubRewardedVideoManager.onRewardedVideoClosed(FacebookRewardedVideo.class, mPlacementId);
        MoPubLog.d("Facebook Rewarded Video creative closed.");
    }

    @Override
    public void onAdLoaded(Ad ad) {
        cancelExpirationTimer();
        mHandler.postDelayed(mAdExpiration, ONE_HOURS_MILLIS);

        MoPubRewardedVideoManager.onRewardedVideoLoadSuccess(FacebookRewardedVideo.class, mPlacementId);
        MoPubLog.d("Facebook Rewarded Video creative cached.");
    }

    @Override
    public void onAdClicked(Ad ad) {
        MoPubRewardedVideoManager.onRewardedVideoClicked(FacebookRewardedVideo.class, mPlacementId);
        MoPubLog.d("Facebook Rewarded Video creative clicked.");
    }

    @Override
    public void onError(Ad ad, AdError adError) {
        cancelExpirationTimer();
        MoPubRewardedVideoManager.onRewardedVideoLoadFailure(FacebookRewardedVideo.class, mPlacementId, mapErrorCode(adError.getErrorCode()));
        MoPubLog.d("Loading/Playing Facebook Rewarded Video creative encountered an error: " + mapErrorCode(adError.getErrorCode()).toString());
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
