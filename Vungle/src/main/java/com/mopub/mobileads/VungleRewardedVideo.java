package com.mopub.mobileads;

import android.app.Activity;
import android.text.TextUtils;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.DataKeys;
import com.mopub.common.LifecycleListener;
import com.mopub.common.MoPubReward;
import com.mopub.common.logging.MoPubLog;
import com.vungle.warren.AdConfig;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOULD_REWARD;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;

/**
 * A custom event for showing Vungle rewarded videos.
 */

@Keep
public class VungleRewardedVideo extends CustomEventRewardedVideo {

    /*
     * These constants are intended for MoPub internal use. Do not modify.
     */
    private static final String APP_ID_KEY = "appId";
    private static final String PLACEMENT_ID_KEY = "pid";

    private static final String VUNGLE_NETWORK_ID_DEFAULT = "vngl_id";
    private static final String VUNGLE_DEFAULT_APP_ID = "YOUR_APP_ID_HERE";

    private static final String ADAPTER_NAME = VungleRewardedVideo.class.getSimpleName();

    private static VungleRouter sVungleRouter;
    private VungleRewardedRouterListener mVungleRewardedRouterListener;
    @NonNull
    private VungleAdapterConfiguration mVungleAdapterConfiguration;
    private static boolean sInitialized;
    private String mAppId;
    @NonNull
    private String mPlacementId = VUNGLE_NETWORK_ID_DEFAULT;
    private boolean mIsPlaying;

    private String mAdUnitId;
    private String mCustomerId;

    public VungleRewardedVideo() {
        sVungleRouter = VungleRouter.getInstance();

        if (mVungleRewardedRouterListener == null) {
            mVungleRewardedRouterListener = new VungleRewardedRouterListener();
        }
        mVungleAdapterConfiguration = new VungleAdapterConfiguration();
    }

    @Nullable
    @Override
    public LifecycleListener getLifecycleListener() {
        return sVungleRouter.getLifecycleListener();
    }

    @NonNull
    @Override
    protected String getAdNetworkId() {
        return mPlacementId;
    }

    @Override
    protected boolean checkAndInitializeSdk(@NonNull final Activity launcherActivity,
                                            @NonNull final Map<String, Object> localExtras,
                                            @NonNull final Map<String, String> serverExtras) throws Exception {
        synchronized (VungleRewardedVideo.class) {
            if (sInitialized) {
                return false;
            }

            if (!validateIdsInServerExtras(serverExtras)) {
                mAppId = VUNGLE_DEFAULT_APP_ID;
            }

            if (!sVungleRouter.isVungleInitialized()) {
                // No longer passing the placement IDs (pids) param per Vungle 6.3.17
                sVungleRouter.initVungle(launcherActivity, mAppId);
                mVungleAdapterConfiguration.setCachedInitializationParameters(launcherActivity, serverExtras);
            }

            sInitialized = true;

            return true;
        }
    }

    @Override
    protected void loadWithSdkInitialized(@NonNull final Activity activity, @NonNull final Map<String, Object> localExtras, @NonNull final Map<String, String> serverExtras) throws Exception {
        mIsPlaying = false;

        if (!validateIdsInServerExtras(serverExtras)) {
            MoPubRewardedVideoManager.onRewardedVideoLoadFailure(VungleRewardedVideo.class, mPlacementId, MoPubErrorCode.NETWORK_NO_FILL);

            MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                    MoPubErrorCode.NETWORK_NO_FILL);
            return;
        }

        Object adUnitObject = localExtras.get(DataKeys.AD_UNIT_ID_KEY);
        if (adUnitObject instanceof String) {
            mAdUnitId = (String) adUnitObject;
        }

        Object customerIdObject = localExtras.get(DataKeys.REWARDED_AD_CUSTOMER_ID_KEY);
        if (customerIdObject instanceof String && !TextUtils.isEmpty((String) customerIdObject)) {
            mCustomerId = (String) customerIdObject;
        }

        if (sVungleRouter.isVungleInitialized()) {
            if (sVungleRouter.isValidPlacement(mPlacementId)) {
                sVungleRouter.loadAdForPlacement(mPlacementId, mVungleRewardedRouterListener);
            } else {
                MoPubLog.log(getAdNetworkId(), CUSTOM, "Invalid or Inactive Placement ID: " + mPlacementId);
                MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Invalid or Inactive Placement ID: " + mPlacementId);
                MoPubRewardedVideoManager.onRewardedVideoLoadFailure(VungleRewardedVideo.class, mPlacementId, MoPubErrorCode.NETWORK_NO_FILL);

                MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                        MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                        MoPubErrorCode.NETWORK_NO_FILL);
            }
            MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);
        } else {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "There should not be this case. loadWithSdkInitialized is " +
                    "called before the SDK starts initialization for Placement ID: " + mPlacementId);
            MoPubRewardedVideoManager.onRewardedVideoLoadFailure(VungleRewardedVideo.class, mPlacementId, MoPubErrorCode.NETWORK_NO_FILL);

            MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                    MoPubErrorCode.NETWORK_NO_FILL);
        }
    }

    @Override
    protected boolean hasVideoAvailable() {
        return sVungleRouter.isAdPlayableForPlacement(mPlacementId);
    }

    @Override
    protected void showVideo() {
        MoPubLog.log(getAdNetworkId(), SHOW_ATTEMPTED, ADAPTER_NAME);

        final AdConfig adConfig = new AdConfig();
        setUpMediationSettingsForRequest(adConfig);

        sVungleRouter.playAdForPlacement(mPlacementId, adConfig);
        mIsPlaying = true;
    }

    @Override
    protected void onInvalidate() {
        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "onInvalidate is called for Placement ID:" + mPlacementId);

        sVungleRouter.removeRouterListener(mPlacementId);
        mVungleRewardedRouterListener = null;
    }

    //private functions
    private boolean validateIdsInServerExtras(Map<String, String> serverExtras) {
        boolean isAllDataValid = true;

        if (serverExtras.containsKey(APP_ID_KEY)) {
            mAppId = serverExtras.get(APP_ID_KEY);
            if (mAppId != null && mAppId.isEmpty()) {
                MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "App ID is empty.");
                isAllDataValid = false;
            }
        } else {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "AppID is not in serverExtras.");
            isAllDataValid = false;
        }

        if (serverExtras.containsKey(PLACEMENT_ID_KEY)) {
            mPlacementId = serverExtras.get(PLACEMENT_ID_KEY);
            if (mPlacementId != null && mPlacementId.isEmpty()) {
                MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Placement ID for this Ad Unit is empty.");
                isAllDataValid = false;
            }
        } else {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Placement ID for this Ad Unit is not in serverExtras.");
            isAllDataValid = false;
        }

        return isAllDataValid;
    }

    private void setUpMediationSettingsForRequest(AdConfig adConfig) {
        VungleMediationConfiguration globalMediationSettings =
                MoPubRewardedVideoManager.getGlobalMediationSettings(VungleMediationConfiguration.class);
        if (globalMediationSettings == null) {
            // pubs are using older adapter version
            globalMediationSettings = MoPubRewardedVideoManager.getGlobalMediationSettings(VungleMediationSettings.class);
        }

        VungleMediationConfiguration instanceMediationSettings =
                MoPubRewardedVideoManager.getInstanceMediationSettings(VungleMediationConfiguration.class, mAdUnitId);
        if (instanceMediationSettings == null) {
            instanceMediationSettings = MoPubRewardedVideoManager.getInstanceMediationSettings(VungleMediationSettings.class, mAdUnitId);
        }
        // Local options override global options.
        if (instanceMediationSettings != null) {
            modifyAdConfig(adConfig, instanceMediationSettings);
        } else if (globalMediationSettings != null) {
            modifyAdConfig(adConfig, globalMediationSettings);
        }
    }

    private void modifyAdConfig(AdConfig adConfig, VungleMediationConfiguration mediationSettings) {
        String userId = null;

        if (!TextUtils.isEmpty(mCustomerId)) {
            userId = mCustomerId;
        } else if (!TextUtils.isEmpty(mediationSettings.getUserId())) {
            userId = mediationSettings.getUserId();
        }
        sVungleRouter.setIncentivizedFields(userId, mediationSettings.getTitle(), mediationSettings.getBody(),
                mediationSettings.getKeepWatchingButtonText(), mediationSettings.getCloseButtonText());
        adConfig.setMuted(mediationSettings.isStartMuted());
        adConfig.setFlexViewCloseTime(mediationSettings.getFlexViewCloseTimeInSec());
        adConfig.setOrdinal(mediationSettings.getOrdinalViewCount());
        adConfig.setAdOrientation(mediationSettings.getAdOrientation());
    }

    /*
     * VungleRewardedRouterListener
     */
    private class VungleRewardedRouterListener implements VungleRouterListener {
        @Override
        public void onAdEnd(@NonNull String placementReferenceId, final boolean wasSuccessfulView, final boolean wasCallToActionClicked) {
            if (mPlacementId.equals(placementReferenceId)) {
                MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "onAdEnd - Placement ID: " + placementReferenceId +
                        ", wasSuccessfulView: " + wasSuccessfulView + ", wasCallToActionClicked: "
                        + wasCallToActionClicked);

                mIsPlaying = false;

                if (wasSuccessfulView) {
                    MoPubLog.log(getAdNetworkId(), SHOULD_REWARD, ADAPTER_NAME, MoPubReward.NO_REWARD_AMOUNT, MoPubReward.NO_REWARD_LABEL);

                    // Vungle does not provide a callback when a user should be rewarded.
                    // You will need to provide your own reward logic if you receive a reward with
                    // "NO_REWARD_LABEL" && "NO_REWARD_AMOUNT"
                    MoPubRewardedVideoManager.onRewardedVideoCompleted(VungleRewardedVideo.class,
                            mPlacementId,
                            MoPubReward.success(MoPubReward.NO_REWARD_LABEL,
                                    MoPubReward.NO_REWARD_AMOUNT));
                }

                if (wasCallToActionClicked) {
                    MoPubRewardedVideoManager.onRewardedVideoClicked(VungleRewardedVideo.class,
                            mPlacementId);

                    MoPubLog.log(getAdNetworkId(), CLICKED, ADAPTER_NAME);
                }

                MoPubRewardedVideoManager.onRewardedVideoClosed(VungleRewardedVideo.class,
                        mPlacementId);

                sVungleRouter.removeRouterListener(mPlacementId);
            }
        }

        @Override
        public void onAdStart(@NonNull String placementReferenceId) {
            if (mPlacementId.equals(placementReferenceId)) {

                MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "onAdStart - Placement ID: " + placementReferenceId);

                mIsPlaying = true;

                MoPubRewardedVideoManager.onRewardedVideoStarted(VungleRewardedVideo.class,
                        mPlacementId);

                MoPubLog.log(getAdNetworkId(), SHOW_SUCCESS, ADAPTER_NAME);
            }
        }

        @Override
        public void onUnableToPlayAd(@NonNull String placementReferenceId, String reason) {
            if (mPlacementId.equals(placementReferenceId)) {
                MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "onUnableToPlayAd - Placement ID: " +
                        placementReferenceId + ", reason: " + reason);

                mIsPlaying = false;
                MoPubRewardedVideoManager.onRewardedVideoPlaybackError(VungleRewardedVideo.class,
                        mPlacementId, MoPubErrorCode.VIDEO_PLAYBACK_ERROR);

                MoPubLog.log(getAdNetworkId(), LOAD_FAILED, MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                        MoPubErrorCode.NETWORK_NO_FILL);
            }
        }

        @Override
        public void onAdAvailabilityUpdate(@NonNull String placementReferenceId, boolean isAdAvailable) {
            if (mPlacementId.equals(placementReferenceId)) {
                if (!mIsPlaying) {
                    if (isAdAvailable) {
                        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "rewarded video ad successfully loaded - " +
                                "Placement ID: " + placementReferenceId);
                        MoPubRewardedVideoManager.onRewardedVideoLoadSuccess(VungleRewardedVideo.class,
                                mPlacementId);

                        MoPubLog.log(getAdNetworkId(), LOAD_SUCCESS, ADAPTER_NAME);
                    } else {
                        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "rewarded video ad is not loaded - " +
                                "Placement ID: " + placementReferenceId);
                        MoPubRewardedVideoManager.onRewardedVideoLoadFailure(VungleRewardedVideo.class,
                                mPlacementId, MoPubErrorCode.NETWORK_NO_FILL);

                        MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                                MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                                MoPubErrorCode.NETWORK_NO_FILL);
                    }
                }
            }
        }
    }

    /**
     * @see com.mopub.mobileads.VungleMediationConfiguration
     * @deprecated
     */
    @Deprecated
    public static class VungleMediationSettings extends VungleMediationConfiguration {

        public static class Builder extends VungleMediationConfiguration.Builder {

            @Override
            public Builder withUserId(@NonNull final String userId) {
                super.withUserId(userId);
                return this;
            }

            @Override
            public Builder withCancelDialogTitle(@NonNull final String title) {
                super.withCancelDialogTitle(title);
                return this;
            }

            @Override
            public Builder withCancelDialogBody(@NonNull final String body) {
                super.withCancelDialogBody(body);
                return this;
            }

            @Override
            public Builder withCancelDialogCloseButton(@NonNull final String buttonText) {
                super.withCancelDialogCloseButton(buttonText);
                return this;
            }

            @Override
            public Builder withCancelDialogKeepWatchingButton(@NonNull final String buttonText) {
                super.withCancelDialogKeepWatchingButton(buttonText);
                return this;
            }

            @Deprecated
            @Override
            public Builder withSoundEnabled(boolean isSoundEnabled) {
                super.withSoundEnabled(isSoundEnabled);
                return this;
            }

            @Override
            public Builder withStartMuted(boolean isStartMuted) {
                super.withStartMuted(isStartMuted);
                return this;
            }

            @Override
            public Builder withFlexViewCloseTimeInSec(int flexViewCloseTimeInSec) {
                super.withFlexViewCloseTimeInSec(flexViewCloseTimeInSec);
                return this;
            }

            @Override
            public Builder withOrdinalViewCount(int ordinalViewCount) {
                super.withOrdinalViewCount(ordinalViewCount);
                return this;
            }

            @Override
            public Builder withAutoRotate(@AdConfig.Orientation int adOrientation) {
                super.withAutoRotate(adOrientation);
                return this;
            }

            @Override
            public VungleMediationSettings build() {
                return new VungleMediationSettings(this);
            }
        }

        private VungleMediationSettings(@NonNull final Builder builder) {
            super(builder);
        }
    }
}
