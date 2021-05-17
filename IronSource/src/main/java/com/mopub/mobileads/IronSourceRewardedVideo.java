package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.sdk.ISDemandOnlyRewardedVideoListener;
import com.mopub.common.BaseLifecycleListener;
import com.mopub.common.DataKeys;
import com.mopub.common.LifecycleListener;
import com.mopub.common.MoPub;
import com.mopub.common.MoPubReward;
import com.mopub.common.Preconditions;
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
import static com.mopub.mobileads.MoPubErrorCode.VIDEO_PLAYBACK_ERROR;

public class IronSourceRewardedVideo extends BaseAd implements ISDemandOnlyRewardedVideoListener {

    /**
     * private vars
     */

    // Configuration keys
    private static final String APPLICATION_KEY = "applicationKey";
    private static final String INSTANCE_ID_KEY = "instanceId";
    private static final String ADAPTER_NAME = IronSourceRewardedVideo.class.getSimpleName();

    // Network identifier of ironSource
    private String mInstanceId = IronSourceAdapterConfiguration.DEFAULT_INSTANCE_ID;

    @NonNull
    @Override
    protected String getAdNetworkId() {
        return mInstanceId;
    }

    @NonNull
    private IronSourceAdapterConfiguration mIronSourceAdapterConfiguration;

    /**
     * MoPub API
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
    protected boolean checkAndInitializeSdk(@NonNull final Activity launcherActivity,
                                            @NonNull final AdData adData) throws IllegalStateException {
        Preconditions.checkNotNull(launcherActivity);
        Preconditions.checkNotNull(adData);

        boolean canCollectPersonalInfo = MoPub.canCollectPersonalInformation();
        IronSource.setConsent(canCollectPersonalInfo);

        final Map<String, String> extras = adData.getExtras();
        try {
            if (TextUtils.isEmpty(extras.get(APPLICATION_KEY))) {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "ironSource Rewarded Video failed to initialize. " +
                        "ironSource applicationKey is not valid. Please make sure it's entered properly on MoPub UI.");

                if (mLoadListener != null) {
                    mLoadListener.onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
                }
                return false;
            }
            String applicationKey = extras.get(APPLICATION_KEY);

            final String instanceId = extras.get(INSTANCE_ID_KEY);
            if (!TextUtils.isEmpty(instanceId)) {
                mInstanceId = instanceId;
            }

            final Context context = launcherActivity.getApplicationContext();

            if (context != null) {
                initIronSourceSDK(context, applicationKey, extras);
                return true;
            } else {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "ironSource Interstitial failed to initialize." +
                        "Application Context obtained by Activity launching this interstitial is null.");
                if (mLoadListener != null) {
                    mLoadListener.onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
                }
                return false;
            }

        } catch (Exception e) {
            MoPubLog.log(CUSTOM_WITH_THROWABLE, e);
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "ironSource Interstitial failed to initialize." +
                    "Ensure ironSource applicationKey and instanceId are properly entered on MoPub UI.");
            if (mLoadListener != null) {
                mLoadListener.onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            }
            return false;
        }
    }

    private void initIronSourceSDK(Context context, String applicationKey, Map<String, String> extras) {
        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "ironSource Rewarded Video initialization is called with applicationKey: " + applicationKey);
        IronSource.setISDemandOnlyRewardedVideoListener(this);

        IronSource.AD_UNIT[] adUnitsToInit = mIronSourceAdapterConfiguration.getIronSourceAdUnitsToInitList(context, extras);
        IronSourceAdapterConfiguration.initIronSourceSDK(context, applicationKey, adUnitsToInit);
    }

    @Override
    protected void load(@NonNull final Context context, @NonNull final AdData adData) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(adData);

        setAutomaticImpressionAndClickTracking(false);

        if (!(context instanceof Activity)) {
            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME, MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR.getIntCode(), MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Failed to load rewarded video as ironSource requires an Activity context.");
            if (mLoadListener != null) {
                mLoadListener.onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            }
            return;
        }

        final Map<String, String> extras = adData.getExtras();

        /* Update instance id if extras contain it. ironSource requires instanceId to perform proper ad requests.
            Ideally instanceId should be populated on MoPub UI.
            For backward compatibility, we don't fail out if instanceId is empty or null below.
            If instanceId is empty, ironSource Ad Server will treat it as "0".
        */
        final String instanceId = extras.get(INSTANCE_ID_KEY);
        if (!TextUtils.isEmpty(instanceId)) {
            mInstanceId = instanceId;
        }

        mIronSourceAdapterConfiguration.retainIronSourceAdUnitsToInitPrefsIfNecessary(context,extras);
        mIronSourceAdapterConfiguration.setCachedInitializationParameters(context, extras);
        MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);

        final String adMarkup = extras.get(DataKeys.ADM_KEY);

        if(!TextUtils.isEmpty(adMarkup)) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "ADM field is populated. Will make Advanced Bidding request.");
            IronSource.loadISDemandOnlyRewardedVideoWithAdm((Activity) context, mInstanceId, adMarkup);
        } else {
            IronSource.loadISDemandOnlyRewardedVideo((Activity) context, mInstanceId);
        }
    }

    protected boolean hasVideoAvailable() {
        boolean isVideoAvailable = IronSource.isISDemandOnlyRewardedVideoAvailable(mInstanceId);
        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "IronSource hasVideoAvailable returned " + isVideoAvailable);

        return isVideoAvailable;
    }

    @Override
    protected void show() {
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

        if (mInteractionListener != null) {
            mInteractionListener.onAdShown();
            mInteractionListener.onAdImpression();
        }
    }

    //Invoked when the user is about to return to the application after closing the RewardedVideo ad.
    @Override
    public void onRewardedVideoAdClosed(String instanceId) {
        MoPubLog.log(CUSTOM, ADAPTER_NAME, "IronSource Rewarded Video closed ad for instance " + instanceId + " (current instance: " + getAdNetworkId() + " )");
        if (mInteractionListener != null) {
            mInteractionListener.onAdDismissed();
        }
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

        if (mInteractionListener != null) {
            mInteractionListener.onAdComplete(reward);
        }
    }

    //Invoked when an Ad failed to display.
    @Override
    public void onRewardedVideoAdShowFailed(String instanceId, IronSourceError ironSourceError) {
        MoPubLog.log(CUSTOM, ADAPTER_NAME, "IronSource Rewarded Video failed to show for instance " +
                instanceId + " (current instance: " + getAdNetworkId() + " )");
        MoPubLog.log(instanceId, SHOW_FAILED, ADAPTER_NAME,
                VIDEO_PLAYBACK_ERROR.getIntCode(),
                VIDEO_PLAYBACK_ERROR);

        if (ironSourceError != null && ironSourceError.getErrorMessage() != null) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "IronSource error: " + ironSourceError.toString());
        }

        if (mInteractionListener != null) {
            mInteractionListener.onAdFailed(VIDEO_PLAYBACK_ERROR);
        }
    }

    //Invoked when the video ad was clicked by the user.
    @Override
    public void onRewardedVideoAdClicked(String instanceId) {
        MoPubLog.log(CUSTOM, ADAPTER_NAME, "IronSource Rewarded Video clicked for instance " + instanceId + " (current instance: " + getAdNetworkId() + " )");
        MoPubLog.log(instanceId, CLICKED, ADAPTER_NAME);

        if (mInteractionListener != null) {
            mInteractionListener.onAdClicked();
        }
    }

    //Invoked when the video ad load succeeded.
    @Override
    public void onRewardedVideoAdLoadSuccess(String instanceId) {
        MoPubLog.log(CUSTOM, ADAPTER_NAME, "IronSource Rewarded Video loaded successfully for instance " + instanceId + " (current instance: " + getAdNetworkId() + " )");
        MoPubLog.log(instanceId, LOAD_SUCCESS, ADAPTER_NAME);

        if (mLoadListener != null) {
            mLoadListener.onAdLoaded();
        }
    }

    //Invoked when the video ad load failed.
    @Override
    public void onRewardedVideoAdLoadFailed(String instanceId, IronSourceError ironSourceError) {
        MoPubLog.log(CUSTOM, ADAPTER_NAME, "IronSource Rewarded Video failed to load for instance "+ instanceId + " (current instance: " + getAdNetworkId() + " )");
        MoPubLog.log(instanceId, LOAD_FAILED, ADAPTER_NAME,
                IronSourceAdapterConfiguration.getMoPubErrorCode(ironSourceError).getIntCode(),
                IronSourceAdapterConfiguration.getMoPubErrorCode(ironSourceError));

        if (mLoadListener != null) {
            mLoadListener.onAdLoadFailed(IronSourceAdapterConfiguration.getMoPubErrorCode(ironSourceError));
        }
    }
}
