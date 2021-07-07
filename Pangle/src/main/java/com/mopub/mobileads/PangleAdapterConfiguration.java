package com.mopub.mobileads;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bytedance.sdk.openadsdk.TTAdConfig;
import com.bytedance.sdk.openadsdk.TTAdManager;
import com.bytedance.sdk.openadsdk.TTAdSdk;
import com.mopub.common.BaseAdapterConfiguration;
import com.mopub.common.MoPub;
import com.mopub.common.OnNetworkInitializationFinishedListener;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.pangle.BuildConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM_WITH_THROWABLE;

public class PangleAdapterConfiguration extends BaseAdapterConfiguration {

    /**
     * Error Code in Pangle SDK
     * For more error code, please refer to
     *
     * @https://partner.oceanengine.com/union/media/union/download/detail?id=20&docId=5de8daa6b1afac0012933137&osType=android
     */
    public static final int CONTENT_TYPE_ERROR = 40000;
    public static final int REQUEST_PARAMETER_ERROR = 40001;
    public static final int NO_AD = 20001;
    public static final int PLACEMENT_EMPTY_ERROR = 40004;
    public static final int PLACEMENT_ERROR = 40006;

    private static final String ADAPTER_VERSION = BuildConfig.VERSION_NAME;
    private static final String ADAPTER_NAME = PangleAdapterConfiguration.class.getSimpleName();
    private static final String MOPUB_NETWORK_NAME = "pangle";

    public static final String AD_PLACEMENT_ID_EXTRA_KEY = "ad_placement_id";
    public static final String APP_ID_EXTRA_KEY = "app_id";

    /**
     * Key for publisher to set on to initialize Pangle SDK. (Optional)
     */
    public static final String SUPPORT_MULTIPROCESS_EXTRA_KEY = "support_multiprocess";

    private static boolean sIsSDKInitialized;
    private static boolean sIsSupportMultiProcess;

    private static String mRewardName;
    private static int mRewardAmount;
    private static String mUserID;
    private static String mMediaExtra;
    private static int mMediaViewWidth;
    private static int mMediaViewHeight;

    @NonNull
    @Override
    public String getAdapterVersion() {
        return ADAPTER_VERSION;
    }

    @Nullable
    @Override
    public String getBiddingToken(@NonNull Context context) {
        return (getPangleSdkManager() != null) ? getPangleSdkManager().getBiddingToken() : null;
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
                if (configuration != null && !configuration.isEmpty()) {
                    final String appId = configuration.get(APP_ID_EXTRA_KEY);

                    sIsSupportMultiProcess = configuration.get(SUPPORT_MULTIPROCESS_EXTRA_KEY) != null ?
                            Boolean.valueOf(configuration.get(SUPPORT_MULTIPROCESS_EXTRA_KEY)) : false;

                    pangleSdkInit(context, appId);
                    networkInitializationSucceeded = true;
                } else {
                    MoPubLog.log(CUSTOM, ADAPTER_NAME,
                            "Pangle SDK is not initialized, please check whether app ID is empty or null in sdkConfiguration.");
                }
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
        return TTAdSdk.getAdManager();
    }

    public static void pangleSdkInit(Context context, String appId) {
        if (TextUtils.isEmpty(appId) || context == null) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME,
                    "Invalid Pangle app ID. Ensure the app id is valid on the MoPub dashboard.");
            return;
        }

        if (!sIsSDKInitialized) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Pangle SDK initializes with app ID: " + appId);

            boolean hasWakeLockPermission = hasWakeLockPermission(context);
            if (!hasWakeLockPermission) {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "For video ads to work in Pangle Ad TextureView, " +
                        "declare the android.permission.WAKE_LOCK permission in your AndroidManifest.");
            }

            TTAdSdk.init(context, new TTAdConfig.Builder()
                    .appId(appId)
                    .useTextureView(hasWakeLockPermission)
                    /* Allow or deny permission to display the landing page ad in the lock screen */
                    .debug(MoPubLog.getLogLevel() == MoPubLog.LogLevel.DEBUG)
                    .supportMultiProcess(sIsSupportMultiProcess)
                    /* true for support multi-process environment, false for single-process */
                    .data(getAdCallSource().toString())
                    .build());

            getPangleSdkManager().setGdpr(MoPub.canCollectPersonalInformation() ? 0 : 1);
            sIsSDKInitialized = true;
        }
    }

    private static JSONArray getAdCallSource() {
        JSONArray adCallSource = new JSONArray();

        try {
            JSONObject mediationObject = new JSONObject();
            mediationObject.putOpt("name", "mediation");
            mediationObject.putOpt("value", "mopub");
            adCallSource.put(mediationObject);

            JSONObject adapterVersionObject = new JSONObject();
            adapterVersionObject.putOpt("name", "adapter_version");
            adapterVersionObject.putOpt("value", "1.4.0");
            adCallSource.put(adapterVersionObject);
        } catch (Throwable exception) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "AdCallSource encounter parsing error: " + exception.getLocalizedMessage());
        }
        return adCallSource;
    }

    private static boolean hasWakeLockPermission(Context context) {
        try {
            String packageName = context.getPackageName();
            PackageManager packageManager = context.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS);
            String[] requestedPermissions = packageInfo.requestedPermissions;
            String wakeLockPermission = Manifest.permission.WAKE_LOCK;
            if (requestedPermissions != null && requestedPermissions.length > 0) {
                for (String per : requestedPermissions) {
                    if (wakeLockPermission.equalsIgnoreCase(per)) {
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
            case CONTENT_TYPE_ERROR:
                return MoPubErrorCode.NO_CONNECTION;
            case REQUEST_PARAMETER_ERROR:
                return MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR;
            case NO_AD:
                return MoPubErrorCode.NETWORK_NO_FILL;
            case PLACEMENT_EMPTY_ERROR:
            case PLACEMENT_ERROR:
                return MoPubErrorCode.MISSING_AD_UNIT_ID;
            default:
                return MoPubErrorCode.UNSPECIFIED;
        }
    }

    public static void setRewardName(String rewardName) {
        mRewardName = rewardName;
    }

    public static String getRewardName() {
        return mRewardName;
    }

    public static void setRewardAmount(int rewardAmount) {
        mRewardAmount = rewardAmount;
    }

    public static int getRewardAmount() {
        return mRewardAmount;
    }

    public static void setUserID(String userID) {
        mUserID = userID;
    }

    public static String getUserID() {
        return mUserID;
    }

    public static void setMediaExtra(String mediaExtra) {
        mMediaExtra = mediaExtra;
    }

    public static String getMediaExtra() {
        return mMediaExtra;
    }

    public static int getMediaViewWidth() {
        return mMediaViewWidth;
    }

    public static void setMediaViewWidth(int mediaViewWidth) {
        mMediaViewWidth = mediaViewWidth;
    }

    public static int getMediaViewHeight() {
        return mMediaViewHeight;
    }

    public static void setMediaViewHeight(int mediaViewHeight) {
        mMediaViewHeight = mediaViewHeight;
    }
}
