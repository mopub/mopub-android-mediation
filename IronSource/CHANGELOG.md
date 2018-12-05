## Changelog
  * 6.8.0.1.0
    * This version of the adapters has been certified with ironSource 6.8.0.1.
    
  * 6.7.12.0
    * This version of the adapters has been certified with ironSource 6.7.12.

  * 6.7.11.0
    * This version of the adapters has been certified with ironSource 6.7.11.
    * Fail the adapter and exit if the ironSource application key is returned empty.

  * 6.7.10.2
    * Update the instance ID returned in the `getAdNetworkId` API (used to generate server-side rewarded video callback URL) to be non-null, and avoid potential NullPointerExceptions.


  * 6.7.10.1
    * This version of the adapters has been certified with IronSource 6.7.10

  * 6.7.9.1.0
    * This version of the adapters has been certified with IronSource 6.7.9.1

  * 6.7.9.1
    * Add Activity lifecycle listeners
    * Call MoPub's `onInterstitialFailed()` when an ironSource interstitial fails to show

  * 6.7.9.0
    * This version of the adapters has been certified with IronSource 6.7.9.
    * General Data Protection Regulation (GDPR) update to support a way for publishers to determine GDPR applicability and to obtain/manage consent from users in European Economic Area, the United Kingdom, or Switzerland to serve personalize ads. Only applicable when integrated with MoPub version 5.0.0 and above.

  * 6.7.8.0
    * This version of the adapters has been certified with IronSource 6.7.8.

  * 6.7.7.0
    * This version of the adapters has been certified with IronSource 6.7.7.
	
  * Initial Commit
  	* Adapters moved from [mopub-android-sdk](https://github.com/mopub/mopub-android-sdk) to [mopub-android-mediation](https://github.com/mopub/mopub-android-mediation/)
