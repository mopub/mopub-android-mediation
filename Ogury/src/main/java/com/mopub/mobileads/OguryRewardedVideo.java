package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.LifecycleListener;
import com.mopub.common.MoPubReward;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.ogury.core.OguryError;
import com.ogury.ed.OguryAdImpressionListener;
import com.ogury.ed.OguryOptinVideoAd;
import com.ogury.ed.OguryOptinVideoAdListener;
import com.ogury.ed.OguryReward;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOULD_REWARD;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_FAILED;

public class OguryRewardedVideo extends BaseAd implements OguryOptinVideoAdListener, OguryAdImpressionListener {
    private static final String ADAPTER_NAME = OguryRewardedVideo.class.getSimpleName();

    private final OguryAdapterConfiguration mOguryAdapterConfiguration;

    private String mAdUnitId;
    private OguryOptinVideoAd mOptInVideo;
    private OguryAdListenerHelper mListenerHelper;

    public OguryRewardedVideo() {
        mOguryAdapterConfiguration = new OguryAdapterConfiguration();
    }

    @Nullable
    @Override
    protected LifecycleListener getLifecycleListener() {
        return null;
    }

    @NonNull
    @Override
    protected String getAdNetworkId() {
        return mAdUnitId != null ? mAdUnitId : "";
    }

    @Override
    protected boolean checkAndInitializeSdk(@NonNull Activity launcherActivity, @NonNull AdData adData) {
        Preconditions.checkNotNull(launcherActivity);
        Preconditions.checkNotNull(adData);

        return OguryAdapterConfiguration.startOgurySDKIfNecessary(launcherActivity, adData.getExtras());
    }

    @Override
    protected void load(@NonNull Context context, @NonNull AdData adData) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(adData);

        setAutomaticImpressionAndClickTracking(false);

        final Map<String, String> extras = adData.getExtras();
        mAdUnitId = OguryAdapterConfiguration.getAdUnitId(extras);

        if (TextUtils.isEmpty(mAdUnitId)) {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Received invalid Ogury ad " +
                    "unit ID for rewarded ad. Failing ad request.");
            MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR.getIntCode(),
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);

            if (mLoadListener != null) {
                mLoadListener.onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            }

            return;
        }

        mOguryAdapterConfiguration.setCachedInitializationParameters(context, extras);

        mListenerHelper = new OguryAdListenerHelper(ADAPTER_NAME, mAdUnitId);
        mListenerHelper.setLoadListener(mLoadListener);

        mOptInVideo = new OguryOptinVideoAd(context, mAdUnitId);
        mOptInVideo.setListener(this);
        mOptInVideo.setAdImpressionListener(this);
        mOptInVideo.load();

        MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);
    }

    @Override
    protected void show() {
        if (mOptInVideo == null || !mOptInVideo.isLoaded()) {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Unable to show Ogury " +
                    "rewarded/opt-in ad because it is null or not yet ready.");

            MoPubLog.log(getAdNetworkId(), SHOW_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.AD_SHOW_ERROR.getIntCode(),
                    MoPubErrorCode.AD_SHOW_ERROR);

            if (mInteractionListener != null) {
                mInteractionListener.onAdFailed(MoPubErrorCode.AD_SHOW_ERROR);
            }

            return;
        }

        mListenerHelper.setInteractionListener(mInteractionListener);
        mOptInVideo.show();

        MoPubLog.log(getAdNetworkId(), SHOW_ATTEMPTED, ADAPTER_NAME);
    }

    @Override
    protected void onInvalidate() {
        mListenerHelper = null;
        mOptInVideo = null;
    }

    @Override
    public void onAdLoaded() {
        mListenerHelper.onAdLoaded();
    }

    @Override
    public void onAdDisplayed() {
        mListenerHelper.onAdDisplayed();
    }

    @Override
    public void onAdClicked() {
        mListenerHelper.onAdClicked();
    }

    @Override
    public void onAdClosed() {
        mListenerHelper.onAdClosed();
    }

    @Override
    public void onAdError(OguryError oguryError) {
        mListenerHelper.onAdError(oguryError);
    }

    @Override
    public void onAdRewarded(OguryReward oguryReward) {
        if (mInteractionListener != null) {
            mInteractionListener.onAdComplete(MoPubReward.success(MoPubReward.NO_REWARD_LABEL,
                    MoPubReward.DEFAULT_REWARD_AMOUNT));
        }

        MoPubLog.log(getAdNetworkId(), SHOULD_REWARD, ADAPTER_NAME, MoPubReward.DEFAULT_REWARD_AMOUNT,
                MoPubReward.NO_REWARD_LABEL);
    }

    @Override
    public void onAdImpression() {
        mListenerHelper.onAdImpression();
    }
}
