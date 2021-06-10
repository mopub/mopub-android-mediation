## Changelog
 * 6.5.0.0
  * This version of the adapters has been certified with Facebook Audience Network 6.5.0 and MoPub 5.17.0.

 * 6.4.0.1
  * Remove mopub-sdk as a required dependency from POM file.

 * 6.4.0.0
   * This version of the adapters has been certified with Facebook Audience Network 6.4.0 and MoPub 5.16.4.

 * 6.3.0.0
   * This version of the adapters has been certified with Facebook Audience Network 6.3.0 and MoPub 5.16.0.

 * 6.2.1.0
   * This version of the adapters has been certified with Facebook Audience Network 6.2.1 and MoPub 5.15.0.

 * 6.2.0.2
   * Fix a bug where the rewarded video adapter fails to request a new ad after a show-related error happens.

 * 6.2.0.1
   * Remove the call to set Facebook's test mode from the adapters.

 * 6.2.0.0
   * **Facebook's test mode was accidentally enabled in this version. Please use v6.2.0.1 instead.**
   * This version of the adapters has been certified with Facebook Audience Network 6.2.0 and MoPub 5.14.0.

 * 6.1.0.0
   * This version of the adapters has been certified with Facebook Audience Network 6.1.0 and MoPub 5.13.1.

 * 6.0.0.0
   * This version of the adapters has been certified with Facebook Audience Network 6.0.0 and MoPub 5.13.1.

 * 5.11.0.1
   * Fix timeout issues due to null `mInteractionListener`.

 * 5.11.0.0
   * This version of the adapters has been certified with Facebook Audience Network 5.11.0 and MoPub 5.13.1.

 * 5.10.1.0
   * This version of the adapters has been certified with Facebook Audience Network 5.10.1 and MoPub 5.13.1.
   * Fixed `java.lang.NoClassDefFoundError: com.facebook.ads.redexgen.X.8O` crashes on some Android 4.x and 5.x builds. 

 * 5.10.0.1
   * Fix custom expiration logic for interstitial and rewarded video. Note: For expired interstitials, publishers need to call `destroy()` before requesting a new ad.

 * 5.10.0.0
   * This version of the adapters has been certified with Facebook Audience Network 5.10.0 and MoPub 5.13.1.

 * 5.9.1.1
   * Fix interstitial load listener usage.

 * 5.9.1.0
   * This version of the adapters has been certified with Facebook Audience Network 5.9.1 and MoPub 5.13.0.
   * Fix rewarded video click not tracked for certain creative templates.

 * 5.9.0.2
   * Refactor non-native adapter classes to use the new consolidated API from MoPub.
   * This and newer adapter versions are only compatible with 5.13.0+ MoPub SDK.
   
 * 5.9.0.1
   * Add support for Facebook native banner rendering via [templates](https://developers.facebook.com/docs/audience-network/guides/ad-formats/native/android-template/#native-banner-ad).

 * 5.9.0.0
   * This version of the adapters has been certified with Facebook Audience Network 5.9.0 and MoPub 5.12.0.

 * 5.8.0.0
   * This version of the adapters has been certified with Facebook Audience Network 5.8.0 and MoPub 5.11.1.

 * 5.7.1.1
   * Fix duplicate firing of `onRewardedVideoClosed()`. 

 * 5.7.1.0
   * This version of the adapters has been certified with Facebook Audience Network 5.7.1.

 * 5.7.0.0
   * This version of the adapters has been certified with Facebook Audience Network 5.7.0.

 * 5.6.1.0
   * This version of the adapters has been certified with Facebook Audience Network 5.6.1.
 
 * 5.6.0.1
    * Log the Facebook placement name in ad lifecycle events.
    * Map additional error codes for failure cases.
    * Fail fast when certain parameters are null. 

 * 5.6.0.0
    * This version of the adapters has been certified with Facebook Audience Network 5.6.0.
    * Add support for [native banner](https://developers.facebook.com/docs/audience-network/android-native-banner/). Refer to the [Mediate Facebook page](https://developers.mopub.com/publishers/mediation/networks/facebook/) for integration instructions.
    * Refactor ad request logic to use Facebook Audience Network's `LoadConfigBuilder`. 

 * 5.5.0.8
    * Replace `AdIconView` with `MediaView` for the ad icon view as it has been deprecated by Facebook. 

 * 5.5.0.7
    * Fix error codes mapping for the banner and interstitial adapters. 

 * 5.5.0.6
    * Fix banner size checks so 250 doesn't always get treated as 90.

 * 5.5.0.5
    * Remove native video handling code and associated comments. Publishers can enable/disable video on the Facebook Audience Network dashboard.

 * 5.5.0.4
    * Add support for AndroidX. This is the minimum version compatible with MoPub 5.9.0.

 * 5.5.0.3
    * Throw a playback error (instead of load failure) when a rewarded video has expired.

 * 5.5.0.2
    * Check if the Facebook ad (interstitial / rewarded video) has been invalidated before showing.

 * 5.5.0.1
    * Support additional interstitial and rewarded video callbacks from the `InterstitialAdExtendedListener` interface.

 * 5.5.0.0
    * This version of the adapters has been certified with Facebook Audience Network 5.5.0.
    * Bidder token generation doesn't depend on Facebook SDK initialization.

 * 5.4.1.1
    * Fix banner size passing as part of 5.8.0+ MoPub SDK release changes.

 * 5.4.1.0
    * This version of the adapters has been certified with Facebook Audience Network 5.4.1.

 * 5.4.0.0
    * This version of the adapters has been certified with Facebook Audience Network 5.4.0.

 * 5.3.1.0
    * This version of the adapters has been certified with Facebook Audience Network 5.3.1.

 * 5.3.0.0
    * This version of the adapters has been certified with Facebook Audience Network 5.3.0.

 * 5.2.1.0
    * This version of the adapters has been certified with Facebook Audience Network 5.2.1.

 * 5.2.0.1
    * Facebook Audience Network Adapter will now be released as an Android Archive (AAR) file that includes manifest file for [FAN manifest changes](https://developers.facebook.com/docs/audience-network/android-interstitial/).

  * 5.2.0.0
    * This version of the adapters has been certified with Facebook Audience Network 5.2.0. 
    * Add `FacebookTemplateRenderer.java` to render native ads using [predefined layouts from Facebook Audience Network](https://developers.facebook.com/docs/audience-network/android/nativeadtemplate). You won't need to bind to your XML layouts/views; instead of creating a new `FacebookAdRenderer`, simply create a new `FacebookTemplateRenderer` and pass in a new `NativeAdViewAttributes()`.
    * Replace `AdChoiceView` with `AdOptionsView`.

  * 5.1.0.2
    * Fix an ANR when getting the bidding token by calling Facebook's `BidderTokenProvider.getBidderToken()` from a background thread.

  * 5.1.0.1
    * **Note**: This version is only compatible with the 5.5.0+ release of the MoPub SDK.
    * Add the `FacebookAdapterConfiguration` class to: 
         * pre-initialize the Facebook Audience Network SDK during MoPub SDK initialization process
         * store adapter and SDK versions for logging purpose
         * return the Advanced Biding token previously returned by `FacebookAdvancedBidder.java`
    * Streamline adapter logs via `MoPubLog` to make debugging more efficient. For more details, check the [Android Initialization guide](https://developers.mopub.com/docs/android/initialization/) and [Writing Custom Events guide](https://developers.mopub.com/docs/android/custom-events/).

  * 5.1.0.0
    * This version of the adapters has been certified with Facebook Audience Network 5.1.0
    * For all ad formats, add support to initialize Facebook Audience Network SDK at the time of the first ad request to Facebook Audience Network.
 
  * 5.0.1.0
    * This version of the adapters has been certified with Facebook Audience Network 5.0.1.

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
