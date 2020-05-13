package com.mopub.mobileads;

import java.util.Map;

public class AdapterUtil {

    public static float[] getAdSizeSafely(Map<String, Object> params, String widthName, String heightName) {
        float[] adSize = new float[]{0, 0};
        if (params == null || widthName == null || heightName == null) {
            return adSize;
        }

        Object oWidth = params.get(widthName);
        if (oWidth != null) {
            adSize[0] = (float) oWidth;
        }

        Object oHeight = params.get(heightName);

        if (oHeight != null) {
            adSize[1] = (float) oHeight;
        }

        return adSize;
    }

    /**
     * pangolin banner support size ：
     * 600*300、600*400、600*500、600*260、600*90、600*150、640*100、690*388
     *
     * @param params
     * @param widthName
     * @param heightName
     * @return
     */
    public static float[] getBannerAdSizeAdapterSafely(Map<String, Object> params, String widthName, String heightName) {
        //适配 600*300、600*400、600*500、600*260、600*90、600*150、640*100、690*388

        float[] adSize = new float[]{0, 0};
        if (params == null || widthName == null || heightName == null) {
            return adSize;
        }

        Object oHeight = params.get(heightName);

        if (oHeight != null) {
            adSize[1] = (float) oHeight;
        }

        Object oWidth = params.get(widthName);
        if (oWidth != null) {
            adSize[0] = (float) oWidth;


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
