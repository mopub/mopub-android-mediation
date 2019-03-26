package com.mopub.mobileads;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;

import com.millennialmedia.AppInfo;
import com.millennialmedia.CreativeInfo;
import com.millennialmedia.InterstitialAd;
import com.millennialmedia.InterstitialAd.InterstitialErrorStatus;
import com.millennialmedia.InterstitialAd.InterstitialListener;
import com.millennialmedia.MMException;
import com.millennialmedia.MMLog;
import com.millennialmedia.MMSDK;
import com.millennialmedia.internal.ActivityListenerManager;
import com.mopub.common.MoPub;
import com.mopub.common.logging.MoPubLog;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM_WITH_THROWABLE;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.EXPIRED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;

final class MillennialInterstitial extends CustomEventInterstitial {

    private static final String DCN_KEY = "dcn";
    private static final String APID_KEY = "adUnitID";
    private static final String ADAPTER_NAME = MillennialInterstitial.class.getSimpleName();

    private InterstitialAd millennialInterstitial;
    private Context context;
    private CustomEventInterstitialListener interstitialListener;
    @NonNull
    private MillennialAdapterConfiguration mMillennialAdapterConfiguration;

    static {
        MoPubLog.log(CUSTOM, ADAPTER_NAME, "Millennial Media Adapter Version: " + MillennialUtils.MEDIATOR_ID);
    }

    private CreativeInfo getCreativeInfo() {
        if (millennialInterstitial == null) {
            return null;
        }
        return millennialInterstitial.getCreativeInfo();
    }

    public MillennialInterstitial() {
        mMillennialAdapterConfiguration = new MillennialAdapterConfiguration();
    }

    @Override
    protected void loadInterstitial(final Context context,
                                    final CustomEventInterstitialListener customEventInterstitialListener, final Map<String, Object> localExtras,
                                    final Map<String, String> serverExtras) {

        setAutomaticImpressionAndClickTracking(false);

        interstitialListener = customEventInterstitialListener;
        this.context = context;

        if (context instanceof Activity) {
            try {
                MMSDK.initialize((Activity) context, ActivityListenerManager.LifecycleState.RESUMED);
                mMillennialAdapterConfiguration.setCachedInitializationParameters(context, serverExtras);
            } catch (IllegalStateException e) {
                MoPubLog.log(CUSTOM_WITH_THROWABLE, "Exception occurred initializing the " +
                        "MM SDK.", e);
                MoPubLog.log(LOAD_FAILED,
                        ADAPTER_NAME,
                        MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                        MoPubErrorCode.NETWORK_NO_FILL);

                if (interstitialListener != null) {
                    interstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
                }

                return;
            }
        } else if (context instanceof Application) {
            try {
                MMSDK.initialize((Application) context);
                mMillennialAdapterConfiguration.setCachedInitializationParameters(context, serverExtras);
            } catch (MMException e) {
                MoPubLog.log(CUSTOM_WITH_THROWABLE, "Exception occurred initializing the " +
                        "MM SDK.", e);
                MoPubLog.log(LOAD_FAILED,
                        ADAPTER_NAME,
                        MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                        MoPubErrorCode.NETWORK_NO_FILL);

                if (interstitialListener != null) {
                    interstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
                }

                return;
            }
        } else {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "MM SDK must be initialized with an Activity or " +
                    "Application context.");
            MoPubLog.log(LOAD_FAILED,
                    ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                    MoPubErrorCode.NETWORK_NO_FILL);

            if (interstitialListener != null) {
                interstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
            }

            return;
        }

        String apid = serverExtras.get(APID_KEY);

        if (MillennialUtils.isEmpty(apid)) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Invalid extras-- Be sure you have an placement ID specified.");
            MoPubLog.log(LOAD_FAILED,
                    ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                    MoPubErrorCode.NETWORK_NO_FILL);

            if (interstitialListener != null) {
                interstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
            }

            return;
        }

        // Add DCN support
        String dcn = serverExtras.get(DCN_KEY);

        AppInfo ai = new AppInfo().setMediator(MillennialUtils.MEDIATOR_ID);
        if (!MillennialUtils.isEmpty(dcn)) {
            ai.setSiteId(dcn);
        }

        try {
            MMSDK.setAppInfo(ai);
            MMSDK.setLocationEnabled(MoPub.getLocationAwareness() != MoPub.LocationAwareness.DISABLED);
            millennialInterstitial = InterstitialAd.createInstance(apid);
            millennialInterstitial.setListener(new MillennialInterstitialListener());
            millennialInterstitial.load(context, null);

            MoPubLog.log(apid, LOAD_ATTEMPTED, ADAPTER_NAME);
        } catch (MMException e) {
            MoPubLog.log(CUSTOM_WITH_THROWABLE, "Exception occurred while obtaining an " +
                    "interstitial from MM SDK.", e);
            MoPubLog.log(LOAD_FAILED,
                    ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                    MoPubErrorCode.NETWORK_NO_FILL);

            if (interstitialListener != null) {
                interstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
            }
        }
    }

    @Override
    protected void showInterstitial() {
        MoPubLog.log(SHOW_ATTEMPTED, ADAPTER_NAME);

        if (millennialInterstitial.isReady()) {
            try {
                millennialInterstitial.show(context);
            } catch (MMException e) {
                MoPubLog.log(CUSTOM_WITH_THROWABLE, "An exception occurred while attempting " +
                        "to show interstitial.", e);
                MoPubLog.log(SHOW_FAILED,
                        ADAPTER_NAME,
                        MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                        MoPubErrorCode.NETWORK_NO_FILL);

                if (interstitialListener != null) {
                    interstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
                }
            }
        } else {
            MoPubLog.log(SHOW_FAILED,
                    ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                    MoPubErrorCode.NETWORK_NO_FILL);

            MoPubLog.log(CUSTOM, ADAPTER_NAME, "showInterstitial called but interstitial is not ready.");
            interstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
        }
    }

    @Override
    protected void onInvalidate() {
        if (millennialInterstitial != null) {
            millennialInterstitial.destroy();
            millennialInterstitial = null;
        }
    }

    class MillennialInterstitialListener implements InterstitialListener {

        @Override
        public void onAdLeftApplication(InterstitialAd interstitialAd) {
            // onLeaveApplication is an alias to on clicked. We are not required to call this.
        }

        @Override
        public void onClicked(InterstitialAd interstitialAd) {
            MillennialUtils.postOnUiThread(new Runnable() {
                @Override
                public void run() {
                    MoPubLog.log(CLICKED, ADAPTER_NAME);

                    if (interstitialListener != null) {
                        interstitialListener.onInterstitialClicked();
                    }
                }
            });
        }

        @Override
        public void onClosed(InterstitialAd interstitialAd) {
            MillennialUtils.postOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (interstitialListener != null) {
                        interstitialListener.onInterstitialDismissed();
                    }
                }
            });
        }

        @Override
        public void onExpired(InterstitialAd interstitialAd) {
            MoPubLog.log(EXPIRED, ADAPTER_NAME);

            MillennialUtils.postOnUiThread(new Runnable() {
                @Override
                public void run() {
                    MoPubLog.log(LOAD_FAILED,
                            ADAPTER_NAME,
                            MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                            MoPubErrorCode.NETWORK_NO_FILL);

                    if (interstitialListener != null) {
                        interstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
                    }
                }
            });
        }

        @Override
        public void onLoadFailed(InterstitialAd interstitialAd, InterstitialErrorStatus interstitialErrorStatus) {

            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Millennial Interstitial Ad - load failed (" +
                    interstitialErrorStatus.getErrorCode() + "): " +
                    interstitialErrorStatus.getDescription());

            final MoPubErrorCode moPubErrorCode;

            switch (interstitialErrorStatus.getErrorCode()) {
                case InterstitialErrorStatus.ALREADY_LOADED:
                    // This will generate discrepancies, as requests will NOT be sent to Millennial.
                    MoPubLog.log(LOAD_SUCCESS, ADAPTER_NAME);

                    if (interstitialListener != null) {
                        interstitialListener.onInterstitialLoaded();
                    }
                    MoPubLog.log(CUSTOM, ADAPTER_NAME, "Millennial Interstitial Ad - Attempted to load ads " +
                            "when ads are already loaded.");
                    return;
                case InterstitialErrorStatus.EXPIRED:
                case InterstitialErrorStatus.DISPLAY_FAILED:
                case InterstitialErrorStatus.INIT_FAILED:
                case InterstitialErrorStatus.ADAPTER_NOT_FOUND:
                    moPubErrorCode = MoPubErrorCode.INTERNAL_ERROR;
                    break;
                case InterstitialErrorStatus.NO_NETWORK:
                    moPubErrorCode = MoPubErrorCode.NO_CONNECTION;
                    break;
                case InterstitialErrorStatus.UNKNOWN:
                    moPubErrorCode = MoPubErrorCode.UNSPECIFIED;
                    break;
                case InterstitialErrorStatus.NOT_LOADED:
                case InterstitialErrorStatus.LOAD_FAILED:
                default:
                    moPubErrorCode = MoPubErrorCode.NETWORK_NO_FILL;
            }

            MillennialUtils.postOnUiThread(new Runnable() {
                @Override
                public void run() {
                    MoPubLog.log(LOAD_FAILED,
                            ADAPTER_NAME,
                            moPubErrorCode.getIntCode(),
                            moPubErrorCode);

                    if (interstitialListener != null) {
                        interstitialListener.onInterstitialFailed(moPubErrorCode);
                    }
                }
            });
        }

        @Override
        public void onLoaded(InterstitialAd interstitialAd) {
            CreativeInfo creativeInfo = getCreativeInfo();

            if ((creativeInfo != null) && MMLog.isDebugEnabled()) {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "Interstitial Creative Info: " + creativeInfo);
            }

            MillennialUtils.postOnUiThread(new Runnable() {
                @Override
                public void run() {
                    MoPubLog.log(LOAD_SUCCESS, ADAPTER_NAME);

                    if (interstitialListener != null) {
                        interstitialListener.onInterstitialLoaded();
                    }
                }
            });
        }

        @Override
        public void onShowFailed(InterstitialAd interstitialAd, InterstitialErrorStatus interstitialErrorStatus) {

            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Millennial Interstitial Ad - Show failed (" +
                    interstitialErrorStatus.getErrorCode() + "): " +
                    interstitialErrorStatus.getDescription());

            MillennialUtils.postOnUiThread(new Runnable() {
                @Override
                public void run() {
                    MoPubLog.log(LOAD_FAILED,
                            ADAPTER_NAME,
                            MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                            MoPubErrorCode.NETWORK_NO_FILL);

                    if (interstitialListener != null) {
                        interstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
                    }
                }
            });
        }

        @Override
        public void onShown(InterstitialAd interstitialAd) {
            MillennialUtils.postOnUiThread(new Runnable() {
                @Override
                public void run() {
                    MoPubLog.log(SHOW_SUCCESS, ADAPTER_NAME);

                    if (interstitialListener != null) {
                        interstitialListener.onInterstitialShown();
                        interstitialListener.onInterstitialImpression();
                    }
                }
            });
        }
    }
}