package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.sdk.ISDemandOnlyInterstitialListener;
import com.mopub.common.LifecycleListener;
import com.mopub.common.MoPub;
import com.mopub.common.MoPubLifecycleManager;
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

public class IronSourceInterstitial extends CustomEventInterstitial implements ISDemandOnlyInterstitialListener {

    /**
     * private vars
     */

    // Configuration keys
    private static final String APPLICATION_KEY = "applicationKey";
    private static final String INSTANCE_ID_KEY = "instanceId";
    private static final String MEDIATION_TYPE = "mopub";
    private static final String ADAPTER_NAME = IronSourceInterstitial.class.getSimpleName();

    private static Handler sHandler;

    private static CustomEventInterstitialListener mMoPubListener;

    // Network identifier of ironSource
    private String mInstanceId = IronSourceAdapterConfiguration.DEFAULT_INSTANCE_ID;

    @NonNull
    protected String getAdNetworkId() {
        return mInstanceId;
    }

    @NonNull
    private IronSourceAdapterConfiguration mIronSourceAdapterConfiguration;

    /**
     * Mopub API
     */

    public IronSourceInterstitial() {
        mIronSourceAdapterConfiguration = new IronSourceAdapterConfiguration();
    }

    @Override
    protected void loadInterstitial(Context context, CustomEventInterstitialListener customEventInterstitialListener, Map<String, Object> map0, Map<String, String> serverExtras) {
        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "loadInterstitial");

        MoPubLifecycleManager.getInstance((Activity) context).addLifecycleListener(lifecycleListener);
        // Pass the user consent from the MoPub SDK to ironSource as per GDPR
        boolean canCollectPersonalInfo = MoPub.canCollectPersonalInformation();
        IronSource.setConsent(canCollectPersonalInfo);

        try {
            String applicationKey = "";
            mMoPubListener = customEventInterstitialListener;
            sHandler = new Handler(Looper.getMainLooper());

            if (!(context instanceof Activity)) {
                // Context not an Activity context, log the reason for failure and fail the
                // initialization.

                MoPubLog.log(CUSTOM, ADAPTER_NAME, "ironSource load interstitial must be called from an " +
                        "Activity context");
                sendMoPubInterstitialFailed(MoPubErrorCode.INTERNAL_ERROR, mInstanceId);

                return;
            }

            if (serverExtras != null) {
                if (serverExtras.get(APPLICATION_KEY) != null) {
                    applicationKey = serverExtras.get(APPLICATION_KEY);
                }

                if (serverExtras.get(INSTANCE_ID_KEY) != null) {
                    if (!TextUtils.isEmpty(serverExtras.get(INSTANCE_ID_KEY))) {
                        mInstanceId = serverExtras.get(INSTANCE_ID_KEY);
                    }
                }
            } else {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "serverExtras is null. Make sure you have entered ironSource's"
                    +" application and instance keys on the MoPub dashboard");
                sendMoPubInterstitialFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR, mInstanceId);

                return;
            }

            if (!TextUtils.isEmpty(applicationKey)) {
                initIronSourceSDK(((Activity) context), applicationKey);
                loadInterstitial(mInstanceId);

                mIronSourceAdapterConfiguration.setCachedInitializationParameters(context, serverExtras);
            } else {
                 MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "ironSource initialization failed, make sure that"+
                        " 'applicationKey' server parameter is added");
                sendMoPubInterstitialFailed(MoPubErrorCode.INTERNAL_ERROR, getAdNetworkId());
            }

        } catch (Exception e) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, e);
            sendMoPubInterstitialFailed(MoPubErrorCode.INTERNAL_ERROR, mInstanceId);
        }
    }

    @Override
    protected void showInterstitial() {
        MoPubLog.log(getAdNetworkId(), SHOW_ATTEMPTED, ADAPTER_NAME);
        if (mInstanceId != null) {
            IronSource.showISDemandOnlyInterstitial(mInstanceId);
        } else {
            MoPubLog.log(getAdNetworkId(), SHOW_FAILED, ADAPTER_NAME);
        }
    }

    @Override
    protected void onInvalidate() {
    }

    /**
     * Class Helper Methods
     **/

    private void initIronSourceSDK(Activity activity, String appKey) {
        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "ironSource Interstitial initialization is called with appkey: " + appKey);

        IronSource.setISDemandOnlyInterstitialListener(this);
        IronSource.setMediationType(MEDIATION_TYPE + IronSourceAdapterConfiguration.IRONSOURCE_ADAPTER_VERSION + 
            "SDK" + IronSourceAdapterConfiguration.getMoPubSdkVersion());
        IronSource.initISDemandOnly(activity, appKey, IronSource.AD_UNIT.INTERSTITIAL);

    }

    private void loadInterstitial(String instanceId) {
        MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);
        mInstanceId = instanceId;
        IronSource.loadISDemandOnlyInterstitial(instanceId);
    }

    private void sendMoPubInterstitialFailed(final MoPubErrorCode errorCode, final String instanceId) {
        sHandler.post(new Runnable() {
            @Override
            public void run() {
                MoPubLog.log(instanceId, LOAD_FAILED, ADAPTER_NAME,
                        errorCode.getIntCode(),
                        errorCode);

                if (mMoPubListener != null) {
                    mMoPubListener.onInterstitialFailed(errorCode);
                }
            }
        });
    }

    /**
     * ironSource Interstitial Listener
     **/

    @Override
    public void onInterstitialAdReady(final String instanceId) {
        MoPubLog.log(CUSTOM, ADAPTER_NAME, "ironSource Interstitial loaded successfully for instance " +
                instanceId + " (current instance: " + mInstanceId + " )");

        sHandler.post(new Runnable() {
            @Override
            public void run() {
                MoPubLog.log(instanceId, LOAD_SUCCESS, ADAPTER_NAME);

                if (mMoPubListener != null) {
                    mMoPubListener.onInterstitialLoaded();
                }
            }
        });
    }

    @Override
    public void onInterstitialAdLoadFailed(String instanceId, IronSourceError ironSourceError) {
        MoPubLog.log(CUSTOM, ADAPTER_NAME, "ironSource Interstitial failed to load for instance " +
                instanceId + " (current instance: " + mInstanceId + " )" + " Error: " + ironSourceError.getErrorMessage());

        sendMoPubInterstitialFailed(IronSourceAdapterConfiguration.getMoPubErrorCode(ironSourceError), instanceId);
    }

    @Override
    public void onInterstitialAdOpened(final String instanceId) {
        MoPubLog.log(CUSTOM, ADAPTER_NAME, "ironSource Interstitial opened ad for instance "
            + instanceId + " (current instance: " + mInstanceId + " )");
        sHandler.post(new Runnable() {
            @Override
            public void run() {
                MoPubLog.log(instanceId, SHOW_SUCCESS, ADAPTER_NAME);

                if (mMoPubListener != null) {
                    mMoPubListener.onInterstitialShown();
                }
            }
        });
    }

    @Override
    public void onInterstitialAdClosed(final String instanceId) {
        MoPubLog.log(CUSTOM, ADAPTER_NAME, "ironSource Interstitial closed ad for instance " + instanceId + " (current instance: " + mInstanceId + " )");

        sHandler.post(new Runnable() {
            @Override
            public void run() {
                MoPubLog.log(instanceId, CUSTOM, ADAPTER_NAME, "ironSource interstitial ad has been dismissed");

                if (mMoPubListener != null) {
                    mMoPubListener.onInterstitialDismissed();
                }
            }
        });
    }

    @Override
    public void onInterstitialAdShowFailed(String instanceId, IronSourceError ironSourceError) {
        MoPubLog.log(CUSTOM, ADAPTER_NAME, "ironSource Interstitial failed to show for instance "
            + instanceId + " (current instance: " + mInstanceId + " )" + " Error: " + ironSourceError.getErrorMessage());
        MoPubLog.log(instanceId, SHOW_FAILED, ADAPTER_NAME);

        sendMoPubInterstitialFailed(IronSourceAdapterConfiguration.getMoPubErrorCode(ironSourceError), instanceId);
    }

    @Override
    public void onInterstitialAdClicked(final String instanceId) {
        MoPubLog.log(CUSTOM, ADAPTER_NAME, "ironSource Interstitial clicked ad for instance "
            + instanceId + " (current instance: " + mInstanceId + " )");

        sHandler.post(new Runnable() {
            @Override
            public void run() {
                MoPubLog.log(instanceId, CLICKED, ADAPTER_NAME);

                if (mMoPubListener != null) {
                    mMoPubListener.onInterstitialClicked();
                }
            }
        });
    }

    private static LifecycleListener lifecycleListener = new LifecycleListener() {
        @Override
        public void onCreate(@NonNull Activity activity) {
        }

        @Override
        public void onStart(@NonNull Activity activity) {
        }

        @Override
        public void onPause(@NonNull Activity activity) {
            IronSource.onPause(activity);
        }

        @Override
        public void onResume(@NonNull Activity activity) {
            IronSource.onResume(activity);
        }

        @Override
        public void onRestart(@NonNull Activity activity) {
        }

        @Override
        public void onStop(@NonNull Activity activity) {
        }

        @Override
        public void onDestroy(@NonNull Activity activity) {
        }

        @Override
        public void onBackPressed(@NonNull Activity activity) {
        }
    };
}