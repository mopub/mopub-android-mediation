package com.mintegral.adapter.common;

import android.util.Log;

import com.mintegral.msdk.MIntegralSDK;
import com.mintegral.msdk.MIntegralUser;
import com.mintegral.msdk.base.common.net.Aa;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * Created by songjunjun on 2018/3/23.
 */

public class AdapterCommonUtil {
    private static String TAG = "AdapterCommonUtil";




    /**
     * report user info
     * @param localExtras
     * @param sdk
     */
    public static void parseLocalExtras(Map<String, Object> localExtras, MIntegralSDK sdk){
        try {
            if(localExtras == null || sdk == null){
                return;
            }
            MIntegralUser user = new MIntegralUser();
            if (localExtras.containsKey(MintegralMopubConstant.REPORT_USER_KEY_AGE_INT)) {
                Log.e(TAG,"parseLocalExtras age:"+localExtras.get(MintegralMopubConstant.REPORT_USER_KEY_AGE_INT));
                user.setAge((int)localExtras.get(MintegralMopubConstant.REPORT_USER_KEY_AGE_INT));
            }
            if (localExtras.containsKey(MintegralMopubConstant.REPORT_USER_KEY_CUSTOM_STR)) {
                Log.e(TAG,"parseLocalExtras custom:"+localExtras.get(MintegralMopubConstant.REPORT_USER_KEY_CUSTOM_STR));
                user.setCustom(localExtras.get(MintegralMopubConstant.REPORT_USER_KEY_CUSTOM_STR).toString());
            }
            if (localExtras.containsKey(MintegralMopubConstant.REPORT_USER_KEY_GENDER_INT)) {
                user.setGender((int)localExtras.get(MintegralMopubConstant.REPORT_USER_KEY_GENDER_INT));
            }
            if (localExtras.containsKey(MintegralMopubConstant.REPORT_USER_KEY_LAT_DOUBLE)) {
                user.setLat((double)localExtras.get(MintegralMopubConstant.REPORT_USER_KEY_LAT_DOUBLE));
            }
            if (localExtras.containsKey(MintegralMopubConstant.REPORT_USER_KEY_LNG_DOUBLE)) {
                user.setLng((double)localExtras.get(MintegralMopubConstant.REPORT_USER_KEY_LNG_DOUBLE));
            }
            if (localExtras.containsKey(MintegralMopubConstant.REPORT_USER_KEY_PAY_INT)) {
                user.setPay((int)localExtras.get(MintegralMopubConstant.REPORT_USER_KEY_PAY_INT));
            }
            sdk.reportUser(user);
        } catch (Throwable t) {
            t.getMessage();
        }
    }

    public static void addChannel(){
        try {
            Aa a = new Aa();
            Class c = a.getClass();
            Method method = c.getDeclaredMethod("b",String.class);
            method.setAccessible(true);
            method.invoke(a,"Y+H6DFttYrPQYcIA+F2F+F5/Hv==");
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
