## Changelog
  * 12.8.1.0
    * This version of the adapters has been certified with Tapjoy 12.8.1 and MoPub SDK 5.17.0.

  * 12.8.0.0
    * This version of the adapters has been certified with Tapjoy 12.8.0 and MoPub SDK 5.16.3.

  * 12.7.1.1
    * Fix a bug where the rewarded video adapter fails to request a new ad after a show-related error happens.

  * 12.7.1.0
    * This version of the adapters has been certified with Tapjoy 12.7.1 and MoPub SDK 5.14.0.

  * 12.7.0.0
    * This version of the adapters has been certified with Tapjoy 12.7.0 and MoPub SDK 5.13.1.
    
  * 12.6.1.0
    * This version of the adapters has been certified with Tapjoy 12.6.1 and MoPub SDK 5.13.1.

  * 12.6.0.0
    * This version of the adapters has been certified with Tapjoy 12.6.0 and MoPub SDK 5.13.0.
    * Update to new GDPR and consent APIs.

  * 12.4.2.1
    * Refactor non-native adapter classes to use the new consolidated API from MoPub.
    * This and newer adapter versions are only compatible with 5.13.0+ MoPub SDK.

  * 12.4.2.0
    * This version of the adapters has been certified with Tapjoy 12.4.2.

  * 12.4.1.0
    * This version of the adapters has been certified with Tapjoy 12.4.1.

  * 12.4.0.1
    * Log the Tapjoy placement ID in ad lifecycle events.

  * 12.4.0.0
    * This version of the adapters has been certified with Tapjoy 12.4.0.

  * 12.3.4.0
    * This version of the adapters has been certified with Tapjoy 12.3.4.

  * 12.3.3.1
    * Prioritize reading data from `localExtras` and fall back to `MediationSettings` if necessary for `TapjoyRewardedVideo.java`.
    * Removed `sdkKey` setter from code. It must be entered on the Mopub dashboard.
    
  * 12.3.3.0
    * This version of adapters has been certified with Tapjoy 12.3.3.

  * 12.3.1.2
    * Add support for AndroidX. This is the minimum version compatible with MoPub 5.9.0.

  * 12.3.1.1
    * Fix `MediationSettings` for Tapjoy to follow POJO to maintain compatibility with MoPub Unity SDK.

  * 12.3.1.0
    * This version of adapters has been certified with Tapjoy 12.3.1.
    * Add `didClick` callback support for interstitial and rewarded video ad clicks.
    * Fix `getBiddingToken` API to return token string when available and return `1` otherwise.

  * 12.2.1.1
    * Refactor the `MediationSettings` implementation to maintain consistency with the MoPub Unity SDK.

  * 12.2.1.0
    * This version of adapters has been certified with Tapjoy 12.2.1.
    * Check if the Tapjoy placement instance is `null` before accessing it in the `TapjoyRewardedVideo` and `TapjoyInterstitial` adapters.
    * Pass MoPub's log level to Tapjoy. To adjust Tapjoy's log level via MoPub's log settings, reference [this page](https://developers.mopub.com/publishers/android/test/#enable-logging).

  * 12.2.0.3
    * Tapjoy Adapter will now be released as an Android Archive (AAR) file that includes manifest file for [Tapjoy manifest changes](https://dev.tapjoy.com/sdk-integration/android/getting-started-guide-publishers-android/#toc_add-app-permissions-and-activities).

  * 12.2.0.2
    * Prevent a null SDK Key from being passed to Tapjoy's initialization call.

  * 12.2.0.1
    * **Note**: This version is only compatible with the 5.5.0+ release of the MoPub SDK.
    * Add the `TapjoyAdapterConfiguration` class to: 
         * pre-initialize the Tapjoy SDK during MoPub SDK initialization process
         * store adapter and SDK versions for logging purpose
         * return the Advanced Biding token previously returned by `TapjoyAdvancedBidder.java`
    * Streamline adapter logs via `MoPubLog` to make debugging more efficient. For more details, check the [Android Initialization guide](https://developers.mopub.com/docs/android/initialization/) and [Writing Custom Events guide](https://developers.mopub.com/docs/android/custom-events/).

  * 12.2.0.0
    * This version of the adapters has been certified with Tapjoy 12.2.0.
    
  * 12.1.0.1
    * Pass the signal from `MoPub.canCollectPersonalInformation()` as a consent status to Tapjoy for consistency with the other mediated adapters.

  * 12.1.0.0
    * This version of the adapters has been certified with Tapjoy 12.1.0.

  * 12.0.0.1
    * Update the ad network constant returned in the `getAdNetworkId` API (used to generate server-side rewarded video callback URL) to be non-null, and avoid potential NullPointerExceptions.

  * 12.0.0.0
    * This version of the adapters has been certified with Tapjoy 12.0.0.
    * Add `TapjoyAdvancedBidder.java` for publishers using Advanced Bidding.

  * 11.12.2.0
    * This version of the adapters has been certified with Tapjoy 11.12.2.
    * General Data Protection Regulation (GDPR) update to support a way for publishers to determine GDPR applicability and to obtain/manage consent from users in European Economic Area, the United Kingdom, or Switzerland to serve personalize ads. Only applicable when integrated with MoPub version 5.0.0 and above.

  * 11.12.0.0
    * This version of the adapters has been certified with Tapjoy 11.12.0.

  * 11.11.0.0
    * This version of the adapters has been certified with Tapjoy 11.11.0.

  * Initial Commit
  	* Adapters moved from [mopub-android-sdk](https://github.com/mopub/mopub-android-sdk) to [mopub-android-mediation](https://github.com/mopub/mopub-android-mediation/)
