// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads.testing;

import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;

import com.mopub.common.Constants;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.Reflection;

import java.lang.reflect.Field;

import static com.mopub.common.logging.MoPubLog.SdkLogEvent.CUSTOM_WITH_THROWABLE;

public class SampleActivityInternalUtils {

    private static final String PROD_HOST = Constants.HOST;
    private static final String TEST_HOST = "ads-staging.mopub.com";

    static void updateEndpointMenu(@NonNull final Menu menu) {
        final boolean production = PROD_HOST.equalsIgnoreCase(Constants.HOST);
        menu.findItem(R.id.nav_production).setChecked(production);
        menu.findItem(R.id.nav_staging).setChecked(!production);
    }

    static void handleEndpointMenuSelection(@NonNull final MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.nav_production:
                setEndpoint(getChosenEndpoint(true));
                break;
            case R.id.nav_staging:
                setEndpoint(getChosenEndpoint(false));
                break;
        }
    }

    private static String getChosenEndpoint(boolean production) {
        return production ? PROD_HOST : TEST_HOST;
    }

    private static void setEndpoint(@NonNull final String host) {
        try {
            final Field field = Reflection.getPrivateField(com.mopub.common.Constants.class, "HOST");
            field.set(null, host);
        } catch (Exception e) {
            MoPubLog.log(CUSTOM_WITH_THROWABLE, "Can't change HOST.", e);
        }
    }
}