## Changelog
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
