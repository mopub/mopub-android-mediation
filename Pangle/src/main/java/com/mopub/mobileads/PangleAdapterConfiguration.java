package com.mopub.mobileads;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

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
import com.mopub.mobileads.pangle.BuildConfig;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM_WITH_THROWABLE;

public class PangleAdapterConfiguration extends BaseAdapterConfiguration {
    public static final int CONTENT_TYPE = 40000;//# http conent_type
    public static final int REQUEST_PB_ERROR = 40001;//# http request pb
    public static final int NO_AD = 20001;//# no ad
    public static final int ADSLOT_EMPTY = 40004;// ad code id can't been null
    public static final int ADSLOT_ID_ERROR = 40006;// code id error
    private static final String ADAPTER_VERSION = "3.0.0.0.1";
    private static final String ADAPTER_NAME = PangleAdapterConfiguration.class.getSimpleName();
    private static final String MOPUB_NETWORK_NAME = "pangle_network";

    public static final String KEY_EXTRA_AD_PLACEMENT_ID = "ad_placement_id";
    public static final String KEY_EXTRA_APP_ID = "app_id";

    /**
     * Key to publisher to set on to initialize Pangle SDK. (Optional)
     */
    public static final String KEY_EXTRA_SUPPORT_MULTIPROCESS = "support_multiprocess";

    private static boolean sIsSDKInitialized;
    private static boolean SIsSupportMultiProcess;

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
        if (sIsSDKInitialized) {
            return TTAdSdk.getAdManager().getSDKVersion();
        } else {
            final String adapterVersion = getAdapterVersion();
            return adapterVersion.substring(0, adapterVersion.lastIndexOf('.'));
        }
    }

    @Override
    public void initializeNetwork(@NonNull Context context, @Nullable Map<String, String> configuration, @NonNull OnNetworkInitializationFinishedListener listener) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(listener);
        boolean networkInitializationSucceeded = false;
        synchronized (PangleAdapterConfiguration.class) {
            try {

                final String appId = configuration.get(KEY_EXTRA_APP_ID);

                SIsSupportMultiProcess = configuration.get(KEY_EXTRA_SUPPORT_MULTIPROCESS) != null ?
                        Boolean.valueOf(configuration.get(KEY_EXTRA_SUPPORT_MULTIPROCESS)) : false;

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

    public static TTAdManager getPangleSdkManager() {
        if (!sIsSDKInitialized) {
            throw new RuntimeException("TTAdSdk is not init, please check, config params or context maybe null   ");
        }
        return TTAdSdk.getAdManager();
    }

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
                    .useTextureView(isUseTextureView(context))
                    .appName(MOPUB_NETWORK_NAME)
                    .titleBarTheme(TTAdConstant.TITLE_BAR_THEME_DARK)
                    .setGDPR(MoPub.canCollectPersonalInformation() ? 1 : 0) /*set gdpr to Pangle sdk, 0 close GDPR Privacy protection ，1: open GDPR Privacy protection */
                    .allowShowPageWhenScreenLock(true) /* Allow or deny permission to display the landing page ad in the lock screen */
                    .debug(BuildConfig.DEBUG) /*Turn it on during the testing phase, you can troubleshoot with the log, remove it after launching the app */
                    .supportMultiProcess(SIsSupportMultiProcess) /* true for support multi-process environment,false for single-process */
                    .build());
            sIsSDKInitialized = true;
        }
    }

    private static boolean isUseTextureView(Context context) {
        try {
            String pkgName = context.getPackageName();
            PackageManager pkg = context.getPackageManager();
            PackageInfo packageInfo = pkg.getPackageInfo(pkgName, PackageManager.GET_PERMISSIONS);
            String[] requestedPermissions = packageInfo.requestedPermissions;
            String PER = Manifest.permission.WAKE_LOCK;
            if (requestedPermissions != null && requestedPermissions.length > 0) {
                for (String per : requestedPermissions) {
                    if (PER.equalsIgnoreCase(per)) {
                        return true;
                    }
                }
            }
        } catch (Throwable ignore) {
        }
        return false;
    }


    public static MoPubErrorCode mapErrorCode(int error) {
        switch (error) {
            case CONTENT_TYPE:
            case REQUEST_PB_ERROR:
                return MoPubErrorCode.NO_CONNECTION;
            case NO_AD:
                return MoPubErrorCode.NETWORK_NO_FILL;
            case ADSLOT_EMPTY:
            case ADSLOT_ID_ERROR:
                return MoPubErrorCode.MISSING_AD_UNIT_ID;
            default:
                return MoPubErrorCode.UNSPECIFIED;
        }
    }

    public static float[] getAdSizeSafely(Map<String, String> params, String widthName, String heightName) {
        final float[] adSize = new float[]{0, 0};
        if (params == null || widthName == null || heightName == null) {
            return adSize;
        }

        final Object oWidth = params.get(widthName);
        if (oWidth != null) {
            String w = String.valueOf(oWidth);
            adSize[0] = Float.valueOf(w);
        }

        final Object oHeight = params.get(heightName);

        if (oHeight != null) {
            String h = String.valueOf(oHeight);
            adSize[1] = Float.valueOf(h);
        }
        return adSize;
    }

    public static int dp2px(Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

    public static float getScreenWidth(Context context) {
        if (context == null) return -1;
        return (float) context.getResources().getDisplayMetrics().widthPixels;
    }

    public static float getScreenHeight(Context context) {
        if (context == null) return -1;
        return (float) context.getResources().getDisplayMetrics().heightPixels;
    }

    public static int pxtosp(Context context, float pxValue) {
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (pxValue / fontScale + 0.5f);
    }

    public static int sptopx(Context context, float spValue) {
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * fontScale + 0.5f);
    }

    /**
     * Pangle banner support size and ratio  ：
     * 600*300、600*400、600*500、600*260、600*90、600*150、640*100、690*388
     *
     * @param params
     * @return
     */
    public static float[] getBannerAdSizeAdapter(AdData params) {
        float[] adSize = new float[]{0, 0};
        if (params == null) {
            adSize = new float[]{600, 90};
            return adSize;
        }

        final Object oHeight = params.getAdHeight();

        if (oHeight != null) {
            if (oHeight instanceof Integer) {
                adSize[1] = (float) ((Integer) oHeight);
            } else if (oHeight instanceof Float) {
                adSize[1] = (float) oHeight;
            } else {
                adSize[1] = Float.valueOf(String.valueOf(oHeight));
            }
        }

        final Object oWidth = params.getAdWidth();
        if (oWidth != null) {
            if (oWidth instanceof Integer) {
                adSize[0] = (float) ((Integer) oWidth);
            } else if (oWidth instanceof Float) {
                adSize[0] = (float) oWidth;
            } else {
                adSize[0] = Float.valueOf(String.valueOf(oWidth));
            }

            if (adSize[0] > 0 && adSize[0] <= 600) {
                adSize[0] = 600;
                if (adSize[1] <= 100) {
                    adSize[1] = 90;
                } else if (adSize[1] <= 150) {
                    adSize[1] = 150;
                } else if (adSize[1] <= 260) {
                    adSize[1] = 260;
                } else if (adSize[1] <= 300) {
                    adSize[1] = 300;
                } else if (adSize[1] <= 450) {
                    adSize[1] = 400;
                } else {
                    adSize[1] = 500;
                }
            } else if (adSize[0] > 600 && adSize[0] <= 640) {
                adSize[0] = 640;
                adSize[1] = 100;
            } else {
                adSize[0] = 690;
                adSize[1] = 388;
            }
        }

        return adSize;
    }
}
