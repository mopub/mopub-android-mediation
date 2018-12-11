## Changelog
  * 11.4.0.0
    * This version of the adapters has been certified with Flurry 11.4.0.
    
  * 10.1.0.2
    * Expose the native ad's advertiser name asset for publishers to show as required by Flurry (https://developer.yahoo.com/flurry/docs/publisher/gettingstarted/nativeadguidelines/).

  * 10.1.0.1
    * Align MoPub's interstitial impression tracking to that of Flurry. 
        * `setAutomaticImpressionAndClickTracking` is set to `false`, and Flurry's `onRendered` callback is leveraged to fire MoPub impressions. This change requires MoPub 5.3.0 or higher.

  * 10.1.0.0
    * This version of the adapters has been certified with Flurry 10.1.0.

  * 9.0.0.0
    * This version of the adapters has been certified with Flurry 9.0.0.

  * 8.2.0.1
    * Fix an NPE in FlurryNativeAdRenderer's StaticNativeViewHolder.

  * 8.2.0.0
    * This version of the adapters has been certified with Flurry 8.2.0.

  * Initial Commit
  	* Adapters moved from [mopub-android-sdk](https://github.com/mopub/mopub-android-sdk) to [mopub-android-mediation](https://github.com/mopub/mopub-android-mediation/)