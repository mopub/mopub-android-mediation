## Changelog
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