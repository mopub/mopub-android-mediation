## Changelog
  * 13.0.31.0
    * This version of the adapters has been certified with Mintegral 13.0.31 and MoPub 5.11.1.
    * Fix `EXCEPTION_SERVICE_REQUEST_OS_VERSION_REQUIRED` and `NumberFormatException` at initialization.

  * 13.0.11.1
    * Add dedicated impression and click callback for interstitial.

  * 13.0.11.0
    * This version of the adapters has been certified with Mintegral 13.0.11 and MoPub 5.11.1.
    * Disable automatic impression and click tracking for banner.

  * 12.2.31.0
    * This version of the adapters has been certified with Mintegral 12.2.31.
    * Add a mute API for interstitial and rewarded video. Unless muted via `MintegralAdapterConfiguration.setMute(true)`, creatives play unmuted by default. 

  * 12.2.11.0
    * This version of the adapters has been certified with Mintegral 12.2.11.
    * Implement `onAdCloseWithIVReward()` for interstitial.

  * 12.1.51.1
    * Use `this.getClass()` instead of hard-coding class names to allow for extension.

  * 12.1.51.0
    * This version of the adapters has been certified with Mintegral 12.1.51.
    * Add `onCloseBanner()` in `MintegralBanner`.
    * Stop notifying MoPub of `onLeaveApplication()` from Mintegral's `onLeaveApp()` to avoid duplicate click tracking.

  * 12.0.01.1
    * MoPub will not be collecting GDPR consent on behalf of Mintegral. It is publisher’s responsibility to work with Mintegral to ensure GDPR compliance.
    * This version of the adapters has been certified with Mintegral 12.0.01.
    * Add support for Advanced Bidding.

  * 12.0.01.0
    * Do Not integrate this version.
    * Initial commit of the Mintegral adapters.
