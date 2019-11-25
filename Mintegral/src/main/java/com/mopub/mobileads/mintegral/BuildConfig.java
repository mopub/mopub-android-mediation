package com.mopub.mobileads.mintegral;

import android.util.Log;

import com.mintegral.msdk.MIntegralSDK;
import com.mintegral.msdk.MIntegralUser;
import com.mintegral.msdk.base.common.net.Aa;
import com.mintegral.msdk.out.MTGConfiguration;

import java.lang.reflect.Method;
import java.util.Map;

public class BuildConfig {
    public static String ADAPTER_VERSION = "3.5.0";
    public static final String SDK_VERSION = MTGConfiguration.SDK_VERSION;
    public static final String NETWORK_NAME = "mintegral_sdk";


    public static String REPORT_USER_KEY_AGE_INT = "age";

    public static String REPORT_USER_KEY_CUSTOM_STR = "custom";

    public static String REPORT_USER_KEY_GENDER_INT = "gender";

    public static String REPORT_USER_KEY_LAT_DOUBLE = "lat";

    public static String REPORT_USER_KEY_LNG_DOUBLE = "lng";

    public static String REPORT_USER_KEY_PAY_INT = "pay";


    public static void parseLocalExtras(Map<String, Object> localExtras, MIntegralSDK sdk) {
        try {
            if (localExtras == null || sdk == null) {
                return;
            }
            MIntegralUser user = new MIntegralUser();
            if (localExtras.containsKey(BuildConfig.REPORT_USER_KEY_AGE_INT)) {
                user.setAge((int) localExtras.get(BuildConfig.REPORT_USER_KEY_AGE_INT));
            }
            if (localExtras.containsKey(BuildConfig.REPORT_USER_KEY_CUSTOM_STR)) {
                user.setCustom(localExtras.get(BuildConfig.REPORT_USER_KEY_CUSTOM_STR).toString());
            }
            if (localExtras.containsKey(BuildConfig.REPORT_USER_KEY_GENDER_INT)) {
                user.setGender((int) localExtras.get(BuildConfig.REPORT_USER_KEY_GENDER_INT));
            }
            if (localExtras.containsKey(BuildConfig.REPORT_USER_KEY_LAT_DOUBLE)) {
                user.setLat((double) localExtras.get(BuildConfig.REPORT_USER_KEY_LAT_DOUBLE));
            }
            if (localExtras.containsKey(BuildConfig.REPORT_USER_KEY_LNG_DOUBLE)) {
                user.setLng((double) localExtras.get(BuildConfig.REPORT_USER_KEY_LNG_DOUBLE));
            }
            if (localExtras.containsKey(BuildConfig.REPORT_USER_KEY_PAY_INT)) {
                user.setPay((int) localExtras.get(BuildConfig.REPORT_USER_KEY_PAY_INT));
            }
            sdk.reportUser(user);
        } catch (Throwable t) {
            t.getMessage();
        }
    }

    public static void addChannel() {
        try {
            Aa a = new Aa();
            Class c = a.getClass();
            Method method = c.getDeclaredMethod("b", String.class);
            method.setAccessible(true);
            method.invoke(a, "Y+H6DFttYrPQYcIA+F2F+F5/Hv==");
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

}