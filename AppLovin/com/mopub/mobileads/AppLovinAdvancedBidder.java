package com.mopub.mobileads;

import android.content.Context;

import com.applovin.sdk.AppLovinSdk;
import com.mopub.common.MoPubAdvancedBidder;

/**
 * Include this class to use advanced bidding from AppLovin.
 * <p>
 * Created by Thomas So on 5/22/18.
 */
public class AppLovinAdvancedBidder
        implements MoPubAdvancedBidder {
    @Override
    public String getCreativeNetworkName() {
        return "applovin_sdk";
    }

    @Override
    public String getToken(final Context context) {
        return AppLovinSdk.getInstance(context).getAdService().getBidToken();
    }
}
