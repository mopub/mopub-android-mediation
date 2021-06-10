## Changelog
  * 8.2.1.0
    * This version of the adapters has been certified with Chartboost 8.2.1 and MoPub 5.17.0.

  * 8.2.0.3
    * Fix a bug where the rewarded video adapter fails to request a new ad after a show-related error happens.

  * 8.2.0.2
    * Prevent Chartboost SDK from initializing same configuration cases.

  * 8.2.0.1
    * Fix a bug where Chartboost SDK initialization can get called multiple times unnecessarily.

  * 8.2.0.0
    * This version of the adapters has been certified with Chartboost 8.2.0 and MoPub 5.15.0.
    * Cache last available Interstitial and Rewarded Video data fetched from the MoPub dashboard.

  * 8.1.0.2
    * Add missing `InteractionListener` methods to fullscreen interaction listener.

  * 8.1.0.1
    * Fail banner load requests for Android API below 21. Chartboost 8.0.1 and above currently doesn't support displaying of banners for devices with Android API below 21.

  * 8.1.0.0
    * This version of the adapters has been certified with Chartboost 8.1.0 and MoPub 5.13.1.
    * Fix a bug where load and interaction listeners for interstitials were not unregistered properly
    * Use `addDataUseConsent` to pass GDPR consent
    * Implement Chartboost's `cache` and `show` APIs for inline ads.

  * 8.0.3.0
    * This version of the adapters has been certified with Chartboost 8.0.3 and MoPub 5.13.0.
    * Fix a bug where rewarded videos fail to show due to the activity presenting it is set to null.
    * Refactor non-native adapter classes to use the new consolidated API from MoPub.
    * This and newer adapter versions are only compatible with 5.13.0+ MoPub SDK.

  * 8.0.2.1
    * Set Chartboost delegation on each load request for interstitials and rewarded videos.

  * 8.0.2.0
    * Add support for banner format.
    * This version of the adapters has been certified with Chartboost 8.0.2 and MoPub 5.11.1.

  * 8.0.1.0
    * This version of the adapters has been certified with Chartboost 8.0.1 and MoPub 5.11.1.
    * Remove `Activity` requirement for Chartboost initialization, and remove `<activity>` declarations from the adapter AndroidManifest.

  * 7.5.0.3
    * Log the Chartboost location String in ad lifecycle events.
    * Map additional error codes for failure cases.

  * 7.5.0.2
    * Prioritize reading data from `localExtras` and fall back to `MediationSettings` if necessary for ChartboostRewardedVideo.java.

  * 7.5.0.1
    * Add support for AndroidX. This is the minimum version compatible with MoPub 5.9.0.

  * 7.5.0.0
    * This version of the adapters has been certified with Chartboost 7.5.0.

  * 7.3.1.4
    * Refactor the `MediationSettings` implementation to maintain consistency with the MoPub Unity SDK.

  * 7.3.1.3
    * Pass MoPub's log level to Chartboost. To adjust Chartboost's log level via MoPub's log settings, reference [this page](https://developers.mopub.com/publishers/android/test/#enable-logging).

  * 7.3.1.2
    * Chartboost Adapter will now be released as an Android Archive (AAR) file that includes manifest file for [Chartboost manifest changes](https://answers.chartboost.com/en-us/child_article/android#androidmanifest).

  * 7.3.1.1
    * **Note**: This version is only compatible with the 5.5.0+ release of the MoPub SDK.
    * Add the `ChartboostAdapterConfiguration` class to: 
         * pre-initialize the Chartboost SDK during MoPub SDK initialization process
         * store adapter and SDK versions for logging purpose
    * Streamline adapter logs via `MoPubLog` to make debugging more efficient. For more details, check the [Android Initialization guide](https://developers.mopub.com/docs/android/initialization/) and [Writing Custom Events guide](https://developers.mopub.com/docs/android/custom-events/).
    * Allow supported mediated networks and publishers to opt-in to process a user’s personal data based on legitimate interest basis. More details [here](https://developers.mopub.com/docs/publisher/gdpr-guide/#legitimate-interest-support).

  * 7.3.1.0
    * This version of the adapters has been certified with Chartboost 7.3.1.

  * 7.3.0.0
    * Use Chartboost's `setPIDataUseConsent()` instead of `restrictDataCollection()` to pass GDPR consent data per Chartboost's 7.3.0 release.

  * 7.2.1.1
    * Add `onInterstitialImpression` that is introduced in the 5.3.0 MoPub release to ChartboostShared.java.

  * 7.2.1.0
    * This version of the adapters has been certified with Chartboost 7.2.1.

  * 7.2.0.0
    * This version of the adapters has been certified with Chartboost 7.2.0.
    * General Data Protection Regulation (GDPR) update to support a way for publishers to determine GDPR applicability and to obtain/manage consent from users in European Economic Area, the United Kingdom, or Switzerland to serve personalize ads. Only applicable when integrated with MoPub version 5.0.0 and above.
    
  * 7.1.0.0
    * This version of the adapters has been certified with Chartboost 7.1.0.

  * 7.0.1.0
    * This version of the adapters has been certified with Chartboost 7.0.1.

  * Initial Commit
  	* Adapters moved from [mopub-android-sdk](https://github.com/mopub/mopub-android-sdk) to [mopub-android-mediation](https://github.com/mopub/mopub-android-mediation/)
