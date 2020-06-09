package com.mopub.mobileads;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bytedance.sdk.openadsdk.TTAdConfig;
import com.bytedance.sdk.openadsdk.TTAdConstant;
import com.bytedance.sdk.openadsdk.TTAdManager;
import com.bytedance.sdk.openadsdk.TTAdSdk;
import com.mopub.common.BaseAdapterConfiguration;
import com.mopub.common.MoPub;
import com.mopub.common.OnNetworkInitializationFinishedListener;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM_WITH_THROWABLE;

public class PangleAdapterConfiguration extends BaseAdapterConfiguration {
    private static final String ADAPTER_VERSION = "3.0.0.0.0";
    private static final String ADAPTER_NAME = PangleAdapterConfiguration.class.getSimpleName();
    private static final String MOPUB_NETWORK_NAME = "pangle_network";

    /**
     * Key to obtain Pangle ad unit ID from the extras provided by MoPub.
     */
    public static final String KEY_EXTRA_AD_PLACEMENT_ID = "ad_placement_id";
    public static final String KEY_EXTRA_APP_ID = "app_id";
    public static final String KEY_EXTRA_SUPPORT_MULTIPROCESS = "support_multiprocess";

    private static boolean sIsSDKInitialized;
    private static boolean sSupporMultiprocess;

    @NonNull
    @Override
    public String getAdapterVersion() {
        return ADAPTER_VERSION;
    }

    @Nullable
    @Override
    public String getBiddingToken(@NonNull Context context) {
        return getPangleSdkManager().getBiddingToken();
    }

    @NonNull
    @Override
    public String getMoPubNetworkName() {
        return MOPUB_NETWORK_NAME;
    }

    @NonNull
    @Override
    public String getNetworkSdkVersion() {
        return getSDKVersion();
    }

    @Override
    public void initializeNetwork(@NonNull Context context, @Nullable Map<String, String> configuration, @NonNull OnNetworkInitializationFinishedListener listener) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(listener);
        boolean networkInitializationSucceeded = false;
        synchronized (PangleAdapterConfiguration.class) {
            try {

                String appId = configuration.get(KEY_EXTRA_APP_ID);
                sSupporMultiprocess = configuration.get(KEY_EXTRA_SUPPORT_MULTIPROCESS) != null ? Boolean.valueOf(configuration.get(KEY_EXTRA_SUPPORT_MULTIPROCESS)) : false;
                pangleSdkInit(context, appId);
                networkInitializationSucceeded = true;
            } catch (Exception e) {
                MoPubLog.log(CUSTOM_WITH_THROWABLE, "Initializing Pangle has encountered " +
                        "an exception.", e);
            }
        }

        if (networkInitializationSucceeded) {
            listener.onNetworkInitializationFinished(PangleAdapterConfiguration.class,
                    MoPubErrorCode.ADAPTER_INITIALIZATION_SUCCESS);
        } else {
            listener.onNetworkInitializationFinished(PangleAdapterConfiguration.class,
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
        }
    }

    public String getSDKVersion() {
        if (!sIsSDKInitialized) {
            final String adapterVersion = getAdapterVersion();
            return adapterVersion.substring(0, adapterVersion.lastIndexOf('.'));
        }
        return TTAdSdk.getAdManager().getSDKVersion();
    }

    public static TTAdManager getPangleSdkManager() {
        if (!sIsSDKInitialized) {
            throw new RuntimeException("TTAdSdk is not init, please check.");
        }
        return TTAdSdk.getAdManager();
    }

    /* Initialize sdk */
    public static void pangleSdkInit(Context context, String appId) {
        if (appId == null || context == null) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME,
                    "Invalid Pangle app Id. Ensure the app id is valid on the MoPub dashboard.");
            return;
        }
        if (!sIsSDKInitialized) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "PangleSDK initialize with appId = " + appId);
            TTAdSdk.init(context, new TTAdConfig.Builder()
                    .appId(appId)
                    .useTextureView(true)
                    .appName(MOPUB_NETWORK_NAME)
                    .titleBarTheme(TTAdConstant.TITLE_BAR_THEME_DARK)
                    .setGDPR(MoPub.canCollectPersonalInformation() ? 1 : 0) /*set gdpr to Pangle sdk, 0 close GDPR Privacy protection ，1: open GDPR Privacy protection */
                    .allowShowPageWhenScreenLock(true) /* Allow or deny permission to display the landing page ad in the lock screen */
                    .debug(BuildConfig.DEBUG) /*Turn it on during the testing phase, you can troubleshoot with the log, remove it after launching the app */
                    .supportMultiProcess(sSupporMultiprocess) /* true for support multi-process environment,false for single-process */
                    .build());
            sIsSDKInitialized = true;
        }
    }
}
