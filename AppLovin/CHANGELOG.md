## Changelog
 * 9.10.4.1
    * Log the AppLovin zone id in ad lifecycle events.
    * Map additional error codes for failure cases.

 * 9.10.4.0
    * This version of the adapters has been certified with AppLovin 9.10.4.

 * 9.10.3.0
    * This version of the adapters has been certified with AppLovin 9.10.3.

 * 9.10.2.0
    * This version of the adapters has been certified with AppLovin 9.10.2.

 * 9.10.1.0
    * This version of the adapters has been certified with AppLovin 9.10.1.

 * 9.9.2.1
    * Fail fast when the AppLovin SDK Key is not inputted.

 * 9.9.2.0
    * This version of the adapters has been certified with AppLovin 9.9.2.

 * 9.9.1.0
    * This version of the adapters has been certified with AppLovin 9.9.1.

 * 9.8.4.1
    * Add support for AndroidX. This is the minimum version compatible with MoPub 5.9.0.

 * 9.8.4.0
    * This version of the adapters has been certified with AppLovin 9.8.4.

 * 9.8.0.0
    * This version of the adapters has been certified with AppLovin 9.8.0.

 * 9.7.2.2
    * Fix banner size passing as part of ad format unification.

 * 9.7.2.1
    * Persist the AppLovin SDK Key from cached AdapterConfiguration so other AppLovin adapter classes can read it. 

 * 9.7.2.0
    * This version of the adapters has been certified with AppLovin 9.7.2.

 * 9.7.1.0
    * This version of the adapters has been certified with AppLovin 9.7.1.

 * 9.6.2.0
    * This version of the adapters has been certified with AppLovin 9.6.2.

 * 9.6.0.0
    * This version of the adapters has been certified with AppLovin 9.6.0.

 * 9.5.4.0
    * This version of the adapters has been certified with AppLovin 9.5.4.

 * 9.4.2.0
    * This version of the adapters has been certified with AppLovin 9.4.2.

 * 9.4.0.0
    * This version of the adapters has been certified with AppLovin 9.4.0.
    * Pass MoPub's log level to AppLovin. To adjust AppLovin's log level via MoPub's log settings, reference [this page](https://developers.mopub.com/publishers/android/test/#enable-logging).
    * Drop AppLovin's `<activity>` tags from the AndroidManifest since the AppLovin SDK AAR already bundles them.

  * 9.2.1.1
     * AppLovin Adapter will now be released as an Android Archive (AAR) file that includes manifest file for [AppLovin manifest changes](https://dash.applovin.com/login#androidIntegration).

  * 9.2.1.0
     * This version of the adapters has been certified with AppLovin 9.2.1.
     * Addressed some issues and optimized adapter:
         * Allow passing in of SDK key via the initialization `configuration` object as many publishers integrate without SDK key in the Android Manifest.
         * Add support for using a cached `configuration` Map for initialization.
         * Do not consider various errors as NO FILLs.
         * Bumped AppLovin SDK plugin version to "MoPub-9.2.1.0".

  * 9.1.3.1
    * **Note**: This version is only compatible with the 5.5.0+ release of the MoPub SDK.
    * Add the `AppLovinAdapterConfiguration` class to: 
         * pre-initialize the AppLovin SDK during MoPub SDK initialization process
         * store adapter and SDK versions for logging purpose
         * return the Advanced Biding token previously returned by `AppLovinAdvancedBidder.java`
    * Streamline adapter logs via `MoPubLog` to make debugging more efficient. For more details, check the [Android Initialization guide](https://developers.mopub.com/docs/android/initialization/) and [Writing Custom Events guide](https://developers.mopub.com/docs/android/custom-events/).

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
