package com.mopub.mobileads;

import android.content.Context;
import android.support.annotation.NonNull;

import com.facebook.ads.BidderTokenProvider;
import com.mopub.common.MoPubAdvancedBidder;

/**
 * Include this class to use advanced bidding from Facebook.
 */
public class FacebookAdvancedBidder implements MoPubAdvancedBidder {
    @Override
    public String getToken(@NonNull final Context context) {
        return BidderTokenProvider.getBidderToken(context);
    }

    @Override
    public String getCreativeNetworkName() {
        return "facebook";
    }
}
