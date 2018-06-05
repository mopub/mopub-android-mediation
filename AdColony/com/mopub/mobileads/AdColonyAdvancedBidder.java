package com.mopub.mobileads;


import android.content.Context;

import com.mopub.common.MoPubAdvancedBidder;

/**
 * Certified with AdColony 3.2.1
 *
 * Include this class to use advanced bidding from AdColony.
 */
public class AdColonyAdvancedBidder implements MoPubAdvancedBidder {

    @Override
    public String getToken(final Context context) {
        return "1";
    }

    @Override
    public String getCreativeNetworkName() {
        return "adcolony";
    }
}

