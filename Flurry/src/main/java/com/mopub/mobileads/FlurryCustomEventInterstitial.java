package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.flurry.android.ads.FlurryAdErrorType;
import com.flurry.android.ads.FlurryAdInterstitial;
import com.flurry.android.ads.FlurryAdInterstitialListener;
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

class FlurryCustomEventInterstitial extends com.mopub.mobileads.CustomEventInterstitial {
    private static final String ADAPTER_NAME = FlurryCustomEventInterstitial.class.getSimpleName();

    private Context mContext;
    private CustomEventInterstitialListener mListener;

    private String mAdSpaceName;

    private FlurryAdInterstitial mInterstitial;

    @NonNull
    private FlurryAdapterConfiguration mFlurryAdapterConfiguration;

    public FlurryCustomEventInterstitial() {
        mFlurryAdapterConfiguration = new FlurryAdapterConfiguration();
    }

    @Override
    protected void loadInterstitial(Context context,
                                    CustomEventInterstitialListener listener,
                                    Map<String, Object> localExtras,
                                    Map<String, String> serverExtras) {
        if (context == null) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Context cannot be null.");
            listener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);

            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                    MoPubErrorCode.NETWORK_NO_FILL);
            return;
        }

        if (listener == null) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "CustomEventInterstitialListener cannot be null.");
            return;
        }

        if (!(context instanceof Activity)) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Ad can be rendered only in Activity context.");

            listener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);

            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                    MoPubErrorCode.NETWORK_NO_FILL);
            return;
        }

        if (!validateExtras(serverExtras)) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Failed interstitial ad fetch: Missing required server " +
                    "extras [FLURRY_APIKEY and/or FLURRY_ADSPACE].");
            listener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);

            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                    MoPubErrorCode.NETWORK_NO_FILL);

            return;
        }

        setAutomaticImpressionAndClickTracking(false);

        mContext = context;
        mListener = listener;

        String apiKey = serverExtras.get(FlurryAgentWrapper.PARAM_API_KEY);
        mAdSpaceName = serverExtras.get(FlurryAgentWrapper.PARAM_AD_SPACE_NAME);

        mFlurryAdapterConfiguration.setCachedInitializationParameters(context, serverExtras);

        FlurryAgentWrapper.getInstance().startSession(context, apiKey, null);

        MoPubLog.log(CUSTOM, ADAPTER_NAME, "Fetching Flurry ad, ad unit name:" + mAdSpaceName);
        mInterstitial = new FlurryAdInterstitial(mContext, mAdSpaceName);
        mInterstitial.setListener(new FlurryMopubInterstitialListener());
        mInterstitial.fetchAd();

        MoPubLog.log(mAdSpaceName, LOAD_ATTEMPTED, ADAPTER_NAME);
    }

    @Override
    protected void onInvalidate() {
        if (mContext == null) {
            return;
        }

        if (mInterstitial != null) {
            mInterstitial.destroy();
            mInterstitial = null;
        }

        FlurryAgentWrapper.getInstance().endSession(mContext);

        mContext = null;
        mListener = null;
    }

    @Override
    protected void showInterstitial() {
        MoPubLog.log(SHOW_ATTEMPTED, ADAPTER_NAME);

        if (mInterstitial != null) {
            mInterstitial.displayAd();
        } else {
            MoPubLog.log(SHOW_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                    MoPubErrorCode.NETWORK_NO_FILL);

            if (mListener != null) {
                mListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
            }
        }
    }

    private boolean validateExtras(final Map<String, String> serverExtras) {
        if (serverExtras == null) {
            return false;
        }

        final String flurryApiKey = serverExtras.get(FlurryAgentWrapper.PARAM_API_KEY);
        final String flurryAdSpace = serverExtras.get(FlurryAgentWrapper.PARAM_AD_SPACE_NAME);

        MoPubLog.log(CUSTOM, ADAPTER_NAME, "ServerInfo fetched from Mopub " +
                FlurryAgentWrapper.PARAM_API_KEY + " : " + flurryApiKey + " and " +
                FlurryAgentWrapper.PARAM_AD_SPACE_NAME + " :" + flurryAdSpace);

        return (!TextUtils.isEmpty(flurryApiKey) && !TextUtils.isEmpty(flurryAdSpace));
    }

    // FlurryAdListener
    private class FlurryMopubInterstitialListener implements FlurryAdInterstitialListener {

        @Override
        public void onFetched(FlurryAdInterstitial adInterstitial) {
            MoPubLog.log(LOAD_SUCCESS, ADAPTER_NAME);

            if (mListener != null) {
                mListener.onInterstitialLoaded();
            }
        }

        @Override
        public void onRendered(FlurryAdInterstitial adInterstitial) {
            MoPubLog.log(SHOW_SUCCESS, ADAPTER_NAME);

            if (mListener != null) {
                mListener.onInterstitialShown();
            }
        }

        @Override
        public void onDisplay(FlurryAdInterstitial adInterstitial) {
            if (mListener != null) {
                mListener.onInterstitialImpression();
            }
        }

        @Override
        public void onClose(FlurryAdInterstitial adInterstitial) {
            if (mListener != null) {
                mListener.onInterstitialDismissed();
            }
        }

        @Override
        public void onAppExit(FlurryAdInterstitial adInterstitial) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "onAppExit: Flurry interstitial ad exited app");
        }

        @Override
        public void onClicked(FlurryAdInterstitial adInterstitial) {
            MoPubLog.log(CLICKED, ADAPTER_NAME);

            if (mListener != null) {
                mListener.onInterstitialClicked();
            }
        }

        @Override
        public void onVideoCompleted(FlurryAdInterstitial adInterstitial) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "onVideoCompleted: Flurry interstitial ad video completed");
        }

        @Override
        public void onError(FlurryAdInterstitial adInterstitial, FlurryAdErrorType adErrorType,
                            int errorCode) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "onError: Flurry interstitial ad not available. " +
                    "Error type: %s. Error code: %s", adErrorType.toString(), errorCode);

            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                    getMoPubErrorCode(adErrorType).getIntCode(),
                    getMoPubErrorCode(adErrorType));
        }

        private MoPubErrorCode getMoPubErrorCode(FlurryAdErrorType adErrorType) {
            switch (adErrorType) {
                case FETCH:
                    return MoPubErrorCode.NETWORK_NO_FILL;
                case RENDER:
                    return MoPubErrorCode.NETWORK_INVALID_STATE;
                default:
                    return MoPubErrorCode.UNSPECIFIED;
            }
        }
    }
}
