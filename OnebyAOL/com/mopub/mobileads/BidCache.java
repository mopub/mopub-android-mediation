package com.mopub.mobileads;

import com.verizon.ads.Bid;
import com.verizon.ads.Configuration;
import com.verizon.ads.support.TimedMemoryCache;


final class BidCache {

	private static final String DOMAIN = "com.verizon.ads";
	private static final int SUPER_AUCTION_CACHE_TIMEOUT_DEFAULT = 600000;
	private static final String SUPER_AUCTION_CACHE_TIMEOUT_KEY = "super.auction.cache.timeout";

	private static TimedMemoryCache<Bid> bidTimedMemoryCache;


	static {
		bidTimedMemoryCache = new TimedMemoryCache<>();
	}


	static void put(final String placementId, final Bid bid) {

		bidTimedMemoryCache.add(placementId, bid,
			(long) Configuration.getInt(DOMAIN, SUPER_AUCTION_CACHE_TIMEOUT_KEY, SUPER_AUCTION_CACHE_TIMEOUT_DEFAULT));
	}


	static Bid get(final String placementId) {

		return bidTimedMemoryCache.get(placementId);
	}
}
