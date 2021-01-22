package com.mopub.mobileads;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;

import com.mopub.common.BaseAdapterConfiguration;
import com.mopub.common.OnNetworkInitializationFinishedListener;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.unityads.BuildConfig;
import com.unity3d.ads.IUnityAdsInitializationListener;
import com.unity3d.ads.UnityAds;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM_WITH_THROWABLE;

public class UnityAdsAdapterConfiguration extends BaseAdapterConfiguration {

    // Adapter's keys
    public static final String ADAPTER_VERSION = BuildConfig.VERSION_NAME;
    private static final String MOPUB_NETWORK_NAME = BuildConfig.NETWORK_NAME;
    private static final String ADAPTER_NAME = UnityAdsAdapterConfiguration.class.getSimpleName();

    @NonNull
    @Override
    public String getAdapterVersion() {
        return ADAPTER_VERSION;
    }

    @Nullable
    @Override
    public String getBiddingToken(@NonNull Context context) {
        return null;
    }

    @NonNull
    @Override
    public String getMoPubNetworkName() {
        return MOPUB_NETWORK_NAME;
    }

    @NonNull
    @Override
    public String getNetworkSdkVersion() {
        final String sdkVersion = UnityAds.getVersion();

        if (!TextUtils.isEmpty(sdkVersion)) {
            return sdkVersion;
        }

        final String adapterVersion = getAdapterVersion();
        return adapterVersion.substring(0, adapterVersion.lastIndexOf('.'));
    }

    @Override
    public void initializeNetwork(@NonNull final Context context, @Nullable final Map<String, String> configuration, @NonNull final OnNetworkInitializationFinishedListener listener) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(listener);

        synchronized (UnityAdsAdapterConfiguration.class) {
            try {
                if (UnityAds.isInitialized()) {
                    MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unity Ads already initialized. Not attempting to reinitialize.");
                    listener.onNetworkInitializationFinished(UnityAdsAdapterConfiguration.class, MoPubErrorCode.ADAPTER_INITIALIZATION_SUCCESS);
                    return;
                }

                if (configuration == null) {
                    MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unity Ads initialization failed. Configuration is null." +
                            "Note that initialization on the first app launch is a no-op. It will attempt again on first ad request.");
                    listener.onNetworkInitializationFinished(UnityAdsAdapterConfiguration.class, MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
                    return;
                }

                String gameId = configuration.get(UnityRouter.GAME_ID_KEY);
                if (gameId == null || gameId.isEmpty()) {
                    MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unity Ads initialization failed. " +
                            "Parameter gameId is missing or entered incorrectly in the Unity Ads network configuration.");
                    listener.onNetworkInitializationFinished(UnityAdsAdapterConfiguration.class, MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
                    return;
                }

                UnityRouter.initUnityAds(configuration, context, new IUnityAdsInitializationListener() {
                    @Override
                    public void onInitializationComplete() {
                        listener.onNetworkInitializationFinished(UnityAdsAdapterConfiguration.class, MoPubErrorCode.ADAPTER_INITIALIZATION_SUCCESS);
                    }

                    @Override
                    public void onInitializationFailed(UnityAds.UnityAdsInitializationError unityAdsInitializationError, String errorMessage) {
                        if (errorMessage != null) {
                            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unity Ads initialization failed with error: " + errorMessage);
                        }
                        
                        listener.onNetworkInitializationFinished(UnityAdsAdapterConfiguration.class, MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
                    }
                });
            } catch (Exception e) {
                MoPubLog.log(CUSTOM_WITH_THROWABLE, "Initializing Unity Ads has encountered " +
                        "an exception.", e);
                listener.onNetworkInitializationFinished(UnityAdsAdapterConfiguration.class, MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            }
        }

        MoPubLog.LogLevel logLevel = MoPubLog.getLogLevel();
        boolean debugModeEnabled = logLevel == MoPubLog.LogLevel.DEBUG;
        UnityAds.setDebugMode(debugModeEnabled);
    }
}
