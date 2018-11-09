## Changelog
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