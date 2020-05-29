package com.mopub.mobileads;

import android.annotation.SuppressLint;
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
import com.mopub.common.privacy.PersonalInfoManager;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM_WITH_THROWABLE;

/**
 * created by wuzejian on 2019/4/14
 */
public class PangleAdapterConfiguration extends BaseAdapterConfiguration {
    public static final String ADAPTER_VERSION = "3.0.0.0";
    private static final String TAG = "PangleAdapterConfiguration";
    private static final String MOPUB_NETWORK_NAME = "pangle_network";


    public static final String PANGLE_APP_ID_KEY = "app_id";
    public static final String PANGLE_APP_NAME_KEY = "app_name";
    /**
     * Key to obtain Pangolin ad unit ID from the extras provided by MoPub.
     */
    public static final String KEY_EXTRA_AD_UNIT_ID = "ad_placement_id";


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

    /**
     * returns a lowercase String that represents your ad network name. Use underscores if the String needs to contain spaces
     *
     * @return
     */
    @NonNull
    @Override
    public String getMoPubNetworkName() {
        return MOPUB_NETWORK_NAME;
    }

    /**
     * returns the version number of your ad network SDK.
     *
     * @return
     */
    @NonNull
    @Override
    public String getNetworkSdkVersion() {
        return getSDKVersion();
    }

    @SuppressLint("LongLogTag")
    @Override
    public void initializeNetwork(@NonNull Context context, @Nullable Map<String, String> configuration, @NonNull OnNetworkInitializationFinishedListener listener) {
        MoPubLog.log(CUSTOM, TAG, "PangleAdapterConfiguration#initializeNetwork....");
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(listener);
        boolean networkInitializationSucceeded = false;
        synchronized (PangleAdapterConfiguration.class) {

            try {

                String appId = configuration.get(PANGLE_APP_ID_KEY);
                String appName = configuration.get(PANGLE_APP_NAME_KEY);

                /** init pangle sdk */
                pangleSdkInit(context, appId, appName);
                PersonalInfoManager personalInformationManager = MoPub.getPersonalInformationManager();
                if (personalInformationManager != null && personalInformationManager.gdprApplies()) {
                    boolean canCollect = MoPub.canCollectPersonalInformation();

                    /** set gdpr to pangle sdk, 0 close GDRP Privacy protection ，1: open GDRP Privacy protection */
                    getPangleSdkManager().setGdpr(canCollect ? 0 : 1);
                }
                networkInitializationSucceeded = true;
            } catch (Exception e) {
                MoPubLog.log(CUSTOM_WITH_THROWABLE, "Initializing AdMob has encountered " +
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


    private static boolean sInit;

    public static String getSDKVersion() {
        if (!sInit) return ADAPTER_VERSION;
        return TTAdSdk.getAdManager().getSDKVersion();
    }

    public static TTAdManager getPangleSdkManager() {
        if (!sInit) {
            throw new RuntimeException("TTAdSdk is not init, please check.");
        }
        return TTAdSdk.getAdManager();
    }

    /* Initialize sdk */
    public static void pangleSdkInit(Context context, String appId, String appName) {
        if (appId == null || context == null) return;
        if (!sInit) {
            TTAdSdk.init(context, new TTAdConfig.Builder()
                    .appId(appId)
                    .useTextureView(true)/*Use TextureView to play the video. The default setting is SurfaceView, when the context is in conflict with SurfaceView, you can use TextureView */
                    .appName(appName)
                    .titleBarTheme(TTAdConstant.TITLE_BAR_THEME_DARK)
                    .setGDPR(1)
                    .allowShowPageWhenScreenLock(true) /* Allow or deny permission to display the landing page ad in the lock screen */
                    .debug(true)/*Turn it on during the testing phase, you can troubleshoot with the log, remove it after launching the app */
                    .supportMultiProcess(false) /* true for support multi-process environment,false for single-process */
                    //.httpStack(new MyOkStack3())/*optional,you can customize network library for sdk, the demo is based on the okhttp3 */
                    .coppa(0) /* Fields to indicate whether you are a child or an adult ，0:adult ，1:child */
                    .build());
            sInit = true;
        }
    }
}
