## Changelog
  * 8.0.1.0
    * This version of the adapters has been certified with AppLovin 8.0.1.
    * General Data Protection Regulation (GDPR) update to support a way for publishers to determine GDPR applicability and to obtain/manage consent from users in European Economic Area, the United Kingdom, or Switzerland to serve personalize ads. Only applicable when integrated with MoPub version 5.0.0 and above.
  * 7.8.6.3
    * Removed reflection from all adapters, as there are cases on MultiDexed applications, where reflection may not be able to find the target class if it's in another dex file. This resolves this issue: https://github.com/mopub/mopub-android-mediation/issues/19#issuecomment-382488061.
  * 7.8.6.2
    * Fixed edge case of rewarded videos not loading on custom zones.
  * 7.8.6.1
    * Added support for passing down AppLovin's `sdk_key` via the dashboard.
    * Do not unnecessarily preload a non-zoned rewarded video, as some publishers may only use zone-based rewarded videos.
    * Ensure ad load callbacks occur on the main queue.

  * 7.8.6.0
    * This version of the adapters has been certified with AppLovin 7.8.6.
	
  * Initial Commit
  	* Adapters moved from [mopub-android-sdk](https://github.com/mopub/mopub-android-sdk) to [mopub-android-mediation](https://github.com/mopub/mopub-android-mediation/)
