## Changelog
  * 3.0.0.0
  	* This version of the adapters has been certified with UnityAds 3.0.0.
  	* Add support for banner ad.
  	* Update GDPR consent passing logic to use MoPub's `gdprApplies()` and `canCollectPersonalInfo`.
  
  * 2.3.0.2
    * Handle no-fill scenarios from Unity Ads. 

  * 2.3.0.1
    * Update the placement ID returned in the `getAdNetworkId` API (used to generate server-side rewarded video callback URL) to be non-null, and avoid potential NullPointerExceptions.

  * 2.3.0.0
    * This version of the adapters has been certified with Unity Ads 2.3.0.

  * 2.2.1.2
    * Update to share consent with Unity Ads only when user provides an explicit yes/no. In all other cases, Unity Ads SDK will collect its own consent per the guidelines published in https://unity3d.com/legal/gdpr

  * 2.2.1.1
    * Pass explicit consent to Unity Ads.

  * 2.2.1.0
    * This version of the adapters has been certified with Unity Ads 2.2.1.
    * General Data Protection Regulation (GDPR) update to support a way for publishers to determine GDPR applicability and to obtain/manage consent from users in European Economic Area, the United Kingdom, or Switzerland to serve personalize ads. Only applicable when integrated with MoPub version 5.0.0 and above.

  * 2.2.0.0
    * This version of the adapters has been certified with Unity Ads 2.2.0.
    * Pass the placement ID to Unity's isReady() checks.

  * 2.1.1.0
    * This version of the adapters has been certified with Unity Ads 2.1.1.

  * Initial Commit
  	* Adapters moved from [mopub-android-sdk](https://github.com/mopub/mopub-android-sdk) to [mopub-android-mediation](https://github.com/mopub/mopub-android-mediation/)
