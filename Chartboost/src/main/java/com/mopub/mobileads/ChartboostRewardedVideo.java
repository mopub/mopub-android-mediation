package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.chartboost.sdk.Chartboost;
import com.mopub.common.LifecycleListener;
import com.mopub.common.MediationSettings;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;

import java.lang.ref.WeakReference;
import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM_WITH_THROWABLE;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_FAILED;
import static com.mopub.mobileads.MoPubErrorCode.FULLSCREEN_SHOW_ERROR;
import static com.mopub.mobileads.MoPubErrorCode.VIDEO_PLAYBACK_ERROR;

public class ChartboostRewardedVideo extends BaseAd {

    private static String ADAPTER_NAME = ChartboostRewardedVideo.class.getSimpleName();
    private static final String CUSTOM_ID_KEY = "customId";

    @NonNull
    public String mLocation = ChartboostShared.LOCATION_DEFAULT;

    @NonNull
    private ChartboostAdapterConfiguration mChartboostAdapterConfiguration;

    /**
     * A Weak reference of the activity used to show the Chartboost Rewarded Ad
     */
    private WeakReference<Activity> mWeakActivity;

    @NonNull
    private static final LifecycleListener sLifecycleListener =
            new ChartboostLifecycleListener();

    @NonNull
    private final Handler mHandler;

    public ChartboostRewardedVideo() {
        mHandler = new Handler();
        mChartboostAdapterConfiguration = new ChartboostAdapterConfiguration();
    }

    @Override
    protected boolean checkAndInitializeSdk(@NonNull final Activity launcherActivity,
                                            @NonNull final AdData adData) {
        Preconditions.checkNotNull(launcherActivity);
        Preconditions.checkNotNull(adData);

        // We need to attempt to reinitialize Chartboost on each request, in case a rewarded video has been
        try {
            ChartboostAdapterConfiguration.initializeChartboostSdk(launcherActivity, adData.getExtras());
        } catch (Exception initializationError) {
            MoPubLog.log(CUSTOM_WITH_THROWABLE, ADAPTER_NAME,
                    "Chartboost initialization called by adapter " + ADAPTER_NAME +
                            " has failed because of an exception", initializationError.getMessage());
        }

        // Always return true so that the lifecycle listener is registered even if a rewarded video
        // did the initialization.
        return true;
    }

    @Override
    protected void load(@NonNull final Context context,
                        @NonNull final AdData adData) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(adData);

        setAutomaticImpressionAndClickTracking(false);

        // Activity check for context
        if (!(context instanceof Activity)) {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Context passed to load " +
                    "was not an Activity. Failing the request.");
            if (mLoadListener != null) {
                mLoadListener.onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            }
            return;
        }
        mWeakActivity = new WeakReference<>((Activity) context);

        // Extract and parse extras
        final Map<String, String> extras = adData.getExtras();
        if (extras.containsKey(ChartboostShared.LOCATION_KEY)) {
            String location = extras.get(ChartboostShared.LOCATION_KEY);
            mLocation = TextUtils.isEmpty(location) ? mLocation : location;
        }

        mChartboostAdapterConfiguration.setCachedInitializationParameters(context, extras);

        // Chartboost delegation can be set to null on some cases in Chartboost SDK 8.0+.
        // We should set the delegation on each load request to prevent this.
        Chartboost.setDelegate(ChartboostShared.getDelegate());

        // If there's already a listener for this location, then another
        // instance is still active and we should fail.
        if (ChartboostShared.getDelegate().hasLoadLocation(mLocation) &&
                ChartboostShared.getDelegate().getLoadListener(mLocation) != mLoadListener) {
            MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                    MoPubErrorCode.NETWORK_NO_FILL);
            mLoadListener.onAdLoadFailed(MoPubErrorCode.NETWORK_NO_FILL);

            return;
        }

        // If there's no listener present, register the new listener
        try {
            ChartboostShared.getDelegate().registerLoadListener(mLocation, mLoadListener);
        } catch (NullPointerException | IllegalStateException e) {
            MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                    MoPubErrorCode.NETWORK_NO_FILL);
            mLoadListener.onAdLoadFailed(MoPubErrorCode.NETWORK_NO_FILL);

            return;
        }

        ChartboostShared.getDelegate().registerRewardedVideoLocation(mLocation);
        setUpMediationSettingsForRequest(adData.getAdUnit(), extras);

        // Request rewarded video. If it's already cached, directly show it.
        if (Chartboost.hasRewardedVideo(mLocation)) {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME,
                    "Chartboost already has the rewarded video ready. Calling didCacheRewardedVideo.");
            ChartboostShared.getDelegate().didCacheRewardedVideo(mLocation);
        } else {
            MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);
            Chartboost.cacheRewardedVideo(mLocation);
        }
    }

    private void setUpMediationSettingsForRequest(String adUnitId, Map<String, String> extras) {
        final ChartboostMediationSettings globalSettings =
                MoPubRewardedVideoManager.getGlobalMediationSettings(ChartboostMediationSettings.class);
        final ChartboostMediationSettings instanceSettings =
                MoPubRewardedVideoManager.getInstanceMediationSettings(ChartboostMediationSettings.class, adUnitId);

        final String customId = extras.get(CUSTOM_ID_KEY);

        if (!TextUtils.isEmpty(customId)) {
            Chartboost.setCustomId(customId);
        } else if (instanceSettings != null) {
            Chartboost.setCustomId(instanceSettings.getCustomId());
        } else if (globalSettings != null) {
            Chartboost.setCustomId(globalSettings.getCustomId());
        }
    }

    @Override
    public void show() {
        ChartboostShared.getDelegate().registerInteractionListener(mLocation, mInteractionListener);
        MoPubLog.log(getAdNetworkId(), SHOW_ATTEMPTED, ADAPTER_NAME);

        if (mWeakActivity != null && mWeakActivity.get() != null) {
            Chartboost.showRewardedVideo(mLocation);
        } else {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME,
                    "Chartboost Rewarded Video Activity reference is null. Cannot show the ad. " +
                            "Ensure that the context requesting the Rewarded Video is an Activity.");
            MoPubLog.log(getAdNetworkId(), SHOW_FAILED, ADAPTER_NAME);
            if (mInteractionListener != null) {
                mInteractionListener.onAdFailed(VIDEO_PLAYBACK_ERROR);
            }
        }
    }

    @Override
    protected void onInvalidate() {
        ChartboostShared.getDelegate().unregisterLoadListener(mLocation);
        ChartboostShared.getDelegate().unregisterInteractionListener(mLocation);
    }

    @Override
    @NonNull
    public LifecycleListener getLifecycleListener() {
        return sLifecycleListener;
    }

    @Override
    @NonNull
    public String getAdNetworkId() {
        return mLocation;
    }

    private static final class ChartboostLifecycleListener implements LifecycleListener {
        @Override
        public void onCreate(@NonNull Activity activity) {
        }

        @Override
        public void onStart(@NonNull Activity activity) {
        }

        @Override
        public void onPause(@NonNull Activity activity) {
        }

        @Override
        public void onResume(@NonNull Activity activity) {
        }

        @Override
        public void onRestart(@NonNull Activity activity) {
        }

        @Override
        public void onStop(@NonNull Activity activity) {
        }

        @Override
        public void onDestroy(@NonNull Activity activity) {
        }

        @Override
        public void onBackPressed(@NonNull Activity activity) {
            Chartboost.onBackPressed();
        }
    }

    public static final class ChartboostMediationSettings implements MediationSettings {
        @NonNull
        private String customId = "";

        public ChartboostMediationSettings() {
        }

        public ChartboostMediationSettings(@NonNull final String customId) {
            this.customId = customId;
        }

        public void setCustomId(@NonNull String customId) {
            this.customId = customId;
        }

        @NonNull
        public String getCustomId() {
            return customId;
        }
    }
}
