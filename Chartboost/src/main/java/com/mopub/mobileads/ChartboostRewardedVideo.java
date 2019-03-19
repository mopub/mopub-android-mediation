package com.mopub.mobileads;

import android.app.Activity;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.chartboost.sdk.Chartboost;
import com.mopub.common.DataKeys;
import com.mopub.common.LifecycleListener;
import com.mopub.common.MediationSettings;
import com.mopub.common.logging.MoPubLog;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_FAILED;

public class ChartboostRewardedVideo extends CustomEventRewardedVideo {

    @NonNull
    public String mLocation = ChartboostShared.LOCATION_DEFAULT;

    @NonNull
    private static final LifecycleListener sLifecycleListener =
            new ChartboostLifecycleListener();

    @NonNull
    private final Handler mHandler;

    private static final String ADAPTER_NAME = ChartboostRewardedVideo.class.getSimpleName();

    @NonNull
    private ChartboostAdapterConfiguration mChartboostAdapterConfiguration;

    public ChartboostRewardedVideo() {
        mHandler = new Handler();
        mChartboostAdapterConfiguration = new ChartboostAdapterConfiguration();
    }

    @Override
    @NonNull
    public CustomEventRewardedVideoListener getVideoListenerForSdk() {
        return ChartboostShared.getDelegate();
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

    @Override
    public boolean checkAndInitializeSdk(@NonNull Activity launcherActivity,
                                         @NonNull Map<String, Object> localExtras,
                                         @NonNull Map<String, String> serverExtras) throws Exception {
        // We need to attempt to reinitialize Chartboost on each request, in case an interstitial has been
        // loaded and used since then.
        ChartboostShared.initializeSdk(launcherActivity, serverExtras);  // throws IllegalStateException

        // Always return true so that the lifecycle listener is registered even if an interstitial
        // did the initialization.
        return true;
    }

    @Override
    protected void loadWithSdkInitialized(@NonNull Activity activity,
                                          @NonNull Map<String, Object> localExtras, @NonNull Map<String, String> serverExtras)
            throws Exception {

        if (serverExtras.containsKey(ChartboostShared.LOCATION_KEY)) {
            String location = serverExtras.get(ChartboostShared.LOCATION_KEY);
            mLocation = TextUtils.isEmpty(location) ? mLocation : location;

            mChartboostAdapterConfiguration.setCachedInitializationParameters(activity, serverExtras);
        }

        ChartboostShared.getDelegate().registerRewardedVideoLocation(mLocation);
        setUpMediationSettingsForRequest((String) localExtras.get(DataKeys.AD_UNIT_ID_KEY));

        // We do this to ensure that the custom event manager has a chance to get the listener
        // and ad unit ID before any delegate callbacks are made.
        mHandler.post(new Runnable() {
            public void run() {
                if (Chartboost.hasRewardedVideo(mLocation)) {
                    ChartboostShared.getDelegate().didCacheRewardedVideo(mLocation);
                } else {
                    Chartboost.cacheRewardedVideo(mLocation);
                    MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);
                }
            }
        });
    }

    private void setUpMediationSettingsForRequest(String moPubId) {
        final ChartboostMediationSettings globalSettings =
                MoPubRewardedVideoManager.getGlobalMediationSettings(ChartboostMediationSettings.class);
        final ChartboostMediationSettings instanceSettings =
                MoPubRewardedVideoManager.getInstanceMediationSettings(ChartboostMediationSettings.class, moPubId);

        // Instance settings override global settings.
        if (instanceSettings != null) {
            Chartboost.setCustomId(instanceSettings.getCustomId());
        } else if (globalSettings != null) {
            Chartboost.setCustomId(globalSettings.getCustomId());
        }
    }

    @Override
    public boolean hasVideoAvailable() {
        return Chartboost.hasRewardedVideo(mLocation);
    }

    @Override
    public void showVideo() {
        MoPubLog.log(SHOW_ATTEMPTED, ADAPTER_NAME);

        if (hasVideoAvailable()) {
            Chartboost.showRewardedVideo(mLocation);
        } else {
            MoPubRewardedVideoManager.onRewardedVideoPlaybackError(
                    ChartboostRewardedVideo.class,
                    getAdNetworkId(),
                    MoPubErrorCode.NETWORK_NO_FILL);

            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Attempted to show Chartboost rewarded video before it " +
                    "was available.");

            MoPubLog.log(SHOW_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                    MoPubErrorCode.NETWORK_NO_FILL);
        }
    }

    @Override
    protected void onInvalidate() {
        // This prevents sending didCache or didFailToCache callbacks.
        ChartboostShared.getDelegate().unregisterRewardedVideoLocation(mLocation);
    }

    private static final class ChartboostLifecycleListener implements LifecycleListener {
        @Override
        public void onCreate(@NonNull Activity activity) {
            Chartboost.onCreate(activity);
        }

        @Override
        public void onStart(@NonNull Activity activity) {
            Chartboost.onStart(activity);
        }

        @Override
        public void onPause(@NonNull Activity activity) {
            Chartboost.onPause(activity);
        }

        @Override
        public void onResume(@NonNull Activity activity) {
            Chartboost.onResume(activity);
        }

        @Override
        public void onRestart(@NonNull Activity activity) {
        }

        @Override
        public void onStop(@NonNull Activity activity) {
            Chartboost.onStop(activity);
        }

        @Override
        public void onDestroy(@NonNull Activity activity) {
            Chartboost.onDestroy(activity);
        }

        @Override
        public void onBackPressed(@NonNull Activity activity) {
            Chartboost.onBackPressed();
        }
    }

    public static final class ChartboostMediationSettings implements MediationSettings {
        @NonNull
        private final String mCustomId;

        public ChartboostMediationSettings(@NonNull final String customId) {
            mCustomId = customId;
        }

        @NonNull
        public String getCustomId() {
            return mCustomId;
        }
    }
}
