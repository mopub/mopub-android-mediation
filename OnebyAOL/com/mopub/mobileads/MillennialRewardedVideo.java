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

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM_WITH_THROWABLE;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.EXPIRED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOULD_REWARD;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;

@SuppressWarnings("unused")
final class MillennialRewardedVideo extends CustomEventRewardedVideo {

    private static final String TAG = MillennialRewardedVideo.class.getSimpleName();
    private static final String DCN_KEY = "dcn";
    private static final String APID_KEY = "adUnitID";
    private static final String ADAPTER_NAME = MillennialRewardedVideo.class.getSimpleName();

    private InterstitialAd millennialInterstitial;
    private MillennialRewardedVideoListener millennialRewardedVideoListener = new MillennialRewardedVideoListener();
    private Activity activity;
    @NonNull
    private String apid = "";
    @NonNull
    private MillennialAdapterConfiguration mMillennialAdapterConfiguration;

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

    public MillennialRewardedVideo() {
        mMillennialAdapterConfiguration = new MillennialAdapterConfiguration();
    }

    @Override
    protected boolean checkAndInitializeSdk(@NonNull final Activity launcherActivity,
                                            @NonNull final Map<String, Object> localExtras, @NonNull final Map<String, String> serverExtras) throws Exception {
        try {
            MMSDK.initialize(launcherActivity, ActivityListenerManager.LifecycleState.RESUMED);
            mMillennialAdapterConfiguration.setCachedInitializationParameters(launcherActivity, serverExtras);
        } catch (IllegalStateException e) {
            MoPubLog.log(CUSTOM_WITH_THROWABLE, "An exception occurred initializing the MM SDK", e);
            MoPubRewardedVideoManager.onRewardedVideoLoadFailure(MillennialRewardedVideo.class, "",
                    MoPubErrorCode.NETWORK_NO_FILL);

            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                    MoPubErrorCode.NETWORK_NO_FILL);

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
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Invalid extras-- Be sure you have a placement ID specified.");
            MoPubRewardedVideoManager.onRewardedVideoLoadFailure(MillennialRewardedVideo.class, "",
                    MoPubErrorCode.NETWORK_NO_FILL);

            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                    MoPubErrorCode.NETWORK_NO_FILL);

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

            MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);
        } catch (MMException e) {
            MoPubLog.log(CUSTOM_WITH_THROWABLE, "An exception occurred loading an " +
                    "ad", e);
            MoPubRewardedVideoManager
                    .onRewardedVideoLoadFailure(MillennialRewardedVideo.class, apid, MoPubErrorCode.NETWORK_NO_FILL);

            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                    MoPubErrorCode.NETWORK_NO_FILL);
        }
    }

    @Override
    protected boolean hasVideoAvailable() {
        return ((millennialInterstitial != null) && millennialInterstitial.isReady());
    }

    @Override
    protected void showVideo() {
        MoPubLog.log(SHOW_ATTEMPTED, ADAPTER_NAME);

        if ((millennialInterstitial != null) && millennialInterstitial.isReady()) {
            try {
                millennialInterstitial.show(activity);
            } catch (MMException e) {
                MoPubLog.log(CUSTOM_WITH_THROWABLE, "An exception occurred showing the " +
                        "MM SDK interstitial.", e);
                MoPubRewardedVideoManager
                        .onRewardedVideoPlaybackError(MillennialRewardedVideo.class, millennialInterstitial.placementId,
                                MoPubErrorCode.NETWORK_NO_FILL);

                MoPubLog.log(SHOW_FAILED, ADAPTER_NAME,
                        MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                        MoPubErrorCode.NETWORK_NO_FILL);
            }
        } else {
            MoPubLog.log(SHOW_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                    MoPubErrorCode.NETWORK_NO_FILL);
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "showVideo called before MillennialInterstitial ad was loaded.");

            MoPubRewardedVideoManager.onRewardedVideoLoadFailure(MillennialRewardedVideo.class, "",
                    MoPubErrorCode.NETWORK_NO_FILL);
        }
    }

    class MillennialRewardedVideoListener
            implements InterstitialListener, XIncentivizedEventListener, CustomEventRewardedVideoListener {

        @Override
        public void onAdLeftApplication(InterstitialAd interstitialAd) {
        }

        @Override
        public void onClicked(final InterstitialAd interstitialAd) {
            MillennialUtils.postOnUiThread(new Runnable() {
                @Override
                public void run() {
                    MoPubRewardedVideoManager
                            .onRewardedVideoClicked(MillennialRewardedVideo.class, interstitialAd.placementId);

                    MoPubLog.log(CLICKED, ADAPTER_NAME);
                }
            });
        }

        @Override
        public void onClosed(final InterstitialAd interstitialAd) {
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
            MoPubLog.log(EXPIRED, ADAPTER_NAME);
            MillennialUtils.postOnUiThread(new Runnable() {
                @Override
                public void run() {
                    MoPubRewardedVideoManager
                            .onRewardedVideoLoadFailure(MillennialRewardedVideo.class, interstitialAd.placementId,
                                    MoPubErrorCode.NETWORK_NO_FILL);

                    MoPubLog.log(SHOW_FAILED,
                            ADAPTER_NAME,
                            MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                            MoPubErrorCode.NETWORK_NO_FILL);
                }
            });
        }

        @Override
        public void onLoadFailed(final InterstitialAd interstitialAd, InterstitialErrorStatus
                interstitialErrorStatus) {

            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Millennial Rewarded Video Ad - load failed (" +
                    interstitialErrorStatus.getErrorCode() + "): " +
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

                            MoPubLog.log(LOAD_SUCCESS, ADAPTER_NAME);
                        }
                    });
                    MoPubLog.log(CUSTOM, ADAPTER_NAME, "Millennial Rewarded Video Ad - Attempted to load " +
                            "ads when ads are already loaded.");
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

                    MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                            moPubErrorCode.getIntCode(),
                            moPubErrorCode);
                }
            });
        }

        @Override
        public void onLoaded(final InterstitialAd interstitialAd) {

            CreativeInfo creativeInfo = getCreativeInfo();

            if ((creativeInfo != null) && MMLog.isDebugEnabled()) {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "Rewarded Video Creative Info: " + creativeInfo);
            }

            MillennialUtils.postOnUiThread(new Runnable() {
                @Override
                public void run() {
                    MoPubRewardedVideoManager
                            .onRewardedVideoLoadSuccess(MillennialRewardedVideo.class, interstitialAd.placementId);

                    MoPubLog.log(LOAD_SUCCESS, ADAPTER_NAME);
                }
            });
        }

        @Override
        public void onShowFailed(final InterstitialAd interstitialAd, InterstitialErrorStatus
                interstitialErrorStatus) {

            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Millennial Rewarded Video Ad - Show failed (" +
                    interstitialErrorStatus.getErrorCode() + "): " +
                    interstitialErrorStatus.getDescription());

            MillennialUtils.postOnUiThread(new Runnable() {
                @Override
                public void run() {
                    MoPubRewardedVideoManager
                            .onRewardedVideoPlaybackError(MillennialRewardedVideo.class, interstitialAd.placementId,
                                    MoPubErrorCode.NETWORK_NO_FILL);

                    MoPubLog.log(SHOW_FAILED, ADAPTER_NAME,
                            MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                            MoPubErrorCode.NETWORK_NO_FILL);
                }
            });
        }

        @Override
        public void onShown(final InterstitialAd interstitialAd) {
            MillennialUtils.postOnUiThread(new Runnable() {
                @Override
                public void run() {

                    MoPubRewardedVideoManager
                            .onRewardedVideoStarted(MillennialRewardedVideo.class, interstitialAd.placementId);

                    MoPubLog.log(SHOW_SUCCESS, ADAPTER_NAME);
                }
            });
        }

        @Override
        public boolean onVideoComplete() {
            MillennialUtils.postOnUiThread(new Runnable() {
                @Override
                public void run() {
                    MoPubLog.log(SHOULD_REWARD, ADAPTER_NAME, MoPubReward.DEFAULT_REWARD_AMOUNT, MoPubReward.NO_REWARD_LABEL);

                    MoPubRewardedVideoManager
                            .onRewardedVideoCompleted(MillennialRewardedVideo.class, millennialInterstitial.placementId,
                                    MoPubReward.success(MoPubReward.NO_REWARD_LABEL, MoPubReward.DEFAULT_REWARD_AMOUNT));
                }
            });
            return false;
        }

        @Override
        public boolean onCustomEvent(XIncentiveEvent xIncentiveEvent) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Millennial Rewarded Video Ad - Custom event received: " +
                    xIncentiveEvent.eventId + ", " + xIncentiveEvent.args);

            return false;
        }
    }
}