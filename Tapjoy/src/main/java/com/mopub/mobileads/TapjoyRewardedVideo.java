package com.mopub.mobileads;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.mopub.common.LifecycleListener;
import com.mopub.common.MediationSettings;
import com.mopub.common.MoPub;
import com.mopub.common.MoPubReward;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.privacy.PersonalInfoManager;
import com.mopub.common.util.Json;
import com.tapjoy.TJActionRequest;
import com.tapjoy.TJConnectListener;
import com.tapjoy.TJError;
import com.tapjoy.TJPlacement;
import com.tapjoy.TJPlacementListener;
import com.tapjoy.TJPlacementVideoListener;
import com.tapjoy.TJVideoListener;
import com.tapjoy.Tapjoy;
import com.tapjoy.TapjoyLog;

import org.json.JSONException;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOULD_REWARD;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

public class TapjoyRewardedVideo extends CustomEventRewardedVideo {
    private static final String TJC_MOPUB_NETWORK_CONSTANT = "mopub";
    private static final String TJC_MOPUB_ADAPTER_VERSION_NUMBER = "4.1.0";
    private static final String TAPJOY_AD_NETWORK_CONSTANT = "tapjoy_id";

    // Configuration keys
    private static final String SDK_KEY = "sdkKey";
    private static final String DEBUG_ENABLED = "debugEnabled";
    private static final String PLACEMENT_NAME = "name";
    private static final String ADAPTER_NAME = TapjoyRewardedVideo.class.getSimpleName();
    private static final String ADM_KEY = "adm";

    private String sdkKey;
    private String placementName;
    private Hashtable<String, Object> connectFlags;
    private TJPlacement tjPlacement;
    private boolean isAutoConnect = false;
    private static TapjoyRewardedVideoListener sTapjoyListener = new TapjoyRewardedVideoListener();
    @NonNull
    private TapjoyAdapterConfiguration mTapjoyAdapterConfiguration;

    static {
        TapjoyLog.i(ADAPTER_NAME, "Class initialized with network adapter version " + TJC_MOPUB_ADAPTER_VERSION_NUMBER);
    }

    @Override
    protected LifecycleListener getLifecycleListener() {
        return null;
    }

    @NonNull
    @Override
    protected String getAdNetworkId() {
        return TAPJOY_AD_NETWORK_CONSTANT;
    }

    @Override
    protected void onInvalidate() {
    }

    public TapjoyRewardedVideo() {
        mTapjoyAdapterConfiguration = new TapjoyAdapterConfiguration();
    }

    @Override
    protected boolean checkAndInitializeSdk(@NonNull Activity launcherActivity,
                                            @NonNull Map<String, Object> localExtras,
                                            @NonNull Map<String, String> serverExtras)
            throws Exception {

        placementName = serverExtras.get(PLACEMENT_NAME);
        if (TextUtils.isEmpty(placementName)) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Tapjoy rewarded video loaded with empty 'name' field. Request will fail.");
            MoPubRewardedVideoManager.onRewardedVideoLoadFailure(TapjoyRewardedVideo.class, TAPJOY_AD_NETWORK_CONSTANT, MoPubErrorCode.NETWORK_NO_FILL);
            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME, MoPubErrorCode.NETWORK_NO_FILL.getIntCode(), MoPubErrorCode.NETWORK_NO_FILL);
        }

        final String adm = serverExtras.get(ADM_KEY);
        if (!Tapjoy.isConnected()) {
            if (checkAndInitMediationSettings()) {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "Connecting to Tapjoy via MoPub mediation settings...");
                connectToTapjoy(launcherActivity, adm);
                mTapjoyAdapterConfiguration.setCachedInitializationParameters(launcherActivity, serverExtras);
                isAutoConnect = true;
                return true;
            } else {
                boolean enableDebug = Boolean.valueOf(serverExtras.get(DEBUG_ENABLED));
                Tapjoy.setDebugEnabled(enableDebug);

                sdkKey = serverExtras.get(SDK_KEY);
                if (!TextUtils.isEmpty(sdkKey)) {
                    MoPubLog.log(CUSTOM, ADAPTER_NAME, "Connecting to Tapjoy via MoPub dashboard settings...");
                    connectToTapjoy(launcherActivity, adm);

                    isAutoConnect = true;
                    return true;
                } else {
                    MoPubLog.log(CUSTOM, ADAPTER_NAME, "Tapjoy rewarded video is initialized with empty 'sdkKey'. You must call Tapjoy.connect()");
                    isAutoConnect = false;
                }
            }
        }

        return false;
    }

    @Override
    protected void loadWithSdkInitialized(@NonNull Activity activity,
                                          @NonNull Map<String, Object> localExtras,
                                          @NonNull Map<String, String> serverExtras)
            throws Exception {
        fetchMoPubGDPRSettings();
        final String adm = serverExtras.get(ADM_KEY);
        createPlacement(activity, adm);
    }

    private void connectToTapjoy(final Activity launcherActivity, final String adm) {
        Tapjoy.connect(launcherActivity, sdkKey, connectFlags, new TJConnectListener() {
            @Override
            public void onConnectSuccess() {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "Tapjoy connected successfully");
                createPlacement(launcherActivity, adm);
            }

            @Override
            public void onConnectFailure() {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "Tapjoy connect failed");
            }
        });
    }

    private void createPlacement(Activity activity, final String adm) {
        if (!TextUtils.isEmpty(placementName)) {
            if (isAutoConnect && !Tapjoy.isConnected()) {
                // If adapter is making the Tapjoy.connect() call on behalf of the pub, wait for it to
                // succeed before making a placement request.
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "Tapjoy is still connecting. Please wait for this to finish before making a placement request");
                return;
            }

            tjPlacement = new TJPlacement(activity, placementName, sTapjoyListener);
            tjPlacement.setMediationName(TJC_MOPUB_NETWORK_CONSTANT);
            tjPlacement.setAdapterVersion(TJC_MOPUB_ADAPTER_VERSION_NUMBER);

            if (!TextUtils.isEmpty(adm)) {
                try {
                    Map<String, String> auctionData = Json.jsonStringToMap(adm);
                    tjPlacement.setAuctionData(new HashMap<>(auctionData));
                } catch (JSONException e) {
                    MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unable to parse auction data.");
                }
            }
            tjPlacement.setVideoListener(sTapjoyListener);
            tjPlacement.requestContent();
            MoPubLog.log(placementName, LOAD_ATTEMPTED, ADAPTER_NAME);
        } else {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Tapjoy placementName is empty. Unable to create TJPlacement.");
        }
    }

    @Override
    protected boolean hasVideoAvailable() {
        if (tjPlacement == null) {
            return false;
        }
        return tjPlacement.isContentAvailable();
    }

    @Override
    protected void showVideo() {
        MoPubLog.log(SHOW_ATTEMPTED, ADAPTER_NAME);
        if (hasVideoAvailable()) {
            tjPlacement.showContent();
        } else {
            MoPubLog.log(SHOW_FAILED, ADAPTER_NAME, MoPubErrorCode.NETWORK_NO_FILL.getIntCode(), MoPubErrorCode.NETWORK_NO_FILL);
        }
    }

    private boolean checkAndInitMediationSettings() {
        final TapjoyMediationSettings globalMediationSettings =
                MoPubRewardedVideoManager.getGlobalMediationSettings(TapjoyMediationSettings.class);

        if (globalMediationSettings != null) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Initializing Tapjoy mediation settings");

            if (!TextUtils.isEmpty(globalMediationSettings.getSdkKey())) {
                sdkKey = globalMediationSettings.getSdkKey();
            } else {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "Cannot initialize Tapjoy -- 'sdkkey' is empty");
                return false;
            }

            if (globalMediationSettings.getConnectFlags() != null) {
                connectFlags = new Hashtable<>();
                connectFlags.putAll(globalMediationSettings.getConnectFlags());
            }

            return true;
        } else {
            return false;
        }
    }

    // Pass the user consent from the MoPub SDK to Tapjoy as per GDPR
    private void fetchMoPubGDPRSettings() {

        PersonalInfoManager personalInfoManager = MoPub.getPersonalInformationManager();

        if (personalInfoManager != null) {
            Boolean gdprApplies = personalInfoManager.gdprApplies();

            if (gdprApplies != null) {
                Tapjoy.subjectToGDPR(gdprApplies);

                if (gdprApplies) {
                    String userConsented = MoPub.canCollectPersonalInformation() ? "1" : "0";

                    Tapjoy.setUserConsent(userConsented);
                } else {
                    Tapjoy.setUserConsent("-1");
                }
            }
        }
    }

    private static class TapjoyRewardedVideoListener implements TJPlacementListener, CustomEventRewardedVideoListener, TJPlacementVideoListener {
        @Override
        public void onRequestSuccess(TJPlacement placement) {
            if (!placement.isContentAvailable()) {
                MoPubRewardedVideoManager.onRewardedVideoLoadFailure(TapjoyRewardedVideo.class, TAPJOY_AD_NETWORK_CONSTANT, MoPubErrorCode.NETWORK_NO_FILL);
                MoPubLog.log(LOAD_FAILED, MoPubErrorCode.NETWORK_NO_FILL.getIntCode(), MoPubErrorCode.NETWORK_NO_FILL);
            }
        }

        @Override
        public void onContentReady(TJPlacement placement) {
            MoPubRewardedVideoManager.onRewardedVideoLoadSuccess(TapjoyRewardedVideo.class, TAPJOY_AD_NETWORK_CONSTANT);
            MoPubLog.log(LOAD_SUCCESS, ADAPTER_NAME);
        }

        @Override
        public void onRequestFailure(TJPlacement placement, TJError error) {
            MoPubRewardedVideoManager.onRewardedVideoLoadFailure(TapjoyRewardedVideo.class, TAPJOY_AD_NETWORK_CONSTANT, MoPubErrorCode.NETWORK_NO_FILL);
            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME, MoPubErrorCode.NETWORK_NO_FILL.getIntCode(), MoPubErrorCode.NETWORK_NO_FILL);
        }

        @Override
        public void onContentShow(TJPlacement placement) {
            MoPubRewardedVideoManager.onRewardedVideoStarted(TapjoyRewardedVideo.class, TAPJOY_AD_NETWORK_CONSTANT);
            MoPubLog.log(SHOW_SUCCESS, ADAPTER_NAME);
        }

        @Override
        public void onContentDismiss(TJPlacement placement) {
            MoPubRewardedVideoManager.onRewardedVideoClosed(TapjoyRewardedVideo.class, TAPJOY_AD_NETWORK_CONSTANT);
        }

        @Override
        public void onClick(TJPlacement placement) {
            MoPubLog.log(CLICKED, ADAPTER_NAME);
            MoPubRewardedVideoManager.onRewardedVideoClicked(TapjoyRewardedVideo.class, TAPJOY_AD_NETWORK_CONSTANT);
        }

        @Override
        public void onPurchaseRequest(TJPlacement placement, TJActionRequest request,
                                      String productId) {
        }

        @Override
        public void onRewardRequest(TJPlacement placement, TJActionRequest request, String itemId,
                                    int quantity) {
        }

        @Override
        public void onVideoStart(TJPlacement tjPlacement) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Tapjoy rewarded video started for placement " +
                    tjPlacement + ".");

        }

        @Override
        public void onVideoError(TJPlacement tjPlacement, String message) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Tapjoy rewarded video failed for placement " +
                    tjPlacement + "with error" + message);

        }

        @Override
        public void onVideoComplete(TJPlacement tjPlacement) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Tapjoy rewarded video completed");
            MoPubRewardedVideoManager.onRewardedVideoCompleted(TapjoyRewardedVideo.class, TAPJOY_AD_NETWORK_CONSTANT, MoPubReward.success(MoPubReward.NO_REWARD_LABEL, MoPubReward.NO_REWARD_AMOUNT));
            MoPubLog.log(SHOULD_REWARD, ADAPTER_NAME, MoPubReward.NO_REWARD_AMOUNT, MoPubReward.NO_REWARD_LABEL);

        }
    }

    public static final class TapjoyMediationSettings implements MediationSettings {
        @Nullable
        private String sdkKey;
        @Nullable
        Map<String, Object> connectFlags;

        public TapjoyMediationSettings() {
        }

        public TapjoyMediationSettings(@Nullable String sdkKey) {
            this.sdkKey = sdkKey;
        }

        public TapjoyMediationSettings(@Nullable String sdkKey, @Nullable Map<String, Object> connectFlags) {
            this.sdkKey = sdkKey;
            this.connectFlags = connectFlags;
        }

        public void setSdkKey(@Nullable String sdkKey) {
            this.sdkKey = sdkKey;
        }

        public void setConnectFlags(@Nullable Map connectFlags) {
            this.connectFlags = connectFlags;
        }

        @Nullable
        String getSdkKey() {
            return sdkKey;
        }

        @Nullable
        Map<String, Object> getConnectFlags() {
            return connectFlags;
        }
    }
}
