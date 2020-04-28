package com.mopub.mobileads;

import android.app.Activity;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.DID_DISAPPEAR;
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

    // Network identifier of ironSource
    @NonNull
    private String mInstanceId = IronSourceAdapterConfiguration.DEFAULT_INSTANCE_ID;

    @NonNull
    @Override
    protected String getAdNetworkId() {
        return mInstanceId;
    }

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
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "IronSource initialization succeeded for RewardedVideo");

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

        MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);
        mIronSourceAdapterConfiguration.setCachedInitializationParameters(activity, serverExtras);
        IronSource.loadISDemandOnlyRewardedVideo(mInstanceId);
    }

    @Override
    protected boolean hasVideoAvailable() {
        boolean isVideoAvailable = IronSource.isISDemandOnlyRewardedVideoAvailable(mInstanceId);
        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "IronSource hasVideoAvailable returned " + isVideoAvailable);

        return isVideoAvailable;
    }

    @Override
    protected void showVideo() {
        MoPubLog.log(getAdNetworkId(), SHOW_ATTEMPTED, ADAPTER_NAME);

        IronSource.showISDemandOnlyRewardedVideo(mInstanceId);
    }

    /**
     * IronSource RewardedVideo Listener
     **/

    //Invoked when the RewardedVideo ad view has opened.
    @Override
    public void onRewardedVideoAdOpened(String instanceId) {
        MoPubLog.log(CUSTOM, ADAPTER_NAME, "IronSource Rewarded Video opened ad for instance " + instanceId + " (current instance: " + getAdNetworkId() + " )");
        MoPubLog.log(instanceId, SHOW_SUCCESS, ADAPTER_NAME);

        MoPubRewardedVideoManager.onRewardedVideoStarted(IronSourceRewardedVideo.class, instanceId);
    }

    //Invoked when the user is about to return to the application after closing the RewardedVideo ad.
    @Override
    public void onRewardedVideoAdClosed(String instanceId) {
        MoPubLog.log(CUSTOM, ADAPTER_NAME, "IronSource Rewarded Video closed ad for instance " + instanceId + " (current instance: " + getAdNetworkId() + " )");
        MoPubRewardedVideoManager.onRewardedVideoClosed(IronSourceRewardedVideo.class, instanceId);
        MoPubLog.log(instanceId, DID_DISAPPEAR, ADAPTER_NAME);
    }

    //Invoked when the user completed the video and should be rewarded.
    @Override
    public void onRewardedVideoAdRewarded(String instanceId) {
        MoPubLog.log(CUSTOM, ADAPTER_NAME, "IronSource Rewarded Video received reward for instance " +
                instanceId + " (current instance: " + getAdNetworkId() + " )");

        MoPubReward reward = MoPubReward.success(MoPubReward.NO_REWARD_LABEL, MoPubReward.DEFAULT_REWARD_AMOUNT);
        MoPubLog.log(instanceId, SHOULD_REWARD, ADAPTER_NAME,
                MoPubReward.NO_REWARD_LABEL,
                MoPubReward.DEFAULT_REWARD_AMOUNT);

        MoPubRewardedVideoManager.onRewardedVideoCompleted(IronSourceRewardedVideo.class, instanceId, reward);
    }

    //Invoked when an Ad failed to display.
    @Override
    public void onRewardedVideoAdShowFailed(String instanceId, IronSourceError ironSourceError) {
        MoPubLog.log(CUSTOM, ADAPTER_NAME, "IronSource Rewarded Video failed to show for instance " +
                instanceId + " (current instance: " + getAdNetworkId() + " )");
        MoPubLog.log(instanceId, SHOW_FAILED, ADAPTER_NAME,
                IronSourceAdapterConfiguration.getMoPubErrorCode(ironSourceError).getIntCode(),
                IronSourceAdapterConfiguration.getMoPubErrorCode(ironSourceError));

        MoPubRewardedVideoManager.onRewardedVideoPlaybackError(IronSourceRewardedVideo.class, instanceId, IronSourceAdapterConfiguration.getMoPubErrorCode(ironSourceError));

    }

    //Invoked when the video ad was clicked by the user.
    @Override
    public void onRewardedVideoAdClicked(String instanceId) {
        MoPubLog.log(CUSTOM, ADAPTER_NAME, "IronSource Rewarded Video clicked for instance " + instanceId + " (current instance: " + getAdNetworkId() + " )");
        MoPubLog.log(instanceId, CLICKED, ADAPTER_NAME);

        MoPubRewardedVideoManager.onRewardedVideoClicked(IronSourceRewardedVideo.class, instanceId);
    }

    //Invoked when the video ad load succeeded.
    @Override
    public void onRewardedVideoAdLoadSuccess(String instanceId) {
        MoPubLog.log(CUSTOM, ADAPTER_NAME, "IronSource Rewarded Video loaded successfully for instance " + instanceId + " (current instance: " + getAdNetworkId() + " )");
        MoPubLog.log(instanceId, LOAD_SUCCESS, ADAPTER_NAME);

        MoPubRewardedVideoManager.onRewardedVideoLoadSuccess(IronSourceRewardedVideo.class, instanceId);
    }

    //Invoked when the video ad load failed.
    @Override
    public void onRewardedVideoAdLoadFailed(String instanceId, IronSourceError ironSourceError) {
        MoPubLog.log(CUSTOM, ADAPTER_NAME, "IronSource Rewarded Video failed to load for instance "+ instanceId + " (current instance: " + getAdNetworkId() + " )");
        MoPubLog.log(instanceId, LOAD_FAILED, ADAPTER_NAME,
                IronSourceAdapterConfiguration.getMoPubErrorCode(ironSourceError).getIntCode(),
                IronSourceAdapterConfiguration.getMoPubErrorCode(ironSourceError));

        MoPubRewardedVideoManager.onRewardedVideoLoadFailure(IronSourceRewardedVideo.class, instanceId, IronSourceAdapterConfiguration.getMoPubErrorCode(ironSourceError));
    }
}