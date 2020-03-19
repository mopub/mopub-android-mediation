## Changelog
  * 1.4.0.0
    * Update Advanced Bidding API
    * This version of the adapters has been certified with Verizon 1.4.0.

  * 1.3.1.0
    * This version of the adapters has been certified with Verizon 1.3.1.
  
  * 1.3.0.0
    * This version of the adapters has been certified with Verizon 1.3.0.
    * Remove Advanced Bidding token generation logic from the adapters. The equivalent logic will be added to the Verizon SDK.

  * 1.2.1.3
    * Log the Verizon placement ID in ad lifecycle events.

  * 1.2.1.2
    * Add support for Advanced Bidding for banner and interstitial.

  * 1.2.1.1
    * Migrate utility methods in `VerizonUtils.java` to `VerizonAdapterConfiguration.java` and delete the former.

  * 1.2.1.0
    * This version of the adapters has been certified with Verizon 1.2.1.
    * Remove all permissions except INTERNET in the adapter's AndroidManifest (to be in parity with the [Verizon SDK](https://sdk.verizonmedia.com/standard-edition/releasenotes-android.html)).

  * 1.2.0.1
    * Log the Verizon SDK edition name (if available) together with the network SDK version.
  
  * 1.2.0.0
    * This version of the adapters has been certified with Verizon 1.2.0.

  * 1.1.4.1
    * Add support for AndroidX. This is the minimum version compatible with MoPub 5.9.0.

  * 1.1.4.0
    * Add support for rewarded video and native ad.
    * This version of the adapters has been certified with Verizon 1.1.4.

  * 1.1.3.0
    * This version of the adapters has been certified with Verizon 1.1.3.

  * 1.1.1.1
    * Add support for parsing banner's width and height from `serverExtras`. This provides backwards compatibility for legacy Millennial adapters.

  * 1.1.1.0
    * This version of the adapters has been certified with Verizon 1.1.1.
    * Add support to initialize the Verizon SDK in conjunction with MoPub's initialization.
  
  * 1.0.2.2
    * Remove `maxSdkVersion` from a permission included in the AndroidManifest to avoid merge conflicts.

  * 1.0.2.1
    * Pass the banner size from the MoPub ad response to Verizon.

  * 1.0.2.0
    * This version of the adapters has been certified with Verizon 1.0.2.
