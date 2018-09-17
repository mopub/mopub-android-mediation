package com.mopub.mobileads;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.ironsource.mediationsdk.IronSource;

import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.model.Placement;
import com.ironsource.mediationsdk.sdk.ISDemandOnlyRewardedVideoListener;
import com.mopub.common.BaseLifecycleListener;
import com.mopub.common.LifecycleListener;
import com.mopub.common.MoPub;
import com.mopub.common.MoPubReward;
import com.mopub.common.logging.MoPubLog;

import java.util.Map;

import static com.mopub.mobileads.MoPubRewardedVideoManager.onRewardedVideoClicked;
import static com.mopub.mobileads.MoPubRewardedVideoManager.onRewardedVideoClosed;
import static com.mopub.mobileads.MoPubRewardedVideoManager.onRewardedVideoCompleted;
import static com.mopub.mobileads.MoPubRewardedVideoManager.onRewardedVideoLoadFailure;
import static com.mopub.mobileads.MoPubRewardedVideoManager.onRewardedVideoLoadSuccess;
import static com.mopub.mobileads.MoPubRewardedVideoManager.onRewardedVideoPlaybackError;
import static com.mopub.mobileads.MoPubRewardedVideoManager.onRewardedVideoStarted;

public class IronSourceRewardedVideo extends CustomEventRewardedVideo implements ISDemandOnlyRewardedVideoListener {

    /**
     * private vars
     */

    // Configuration keys
    private static final String APPLICATION_KEY = "applicationKey";
    private static final String PLACEMENT_KEY = "placementName";
    private static final String INSTANCE_ID_KEY = "instanceId";
    private static final String MEDIATION_TYPE = "mopub";
    private static final String ADAPTER_VERSION = "300";


    // This is the instance id used inside ironSource SDK
    @NonNull
    private String mInstanceId = "0";
    // This is the placement name used inside ironSource SDK
    private String mPlacementName = null;
    // Indicates if IronSource RV adapter is in its first init flow
    private static boolean mIsFirstInitFlow = true;

    /**
     * Mopub API
     */

    @Nullable
    @Override
    protected LifecycleListener getLifecycleListener() {
        return mLifecycleListener;
    }

    private LifecycleListener mLifecycleListener = new BaseLifecycleListener() {
        @Override
        public void onPause(@NonNull Activity activity) {
            super.onPause(activity);
            IronSource.onPause(activity);
        }

        @Override
        public void onResume(@NonNull Activity activity) {
            super.onResume(activity);
            IronSource.onResume(activity);
        }
    };

    @Override
    protected void onInvalidate() {
    }

    @NonNull
    @Override
    protected String getAdNetworkId() {
        return mInstanceId;
    }

    @Override
    protected boolean checkAndInitializeSdk(@NonNull Activity launcherActivity, @NonNull Map<String, Object> localExtras, @NonNull Map<String, String> serverExtras) throws Exception {

        // Pass the user consent from the MoPub SDK to ironSource as per GDPR
        boolean canCollectPersonalInfo = MoPub.canCollectPersonalInformation();
        IronSource.setConsent(canCollectPersonalInfo);

        try {
            String applicationKey = "";
            if (serverExtras.get(APPLICATION_KEY) != null) {
                applicationKey = serverExtras.get(APPLICATION_KEY);
            }
            setCredentials(serverExtras);
            initIronSourceSDK(launcherActivity, applicationKey);

            return mIsFirstInitFlow;
        } catch (Exception e) {
            MoPubLog.d(e.toString());
            return false;
        }
    }

    @Override
    protected void loadWithSdkInitialized(@NonNull Activity activity, @NonNull Map<String, Object> localExtras, @NonNull Map<String, String> serverExtras) throws Exception {

        setCredentials(serverExtras);

        if (!mIsFirstInitFlow) {
            if (hasVideoAvailable()) {
                onRewardedVideoLoadSuccess(IronSourceRewardedVideo.class, mInstanceId);
                MoPubLog.d("IronSource Rewarded Video loaded successfully for instance " + mInstanceId);
            } else {
                onRewardedVideoLoadFailure(IronSourceRewardedVideo.class, mInstanceId, MoPubErrorCode.NETWORK_NO_FILL);
                MoPubLog.d("IronSource Rewarded Video failed to load for instance " + mInstanceId);
            }
        }
    }

    @Override
    protected boolean hasVideoAvailable() {
        return IronSource.isISDemandOnlyRewardedVideoAvailable(mInstanceId);
    }

    @Override
    protected boolean isReady() {
        return IronSource.isISDemandOnlyRewardedVideoAvailable(mInstanceId);
    }

    @Override
    protected void showVideo() {
        if (TextUtils.isEmpty(mPlacementName)) {
            IronSource.showISDemandOnlyRewardedVideo(mInstanceId);
        } else {
            IronSource.showISDemandOnlyRewardedVideo(mInstanceId, mPlacementName);
        }
    }

    @Override
    protected void show() {
        if (TextUtils.isEmpty(mPlacementName)) {
            IronSource.showISDemandOnlyRewardedVideo(mInstanceId);
        } else {
            IronSource.showISDemandOnlyRewardedVideo(mInstanceId, mPlacementName);
        }
    }

    /**
     * Class Helper Methods
     **/
    private void initIronSourceSDK(Activity activity, String appKey) {

        if (!TextUtils.isEmpty(appKey)) {
            IronSource.setISDemandOnlyRewardedVideoListener(this);

            if (mIsFirstInitFlow) {
                MoPubLog.d("IronSource initialization succeeded for RewardedVideo");
                IronSource.setMediationType(MEDIATION_TYPE + ADAPTER_VERSION);
                IronSource.initISDemandOnly(activity, appKey, IronSource.AD_UNIT.REWARDED_VIDEO);
            }
        } else {
            MoPubLog.d("IronSource initialization Failed, make sure that 'applicationKey' server parameter is added");
            onRewardedVideoLoadFailure(IronSourceRewardedVideo.class, mInstanceId,
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
        }
    }

    private void setCredentials(Map<String, String> serverExtras) {

        if (serverExtras.get(PLACEMENT_KEY) != null) {
            mPlacementName = serverExtras.get(PLACEMENT_KEY);
        }

        if (serverExtras.get(INSTANCE_ID_KEY) != null) {
            if (!TextUtils.isEmpty(serverExtras.get(INSTANCE_ID_KEY))) {
                mInstanceId = serverExtras.get(INSTANCE_ID_KEY);
            }
        }
    }

    private MoPubErrorCode getMoPubErrorMessage(IronSourceError ironSourceError) {
        if (ironSourceError == null) {
            return MoPubErrorCode.INTERNAL_ERROR;
        }

        switch (ironSourceError.getErrorCode()) {
            case IronSourceError.ERROR_CODE_NO_CONFIGURATION_AVAILABLE:
            case IronSourceError.ERROR_CODE_KEY_NOT_SET:
            case IronSourceError.ERROR_CODE_INVALID_KEY_VALUE:
            case IronSourceError.ERROR_CODE_INIT_FAILED:
                return MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR;
            case IronSourceError.ERROR_CODE_USING_CACHED_CONFIGURATION:
                return MoPubErrorCode.VIDEO_CACHE_ERROR;
            case IronSourceError.ERROR_CODE_NO_ADS_TO_SHOW:
                return MoPubErrorCode.NETWORK_NO_FILL;
            case IronSourceError.ERROR_CODE_GENERIC:
                return MoPubErrorCode.INTERNAL_ERROR;
            case IronSourceError.ERROR_NO_INTERNET_CONNECTION:
                return MoPubErrorCode.NO_CONNECTION;
            default:
                return MoPubErrorCode.UNSPECIFIED;
        }
    }

    /**
     * IronSource RewardedVideo Listener
     **/

    //Invoked when there is a change in the ad availability status.
    @Override
    public void onRewardedVideoAvailabilityChanged(String instanceId, boolean available) {
        MoPubLog.d("IronSource Rewarded Video changed availability: " + available + " for instance " + mInstanceId);

        // Ignore callback
        if (!mInstanceId.equals(instanceId)) {
            return;
        }

        // Invoke only for first load, ignore for all others and rely on 'hasAdAvailable'
        if (mIsFirstInitFlow) {
            if (available) {
                onRewardedVideoLoadSuccess(IronSourceRewardedVideo.class, mInstanceId);
                MoPubLog.d("IronSource Rewarded Video loaded successfully for instance " + mInstanceId);
            } else {
                onRewardedVideoLoadFailure(IronSourceRewardedVideo.class, mInstanceId, MoPubErrorCode.NETWORK_NO_FILL);
                MoPubLog.d("IronSource Rewarded Video failed to load for instance " + mInstanceId);
            }
            mIsFirstInitFlow = false;
        }
    }

    //Invoked when the RewardedVideo ad view has opened.
    @Override
    public void onRewardedVideoAdOpened(String instanceId) {
        MoPubLog.d("IronSource Rewarded Video opened ad for instance " + instanceId);
        onRewardedVideoStarted(IronSourceRewardedVideo.class, instanceId);
    }

    //Invoked when the user is about to return to the application after closing the RewardedVideo ad.
    @Override
    public void onRewardedVideoAdClosed(String instanceId) {
        MoPubLog.d("IronSource Rewarded Video closed ad for instance " + instanceId);
        onRewardedVideoClosed(IronSourceRewardedVideo.class, instanceId);
    }

    //Invoked when the user completed the video and should be rewarded.
    @Override
    public void onRewardedVideoAdRewarded(String instanceId, Placement placement) {
        MoPubLog.d("IronSource Rewarded Video received reward for instance " + instanceId);

        //Placement can return null if the placementName is not valid.
        if (placement != null) {
            String rewardName = placement.getRewardName();
            int rewardAmount = placement.getRewardAmount();
            MoPubReward reward = MoPubReward.success(rewardName, rewardAmount);
            onRewardedVideoCompleted(IronSourceRewardedVideo.class, instanceId, reward);
        }
    }

    //Invoked when an Ad failed to display.
    @Override
    public void onRewardedVideoAdShowFailed(String instanceId, IronSourceError ironSourceError) {
        MoPubLog.d("IronSource Rewarded Video failed to show for instance " + instanceId);
        onRewardedVideoPlaybackError(IronSourceRewardedVideo.class, instanceId, getMoPubErrorMessage(ironSourceError));
    }

    //Invoked when the video ad was clicked by the user.
    @Override
    public void onRewardedVideoAdClicked(String instanceId, Placement placement) {
        MoPubLog.d("IronSource Rewarded Video clicked for instance " + instanceId);
        onRewardedVideoClicked(IronSourceRewardedVideo.class, instanceId);
    }
}