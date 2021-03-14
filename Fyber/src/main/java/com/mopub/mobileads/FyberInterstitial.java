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
import androidx.annotation.Nullable;

import com.fyber.inneractive.sdk.external.InneractiveAdManager;
import com.fyber.inneractive.sdk.external.InneractiveAdRequest;
import com.fyber.inneractive.sdk.external.InneractiveAdSpot;
import com.fyber.inneractive.sdk.external.InneractiveAdSpotManager;
import com.fyber.inneractive.sdk.external.InneractiveErrorCode;
import com.fyber.inneractive.sdk.external.InneractiveFullscreenAdEventsListener;
import com.fyber.inneractive.sdk.external.InneractiveFullscreenUnitController;
import com.fyber.inneractive.sdk.external.InneractiveFullscreenVideoContentController;
import com.fyber.inneractive.sdk.external.InneractiveMediationName;
import com.fyber.inneractive.sdk.external.InneractiveUnitController.AdDisplayError;
import com.fyber.inneractive.sdk.external.OnFyberMarketplaceInitializedListener;
import com.fyber.inneractive.sdk.external.VideoContentListener;
import com.mopub.common.LifecycleListener;
import com.mopub.common.MoPub;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.AdData;
import com.mopub.mobileads.BaseAd;
import com.mopub.mobileads.MoPubErrorCode;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;

/**
 * Implements Fyber's interstitial Mopub's custom event class
 */
public class FyberInterstitial extends BaseAd {
  // Mopub log tag definition
  private final static String LOG_TAG = "FyberInterstitialForMopub";

  // Members
  /**
   * Fyber's interstitial ad object
   */
  InneractiveAdSpot mInterstitialSpot;

  String mSpotId;
  /**
   * Context for showing the Ad
   */
  @Nullable
  Context mContext;

  @Nullable
  @Override
  protected LifecycleListener getLifecycleListener() {
    return null;
  }

  @NonNull
  @Override
  protected String getAdNetworkId() {
    return mSpotId;
  }

  @Override
  protected boolean checkAndInitializeSdk(@NonNull Activity launcherActivity, @NonNull AdData adData) throws Exception {
    return false;
  }

  @Override
  protected void load(final @NonNull Context context, @NonNull AdData adData) throws Exception {
    Preconditions.checkNotNull(context);
    Preconditions.checkNotNull(adData);

    log("load interstitial requested");

    mContext = context;

    setAutomaticImpressionAndClickTracking(false);

    final Map<String, String> extras = adData.getExtras();

    // Set variables from MoPub console.
    final String appId = extras == null ? null : extras.get(FyberMoPubMediationDefs.REMOTE_KEY_APP_ID);
    final String spotId = extras == null ? null : extras.get(FyberMoPubMediationDefs.REMOTE_KEY_SPOT_ID);

    if (TextUtils.isEmpty(spotId)) {
      log("No spotID defined for ad unit. Cannot load interstitial");
      if (mLoadListener != null) {
        mLoadListener.onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
      }
      return;
    }

    mSpotId = spotId;

    // If we've received an appId for this unit, try initializing the Fyber Marketplace SDK, if it was not already initialized
    if (!TextUtils.isEmpty(appId)) {
      FyberAdapterConfiguration.initializeFyberMarketplace(context, appId, extras.containsKey(
              FyberMoPubMediationDefs.REMOTE_KEY_DEBUG),
              new FyberAdapterConfiguration.OnFyberAdapterConfigurationResolvedListener() {
                @Override
                public void onFyberAdapterConfigurationResolved(
                        OnFyberMarketplaceInitializedListener.FyberInitStatus status) {
                  //note - we try to load ads when "FAILED" because an ad request will re-attempt to initialize the relevant parts of the SDK.
                  if (status == OnFyberMarketplaceInitializedListener.FyberInitStatus.SUCCESSFULLY || status == OnFyberMarketplaceInitializedListener.FyberInitStatus.FAILED) {
                    requestInterstitial(context, spotId, extras);
                  } else if (mLoadListener != null) {
                    mLoadListener.onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
                  }
                }
              });
    } else if (InneractiveAdManager.wasInitialized()) {
      requestInterstitial(context, spotId, extras);
    } else if (mLoadListener != null) {
      mLoadListener.onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
    }
  }

  /**
   * Called by the Mopub infra-structure in order for the plugin to start showing a Fyber Marketplace interstitial
   */
  @Override
  public void show() {
    log("show interstitial called");
    // check if the ad is ready
    if (mInterstitialSpot != null && mInterstitialSpot.isReady()) {

      InneractiveFullscreenUnitController fullscreenUnitController = (InneractiveFullscreenUnitController) mInterstitialSpot
              .getSelectedUnitController();
      fullscreenUnitController.setEventsListener(new InneractiveFullscreenAdEventsListener() {

        /**
         * Called by Fyber Marketplace when an interstitial ad activity is closed
         * @param adSpot Spot object
         */
        @Override
        public void onAdDismissed(InneractiveAdSpot adSpot) {
          log("onAdDismissed");
          if (mInteractionListener != null) {
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
          log("onAdClicked");
          if (mInteractionListener != null) {
            mInteractionListener.onAdClicked();
          }
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
          log("Got video content progress: total time = " + totalDurationInMsec
                  + " position = " + positionInMsec);
        }

        /**
         * Called by Fyber Marketplace when an Interstitial video ad was played to the end
         * <br>Can be used for incentive flow
         * <br>Note: This event does not indicate that the interstitial was closed
         */
        @Override
        public void onCompleted() {
          log("Got video content completed event");
        }

        @Override
        public void onPlayerError() {
          log("Got video content play error event");
        }
      });

      // Now add the content controller to the unit controller
      fullscreenUnitController.addContentController(videoContentController);

      fullscreenUnitController.show((Activity) mContext);
    } else {
      if (mInteractionListener != null) {
        mInteractionListener.onAdFailed(MoPubErrorCode.EXPIRED);
      }
      log("The Interstitial ad is not ready yet.");
    }
  }


  /**
   * Called by Mopub when an ad should be destroyed
   * <br>Destroy the underline Fyber Marketplace ad
   */
  @Override
  protected void onInvalidate() {
    log("onInvalidate called by Mopub");
    // We do the cleanup on the event of loadInterstitial.
    if (mInterstitialSpot != null) {
      mInterstitialSpot.destroy();
      mInterstitialSpot = null;
    }
  }

  /**
   * requests an interstitial ad from Fyber Marketplace
   * @param context
   * @param spotId
   * @param localExtras
   */
  private void requestInterstitial(final Context context, String spotId, Map<String, String> localExtras) {
    mContext = context;

    FyberAdapterConfiguration.updateGdprConsentStatusFromMopub();

    if (mInterstitialSpot != null) {
      mInterstitialSpot.destroy();
    }

    mInterstitialSpot = InneractiveAdSpotManager.get().createSpot();
    // Set your mediation name and version
    mInterstitialSpot.setMediationName(InneractiveMediationName.MOPUB);
    mInterstitialSpot.setMediationVersion(MoPub.SDK_VERSION);

    InneractiveFullscreenUnitController fullscreenUnitController = new InneractiveFullscreenUnitController();
    mInterstitialSpot.addUnitController(fullscreenUnitController);

    InneractiveAdRequest request = new InneractiveAdRequest(spotId);
    FyberAdapterConfiguration.updateRequestFromExtras(request, localExtras);

    // Load ad
    mInterstitialSpot.setRequestListener(new InneractiveAdSpot.RequestListener() {

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
      public void onInneractiveFailedAdRequest(InneractiveAdSpot adSpot,
                                               InneractiveErrorCode errorCode) {
        log("Failed loading interstitial with error: " + errorCode);
        if (mLoadListener != null) {
          if (errorCode == InneractiveErrorCode.CONNECTION_ERROR) {
            mLoadListener.onAdLoadFailed(MoPubErrorCode.NO_CONNECTION);
          } else if (errorCode == InneractiveErrorCode.CONNECTION_TIMEOUT) {
            mLoadListener.onAdLoadFailed(MoPubErrorCode.NETWORK_TIMEOUT);
          } else if (errorCode == InneractiveErrorCode.NO_FILL) {
            mLoadListener.onAdLoadFailed(MoPubErrorCode.NO_FILL);
          } else {
            mLoadListener.onAdLoadFailed(MoPubErrorCode.SERVER_ERROR);
          }
        }
      }
    });

    mInterstitialSpot.requestAd(request);
  }

  /**
   * MopubLog helper
   * @param message
   */
  private void log(String message) {
    MoPubLog.log(CUSTOM, LOG_TAG, message);
  }
}
