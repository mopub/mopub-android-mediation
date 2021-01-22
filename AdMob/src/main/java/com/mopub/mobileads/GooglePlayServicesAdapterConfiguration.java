package com.mopub.mobileads;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdFormat;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.query.QueryInfo;
import com.google.android.gms.ads.query.QueryInfoGenerationCallback;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mopub.common.BaseAdapterConfiguration;
import com.mopub.common.MoPub;
import com.mopub.common.OnNetworkInitializationFinishedListener;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
//import com.mopub.mobileads.admob.BuildConfig;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM_WITH_THROWABLE;

public class GooglePlayServicesAdapterConfiguration extends BaseAdapterConfiguration {

    private static final String ADAPTER_NAME = GooglePlayServicesAdapterConfiguration.class.getSimpleName();
    private static final String ADAPTER_VERSION = BuildConfig.VERSION_NAME;
    private static final String MOPUB_NETWORK_NAME = "admob_native";

    protected static Cache<String, QueryInfo> dv3Tokens;
    private AtomicReference<String> tokenReference = new AtomicReference<>(null);
    private AtomicBoolean isComputingToken = new AtomicBoolean(false);

    @NonNull
    @Override
    public String getAdapterVersion() {
        return ADAPTER_VERSION;
    }

    @Nullable
    @Override
    public String getBiddingToken(@NonNull Context context) {
        refreshBidderToken(context);
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
        /* com.google.android.gms:play-services-ads (AdMob) does not have an API to get the compiled
        version */
        final String adapterVersion = getAdapterVersion();

        return (!TextUtils.isEmpty(adapterVersion)) ?
            adapterVersion.substring(0, adapterVersion.lastIndexOf('.')) : "";
    }

    @Override
    public void initializeNetwork(@NonNull Context context, @Nullable Map<String, String>
        configuration, @NonNull OnNetworkInitializationFinishedListener listener) {

        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(listener);

        boolean networkInitializationSucceeded = false;

        synchronized (GooglePlayServicesAdapterConfiguration.class) {
            try {
                MobileAds.initialize(context);
                networkInitializationSucceeded = true;
            } catch (Exception e) {
                MoPubLog.log(CUSTOM_WITH_THROWABLE, "Initializing AdMob has encountered " +
                    "an exception.", e);
            }
        }

        if (networkInitializationSucceeded) {
            listener.onNetworkInitializationFinished(GooglePlayServicesAdapterConfiguration.class,
                MoPubErrorCode.ADAPTER_INITIALIZATION_SUCCESS);
        } else {
            listener.onNetworkInitializationFinished(GooglePlayServicesAdapterConfiguration.class,
                MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
        }

        dv3Tokens = CacheBuilder.newBuilder()
                .expireAfterWrite(15, TimeUnit.MINUTES)
                .build();
    }

    // MoPub collects GDPR consent on behalf of Google
    public static AdRequest.Builder forwardNpaIfSet(AdRequest.Builder builder) {
        final Bundle npaBundle = new Bundle();

        if (!MoPub.canCollectPersonalInformation()) {
            npaBundle.putString("npa", "1");
        }

        if (!npaBundle.isEmpty()) {
            builder.addNetworkExtrasBundle(AdMobAdapter.class, npaBundle);
        }

        return builder;
    }

    private void refreshBidderToken(final Context context) {
        if (isComputingToken.compareAndSet(false, true)) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    QueryInfo.generate(context, AdFormat.INTERSTITIAL, new AdRequest.Builder().build(),
                        new QueryInfoGenerationCallback() {
                            @Override
                            public void onSuccess(QueryInfo queryInfo) {
                                dv3Tokens.put(queryInfo.getRequestId(), queryInfo);
                                tokenReference.set(queryInfo.getQuery());
                            }
                        });
                    isComputingToken.set(false);
                }
            }).start();
        }
    }
}
