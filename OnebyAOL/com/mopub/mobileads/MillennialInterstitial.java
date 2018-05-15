package com.mopub.mobileads;

import android.app.Activity;
import android.app.Application;
import android.content.Context;

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
import com.mopub.common.privacy.ConsentStatus;
import com.mopub.common.privacy.PersonalInfoManager;

import java.util.Map;

import static com.millennialmedia.MMSDK.setConsentData;
import static com.millennialmedia.MMSDK.setConsentRequired;

/**
 * Compatible with version 6.6 of the Millennial Media SDK.
 */

final class MillennialInterstitial extends CustomEventInterstitial {

    private static final String TAG = MillennialInterstitial.class.getSimpleName();
    private static final String DCN_KEY = "dcn";
    private static final String APID_KEY = "adUnitID";

    private InterstitialAd millennialInterstitial;
    private Context context;
    private CustomEventInterstitialListener interstitialListener;

    static {
        MoPubLog.d("Millennial Media Adapter Version: " + MillennialUtils.MEDIATOR_ID);
    }

    private CreativeInfo getCreativeInfo() {
        if (millennialInterstitial == null) {
            return null;
        }
        return millennialInterstitial.getCreativeInfo();
    }

    @Override
    protected void loadInterstitial(final Context context,
                                    final CustomEventInterstitialListener customEventInterstitialListener, final Map<String, Object> localExtras,
                                    final Map<String, String> serverExtras) {
        interstitialListener = customEventInterstitialListener;
        this.context = context;

        if (context instanceof Activity) {
            try {
                MMSDK.initialize((Activity) context, ActivityListenerManager.LifecycleState.RESUMED);

                PersonalInfoManager personalInfoManager = MoPub.getPersonalInformationManager();

                if (personalInfoManager != null) {
                    boolean gdprApplies = personalInfoManager.gdprApplies();

                    // Set if GDPR applies / if consent is required
                    setConsentRequired(gdprApplies);

                    // Pass the user consent from the MoPub SDK to One by AOL as per GDPR
                    if (personalInfoManager.getPersonalInfoConsentStatus() == ConsentStatus.EXPLICIT_YES) {
                        setConsentData("mopub", "1");
                    }
                }
            } catch (IllegalStateException e) {
                MoPubLog.d("Exception occurred initializing the MM SDK.", e);
                interstitialListener.onInterstitialFailed(MoPubErrorCode.INTERNAL_ERROR);

                return;
            }
        } else if (context instanceof Application) {
            try {
                MMSDK.initialize((Application) context);
            } catch (MMException e) {
                MoPubLog.d("Exception occurred initializing the MM SDK.", e);
                interstitialListener.onInterstitialFailed(MoPubErrorCode.INTERNAL_ERROR);

                return;
            }
        } else {
            MoPubLog.d("MM SDK must be initialized with an Activity or Application context.");
            interstitialListener.onInterstitialFailed(MoPubErrorCode.INTERNAL_ERROR);

            return;
        }

        String apid = serverExtras.get(APID_KEY);

        if (MillennialUtils.isEmpty(apid)) {
            MoPubLog.d("Invalid extras-- Be sure you have an placement ID specified.");
            interstitialListener.onInterstitialFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);

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
        } catch (MMException e) {
            MoPubLog.d("Exception occurred while obtaining an interstitial from MM SDK.", e);
            interstitialListener.onInterstitialFailed(MoPubErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    protected void showInterstitial() {
        if (millennialInterstitial.isReady()) {
            try {
                millennialInterstitial.show(context);
            } catch (MMException e) {
                MoPubLog.d("An exception occurred while attempting to show interstitial.", e);
                interstitialListener.onInterstitialFailed(MoPubErrorCode.INTERNAL_ERROR);
            }
        } else {
            MoPubLog.d("showInterstitial called but interstitial is not ready.");
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
            MoPubLog.d("Millennial Interstitial Ad - Leaving application");
        }

        @Override
        public void onClicked(InterstitialAd interstitialAd) {
            MoPubLog.d("Millennial Interstitial Ad - Ad was clicked");
            MillennialUtils.postOnUiThread(new Runnable() {
                @Override
                public void run() {
                    interstitialListener.onInterstitialClicked();
                }
            });
        }

        @Override
        public void onClosed(InterstitialAd interstitialAd) {
            MoPubLog.d("Millennial Interstitial Ad - Ad was closed");
            MillennialUtils.postOnUiThread(new Runnable() {
                @Override
                public void run() {
                    interstitialListener.onInterstitialDismissed();
                }
            });
        }

        @Override
        public void onExpired(InterstitialAd interstitialAd) {
            MoPubLog.d("Millennial Interstitial Ad - Ad expired");
            MillennialUtils.postOnUiThread(new Runnable() {
                @Override
                public void run() {
                    interstitialListener.onInterstitialFailed(MoPubErrorCode.NO_FILL);
                }
            });
        }

        @Override
        public void onLoadFailed(InterstitialAd interstitialAd, InterstitialErrorStatus interstitialErrorStatus) {
            MoPubLog.d("Millennial Interstitial Ad - load failed (" + interstitialErrorStatus.getErrorCode() + "): " +
                    interstitialErrorStatus.getDescription());

            final MoPubErrorCode moPubErrorCode;

            switch (interstitialErrorStatus.getErrorCode()) {
                case InterstitialErrorStatus.ALREADY_LOADED:
                    // This will generate discrepancies, as requests will NOT be sent to Millennial.
                    interstitialListener.onInterstitialLoaded();
                    MoPubLog.d("Millennial Interstitial Ad - Attempted to load ads when ads are already loaded.");
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
                    interstitialListener.onInterstitialFailed(moPubErrorCode);
                }
            });
        }

        @Override
        public void onLoaded(InterstitialAd interstitialAd) {
            MoPubLog.d("Millennial Interstitial Ad - Ad loaded splendidly");

            CreativeInfo creativeInfo = getCreativeInfo();

            if ((creativeInfo != null) && MMLog.isDebugEnabled()) {
                MoPubLog.d("Interstitial Creative Info: " + creativeInfo);
            }

            MillennialUtils.postOnUiThread(new Runnable() {
                @Override
                public void run() {
                    interstitialListener.onInterstitialLoaded();
                }
            });
        }

        @Override
        public void onShowFailed(InterstitialAd interstitialAd, InterstitialErrorStatus interstitialErrorStatus) {
            MoPubLog.d("Millennial Interstitial Ad - Show failed (" + interstitialErrorStatus.getErrorCode() + "): " +
                    interstitialErrorStatus.getDescription());

            MillennialUtils.postOnUiThread(new Runnable() {
                @Override
                public void run() {
                    interstitialListener.onInterstitialFailed(MoPubErrorCode.INTERNAL_ERROR);
                }
            });
        }

        @Override
        public void onShown(InterstitialAd interstitialAd) {
            MoPubLog.d("Millennial Interstitial Ad - Ad shown");
            MillennialUtils.postOnUiThread(new Runnable() {
                @Override
                public void run() {
                    interstitialListener.onInterstitialShown();
                }
            });
        }
    }
}