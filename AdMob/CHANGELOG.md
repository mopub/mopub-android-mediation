## Changelog
  * 15.0.0.11
    * Remove manual impression tracking for banner, since AdMob does not have an equivalent callback for impressions (AdListener.onAdImpression() is only applicable for Google's native ads).

  * 15.0.0.10
    * Align MoPub's banner and interstitial impression tracking to that of AdMob.
        * `setAutomaticImpressionAndClickTracking` is set to `false`, and AdMob's `onAdImpression` and `onAdOpened` callbacks are leveraged to fire MoPub impressions. This change requires MoPub 5.3.0 or higher.

  * 15.0.0.9
    * Update the ad unit ID returned in the `getAdNetworkId` API (used to generate server-side rewarded video callback URL) to be non-null, and avoid potential NullPointerExceptions.

  * 15.0.0.8
    * Really fix the AdMob `isLoaded()` crash (15.0.0.3).

  * 15.0.0.7
    * Improve 15.0.0.5 to no longer call out to MoPub's rewarded video APIs to store and process the `npa` value.

  * 15.0.0.6
    * Guard against potential NPEs in 15.0.0.5.

  * 15.0.0.5
    * Append user's ad personalization preference via MoPub's GlobalMediationSettings to AdMob's ad requests. Publishers should work with Google to be GDPR-compliant and Google’s personalization preference does not MoPub’s consent.

  * 15.0.0.4
    * Append user's ad personalization preference from `localExtras` to AdMob's ad requests. [Deprecated]

  * 15.0.0.3
    * Forced AdMob's rewarded video's `isLoaded()` check to run on the main thread (in light of multithreading crashes when mediating AdMob on Unity).

  * 15.0.0.2
    * Resolved the previous Known Issue (AdMob's native ads are occasionally removed from the view hierarchy when a ListView/RecyclerView is scrolled).

  * 15.0.0.1
    * Removed an extra class from the JCenter jar. The adapter binaries on this GitHub repository are not affected.

  * 15.0.0.0
    * This version of the adapters has been certified with AdMob 15.0.0.
	* Implement AdMob's onRewardedVideoCompleted() callback. 
    * [Known Issue] AdMob's native ads are occasionally removed from the view hierarchy when a ListView/RecyclerView is scrolled.

  * 11.8.0.0
    * This version of the adapters has been certified with AdMob 11.8.0.
	
  * Initial Commit
  	* Adapters moved from [mopub-android-sdk](https://github.com/mopub/mopub-android-sdk) to [mopub-android-mediation](https://github.com/mopub/mopub-android-mediation/)
