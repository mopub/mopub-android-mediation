package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.fyber.inneractive.sdk.external.InneractiveAdManager;
import com.fyber.inneractive.sdk.external.InneractiveAdRequest;
import com.fyber.inneractive.sdk.external.InneractiveAdSpot;
import com.fyber.inneractive.sdk.external.InneractiveAdSpotManager;
import com.fyber.inneractive.sdk.external.InneractiveErrorCode;
import com.fyber.inneractive.sdk.external.InneractiveFullScreenAdRewardedListener;
import com.fyber.inneractive.sdk.external.InneractiveFullscreenAdEventsListener;
import com.fyber.inneractive.sdk.external.InneractiveFullscreenUnitController;
import com.fyber.inneractive.sdk.external.InneractiveFullscreenVideoContentController;
import com.fyber.inneractive.sdk.external.InneractiveMediationName;
import com.fyber.inneractive.sdk.external.InneractiveUnitController.AdDisplayError;
import com.fyber.inneractive.sdk.external.OnFyberMarketplaceInitializedListener;
import com.fyber.inneractive.sdk.external.VideoContentListener;
import com.mopub.common.LifecycleListener;
import com.mopub.common.MoPub;
import com.mopub.common.MoPubReward;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.AdData;
import com.mopub.mobileads.BaseAd;
import com.mopub.mobileads.MoPubErrorCode;

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
import static com.mopub.mobileads.MoPubErrorCode.FULLSCREEN_LOAD_ERROR;

public class FyberRewardedVideo extends BaseAd {

    private static final String ADAPTER_NAME = FyberRewardedVideo.class.getSimpleName();
    private final FyberAdapterConfiguration mFyberAdapterConfiguration;

    public FyberRewardedVideo() {
        mFyberAdapterConfiguration = new FyberAdapterConfiguration();
    }

    private String mSpotId = "";

    InneractiveAdSpot mRewardedSpot;
    Activity mParentActivity;

    @Override
    protected LifecycleListener getLifecycleListener() {
        return null;
    }

    @Override
    protected String getAdNetworkId() {
        return mSpotId == null ? "" : mSpotId;
    }

    @Override
    protected boolean checkAndInitializeSdk(@NonNull Activity launcherActivity, @NonNull AdData adData) throws Exception {
        Preconditions.checkNotNull(launcherActivity);
        Preconditions.checkNotNull(adData);

        mParentActivity = launcherActivity;
        return false;
    }

    @Override
    protected void load(@NonNull Context context, @NonNull AdData adData) throws Exception {
        Preconditions.checkNotNull(adData);
        Preconditions.checkNotNull(context);

        MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);

        final Map<String, String> extras = adData.getExtras();
        final String appId = extras == null ? null : extras.get(FyberMoPubMediationDefs.REMOTE_KEY_APP_ID);
        final String spotId = extras == null ? null : extras.get(FyberMoPubMediationDefs.REMOTE_KEY_SPOT_ID);

        if (TextUtils.isEmpty(spotId)) {
            MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR.getIntCode(),
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            if (mLoadListener != null) {
                mLoadListener.onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            }
            return;
        }

        mSpotId = spotId;

        if (!TextUtils.isEmpty(appId)) {
            FyberAdapterConfiguration.initializeFyberMarketplace(context.getApplicationContext(), appId, extras.containsKey(
                    FyberMoPubMediationDefs.REMOTE_KEY_DEBUG),
                    new FyberAdapterConfiguration.OnFyberAdapterConfigurationResolvedListener() {
                        @Override
                        public void onFyberAdapterConfigurationResolved(
                                OnFyberMarketplaceInitializedListener.FyberInitStatus status) {
                            //note - Fyber tries to load ads when "FAILED" because an ad request will re-attempt to initialize the relevant parts of the SDK.
                            if (status == OnFyberMarketplaceInitializedListener.FyberInitStatus.SUCCESSFULLY || status == OnFyberMarketplaceInitializedListener.FyberInitStatus.FAILED) {
                                requestRewarded(extras);
                            } else if (mLoadListener != null) {
                                MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                                        MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR.getIntCode(),
                                        MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
                                mLoadListener.onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
                            }
                        }
                    });
        } else if (InneractiveAdManager.wasInitialized()) {
            requestRewarded(extras);
        } else if (mLoadListener != null) {
            MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR.getIntCode(),
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            mLoadListener.onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
        }
        mFyberAdapterConfiguration.setCachedInitializationParameters(context, extras);
    }

    @Override
    protected void onInvalidate() {
        if (mRewardedSpot != null) {
            mRewardedSpot.destroy();
            mRewardedSpot = null;
        }
    }

    @Override
    public void show() {
        MoPubLog.log(getAdNetworkId(), SHOW_ATTEMPTED, ADAPTER_NAME);

        if (mRewardedSpot != null && mRewardedSpot.isReady()) {
            final InneractiveFullscreenUnitController fullscreenUnitController = (InneractiveFullscreenUnitController)mRewardedSpot.getSelectedUnitController();
            fullscreenUnitController.setEventsListener(new InneractiveFullscreenAdEventsListener() {
                @Override
                public void onAdDismissed(InneractiveAdSpot adSpot) {
                    MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Fyber rewarded video has been dismissed");

                    if (mInteractionListener != null) {
                        mInteractionListener.onAdDismissed();
                    }
                }

                @Override
                public void onAdImpression(InneractiveAdSpot adSpot) {
                    MoPubLog.log(getAdNetworkId(), SHOW_SUCCESS, ADAPTER_NAME);

                    if (mInteractionListener != null) {
                        mInteractionListener.onAdShown();
                        mInteractionListener.onAdImpression();
                    }
                }

                @Override
                public void onAdClicked(InneractiveAdSpot adSpot) {
                    MoPubLog.log(getAdNetworkId(), CLICKED, ADAPTER_NAME);

                    if (mInteractionListener != null) {
                        mInteractionListener.onAdClicked();
                    }
                }

                @Override
                public void onAdWillOpenExternalApp(InneractiveAdSpot adSpot) {
                    MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "onAdWillOpenExternalApp");
                    // Don't call the onLeaveApplication() API since it causes a false Click event on MoPub
                }

                @Override
                public void onAdEnteredErrorState(InneractiveAdSpot adSpot, AdDisplayError error) {
                    MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "onAdEnteredErrorState - " +
                            error.getMessage());
                }

                @Override
                public void onAdWillCloseInternalBrowser(InneractiveAdSpot adSpot) {
                    MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "onAdWillCloseInternalBrowser");
                }
            });

            InneractiveFullscreenVideoContentController videoContentController = new InneractiveFullscreenVideoContentController();
            videoContentController.setEventsListener(new VideoContentListener() {
                @Override
                public void onProgress(int totalDurationInMsec, int positionInMsec) {
                        // Nothing to do here
                }

                @Override
                public void onCompleted() {
                    /* Got video content completed event. Do not report reward back just yet.*/
                    MoPubLog.log(CUSTOM, ADAPTER_NAME, "Video content completed event. Do not report reward back yet.");
                }

                @Override
                public void onPlayerError() {
                    MoPubLog.log(getAdNetworkId(), SHOW_FAILED, ADAPTER_NAME, "Video content play error event");

                    if (mInteractionListener != null) {
                        mInteractionListener.onAdFailed(MoPubErrorCode.AD_SHOW_ERROR);
                    }
                }
            });

            fullscreenUnitController.setRewardedListener(new InneractiveFullScreenAdRewardedListener() {
                @Override
                public void onAdRewarded(InneractiveAdSpot adSpot) {
                    if (mInteractionListener != null) {
                        MoPubLog.log(getAdNetworkId(), SHOULD_REWARD, ADAPTER_NAME, MoPubReward.DEFAULT_REWARD_AMOUNT,
                                MoPubReward.NO_REWARD_LABEL);
                        mInteractionListener.onAdComplete(MoPubReward.success(MoPubReward.NO_REWARD_LABEL,
                                MoPubReward.DEFAULT_REWARD_AMOUNT));
                    }
                }
            });

            fullscreenUnitController.addContentController(videoContentController);
            fullscreenUnitController.show(mParentActivity);
        } else {
            if (mInteractionListener != null) {
                MoPubLog.log(getAdNetworkId(), SHOW_FAILED, ADAPTER_NAME, MoPubErrorCode.EXPIRED);
                mInteractionListener.onAdFailed(MoPubErrorCode.EXPIRED);
            }
        }
    }

    private void requestRewarded(Map<String, String> localExtras) {

        if (mParentActivity == null || TextUtils.isEmpty(mSpotId)) {
            MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME, MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            mLoadListener.onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
        }

        if (mRewardedSpot != null) {
            mRewardedSpot.destroy();
        }

        mRewardedSpot = InneractiveAdSpotManager.get().createSpot();

        mRewardedSpot.setMediationName(InneractiveMediationName.MOPUB);
        mRewardedSpot.setMediationVersion(MoPub.SDK_VERSION);

        InneractiveFullscreenUnitController fullscreenUnitController = new InneractiveFullscreenUnitController();
        mRewardedSpot.addUnitController(fullscreenUnitController);

        final InneractiveAdRequest request = new InneractiveAdRequest(mSpotId);
        FyberAdapterConfiguration.updateRequestFromExtras(request, localExtras);

        mRewardedSpot.setRequestListener(new InneractiveAdSpot.RequestListener() {

            @Override
            public void onInneractiveSuccessfulAdRequest(InneractiveAdSpot adSpot) {
                MoPubLog.log(getAdNetworkId(), LOAD_SUCCESS, ADAPTER_NAME);

                if (mLoadListener != null) {
                    mLoadListener.onAdLoaded();
                }
            }

            @Override
            public void onInneractiveFailedAdRequest(InneractiveAdSpot adSpot, InneractiveErrorCode errorCode) {
                MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME, FULLSCREEN_LOAD_ERROR.getIntCode(),
                        errorCode);
                if (mLoadListener != null) {
                    if (errorCode == InneractiveErrorCode.CONNECTION_ERROR) {
                        mLoadListener.onAdLoadFailed(MoPubErrorCode.NO_CONNECTION);
                    } else if (errorCode == InneractiveErrorCode.CONNECTION_TIMEOUT) {
                        mLoadListener.onAdLoadFailed(MoPubErrorCode.NETWORK_TIMEOUT);
                    } else if (errorCode == InneractiveErrorCode.NO_FILL) {
                        mLoadListener.onAdLoadFailed(MoPubErrorCode.NETWORK_NO_FILL);
                    } else {
                        mLoadListener.onAdLoadFailed(MoPubErrorCode.UNSPECIFIED);
                    }
                }
            }
        });

        mRewardedSpot.requestAd(request);
    }
}
