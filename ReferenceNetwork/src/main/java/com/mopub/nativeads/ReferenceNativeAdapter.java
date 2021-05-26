// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.nativeads;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.ReferenceAdapterConfiguration;
import com.mopub.sdk.ReferenceNativeAdBase;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;
import static com.mopub.nativeads.NativeErrorCode.NETWORK_NO_FILL;

/**
 * This is a reference adapter implementation designed for testing the MoPub mediation protocol ONLY.
 * For that purpose, it leverages a simple network SDK implementation and does not constitute any real-world
 * testing as far as a live ad experience goes.
 * <p>
 * Future MoPub adapter implementations may refer to the implementation here for best practices.
 */
public class ReferenceNativeAdapter extends CustomEventNative {

    // Declare any global constants/keys needed by the adapter here for readability. Ensure you
    // are alphabetizing and grouping your constants based on names and visibility identifiers.
    private static final String ADAPTER_NAME = ReferenceNativeAdapter.class.getSimpleName();
    private static final String AD_UNIT_ID_KEY = "mAdUnitId";

    private static String mAdUnitId;
    private static CustomEventNativeListener mCustomEventNativeListener;

    @NonNull
    private final ReferenceAdapterConfiguration mReferenceAdapterConfiguration;

    public ReferenceNativeAdapter() {
        mReferenceAdapterConfiguration = new ReferenceAdapterConfiguration();
    }

    /**
     * When the MoPub SDK receives a response indicating it should load a custom event, it will send
     * this message to your custom event class. Your implementation of this method can either load a
     * native ad from a third-party ad network, or execute any application code. It must also notify
     * the provided {@link CustomEventNativeListener} Object of certain lifecycle events.
     *
     * @param context                   The context.
     * @param customEventNativeListener An Object that must be notified of certain lifecycle
     *                                  events.
     * @param localExtras               A Map containing additional custom data that is set within your
     *                                  application by calling {@link MoPubNative#setLocalExtras(Map)}. Note that the
     *                                  localExtras Map is a copy of the Map supplied to {@link MoPubNative#setLocalExtras(Map)}.
     * @param serverExtras              A Map containing additional custom data configurable on the MoPub website
     *                                  that you want to associate with a given custom event request. This data may be used to pass
     *                                  dynamic information, such as publisher IDs, without changes in application code.
     */
    @Override
    protected void loadNativeAd(@NonNull final Context context,
                                @NonNull final CustomEventNativeListener customEventNativeListener,
                                @NonNull final Map<String, Object> localExtras,
                                @NonNull final Map<String, String> serverExtras) {

        // It is recommended to fail fast if required parameters are invalid
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(customEventNativeListener);
        Preconditions.checkNotNull(localExtras);
        Preconditions.checkNotNull(serverExtras);

        /*
         * serverExtras contains network-specific data that you have entered on the MoPub dashboard.
         * Parse for the data needed by the network SDK to request ads and handle the ad experience.
         */
        if (!serverExtras.isEmpty()) {
            mAdUnitId = serverExtras.get(AD_UNIT_ID_KEY);
            mReferenceAdapterConfiguration.setCachedInitializationParameters(context, serverExtras);
        } else {
            customEventNativeListener.onNativeAdFailed(NETWORK_NO_FILL);
            MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME, NETWORK_NO_FILL.getIntCode(),
                    NETWORK_NO_FILL);
            return;
        }

        final ReferenceNativeAd referenceNativeAd = new ReferenceNativeAd(new ReferenceNativeAdBase(context),
                customEventNativeListener);

        referenceNativeAd.loadAd();
    }

    private static String getAdNetworkId() {
        return mAdUnitId == null ? "" : mAdUnitId;
    }

    public static class ReferenceNativeAd extends BaseNativeAd implements ReferenceNativeAdBase.ReferenceNativeAdListener {
        private ReferenceNativeAdBase mNativeAd;

        ReferenceNativeAd(final ReferenceNativeAdBase nativeAd,
                          final CustomEventNativeListener customEventNativeListener) {
            mNativeAd = nativeAd;
            mCustomEventNativeListener = customEventNativeListener;

            mNativeAd.setAdListener(this);
        }

        void loadAd() {
            mNativeAd.loadAd();
            MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);
        }

        final public String getMainImageUrl() {
            return mNativeAd.getMainImageUrl();
        }

        final public String getIconImageUrl() {
            return mNativeAd.getIconImageUrl();
        }

        /**
         * Returns the String corresponding to the advertiser name
         */
        final public String getAdvertiserName() {
            return mNativeAd.getAdvertiserName();
        }

        /**
         * Returns the String corresponding to the ad's title.
         */
        final public String getTitle() {
            return mNativeAd.getAdTitle();
        }

        /**
         * Returns the String corresponding to the ad's body text. May be null.
         */
        final public String getText() {
            return mNativeAd.getAdBodyText();
        }

        /**
         * Returns the Call To Action String (i.e. "Download" or "Learn More") associated with this ad.
         */
        final public String getCallToAction() {
            return mNativeAd.getAdCallToAction();
        }

        /**
         * Returns the Sponsored Label associated with this ad.
         */
        @Nullable
        final public String getSponsoredName() {
            return mNativeAd.getSponsoredTranslation();
        }

        /**
         * Returns the Privacy Information click through url.
         *
         * @return String representing the Privacy Information Icon click through url, or {@code null}
         * if not set.
         */
        final public String getPrivacyInformationIconClickThroughUrl() {
            return mNativeAd.getAdChoicesLinkUrl();
        }

        @Override
        public void onAdLoaded() {
            mCustomEventNativeListener.onNativeAdLoaded(this);
            MoPubLog.log(getAdNetworkId(), LOAD_SUCCESS, ADAPTER_NAME);
        }

        @Override
        public void onAdImpression() {
            notifyAdImpressed();
            MoPubLog.log(getAdNetworkId(), SHOW_SUCCESS, ADAPTER_NAME);
        }

        @Override
        public void onAdClicked() {
            notifyAdClicked();
            MoPubLog.log(getAdNetworkId(), CLICKED, ADAPTER_NAME);
        }

        @Override
        public void onAdFailedToLoad() {
            MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME, NETWORK_NO_FILL.getIntCode(),
                    NETWORK_NO_FILL);

            if (mCustomEventNativeListener != null) {
                mCustomEventNativeListener.onNativeAdFailed(NETWORK_NO_FILL);
            }
        }

        /**
         * Your {@link BaseNativeAd} subclass should implement this method if the network requires the developer
         * to prepare state for recording an impression or click before a view is rendered to screen.
         * <p>
         * This method is optional.
         */
        @Override
        public void prepare(@NonNull View view) {
        }

        /**
         * Your {@link BaseNativeAd} subclass should implement this method if the network requires the developer
         * to reset or clear state of the native ad after it goes off screen and before it is rendered
         * again.
         * <p>
         * This method is optional.
         */
        @Override
        public void clear(@NonNull View view) {
            Preconditions.checkNotNull(view);
        }

        /**
         * Your {@link BaseNativeAd} subclass should implement this method if the network requires the developer
         * to destroy or cleanup their native ad when they are permanently finished with it.
         * <p>
         * This method is optional.
         */
        @Override
        public void destroy() {
            if (mNativeAd != null) {
                mNativeAd.destroy();
                mNativeAd = null;
            }
        }
    }
}
