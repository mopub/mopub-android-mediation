package com.mopub.mobileads.mbridge;

import android.content.Context;
import android.text.TextUtils;

import com.mbridge.msdk.MBridgeConstans;
import com.mbridge.msdk.MBridgeSDK;
import com.mbridge.msdk.interstitialvideo.out.MBInterstitialVideoHandler;
import com.mbridge.msdk.out.MBRewardVideoHandler;
import com.mbridge.msdk.out.MBridgeSDKFactory;
import com.mbridge.msdk.out.SDKInitStatusListener;

import java.util.HashMap;
import java.util.Map;

/**
 * mb Android SDK Manager
 */
public final class MBridgeSDKManager {
    private Context context;
    private volatile String appKey;
    private volatile String appID;
    private volatile MBSDKInitializeListener sdkInitializeListener;
    private volatile Map<String, MBRewardVideoHandler> rewardVideoHandlerMap;
    private volatile Map<String, MBInterstitialVideoHandler> interstitialVideoHandlerMap;

    /**
     * current sdk init state
     */
    private static MBSDKInitializeState currentState;
    private MBridgeSDK mBridgeSDK;

    private MBridgeSDKManager() {
        currentState = MBSDKInitializeState.SDK_STATE_UN_INITIALIZE;
        mBridgeSDK = MBridgeSDKFactory.getMBridgeSDK();
    }

    public static MBridgeSDKManager getInstance() {
        return ClassHolder.M_BRIDGE_CUSTOM_SDK_MANAGER;
    }

    /**
     * get current sdk init state
     *
     * @return SDK current state
     */
    public MBSDKInitializeState getCurrentState() {
        return currentState;
    }

    /**
     * get current app id
     *
     * @return
     */
    public String getAppID() {
        return appID;
    }

    /**
     * get current app key
     *
     * @return
     */
    public String getAppKey() {
        return appKey;
    }

    /**
     * get current mbSDK object, return null if sdk init state is not SDK_STATE_INITIALIZE_SUCCESS
     *
     * @return
     */
    public MBridgeSDK getMBridgeSDK() {
        return mBridgeSDK;
    }

    /**
     * init mb sdk.
     *
     * @param context
     * @param appKey
     * @param appID
     * @param debug
     */
    public synchronized void initialize(final Context context, final String appKey, final String appID, final boolean debug, final Map<String, String> map, final MBSDKInitializeListener sdkInitializeListener) {

        if (currentState == MBSDKInitializeState.SDK_STATE_INITIALIZING) {
            // 正在初始化
            if (null != sdkInitializeListener) {
                sdkInitializeListener.onInitializeFailure("sdk is initializing");
            }
            return;
        }

        this.sdkInitializeListener = sdkInitializeListener;
        if (checkSDKInitializeEnvironment(context, appKey, appID)) {
            if (currentState == MBSDKInitializeState.SDK_STATE_INITIALIZE_SUCCESS) {
                // 已经初始化成功了
                if (TextUtils.equals(this.appID, appID) && TextUtils.equals(this.appKey, appKey)) {
                    // 没有变化
                    if (null != this.sdkInitializeListener) {
                        this.sdkInitializeListener.onInitializeSuccess(this.appKey, this.appID);
                    }
                    return;
                }
            }

            currentState = MBSDKInitializeState.SDK_STATE_INITIALIZING;
            this.context = context;
            this.appKey = appKey;
            this.appID = appID;
            realSDKInitialize(debug, map, this.sdkInitializeListener);
        }
    }

    public synchronized void initialize(final Context context, final String appKey, final String appID, final boolean debug, final Map<String, String> map) {
        this.initialize(context, appKey, appID, debug, map, null);
    }


    public synchronized void initialize(final Context context, final String appKey, final String appID) {
        this.initialize(context, appKey, appID, false, null, null);
    }

    public synchronized void initialize(final Context context, final String appKey, final String appID, final boolean debug) {
        this.initialize(context, appKey, appID, debug, null, null);
    }

    public synchronized void initialize(final Context context, final String appKey, final String appID, final boolean debug, final MBSDKInitializeListener sdkInitializeListener) {
        this.initialize(context, appKey, appID, debug, null, sdkInitializeListener);
    }

    public synchronized void initialize(final Context context, final String appKey, final String appID, final MBSDKInitializeListener sdkInitializeListener) {
        this.initialize(context, appKey, appID, false, null, sdkInitializeListener);
    }

    private void realSDKInitialize(boolean debug, Map<String, String> map, MBSDKInitializeListener sdkInitializeListener) {
        try {
            // config SDK debug mode
            MBridgeConstans.DEBUG = debug;
            Map<String, String> configurationMap = mBridgeSDK.getMBConfigurationMap(this.appID, this.appKey);
            if (null != map && !map.isEmpty()) {
                configurationMap.putAll(map);
            }
            mBridgeSDK.init(configurationMap, context, new DefaultSDKInitStatusListener(this.appKey, this.appID, this.sdkInitializeListener));
        } catch (Exception e) {
            currentState = MBSDKInitializeState.SDK_STATE_INITIALIZE_FAILURE;
            if (null != this.sdkInitializeListener) {
                sdkInitializeListener.onInitializeFailure(e.getMessage());
            }
        }
    }

    private static final class ClassHolder {
        private static final MBridgeSDKManager M_BRIDGE_CUSTOM_SDK_MANAGER = new MBridgeSDKManager();
    }

    private static class DefaultSDKInitStatusListener implements SDKInitStatusListener {
        private String appKey;
        private String appID;
        private MBSDKInitializeListener sdkInitializeListener;

        public DefaultSDKInitStatusListener(String appKey, String appID, MBSDKInitializeListener sdkInitializeListener) {
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
                sdkInitializeListener.onInitializeFailure("sdk initialize failed： an exception occurs");
            }
        }
    }

    private boolean checkSDKInitializeEnvironment(final Context context, final String appKey, final String appID) {
        boolean environmentAvailable = true;
        String errorMessage = "";
        if (null == context) {
            environmentAvailable = false;
            errorMessage = "context must not null";
        }

        if (TextUtils.isEmpty(appKey) || TextUtils.isEmpty(appID)) {
            environmentAvailable = false;
            errorMessage = TextUtils.isEmpty(errorMessage) ? "appKey or appID must not null" : errorMessage + " & appKey or appID must not null";
        }

        if (!environmentAvailable && !TextUtils.isEmpty(errorMessage)) {
            if (null != sdkInitializeListener) {
                currentState = MBSDKInitializeState.SDK_STATE_INITIALIZE_FAILURE;
                sdkInitializeListener.onInitializeFailure(errorMessage);
            }
        }
        return environmentAvailable;
    }

    /**
     * SDK 初始化的监听
     */
    public interface MBSDKInitializeListener {
        /**
         * SDK 初始化成功回调
         *
         * @param appKey 用户配置的 appKey
         * @param appID  用户配置的 appID
         */
        void onInitializeSuccess(String appKey, String appID);

        /**
         * SDK 初始化失败回调
         *
         * @param message 错误信息
         */
        void onInitializeFailure(String message);
    }

    public enum MBSDKInitializeState {
        /**
         * 尚未初始化
         */
        SDK_STATE_UN_INITIALIZE,
        /**
         * 正在初始化
         */
        SDK_STATE_INITIALIZING,
        /**
         * 初始化成功
         */
        SDK_STATE_INITIALIZE_SUCCESS,
        /**
         * 初始化失败
         */
        SDK_STATE_INITIALIZE_FAILURE
    }

    /**
     * create MBRewardVideoHandler
     *
     * @param context
     * @param placementID
     * @param unitID
     * @return
     */
    public synchronized MBRewardVideoHandler createRewardVideoHandler(Context context, String placementID, String unitID) {

        if (rewardVideoHandlerMap == null) {
            rewardVideoHandlerMap = new HashMap<>();
        }

        MBRewardVideoHandler rewardVideoHandler = null;
        if (!TextUtils.isEmpty(unitID)) {
            if (rewardVideoHandlerMap.containsKey(unitID)) {
                rewardVideoHandler = rewardVideoHandlerMap.get(unitID);
            } else {
                rewardVideoHandler = new MBRewardVideoHandler(context, placementID, unitID);
                rewardVideoHandlerMap.put(unitID, rewardVideoHandler);
            }
        }
        return rewardVideoHandler;
    }

    /**
     * create MBRewardVideoHandler
     *
     * @param placementID
     * @param unitID
     * @return
     */
    public synchronized MBRewardVideoHandler createRewardVideoHandler(String placementID, String unitID) {
        return createRewardVideoHandler(null, placementID, unitID);
    }

    /**
     * release MBRewardVideoHandler
     *
     * @param unitIDs
     */
    public synchronized void releaseRewardVideoHandler(String... unitIDs) {
        if (rewardVideoHandlerMap != null && unitIDs != null && unitIDs.length > 0) {
            for (String unitID : unitIDs) {
                if (!TextUtils.isEmpty(unitID)) {
                    rewardVideoHandlerMap.remove(unitID);
                }
            }
        }
    }

    /**
     * create MBInterstitialVideoHandler
     *
     * @param context
     * @param placementID
     * @param unitID
     * @return
     */
    public synchronized MBInterstitialVideoHandler createInterstitialVideoHandler(Context context, String placementID, String unitID) {
        if (interstitialVideoHandlerMap == null) {
            interstitialVideoHandlerMap = new HashMap<String, MBInterstitialVideoHandler>();
        }

        MBInterstitialVideoHandler interstitialVideoHandler = null;
        if (interstitialVideoHandlerMap.containsKey(unitID)) {
            interstitialVideoHandler = interstitialVideoHandlerMap.get(unitID);
        } else {
            interstitialVideoHandler = new MBInterstitialVideoHandler(context, placementID, unitID);
            interstitialVideoHandlerMap.put(unitID, interstitialVideoHandler);
        }
        return interstitialVideoHandler;
    }

    /**
     * create MBInterstitialVideoHandler
     *
     * @param placementID
     * @param unitID
     * @return
     */
    public synchronized MBInterstitialVideoHandler createInterstitialVideoHandler(String placementID, String unitID) {
        return createInterstitialVideoHandler(null, placementID, unitID);
    }

    /**
     * release MBInterstitialVideoHandler
     *
     * @param unitIDs
     */
    public synchronized void releaseInterstitialVideoHandler(String... unitIDs) {
        if (interstitialVideoHandlerMap != null && unitIDs != null && unitIDs.length > 0) {
            for (String unitID : unitIDs) {
                if (!TextUtils.isEmpty(unitID)) {
                    interstitialVideoHandlerMap.remove(unitID);
                }
            }
        }
    }
}
