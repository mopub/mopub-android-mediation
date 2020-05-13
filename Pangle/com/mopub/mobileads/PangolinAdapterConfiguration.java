package com.mopub.mobileads;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.mopub.common.BaseAdapterConfiguration;
import com.mopub.common.OnNetworkInitializationFinishedListener;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.union_test.toutiao.config.TTAdManagerHolder;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM_WITH_THROWABLE;

/**
 * created by wuzejian on 2019/4/14
 */
public class PangolinAdapterConfiguration extends BaseAdapterConfiguration {
    private static final String TAG = "PangolinAdapterConfiguration";

    private static final String ADAPTER_VERSION = "3.0.0.0";

    private static final String MOPUB_NETWORK_NAME = "tiktok_audience_network";

    @NonNull
    @Override
    public String getAdapterVersion() {
        return ADAPTER_VERSION;
    }

    @Nullable
    @Override
    public String getBiddingToken(@NonNull Context context) {
        return TTAdManagerHolder.get().getBiddingToken();
    }

    /**
     * returns a lowercase String that represents your ad network name. Use underscores if the String needs to contain spaces
     *
     * @return
     */
    @NonNull
    @Override
    public String getMoPubNetworkName() {
        return MOPUB_NETWORK_NAME;
    }

    /**
     * returns the version number of your ad network SDK.
     *
     * @return
     */
    @NonNull
    @Override
    public String getNetworkSdkVersion() {
        return TTAdManagerHolder.get().getSDKVersion();
    }

    @SuppressLint("LongLogTag")
    @Override
    public void initializeNetwork(@NonNull Context context, @Nullable Map<String, String> configuration, @NonNull OnNetworkInitializationFinishedListener listener) {
        MoPubLog.log(CUSTOM, TAG, "PangolinAdapterConfiguration#initializeNetwork....");
        Log.e(TAG, "initializeNetwork.....");
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(listener);
        boolean networkInitializationSucceeded = false;
        synchronized (PangolinAdapterConfiguration.class) {
            try {
                TTAdManagerHolder.init(context);
                networkInitializationSucceeded = true;
            } catch (Exception e) {
                MoPubLog.log(CUSTOM_WITH_THROWABLE, "Initializing AdMob has encountered " +
                        "an exception.", e);
            }
        }

        if (networkInitializationSucceeded) {
            listener.onNetworkInitializationFinished(PangolinAdapterConfiguration.class,
                    MoPubErrorCode.ADAPTER_INITIALIZATION_SUCCESS);
        } else {
            listener.onNetworkInitializationFinished(PangolinAdapterConfiguration.class,
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
        }
    }
}
