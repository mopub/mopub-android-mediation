## Changelog

  * 5.1.0.0-beta
    * Initialize the Facebook Audience Network SDK for all ad formats.

  * 5.0.0.0
    * This version of the adapters has been certified with Facebook Audience Network 5.0.0.
    * Remove calls to `disableAutoRefresh()` for banner (deprecated by Facebook).
    * Remove calls to `getAdView()` and `getInterstitialAd()` for banner and interstitial, respectively (deprecated and used for testing).
    * Fire MoPub's `onRewardedVideoPlaybackError()` instead of `onRewardedVideoLoadFailure()` when there is no rewarded video to play (parity with other rewarded video adapters).
    * Enable publishers to use the advertiser name asset as it is a required asset starting in Facebook 4.99.0 (https://developers.facebook.com/docs/audience-network/guidelines/native-ads#name).

  * 4.99.1.3
    * Fix a crash caused by the FB AdChoices icon getting positioned using `ALIGN_PARENT_END`. Older Android APIs will use `ALIGN_PARENT_RIGHT`.

  * 4.99.1.2
    * Align MoPub's banner and interstitial impression tracking to that of Facebook Audience Network.
        * `setAutomaticImpressionAndClickTracking` is set to `false`, and Facebook's `onLoggingImpression` callback is leveraged to fire MoPub impressions. This change requires MoPub 5.3.0 or higher.

  * 4.99.1.1
    * Update the placement ID returned in the `getAdNetworkId` API (used to generate server-side rewarded video callback URL) to be non-null, and avoid potential NullPointerExceptions.

  * 4.99.1.0
    * This version of the adapters has been certified with Facebook Audience Network 4.99.1 for all ad formats. Publishers must use the latest native ad adapters for compatibility.

  * 4.99.0.0
    * This version of the adapters has been certified with Facebook Audience Network 4.99.0 for all ad formats except native ads.
    * This version of the Audience Network SDK deprecates several existing native ad APIs used in the existing adapters. As a result, the current native ad adapters are not compatible. Updates require changes from the MoPub SDK as well, so we are planning to release new native ad adapters along with our next SDK release. Publishers integrated with Facebook native ads are recommended to use the pre-4.99.0 SDKs until the updates are available.

  * 4.28.1.1
    * Enables advanced bidding for all adapters and adds FacebookAdvancedBidder.

  * 4.28.1.0
    * This version of the adapters has been certified with Facebook Audience Network 4.28.1.

  * 4.28.0.0
    * This version of the adapters has been certified with Facebook Audience Network 4.28.0.
	* Removed star rating from the native ad adapter as it has been deprecated by Facebook.

  * 4.27.0.0
    * This version of the adapters has been certified with Facebook Audience Network 4.27.0.

  * Initial Commit
    * Adapters moved from [mopub-android-sdk](https://github.com/mopub/mopub-android-sdk) to [mopub-android-mediation](https://github.com/mopub/mopub-android-mediation/)
