package com.mopub.mobileads;

import android.content.Context;
import android.text.TextUtils;

import com.mbridge.msdk.MBridgeConstans;
import com.mbridge.msdk.MBridgeSDK;
import com.mbridge.msdk.out.MBridgeSDKFactory;
import com.mbridge.msdk.out.SDKInitStatusListener;

import java.util.Map;

public final class MintegralSdkManager {
    private static MBSDKInitializeState currentState;

    private volatile String mAppKey;
    private volatile String mAppId;
    private volatile MBSDKInitializeListener sdkInitializeListener;

    private final MBridgeSDK mintegralSdk;

    private MintegralSdkManager() {
        currentState = MBSDKInitializeState.SDK_STATE_UN_INITIALIZE;
        mintegralSdk = MBridgeSDKFactory.getMBridgeSDK();
    }

    public static MintegralSdkManager getInstance() {
        return ClassHolder.M_BRIDGE_CUSTOM_SDK_MANAGER;
    }

    public MBridgeSDK getMBridgeSDK() {
        return mintegralSdk;
    }

    public synchronized void initialize(final Context context, final String appKey, final String appID,
                                        final boolean debugLogEnabled, final Map<String, String> map,
                                        final MBSDKInitializeListener sdkInitializeListener) {
        if (currentState == MBSDKInitializeState.SDK_STATE_INITIALIZING) {
            if (null != sdkInitializeListener) {
                sdkInitializeListener.onInitializeFailure("Mintegral failed to initialize");
            }
            return;
        }

        this.sdkInitializeListener = sdkInitializeListener;

        if (checkSDKInitializeEnvironment(context, appKey, appID)) {
            if (currentState == MBSDKInitializeState.SDK_STATE_INITIALIZE_SUCCESS) {
                if (TextUtils.equals(this.mAppId, appID) && TextUtils.equals(this.mAppKey, appKey)) {
                    if (null != this.sdkInitializeListener) {
                        this.sdkInitializeListener.onInitializeSuccess(this.mAppKey, this.mAppId);
                    }
                    return;
                }
            }

            currentState = MBSDKInitializeState.SDK_STATE_INITIALIZING;
            this.mAppKey = appKey;
            this.mAppId = appID;

            try {
                MBridgeConstans.DEBUG = debugLogEnabled;
                final Map<String, String> configs = mintegralSdk.getMBConfigurationMap(this.mAppId, this.mAppKey);

                if (null != map && !map.isEmpty()) {
                    configs.putAll(map);
                }

                mintegralSdk.init(configs, context, new DefaultSDKInitStatusListener(this.mAppKey,
                        this.mAppId, this.sdkInitializeListener));
            } catch (Exception e) {
                currentState = MBSDKInitializeState.SDK_STATE_INITIALIZE_FAILURE;

                if (null != this.sdkInitializeListener) {
                    sdkInitializeListener.onInitializeFailure(e.getMessage());
                }
            }
        }
    }

    private static final class ClassHolder {
        private static final MintegralSdkManager M_BRIDGE_CUSTOM_SDK_MANAGER = new MintegralSdkManager();
    }

    private static class DefaultSDKInitStatusListener implements SDKInitStatusListener {
        private final String appKey;
        private final String appID;
        private final MBSDKInitializeListener sdkInitializeListener;

        public DefaultSDKInitStatusListener(String appKey, String appID,
                                            MBSDKInitializeListener sdkInitializeListener) {
            this.appKey = appKey;
            this.appID = appID;
            this.sdkInitializeListener = sdkInitializeListener;
        }

        @Override
        public void onInitSuccess() {
            currentState = MBSDKInitializeState.SDK_STATE_INITIALIZE_SUCCESS;

            if (null != sdkInitializeListener) {
                sdkInitializeListener.onInitializeSuccess(this.appKey, this.appID);
            }
        }

        @Override
        public void onInitFail() {
            currentState = MBSDKInitializeState.SDK_STATE_INITIALIZE_FAILURE;

            if (null != sdkInitializeListener) {
                sdkInitializeListener.onInitializeFailure("Mintegral initialization failed due to " +
                        "unknown reasons.");
            }
        }
    }

    private boolean checkSDKInitializeEnvironment(final Context context, final String appKey, final String appID) {
        boolean environmentAvailable = true;
        String errorMessage = "";

        if (context == null) {
            environmentAvailable = false;
            errorMessage = "context must not null";
        }

        if (TextUtils.isEmpty(appKey) || TextUtils.isEmpty(appID)) {
            environmentAvailable = false;
            errorMessage = TextUtils.isEmpty(errorMessage) ? "appKey or appID must not null" :
                    errorMessage + " & appKey or appID must not null";
        }

        if (!environmentAvailable && !TextUtils.isEmpty(errorMessage)) {
            if (null != sdkInitializeListener) {
                currentState = MBSDKInitializeState.SDK_STATE_INITIALIZE_FAILURE;
                sdkInitializeListener.onInitializeFailure(errorMessage);
            }
        }

        return environmentAvailable;
    }

    public interface MBSDKInitializeListener {
        void onInitializeSuccess(String appKey, String appID);

        void onInitializeFailure(String message);
    }

    public enum MBSDKInitializeState {
        SDK_STATE_UN_INITIALIZE,
        SDK_STATE_INITIALIZING,
        SDK_STATE_INITIALIZE_SUCCESS,
        SDK_STATE_INITIALIZE_FAILURE
    }
}
