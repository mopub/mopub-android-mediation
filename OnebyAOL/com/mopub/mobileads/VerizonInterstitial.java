package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;

import com.mopub.common.MoPub;
import com.mopub.common.logging.MoPubLog;
import com.verizon.ads.Bid;
import com.verizon.ads.BidRequestListener;
import com.verizon.ads.CreativeInfo;
import com.verizon.ads.ErrorInfo;
import com.verizon.ads.RequestMetadata;
import com.verizon.ads.VASAds;
import com.verizon.ads.edition.StandardEdition;
import com.verizon.ads.interstitialplacement.InterstitialAd;
import com.verizon.ads.interstitialplacement.InterstitialAdFactory;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.DID_DISAPPEAR;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.WILL_LEAVE_APPLICATION;


/**
 * Compatible with version 1.0.1 of the Verizon Ads SDK StandardEdition.
 */

public class VerizonInterstitial extends CustomEventInterstitial {

	private static final String ADAPTER_NAME = VerizonInterstitial.class.getSimpleName();

	private static final String PLACEMENT_ID_KEY = "placementId";
	private static final String SITE_ID_KEY = "siteId";

	private InterstitialAd verizonInterstitialAd;
	private Context context;
	private CustomEventInterstitialListener interstitialListener;

	static {
		MoPubLog.log(CUSTOM, ADAPTER_NAME, "Verizon Adapter Version: " + VerizonAdapterConfiguration.MEDIATOR_ID);
	}


	/**
	 * Call this method to cache a super auction bid for the specified placement ID
	 *
	 * @param context            a non-null Context
	 * @param placementId        a valid placement ID. Cannot be null or empty.
	 * @param requestMetadata    a {@link RequestMetadata} instance for the request or null
	 * @param bidRequestListener an instance of {@link BidRequestListener}. Cannot be null.
	 */
	public static void requestBid(final Context context, final String placementId, final RequestMetadata requestMetadata,
		final BidRequestListener bidRequestListener) {

		if (bidRequestListener == null) {
			MoPubLog.log(CUSTOM, ADAPTER_NAME, "bidRequestListener parameter cannot be null.");

			return;
		}

		RequestMetadata.Builder builder = new RequestMetadata.Builder(requestMetadata);
		RequestMetadata actualRequestMetadata = builder.setMediator(VerizonAdapterConfiguration.MEDIATOR_ID).build();

		InterstitialAdFactory.requestBid(context, placementId, actualRequestMetadata, new BidRequestListener() {
			@Override
			public void onComplete(Bid bid, ErrorInfo errorInfo) {

				if (errorInfo == null) {
					BidCache.put(placementId, bid);
				}

				bidRequestListener.onComplete(bid, errorInfo);
			}
		});
	}


	private CreativeInfo getCreativeInfo() {

		if (verizonInterstitialAd == null) {
			return null;
		}

		return verizonInterstitialAd.getCreativeInfo();
	}


	@Override
	protected void loadInterstitial(final Context context,
		final CustomEventInterstitialListener customEventInterstitialListener, final Map<String, Object> localExtras,
		final Map<String, String> serverExtras) {

		interstitialListener = customEventInterstitialListener;
		this.context = context;

		if (serverExtras == null) {
			MoPubLog.log(CUSTOM, ADAPTER_NAME, "serverExtras is null");
			MoPubLog.log(LOAD_FAILED, ADAPTER_NAME, MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR.getIntCode(),
				MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);

			interstitialListener.onInterstitialFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);

			return;
		}

		if (!VASAds.isInitialized()) {
			String siteId = serverExtras.get(getSiteIdKey());

			if (VerizonUtils.isEmpty(siteId)) {
				MoPubLog.log(CUSTOM, ADAPTER_NAME, "siteId is empty");
				MoPubLog.log(LOAD_FAILED, ADAPTER_NAME, MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR.getIntCode(),
					MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);

				interstitialListener.onInterstitialFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);

				return;
			}

			if (!(context instanceof Activity)) {
				MoPubLog.log(CUSTOM, ADAPTER_NAME, "context is not an Activity");
				MoPubLog.log(LOAD_FAILED, ADAPTER_NAME, MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR.getIntCode(),
					MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);

				interstitialListener.onInterstitialFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);

				return;
			}

			boolean success = StandardEdition.initializeWithActivity((Activity) context, siteId);

			if (!success) {
				MoPubLog.log(CUSTOM, ADAPTER_NAME, "Failed to initialize the Verizon SDK");
				MoPubLog.log(LOAD_FAILED, ADAPTER_NAME, MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR.getIntCode(),
					MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);

				interstitialListener.onInterstitialFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);

				return;
			}
		}

		String placementId = serverExtras.get(getPlacementIdKey());

		if (VerizonUtils.isEmpty(placementId)) {
			MoPubLog.log(CUSTOM, ADAPTER_NAME, "Invalid server extras! Make sure placementId is set");
			MoPubLog.log(LOAD_FAILED, ADAPTER_NAME, MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR.getIntCode(),
				MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);

			interstitialListener.onInterstitialFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);

			return;
		}

		VASAds.setLocationEnabled(MoPub.getLocationAwareness() != MoPub.LocationAwareness.DISABLED);

		InterstitialAdFactory interstitialAdFactory = new InterstitialAdFactory(context, placementId,
			new VerizonInterstitialFactoryListener());

		Bid bid = BidCache.get(placementId);

		if (bid == null) {
			RequestMetadata requestMetadata = new RequestMetadata.Builder().setMediator(VerizonAdapterConfiguration.MEDIATOR_ID).build();
			interstitialAdFactory.setRequestMetaData(requestMetadata);
			interstitialAdFactory.load(new VerizonInterstitialListener());
		} else {
			interstitialAdFactory.load(bid, new VerizonInterstitialListener());
		}
	}


	@Override
	protected void showInterstitial() {

		MoPubLog.log(SHOW_ATTEMPTED, ADAPTER_NAME, "Loaded");

		VerizonUtils.postOnUiThread(new Runnable() {
			@Override
			public void run() {

				if (verizonInterstitialAd == null) {
					interstitialListener.onInterstitialFailed(MoPubErrorCode.INTERNAL_ERROR);
					MoPubLog.log(SHOW_FAILED, ADAPTER_NAME, MoPubErrorCode.UNSPECIFIED, "interstitial is not ready");

					return;
				}

				verizonInterstitialAd.show(context);

			}

		});
	}


	@Override
	protected void onInvalidate() {

		VerizonUtils.postOnUiThread(new Runnable() {
			@Override
			public void run() {

				// Destroy any hanging references
				if (verizonInterstitialAd != null) {
					verizonInterstitialAd.destroy();
					verizonInterstitialAd = null;
				}
			}
		});
	}


	protected String getPlacementIdKey() {

		return PLACEMENT_ID_KEY;
	}


	protected String getSiteIdKey() {

		return SITE_ID_KEY;
	}


	class VerizonInterstitialFactoryListener implements InterstitialAdFactory.InterstitialAdFactoryListener {


		@Override
		public void onLoaded(final InterstitialAdFactory interstitialAdFactory, final InterstitialAd interstitialAd) {

			VerizonInterstitial.this.verizonInterstitialAd = interstitialAd;

			MoPubLog.log(LOAD_SUCCESS, ADAPTER_NAME);

			CreativeInfo creativeInfo = getCreativeInfo();

			if (creativeInfo != null) {
				MoPubLog.log(CUSTOM, ADAPTER_NAME, "Ad Creative Info: " + creativeInfo);
			}

			VerizonUtils.postOnUiThread(new Runnable() {
				@Override
				public void run() {

					interstitialListener.onInterstitialLoaded();
				}
			});
		}


		@Override
		public void onCacheLoaded(final InterstitialAdFactory interstitialAdFactory, final int i, final int i1) {

		}


		@Override
		public void onCacheUpdated(final InterstitialAdFactory interstitialAdFactory, final int i) {

		}


		@Override
		public void onError(final InterstitialAdFactory interstitialAdFactory, final ErrorInfo errorInfo) {

			MoPubLog.log(CUSTOM, ADAPTER_NAME, "Error Loading: " + errorInfo);
			VerizonUtils.postOnUiThread(new Runnable() {
				@Override
				public void run() {

					MoPubErrorCode errorCode = VerizonUtils.convertErrorCodeToMoPub(errorInfo.getErrorCode());
					interstitialListener.onInterstitialFailed(errorCode);
					MoPubLog.log(LOAD_FAILED, ADAPTER_NAME, errorCode.getIntCode(), errorCode);
				}
			});
		}
	}


	class VerizonInterstitialListener implements InterstitialAd.InterstitialAdListener {


		@Override
		public void onError(final InterstitialAd interstitialAd, final ErrorInfo errorInfo) {

			MoPubLog.log(CUSTOM, ADAPTER_NAME, "Error: " + errorInfo);
			VerizonUtils.postOnUiThread(new Runnable() {
				@Override
				public void run() {

					MoPubErrorCode errorCode = VerizonUtils.convertErrorCodeToMoPub(errorInfo.getErrorCode());
					interstitialListener.onInterstitialFailed(errorCode);
					MoPubLog.log(SHOW_FAILED, ADAPTER_NAME, errorCode.getIntCode(), errorCode);
				}
			});
		}


		@Override
		public void onShown(final InterstitialAd interstitialAd) {

			MoPubLog.log(SHOW_SUCCESS, ADAPTER_NAME);
			VerizonUtils.postOnUiThread(new Runnable() {
				@Override
				public void run() {

					interstitialListener.onInterstitialShown();
				}
			});
		}


		@Override
		public void onClosed(final InterstitialAd interstitialAd) {

			MoPubLog.log(DID_DISAPPEAR, ADAPTER_NAME);
			VerizonUtils.postOnUiThread(new Runnable() {
				@Override
				public void run() {

					interstitialListener.onInterstitialDismissed();
				}
			});
		}


		@Override
		public void onClicked(final InterstitialAd interstitialAd) {

			MoPubLog.log(CLICKED, ADAPTER_NAME);
			VerizonUtils.postOnUiThread(new Runnable() {
				@Override
				public void run() {

					interstitialListener.onInterstitialClicked();
				}
			});
		}


		@Override
		public void onAdLeftApplication(final InterstitialAd interstitialAd) {

			MoPubLog.log(WILL_LEAVE_APPLICATION, ADAPTER_NAME);
			VerizonUtils.postOnUiThread(new Runnable() {
				@Override
				public void run() {

					interstitialListener.onLeaveApplication();
				}
			});
		}


		@Override
		public void onEvent(final InterstitialAd interstitialAd, final String s, final String s1, final Map<String, Object> map) {

		}
	}
}
