package com.mopub.mobileads;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;

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
import com.mopub.mobileads.admob.BuildConfig;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM_WITH_THROWABLE;

public class GooglePlayServicesAdapterConfiguration extends BaseAdapterConfiguration {

    private static final String ADAPTER_VERSION = BuildConfig.VERSION_NAME;
    private static final String MOPUB_NETWORK_NAME = BuildConfig.NETWORK_NAME;

    protected static Cache<String, QueryInfo> adMobTokens;

    @NonNull
    @Override
    public String getAdapterVersion() {
        return ADAPTER_VERSION;
    }

    @Nullable
    @Override
    public String getBiddingToken(@NonNull Context context) {
        return refreshBidderToken(context);
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

        adMobTokens = CacheBuilder.newBuilder()
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

    private String refreshBidderToken(final Context context) {
        FutureTask<String> generateQuery = new FutureTask<>(new Callable<String>() {
            @Override
            public String call() throws Exception {
                final String[] biddingToken = {null};
                QueryInfo.generate(context, AdFormat.INTERSTITIAL, new AdRequest.Builder().build(),
                    new QueryInfoGenerationCallback() {
                        @Override
                        public void onSuccess(QueryInfo queryInfo) {
                            adMobTokens.put(queryInfo.getRequestId(), queryInfo);
                            biddingToken[0] = queryInfo.getQuery();
                        }
                    });
                return biddingToken[0];
            }
        });
        try {
            String biddingToken = generateQuery.get();
            return biddingToken;
        } catch (Exception e){
            return null;
        }
    }


}
