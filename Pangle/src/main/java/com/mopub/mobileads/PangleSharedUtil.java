package com.mopub.mobileads;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.util.Map;

public class PangleSharedUtil {

    public static final int CONTENT_TYPE = 40000;//# http conent_type
    public static final int REQUEST_PB_ERROR = 40001;//# http request pb
    public static final int NO_AD = 20001;//# no ad
    public static final int ADSLOT_EMPTY = 40004;// ad code id can't been null
    public static final int ADSLOT_ID_ERROR = 40006;// code id error

    public static MoPubErrorCode mapErrorCode(int error) {
        switch (error) {
            case PangleSharedUtil.CONTENT_TYPE:
            case PangleSharedUtil.REQUEST_PB_ERROR:
                return MoPubErrorCode.NO_CONNECTION;
            case PangleSharedUtil.NO_AD:
                return MoPubErrorCode.NETWORK_NO_FILL;
            case PangleSharedUtil.ADSLOT_EMPTY:
            case PangleSharedUtil.ADSLOT_ID_ERROR:
                return MoPubErrorCode.MISSING_AD_UNIT_ID;
            default:
                return MoPubErrorCode.UNSPECIFIED;
        }
    }

    public static float[] getAdSizeSafely(Map<String, Object> params, String widthName, String heightName) {
        float[] adSize = new float[]{0, 0};
        if (params == null || widthName == null || heightName == null) {
            return adSize;
        }

        Object oWidth = params.get(widthName);
        if (oWidth != null) {
            String w = String.valueOf(oWidth);
            adSize[0] = Float.valueOf(w);
        }

        Object oHeight = params.get(heightName);

        if (oHeight != null) {
            String h = String.valueOf(oHeight);
            adSize[1] = Float.valueOf(h);
        }

        return adSize;
    }

    public static int dp2px(Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

    public static float getScreenWidth(Context context) {
        if (context == null) return -1;
        return (float) context.getResources().getDisplayMetrics().widthPixels;
    }

    public static float getScreenHeight(Context context) {
        if (context == null) return -1;
        return (float) context.getResources().getDisplayMetrics().heightPixels;
    }


    public static int pxtosp(Context context, float pxValue) {
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (pxValue / fontScale + 0.5f);
    }

    public static int sptopx(Context context, float spValue) {
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * fontScale + 0.5f);
    }

    /**
     * pangle banner support size ：
     * 600*300、600*400、600*500、600*260、600*90、600*150、640*100、690*388
     *
     * @param params
     * @param widthName
     * @param heightName
     * @return
     */
    public static float[] getBannerAdSizeAdapterSafely(Map<String, Object> params, String widthName, String heightName) {
        float[] adSize = new float[]{0, 0};
        if (params == null || widthName == null || heightName == null) {
            return adSize;
        }

        Object oHeight = params.get(heightName);

        if (oHeight != null) {
            adSize[1] = (float) ((Integer) oHeight);
        }

        Object oWidth = params.get(widthName);
        if (oWidth != null) {
            adSize[0] = (float) ((Integer) oWidth);

            if (adSize[0] > 0 && adSize[0] <= 600) {
                adSize[0] = 600;
                if (adSize[1] <= 100) {
                    adSize[1] = 90;
                } else if (adSize[1] <= 150) {
                    adSize[1] = 150;
                } else if (adSize[1] <= 260) {
                    adSize[1] = 260;
                } else if (adSize[1] <= 300) {
                    adSize[1] = 300;
                } else if (adSize[1] <= 450) {
                    adSize[1] = 400;
                } else {
                    adSize[1] = 500;
                }
            } else if (adSize[0] > 600 && adSize[0] <= 640) {
                adSize[0] = 640;
                adSize[1] = 100;
            } else {
                adSize[0] = 690;
                adSize[1] = 388;
            }
        }

        return adSize;
    }

}
