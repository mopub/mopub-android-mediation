package com.mopub.mobileads;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.sdk.ISDemandOnlyRewardedVideoListener;
import com.mopub.common.BaseLifecycleListener;
import com.mopub.common.LifecycleListener;
import com.mopub.common.MoPub;
import com.mopub.common.MoPubReward;
import com.mopub.common.logging.MoPubLog;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM_WITH_THROWABLE;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOULD_REWARD;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;

public class IronSourceRewardedVideo extends CustomEventRewardedVideo implements ISDemandOnlyRewardedVideoListener {

    /**
     * private vars
     */

    // Configuration keys
    private static final String APPLICATION_KEY = "applicationKey";
    private static final String INSTANCE_ID_KEY = "instanceId";
    private static final String MEDIATION_TYPE = "mopub";
    private static final String ADAPTER_NAME = IronSourceRewardedVideo.class.getSimpleName();

    // This is the instance id used inside ironSource SDK
    @NonNull
    private String mInstanceId = IronSourceAdapterConfiguration.DEFAULT_INSTANCE_ID;

    @NonNull
    private IronSourceAdapterConfiguration mIronSourceAdapterConfiguration;

    /**
     * Mopub API
     */

    public IronSourceRewardedVideo() {
        mIronSourceAdapterConfiguration = new IronSourceAdapterConfiguration();
    }

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
        MoPubLog.log(CUSTOM, ADAPTER_NAME, "checkAndInitializeSdk");

        // Pass the user consent from the MoPub SDK to ironSource as per GDPR
        boolean canCollectPersonalInfo = MoPub.canCollectPersonalInformation();
        IronSource.setConsent(canCollectPersonalInfo);
        try {
            String applicationKey = "";
            if(serverExtras == null) {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "serverExtras is null. Make sure you have entered ironSource's application and instance keys on the MoPub dashboard");
                MoPubRewardedVideoManager.onRewardedVideoLoadFailure(IronSourceRewardedVideo.class, mInstanceId, MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);

                return false;
            }

            if(TextUtils.isEmpty(serverExtras.get(APPLICATION_KEY))){
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "IronSource didn't perform initRewardedVideo- null or empty appkey");
                MoPubRewardedVideoManager.onRewardedVideoLoadFailure(IronSourceRewardedVideo.class, mInstanceId,
                        MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);

                return false;
            }
            if (!TextUtils.isEmpty(serverExtras.get(INSTANCE_ID_KEY))) {
                mInstanceId = serverExtras.get(INSTANCE_ID_KEY);
            }

            applicationKey = serverExtras.get(APPLICATION_KEY);

            IronSource.setISDemandOnlyRewardedVideoListener(this);
            IronSource.setMediationType(MEDIATION_TYPE + IronSourceAdapterConfiguration.IRONSOURCE_ADAPTER_VERSION + "SDK" + IronSourceAdapterConfiguration.getMoPubSdkVersion());
            IronSource.initISDemandOnly(launcherActivity, applicationKey, IronSource.AD_UNIT.REWARDED_VIDEO);
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "IronSource initialization succeeded for RewardedVideo" + " (current instance: " + mInstanceId + " )");

            return true;
        } catch (Exception e) {
            MoPubLog.log(CUSTOM_WITH_THROWABLE, e);

            MoPubRewardedVideoManager.onRewardedVideoLoadFailure(IronSourceRewardedVideo.class, mInstanceId,
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);

            return false;
        }
    }

    @Override
    protected void loadWithSdkInitialized(@NonNull Activity activity, @NonNull Map<String, Object> localExtras, @NonNull Map<String, String> serverExtras) throws Exception {
        if (!TextUtils.isEmpty(serverExtras.get(INSTANCE_ID_KEY))) {
            mInstanceId = serverExtras.get(INSTANCE_ID_KEY);
        }

        MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME, "IronSource load RewardedVideo for instance " + mInstanceId);
        mIronSourceAdapterConfiguration.setCachedInitializationParameters(activity, serverExtras);
        IronSource.loadISDemandOnlyRewardedVideo(mInstanceId);
    }

    @Override
    protected boolean hasVideoAvailable() {
        boolean isVideoAvailable = IronSource.isISDemandOnlyRewardedVideoAvailable(mInstanceId);
        MoPubLog.log(CUSTOM, ADAPTER_NAME, "IronSource hasVideoAvailable returned " + isVideoAvailable + " (current instance: " + mInstanceId + " )");

        return isVideoAvailable;
    }

    @Override
    protected void showVideo() {
        MoPubLog.log(SHOW_ATTEMPTED, ADAPTER_NAME, "with instanceId: " + mInstanceId);

        IronSource.showISDemandOnlyRewardedVideo(mInstanceId);
    }

    /**
     * Class Helper Methods
     **/
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

    //Invoked when the RewardedVideo ad view has opened.
    @Override
    public void onRewardedVideoAdOpened(String instanceId) {
        MoPubLog.log(CUSTOM, ADAPTER_NAME, "IronSource Rewarded Video opened ad for instance " + instanceId + " (current instance: " + mInstanceId + " )");
        MoPubLog.log(SHOW_SUCCESS, ADAPTER_NAME);

        MoPubRewardedVideoManager.onRewardedVideoStarted(IronSourceRewardedVideo.class, instanceId);
    }

    //Invoked when the user is about to return to the application after closing the RewardedVideo ad.
    @Override
    public void onRewardedVideoAdClosed(String instanceId) {
        MoPubLog.log(CUSTOM, ADAPTER_NAME, "IronSource Rewarded Video closed ad for instance " + instanceId + " (current instance: " + mInstanceId + " )");

        MoPubRewardedVideoManager.onRewardedVideoClosed(IronSourceRewardedVideo.class, instanceId);
    }

    //Invoked when the user completed the video and should be rewarded.
    @Override
    public void onRewardedVideoAdRewarded(String instanceId) {
        MoPubLog.log(CUSTOM, ADAPTER_NAME, "IronSource Rewarded Video received reward for instance " +
                instanceId + " (current instance: " + mInstanceId + " )");

        MoPubReward reward = MoPubReward.success(MoPubReward.NO_REWARD_LABEL, MoPubReward.DEFAULT_REWARD_AMOUNT);
        MoPubLog.log(SHOULD_REWARD, ADAPTER_NAME, MoPubReward.NO_REWARD_LABEL, MoPubReward.DEFAULT_REWARD_AMOUNT);

        MoPubRewardedVideoManager.onRewardedVideoCompleted(IronSourceRewardedVideo.class, instanceId, reward);
    }

    //Invoked when an Ad failed to display.
    @Override
    public void onRewardedVideoAdShowFailed(String instanceId, IronSourceError ironSourceError) {
        MoPubLog.log(CUSTOM, ADAPTER_NAME, "IronSource Rewarded Video failed to show for instance " +
                instanceId + " (current instance: " + mInstanceId + " )");
        MoPubLog.log(SHOW_FAILED, ADAPTER_NAME,
                getMoPubErrorMessage(ironSourceError).getIntCode(),
                getMoPubErrorMessage(ironSourceError));

        MoPubRewardedVideoManager.onRewardedVideoPlaybackError(IronSourceRewardedVideo.class, instanceId, getMoPubErrorMessage(ironSourceError));

    }

    //Invoked when the video ad was clicked by the user.
    @Override
    public void onRewardedVideoAdClicked(String instanceId) {
        MoPubLog.log(CUSTOM, ADAPTER_NAME, "IronSource Rewarded Video clicked for instance " + instanceId + " (current instance: " + mInstanceId + " )");
        MoPubLog.log(CLICKED, ADAPTER_NAME);

        MoPubRewardedVideoManager.onRewardedVideoClicked(IronSourceRewardedVideo.class, instanceId);
    }

    //Invoked when the video ad load succeeded.
    @Override
    public void onRewardedVideoAdLoadSuccess(String instanceId) {
        MoPubLog.log(CUSTOM, ADAPTER_NAME, "IronSource Rewarded Video loaded successfully for " + "instance " + instanceId + " (current instance: " + mInstanceId + " )");
        MoPubLog.log(LOAD_SUCCESS, ADAPTER_NAME);

        MoPubRewardedVideoManager.onRewardedVideoLoadSuccess(IronSourceRewardedVideo.class, instanceId);
    }

    //Invoked when the video ad load failed.
    @Override
    public void onRewardedVideoAdLoadFailed(String instanceId, IronSourceError ironSourceError) {
        MoPubLog.log(CUSTOM, ADAPTER_NAME, "IronSource Rewarded Video failed to load for instance "+ instanceId + " (current instance: " + mInstanceId + " )");
        MoPubLog.log(LOAD_FAILED, ADAPTER_NAME, getMoPubErrorMessage(ironSourceError).getIntCode(), getMoPubErrorMessage(ironSourceError));

        MoPubRewardedVideoManager.onRewardedVideoLoadFailure(IronSourceRewardedVideo.class, instanceId, getMoPubErrorMessage(ironSourceError));
    }
}