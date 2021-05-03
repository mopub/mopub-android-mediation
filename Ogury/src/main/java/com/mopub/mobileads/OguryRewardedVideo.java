package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.LifecycleListener;
import com.mopub.common.MoPubReward;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.ogury.core.OguryError;
import com.ogury.ed.OguryOptinVideoAd;
import com.ogury.ed.OguryOptinVideoAdListener;
import com.ogury.ed.OguryReward;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOULD_REWARD;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_FAILED;

public class OguryRewardedVideo extends BaseAd implements OguryOptinVideoAdListener {

    private static final String ADAPTER_NAME = OguryRewardedVideo.class.getSimpleName();

    private String mAdUnitId;
    private OguryOptinVideoAd mOptInVideo;
    private OguryAdListenerHelper mListenerHelper;

    @Nullable
    @Override
    protected LifecycleListener getLifecycleListener() {
        return null;
    }

    @NonNull
    @Override
    protected String getAdNetworkId() {
        return mAdUnitId == null ? mAdUnitId : "";
    }

    @Override
    protected boolean checkAndInitializeSdk(
            @NonNull Activity launcherActivity,
            @NonNull AdData adData) {
        final boolean wasInitialized = OguryAdapterConfiguration.startOgurySDKIfNecessary(launcherActivity, adData.getExtras());
        OguryAdapterConfiguration.updateConsent();
        return wasInitialized;
    }

    @Override
    protected void load(
            @NonNull Context context,
            @NonNull AdData adData) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(adData);

        setAutomaticImpressionAndClickTracking(false);

        mAdUnitId = OguryAdapterConfiguration.getAdUnitId(adData.getExtras());
        if (!OguryAdapterConfiguration.isAdUnitIdValid(mAdUnitId)) {
            MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(), MoPubErrorCode.NETWORK_NO_FILL);
            if (mLoadListener != null) {
                mLoadListener.onAdLoadFailed(MoPubErrorCode.NETWORK_NO_FILL);
            }
            return;
        }

        mOptInVideo = new OguryOptinVideoAd(context, mAdUnitId);
        mListenerHelper = new OguryAdListenerHelper(ADAPTER_NAME, mAdUnitId);

        mListenerHelper.setLoadListener(mLoadListener);
        mOptInVideo.setListener(this);
        mOptInVideo.load();

        MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);
    }

    @Override
    protected void show() {
        if (mOptInVideo == null || !mOptInVideo.isLoaded()) {
            MoPubLog.log(getAdNetworkId(), SHOW_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(), MoPubErrorCode.NETWORK_NO_FILL);
            if (mInteractionListener != null) {
                mInteractionListener.onAdFailed(MoPubErrorCode.NETWORK_NO_FILL);
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

    /**
     * OguryRewardedVideo implementation
     */

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
        MoPubLog.log(getAdNetworkId(), SHOULD_REWARD, ADAPTER_NAME,
                MoPubReward.DEFAULT_REWARD_AMOUNT, MoPubReward.NO_REWARD_LABEL);
    }
}
