package com.mopub.mobileads;

import android.location.Location;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.webkit.URLUtil;

import com.mopub.common.logging.MoPubLog;
import com.mopub.nativeads.NativeErrorCode;
import com.verizon.ads.Configuration;
import com.verizon.ads.EnvironmentInfo;
import com.verizon.ads.ErrorInfo;
import com.verizon.ads.Plugin;
import com.verizon.ads.RequestMetadata;
import com.verizon.ads.VASAds;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.verizon.ads.VASAds.DOMAIN;
import static com.verizon.ads.VASAds.ERROR_AD_REQUEST_FAILED;
import static com.verizon.ads.VASAds.ERROR_AD_REQUEST_TIMED_OUT;
import static com.verizon.ads.VASAds.ERROR_NO_FILL;


public final class VerizonUtils {

    private static final String LOG_CLASS_NAME = VerizonUtils.class.getSimpleName();
    private static final Handler handler = new Handler(Looper.getMainLooper());

    private static final String DEFAULT_BASE_URL = "https://ads.nexage.com";
    private static final String VERIZON_ADS_DOMAIN = "com.verizon.ads";
    private static final String EDITION_NAME_KEY = "editionName";
    private static final String EDITION_VERSION_KEY = "editionVersion";
    private static final String WATERFALL_PROVIDER_BASE_URL_KEY = "waterfallProviderBaseUrl";
    private static final String APP_DATA_MEDIATOR_KEY = "mediator";
    private static final String PLACEMENT_DATA_IMP_GROUP_KEY = "impressionGroup";
    private static final String PLACEMENT_DATA_REFRESH_RATE_KEY = "refreshRate";


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

    static String buildBiddingToken(final RequestMetadata requestMetadata, final EnvironmentInfo environmentInfo) {
        try {
            JSONObject json = new JSONObject();

            json.put("env", buildEnvironmentInfoJSON(environmentInfo));
            json.put("req", buildRequestInfoJSON(requestMetadata));

            // Serialize the json request to a string and return
            return json.toString();

        } catch (Exception e) {
            MoPubLog.log(CUSTOM, LOG_CLASS_NAME, "Error creating JSON: " + e);
        }

        return null;
    }

    private static JSONObject buildEnvironmentInfoJSON(final EnvironmentInfo environmentInfo) throws JSONException {
        JSONObject json = new JSONObject();

        final EnvironmentInfo.DeviceInfo deviceInfo = environmentInfo.getDeviceInfo();
        final EnvironmentInfo.NetworkOperatorInfo networkOperatorInfo = environmentInfo.getNetworkOperatorInfo();

        JSONObject sdkInfo = new JSONObject();
        sdkInfo.put("coreVer", VASAds.getSDKInfo().version);

        String editionName = Configuration.getString(VERIZON_ADS_DOMAIN, EDITION_NAME_KEY, null);
        String editionVersion = Configuration.getString(VERIZON_ADS_DOMAIN, EDITION_VERSION_KEY, null);
        if (editionName != null && editionVersion != null) {
            sdkInfo.put("editionId", String.format("%s-%s", editionName, editionVersion));
        }

        Set<Plugin> registeredPlugins = VASAds.getRegisteredPlugins();
        if (!registeredPlugins.isEmpty()) {
            JSONObject sdkPlugins = new JSONObject();

            for (Plugin registeredPlugin : registeredPlugins) {
                JSONObject jsonRegisteredPlugin = new JSONObject();
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

        // General properties
        if (networkOperatorInfo != null) {
            putIfNotNull(json, "mcc", networkOperatorInfo.getMCC());
            putIfNotNull(json, "mnc", networkOperatorInfo.getMNC());
            putIfNotNull(json, "cellSignalDbm", networkOperatorInfo.getCellSignalDbm());
        }
        json.put("lang", deviceInfo.getLanguage());

        String requestUrl = Configuration.get(VERIZON_ADS_DOMAIN, WATERFALL_PROVIDER_BASE_URL_KEY, String.class,
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
        Location location = environmentInfo.getLocation();
        if (location != null && VASAds.isLocationEnabled()) {
            JSONObject locationJson = new JSONObject();

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

        JSONObject deviceFeatures = new JSONObject();

        EnvironmentInfo.CameraType[] cameraTypes = deviceInfo.getCameras();

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
        JSONObject json = new JSONObject();

        putIfNotNull(json, "gdpr", isProtectedByGDPR());

        if (requestMetadata == null) {
            return json;
        }

        Map<String, Object> appInfo = requestMetadata.getAppData();
        if (appInfo != null) {
            json.put("mediator", appInfo.get(APP_DATA_MEDIATOR_KEY));
        }

        Map<String, Object> placementData = requestMetadata.getPlacementData();

        if (placementData != null) {
            putIfNotNull(json, "grp", placementData.get(PLACEMENT_DATA_IMP_GROUP_KEY));

            Map<String, String> customTargeting = requestMetadata.getCustomTargeting();
            JSONObject customTargetingJSON = toJSONObject(customTargeting);

            if (customTargetingJSON != null && customTargetingJSON.length() > 0) {
                json.put("targeting", customTargetingJSON);
            }

            Map<String, String> consentData =
                Configuration.get(DOMAIN, VASAds.USER_CONSENT_DATA_KEY, Map.class, null);

            JSONObject consentDataJSON = toJSONObject(consentData);
            if (consentDataJSON != null && consentDataJSON.length() > 0) {
                json.put("consentstrings", consentDataJSON);
            }

            json.put("keywords", toJSONArray(requestMetadata.getKeywords()));
            json.put("refreshRate", placementData.get(PLACEMENT_DATA_REFRESH_RATE_KEY));
        }

        return json;
    }

    private static Boolean isProtectedByGDPR() {
        /*
         origin -> restricted origin
         location -> IP location requires consent
         origin  location  GDPR
         YES      unknown   YES
         NO       unknown   null
         YES      YES       YES
         YES      NO        YES
         NO       YES       YES
         NO       NO        NO
         */

        boolean restrictedOrigin = isRestrictedOrigin();
        Boolean locationRequiresConsent = doesLocationRequireConsent();

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
        // Null means location has not been determined (unknown)
        return Configuration.get(DOMAIN, VASAds.LOCATION_REQUIRES_CONSENT_KEY, Boolean.class, null);
    }

    private static void putIfNotNull(final JSONObject jsonObject, final String key, final Object value) {
        if (key == null) {
            MoPubLog.log(CUSTOM, LOG_CLASS_NAME, "Unable to put value, specified key is null");

            return;
        }

        // this is a common and expected occurrence, don't log
        if (value == null) {
            return;
        }

        try {
            jsonObject.put(key, value);
        } catch (Exception e) {
            MoPubLog.log(CUSTOM, LOG_CLASS_NAME, "Error adding " + key + ":" + value + " to JSON: " + e);
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

        JSONArray json = new JSONArray();
        for (Object entry : objects) {
            json.put(buildFromObject(entry));
        }

        return json;
    }

    private static JSONObject toJSONObject(final Map<String, ?> map) {
        if (map == null) {
            return null;
        }

        JSONObject json = new JSONObject();
        try {
            for (Map.Entry<String, ?> entry : map.entrySet()) {
                json.put(entry.getKey(), buildFromObject(entry.getValue()));
            }
        } catch (Exception e) {
            MoPubLog.log(CUSTOM, LOG_CLASS_NAME, "Error building JSON from Map: " + e);
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
            MoPubLog.log(CUSTOM, LOG_CLASS_NAME, "Error building JSON from Object: " + e);
        }

        return "";
    }
}
