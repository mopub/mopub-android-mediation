package com.mopub.mobileads;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.BaseAdapterConfiguration;
import com.mopub.common.OnNetworkInitializationFinishedListener;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.verizon.BuildConfig;
import com.mopub.nativeads.NativeErrorCode;
import com.verizon.ads.Configuration;
import com.verizon.ads.ErrorInfo;
import com.verizon.ads.Logger;
import com.verizon.ads.VASAds;
import com.verizon.ads.edition.StandardEdition;
import com.verizon.ads.utils.ThreadUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.zip.DeflaterOutputStream;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM_WITH_THROWABLE;
import static com.verizon.ads.VASAds.ERROR_AD_REQUEST_FAILED;
import static com.verizon.ads.VASAds.ERROR_AD_REQUEST_TIMED_OUT;
import static com.verizon.ads.VASAds.ERROR_NO_FILL;

public class VerizonAdapterConfiguration extends BaseAdapterConfiguration {

    private static final String ADAPTER_VERSION = BuildConfig.VERSION_NAME;
    private static final String BIDDING_TOKEN_VERSION = "1.0";
    private static final String EDITION_NAME_KEY = "editionName";
    private static final String EDITION_VERSION_KEY = "editionVersion";
    private static final String MOPUB_NETWORK_NAME = BuildConfig.NETWORK_NAME;
    private static final String VERIZON_ADS_DOMAIN = "com.verizon.ads";

    private static final Handler handler = new Handler(Looper.getMainLooper());
    private static String biddingToken = null;

    public static final String MEDIATOR_ID = "MoPubVAS-" + ADAPTER_VERSION;
    public static final String SERVER_EXTRAS_AD_CONTENT_KEY = "adm";
    public static final String VAS_SITE_ID_KEY = "siteId";

    static final String REQUEST_METADATA_AD_CONTENT_KEY = "adContent";

    @NonNull
    @Override
    public String getAdapterVersion() {
        return ADAPTER_VERSION;
    }

    @Nullable
    @Override
    public String getBiddingToken(@NonNull Context context) {
        if (biddingToken == null) {
            final String token = getToken();
            biddingToken = getCompressedToken(token);
        }

        return biddingToken;
    }

    @NonNull
    @Override
    public String getMoPubNetworkName() {
        return MOPUB_NETWORK_NAME;
    }

    @NonNull
    @Override
    public String getNetworkSdkVersion() {

        final String editionVersion = Configuration.getString("com.verizon.ads",
                "editionVersion", null);

        final String editionName = Configuration.getString("com.verizon.ads",
                "editionName", null);

        if (!TextUtils.isEmpty(editionVersion) && !TextUtils.isEmpty(editionName)) {
            return editionName + "-" + editionVersion;
        }

        final String adapterVersion = getAdapterVersion();
        return (!TextUtils.isEmpty(adapterVersion)) ? adapterVersion.substring(0,
                adapterVersion.lastIndexOf('.')) : "";
    }

    @Override
    public void initializeNetwork(@NonNull final Context context,
                                  @Nullable final Map<String, String> configuration,
                                  @NonNull final OnNetworkInitializationFinishedListener listener) {

        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(listener);

        final MoPubLog.LogLevel mopubLogLevel = MoPubLog.getLogLevel();

        if (mopubLogLevel == MoPubLog.LogLevel.DEBUG) {
            VASAds.setLogLevel(Logger.DEBUG);
        } else if (mopubLogLevel == MoPubLog.LogLevel.INFO) {
            VASAds.setLogLevel(Logger.INFO);
        }

        String mSiteId = null;

        if (configuration != null) {
            mSiteId = configuration.get(VAS_SITE_ID_KEY);
        }

        // The Verizon SDK needs a meaningful siteId to initialize. siteId is cached on the first request.
        if (TextUtils.isEmpty(mSiteId)) {
            listener.onNetworkInitializationFinished(VerizonAdapterConfiguration.class,
                    MoPubErrorCode.ADAPTER_INITIALIZATION_SUCCESS);

            return;
        }

        final String finalSiteId = mSiteId;
        ThreadUtils.postOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (context instanceof Application && StandardEdition.initialize((Application) context,
                        finalSiteId)) {
                    listener.onNetworkInitializationFinished(VerizonAdapterConfiguration.class,
                            MoPubErrorCode.ADAPTER_INITIALIZATION_SUCCESS);
                } else {
                    listener.onNetworkInitializationFinished(VerizonAdapterConfiguration.class,
                            MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
                }
            }
        });
    }

    public static void postOnUiThread(final Runnable runnable) {
        handler.post(runnable);
    }

    static MoPubErrorCode convertErrorInfoToMoPub(final ErrorInfo errorInfo) {
        if (errorInfo == null) {
            return MoPubErrorCode.UNSPECIFIED;
        }

        switch (errorInfo.getErrorCode()) {
            case ERROR_NO_FILL:
                return MoPubErrorCode.NETWORK_NO_FILL;
            case ERROR_AD_REQUEST_TIMED_OUT:
                return MoPubErrorCode.NETWORK_TIMEOUT;
            case ERROR_AD_REQUEST_FAILED:
            default:
                return MoPubErrorCode.NETWORK_INVALID_STATE;
        }
    }

    public static NativeErrorCode convertErrorInfoToMoPubNative(final ErrorInfo errorInfo) {
        if (errorInfo == null) {
            return NativeErrorCode.UNSPECIFIED;
        }

        switch (errorInfo.getErrorCode()) {
            case ERROR_NO_FILL:
                return NativeErrorCode.NETWORK_NO_FILL;
            case ERROR_AD_REQUEST_TIMED_OUT:
                return NativeErrorCode.NETWORK_TIMEOUT;
            case ERROR_AD_REQUEST_FAILED:
            default:
                return NativeErrorCode.NETWORK_INVALID_STATE;
        }
    }

    private String getToken() {
        final JSONObject biddingTokenJson = new JSONObject();
        final JSONObject envJson = new JSONObject();
        final JSONObject sdkInfoJson = new JSONObject();

        try {
            final String editionName = Configuration.getString(VERIZON_ADS_DOMAIN, EDITION_NAME_KEY,
                    null);
            final String editionVersion = Configuration.getString(VERIZON_ADS_DOMAIN, EDITION_VERSION_KEY,
                    null);

            if (editionName != null && editionVersion != null) {
                sdkInfoJson.put("editionId", String.format("%s-%s", editionName, editionVersion));
            }

            sdkInfoJson.put("version", BIDDING_TOKEN_VERSION);
            envJson.put("sdkInfo", sdkInfoJson);
            biddingTokenJson.put("env", envJson);

            return biddingTokenJson.toString();
        } catch (JSONException e) {
            MoPubLog.log(CUSTOM_WITH_THROWABLE, "Unable to get bidding token.", e);
        }

        return null;
    }


    private String getCompressedToken(final String stringToCompress) {

        if (TextUtils.isEmpty(stringToCompress)) {
            return null;
        }

        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(byteArrayOutputStream);

        try {
            deflaterOutputStream.write(stringToCompress.getBytes());
            deflaterOutputStream.flush();
            deflaterOutputStream.close();

            return Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT);
        } catch (Exception e) {
            MoPubLog.log(CUSTOM_WITH_THROWABLE, "Unable to compress bidding token.", e);
        }

        return null;
    }
}
