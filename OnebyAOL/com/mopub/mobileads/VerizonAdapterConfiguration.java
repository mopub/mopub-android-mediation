package com.mopub.mobileads;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.mopub.common.BaseAdapterConfiguration;
import com.mopub.common.OnNetworkInitializationFinishedListener;

import java.util.Map;


/**
 * Compatible with version 1.0.1 of the Verizon Ads SDK StandardEdition.
 */

public class VerizonAdapterConfiguration extends BaseAdapterConfiguration {

	private static final String ADAPTER_VERSION = "1.0.1.0";
	private static final String MOPUB_NETWORK_NAME = "Verizon";

	public static final String MEDIATOR_ID = "MoPubVAS-" + ADAPTER_VERSION;
	public static final String BIDDING_TOKEN = "sy_bp";


	@NonNull
	@Override
	public String getAdapterVersion() {

		return ADAPTER_VERSION;
	}


	@Nullable
	@Override
	public String getBiddingToken(@NonNull Context context) {

		return BIDDING_TOKEN;
	}


	@NonNull
	@Override
	public String getMoPubNetworkName() {

		return MOPUB_NETWORK_NAME;
	}


	@NonNull
	@Override
	public String getNetworkSdkVersion() {

		String adapterVersion = getAdapterVersion();
		return (!TextUtils.isEmpty(adapterVersion)) ?
			adapterVersion.substring(0, adapterVersion.lastIndexOf('.')) : "";
	}


	@Override
	public void initializeNetwork(@NonNull final Context context, @Nullable final Map<String, String> configuration,
		@NonNull final OnNetworkInitializationFinishedListener listener) {

		// The context here is Application. If VAS is initialized with Application here, then VAS will not know that the activity is
		// visible.
		listener.onNetworkInitializationFinished(VerizonAdapterConfiguration.class, MoPubErrorCode.ADAPTER_INITIALIZATION_SUCCESS);
	}
}