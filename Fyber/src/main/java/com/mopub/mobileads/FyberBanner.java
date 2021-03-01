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
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fyber.inneractive.sdk.external.InneractiveAdManager;
import com.fyber.inneractive.sdk.external.InneractiveAdRequest;
import com.fyber.inneractive.sdk.external.InneractiveAdSpot;
import com.fyber.inneractive.sdk.external.InneractiveAdSpotManager;
import com.fyber.inneractive.sdk.external.InneractiveAdViewEventsListener;
import com.fyber.inneractive.sdk.external.InneractiveAdViewUnitController;
import com.fyber.inneractive.sdk.external.InneractiveErrorCode;
import com.fyber.inneractive.sdk.external.InneractiveMediationName;
import com.fyber.inneractive.sdk.external.InneractiveUnitController.AdDisplayError;
import com.fyber.inneractive.sdk.external.OnFyberMarketplaceInitializedListener;
import com.fyber.inneractive.sdk.external.WebViewRendererProcessHasGoneError;
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
 * Implements Fyber's banner Mopub's custom event class
 */
public class FyberBanner extends BaseAd {
  // Mopub log tag definition
  private final static String LOG_TAG = "FyberBannerForMopub";

  String mSpotId;

  /**
   * The Spot object for the banner
   */
  InneractiveAdSpot mBannerSpot;

  /**
   * The parent ad layout
   */
  ViewGroup mAdLayout;

  /**
   * Called by the Mopub infra-structure when Mopub requests a banner from Fyber Marketplace
   *
   * @param context
   * @param spotId
   * @param localExtras
   */


  private void requestBanner(final Context context, String spotId, Map<String, String> localExtras) {
    FyberAdapterConfiguration.updateGdprConsentStatusFromMopub();

    mSpotId = spotId;
  
    // Destroy previous ad
    if (mBannerSpot != null) {
      mBannerSpot.destroy();
    }
  
    mBannerSpot = InneractiveAdSpotManager.get().createSpot();
    // Set your mediation name and version
    mBannerSpot.setMediationName(InneractiveMediationName.MOPUB);
    mBannerSpot.setMediationVersion(MoPub.SDK_VERSION);
  
    InneractiveAdViewUnitController controller = new InneractiveAdViewUnitController();
    mBannerSpot.addUnitController(controller);
  
    InneractiveAdRequest request = new InneractiveAdRequest(spotId);
    FyberAdapterConfiguration.updateRequestFromExtras(request, localExtras);
  
    // Load an Ad
    mBannerSpot.setRequestListener(new InneractiveAdSpot.RequestListener() {
      @Override
      public void onInneractiveSuccessfulAdRequest(InneractiveAdSpot adSpot) {
        if (adSpot != mBannerSpot) {
          log("Wrong Banner Spot: Received - " + adSpot + ", Actual - " + mBannerSpot);
          return;
        }
      
        log("on ad loaded successfully");
      
        // Create a parent layout for the Banner Ad
        mAdLayout = new FrameLayout(context);
        InneractiveAdViewUnitController controller = (InneractiveAdViewUnitController) mBannerSpot
                .getSelectedUnitController();
        controller.setEventsListener(new InneractiveAdViewEventsListener() {
          @Override
          public void onAdImpression(InneractiveAdSpot adSpot) {
            log("onAdImpression");
            if (mInteractionListener != null) {
              mInteractionListener.onAdImpression();
            }
          }
        
          @Override
          public void onAdClicked(InneractiveAdSpot adSpot) {
            log("onAdClicked");
            if (mInteractionListener != null) {
              mInteractionListener.onAdClicked();
            }
          }
        
          @Override
          public void onAdWillCloseInternalBrowser(InneractiveAdSpot adSpot) {
            log("onAdWillCloseInternalBrowser");
          }
        
          @Override
          public void onAdWillOpenExternalApp(InneractiveAdSpot adSpot) {
            log("onAdWillOpenExternalApp");
            // customEventListener.onLeaveApplication();
            // Don't call the onLeaveApplication() API since it causes a false Click event on MoPub
          }
        
          @Override
          public void onAdEnteredErrorState(InneractiveAdSpot adSpot, AdDisplayError error) {
            log("onAdEnteredErrorState - " + error.getMessage());
            if (error instanceof WebViewRendererProcessHasGoneError) {
              if (mInteractionListener != null) {
                mInteractionListener.onAdFailed(MoPubErrorCode.RENDER_PROCESS_GONE_UNSPECIFIED);
              } else if (mLoadListener != null) {
                mLoadListener.onAdLoadFailed(MoPubErrorCode.RENDER_PROCESS_GONE_UNSPECIFIED);
              }
            }
          }
        
          @Override
          public void onAdExpanded(InneractiveAdSpot adSpot) {
            log("onAdExpanded");
            if (mInteractionListener != null) {
              mInteractionListener.onAdExpanded();
            }
          }
        
          @Override
          public void onAdResized(InneractiveAdSpot adSpot) {
            log("onAdResized");
          }
        
          @Override
          public void onAdCollapsed(InneractiveAdSpot adSpot) {
            log("onAdCollapsed");
            if (mInteractionListener != null) {
              mInteractionListener.onAdCollapsed();
            }
          }
        });

        controller.bindView(mAdLayout);
        if (mLoadListener != null) {
          mLoadListener.onAdLoaded();
        }
      }
    
      @Override
      public void onInneractiveFailedAdRequest(InneractiveAdSpot adSpot,
                                               InneractiveErrorCode errorCode) {
        log("on ad failed loading with Error: " + errorCode);
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
  
    mBannerSpot.requestAd(request);
  }

  /**
   * Called when an ad view should be cleared
   */
  @Override
  protected void onInvalidate() {
    log("onInvalidate called by Mopub");
    if (mBannerSpot != null) {
      mBannerSpot.destroy();
      mBannerSpot = null;
    }
  }

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

    log("load banner requested");

    setAutomaticImpressionAndClickTracking(false);

    final Map<String, String> extras = adData.getExtras();
    // Set variables from MoPub console.
    final String appId = extras == null ? null : extras.get(FyberMopubMediationDefs.REMOTE_KEY_APP_ID);
    final String spotId = extras == null ? null : extras.get(FyberMopubMediationDefs.REMOTE_KEY_SPOT_ID);

    if (TextUtils.isEmpty(spotId)) {
      log("No spotID defined for ad unit. Cannot load banner");
      if (mLoadListener != null) {
        mLoadListener.onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
      }
      return;
    }

    // If we've received an appId for this unit, try initializing the Fyber Marketplace SDK, if it was not already initialized
    if (!TextUtils.isEmpty(appId)) {
      FyberAdapterConfiguration.initializeFyberMarketplace(context, appId, extras.containsKey(
              FyberMopubMediationDefs.REMOTE_KEY_DEBUG),
              new FyberAdapterConfiguration.OnFyberAdapterConfigurationResolvedListener() {
                @Override
                public void onFyberAdapterConfigurationResolved(
                        OnFyberMarketplaceInitializedListener.FyberInitStatus status) {
                  //note - we try to load ads when "FAILED" because an ad request will re-attempt to initialize the relevant parts of the SDK.
                  if (status == OnFyberMarketplaceInitializedListener.FyberInitStatus.SUCCESSFULLY || status == OnFyberMarketplaceInitializedListener.FyberInitStatus.FAILED) {
                    requestBanner(context, spotId, extras);
                  } else if (mLoadListener != null) {
                      mLoadListener.onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
                  }
                }
              });
    } else if (InneractiveAdManager.wasInitialized()) {
      requestBanner(context, spotId, extras);
    } else if (mLoadListener != null) {
        mLoadListener.onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
    }
  }

  /**
   * Implementers should now show the ad for this base ad. Optional for inline ads that correctly
   * return a view from getAdView
   */
  protected void show() {
    if (mBannerSpot != null && mBannerSpot.getSelectedUnitController() != null && mAdLayout != null) {
      InneractiveAdViewUnitController controller = (InneractiveAdViewUnitController) mBannerSpot
              .getSelectedUnitController();

      controller.bindView(mAdLayout);
    }
  }

  /**
   * Provides the {@link View} of the base ad's ad network. This is required for Inline ads to
   * show correctly, but is otherwise optional.
   *
   * @return a View. Default implementation returns null.
   */
  @Nullable
  protected View getAdView() {
    return mAdLayout;
  }

  /**
   * MopubLog helper
   * @param message
   */
  private void log(String message) {
    MoPubLog.log(CUSTOM, LOG_TAG, message);
  }
}
