## Changelog
  * 6.3.17.1
    * Update the placement ID returned in the `getAdNetworkId` API (used to generate server-side rewarded video callback URL) to be non-null, and avoid potential NullPointerExceptions.


  * 6.3.17.0
    * This version of the adapters has been certified with Vungle 6.13.17.
    * Remove the placement IDS (pids) parameter from Vungle's init call per Vungle's new SDK requirements.
    * Update the implementation of Vungle's `updateConsentStatus()` to now also pass a `consentMessageVersion`.

  * 6.2.5.1
    * Fixed an NPE when passing consent before the Vungle SDK is initialized.

  * 6.2.5.0
    * This version of the adapters has been certified with Vungle 6.2.5.
    * General Data Protection Regulation (GDPR) update to support a way for publishers to determine GDPR applicability and to obtain/manage consent from users in European Economic Area, the United Kingdom, or Switzerland to serve personalize ads. Only applicable when integrated with MoPub version 5.0.0 and above.

  * 5.3.2.0
    * This version of the adapters has been certified with Vungle 5.3.2.

  * Initial Commit
  	* Adapters moved from [mopub-android-sdk](https://github.com/mopub/mopub-android-sdk) to [mopub-android-mediation](https://github.com/mopub/mopub-android-mediation/)