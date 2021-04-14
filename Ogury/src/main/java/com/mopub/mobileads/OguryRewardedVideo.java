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
    private OguryOptinVideoAd mOptinVideo;
    private OguryAdListenerHelper mListenerHelper;

    @Nullable
    @Override
    protected LifecycleListener getLifecycleListener() {
        return null;
    }

    @NonNull
    @Override
    protected String getAdNetworkId() {
        if (mAdUnitId == null) {
            return "";
        }
        return mAdUnitId;
    }

    @Override
    protected boolean checkAndInitializeSdk(
            @NonNull Activity launcherActivity,
            @NonNull AdData adData) {
        return false;
    }

    @Override
    protected void load(
            @NonNull Context context,
            @NonNull AdData adData) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(adData);

        setAutomaticImpressionAndClickTracking(false);

        OguryInitializer.startOgurySDKIfNecessary(context, adData.getExtras());
        OguryInitializer.updateConsent();

        mAdUnitId = OguryConfigurationParser.getAdUnitId(adData.getExtras());
        if (!OguryConfigurationParser.isAdUnitIdValid(mAdUnitId)) {
            if (mLoadListener != null) {
                mLoadListener.onAdLoadFailed(MoPubErrorCode.NETWORK_NO_FILL);
                MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                        MoPubErrorCode.NETWORK_NO_FILL.getIntCode(), MoPubErrorCode.NETWORK_NO_FILL);
            }
            return;
        }

        mOptinVideo = new OguryOptinVideoAd(context, mAdUnitId);
        mListenerHelper = new OguryAdListenerHelper(ADAPTER_NAME, mAdUnitId);

        mListenerHelper.setLoadListener(mLoadListener);
        mOptinVideo.setListener(this);
        mOptinVideo.load();

        MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);
    }

    @Override
    protected void show() {
        if (mOptinVideo == null || !mOptinVideo.isLoaded()) {
            MoPubLog.log(getAdNetworkId(), SHOW_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(), MoPubErrorCode.NETWORK_NO_FILL);
            if (mInteractionListener != null) {
                mInteractionListener.onAdFailed(MoPubErrorCode.NETWORK_NO_FILL);
            }
            return;
        }
        mListenerHelper.setInteractionListener(mInteractionListener);
        mOptinVideo.show();

        MoPubLog.log(getAdNetworkId(), SHOW_ATTEMPTED, ADAPTER_NAME);
    }

    @Override
    protected void onInvalidate() {
        mListenerHelper = null;
        mOptinVideo = null;
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
