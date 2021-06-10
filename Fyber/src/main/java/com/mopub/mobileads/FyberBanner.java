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

import static com.mopub.common.DataKeys.ADUNIT_FORMAT;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;
import static com.mopub.mobileads.MoPubErrorCode.INLINE_LOAD_ERROR;

public class FyberBanner extends BaseAd {

  private static final String ADAPTER_NAME = FyberBanner.class.getSimpleName();
  private final FyberAdapterConfiguration mFyberAdapterConfiguration;

  public FyberBanner() {
    mFyberAdapterConfiguration = new FyberAdapterConfiguration();
  }

  private String mSpotId;
  private InneractiveAdSpot mBannerSpot;
  private ViewGroup mAdLayout;

  private void requestBanner(final Context context, String spotId, Map<String, String> localExtras) {

    mSpotId = spotId;

    if (mBannerSpot != null) {
      mBannerSpot.destroy();
      mBannerSpot = null;
    }

    mBannerSpot = InneractiveAdSpotManager.get().createSpot();
    mBannerSpot.setMediationName(InneractiveMediationName.MOPUB);
    mBannerSpot.setMediationVersion(MoPub.SDK_VERSION);
  
    InneractiveAdViewUnitController controller = new InneractiveAdViewUnitController();
    mBannerSpot.addUnitController(controller);
  
    final InneractiveAdRequest request = new InneractiveAdRequest(spotId);
    FyberAdapterConfiguration.updateRequestFromExtras(request, localExtras);

    mBannerSpot.setRequestListener(new InneractiveAdSpot.RequestListener() {
      @Override
      public void onInneractiveSuccessfulAdRequest(InneractiveAdSpot adSpot) {
        if (adSpot != mBannerSpot) {
          MoPubLog.log(getAdNetworkId(), LOAD_FAILED, "Wrong Banner Spot received - " + adSpot + ", Actual - " + mBannerSpot);
          if (mLoadListener != null) {
            mLoadListener.onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
          }
          return;
        }

        mAdLayout = new FrameLayout(context);
        InneractiveAdViewUnitController controller = (InneractiveAdViewUnitController) mBannerSpot
                .getSelectedUnitController();
        controller.setEventsListener(new InneractiveAdViewEventsListener() {
          @Override
          public void onAdImpression(InneractiveAdSpot adSpot) {
            MoPubLog.log(getAdNetworkId(), LOAD_SUCCESS, ADAPTER_NAME);
            MoPubLog.log(getAdNetworkId(), SHOW_SUCCESS, ADAPTER_NAME);

            if (mInteractionListener != null) {
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
          public void onAdWillCloseInternalBrowser(InneractiveAdSpot adSpot) {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "onAdWillCloseInternalBrowser");
          }
        
          @Override
          public void onAdWillOpenExternalApp(InneractiveAdSpot adSpot) {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "onAdWillOpenExternalApp");
            // Don't call the onLeaveApplication() API since it causes a false Click event on MoPub
          }
        
          @Override
          public void onAdEnteredErrorState(InneractiveAdSpot adSpot, AdDisplayError error) {
            MoPubLog.log(CUSTOM, "onAdEnteredErrorState - " + error.getMessage());

            if (error instanceof WebViewRendererProcessHasGoneError) {
              if (mInteractionListener != null) {
                MoPubLog.log(getAdNetworkId(), SHOW_FAILED, ADAPTER_NAME);
                mInteractionListener.onAdFailed(MoPubErrorCode.RENDER_PROCESS_GONE_UNSPECIFIED);
              } else if (mLoadListener != null) {
                MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME);
                mLoadListener.onAdLoadFailed(MoPubErrorCode.RENDER_PROCESS_GONE_UNSPECIFIED);
              }
            }
          }
        
          @Override
          public void onAdExpanded(InneractiveAdSpot adSpot) {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "onAdExpanded");

            if (mInteractionListener != null) {
              mInteractionListener.onAdExpanded();
            }
          }
        
          @Override
          public void onAdResized(InneractiveAdSpot adSpot) {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "onAdResized");
          }
        
          @Override
          public void onAdCollapsed(InneractiveAdSpot adSpot) {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "onAdCollapsed");

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
        MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME, INLINE_LOAD_ERROR.getIntCode(),
                errorCode);

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

  @Override
  protected void onInvalidate() {

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
    return mSpotId == null ? "" : mSpotId;
  }

  @Override
  protected boolean checkAndInitializeSdk(@NonNull Activity launcherActivity, @NonNull AdData adData) throws Exception {
    return false;
  }

  @Override
  protected void load(final @NonNull Context context, @NonNull AdData adData) throws Exception {
    Preconditions.checkNotNull(context);
    Preconditions.checkNotNull(adData);

    setAutomaticImpressionAndClickTracking(false);

    final Map<String, String> extras = adData.getExtras();
    final String appId = extras == null ? null : extras.get(FyberMoPubMediationDefs.REMOTE_KEY_APP_ID);
    final String spotId = extras == null ? null : extras.get(FyberMoPubMediationDefs.REMOTE_KEY_SPOT_ID);

    mFyberAdapterConfiguration.setCachedInitializationParameters(context, extras);

    String adUnitFormat = extras.get(ADUNIT_FORMAT);
    if (!TextUtils.isEmpty(adUnitFormat)) {
      adUnitFormat = adUnitFormat.toLowerCase();
    }

    final boolean isBannerFormat = "banner".equalsIgnoreCase(adUnitFormat);
    final boolean isMrectFormat = "medium_rectangle".equalsIgnoreCase(adUnitFormat);

    if (!isBannerFormat && !isMrectFormat) {
      MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME,
              "Fyber only supports 320*50, 728*90 and 300*250 sized ads. " +
                      "Please ensure your MoPub ad unit's format is either Banner or Medium Rectangle.");
      MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
              MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR.getIntCode(),
              MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);

      if (mLoadListener != null) {
        mLoadListener.onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
      }
      return;
    }

    if (TextUtils.isEmpty(spotId)) {
      MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
              MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR.getIntCode(),
              MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);

      if (mLoadListener != null) {
        mLoadListener.onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
      }
      return;
    }

    if (!TextUtils.isEmpty(appId)) {
      FyberAdapterConfiguration.initializeFyberMarketplace(context, appId, extras.containsKey(
              FyberMoPubMediationDefs.REMOTE_KEY_DEBUG),
              new FyberAdapterConfiguration.OnFyberAdapterConfigurationResolvedListener() {
                @Override
                public void onFyberAdapterConfigurationResolved(
                        OnFyberMarketplaceInitializedListener.FyberInitStatus status) {
                  //note - Fyber tries to load ads when "FAILED" because an ad request will re-attempt to initialize the relevant parts of the SDK.
                  if (status == OnFyberMarketplaceInitializedListener.FyberInitStatus.SUCCESSFULLY
                          || status == OnFyberMarketplaceInitializedListener.FyberInitStatus.FAILED) {
                    requestBanner(context, spotId, extras);
                  } else if (mLoadListener != null) {
                    MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME, INLINE_LOAD_ERROR.getIntCode(),
                            INLINE_LOAD_ERROR);
                      mLoadListener.onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
                  }
                }
              });
    } else if (InneractiveAdManager.wasInitialized()) {
      requestBanner(context, spotId, extras);
    } else if (mLoadListener != null) {
        MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
              MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR.getIntCode(),
              MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
        mLoadListener.onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
    }
  }

  /**
   * Implementers should now show the ad for this base ad. Optional for inline ads that correctly
   * return a view from getAdView
   */
  protected void show() {
    if (mBannerSpot != null && mBannerSpot.getSelectedUnitController() != null && mAdLayout != null) {
      final InneractiveAdViewUnitController controller = (InneractiveAdViewUnitController) mBannerSpot
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

}
