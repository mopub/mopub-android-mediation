package com.mopub.mobileads;

import com.verizon.ads.Bid;
import com.verizon.ads.Configuration;
import com.verizon.ads.support.TimedMemoryCache;

final class BidCache {

    private static final int TEN_MINUTES_MILLIS = 10 * 60 * 1000; // super auction timeout
    private static final String DOMAIN = "com.verizon.ads";
    private static final String CACHE_TIMEOUT_KEY = "super.auction.cache.timeout";

    private static final TimedMemoryCache<Bid> bidTimedMemoryCache;

    static {
        bidTimedMemoryCache = new TimedMemoryCache<>();
    }

    static void put(final String placementId, final Bid bid) {

        final long timeLimit = (long) Configuration.getInt(DOMAIN,
                CACHE_TIMEOUT_KEY,
                TEN_MINUTES_MILLIS);

        bidTimedMemoryCache.add(placementId, bid, timeLimit);
    }

    static Bid get(final String placementId) {
        return bidTimedMemoryCache.get(placementId);
    }
}
