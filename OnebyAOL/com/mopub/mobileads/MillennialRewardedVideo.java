package com.mopub.mobileads;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.millennialmedia.AppInfo;
import com.millennialmedia.CreativeInfo;
import com.millennialmedia.InterstitialAd;
import com.millennialmedia.InterstitialAd.InterstitialErrorStatus;
import com.millennialmedia.InterstitialAd.InterstitialListener;
import com.millennialmedia.MMException;
import com.millennialmedia.MMLog;
import com.millennialmedia.MMSDK;
import com.millennialmedia.XIncentivizedEventListener;
import com.millennialmedia.internal.ActivityListenerManager;
import com.mopub.common.BaseLifecycleListener;
import com.mopub.common.LifecycleListener;
import com.mopub.common.MoPub;
import com.mopub.common.MoPubReward;
import com.mopub.common.logging.MoPubLog;

import java.util.Map;

@SuppressWarnings("unused")
final class MillennialRewardedVideo extends CustomEventRewardedVideo {

    private static final String TAG = MillennialRewardedVideo.class.getSimpleName();
    private static final String DCN_KEY = "dcn";
    private static final String APID_KEY = "adUnitID";

    private InterstitialAd millennialInterstitial;
    private MillennialRewardedVideoListener millennialRewardedVideoListener = new MillennialRewardedVideoListener();
    private Activity activity;
    @NonNull
    private String apid = "";

    static {
        MoPubLog.d("Millennial Media Adapter Version: " + MillennialUtils.MEDIATOR_ID);
    }

    private CreativeInfo getCreativeInfo() {

        if (millennialInterstitial == null) {
            return null;
        }

        return millennialInterstitial.getCreativeInfo();
    }

    @Nullable
    @Override
    protected CustomEventRewardedVideoListener getVideoListenerForSdk() {
        return millennialRewardedVideoListener;
    }

    @Nullable
    @Override
    protected LifecycleListener getLifecycleListener() {
        return new BaseLifecycleListener();
    }

    @NonNull
    @Override
    protected String getAdNetworkId() {
        return apid;
    }

    @Override
    protected void onInvalidate() {
        if (millennialInterstitial != null) {
            millennialInterstitial.destroy();
            millennialInterstitial = null;
            apid = "";
        }
    }

    @Override
    protected boolean checkAndInitializeSdk(@NonNull Activity launcherActivity,
                                            @NonNull Map<String, Object> localExtras, @NonNull Map<String, String> serverExtras) throws Exception {
        try {
            MMSDK.initialize(launcherActivity, ActivityListenerManager.LifecycleState.RESUMED);
        } catch (IllegalStateException e) {
            MoPubLog.d("An exception occurred initializing the MM SDK", e);

            return false;
        }
        return true;
    }

    @Override
    protected void loadWithSdkInitialized(@NonNull Activity activity, @NonNull Map<String, Object> localExtras,
                                          @NonNull Map<String, String> serverExtras) throws Exception {
        this.activity = activity;
        apid = serverExtras.get(APID_KEY);
        String dcn = serverExtras.get(DCN_KEY);

        if (MillennialUtils.isEmpty(apid)) {
            MoPubLog.d("Invalid extras-- Be sure you have a placement ID specified.");
            MoPubRewardedVideoManager.onRewardedVideoLoadFailure(MillennialRewardedVideo.class, "",
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);

            return;
        }

        // Add DCN support
        AppInfo ai = new AppInfo().setMediator(MillennialUtils.MEDIATOR_ID).setSiteId(dcn);
        try {
            MMSDK.setAppInfo(ai);
            /* If MoPub gets location, so do we. */
            MMSDK.setLocationEnabled(MoPub.getLocationAwareness() != MoPub.LocationAwareness.DISABLED);

            millennialInterstitial = InterstitialAd.createInstance(apid);
            millennialInterstitial.setListener(millennialRewardedVideoListener);
            millennialInterstitial.xSetIncentivizedListener(millennialRewardedVideoListener);
            millennialInterstitial.load(activity, null);

        } catch (MMException e) {
            MoPubLog.d("An exception occurred loading an InterstitialAd", e);
            MoPubRewardedVideoManager
                    .onRewardedVideoLoadFailure(MillennialRewardedVideo.class, apid, MoPubErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    protected boolean hasVideoAvailable() {
        return ((millennialInterstitial != null) && millennialInterstitial.isReady());
    }

    @Override
    protected void showVideo() {
        if ((millennialInterstitial != null) && millennialInterstitial.isReady()) {
            try {
                millennialInterstitial.show(activity);
            } catch (MMException e) {
                MoPubLog.d("An exception occurred showing the MM SDK interstitial.", e);
                MoPubRewardedVideoManager
                        .onRewardedVideoPlaybackError(MillennialRewardedVideo.class, millennialInterstitial.placementId,
                                MoPubErrorCode.INTERNAL_ERROR);
            }
        } else {
            MoPubLog.d("showVideo called before MillennialInterstitial ad was loaded.");
        }
    }

    class MillennialRewardedVideoListener
            implements InterstitialListener, XIncentivizedEventListener, CustomEventRewardedVideoListener {

        @Override
        public void onAdLeftApplication(InterstitialAd interstitialAd) {
            // onLeaveApplication is an alias to on clicked. We are not required to call this.

            // @formatter:off
            // https://github.com/mopub/mopub-android-sdk/blob/940eee70fe1980b4869d61cb5d668ccbab75c0ee/mopub-sdk/mopub-sdk-interstitial/src/main/java/com/mopub/mobileads/CustomEventInterstitial.java
            // @formatter:on
            MoPubLog.d("Millennial Rewarded Video Ad - Leaving application");
        }

        @Override
        public void onClicked(final InterstitialAd interstitialAd) {
            MoPubLog.d("Millennial Rewarded Video Ad - Ad was clicked");
            MillennialUtils.postOnUiThread(new Runnable() {
                @Override
                public void run() {
                    MoPubRewardedVideoManager
                            .onRewardedVideoClicked(MillennialRewardedVideo.class, interstitialAd.placementId);
                }
            });
        }

        @Override
        public void onClosed(final InterstitialAd interstitialAd) {
            MoPubLog.d("Millennial Rewarded Video Ad - Ad was closed");
            MillennialUtils.postOnUiThread(new Runnable() {
                @Override
                public void run() {
                    MoPubRewardedVideoManager
                            .onRewardedVideoClosed(MillennialRewardedVideo.class, interstitialAd.placementId);
                }
            });
        }

        @Override
        public void onExpired(final InterstitialAd interstitialAd) {
            MoPubLog.d("Millennial Rewarded Video Ad - Ad expired");
            MillennialUtils.postOnUiThread(new Runnable() {
                @Override
                public void run() {
                    MoPubRewardedVideoManager
                            .onRewardedVideoLoadFailure(MillennialRewardedVideo.class, interstitialAd.placementId,
                                    MoPubErrorCode.VIDEO_NOT_AVAILABLE);
                }
            });
        }

        @Override
        public void onLoadFailed(final InterstitialAd interstitialAd, InterstitialErrorStatus
                interstitialErrorStatus) {
            MoPubLog.d("Millennial Rewarded Video Ad - load failed (" + interstitialErrorStatus.getErrorCode() + "): " +
                    interstitialErrorStatus.getDescription());

            final MoPubErrorCode moPubErrorCode;

            switch (interstitialErrorStatus.getErrorCode()) {
                case InterstitialErrorStatus.ALREADY_LOADED:
                    // This will generate discrepancies, as requests will NOT be sent to Millennial.
                    MillennialUtils.postOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            MoPubRewardedVideoManager
                                    .onRewardedVideoLoadSuccess(MillennialRewardedVideo.class, interstitialAd.placementId);
                        }
                    });
                    MoPubLog.d("Millennial Rewarded Video Ad - Attempted to load ads when ads are already loaded.");
                    return;
                case InterstitialErrorStatus.EXPIRED:
                case InterstitialErrorStatus.DISPLAY_FAILED:
                case InterstitialErrorStatus.INIT_FAILED:
                case InterstitialErrorStatus.ADAPTER_NOT_FOUND:
                    moPubErrorCode = MoPubErrorCode.INTERNAL_ERROR;
                    break;
                case InterstitialErrorStatus.NO_NETWORK:
                    moPubErrorCode = MoPubErrorCode.NO_CONNECTION;
                    break;
                case InterstitialErrorStatus.UNKNOWN:
                    moPubErrorCode = MoPubErrorCode.UNSPECIFIED;
                    break;
                case InterstitialErrorStatus.NOT_LOADED:
                case InterstitialErrorStatus.LOAD_FAILED:
                default:
                    moPubErrorCode = MoPubErrorCode.NETWORK_NO_FILL;
            }

            MillennialUtils.postOnUiThread(new Runnable() {
                @Override
                public void run() {
                    MoPubRewardedVideoManager
                            .onRewardedVideoLoadFailure(MillennialRewardedVideo.class, interstitialAd.placementId,
                                    moPubErrorCode);
                }
            });
        }

        @Override
        public void onLoaded(final InterstitialAd interstitialAd) {

            MoPubLog.d("Millennial Rewarded Video Ad - Ad loaded splendidly");

            CreativeInfo creativeInfo = getCreativeInfo();

            if ((creativeInfo != null) && MMLog.isDebugEnabled()) {
                MoPubLog.d("Rewarded Video Creative Info: " + creativeInfo);
            }

            MillennialUtils.postOnUiThread(new Runnable() {
                @Override
                public void run() {
                    MoPubRewardedVideoManager
                            .onRewardedVideoLoadSuccess(MillennialRewardedVideo.class, interstitialAd.placementId);
                }
            });
        }

        @Override
        public void onShowFailed(final InterstitialAd interstitialAd, InterstitialErrorStatus
                interstitialErrorStatus) {

            MoPubLog.d("Millennial Rewarded Video Ad - Show failed (" + interstitialErrorStatus.getErrorCode() + "): " +
                    interstitialErrorStatus.getDescription());

            MillennialUtils.postOnUiThread(new Runnable() {
                @Override
                public void run() {
                    MoPubRewardedVideoManager
                            .onRewardedVideoPlaybackError(MillennialRewardedVideo.class, interstitialAd.placementId,
                                    MoPubErrorCode.VIDEO_PLAYBACK_ERROR);
                }
            });
        }

        @Override
        public void onShown(final InterstitialAd interstitialAd) {
            MoPubLog.d("Millennial Rewarded Video Ad - Ad shown");
            MillennialUtils.postOnUiThread(new Runnable() {
                @Override
                public void run() {
                    MoPubRewardedVideoManager
                            .onRewardedVideoStarted(MillennialRewardedVideo.class, interstitialAd.placementId);
                }
            });
        }

        @Override
        public boolean onVideoComplete() {
            MoPubLog.d("Millennial Rewarded Video Ad - Video completed");
            MillennialUtils.postOnUiThread(new Runnable() {
                @Override
                public void run() {
                    MoPubRewardedVideoManager
                            .onRewardedVideoCompleted(MillennialRewardedVideo.class, millennialInterstitial.placementId,
                                    MoPubReward.success(MoPubReward.NO_REWARD_LABEL, MoPubReward.DEFAULT_REWARD_AMOUNT));
                }
            });
            return false;
        }

        @Override
        public boolean onCustomEvent(XIncentiveEvent xIncentiveEvent) {
            MoPubLog.d("Millennial Rewarded Video Ad - Custom event received: " + xIncentiveEvent.eventId + ", " +
                    xIncentiveEvent.args);

            return false;
        }
    }
}