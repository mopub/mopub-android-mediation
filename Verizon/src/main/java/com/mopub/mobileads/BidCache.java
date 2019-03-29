package com.mopub.mobileads;

import com.verizon.ads.Bid;
import com.verizon.ads.Configuration;
import com.verizon.ads.support.TimedMemoryCache;

final class BidCache {

    private static final int SUPER_AUCTION_CACHE_TIMEOUT_DEFAULT_MS = 10 * 60 * 1000; // 10 minutes;
    private static final String DOMAIN = "com.verizon.ads";
    private static final String SUPER_AUCTION_CACHE_TIMEOUT_KEY = "super.auction.cache.timeout";

    private static final TimedMemoryCache<Bid> bidTimedMemoryCache;

    static {
        bidTimedMemoryCache = new TimedMemoryCache<>();
    }

    static void put(final String placementId, final Bid bid) {

        final long timeLimit = (long) Configuration.getInt(DOMAIN,
                SUPER_AUCTION_CACHE_TIMEOUT_KEY,
                SUPER_AUCTION_CACHE_TIMEOUT_DEFAULT_MS);

        bidTimedMemoryCache.add(placementId, bid, timeLimit);
    }

    static Bid get(final String placementId) {
        return bidTimedMemoryCache.get(placementId);
    }
}
