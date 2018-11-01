## Changelog
  * 6.8.2.0
    * This version of the adapters has been certified with ONE by AOL 6.8.2.

  * 6.8.1.4
    * Align MoPub's interstitial impression tracking to that of One by AOL. 
        * `setAutomaticImpressionAndClickTracking` is set to `false`, and AOL's `onShown` callback is leveraged to fire MoPub impressions. This change requires MoPub 5.3.0 or higher.

  * 6.8.1.3
    * Update the placement ID returned in the `getAdNetworkId` API (used to generate server-side rewarded video callback URL) to be non-null, and avoid potential NullPointerExceptions.


  * 6.8.1.2
    * MoPub will not be obtaining consent on behalf of One by AOL. Publishers should work directly with One by AOL to understand their obligations to comply with GDPR. Changes are updated on the supported partners page and our GDPR FAQ.
    * Add a null check for the native rating and disclaimer assets.

  * 6.8.1.1
    * Guard against a potential NullPointerException when getting GDPR applicability.

  * 6.8.1.0
    * This version of the adapters has been certified with ONE by AOL 6.8.1.
    * General Data Protection Regulation (GDPR) update to support a way for publishers to determine GDPR applicability and to obtain/manage consent from users in European Economic Area, the United Kingdom, or Switzerland to serve personalize ads. Only applicable when integrated with MoPub version 5.0.0 and above.

  * 6.7.0.0
    * This version of the adapters has been certified with ONE by AOL 6.7.0.
    * Reverted to use MMSDK.initialize() to initialize the ONE by AOL SDK.

  * 6.6.1.0
    * This version of the adapters has been certified with ONE by AOL 6.6.1.

  * Initial Commit
  	* Adapters moved from [mopub-android-sdk](https://github.com/mopub/mopub-android-sdk) to [mopub-android-mediation](https://github.com/mopub/mopub-android-mediation/)