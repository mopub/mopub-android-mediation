/* Copyright 2020 Fyber N.V.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License
 */

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
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.AdData;
import com.mopub.mobileads.BaseAd;
import com.mopub.mobileads.MoPubErrorCode;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;

/**
 * Implements Fyber's rewarded video Mopub's custom event class
 */
public class FyberRewardedVideo extends BaseAd {
    // Mopub log tag definition
    private final static String LOG_TAG = "FyberRewardedVideoForMopub";

    /** Cache the spot id, and return when getAdNetworkId is called.
     * Initialized to "", because getAdNetworkId cannot return null */
    private String mSpotId = "";

    InneractiveAdSpot mRewardedSpot;
    Activity mParentActivity;
    private boolean mRewarded = false;

    @Override
    protected LifecycleListener getLifecycleListener() {
        return null;
    }

    @Override
    protected String getAdNetworkId() {
        return mSpotId;
    }

    @Override
    protected boolean checkAndInitializeSdk(@NonNull Activity launcherActivity, @NonNull AdData adData) throws Exception {
        mParentActivity = launcherActivity;
        return false;
    }

    @Override
    protected void load(@NonNull Context context, @NonNull AdData adData) throws Exception {
        log("load rewarded requested");

        // Set variables from MoPub console.
        final Map<String, String> extras = adData.getExtras();
        final String appId = extras == null ? null : extras.get(FyberMopubMediationDefs.REMOTE_KEY_APP_ID);
        final String spotId = extras == null ? null : extras.get(FyberMopubMediationDefs.REMOTE_KEY_SPOT_ID);

        if (TextUtils.isEmpty(spotId)) {
            log("No spotID defined for ad unit. Cannot load rewarded");
            if (mLoadListener != null) {
                mLoadListener.onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            }
            return;
        }

        mSpotId = spotId;

        // If we've received an appId for this unit, try initializing the Fyber Marketplace SDK, if it was not already initialized
        if (!TextUtils.isEmpty(appId)) {
            FyberAdapterConfiguration.initializeFyberMarketplace(context.getApplicationContext(), appId, extras.containsKey(
                    FyberMopubMediationDefs.REMOTE_KEY_DEBUG),
                    new FyberAdapterConfiguration.OnFyberAdapterConfigurationResolvedListener() {
                        @Override
                        public void onFyberAdapterConfigurationResolved(
                                OnFyberMarketplaceInitializedListener.FyberInitStatus status) {
                            //note - we try to load ads when "FAILED" because an ad request will re-attempt to initialize the relevant parts of the SDK.
                            if (status == OnFyberMarketplaceInitializedListener.FyberInitStatus.SUCCESSFULLY || status == OnFyberMarketplaceInitializedListener.FyberInitStatus.FAILED) {
                                requestRewarded(extras);
                            } else if (mLoadListener != null) {
                                mLoadListener.onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
                            }
                        }
                    });
        } else if (InneractiveAdManager.wasInitialized()) {
            requestRewarded(extras);
        } else if (mLoadListener != null) {
            mLoadListener.onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
        }
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
        log("showVideo called for rewarded");
        // check if the ad is ready
        if (mRewardedSpot != null && mRewardedSpot.isReady()) {

            InneractiveFullscreenUnitController fullscreenUnitController = (InneractiveFullscreenUnitController)mRewardedSpot.getSelectedUnitController();
            fullscreenUnitController.setEventsListener(new InneractiveFullscreenAdEventsListener() {

                /**
                 * Called by Fyber Marketplace when an interstitial ad activity is closed
                 * @param adSpot Spot object
                 */
                @Override
                public void onAdDismissed(InneractiveAdSpot adSpot) {
                    log("onAdDismissed");

                    // We fire the reward when the video completes
                    if (mInteractionListener != null) {
                        mInteractionListener.onAdComplete(mRewarded ? MoPubReward.success(MoPubReward.NO_REWARD_LABEL, MoPubReward.DEFAULT_REWARD_AMOUNT) : MoPubReward.failure());
                        mInteractionListener.onAdDismissed();
                    }
                }

                /**
                 * Called by Fyber Marketplace when an interstitial ad activity is shown
                 * @param adSpot Spot object
                 */
                @Override
                public void onAdImpression(InneractiveAdSpot adSpot) {
                    log("onAdImpression");
                    if (mInteractionListener != null) {
                        mInteractionListener.onAdShown();
                        mInteractionListener.onAdImpression();
                    }
                }

                /**
                 * Called by Fyber Marketplace when an interstitial ad is clicked
                 * @param adSpot Spot object
                 */
                @Override
                public void onAdClicked(InneractiveAdSpot adSpot) {
                    if (mInteractionListener != null) {
                        mInteractionListener.onAdClicked();
                    }
                    log("onAdClicked");
                }

                /**
                 * Called by Fyber Marketplace when an interstitial ad opened an external application
                 * @param adSpot Spot object
                 */
                @Override
                public void onAdWillOpenExternalApp(InneractiveAdSpot adSpot) {
                    log("onAdWillOpenExternalApp");
                    // Don't call the onLeaveApplication() API since it causes a false Click event on MoPub
                }

                /**
                 * Called when an ad has entered an error state, this will only happen when the ad is being shown
                 * @param adSpot the relevant ad spot
                 */
                @Override
                public void onAdEnteredErrorState(InneractiveAdSpot adSpot, AdDisplayError error) {
                    log("onAdEnteredErrorState - " + error.getMessage());
                }

                /**
                 * Called by Fyber Marketplace when Fyber Marketplace's internal browser, which was opened by this interstitial, was closed
                 * @param adSpot Spot object
                 */
                @Override
                public void onAdWillCloseInternalBrowser(InneractiveAdSpot adSpot) {
                    log("onAdWillCloseInternalBrowser");
                }
            });

            // Add video content controller, for controlling video ads
            InneractiveFullscreenVideoContentController videoContentController = new InneractiveFullscreenVideoContentController();
            videoContentController.setEventsListener(new VideoContentListener() {
                @Override
                public void onProgress(int totalDurationInMsec, int positionInMsec) {
                        // Nothing to do here
                }

                /**
                 * Called by Fyber Marketplace when an Interstitial video ad was played to the end
                 * <br>Can be used for incentive flow
                 * <br>Note: This event does not indicate that the interstitial was closed
                 */
                @Override
                public void onCompleted() {
                    mRewarded = true;
                    log("Got video content completed event. Do not report reward back just yet. wait for dismiss");
                }

                @Override
                public void onPlayerError() {
                    log("Got video content play error event");
                    if (mInteractionListener != null) {
                        mInteractionListener.onAdFailed(MoPubErrorCode.VIDEO_PLAYBACK_ERROR);
                    }
                }
            });

            fullscreenUnitController.setRewardedListener(new InneractiveFullScreenAdRewardedListener() {
                @Override
                public void onAdRewarded(InneractiveAdSpot adSpot) {
                    mRewarded = true;
                }
            });

            // Now add the content controller to the unit controller
            fullscreenUnitController.addContentController(videoContentController);

            fullscreenUnitController.show(mParentActivity);
        } else {
            if (mInteractionListener != null) {
                mInteractionListener.onAdFailed(MoPubErrorCode.EXPIRED);
            }
            log("The rewarded ad is not ready yet.");
        }
    }

    private void requestRewarded(Map<String, String> localExtras) {

        if (mParentActivity == null || TextUtils.isEmpty(mSpotId)) {
            mLoadListener.onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
        }

        FyberAdapterConfiguration.updateGdprConsentStatusFromMopub();

        if (mRewardedSpot != null) {
            mRewardedSpot.destroy();
        }

        mRewardedSpot = InneractiveAdSpotManager.get().createSpot();
        // Set your mediation name and version
        mRewardedSpot.setMediationName(InneractiveMediationName.MOPUB);
        mRewardedSpot.setMediationVersion(MoPub.SDK_VERSION);

        InneractiveFullscreenUnitController fullscreenUnitController = new InneractiveFullscreenUnitController();
        mRewardedSpot.addUnitController(fullscreenUnitController);

        InneractiveAdRequest request = new InneractiveAdRequest(mSpotId);
        FyberAdapterConfiguration.updateRequestFromExtras(request, localExtras);

        // Load ad
        mRewardedSpot.setRequestListener(new InneractiveAdSpot.RequestListener() {

            /**
             * Called by Fyber Marketplace when an interstitial is ready for display
             * @param adSpot Spot object
             */
            @Override
            public void onInneractiveSuccessfulAdRequest(InneractiveAdSpot adSpot) {
                log("on ad loaded successfully");
                if (mLoadListener != null) {
                    mLoadListener.onAdLoaded();
                }
            }

            /**
             * Called by Fyber Marketplace when an interstitial fails loading
             * @param adSpot Spot object
             * @param errorCode the failure's error.
             */
            @Override
            public void onInneractiveFailedAdRequest(InneractiveAdSpot adSpot, InneractiveErrorCode errorCode) {
                log("Failed loading rewarded with error: " + errorCode);
                if (mLoadListener != null) {
                    if (errorCode == InneractiveErrorCode.CONNECTION_ERROR) {
                        mLoadListener.onAdLoadFailed(MoPubErrorCode.NO_CONNECTION);
                    } else if  (errorCode == InneractiveErrorCode.CONNECTION_TIMEOUT) {
                        mLoadListener.onAdLoadFailed(MoPubErrorCode.NETWORK_TIMEOUT);
                    } else if (errorCode == InneractiveErrorCode.NO_FILL) {
                        mLoadListener.onAdLoadFailed(MoPubErrorCode.NO_FILL);
                    } else {
                        mLoadListener.onAdLoadFailed(MoPubErrorCode.SERVER_ERROR);
                    }
                }
            }
        });

        mRewardedSpot.requestAd(request);
    }

    /**
     * MopubLog helper
     * @param message
     */
    private void log(String message) {
        MoPubLog.log(CUSTOM, LOG_TAG, message);
    }
}
