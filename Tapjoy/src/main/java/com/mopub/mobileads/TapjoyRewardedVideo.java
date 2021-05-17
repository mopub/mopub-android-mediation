package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
import com.tapjoy.Tapjoy;
import com.tapjoy.TJPrivacyPolicy;

import org.json.JSONException;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOULD_REWARD;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;

public class TapjoyRewardedVideo extends BaseAd {
    private static final String TJC_MOPUB_NETWORK_CONSTANT = "mopub";
    private static final String TJC_MOPUB_ADAPTER_VERSION_NUMBER = "4.1.0";
    private static final String TAPJOY_AD_NETWORK_CONSTANT = "tapjoy_id";

    // Configuration keys
    private static final String SDK_KEY = "sdkKey";
    private static final String DEBUG_ENABLED = "debugEnabled";
    private static final String PLACEMENT_NAME = "name";
    private static final String ADAPTER_NAME = TapjoyRewardedVideo.class.getSimpleName();
    private static final String AD_MARKUP_KEY = "adm";

    private String sdkKey;
    private Hashtable<String, Object> connectFlags;
    private TJPlacement tjPlacement;
    private boolean isAutoConnect = false;
    private String mPlacementName;
    private TapjoyRewardedVideoListener mTapjoyListener = new TapjoyRewardedVideoListener();
    private static TJPrivacyPolicy tjPrivacyPolicy;
	
    @NonNull
    private TapjoyAdapterConfiguration mTapjoyAdapterConfiguration;


    static {
       MoPubLog.log(CUSTOM, "Class initialized with network adapter version", TJC_MOPUB_ADAPTER_VERSION_NUMBER);
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
        tjPrivacyPolicy = Tapjoy.getPrivacyPolicy();
    }

    @Override
    protected boolean checkAndInitializeSdk(@NonNull final Activity launcherActivity,
                                            @NonNull final AdData adData) {

        final Map<String, String> extras = adData.getExtras();
        mPlacementName = extras.get(PLACEMENT_NAME);
        if (TextUtils.isEmpty(mPlacementName)) {
            MoPubLog.log(mPlacementName, CUSTOM, ADAPTER_NAME, "Tapjoy rewarded video loaded with empty 'name' field. Request will fail.");
            if (mLoadListener != null) {
                mLoadListener.onAdLoadFailed(MoPubErrorCode.NETWORK_NO_FILL);
            }
            MoPubLog.log(mPlacementName, LOAD_FAILED, ADAPTER_NAME, MoPubErrorCode.NETWORK_NO_FILL.getIntCode(), MoPubErrorCode.NETWORK_NO_FILL);
            throw new IllegalStateException("Tapjoy failed to load. No Placement name.");
        }

        final String adMarkup = extras.get(AD_MARKUP_KEY);
        if (!Tapjoy.isConnected()) {
            setupWithMediationSetting();

            mTapjoyAdapterConfiguration.setCachedInitializationParameters(launcherActivity, extras);

            boolean enableDebug = Boolean.valueOf(extras.get(DEBUG_ENABLED));
            Tapjoy.setDebugEnabled(enableDebug);

            sdkKey = extras.get(SDK_KEY);
            if (!TextUtils.isEmpty(sdkKey)) {
                MoPubLog.log(mPlacementName, CUSTOM, ADAPTER_NAME, "Connecting to Tapjoy via MoPub dashboard settings...");
                connectToTapjoy(launcherActivity, adMarkup);

                isAutoConnect = true;
                return true;
            } else {
                MoPubLog.log(mPlacementName, CUSTOM, ADAPTER_NAME, "Tapjoy rewarded video is initialized with empty 'sdkKey'. You must call Tapjoy.connect()");
                isAutoConnect = false;
            }
        }
        return false;
    }

    @Override
    protected void load(@NonNull final Context context, @NonNull final AdData adData) {
        fetchMoPubGDPRSettings();
        setAutomaticImpressionAndClickTracking(false);
        final String adMarkup = adData.getExtras().get(AD_MARKUP_KEY);
        createPlacement((Activity) context, adMarkup);
    }

    private void connectToTapjoy(final Activity launcherActivity, final String adMarkup) {
        Tapjoy.connect(launcherActivity, sdkKey, connectFlags, new TJConnectListener() {
            @Override
            public void onConnectSuccess() {
                MoPubLog.log(mPlacementName, CUSTOM, ADAPTER_NAME, "Tapjoy connected successfully");
                createPlacement(launcherActivity, adMarkup);
            }

            @Override
            public void onConnectFailure() {
                MoPubLog.log(mPlacementName, CUSTOM, ADAPTER_NAME, "Tapjoy connect failed");
            }
        });
    }

    private void createPlacement(Activity activity, final String adm) {
        if (!TextUtils.isEmpty(mPlacementName)) {
            if (isAutoConnect && !Tapjoy.isConnected()) {
                // If adapter is making the Tapjoy.connect() call on behalf of the pub, wait for it to
                // succeed before making a placement request.
                MoPubLog.log(mPlacementName, CUSTOM, ADAPTER_NAME, "Tapjoy is still connecting. Please wait for this to finish before making a placement request");
                return;
            }

            tjPlacement = new TJPlacement(activity, mPlacementName, mTapjoyListener);
            tjPlacement.setMediationName(TJC_MOPUB_NETWORK_CONSTANT);
            tjPlacement.setAdapterVersion(TJC_MOPUB_ADAPTER_VERSION_NUMBER);

            if (!TextUtils.isEmpty(adm)) {
                try {
                    Map<String, String> auctionData = Json.jsonStringToMap(adm);
                    tjPlacement.setAuctionData(new HashMap<>(auctionData));
                } catch (JSONException e) {
                    MoPubLog.log(mPlacementName, CUSTOM, ADAPTER_NAME, "Unable to parse auction data.");
                }
            }
            tjPlacement.setVideoListener(mTapjoyListener);
            tjPlacement.requestContent();
            MoPubLog.log(mPlacementName, LOAD_ATTEMPTED, ADAPTER_NAME);
        } else {
            MoPubLog.log(mPlacementName, CUSTOM, ADAPTER_NAME, "Tapjoy placementName is empty. Unable to create TJPlacement.");
        }
    }

    private boolean hasVideoAvailable() {
        if (tjPlacement == null) {
            return false;
        }
        return tjPlacement.isContentAvailable();
    }

    @Override
    protected void show() {
        MoPubLog.log(mPlacementName, SHOW_ATTEMPTED, ADAPTER_NAME);
        if (hasVideoAvailable()) {
            tjPlacement.showContent();
        } else {
            MoPubLog.log(mPlacementName, SHOW_FAILED, ADAPTER_NAME, MoPubErrorCode.VIDEO_PLAYBACK_ERROR.getIntCode(), MoPubErrorCode.VIDEO_PLAYBACK_ERROR);
            if (mInteractionListener != null) {
                mInteractionListener.onAdFailed(MoPubErrorCode.VIDEO_PLAYBACK_ERROR);
            }
        }
    }

    private void setupWithMediationSetting() {
        final TapjoyMediationSettings globalMediationSettings =
                MoPubRewardedVideoManager.getGlobalMediationSettings(TapjoyMediationSettings.class);

        if (globalMediationSettings != null) {
            MoPubLog.log(mPlacementName, CUSTOM, ADAPTER_NAME, "Reading connectFlags from Tapjoy mediation settings");

            Map<String, Object> connectFlagsMediationSettings = globalMediationSettings.getConnectFlags();
            if (connectFlagsMediationSettings != null) {
                connectFlags = new Hashtable<>();
                connectFlags.putAll(connectFlagsMediationSettings);
            }
        }

    }

    // Pass the user consent from the MoPub SDK to Tapjoy as per GDPR
    private void fetchMoPubGDPRSettings() {

        PersonalInfoManager personalInfoManager = MoPub.getPersonalInformationManager();

        if (personalInfoManager != null) {
            Boolean gdprApplies = personalInfoManager.gdprApplies();

            if (gdprApplies != null) {
                tjPrivacyPolicy.setSubjectToGDPR(gdprApplies);

                if (gdprApplies) {
                    String userConsented = MoPub.canCollectPersonalInformation() ? "1" : "0";

                    tjPrivacyPolicy.setUserConsent(userConsented);
                } else {
                    tjPrivacyPolicy.setUserConsent("-1");
                }
            }
        }
    }

    private class TapjoyRewardedVideoListener implements TJPlacementListener, TJPlacementVideoListener {
        @Override
        public void onRequestSuccess(TJPlacement placement) {
            if (!placement.isContentAvailable()) {
                if (mLoadListener != null) {
                    mLoadListener.onAdLoadFailed(MoPubErrorCode.NETWORK_NO_FILL);
                }
                MoPubLog.log(mPlacementName, LOAD_FAILED, MoPubErrorCode.NETWORK_NO_FILL.getIntCode(), MoPubErrorCode.NETWORK_NO_FILL);
            }
        }

        @Override
        public void onContentReady(TJPlacement placement) {
            if (mLoadListener != null) {
                mLoadListener.onAdLoaded();
            }
            MoPubLog.log(mPlacementName, LOAD_SUCCESS, ADAPTER_NAME);
        }

        @Override
        public void onRequestFailure(TJPlacement placement, TJError error) {
            if (mLoadListener != null) {
                mLoadListener.onAdLoadFailed(MoPubErrorCode.NETWORK_NO_FILL);
            }
            MoPubLog.log(mPlacementName, LOAD_FAILED, ADAPTER_NAME, MoPubErrorCode.NETWORK_NO_FILL.getIntCode(), MoPubErrorCode.NETWORK_NO_FILL);
        }

        @Override
        public void onContentShow(TJPlacement placement) {
            if (mInteractionListener != null) {
                mInteractionListener.onAdShown();
                mInteractionListener.onAdImpression();
            }
            MoPubLog.log(mPlacementName, SHOW_SUCCESS, ADAPTER_NAME);
        }

        @Override
        public void onContentDismiss(TJPlacement placement) {
            if (mInteractionListener != null) {
                mInteractionListener.onAdDismissed();
            }
        }

        @Override
        public void onClick(TJPlacement placement) {
            MoPubLog.log(mPlacementName, CLICKED, ADAPTER_NAME);
            if (mInteractionListener != null) {
                mInteractionListener.onAdClicked();
            }
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
            MoPubLog.log(mPlacementName, CUSTOM, ADAPTER_NAME, "Tapjoy rewarded video started for placement " +
                    tjPlacement + ".");

        }

        @Override
        public void onVideoError(TJPlacement tjPlacement, String message) {
            MoPubLog.log(mPlacementName, CUSTOM, ADAPTER_NAME, "Tapjoy rewarded video failed for placement " +
                    tjPlacement + "with error" + message);
            if (mInteractionListener != null) {
                mInteractionListener.onAdFailed(MoPubErrorCode.VIDEO_PLAYBACK_ERROR);
            }
        }

        @Override
        public void onVideoComplete(TJPlacement tjPlacement) {
            MoPubLog.log(mPlacementName, CUSTOM, ADAPTER_NAME, "Tapjoy rewarded video completed");
            if (mInteractionListener != null) {
                mInteractionListener.onAdComplete(MoPubReward.success(MoPubReward.NO_REWARD_LABEL, MoPubReward.DEFAULT_REWARD_AMOUNT));
            }
            MoPubLog.log(mPlacementName, SHOULD_REWARD, ADAPTER_NAME, MoPubReward.NO_REWARD_AMOUNT, MoPubReward.DEFAULT_REWARD_AMOUNT);
        }
    }

    public static final class TapjoyMediationSettings implements MediationSettings {
        @Nullable
        Map<String, Object> connectFlags;

        public TapjoyMediationSettings() {
        }

        public TapjoyMediationSettings(@Nullable Map<String, Object> connectFlags) {
            this.connectFlags = connectFlags;
        }

        public void setConnectFlags(@Nullable Map<String, Object> connectFlags) {
            this.connectFlags = connectFlags;
        }

        @Nullable
        Map<String, Object> getConnectFlags() {
            return connectFlags;
        }
    }
}
