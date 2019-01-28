## Changelog
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