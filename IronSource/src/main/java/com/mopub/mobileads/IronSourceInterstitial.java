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

    // This is the instance id used inside ironSource SDK
    private String mInstanceId = IronSourceAdapterConfiguration.DEFAULT_INSTANCE_ID;
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
        MoPubLog.log(CUSTOM, ADAPTER_NAME, "loadInterstitial");

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

                MoPubLog.log(CUSTOM, ADAPTER_NAME, "IronSource load interstitial must be called from an " +
                        "Activity context");
                sendMoPubInterstitialFailed(MoPubErrorCode.INTERNAL_ERROR);

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
                sendMoPubInterstitialFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);

                return;
            }

            if (!TextUtils.isEmpty(applicationKey)) {
                initIronSourceSDK(((Activity) context), applicationKey);
                loadInterstitial(mInstanceId);

                mIronSourceAdapterConfiguration.setCachedInitializationParameters(context, serverExtras);
            } else {
                 MoPubLog.log(CUSTOM, ADAPTER_NAME, "IronSource initialization failed, make sure that"+
                        " 'applicationKey' server parameter is added");
                sendMoPubInterstitialFailed(MoPubErrorCode.INTERNAL_ERROR);
            }

        } catch (Exception e) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, e);
            sendMoPubInterstitialFailed(MoPubErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    protected void showInterstitial() {
        MoPubLog.log(SHOW_ATTEMPTED, ADAPTER_NAME);
        if (mInstanceId != null) {
            IronSource.showISDemandOnlyInterstitial(mInstanceId);
        } else {
            MoPubLog.log(SHOW_FAILED, ADAPTER_NAME);
        }
    }

    @Override
    protected void onInvalidate() {
    }

    /**
     * Class Helper Methods
     **/

    private void initIronSourceSDK(Activity activity, String appKey) {
        MoPubLog.log(CUSTOM, ADAPTER_NAME, "IronSource Interstitial Init with appkey: " + appKey);

        IronSource.setISDemandOnlyInterstitialListener(this);
        IronSource.setMediationType(MEDIATION_TYPE + IronSourceAdapterConfiguration.IRONSOURCE_ADAPTER_VERSION + 
            "SDK" + IronSourceAdapterConfiguration.getMoPubSdkVersion());
        IronSource.initISDemandOnly(activity, appKey, IronSource.AD_UNIT.INTERSTITIAL);

    }

    private void loadInterstitial(String instanceId) {
        MoPubLog.log(LOAD_ATTEMPTED, ADAPTER_NAME, "IronSource Interstitial load ad for instance " + instanceId);
        mInstanceId = instanceId;
        IronSource.loadISDemandOnlyInterstitial(instanceId);
    }

    private void sendMoPubInterstitialFailed(final MoPubErrorCode errorCode) {
        sHandler.post(new Runnable() {
            @Override
            public void run() {
                MoPubLog.log(LOAD_FAILED, errorCode.getIntCode(),
                        errorCode);

                if (mMoPubListener != null) {
                    mMoPubListener.onInterstitialFailed(errorCode);
                }
            }
        });
    }

    private MoPubErrorCode getMoPubErrorMessage(IronSourceError ironSourceError) {
        if (ironSourceError == null) {
            return MoPubErrorCode.INTERNAL_ERROR;
        }
        switch (ironSourceError.getErrorCode()) {
            case IronSourceError.ERROR_CODE_NO_CONFIGURATION_AVAILABLE:
            case IronSourceError.ERROR_CODE_KEY_NOT_SET:
            case IronSourceError.ERROR_CODE_INVALID_KEY_VALUE:
            case IronSourceError.ERROR_CODE_INIT_FAILED:
                return MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR;
            case IronSourceError.ERROR_CODE_USING_CACHED_CONFIGURATION:
                return MoPubErrorCode.VIDEO_CACHE_ERROR;
            case IronSourceError.ERROR_CODE_NO_ADS_TO_SHOW:
                return MoPubErrorCode.NETWORK_NO_FILL;
            case IronSourceError.ERROR_CODE_GENERIC:
                return MoPubErrorCode.INTERNAL_ERROR;
            case IronSourceError.ERROR_NO_INTERNET_CONNECTION:
                return MoPubErrorCode.NO_CONNECTION;
            default:
                return MoPubErrorCode.UNSPECIFIED;
        }
    }

    /**
     * IronSource Interstitial Listener
     **/

    @Override
    public void onInterstitialAdReady(String instanceId) {
        MoPubLog.log(CUSTOM, ADAPTER_NAME, "IronSource Interstitial loaded successfully for instance " +
                instanceId + " (current instance: " + mInstanceId + " )");

        sHandler.post(new Runnable() {
            @Override
            public void run() {
                MoPubLog.log(LOAD_SUCCESS, ADAPTER_NAME);

                if (mMoPubListener != null) {
                    mMoPubListener.onInterstitialLoaded();
                }
            }
        });
    }

    @Override
    public void onInterstitialAdLoadFailed(String instanceId, IronSourceError ironSourceError) {
        MoPubLog.log(CUSTOM, ADAPTER_NAME, "IronSource Interstitial failed to load for instance " +
                instanceId + " (current instance: " + mInstanceId + " )" + " Error: " + ironSourceError.getErrorMessage());

        sendMoPubInterstitialFailed(getMoPubErrorMessage(ironSourceError));
    }

    @Override
    public void onInterstitialAdOpened(String instanceId) {
        MoPubLog.log(CUSTOM, ADAPTER_NAME, "IronSource Interstitial opened ad for instance " 
            + instanceId + " (current instance: " + mInstanceId + " )");
        sHandler.post(new Runnable() {
            @Override
            public void run() {
                MoPubLog.log(SHOW_SUCCESS, ADAPTER_NAME);

                if (mMoPubListener != null) {
                    mMoPubListener.onInterstitialShown();
                }
            }
        });
    }

    @Override
    public void onInterstitialAdClosed(String instanceId) {
        MoPubLog.log(CUSTOM, ADAPTER_NAME, "IronSource Interstitial closed ad for instance " + instanceId + " (current instance: " + mInstanceId + " )");

        sHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mMoPubListener != null) {
                    mMoPubListener.onInterstitialDismissed();
                }
            }
        });
    }

    @Override
    public void onInterstitialAdShowFailed(String instanceId, IronSourceError ironSourceError) {
        MoPubLog.log(CUSTOM, ADAPTER_NAME, "IronSource Interstitial failed to show for instance " 
            + instanceId + " (current instance: " + mInstanceId + " )" + " Error: " + ironSourceError.getErrorMessage());
        MoPubLog.log(SHOW_FAILED, ADAPTER_NAME);

        sendMoPubInterstitialFailed(MoPubErrorCode.INTERNAL_ERROR);
    }

    @Override
    public void onInterstitialAdClicked(String instanceId) {
        MoPubLog.log(CUSTOM, ADAPTER_NAME, "IronSource Interstitial clicked ad for instance " 
            + instanceId + " (current instance: " + mInstanceId + " )");

        sHandler.post(new Runnable() {
            @Override
            public void run() {
                MoPubLog.log(CLICKED, ADAPTER_NAME);

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