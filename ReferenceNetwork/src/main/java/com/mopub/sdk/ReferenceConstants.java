// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.sdk;

/**
 * A simple network SDK implementation that works with the reference adapters.
 * <p>
 * INTERNAL USE ONLY. DO NOT REFERENCE OTHERWISE.
 */
public class ReferenceConstants {
    public static final String FULLSCREEN_MARKUP_KEY = "fullscreen_ad_markup";
    public static final String CLICKTHROUGH_URL = "http://www.mopub.com";
    public static final String BROADCAST_IDENTIFIER_KEY = "broadcast_identifier";
    public static final String REWARDED_KEY = "rewarded";

    // Sample ad markup
    public static final String VIDEO_MARKUP = "https://cdn.cms-mpdigitalassets.com/content/dam/" +
            "mopub-aem-twitter/videos/MoPub_ElevateYourGame_video-min.mp4";
    public static final String STATIC_MARKUP = "<html> <body style=\"justify-content: center;align-items: " +
            "center; background-color: %231DA1F2; font-family: 'Helvetica Neue', Helvetica, Arial, " +
            "sans-serif; display: flex;\"> <p style='color: white; font-size: xx-large'>MEDIATION TEST AD</p> </body> </html>";

    public static final String NATIVE_MAIN_IMAGE_URL = "https://images.squarespace-cdn.com/content/v1/5" +
            "4992035e4b0c49940a59e49/1586714639270-6O27VM796316NPD4R3DH/ke17ZwdGBToddI8pDm48kM67dVXED_y" +
            "MBjHtlg_uL0sUqsxRUqqbr1mOJYKfIPR7LoDQ9mXPOjoJoqy81S2I8N_N4V1vUb5AoIIIbLZhVYxCRW4BPu10St3TBA" +
            "UQYVKcNPDvIAs8f1WUKmWSu3fO4mWtI-U7sMj89S5lCDO4xNISmZH7m9nvOKn--z65CT5h/image-asset.jpeg";
    public static final String NATIVE_ICON_IMAGE_URL = "https://cdn.vox-cdn.com/thumbor/RsL5FNihoaV9od" +
            "gkWQWIATp1xr0=/0x16:1103x751/1400x1400/filters:focal(0x16:1103x751):format(png)/cdn.vox-cdn." +
            "com/uploads/chorus_image/image/46840054/Screenshot_2015-07-27_15.11.13.0.0.png";
    public static final String NATIVE_ADVERTISER_NAME = "Test Advertiser";
    public static final String NATIVE_AD_TITLE = "Test Title";
    public static final String NATIVE_BODY_TEXT = "Test Body";
    public static final String NATIVE_CTA = "Test";
    public static final String NATIVE_SPONSORED = "Test Sponsor";
    public static final String NATIVE_OPT_OUT_URL = "https://www.mopub.com/en/legal/optout";
}
