package com.mopub.mobileads;

import android.app.Activity;
import android.app.Application;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.BaseLifecycleListener;
import com.mopub.common.LifecycleListener;
import com.mopub.common.MoPub;
import com.mopub.common.MoPubReward;
import com.mopub.common.logging.MoPubLog;
import com.verizon.ads.ActivityStateManager;
import com.verizon.ads.CreativeInfo;
import com.verizon.ads.ErrorInfo;
import com.verizon.ads.RequestMetadata;
import com.verizon.ads.VASAds;
import com.verizon.ads.edition.StandardEdition;
import com.verizon.ads.interstitialplacement.InterstitialAd;
import com.verizon.ads.interstitialplacement.InterstitialAdFactory;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.DID_DISAPPEAR;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.WILL_LEAVE_APPLICATION;
import static com.mopub.mobileads.MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR;
import static com.mopub.mobileads.VerizonAdapterConfiguration.convertErrorInfoToMoPub;

public class VerizonRewardedVideo extends CustomEventRewardedVideo {

    private static final String ADAPTER_NAME = VerizonRewardedVideo.class.getSimpleName();
    private static final LifecycleListener lifecycleListener = new VerizonLifecycleListener();

    private static final String PLACEMENT_ID_KEY = "placementId";
    private static final String SITE_ID_KEY = "siteId";
    private static final String VIDEO_COMPLETE_EVENT_ID = "onVideoComplete";

    private InterstitialAd verizonInterstitialAd;
    private Activity activity;
    private String placementId = null;
    private boolean rewarded = false;

    @NonNull
    private VerizonAdapterConfiguration verizonAdapterConfiguration;

    public VerizonRewardedVideo() {
        verizonAdapterConfiguration = new VerizonAdapterConfiguration();
    }

    @Nullable
    @Override
    protected LifecycleListener getLifecycleListener() {
        return lifecycleListener;
    }

    @NonNull
    @Override
    protected String getAdNetworkId() {
        return (placementId == null) ? "" : placementId;
    }

    @Override
    protected void onInvalidate() {
        if (verizonInterstitialAd != null) {
            verizonInterstitialAd.destroy();
            verizonInterstitialAd = null;
        }
    }

    private static final class VerizonLifecycleListener extends BaseLifecycleListener {
        @Override
        public void onCreate(@NonNull final Activity activity) {
            super.onCreate(activity);
        }

        @Override
        public void onResume(@NonNull final Activity activity) {
            super.onResume(activity);
        }
    }

    @Override
    protected boolean checkAndInitializeSdk(@NonNull final Activity launcherActivity,
                                            @NonNull final Map<String, Object> localExtras,
                                            @NonNull final Map<String, String> serverExtras) {
        if (serverExtras.isEmpty()) {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Ad request to Verizon " +
                    "failed because serverExtras is null or empty");

            MoPubRewardedVideoManager.onRewardedVideoLoadFailure(VerizonRewardedVideo.class, getAdNetworkId(),
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);

            return false;
        }

        String siteId = serverExtras.get(SITE_ID_KEY);

        if (!VASAds.isInitialized()) {
            Application application = launcherActivity.getApplication();

            if (!StandardEdition.initialize(application, siteId)) {
                MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                        ADAPTER_CONFIGURATION_ERROR.getIntCode(), ADAPTER_CONFIGURATION_ERROR);
                MoPubRewardedVideoManager.onRewardedVideoLoadFailure(VerizonRewardedVideo.class, getAdNetworkId(),
                        MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);

                return false;
            }
        }

        placementId = serverExtras.get(PLACEMENT_ID_KEY);

        if (TextUtils.isEmpty(placementId)) {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Invalid extras--Make sure " +
                    "you have a valid placement ID specified on the MoPub dashboard.");
            MoPubRewardedVideoManager.onRewardedVideoLoadFailure(VerizonRewardedVideo.class, getAdNetworkId(),
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);

            return false;
        }

        // Cache serverExtras so siteId can be used to initalizate VAS early at next launch
        verizonAdapterConfiguration.setCachedInitializationParameters(launcherActivity, serverExtras);

        // The current activity must be set as resumed so VAS can track ad visibility
        ActivityStateManager activityStateManager = VASAds.getActivityStateManager();
        if (activityStateManager != null) {
            activityStateManager.setState(launcherActivity, ActivityStateManager.ActivityState.RESUMED);
        }

        return true;
    }

    @Override
    protected void loadWithSdkInitialized(@NonNull final Activity activity,
                                          @NonNull final Map<String, Object> localExtras,
                                          @NonNull final Map<String, String> serverExtras) {

        this.activity = activity;

        VASAds.setLocationEnabled(MoPub.getLocationAwareness() != MoPub.LocationAwareness.DISABLED);

        final InterstitialAdFactory interstitialAdFactory = new InterstitialAdFactory(activity,
                placementId, new VerizonInterstitialFactoryListener());

        final RequestMetadata requestMetadata = new RequestMetadata.Builder().setMediator
                (VerizonAdapterConfiguration.MEDIATOR_ID)
                .build();

        interstitialAdFactory.setRequestMetaData(requestMetadata);
        interstitialAdFactory.load(new VerizonInterstitialListener());

        MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);
    }

    @Override
    protected boolean hasVideoAvailable() {
        // forwarding deprecated method to its replacement
        return isReady();
    }

    @Override
    protected void showVideo() {
        // forwarding deprecated method to its replacement
        show();
    }

    @Override
    protected boolean isReady() {
        return verizonInterstitialAd != null;
    }

    @Override
    protected void show() {

        VerizonAdapterConfiguration.postOnUiThread(new Runnable() {

            @Override
            public void run() {
                if (verizonInterstitialAd != null) {
                    verizonInterstitialAd.show(activity);
                } else {
                    MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Show() called " +
                            "before Verizon rewarded video ad was loaded.");
                    MoPubRewardedVideoManager.onRewardedVideoLoadFailure(VerizonRewardedVideo.class,
                            getAdNetworkId(), MoPubErrorCode.NETWORK_INVALID_STATE);
                }
            }
        });
    }

    private class VerizonInterstitialFactoryListener implements InterstitialAdFactory.InterstitialAdFactoryListener {

        @Override
        public void onLoaded(final InterstitialAdFactory interstitialAdFactory, final InterstitialAd interstitialAd) {

            verizonInterstitialAd = interstitialAd;

            MoPubLog.log(getAdNetworkId(), LOAD_SUCCESS, ADAPTER_NAME);

            VerizonAdapterConfiguration.postOnUiThread(new Runnable() {
                @Override
                public void run() {

                    final CreativeInfo creativeInfo = verizonInterstitialAd == null ? null :
                            verizonInterstitialAd.getCreativeInfo();
                    MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Verizon creative " +
                            "info: " + creativeInfo);
                }
            });

            MoPubRewardedVideoManager.onRewardedVideoLoadSuccess(VerizonRewardedVideo.class, getAdNetworkId());
        }

        @Override
        public void onCacheLoaded(final InterstitialAdFactory interstitialAdFactory,
                                  final int numRequested, final int numReceived) {
        }

        @Override
        public void onCacheUpdated(final InterstitialAdFactory interstitialAdFactory, final int cacheSize) {
        }

        @Override
        public void onError(final InterstitialAdFactory interstitialAdFactory, final ErrorInfo errorInfo) {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Failed to load Verizon " +
                    "rewarded video due to error: " + errorInfo.toString());

            MoPubRewardedVideoManager.onRewardedVideoLoadFailure(VerizonRewardedVideo.class, getAdNetworkId(),
                    convertErrorInfoToMoPub(errorInfo));
        }
    }

    private class VerizonInterstitialListener implements InterstitialAd.InterstitialAdListener {

        @Override
        public void onError(final InterstitialAd interstitialAd, final ErrorInfo errorInfo) {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Failed to show Verizon " +
                    "rewarded video due to error: " + errorInfo.toString());

            MoPubRewardedVideoManager.onRewardedVideoPlaybackError(VerizonRewardedVideo.class, getAdNetworkId(),
                    MoPubErrorCode.VIDEO_PLAYBACK_ERROR);
        }

        @Override
        public void onShown(final InterstitialAd interstitialAd) {
            MoPubLog.log(getAdNetworkId(), SHOW_SUCCESS, ADAPTER_NAME);
            MoPubRewardedVideoManager.onRewardedVideoStarted(VerizonRewardedVideo.class, getAdNetworkId());
        }

        @Override
        public void onClosed(final InterstitialAd interstitialAd) {
            MoPubLog.log(getAdNetworkId(), DID_DISAPPEAR, ADAPTER_NAME);
            MoPubRewardedVideoManager.onRewardedVideoClosed(VerizonRewardedVideo.class, getAdNetworkId());
        }

        @Override
        public void onClicked(final InterstitialAd interstitialAd) {
            MoPubLog.log(getAdNetworkId(), CLICKED, ADAPTER_NAME);
            MoPubRewardedVideoManager.onRewardedVideoClicked(VerizonRewardedVideo.class, getAdNetworkId());
        }

        @Override
        public void onAdLeftApplication(final InterstitialAd interstitialAd) {
            // Only logging this event. No need to call interstitialListener.onLeaveApplication()
            // because it's an alias for interstitialListener.onInterstitialClicked()
            MoPubLog.log(getAdNetworkId(), WILL_LEAVE_APPLICATION, ADAPTER_NAME);
        }

        @Override
        public void onEvent(final InterstitialAd interstitialAd, final String source,
                            final String eventId, final Map<String, Object> arguments) {

            if (!rewarded && VIDEO_COMPLETE_EVENT_ID.equals(eventId)) {
                MoPubRewardedVideoManager.onRewardedVideoCompleted(VerizonRewardedVideo.class, getAdNetworkId(),
                        MoPubReward.success(MoPubReward.NO_REWARD_LABEL, MoPubReward.DEFAULT_REWARD_AMOUNT));

                rewarded = true;
            }
        }
    }
}
