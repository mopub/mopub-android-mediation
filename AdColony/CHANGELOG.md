## Changelog
  * 4.1.4.0
    * This version of the adapters has been certified with AdColony 4.1.4.
    * Fix a NullPointerException crash while requesting ads from the `onExpiring` callback.

  * 4.1.3.0
    * This version of the adapters has been certified with AdColony 4.1.3.
    * Log the AdColony zone id in ad lifecycle events.

  * 4.1.1.0
    * This version of the adapters has been certified with AdColony 4.1.1.

  * 4.1.0.2
    * Fail initialization only if `allZoneIds` parameter is missing in `AdColonyAdapterConfiguration`.

  * 4.1.0.1
    * Add support for banners which was introduced on AdColony 4.1.0.
    * Move AdColony parameters to the `AdColonyAdapterConfiguration`.

  * 4.1.0.0
    * This version of the adapters has been certified with AdColony 4.1.0.

  * 3.3.11.2
    * Prioritize reading data from `localExtras` and fall back to `MediationSettings` if necessary for AdColonyRewardedVideo.java.

  * 3.3.11.1
    * Add support for AndroidX. This is the minimum version compatible with MoPub 5.9.0.

  * 3.3.11.0
    * This version of the adapters has been certified with AdColony 3.3.11.

  * 3.3.10.0
    * This version of the adapters has been certified with AdColony 3.3.10.

  * 3.3.9.0
    * This version of the adapters has been certified with AdColony 3.3.9.
    * Refactor the `MediationSettings` implementation to maintain consistency with the MoPub Unity SDK.
    * Bail out of the adapter if the AdColony app ID is empty.

  * 3.3.8.1
    * AdColony Adapter will now be released as an Android Archive (AAR) file that includes manifest file for [AdColony manifest changes](https://github.com/AdColony/AdColony-Android-SDK-3/wiki/Project-Setup#step-2-edit-manifest).

  * 3.3.8.0
    * This version of the adapters has been certified with AdColony 3.3.8.

  * 3.3.7.1
    * **Note**: This version is only compatible with the 5.5.0+ release of the MoPub SDK.
    * Add the `AdColonyAdapterConfiguration` class to: 
         * pre-initialize the AdColony SDK during MoPub SDK initialization process
         * store adapter and SDK versions for logging purpose
         * return the Advanced Biding token previously returned by `AdColonyAdvancedBidder.java`
    * Streamline adapter logs via `MoPubLog` to make debugging more efficient. For more details, check the [Android Initialization guide](https://developers.mopub.com/docs/android/initialization/) and [Writing Custom Events guide](https://developers.mopub.com/docs/android/custom-events/).
    * Allow supported mediated networks and publishers to opt-in to process a user’s personal data based on legitimate interest basis. More details [here](https://developers.mopub.com/docs/publisher/gdpr-guide/#legitimate-interest-support).

  * 3.3.7.0
    * This version of the adapters has been certified with AdColony 3.3.7.

  * 3.3.5.1
    * Update the zone ID returned in the `getAdNetworkId` API (used to generate server-side rewarded video callback URL) to be non-null, and avoid potential NullPointerExceptions.

  * 3.3.5.0
    * This version of the adapters has been certified with AdColony 3.3.5.

  * 3.3.4.0
    * This version of the adapters has been certified with AdColony 3.3.4.
    * General Data Protection Regulation (GDPR) update to support a way for publishers to determine GDPR applicability and to obtain/manage consent from users in European Economic Area, the United Kingdom, or Switzerland to serve personalize ads. Only applicable when integrated with MoPub version 5.0.0 and above.
    * Add AdColonyAdvancedBidder.java for publishers using Advaced Bidding

  * 3.3.2.0
    * This version of the adapters has been certified with AdColony 3.3.2.

  * 3.3.0.0
    * This version of the adapters has been certified with AdColony 3.3.0.
	
  * Initial Commit
  	* Adapters moved from [mopub-android-sdk](https://github.com/mopub/mopub-android-sdk) to [mopub-android-mediation](https://github.com/mopub/mopub-android-mediation/)