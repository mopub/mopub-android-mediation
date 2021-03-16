package com.mopub.mobileads;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.BaseAdapterConfiguration;
import com.mopub.common.OnNetworkInitializationFinishedListener;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.vungle.BuildConfig;
import com.vungle.warren.Vungle;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM_WITH_THROWABLE;

public class VungleAdapterConfiguration extends BaseAdapterConfiguration {

    public static final String ADAPTER_VERSION = BuildConfig.VERSION_NAME;
    // Vungle's keys
    private static final String APP_ID_KEY = "appId";
    // Adapter's keys
    private static final String ADAPTER_NAME = VungleAdapterConfiguration.class.getSimpleName();
    private static final String MOPUB_NETWORK_NAME = BuildConfig.NETWORK_NAME;

    private static VungleRouter sVungleRouter;
    private static String sWithAutoRotate;

    /**
     * (Optional) Key for publisher to set on to initialize Vungle SDK
     */
    public static final String WITH_AUTO_ROTATE_KEY = "orientations";

    private AtomicReference<String> tokenReference = new AtomicReference<>(null);

    public VungleAdapterConfiguration() {
        sVungleRouter = VungleRouter.getInstance();
    }

    @NonNull
    @Override
    public String getAdapterVersion() {
        return ADAPTER_VERSION;
    }

    @Nullable
    @Override
    public String getBiddingToken(@NonNull Context context) {
        String token = Vungle.getAvailableBidTokens(context, 10);
        if (token != null) {
            tokenReference.set(token);
        }

        return tokenReference.get();
    }

    @NonNull
    @Override
    public String getMoPubNetworkName() {
        return MOPUB_NETWORK_NAME;
    }

    @NonNull
    @Override
    public String getNetworkSdkVersion() {
        return com.vungle.warren.BuildConfig.VERSION_NAME;
    }

    @Override
    public void initializeNetwork(@NonNull final Context context, @Nullable final Map<String, String> configuration,
                                  @NonNull final OnNetworkInitializationFinishedListener listener) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(listener);

        VungleRouter.getInstance().applyVungleNetworkSettings(configuration);

        boolean networkInitializationSucceeded = false;

        synchronized (VungleAdapterConfiguration.class) {
            try {
                if (Vungle.isInitialized()) {
                    networkInitializationSucceeded = true;

                } else if (configuration != null && sVungleRouter != null) {
                    sWithAutoRotate = configuration.get(WITH_AUTO_ROTATE_KEY);
                    final String mAppId = configuration.get(APP_ID_KEY);
                    if (TextUtils.isEmpty(mAppId)) {
                        MoPubLog.log(mAppId, CUSTOM, ADAPTER_NAME, "Vungle's initialization not " +
                                "started. Ensure Vungle's appId is populated");
                        listener.onNetworkInitializationFinished(this.getClass(),
                                MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
                        return;
                    }
                    if (!sVungleRouter.isVungleInitialized()) {
                        sVungleRouter.initVungle(context, mAppId);

                        networkInitializationSucceeded = true;
                    }
                }
            } catch (Exception e) {
                MoPubLog.log(CUSTOM_WITH_THROWABLE, "Initializing Vungle has encountered" +
                        "an exception.", e);
            }
        }
        if (networkInitializationSucceeded) {
            listener.onNetworkInitializationFinished(this.getClass(),
                    MoPubErrorCode.ADAPTER_INITIALIZATION_SUCCESS);
        } else {
            listener.onNetworkInitializationFinished(this.getClass(),
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
        }
    }

    public static String getWithAutoRotate() {
        return sWithAutoRotate;
    }
}
