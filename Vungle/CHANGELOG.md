## Changelog
  * 6.3.24.1
    * **Note**: This version is only compatible with the 5.5.0+ release of the MoPub SDK.
    * Add the `VungleAdapterConfiguration` class to: 
         * pre-initialize the Vungle SDK during MoPub SDK initialization process
         * store adapter and SDK versions for logging purpose
    * Streamline adapter logs via `MoPubLog` to make debugging more efficient. For more details, check the [Android Initialization guide](https://developers.mopub.com/docs/android/initialization/) and [Writing Custom Events guide](https://developers.mopub.com/docs/android/custom-events/).

  * 6.3.24.0
    * This version of the adapters has been certified with Vungle 6.3.24.
    * Check if the passed Placement ID is a valid placement for the given App ID.

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