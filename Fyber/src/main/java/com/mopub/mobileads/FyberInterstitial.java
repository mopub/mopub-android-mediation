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

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;
import static com.mopub.mobileads.MoPubErrorCode.FULLSCREEN_LOAD_ERROR;
import static com.mopub.mobileads.MoPubErrorCode.INLINE_LOAD_ERROR;

public class FyberInterstitial extends BaseAd {

  private static final String ADAPTER_NAME = FyberInterstitial.class.getSimpleName();
  private final FyberAdapterConfiguration mFyberAdapterConfiguration;

  public FyberInterstitial() {
    mFyberAdapterConfiguration = new FyberAdapterConfiguration();
  }

  InneractiveAdSpot mInterstitialSpot;
  private String mSpotId;

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

    mContext = context;
    setAutomaticImpressionAndClickTracking(false);

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
      FyberAdapterConfiguration.initializeFyberMarketplace(context, appId, extras.containsKey(
              FyberMoPubMediationDefs.REMOTE_KEY_DEBUG),
              new FyberAdapterConfiguration.OnFyberAdapterConfigurationResolvedListener() {
                @Override
                public void onFyberAdapterConfigurationResolved(
                        OnFyberMarketplaceInitializedListener.FyberInitStatus status) {
                  //note - Fyber tries to load ads when "FAILED" because an ad request will re-attempt to initialize the relevant parts of the SDK.
                  if (status == OnFyberMarketplaceInitializedListener.FyberInitStatus.SUCCESSFULLY || status == OnFyberMarketplaceInitializedListener.FyberInitStatus.FAILED) {
                    requestInterstitial(context, spotId, extras);
                  } else if (mLoadListener != null) {
                    MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                            MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR.getIntCode(),
                            MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
                    mLoadListener.onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
                  }
                }
              });
    } else if (InneractiveAdManager.wasInitialized()) {
      MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);
      requestInterstitial(context, spotId, extras);
    } else if (mLoadListener != null) {
      MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
              MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR.getIntCode(),
              MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
      mLoadListener.onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
    }

    mFyberAdapterConfiguration.setCachedInitializationParameters(context, extras);
  }

  @Override
  public void show() {
    MoPubLog.log(getAdNetworkId(), SHOW_ATTEMPTED, ADAPTER_NAME);

    if (mInterstitialSpot != null && mInterstitialSpot.isReady()) {

      final InneractiveFullscreenUnitController fullscreenUnitController = (InneractiveFullscreenUnitController) mInterstitialSpot
              .getSelectedUnitController();
      fullscreenUnitController.setEventsListener(new InneractiveFullscreenAdEventsListener() {

        @Override
        public void onAdDismissed(InneractiveAdSpot adSpot) {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Fyber interstitial is dismissed");

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
          MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Got video content progress: total time = " +
                  totalDurationInMsec + " position = " + positionInMsec);
        }

        @Override
        public void onCompleted() {
          MoPubLog.log(CUSTOM, ADAPTER_NAME, "Video content completed event");
        }

        @Override
        public void onPlayerError() {
          MoPubLog.log(CUSTOM, ADAPTER_NAME, "Video content play error event");
        }
      });

      fullscreenUnitController.addContentController(videoContentController);

      fullscreenUnitController.show((Activity) mContext);
    } else {
      MoPubLog.log(getAdNetworkId(), SHOW_FAILED, ADAPTER_NAME);

      if (mInteractionListener != null) {
        mInteractionListener.onAdFailed(MoPubErrorCode.EXPIRED);
      }
    }
  }

  protected void onInvalidate() {
    if (mInterstitialSpot != null) {
      mInterstitialSpot.destroy();
      mInterstitialSpot = null;
    }
  }

  private void requestInterstitial(final Context context, String spotId, Map<String, String> localExtras) {
    mContext = context;

    if (mInterstitialSpot != null) {
      mInterstitialSpot.destroy();
    }

    mInterstitialSpot = InneractiveAdSpotManager.get().createSpot();
    mInterstitialSpot.setMediationName(InneractiveMediationName.MOPUB);
    mInterstitialSpot.setMediationVersion(MoPub.SDK_VERSION);

    InneractiveFullscreenUnitController fullscreenUnitController = new InneractiveFullscreenUnitController();
    mInterstitialSpot.addUnitController(fullscreenUnitController);

    final InneractiveAdRequest request = new InneractiveAdRequest(spotId);
    FyberAdapterConfiguration.updateRequestFromExtras(request, localExtras);

    mInterstitialSpot.setRequestListener(new InneractiveAdSpot.RequestListener() {

      @Override
      public void onInneractiveSuccessfulAdRequest(InneractiveAdSpot adSpot) {
        MoPubLog.log(getAdNetworkId(), LOAD_SUCCESS, ADAPTER_NAME);

        if (mLoadListener != null) {
          mLoadListener.onAdLoaded();
        }
      }

      @Override
      public void onInneractiveFailedAdRequest(InneractiveAdSpot adSpot,
                                               InneractiveErrorCode errorCode) {
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

    mInterstitialSpot.requestAd(request);
  }
}
