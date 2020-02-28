package com.mopub.mobileads;

import android.app.Activity;
import android.os.Handler;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.AudienceNetworkAds;
import com.facebook.ads.RewardedVideoAd;
import com.facebook.ads.RewardedVideoAdExtendedListener;
import com.mopub.common.DataKeys;
import com.mopub.common.LifecycleListener;
import com.mopub.common.MoPubReward;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.facebook.ads.AdError.BROKEN_MEDIA_ERROR_CODE;
import static com.facebook.ads.AdError.CACHE_ERROR_CODE;
import static com.facebook.ads.AdError.INTERNAL_ERROR_CODE;
import static com.facebook.ads.AdError.LOAD_TOO_FREQUENTLY_ERROR_CODE;
import static com.facebook.ads.AdError.MEDIATION_ERROR_CODE;
import static com.facebook.ads.AdError.NETWORK_ERROR_CODE;
import static com.facebook.ads.AdError.NO_FILL_ERROR_CODE;
import static com.facebook.ads.AdError.SERVER_ERROR_CODE;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOULD_REWARD;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;
import static com.mopub.mobileads.MoPubErrorCode.CANCELLED;
import static com.mopub.mobileads.MoPubErrorCode.EXPIRED;
import static com.mopub.mobileads.MoPubErrorCode.NETWORK_INVALID_STATE;
import static com.mopub.mobileads.MoPubErrorCode.NETWORK_NO_FILL;
import static com.mopub.mobileads.MoPubErrorCode.NO_CONNECTION;
import static com.mopub.mobileads.MoPubErrorCode.UNSPECIFIED;
import static com.mopub.mobileads.MoPubErrorCode.VIDEO_CACHE_ERROR;
import static com.mopub.mobileads.MoPubErrorCode.VIDEO_PLAYBACK_ERROR;

public class FacebookRewardedVideo extends CustomEventRewardedVideo implements RewardedVideoAdExtendedListener {

    private static final int ONE_HOURS_MILLIS = 60 * 60 * 1000;
    private static final String ADAPTER_NAME = FacebookRewardedVideo.class.getSimpleName();
    private static AtomicBoolean sIsInitialized = new AtomicBoolean(false);
    @Nullable
    private RewardedVideoAd mRewardedVideoAd;
    private String mPlacementId = "";
    @NonNull
    private Handler mHandler;
    private Runnable mAdExpiration;
    @NonNull
    private FacebookAdapterConfiguration mFacebookAdapterConfiguration;
    private boolean closeCallbackFired;

    public FacebookRewardedVideo() {
        mHandler = new Handler();
        mFacebookAdapterConfiguration = new FacebookAdapterConfiguration();

        mAdExpiration = new Runnable() {
            @Override
            public void run() {
                MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Expiring unused " +
                        "Facebook Rewarded Video ad due to Facebook's 60-minute expiration policy.");
                MoPubRewardedVideoManager.onRewardedVideoPlaybackError(FacebookRewardedVideo.class,
                        mPlacementId, EXPIRED);
                MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                        MoPubErrorCode.EXPIRED.getIntCode(), MoPubErrorCode.EXPIRED);

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
    protected boolean checkAndInitializeSdk(@NonNull Activity launcherActivity,
                                            @NonNull Map<String, Object> localExtras,
                                            @NonNull Map<String, String> serverExtras)
            throws Exception {
        Preconditions.checkNotNull(launcherActivity);
        Preconditions.checkNotNull(localExtras);
        Preconditions.checkNotNull(serverExtras);

        boolean requiresInitialization = !sIsInitialized.getAndSet(true);
        if (requiresInitialization) {
            AudienceNetworkAds.initialize(launcherActivity);
        }
        return requiresInitialization;
    }

    @Override
    protected void loadWithSdkInitialized(@NonNull Activity activity,
                                          @NonNull Map<String, Object> localExtras,
                                          @NonNull Map<String, String> serverExtras)
            throws Exception {
        Preconditions.checkNotNull(activity);
        Preconditions.checkNotNull(localExtras);
        Preconditions.checkNotNull(serverExtras);

        if (!serverExtras.isEmpty()) {
            mPlacementId = serverExtras.get("placement_id");
            mFacebookAdapterConfiguration.setCachedInitializationParameters(
                    activity.getApplicationContext(), serverExtras);

            if (!TextUtils.isEmpty(mPlacementId)) {
                if (mRewardedVideoAd != null) {
                    mRewardedVideoAd.destroy();
                    mRewardedVideoAd = null;
                }
                MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Creating a Facebook " +
                        "Rewarded Video instance, and registering callbacks.");
                mRewardedVideoAd = new RewardedVideoAd(activity, mPlacementId);
            } else {
                MoPubRewardedVideoManager.onRewardedVideoLoadFailure(FacebookRewardedVideo.class,
                        getAdNetworkId(), MoPubErrorCode.NETWORK_NO_FILL);
                MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                        MoPubErrorCode.NETWORK_NO_FILL.getIntCode(), MoPubErrorCode.NETWORK_NO_FILL);
                MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Placement ID is null " +
                        "or empty.");
                return;
            }
        }

        if (mRewardedVideoAd != null) {
            if (mRewardedVideoAd.isAdLoaded()) {
                MoPubRewardedVideoManager.onRewardedVideoLoadSuccess(FacebookRewardedVideo.class,
                        mPlacementId);
                MoPubLog.log(getAdNetworkId(), LOAD_SUCCESS, ADAPTER_NAME);
                return;
            }

            final String adm = serverExtras.get(DataKeys.ADM_KEY);

            RewardedVideoAd.RewardedVideoAdLoadConfigBuilder rewardedVideoAdLoadConfigBuilder =
                    mRewardedVideoAd.buildLoadAdConfig().withAdListener(this);

            if (!TextUtils.isEmpty(adm)) {
                mRewardedVideoAd.loadAd(rewardedVideoAdLoadConfigBuilder.withBid(adm).build());
                MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);
            } else {
                mRewardedVideoAd.loadAd(rewardedVideoAdLoadConfigBuilder.build());
                MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);
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
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Performing cleanup tasks...");
            mRewardedVideoAd.destroy();
            mRewardedVideoAd = null;
        }
    }

    @Override
    protected boolean hasVideoAvailable() {
        return mRewardedVideoAd != null && mRewardedVideoAd.isAdLoaded()
                && !mRewardedVideoAd.isAdInvalidated();
    }

    @Override
    protected void showVideo() {
        MoPubLog.log(getAdNetworkId(), SHOW_ATTEMPTED, ADAPTER_NAME);
        if (mRewardedVideoAd != null && hasVideoAvailable()) {
            mRewardedVideoAd.show();
        } else {
            MoPubRewardedVideoManager.onRewardedVideoPlaybackError(FacebookRewardedVideo.class,
                    mPlacementId, MoPubErrorCode.NETWORK_NO_FILL);
            MoPubLog.log(getAdNetworkId(), SHOW_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(), MoPubErrorCode.NETWORK_NO_FILL);
        }
    }

    @Override
    public void onRewardedVideoCompleted() {
        MoPubRewardedVideoManager.onRewardedVideoCompleted(FacebookRewardedVideo.class,
                mPlacementId, MoPubReward.success(MoPubReward.NO_REWARD_LABEL,
                        MoPubReward.DEFAULT_REWARD_AMOUNT));
        MoPubLog.log(getAdNetworkId(), SHOULD_REWARD, ADAPTER_NAME,
                MoPubReward.DEFAULT_REWARD_AMOUNT, MoPubReward.NO_REWARD_LABEL);
    }

    @Override
    public void onLoggingImpression(Ad ad) {
        cancelExpirationTimer();
        MoPubRewardedVideoManager.onRewardedVideoStarted(FacebookRewardedVideo.class, mPlacementId);
        MoPubLog.log(getAdNetworkId(), SHOW_SUCCESS, ADAPTER_NAME);
    }

    @Override
    public void onRewardedVideoClosed() {
        closeCallbackFired = true;
        MoPubRewardedVideoManager.onRewardedVideoClosed(FacebookRewardedVideo.class, mPlacementId);
    }

    @Override
    public void onAdLoaded(Ad ad) {
        cancelExpirationTimer();
        mHandler.postDelayed(mAdExpiration, ONE_HOURS_MILLIS);

        MoPubRewardedVideoManager.onRewardedVideoLoadSuccess(FacebookRewardedVideo.class, mPlacementId);
        MoPubLog.log(getAdNetworkId(), LOAD_SUCCESS, ADAPTER_NAME);
    }

    @Override
    public void onAdClicked(Ad ad) {
        MoPubRewardedVideoManager.onRewardedVideoClicked(FacebookRewardedVideo.class, mPlacementId);
        MoPubLog.log(getAdNetworkId(), CLICKED, ADAPTER_NAME);
    }

    @Override
    public void onError(Ad ad, AdError adError) {
        cancelExpirationTimer();
        MoPubRewardedVideoManager.onRewardedVideoLoadFailure(FacebookRewardedVideo.class,
                mPlacementId, mapErrorCode(adError.getErrorCode()));
        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Loading/Playing Facebook " +
                "Rewarded Video creative encountered an error: " +
                mapErrorCode(adError.getErrorCode()).toString());
        MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                mapErrorCode(adError.getErrorCode()), mapErrorCode(adError.getErrorCode()).toString());
    }

    @Override
    public void onRewardedVideoActivityDestroyed() {
        if (!closeCallbackFired) {
            MoPubRewardedVideoManager.onRewardedVideoClosed(FacebookRewardedVideo.class, mPlacementId);
        }
    }

    @NonNull
    private static MoPubErrorCode mapErrorCode(int error) {
        switch (error) {
            case NO_FILL_ERROR_CODE:
                return NETWORK_NO_FILL;
            case INTERNAL_ERROR_CODE:
                return MoPubErrorCode.INTERNAL_ERROR;
            case NETWORK_ERROR_CODE:
                return NO_CONNECTION;
            case LOAD_TOO_FREQUENTLY_ERROR_CODE:
                return CANCELLED;
            case SERVER_ERROR_CODE:
                return MoPubErrorCode.SERVER_ERROR;
            case CACHE_ERROR_CODE:
                return VIDEO_CACHE_ERROR;
            case MEDIATION_ERROR_CODE:
                return NETWORK_INVALID_STATE;
            case BROKEN_MEDIA_ERROR_CODE:
                return VIDEO_PLAYBACK_ERROR;
            default:
                return UNSPECIFIED;
        }
    }

    private void cancelExpirationTimer() {
        mHandler.removeCallbacks(mAdExpiration);
    }
}
