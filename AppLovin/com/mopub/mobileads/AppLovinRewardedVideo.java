package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.applovin.adview.AppLovinIncentivizedInterstitial;
import com.applovin.sdk.AppLovinAd;
import com.applovin.sdk.AppLovinAdClickListener;
import com.applovin.sdk.AppLovinAdDisplayListener;
import com.applovin.sdk.AppLovinAdLoadListener;
import com.applovin.sdk.AppLovinAdRewardListener;
import com.applovin.sdk.AppLovinAdVideoPlaybackListener;
import com.applovin.sdk.AppLovinErrorCodes;
import com.applovin.sdk.AppLovinMediationProvider;
import com.applovin.sdk.AppLovinPrivacySettings;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkSettings;
import com.mopub.common.DataKeys;
import com.mopub.common.LifecycleListener;
import com.mopub.common.MoPub;
import com.mopub.common.MoPubReward;
import com.mopub.common.logging.MoPubLog;

import java.util.HashMap;
import java.util.Map;

public class AppLovinRewardedVideo extends CustomEventRewardedVideo implements AppLovinAdLoadListener, AppLovinAdDisplayListener, AppLovinAdClickListener, AppLovinAdVideoPlaybackListener, AppLovinAdRewardListener {

    private static final String DEFAULT_ZONE = "";
    private static final String DEFAULT_TOKEN_ZONE = "token";
    private static final String ZONE_ID_SERVER_EXTRAS_KEY = "zone_id";

    // A map of Zone -> `AppLovinIncentivizedInterstitial` to be shared by instances of the custom event.
    // This prevents skipping of ads as this adapter will be re-created and preloaded (along with underlying `AppLovinIncentivizedInterstitial`)
    // on every ad load regardless if ad was actually displayed or not.
    private static final Map<String, AppLovinIncentivizedInterstitial> GLOBAL_INCENTIVIZED_INTERSTITIAL_ADS = new HashMap<String, AppLovinIncentivizedInterstitial>();

    private boolean initialized;

    private AppLovinSdk sdk;
    private AppLovinIncentivizedInterstitial incentivizedInterstitial;
    private Activity parentActivity;

    private boolean fullyWatched;
    private MoPubReward reward;

    private boolean isTokenEvent;
    private AppLovinAd tokenAd;
    private String serverExtrasZoneId = DEFAULT_ZONE;

    //
    // MoPub Custom Event Methods
    //

    @Override
    protected boolean checkAndInitializeSdk(@NonNull final Activity activity, @NonNull final Map<String, Object> localExtras, @NonNull final Map<String, String> serverExtras) throws Exception {

        // Pass the user consent from the MoPub SDK to AppLovin as per GDPR
        boolean canCollectPersonalInfo = MoPub.canCollectPersonalInformation();
        AppLovinPrivacySettings.setHasUserConsent(canCollectPersonalInfo, activity.getApplicationContext());

        MoPubLog.d("Initializing AppLovin rewarded video...");

        if (!initialized) {
            sdk = retrieveSdk(serverExtras, activity);
            sdk.setPluginVersion("MoPub-3.1.0");
            sdk.setMediationProvider(AppLovinMediationProvider.MOPUB);

            initialized = true;

            return true;
        }
        return false;
    }

    @Override
    protected void loadWithSdkInitialized(@NonNull final Activity activity, @NonNull final Map<String, Object> localExtras, @NonNull final Map<String, String> serverExtras) throws Exception {

        parentActivity = activity;

        final String adMarkup = serverExtras.get(DataKeys.ADM_KEY);
        final boolean hasAdMarkup = !TextUtils.isEmpty(adMarkup);

        MoPubLog.d("Requesting AppLovin banner with serverExtras: " + serverExtras + ", localExtras: " + localExtras + " and has ad markup: " + hasAdMarkup);

        // Determine zone
        final String zoneId;
        if (hasAdMarkup) {
            zoneId = DEFAULT_TOKEN_ZONE;
        } else {
            if (!TextUtils.isEmpty(serverExtras.get(ZONE_ID_SERVER_EXTRAS_KEY))) {
                serverExtrasZoneId = serverExtras.get(ZONE_ID_SERVER_EXTRAS_KEY);
                zoneId = serverExtrasZoneId;
            } else {
                zoneId = DEFAULT_ZONE;
            }
        }

        // Create incentivized ad based off of zone
        incentivizedInterstitial = createIncentivizedInterstitialAd(zoneId, activity, sdk);

        // Use token API
        if (hasAdMarkup) {
            isTokenEvent = true;

            sdk.getAdService().loadNextAdForAdToken(adMarkup, this);
        }
        // Zone/regular ad load
        else {
            incentivizedInterstitial.preload(this);
        }
    }

    @Override
    protected void showVideo() {
        if (hasVideoAvailable()) {
            fullyWatched = false;
            reward = null;

            if (isTokenEvent) {
                incentivizedInterstitial.show(tokenAd, parentActivity, this, this, this, this);
            } else {
                incentivizedInterstitial.show(parentActivity, null, this, this, this, this);
            }
        } else {
            MoPubLog.d("Failed to show an AppLovin rewarded video before one was loaded");
            MoPubRewardedVideoManager.onRewardedVideoPlaybackError(getClass(), getAdNetworkId(), MoPubErrorCode.VIDEO_PLAYBACK_ERROR);
        }
    }

    @Override
    protected boolean hasVideoAvailable() {
        if (isTokenEvent) {
            return tokenAd != null;
        } else {
            return (incentivizedInterstitial != null && incentivizedInterstitial.isAdReadyToDisplay());
        }
    }

    @Override
    @Nullable
    protected LifecycleListener getLifecycleListener() {
        return null;
    }

    @Override
    @NonNull
    protected String getAdNetworkId() {
        return serverExtrasZoneId;
    }

    @Override
    protected void onInvalidate() {
    }

    //
    // Ad Load Listener
    //

    @Override
    public void adReceived(final AppLovinAd ad) {
        MoPubLog.d("Rewarded video did load ad: " + ad.getAdIdNumber());

        if (isTokenEvent) {
            tokenAd = ad;
        }

        parentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    MoPubRewardedVideoManager.onRewardedVideoLoadSuccess(AppLovinRewardedVideo.this.getClass(), getAdNetworkId());
                } catch (Throwable th) {
                    MoPubLog.e("Unable to notify listener of successful ad load.", th);
                }
            }
        });
    }

    @Override
    public void failedToReceiveAd(final int errorCode) {
        MoPubLog.d("Rewarded video failed to load with error: " + errorCode);

        parentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    MoPubRewardedVideoManager.onRewardedVideoLoadFailure(AppLovinRewardedVideo.this.getClass(), getAdNetworkId(), toMoPubErrorCode(errorCode));
                } catch (Throwable th) {
                    MoPubLog.e("Unable to notify listener of failure to receive ad.", th);
                }
            }
        });
    }

    //
    // Ad Display Listener
    //

    @Override
    public void adDisplayed(final AppLovinAd ad) {
        MoPubLog.d("Rewarded video displayed");
        MoPubRewardedVideoManager.onRewardedVideoStarted(getClass(), getAdNetworkId());
    }

    @Override
    public void adHidden(final AppLovinAd ad) {
        MoPubLog.d("Rewarded video dismissed");

        if (fullyWatched && reward != null) {
            MoPubLog.d("Rewarded" + reward.getAmount() + " " + reward.getLabel());
            MoPubRewardedVideoManager.onRewardedVideoCompleted(getClass(), getAdNetworkId(), reward);
        }

        MoPubRewardedVideoManager.onRewardedVideoClosed(getClass(), getAdNetworkId());
    }

    //
    // Ad Click Listener
    //

    @Override
    public void adClicked(final AppLovinAd ad) {
        MoPubLog.d("Rewarded video clicked");
        MoPubRewardedVideoManager.onRewardedVideoClicked(getClass(), getAdNetworkId());
    }

    //
    // Video Playback Listener
    //

    @Override
    public void videoPlaybackBegan(final AppLovinAd ad) {
        MoPubLog.d("Rewarded video playback began");
    }

    @Override
    public void videoPlaybackEnded(final AppLovinAd ad, final double percentViewed, final boolean fullyWatched) {
        MoPubLog.d("Rewarded video playback ended at playback percent: " + percentViewed);

        this.fullyWatched = fullyWatched;
    }

    //
    // Reward Listener
    //

    @Override
    public void userOverQuota(final AppLovinAd appLovinAd, final Map map) {
        MoPubLog.d("Rewarded video validation request for ad did exceed quota with response: " + map);
    }

    @Override
    public void validationRequestFailed(final AppLovinAd appLovinAd, final int errorCode) {
        MoPubLog.d("Rewarded video validation request for ad failed with error code: " + errorCode);
    }

    @Override
    public void userRewardRejected(final AppLovinAd appLovinAd, final Map map) {
        MoPubLog.d("Rewarded video validation request was rejected with response: " + map);
    }

    @Override
    public void userDeclinedToViewAd(final AppLovinAd appLovinAd) {
        MoPubLog.d("User declined to view rewarded video");
        MoPubRewardedVideoManager.onRewardedVideoClosed(getClass(), getAdNetworkId());
    }

    @Override
    public void userRewardVerified(final AppLovinAd appLovinAd, final Map map) {
        final String currency = (String) map.get("currency");
        final int amount = (int) Double.parseDouble((String) map.get("amount")); // AppLovin returns amount as double

        MoPubLog.d("Verified " + amount + " " + currency);

        reward = MoPubReward.success(currency, amount);
    }

    //
    // Utility Methods
    //

    private static MoPubErrorCode toMoPubErrorCode(final int applovinErrorCode) {
        if (applovinErrorCode == AppLovinErrorCodes.NO_FILL) {
            return MoPubErrorCode.NETWORK_NO_FILL;
        } else if (applovinErrorCode == AppLovinErrorCodes.UNSPECIFIED_ERROR) {
            return MoPubErrorCode.UNSPECIFIED;
        } else if (applovinErrorCode == AppLovinErrorCodes.NO_NETWORK) {
            return MoPubErrorCode.NO_CONNECTION;
        } else if (applovinErrorCode == AppLovinErrorCodes.FETCH_AD_TIMEOUT) {
            return MoPubErrorCode.NETWORK_TIMEOUT;
        } else {
            return MoPubErrorCode.UNSPECIFIED;
        }
    }

    /**
     * Retrieves the appropriate instance of AppLovin's SDK from the SDK key given in the server parameters, or Android Manifest.
     */
    private static AppLovinSdk retrieveSdk(final Map<String, String> serverExtras, final Context context) {
        final String sdkKey = serverExtras != null ? serverExtras.get("sdk_key") : null;
        final AppLovinSdk sdk;

        if (!TextUtils.isEmpty(sdkKey)) {
            sdk = AppLovinSdk.getInstance(sdkKey, new AppLovinSdkSettings(), context);
        } else {
            sdk = AppLovinSdk.getInstance(context);
        }
        return sdk;
    }

    private static AppLovinIncentivizedInterstitial createIncentivizedInterstitialAd(final String zoneId, final Activity activity, final AppLovinSdk sdk) {
        final AppLovinIncentivizedInterstitial incent;

        // Check if incentivized ad for zone already exists
        if (GLOBAL_INCENTIVIZED_INTERSTITIAL_ADS.containsKey(zoneId)) {
            incent = GLOBAL_INCENTIVIZED_INTERSTITIAL_ADS.get(zoneId);
        } else {
            // If this is a default or token Zone, create the incentivized ad normally
            if (DEFAULT_ZONE.equals(zoneId) || DEFAULT_TOKEN_ZONE.equals(zoneId)) {
                incent = AppLovinIncentivizedInterstitial.create(activity);
            }
            // Otherwise, use the Zones API
            else {
                incent = AppLovinIncentivizedInterstitial.create(zoneId, sdk);
            }

            GLOBAL_INCENTIVIZED_INTERSTITIAL_ADS.put(zoneId, incent);
        }

        return incent;
    }
}