package com.mopub.mobileads;

import android.app.Application;
import android.content.Context;
import android.location.Location;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.webkit.URLUtil;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.BaseAdapterConfiguration;
import com.mopub.common.OnNetworkInitializationFinishedListener;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.verizon.BuildConfig;
import com.mopub.nativeads.NativeErrorCode;
import com.verizon.ads.Configuration;
import com.verizon.ads.EnvironmentInfo;
import com.verizon.ads.ErrorInfo;
import com.verizon.ads.Logger;
import com.verizon.ads.Plugin;
import com.verizon.ads.RequestMetadata;
import com.verizon.ads.VASAds;
import com.verizon.ads.edition.StandardEdition;
import com.verizon.ads.utils.ThreadUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM_WITH_THROWABLE;
import static com.verizon.ads.VASAds.DOMAIN;
import static com.verizon.ads.VASAds.ERROR_AD_REQUEST_FAILED;
import static com.verizon.ads.VASAds.ERROR_AD_REQUEST_TIMED_OUT;
import static com.verizon.ads.VASAds.ERROR_NO_FILL;

public class VerizonAdapterConfiguration extends BaseAdapterConfiguration {

    private static final String ADAPTER_NAME = VerizonAdapterConfiguration.class.getSimpleName();
    private static final String ADAPTER_VERSION = BuildConfig.VERSION_NAME;
    private static final String MOPUB_NETWORK_NAME = BuildConfig.NETWORK_NAME;
    private static final Handler handler = new Handler(Looper.getMainLooper());

    // Advanced Bidding keys
    private static final String APP_DATA_MEDIATOR_KEY = "mediator";
    private static final String DEFAULT_BASE_URL = "https://ads.nexage.com";
    private static final String EDITION_NAME_KEY = "editionName";
    private static final String EDITION_VERSION_KEY = "editionVersion";
    private static final String PLACEMENT_DATA_IMP_GROUP_KEY = "impressionGroup";
    private static final String PLACEMENT_DATA_REFRESH_RATE_KEY = "refreshRate";
    private static final String VERIZON_ADS_DOMAIN = "com.verizon.ads";
    private static final String WATERFALL_PROVIDER_BASE_URL_KEY = "waterfallProviderBaseUrl";

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
        final RequestMetadata.Builder requestMetadataBuilder = new RequestMetadata.Builder(VASAds.getRequestMetadata());
        requestMetadataBuilder.setMediator(VerizonAdapterConfiguration.MEDIATOR_ID);

        return buildBiddingToken(requestMetadataBuilder.build(), new EnvironmentInfo(context));
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

        String siteId = null;

        if (configuration != null) {
            siteId = configuration.get(VAS_SITE_ID_KEY);
        }

        // The Verizon SDK needs a meaningful siteId to initialize. siteId is cached on the first request.
        if (TextUtils.isEmpty(siteId)) {
            listener.onNetworkInitializationFinished(VerizonAdapterConfiguration.class,
                    MoPubErrorCode.ADAPTER_INITIALIZATION_SUCCESS);

            return;
        }

        final String finalSiteId = siteId;
        ThreadUtils.postOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (context instanceof Application && StandardEdition.initialize((Application) context, finalSiteId)) {
                    listener.onNetworkInitializationFinished(VerizonAdapterConfiguration.class,
                            MoPubErrorCode.ADAPTER_INITIALIZATION_SUCCESS);
                } else {
                    listener.onNetworkInitializationFinished(VerizonAdapterConfiguration.class,
                            MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
                }
            }
        });
    }

    private static String buildBiddingToken(final RequestMetadata requestMetadata, final EnvironmentInfo environmentInfo) {
        try {
            final JSONObject json = new JSONObject();

            json.put("env", buildEnvironmentInfoJSON(environmentInfo));
            json.put("req", buildRequestInfoJSON(requestMetadata));

            return json.toString();
        } catch (Exception e) {
            MoPubLog.log(CUSTOM_WITH_THROWABLE, ADAPTER_NAME, "Error creating JSON: " + e);
        }

        return null;
    }

    private static JSONObject buildEnvironmentInfoJSON(final EnvironmentInfo environmentInfo) throws JSONException {
        final JSONObject json = new JSONObject();
        final JSONObject sdkInfo = new JSONObject();

        final EnvironmentInfo.DeviceInfo deviceInfo = environmentInfo.getDeviceInfo();
        final EnvironmentInfo.NetworkOperatorInfo networkOperatorInfo = environmentInfo.getNetworkOperatorInfo();

        final String editionName = Configuration.getString(VERIZON_ADS_DOMAIN, EDITION_NAME_KEY, null);
        final String editionVersion = Configuration.getString(VERIZON_ADS_DOMAIN, EDITION_VERSION_KEY, null);

        sdkInfo.put("coreVer", VASAds.getSDKInfo().version);

        if (editionName != null && editionVersion != null) {
            sdkInfo.put("editionId", String.format("%s-%s", editionName, editionVersion));
        }

        final Set<Plugin> registeredPlugins = VASAds.getRegisteredPlugins();

        if (!registeredPlugins.isEmpty()) {
            final JSONObject sdkPlugins = new JSONObject();

            for (Plugin registeredPlugin : registeredPlugins) {
                final JSONObject jsonRegisteredPlugin = new JSONObject();

                jsonRegisteredPlugin.put("name", registeredPlugin.getName());
                jsonRegisteredPlugin.put("version", registeredPlugin.getVersion());
                jsonRegisteredPlugin.put("author", registeredPlugin.getAuthor());
                jsonRegisteredPlugin.put("email", registeredPlugin.getEmail());
                jsonRegisteredPlugin.put("website", registeredPlugin.getWebsite());
                jsonRegisteredPlugin.put("minApiLevel", registeredPlugin.getMinApiLevel());
                jsonRegisteredPlugin.put("enabled", VASAds.isPluginEnabled(registeredPlugin.getId()));

                sdkPlugins.put(registeredPlugin.getId(), jsonRegisteredPlugin);
            }

            sdkInfo.put("sdkPlugins", sdkPlugins);
        }

        json.put("sdkInfo", sdkInfo);

        if (networkOperatorInfo != null) {
            putIfNotNull(json, "mcc", networkOperatorInfo.getMCC());
            putIfNotNull(json, "mnc", networkOperatorInfo.getMNC());
            putIfNotNull(json, "cellSignalDbm", networkOperatorInfo.getCellSignalDbm());
        }

        json.put("lang", deviceInfo.getLanguage());

        final String requestUrl = Configuration.get(VERIZON_ADS_DOMAIN, WATERFALL_PROVIDER_BASE_URL_KEY, String.class,
                DEFAULT_BASE_URL);

        if (URLUtil.isHttpsUrl(requestUrl)) {
            json.put("secureContent", true);
        }

        // Device properties
        json.put("natOrient", deviceInfo.getNaturalOrientation());
        putIfNotNull(json, "storage", deviceInfo.getAvailableStorage());
        putIfNotNull(json, "vol", deviceInfo.getVolume(AudioManager.STREAM_MUSIC));
        putIfNotNull(json, "headphones", deviceInfo.hasHeadphonesPluggedIn());

        // Battery state
        putIfNotNull(json, "charging", deviceInfo.isCharging());
        putIfNotNull(json, "charge", deviceInfo.getBatteryLevel());

        // Network state
        putIfNotNull(json, "ip", deviceInfo.getIP());

        // Location date
        final Location location = environmentInfo.getLocation();

        if (location != null && VASAds.isLocationEnabled()) {
            final JSONObject locationJson = new JSONObject();

            locationJson.put("lat", location.getLatitude());
            locationJson.put("lon", location.getLongitude());
            locationJson.put("src", location.getProvider());

            // convert from MS to seconds so the server gets the expected timestamp format
            locationJson.put("ts", location.getTime() / 1000);

            if (location.hasAccuracy()) {
                locationJson.put("horizAcc", location.getAccuracy());
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (location.hasVerticalAccuracy()) {
                    locationJson.put("vertAcc", location.getVerticalAccuracyMeters());
                }
            }

            if (location.hasSpeed()) {
                locationJson.put("speed", location.getSpeed());
            }

            if (location.hasBearing()) {
                locationJson.put("bearing", location.getBearing());
            }

            if (location.hasAltitude()) {
                locationJson.put("alt", location.getAltitude());
            }

            json.put("loc", locationJson);
        }

        final JSONObject deviceFeatures = new JSONObject();
        final EnvironmentInfo.CameraType[] cameraTypes = deviceInfo.getCameras();

        for (EnvironmentInfo.CameraType cameraType : cameraTypes) {
            if (cameraType == EnvironmentInfo.CameraType.FRONT) {
                deviceFeatures.put("cameraFront", "true");
            } else if (cameraType == EnvironmentInfo.CameraType.BACK) {
                deviceFeatures.put("cameraRear", "true");
            }
        }

        putAsStringIfNotNull(deviceFeatures, "nfc", deviceInfo.hasNFC());
        putAsStringIfNotNull(deviceFeatures, "bt", deviceInfo.hasBluetooth());
        putAsStringIfNotNull(deviceFeatures, "mic", deviceInfo.hasMicrophone());
        putAsStringIfNotNull(deviceFeatures, "gps", deviceInfo.hasGPS());

        putIfTrue(json, "deviceFeatures", deviceFeatures, !VASAds.isAnonymous());

        return json;
    }

    private static JSONObject buildRequestInfoJSON(final RequestMetadata requestMetadata) throws JSONException {
        final JSONObject json = new JSONObject();

        putIfNotNull(json, "gdpr", isProtectedByGDPR());

        if (requestMetadata == null) {
            return json;
        }

        final Map<String, Object> appInfo = requestMetadata.getAppData();

        if (appInfo != null) {
            json.put("mediator", appInfo.get(APP_DATA_MEDIATOR_KEY));
        }

        final Map<String, Object> placementData = requestMetadata.getPlacementData();

        if (placementData != null) {
            putIfNotNull(json, "grp", placementData.get(PLACEMENT_DATA_IMP_GROUP_KEY));

            final Map<String, String> customTargeting = requestMetadata.getCustomTargeting();
            final JSONObject customTargetingJSON = toJSONObject(customTargeting);

            if (customTargetingJSON != null && customTargetingJSON.length() > 0) {
                json.put("targeting", customTargetingJSON);
            }

            final Map<String, ?> consentData = Configuration.get(DOMAIN, VASAds.USER_CONSENT_DATA_KEY, Map.class, null);
            final JSONObject consentDataJSON = toJSONObject(consentData);

            if (consentDataJSON != null && consentDataJSON.length() > 0) {
                json.put("consentstrings", consentDataJSON);
            }

            json.put("keywords", toJSONArray(requestMetadata.getKeywords()));
            json.put("refreshRate", placementData.get(PLACEMENT_DATA_REFRESH_RATE_KEY));
        }

        return json;
    }

    private static Boolean isProtectedByGDPR() {
        final boolean restrictedOrigin = isRestrictedOrigin();
        final Boolean locationRequiresConsent = doesLocationRequireConsent();

        if (locationRequiresConsent != null) {
            return (locationRequiresConsent || restrictedOrigin);
        } else {
            if (restrictedOrigin) {
                return true;
            } else {
                return null;
            }
        }
    }

    private static boolean isRestrictedOrigin() {
        return Configuration.getBoolean(DOMAIN, VASAds.USER_RESTRICTED_ORIGIN_KEY, false);
    }

    private static Boolean doesLocationRequireConsent() {
        return Configuration.get(DOMAIN, VASAds.LOCATION_REQUIRES_CONSENT_KEY, Boolean.class, null);
    }

    private static void putIfNotNull(final JSONObject jsonObject, final String key, final Object value) {
        if (key == null) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unable to put value, specified key is null");

            return;
        }

        if (value == null) {
            return;
        }

        try {
            jsonObject.put(key, value);
        } catch (Exception e) {
            MoPubLog.log(CUSTOM_WITH_THROWABLE, ADAPTER_NAME, "Error adding " + key + ":" + value + " to JSON: " + e);
        }
    }

    private static void putIfTrue(final JSONObject jsonObject, final String key, final Object value, final Boolean inject) {
        if (Boolean.TRUE.equals(inject)) {
            putIfNotNull(jsonObject, key, value);
        }
    }

    private static void putAsStringIfNotNull(final JSONObject jsonObject, final String key, final Object value) {
        if (value == null) {
            return;
        }

        putIfNotNull(jsonObject, key, String.valueOf(value));
    }

    private static JSONArray toJSONArray(final Collection objects) {
        if (objects == null) {
            return null;
        }

        final JSONArray json = new JSONArray();
        for (Object entry : objects) {
            json.put(buildFromObject(entry));
        }

        return json;
    }

    private static JSONObject toJSONObject(final Map<String, ?> map) {
        if (map == null) {
            return null;
        }

        final JSONObject json = new JSONObject();
        try {
            for (Map.Entry<String, ?> entry : map.entrySet()) {
                json.put(entry.getKey(), buildFromObject(entry.getValue()));
            }
        } catch (Exception e) {
            MoPubLog.log(CUSTOM_WITH_THROWABLE, ADAPTER_NAME, "Error building JSON from Map: " + e);
        }

        return json;
    }

    private static Object buildFromObject(final Object value) {
        try {
            if (value instanceof Map) {
                return toJSONObject((Map<String, ?>) value);
            } else if (value instanceof List) {
                return toJSONArray((List) value);
            } else {
                return value;
            }
        } catch (Exception e) {
            MoPubLog.log(CUSTOM_WITH_THROWABLE, ADAPTER_NAME, "Error building JSON from Object: " + e);
        }

        return "";
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
}
