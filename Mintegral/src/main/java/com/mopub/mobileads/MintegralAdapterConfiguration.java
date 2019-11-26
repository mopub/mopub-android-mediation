package com.mopub.mobileads;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mintegral.msdk.MIntegralConstans;
import com.mintegral.msdk.MIntegralSDK;
import com.mintegral.msdk.MIntegralUser;
import com.mintegral.msdk.base.common.net.Aa;
import com.mintegral.msdk.mtgbid.out.BidManager;
import com.mintegral.msdk.out.MIntegralSDKFactory;
import com.mintegral.msdk.out.MTGConfiguration;
import com.mopub.common.BaseAdapterConfiguration;
import com.mopub.common.MoPub;
import com.mopub.common.OnNetworkInitializationFinishedListener;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.mintegral.BuildConfig;

import java.lang.reflect.Method;
import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM_WITH_THROWABLE;

public class MintegralAdapterConfiguration extends BaseAdapterConfiguration {

    public static final String APP_ID_KEY = "appId";
    public static final String APP_KEY = "appKey";
    public static final String UNIT_ID_KEY = "unitId";
    static final String USER_ID_KEY = "Rewarded-Video-Customer-Id";

    // Mintegral targeting keys
    public static final String AGE_TARGETING_KEY = "age";
    public static final String CUSTOM_TARGETING_KEY = "custom";
    public static final String GENDER_TARGETING_KEY = "gender";
    public static final String LATITUDE_TARGETING_KEY = "lat";
    public static final String LONGITUDE_TARGETING_KEY = "lng";
    public static final String PAY_TARGETING_KEY = "pay";

    private static final String ADAPTER_VERSION = BuildConfig.VERSION_NAME;
    private static final String SDK_VERSION = MTGConfiguration.SDK_VERSION;
    private static final String MOPUB_NETWORK_NAME = BuildConfig.NETWORK_NAME;

    @NonNull
    @Override
    public String getAdapterVersion() {
        return ADAPTER_VERSION;
    }

    @Nullable
    @Override
    public String getBiddingToken(@NonNull Context context) {
        Preconditions.checkNotNull(context);

        return BidManager.getBuyerUid(context);
    }

    @NonNull
    @Override
    public String getMoPubNetworkName() {
        return MOPUB_NETWORK_NAME;
    }

    @NonNull
    @Override
    public String getNetworkSdkVersion() {
        return SDK_VERSION;
    }

    @Override
    public void initializeNetwork(@NonNull Context context, @Nullable Map<String, String> configuration,
                                  @NonNull OnNetworkInitializationFinishedListener listener) {

        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(listener);

        try {
            if (configuration != null && !configuration.isEmpty()) {
                final String appId = configuration.get(APP_ID_KEY);
                final String appKey = configuration.get(APP_KEY);

                if (!TextUtils.isEmpty(appId) && !TextUtils.isEmpty(appKey)) {
                    configureMintegral(appId, appKey, context);

                    listener.onNetworkInitializationFinished(MintegralAdapterConfiguration.class,
                            MoPubErrorCode.ADAPTER_INITIALIZATION_SUCCESS);
                } else {
                    listener.onNetworkInitializationFinished(MintegralAdapterConfiguration.class,
                            MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
                }
            }
        } catch (Exception e) {
            MoPubLog.log(CUSTOM_WITH_THROWABLE, "Failed to initialize the Mintegral SDK due " +
                    "to an exception", e);
        }
    }

    public static void configureMintegral(String appId, String appKey, Context context) {
        final MIntegralSDK sdk = MIntegralSDKFactory.getMIntegralSDK();

        if (sdk != null) {
            final Map<String, String> mtgConfigurationMap = sdk.getMTGConfigurationMap(appId, appKey);

            if (context instanceof Activity) {
                sdk.init(mtgConfigurationMap, ((Activity) context).getApplication());
            } else if (context instanceof Application) {
                sdk.init(mtgConfigurationMap, context);
            }

            handleGdpr(MoPub.canCollectPersonalInformation(), sdk, context);
        } else {
            MoPubLog.log(CUSTOM, "Failed to initialize the Mintegral SDK because the SDK " +
                    "instance is null.");
        }
    }

    static void handleGdpr(boolean canCollectPersonalInfo, MIntegralSDK sdk, Context context) {
        if (canCollectPersonalInfo) {
            sdk.setUserPrivateInfoType(context, MIntegralConstans.AUTHORITY_ALL_INFO,
                    MIntegralConstans.IS_SWITCH_ON);
        } else {
            sdk.setUserPrivateInfoType(context, MIntegralConstans.AUTHORITY_ALL_INFO,
                    MIntegralConstans.IS_SWITCH_OFF);
        }
    }

    public static void setTargeting(Map<String, Object> localExtras, MIntegralSDK sdk) {
        try {
            if (localExtras == null || localExtras.isEmpty() || sdk == null) {
                MoPubLog.log(CUSTOM, "Failed to set ad targeting for Mintegral.");
                return;
            }

            final MIntegralUser user = new MIntegralUser();

            final Object ageObj = localExtras.get(AGE_TARGETING_KEY);

            if (ageObj instanceof Integer) {
                final int age = (int) ageObj;
                user.setAge(age);
            }

            final Object customObj = localExtras.get(CUSTOM_TARGETING_KEY);

            if (customObj instanceof String) {
                final String customData = (String) customObj;
                user.setCustom(customData);
            }

            final Object genderObj = localExtras.get(GENDER_TARGETING_KEY);

            if (genderObj instanceof Integer) {
                final int gender = (int) genderObj;
                user.setGender(gender);
            }

            final Object latitudeObj = localExtras.get(LATITUDE_TARGETING_KEY);

            if (latitudeObj instanceof Double) {
                final double latitude = (double) latitudeObj;
                user.setLat(latitude);
            }

            final Object longitudeObj = localExtras.get(LONGITUDE_TARGETING_KEY);

            if (longitudeObj instanceof Double) {
                final double longitude = (double) longitudeObj;
                user.setLng(longitude);
            }

            final Object payObj = localExtras.get(PAY_TARGETING_KEY);

            if (payObj instanceof Integer) {
                final int pay = (int) payObj;
                user.setPay(pay);
            }

            sdk.reportUser(user);
        } catch (Throwable t) {
            MoPubLog.log(CUSTOM_WITH_THROWABLE, "Failed to set ad targeting for Mintegral.", t);
        }
    }

    public static void addChannel() {
        try {
            final Aa a = new Aa();
            final Class c = a.getClass();

            final Method method = c.getDeclaredMethod("b", String.class);
            method.setAccessible(true);
            method.invoke(a, "Y+H6DFttYrPQYcIA+F2F+F5/Hv==");
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
