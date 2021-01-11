package com.mopub.mobileads;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.BaseLifecycleListener;
import com.mopub.common.DataKeys;
import com.mopub.common.LifecycleListener;
import com.mopub.common.MoPubReward;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.verizon.ads.ActivityStateManager;
import com.verizon.ads.Bid;
import com.verizon.ads.CreativeInfo;
import com.verizon.ads.ErrorInfo;
import com.verizon.ads.RequestMetadata;
import com.verizon.ads.VASAds;
import com.verizon.ads.edition.StandardEdition;
import com.verizon.ads.interstitialplacement.InterstitialAd;
import com.verizon.ads.interstitialplacement.InterstitialAdFactory;

import java.util.HashMap;
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

public class VerizonRewardedVideo extends BaseAd {

    private static final String ADAPTER_NAME = VerizonRewardedVideo.class.getSimpleName();
    private static final LifecycleListener lifecycleListener = new VerizonLifecycleListener();

    private static final String AD_IMPRESSION_EVENT_ID = "adImpression";
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
            Preconditions.checkNotNull(activity);
            super.onCreate(activity);
        }

        @Override
        public void onResume(@NonNull final Activity activity) {
            Preconditions.checkNotNull(activity);
            super.onResume(activity);
        }
    }

    @Override
    protected boolean checkAndInitializeSdk(@NonNull final Activity launcherActivity,
                                            @NonNull final AdData adData) {
        Preconditions.checkNotNull(launcherActivity);
        Preconditions.checkNotNull(adData);

        setAutomaticImpressionAndClickTracking(false);

        final Map<String, String> extras = adData.getExtras();
        if (extras.isEmpty()) {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Ad request to Verizon " +
                    "failed because server data is null or empty");

            if (mLoadListener != null) {
                mLoadListener.onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            }
            throw new IllegalStateException("Verizon failed to initialize due to empty extras.");
        }

        final String siteId = extras.get(SITE_ID_KEY);

        if (!VASAds.isInitialized()) {
            final Application application = launcherActivity.getApplication();

            if (!StandardEdition.initialize(application, siteId)) {
                MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                        ADAPTER_CONFIGURATION_ERROR.getIntCode(), ADAPTER_CONFIGURATION_ERROR);
                if (mLoadListener != null) {
                    mLoadListener.onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
                }
                throw new IllegalStateException("Verizon failed to initialize.");
            }
        }

        placementId = extras.get(PLACEMENT_ID_KEY);

        if (TextUtils.isEmpty(placementId)) {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Invalid extras--Make sure " +
                    "you have a valid placement ID specified on the MoPub dashboard.");
            if (mLoadListener != null) {
                mLoadListener.onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            }
            throw new IllegalStateException("Verizon failed to initialize due to invalid placement ID.");
        }

        // Cache server data so siteId can be used to initalizate VAS early at next launch
        verizonAdapterConfiguration.setCachedInitializationParameters(launcherActivity, extras);

        // The current activity must be set as resumed so VAS can track ad visibility
        final ActivityStateManager activityStateManager = VASAds.getActivityStateManager();
        if (activityStateManager != null) {
            activityStateManager.setState(launcherActivity, ActivityStateManager.ActivityState.RESUMED);
        }

        return true;
    }

    @Override
    protected void load(@NonNull final Context context, @NonNull final AdData adData) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(adData);

        final Map<String, String> extras = adData.getExtras();

        this.activity = (Activity) context;

        final InterstitialAdFactory interstitialAdFactory = new InterstitialAdFactory(activity,
                placementId, new VerizonInterstitialFactoryListener());

        final Bid bid = BidCache.get(placementId);

        if (bid == null) {
            final RequestMetadata.Builder requestMetadataBuilder = new RequestMetadata.Builder(
                    VASAds.getRequestMetadata());
            requestMetadataBuilder.setMediator(VerizonAdapterConfiguration.MEDIATOR_ID);

            final String adContent = extras.get(DataKeys.ADM_KEY);

            if (!TextUtils.isEmpty(adContent)) {
                final Map<String, Object> placementData = new HashMap<>();

                placementData.put(VerizonAdapterConfiguration.REQUEST_METADATA_AD_CONTENT_KEY, adContent);
                placementData.put("overrideWaterfallProvider", "waterfallprovider/sideloading");

                requestMetadataBuilder.setPlacementData(placementData);
            }

            interstitialAdFactory.setRequestMetaData(requestMetadataBuilder.build());
            interstitialAdFactory.load(new VerizonInterstitialListener());
        } else {
            interstitialAdFactory.load(bid, new VerizonInterstitialListener());
        }

        MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);
    }

    @Override
    protected void show() {

        VerizonAdapterConfiguration.postOnUiThread(new Runnable() {

            @Override
            public void run() {
                if (verizonInterstitialAd != null) {
                    verizonInterstitialAd.show(activity);
                } else {
                    MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Attempting to " +
                            "show a Verizon rewarded video before it is ready.");
                    if (mLoadListener != null) {
                        mLoadListener.onAdLoadFailed(MoPubErrorCode.VIDEO_PLAYBACK_ERROR);
                    }
                }
            }
        });
    }

    private class VerizonInterstitialFactoryListener implements
            InterstitialAdFactory.InterstitialAdFactoryListener {

        @Override
        public void onLoaded(final InterstitialAdFactory interstitialAdFactory,
                             final InterstitialAd interstitialAd) {

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

            if (mLoadListener != null) {
                mLoadListener.onAdLoaded();
            }
        }


        @Override
        public void onError(final InterstitialAdFactory interstitialAdFactory,
                            final ErrorInfo errorInfo) {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Failed to load Verizon " +
                    "rewarded video due to error: " + errorInfo.toString());

            if (mLoadListener != null) {
                mLoadListener.onAdLoadFailed(convertErrorInfoToMoPub(errorInfo));
            }
        }
    }

    private class VerizonInterstitialListener implements InterstitialAd.InterstitialAdListener {

        @Override
        public void onError(final InterstitialAd interstitialAd, final ErrorInfo errorInfo) {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Failed to show Verizon " +
                    "rewarded video due to error: " + errorInfo.toString());

            if (mInteractionListener != null) {
                mInteractionListener.onAdFailed(MoPubErrorCode.VIDEO_PLAYBACK_ERROR);
            }
        }

        @Override
        public void onShown(final InterstitialAd interstitialAd) {
            MoPubLog.log(getAdNetworkId(), SHOW_SUCCESS, ADAPTER_NAME);
            if (mInteractionListener != null) {
                mInteractionListener.onAdShown();
            }
        }

        @Override
        public void onClosed(final InterstitialAd interstitialAd) {
            MoPubLog.log(getAdNetworkId(), DID_DISAPPEAR, ADAPTER_NAME);
            if (mInteractionListener != null) {
                mInteractionListener.onAdDismissed();
            }
        }

        @Override
        public void onClicked(final InterstitialAd interstitialAd) {
            MoPubLog.log(getAdNetworkId(), CLICKED, ADAPTER_NAME);
            if (mInteractionListener != null) {
                mInteractionListener.onAdClicked();
            }
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

            if (AD_IMPRESSION_EVENT_ID.equals(eventId)) {
                VerizonAdapterConfiguration.postOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mInteractionListener != null) {
                            mInteractionListener.onAdImpression();
                        }
                    }
                });
            } else if (!rewarded && VIDEO_COMPLETE_EVENT_ID.equals(eventId)) {
                if (mInteractionListener != null) {
                    mInteractionListener.onAdComplete(MoPubReward.success(MoPubReward.NO_REWARD_LABEL,
                            MoPubReward.DEFAULT_REWARD_AMOUNT));
                }

                rewarded = true;
            }
        }
    }
}
