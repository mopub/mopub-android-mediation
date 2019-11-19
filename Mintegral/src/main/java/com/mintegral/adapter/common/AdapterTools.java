package com.mintegral.adapter.common;

import android.util.Log;

import com.mopub.common.MoPub;

import java.util.ArrayList;

/**
 * Created by songjunjun on 2018/5/8.
 */

public class AdapterTools {
    private static String TAG = "AdapterTools";



    private static ArrayList<String> keyList = new ArrayList<String>();

    /**
     * 是否可以收集用户数据
     * @return
     */
    public static boolean canCollectPersonalInformation(){
        boolean canCollect = false;
        //如果是gdpr才检查是否可以收集用户
//        if(MoPub.getPersonalInformationManager().gdprApplies()){
            Log.e(TAG,"GDPR   applicatin");
            canCollect = MoPub.canCollectPersonalInformation();
//        }
        Log.e(TAG,"GDPR   applicatin canCollect:"+canCollect);
        return canCollect;
    }


}
