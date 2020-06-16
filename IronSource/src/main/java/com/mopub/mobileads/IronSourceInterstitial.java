package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.sdk.ISDemandOnlyInterstitialListener;
import com.mopub.common.LifecycleListener;
import com.mopub.common.MoPub;
import com.mopub.common.MoPubLifecycleManager;
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

public class IronSourceInterstitial extends BaseAd implements ISDemandOnlyInterstitialListener {

    /**
     * private vars
     */

    // Configuration keys
    private static final String APPLICATION_KEY = "applicationKey";
    private static final String INSTANCE_ID_KEY = "instanceId";
    private static final String MEDIATION_TYPE = "mopub";
    private static final String ADAPTER_NAME = IronSourceInterstitial.class.getSimpleName();

    private Handler mHandler;

    // Network identifier of ironSource
    private String mInstanceId = IronSourceAdapterConfiguration.DEFAULT_INSTANCE_ID;

    @NonNull
    protected String getAdNetworkId() {
        return mInstanceId;
    }

    @Override
    protected boolean checkAndInitializeSdk(@NonNull Activity launcherActivity, @NonNull AdData adData) {
        return false;
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
    protected void load(@NonNull final Context context, @NonNull final AdData adData) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(adData);

        setAutomaticImpressionAndClickTracking(false);

        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "load");

        MoPubLifecycleManager.getInstance((Activity) context).addLifecycleListener(lifecycleListener);
        // Pass the user consent from the MoPub SDK to ironSource as per GDPR
        boolean canCollectPersonalInfo = MoPub.canCollectPersonalInformation();
        IronSource.setConsent(canCollectPersonalInfo);

        try {
            String applicationKey = "";
            mHandler = new Handler(Looper.getMainLooper());

            if (!(context instanceof Activity)) {
                // Context not an Activity context, log the reason for failure and fail the
                // initialization.

                MoPubLog.log(CUSTOM, ADAPTER_NAME, "ironSource load interstitial must be called from an " +
                        "Activity context");
                sendMoPubAdLoadFailed(MoPubErrorCode.INTERNAL_ERROR, mInstanceId);

                return;
            }

            final Map<String, String> extras = adData.getExtras();
            if (extras.get(APPLICATION_KEY) != null) {
                applicationKey = extras.get(APPLICATION_KEY);
            }

            if (!TextUtils.isEmpty(extras.get(INSTANCE_ID_KEY))) {
                mInstanceId = extras.get(INSTANCE_ID_KEY);
            }

            if (!TextUtils.isEmpty(applicationKey)) {
                initIronSourceSDK(((Activity) context), applicationKey);
                loadInterstitial(mInstanceId);

                mIronSourceAdapterConfiguration.setCachedInitializationParameters(context, extras);
            } else {
                 MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "ironSource initialization failed, make sure that"+
                        " 'applicationKey' parameter is added");
                sendMoPubAdLoadFailed(MoPubErrorCode.INTERNAL_ERROR, getAdNetworkId());
            }

        } catch (Exception e) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, e);
            sendMoPubAdLoadFailed(MoPubErrorCode.INTERNAL_ERROR, mInstanceId);
        }
    }

    @Override
    protected void show() {
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

    @Nullable
    @Override
    protected LifecycleListener getLifecycleListener() {
        return null;
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

    private void sendMoPubAdLoadFailed(final MoPubErrorCode errorCode, final String instanceId) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                MoPubLog.log(instanceId, LOAD_FAILED, ADAPTER_NAME,
                        errorCode.getIntCode(),
                        errorCode);

                if (mLoadListener != null) {
                    mLoadListener.onAdLoadFailed(errorCode);
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

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                MoPubLog.log(instanceId, LOAD_SUCCESS, ADAPTER_NAME);

                if (mLoadListener != null) {
                    mLoadListener.onAdLoaded();
                }
            }
        });
    }

    @Override
    public void onInterstitialAdLoadFailed(String instanceId, IronSourceError ironSourceError) {
        MoPubLog.log(CUSTOM, ADAPTER_NAME, "ironSource Interstitial failed to load for instance " +
                instanceId + " (current instance: " + mInstanceId + " )" + " Error: " + ironSourceError.getErrorMessage());

        sendMoPubAdLoadFailed(IronSourceAdapterConfiguration.getMoPubErrorCode(ironSourceError), instanceId);
    }

    @Override
    public void onInterstitialAdOpened(final String instanceId) {
        MoPubLog.log(CUSTOM, ADAPTER_NAME, "ironSource Interstitial opened ad for instance "
            + instanceId + " (current instance: " + mInstanceId + " )");
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                MoPubLog.log(instanceId, SHOW_SUCCESS, ADAPTER_NAME);

                if (mInteractionListener != null) {
                    mInteractionListener.onAdShown();
                    mInteractionListener.onAdImpression();
                }
            }
        });
    }

    @Override
    public void onInterstitialAdClosed(final String instanceId) {
        MoPubLog.log(CUSTOM, ADAPTER_NAME, "ironSource Interstitial closed ad for instance " + instanceId + " (current instance: " + mInstanceId + " )");

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                MoPubLog.log(instanceId, CUSTOM, ADAPTER_NAME, "ironSource interstitial ad has been dismissed");

                if (mInteractionListener != null) {
                    mInteractionListener.onAdDismissed();
                }
            }
        });
    }

    @Override
    public void onInterstitialAdShowFailed(final String instanceId, final IronSourceError ironSourceError) {
        MoPubLog.log(CUSTOM, ADAPTER_NAME, "ironSource Interstitial failed to show for instance "
            + instanceId + " (current instance: " + mInstanceId + " )" + " Error: " + ironSourceError.getErrorMessage());
        MoPubLog.log(instanceId, SHOW_FAILED, ADAPTER_NAME);

        final MoPubErrorCode errorCode = IronSourceAdapterConfiguration.getMoPubErrorCode(ironSourceError);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                MoPubLog.log(instanceId, LOAD_FAILED, ADAPTER_NAME,
                        errorCode.getIntCode(),
                        errorCode);

                if (mInteractionListener != null) {
                    mInteractionListener.onAdFailed(errorCode);
                }
            }
        });
    }

    @Override
    public void onInterstitialAdClicked(final String instanceId) {
        MoPubLog.log(CUSTOM, ADAPTER_NAME, "ironSource Interstitial clicked ad for instance "
            + instanceId + " (current instance: " + mInstanceId + " )");

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                MoPubLog.log(instanceId, CLICKED, ADAPTER_NAME);

                if (mInteractionListener != null) {
                    mInteractionListener.onAdClicked();
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
