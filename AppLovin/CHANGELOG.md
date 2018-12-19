## Changelog
  * 9.1.3.0
    * This version of the adapters has been certified with AppLovin 9.1.3.

  * 8.1.4.3
    * Guard against nullable zone IDs. 

  * 8.1.4.2
    * Add support for AppLovin to be an Advanced Bidder on the MoPub platform.

  * 8.1.4.1
    * Roll back the change introduced in v8.1.0.2 that aligned MoPub's banner and interstitial impression tracking to that of AppLovin. The decision was taken to avoid incorrectly overcounting impressions - `adDisplayed()` callback instances are not de-duped.

  * 8.1.4.0
    * This version of the adapters has been certified with AppLovin 8.1.4.

  * 8.1.0.2
    * Align MoPub's banner and interstitial impression tracking to that of AppLovin.
        * `setAutomaticImpressionAndClickTracking` is set to `false`, and AppLovin's `adDisplayed` callback is leveraged to fire MoPub impressions. This change requires MoPub 5.3.0 or higher.

  * 8.1.0.1
    * Update the zone ID returned in the `getAdNetworkId` API (used to generate server-side rewarded video callback URL) to be non-null, and avoid potential NullPointerExceptions.

  * 8.1.0.0
    * This version of the adapters has been certified with AppLovin 8.1.0.

  * 8.0.2.0
    * This version of the adapters has been certified with AppLovin 8.0.2.

  * 8.0.1.0
    * This version of the adapters has been certified with AppLovin 8.0.1.
    * General Data Protection Regulation (GDPR) update to support a way for publishers to determine GDPR applicability and to obtain/manage consent from users in European Economic Area, the United Kingdom, or Switzerland to serve personalize ads. Only applicable when integrated with MoPub version 5.0.0 and above.
  * 7.8.6.3
    * Removed reflection from all adapters, as there are cases on MultiDexed applications, where reflection may not be able to find the target class if it's in another dex file. This resolves this issue: https://github.com/mopub/mopub-android-mediation/issues/19#issuecomment-382488061.
  * 7.8.6.2
    * Fixed edge case of rewarded videos not loading on custom zones.
  * 7.8.6.1
    * Added support for passing down AppLovin's `sdk_key` via the dashboard.
    * Do not unnecessarily preload a non-zoned rewarded video, as some publishers may only use zone-based rewarded videos.
    * Ensure ad load callbacks occur on the main queue.

  * 7.8.6.0
    * This version of the adapters has been certified with AppLovin 7.8.6.
	
  * Initial Commit
  	* Adapters moved from [mopub-android-sdk](https://github.com/mopub/mopub-android-sdk) to [mopub-android-mediation](https://github.com/mopub/mopub-android-mediation/)
