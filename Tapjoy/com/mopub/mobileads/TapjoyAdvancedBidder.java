package com.mopub.mobileads;

import android.content.Context;

import com.mopub.common.MoPubAdvancedBidder;
import com.tapjoy.Tapjoy;

public class TapjoyAdvancedBidder implements MoPubAdvancedBidder {
    @Override
    public String getToken(final Context context) {
        return "1";
    }

    @Override
    public String getCreativeNetworkName() {
        return "tapjoy";
    }
}
