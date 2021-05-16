package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.sdk.ISDemandOnlyInterstitialListener;
import com.mopub.common.DataKeys;
import com.mopub.common.LifecycleListener;
import com.mopub.common.MoPub;
import com.mopub.common.MoPubLifecycleManager;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM_WITH_THROWABLE;
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
    private static final String ADAPTER_NAME = IronSourceInterstitial.class.getSimpleName();

    // Network identifier of ironSource
    private String mInstanceId = IronSourceAdapterConfiguration.DEFAULT_INSTANCE_ID;

    @NonNull
    protected String getAdNetworkId() {
        return mInstanceId;
    }

    @Override
    protected boolean checkAndInitializeSdk(@NonNull Activity launcherActivity, @NonNull AdData adData) {
        Preconditions.checkNotNull(launcherActivity);
        Preconditions.checkNotNull(adData);

        boolean canCollectPersonalInfo = MoPub.canCollectPersonalInformation();
        IronSource.setConsent(canCollectPersonalInfo);

        final Map<String, String> extras = adData.getExtras();
        try {
            if (TextUtils.isEmpty(extras.get(APPLICATION_KEY))) {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "ironSource Interstitial failed to initialize. " +
                        "ironSource applicationKey is not valid. Please make sure it's entered properly on MoPub UI.");

                if (mLoadListener != null) {
                    mLoadListener.onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
                }
                return false;
            }

            String applicationKey = extras.get(APPLICATION_KEY);
            final String instanceId = extras.get(INSTANCE_ID_KEY);
            if (!TextUtils.isEmpty(instanceId)) {
                mInstanceId = instanceId;
            }

            final Context context = launcherActivity.getApplicationContext();

            if (context != null) {
                initIronSourceSDK(context, applicationKey, extras);
                return true;
            } else {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "ironSource Interstitial failed to initialize." +
                        "Application Context obtained by Activity launching this interstitial is null.");
                if (mLoadListener != null) {
                    mLoadListener.onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
                }
                return false;
            }

        } catch (Exception e) {
            MoPubLog.log(CUSTOM_WITH_THROWABLE, e);
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "ironSource Interstitial failed to initialize." +
                    "Ensure ironSource applicationKey and instanceId are properly entered on MoPub UI.");
            if (mLoadListener != null) {
                mLoadListener.onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            }
            return false;
        }
    }

    private void initIronSourceSDK(Context context, String appKey, Map<String, String> extras) {
        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "ironSource Interstitial initialization is called with applicationKey: " + appKey);
        IronSource.setISDemandOnlyInterstitialListener(this);

        IronSource.AD_UNIT[] adUnitsToInit = mIronSourceAdapterConfiguration.getIronSourceAdUnitsToInitList(context, extras);
        IronSourceAdapterConfiguration.initIronSourceSDK(context, appKey, adUnitsToInit);
    }

    @NonNull
    private IronSourceAdapterConfiguration mIronSourceAdapterConfiguration;

    /**
     * MoPub API
     */

    public IronSourceInterstitial() {
        mIronSourceAdapterConfiguration = new IronSourceAdapterConfiguration();
    }

    @Override
    protected void load(@NonNull final Context context, @NonNull final AdData adData) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(adData);

        setAutomaticImpressionAndClickTracking(false);

        if (!(context instanceof Activity)) {
            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME, MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR.getIntCode(), MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Failed to load interstitial as ironSource requires an Activity context.");
            if (mLoadListener != null) {
                mLoadListener.onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            }
            return;
        }

        final Map<String, String> extras = adData.getExtras();

        /* Update instance id if extras contain it. ironSource requires instanceId to perform proper ad requests.
            Ideally instanceId should be populated on MoPub UI.
            For backward compatibility, we don't fail out if instanceId is empty or null below.
            If instanceId is empty, ironSource Ad Server will treat it as "0".
        */
        final String instanceId = extras.get(INSTANCE_ID_KEY);
        if (instanceId != null && !TextUtils.isEmpty(instanceId)) {
            mInstanceId = instanceId;
        }

        mIronSourceAdapterConfiguration.retainIronSourceAdUnitsToInitPrefsIfNecessary(context,extras);
        mIronSourceAdapterConfiguration.setCachedInitializationParameters(context, extras);
        MoPubLifecycleManager.getInstance((Activity) context).addLifecycleListener(lifecycleListener);
        MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);

        final String adMarkup = extras.get(DataKeys.ADM_KEY);

        if (!TextUtils.isEmpty(adMarkup)) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "ADM field is populated. Will make Advanced Bidding request.");
            IronSource.loadISDemandOnlyInterstitialWithAdm((Activity) context, mInstanceId, adMarkup);
        } else {
            IronSource.loadISDemandOnlyInterstitial((Activity) context, mInstanceId);
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

    private void logAndFail(final MoPubErrorCode errorCode, final String instanceId) {

        MoPubLog.log(instanceId, LOAD_FAILED, ADAPTER_NAME,
                errorCode.getIntCode(),
                errorCode);

        if (mLoadListener != null) {
            mLoadListener.onAdLoadFailed(errorCode);
        }
    }

    /**
     * ironSource Interstitial Listener
     **/

    @Override
    public void onInterstitialAdReady(final String instanceId) {
        MoPubLog.log(CUSTOM, ADAPTER_NAME, "ironSource Interstitial loaded successfully for instance " +
                instanceId + " (current instance: " + mInstanceId + " )");

        MoPubLog.log(instanceId, LOAD_SUCCESS, ADAPTER_NAME);

        if (mLoadListener != null) {
            mLoadListener.onAdLoaded();
        }
    }

    @Override
    public void onInterstitialAdLoadFailed(String instanceId, IronSourceError ironSourceError) {
        MoPubLog.log(CUSTOM, ADAPTER_NAME, "ironSource Interstitial failed to load for instance " +
                instanceId + " (current instance: " + mInstanceId + " )" + " Error: " + ironSourceError.getErrorMessage());

        logAndFail(IronSourceAdapterConfiguration.getMoPubErrorCode(ironSourceError), instanceId);
    }

    @Override
    public void onInterstitialAdOpened(final String instanceId) {
        MoPubLog.log(CUSTOM, ADAPTER_NAME, "ironSource Interstitial opened ad for instance "
                + instanceId + " (current instance: " + mInstanceId + " )");

        MoPubLog.log(instanceId, SHOW_SUCCESS, ADAPTER_NAME);

        if (mInteractionListener != null) {
            mInteractionListener.onAdShown();
            mInteractionListener.onAdImpression();
        }
    }

    @Override
    public void onInterstitialAdClosed(final String instanceId) {
        MoPubLog.log(CUSTOM, ADAPTER_NAME, "ironSource Interstitial closed ad for instance " + instanceId + " (current instance: " + mInstanceId + " )");

        MoPubLog.log(instanceId, CUSTOM, ADAPTER_NAME, "ironSource interstitial ad has been dismissed");

        if (mInteractionListener != null) {
            mInteractionListener.onAdDismissed();
        }
    }

    @Override
    public void onInterstitialAdShowFailed(final String instanceId, final IronSourceError ironSourceError) {
        MoPubLog.log(CUSTOM, ADAPTER_NAME, "ironSource Interstitial failed to show for instance "
                + instanceId + " (current instance: " + mInstanceId + " )" + " Error: " + ironSourceError.getErrorMessage());
        MoPubLog.log(instanceId, SHOW_FAILED, ADAPTER_NAME);

        final MoPubErrorCode errorCode = IronSourceAdapterConfiguration.getMoPubErrorCode(ironSourceError);

        MoPubLog.log(instanceId, LOAD_FAILED, ADAPTER_NAME,
                errorCode.getIntCode(),
                errorCode);

        if (mInteractionListener != null) {
            mInteractionListener.onAdFailed(errorCode);
        }
    }

    @Override
    public void onInterstitialAdClicked(final String instanceId) {
        MoPubLog.log(CUSTOM, ADAPTER_NAME, "ironSource Interstitial clicked ad for instance "
                + instanceId + " (current instance: " + mInstanceId + " )");

        MoPubLog.log(instanceId, CLICKED, ADAPTER_NAME);

        if (mInteractionListener != null) {
            mInteractionListener.onAdClicked();
        }
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
