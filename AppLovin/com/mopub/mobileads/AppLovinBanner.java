package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;

import com.applovin.adview.AppLovinAdView;
import com.applovin.adview.AppLovinAdViewEventListener;
import com.applovin.sdk.AppLovinAd;
import com.applovin.sdk.AppLovinAdClickListener;
import com.applovin.sdk.AppLovinAdDisplayListener;
import com.applovin.sdk.AppLovinAdLoadListener;
import com.applovin.sdk.AppLovinAdSize;
import com.applovin.sdk.AppLovinErrorCodes;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkUtils;
import com.mopub.common.logging.MoPubLog;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

public class AppLovinBanner extends CustomEventBanner {
    private static final int BANNER_STANDARD_HEIGHT = 50;
    private static final int BANNER_HEIGHT_OFFSET_TOLERANCE = 10;
    private static final int LEADER_STANDARD_HEIGHT = 90;
    private static final int LEADER_HEIGHT_OFFSET_TOLERANCE = 16;

    private static final String AD_WIDTH_KEY = "com_mopub_ad_width";
    private static final String AD_HEIGHT_KEY = "com_mopub_ad_height";

    //
    // MoPub Custom Event Methods
    //

    @Override
    protected void loadBanner(final Context context, final CustomEventBannerListener customEventBannerListener, final Map<String, Object> localExtras, final Map<String, String> serverExtras) {
        // SDK versions BELOW 7.1.0 require a instance of an Activity to be passed in as the context
        if (AppLovinSdk.VERSION_CODE < 710 && !(context instanceof Activity)) {
            MoPubLog.d("Unable to request AppLovin banner. Invalid context provided.");
            customEventBannerListener.onBannerFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);

            return;
        }

        MoPubLog.d("Requesting AppLovin banner with localExtras: " + localExtras);

        final AppLovinAdSize adSize = appLovinAdSizeFromLocalExtras(localExtras);
        if (adSize != null) {
            final AppLovinSdk sdk = AppLovinSdk.getInstance(context);
            sdk.setPluginVersion("MoPub-Certified-2.1.0");

            final AppLovinAdView adView = createAdView(adSize, serverExtras, context, customEventBannerListener);

            adView.setAdLoadListener(new AppLovinAdLoadListener() {
                @Override
                public void adReceived(final AppLovinAd ad) {
                    // Ensure logic is ran on main queue
                    AppLovinSdkUtils.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            MoPubLog.d("Successfully loaded banner ad");
                            customEventBannerListener.onBannerLoaded(adView);
                        }
                    });
                }

                @Override
                public void failedToReceiveAd(final int errorCode) {
                    // Ensure logic is ran on main queue
                    AppLovinSdkUtils.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            MoPubLog.d("Failed to load banner ad with code: " + errorCode);
                            customEventBannerListener.onBannerFailed(toMoPubErrorCode(errorCode));
                        }
                    });
                }
            });
            adView.setAdDisplayListener(new AppLovinAdDisplayListener() {
                @Override
                public void adDisplayed(final AppLovinAd ad) {
                    MoPubLog.d("Banner displayed");
                }

                @Override
                public void adHidden(final AppLovinAd ad) {
                    MoPubLog.d("Banner dismissed");
                }
            });
            adView.setAdClickListener(new AppLovinAdClickListener() {
                @Override
                public void adClicked(final AppLovinAd ad) {
                    MoPubLog.d("Banner clicked");

                    customEventBannerListener.onBannerClicked(); // No need to call onLeaveApplication() since it's an alias for onBannerClicked()
                }
            });

            // As of Android SDK >= 7.3.0, we added a listener for banner events
            if (AppLovinSdk.VERSION_CODE >= 730) {
                adView.setAdViewEventListener((AppLovinAdViewEventListener) AppLovinAdViewEventListenerProxy.newInstance(customEventBannerListener));
            }

            adView.loadNextAd();
        } else {
            MoPubLog.d("Unable to request AppLovin banner");

            customEventBannerListener.onBannerFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
        }
    }

    @Override
    protected void onInvalidate() {
    }

    //
    // Utility Methods
    //

    private AppLovinAdSize appLovinAdSizeFromLocalExtras(final Map<String, Object> localExtras) {
        // Handle trivial case
        if (localExtras == null || localExtras.isEmpty()) {
            MoPubLog.d("No serverExtras provided");
            return null;
        }

        try {
            final int width = (Integer) localExtras.get(AD_WIDTH_KEY);
            final int height = (Integer) localExtras.get(AD_HEIGHT_KEY);

            // We have valid dimensions
            if (width > 0 && height > 0) {
                MoPubLog.d("Valid width (" + width + ") and height (" + height + ") provided");

                // Assume fluid width, and check for height with offset tolerance
                final int bannerOffset = Math.abs(BANNER_STANDARD_HEIGHT - height);
                final int leaderOffset = Math.abs(LEADER_STANDARD_HEIGHT - height);

                if (bannerOffset <= BANNER_HEIGHT_OFFSET_TOLERANCE) {
                    return AppLovinAdSize.BANNER;
                } else if (leaderOffset <= LEADER_HEIGHT_OFFSET_TOLERANCE) {
                    return AppLovinAdSize.LEADER;
                } else if (height <= AppLovinAdSize.MREC.getHeight()) {
                    return AppLovinAdSize.MREC;
                } else {
                    MoPubLog.d("Provided dimensions does not meet the dimensions required of banner or mrec ads");
                }
            } else {
                MoPubLog.d("Invalid width (" + width + ") and height (" + height + ") provided");
            }
        } catch (Throwable th) {
            MoPubLog.d("Encountered error while parsing width and height from serverExtras", th);
        }

        return null;
    }

    //
    // Utility Methods
    //

    private AppLovinAdView createAdView(final AppLovinAdSize size, final Map<String, String> serverExtras, final Context parentContext, final CustomEventBannerListener customEventBannerListener) {
        AppLovinAdView adView = null;

        try {
            // AppLovin SDK < 7.1.0 uses an Activity, as opposed to Context in >= 7.1.0
            final Class<?> contextClass = (AppLovinSdk.VERSION_CODE < 710) ? Activity.class : Context.class;

            // Zones support is available on AppLovin SDK 7.5.0 and higher
            final Constructor<?> constructor;
            if (AppLovinSdk.VERSION_CODE >= 750 && serverExtras != null && serverExtras.containsKey("zone_id")) {
                // Dynamically create an instance of AppLovinAdView with a given zone without breaking backwards compatibility for publishers on older SDKs.
                constructor = AppLovinAdView.class.getConstructor(AppLovinAdSize.class, String.class, contextClass);
                adView = (AppLovinAdView) constructor.newInstance(size, serverExtras.get("zone_id"), parentContext);
            } else {
                constructor = AppLovinAdView.class.getConstructor(AppLovinAdSize.class, contextClass);
                adView = (AppLovinAdView) constructor.newInstance(size, parentContext);
            }
        } catch (Throwable th) {
            MoPubLog.d("Unable to get create AppLovinAdView.");
            customEventBannerListener.onBannerFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
        }

        return adView;
    }

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
     * Dynamic proxy class for AppLovin's AppLovinAdViewEventListener. Used to keep compilation compatibility if publisher is on a version of the SDK before the listener was introduced (< 7.3.0).
     */
    private static final class AppLovinAdViewEventListenerProxy
            implements InvocationHandler {
        private final CustomEventBannerListener customEventBannerListener;

        private static Object newInstance(final CustomEventBannerListener customEventBannerListener) {
            return Proxy.newProxyInstance(AppLovinAdViewEventListener.class.getClassLoader(),
                    new Class[]{AppLovinAdViewEventListener.class},
                    new AppLovinAdViewEventListenerProxy(customEventBannerListener));
        }

        private AppLovinAdViewEventListenerProxy(final CustomEventBannerListener customEventBannerListener) {
            this.customEventBannerListener = customEventBannerListener;
        }

        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
            final String methodName = method.getName();

            if ("adOpenedFullscreen".equals(methodName)) {
                MoPubLog.d("Banner opened fullscreen");
                customEventBannerListener.onBannerExpanded();
            } else if ("adClosedFullscreen".equals(methodName)) {
                MoPubLog.d("Banner closed fullscreen");
                customEventBannerListener.onBannerCollapsed();
            } else if ("adLeftApplication".equals(methodName)) {
                // We will fire onLeaveApplication() in the adClicked() callback
                MoPubLog.d("Banner left application");
            }
            return null;
        }
    }
}