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
import com.snap.adkit.dagger.AdKitApplication;
import com.snap.adkit.external.AdKitSlotType;
import com.snap.adkit.external.SnapAdClicked;
import com.snap.adkit.external.SnapAdDismissed;
import com.snap.adkit.external.SnapAdEventListener;
import com.snap.adkit.external.SnapAdImpressionHappened;
import com.snap.adkit.external.SnapAdKit;
import com.snap.adkit.external.SnapAdKitEvent;
import com.snap.adkit.external.SnapAdKitSlot;
import com.snap.adkit.external.SnapAdLoadFailed;
import com.snap.adkit.external.SnapAdLoadSucceeded;
import com.snap.adkit.external.SnapAdRewardEarned;
import com.snap.adkit.external.SnapAdVisible;

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
import static com.mopub.mobileads.MoPubErrorCode.FULLSCREEN_LOAD_ERROR;
import static com.mopub.mobileads.MoPubErrorCode.NETWORK_NO_FILL;
import static com.mopub.mobileads.MoPubErrorCode.VIDEO_PLAYBACK_ERROR;

public class SnapAdRewardedVideo extends BaseAd {
    private static final String ADAPTER_NAME = SnapAdRewardedVideo.class.getSimpleName();
    private static final String SLOT_ID_KEY = "slotId";

    private static String mSlotId;
    private final SnapAdAdapterConfiguration mSnapAdAdapterConfiguration;

    public SnapAdRewardedVideo() {
        mSnapAdAdapterConfiguration = new SnapAdAdapterConfiguration();
    }

    @NonNull
    private final SnapAdKit snapAdKit = AdKitApplication.getSnapAdKit();

    @Nullable
    @Override
    protected LifecycleListener getLifecycleListener() {
        return null;
    }

    @Override
    protected boolean checkAndInitializeSdk(@NonNull Activity launcherActivity,
                                            @NonNull AdData adData) {
        return false;
    }

    @Override
    protected void load(@NonNull Context context, @NonNull AdData adData) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(adData);

        setAutomaticImpressionAndClickTracking(false);

        final Map<String, String> extras = adData.getExtras();

        if (extras == null || extras.isEmpty()) {
            MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR.getIntCode(),
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            if (mLoadListener != null) {
                mLoadListener.onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            }
            return;
        }

        mSlotId = extras.get(SLOT_ID_KEY);

        snapAdKit.setupListener(new SnapAdEventListener() {
            @Override
            public void onEvent(SnapAdKitEvent snapAdKitEvent, String slotId) {
                if (snapAdKitEvent instanceof SnapAdLoadSucceeded) {
                    MoPubLog.log(getAdNetworkId(), LOAD_SUCCESS, ADAPTER_NAME);

                    if (mLoadListener != null) {
                        mLoadListener.onAdLoaded();
                    }
                } else if (snapAdKitEvent instanceof SnapAdLoadFailed) {
                    MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                            FULLSCREEN_LOAD_ERROR.getIntCode(), FULLSCREEN_LOAD_ERROR);

                    if (mLoadListener != null) {
                        mLoadListener.onAdLoadFailed(FULLSCREEN_LOAD_ERROR);
                    }
                } else if (snapAdKitEvent instanceof SnapAdVisible) {
                    MoPubLog.log(getAdNetworkId(), SHOW_SUCCESS, ADAPTER_NAME);

                    if (mInteractionListener != null) {
                        mInteractionListener.onAdShown();
                    }
                } else if (snapAdKitEvent instanceof SnapAdClicked) {
                    MoPubLog.log(getAdNetworkId(), CLICKED, ADAPTER_NAME);

                    if (mInteractionListener != null) {
                        mInteractionListener.onAdClicked();
                    }
                } else if (snapAdKitEvent instanceof SnapAdImpressionHappened) {
                    MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Snap recorded " +
                            "impression: " + snapAdKitEvent.toString());

                    if (mInteractionListener != null) {
                        mInteractionListener.onAdImpression();
                    }
                } else if (snapAdKitEvent instanceof SnapAdDismissed) {
                    MoPubLog.log(getAdNetworkId(), DID_DISAPPEAR, ADAPTER_NAME);

                    if (mInteractionListener != null) {
                        mInteractionListener.onAdDismissed();
                    }
                } else if (snapAdKitEvent instanceof SnapAdRewardEarned) {
                    MoPubLog.log(getAdNetworkId(), SHOULD_REWARD, ADAPTER_NAME,
                            MoPubReward.DEFAULT_REWARD_AMOUNT, MoPubReward.NO_REWARD_LABEL);

                    if (mInteractionListener != null) {
                        mInteractionListener.onAdComplete(MoPubReward.success(MoPubReward.NO_REWARD_LABEL,
                                MoPubReward.DEFAULT_REWARD_AMOUNT));
                    }
                } else {
                    MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Received event " +
                            "from Snap Ad Kit: " + snapAdKitEvent.toString());
                }
            }
        });

        mSnapAdAdapterConfiguration.setCachedInitializationParameters(context, extras);
        MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);

        snapAdKit.loadRewarded(mSlotId, null);
    }

    @Override
    protected void show() {
        MoPubLog.log(getAdNetworkId(), SHOW_ATTEMPTED, ADAPTER_NAME);

        try {
            snapAdKit.playAd(new SnapAdKitSlot(null, AdKitSlotType.REWARDED));
        } catch (Exception exception) {
            MoPubLog.log(getAdNetworkId(), CUSTOM_WITH_THROWABLE, ADAPTER_NAME, exception);
            MoPubLog.log(getAdNetworkId(), SHOW_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.VIDEO_PLAYBACK_ERROR.getIntCode(),
                    MoPubErrorCode.VIDEO_PLAYBACK_ERROR);

            if (mInteractionListener != null) {
                mInteractionListener.onAdFailed(VIDEO_PLAYBACK_ERROR);
            }
        }
    }

    @Override
    protected void onInvalidate() {
        // no-op
    }

    @NonNull
    @Override
    protected String getAdNetworkId() {
        return TextUtils.isEmpty(mSlotId) ? "" : mSlotId;
    }
}
