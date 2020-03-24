// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads.testing;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;

import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static com.mopub.common.logging.MoPubLog.SdkLogEvent.CUSTOM;
import static com.mopub.mobileads.testing.MoPubSQLiteHelper.COLUMN_AD_TYPE;
import static com.mopub.mobileads.testing.MoPubSQLiteHelper.COLUMN_AD_UNIT_ID;
import static com.mopub.mobileads.testing.MoPubSQLiteHelper.COLUMN_DESCRIPTION;
import static com.mopub.mobileads.testing.MoPubSQLiteHelper.COLUMN_ID;
import static com.mopub.mobileads.testing.MoPubSQLiteHelper.COLUMN_KEYWORDS;
import static com.mopub.mobileads.testing.MoPubSQLiteHelper.COLUMN_USER_GENERATED;
import static com.mopub.mobileads.testing.MoPubSQLiteHelper.TABLE_AD_CONFIGURATIONS;
import static com.mopub.mobileads.testing.MoPubSampleAdUnit.AdType;

class AdUnitDataSource {
    private Context mContext;
    private MoPubSQLiteHelper mDatabaseHelper;
    private String[] mAllColumns = {
            COLUMN_ID,
            COLUMN_AD_UNIT_ID,
            COLUMN_DESCRIPTION,
            COLUMN_USER_GENERATED,
            COLUMN_AD_TYPE,
            COLUMN_KEYWORDS
    };

    AdUnitDataSource(final Context context) {
        mContext = context.getApplicationContext();
        mDatabaseHelper = new MoPubSQLiteHelper(context);
        populateDefaultSampleAdUnits();
    }

    MoPubSampleAdUnit createDefaultSampleAdUnit(final MoPubSampleAdUnit sampleAdUnit) {
        return createSampleAdUnit(sampleAdUnit, false);
    }

    MoPubSampleAdUnit createSampleAdUnit(final MoPubSampleAdUnit sampleAdUnit) {
        return createSampleAdUnit(sampleAdUnit, true);
    }

    private MoPubSampleAdUnit createSampleAdUnit(final MoPubSampleAdUnit sampleAdUnit,
                                                 final boolean isUserGenerated) {
        deleteAllAdUnitsWithAdUnitIdAndAdTypeAndKeywords(sampleAdUnit.getAdUnitId(),
                sampleAdUnit.getFragmentClassName(),
                sampleAdUnit.getKeywords(),
                isUserGenerated);

        final ContentValues values = new ContentValues();
        final int userGenerated = isUserGenerated ? 1 : 0;
        values.put(COLUMN_AD_UNIT_ID, sampleAdUnit.getAdUnitId());
        values.put(COLUMN_DESCRIPTION, sampleAdUnit.getDescription());
        values.put(COLUMN_USER_GENERATED, userGenerated);
        values.put(COLUMN_AD_TYPE, sampleAdUnit.getFragmentClassName());
        values.put(COLUMN_KEYWORDS, sampleAdUnit.getKeywords());

        final SQLiteDatabase database = mDatabaseHelper.getWritableDatabase();
        final long insertId = database.insert(TABLE_AD_CONFIGURATIONS, null, values);
        final Cursor cursor = database.query(TABLE_AD_CONFIGURATIONS, mAllColumns,
                COLUMN_ID + " = " + insertId, null, null, null, null);
        cursor.moveToFirst();

        final MoPubSampleAdUnit newAdConfiguration = cursorToAdConfiguration(cursor);
        cursor.close();
        database.close();

        if (newAdConfiguration != null) {
            MoPubLog.log(CUSTOM, "Ad configuration added with id: " + newAdConfiguration.getId());
        }
        return newAdConfiguration;
    }

    void deleteSampleAdUnit(final MoPubSampleAdUnit adConfiguration) {
        final long id = adConfiguration.getId();
        SQLiteDatabase database = mDatabaseHelper.getWritableDatabase();
        database.delete(TABLE_AD_CONFIGURATIONS, COLUMN_ID + " = " + id, null);
        MoPubLog.log(CUSTOM, "Ad Configuration deleted with id: " + id);
        database.close();
    }

    private void deleteAllAdUnitsWithAdUnitIdAndAdTypeAndKeywords(@NonNull final String adUnitId,
                                                                  @NonNull final String adType,
                                                                  @NonNull final String keywords,
                                                                  boolean isUserGenerated) {
        Preconditions.checkNotNull(adUnitId);
        Preconditions.checkNotNull(adType);
        Preconditions.checkNotNull(keywords);

        final String userGenerated = isUserGenerated ? "1" : "0";
        final SQLiteDatabase database = mDatabaseHelper.getWritableDatabase();
        final int numDeletedRows = database.delete(TABLE_AD_CONFIGURATIONS,
                COLUMN_AD_UNIT_ID + " = '" + adUnitId
                        + "' AND " + COLUMN_USER_GENERATED + " = " + userGenerated
                        + " AND " + COLUMN_AD_TYPE + " = '" + adType + "'"
                        + " AND " + COLUMN_KEYWORDS + " = '" + keywords + "'", null);
        MoPubLog.log(CUSTOM, numDeletedRows + " rows deleted with adUnitId: " + adUnitId);
        database.close();
    }

    List<MoPubSampleAdUnit> getAllAdUnits() {
        final List<MoPubSampleAdUnit> adConfigurations = new ArrayList<>();
        SQLiteDatabase database = mDatabaseHelper.getReadableDatabase();
        final Cursor cursor = database.query(TABLE_AD_CONFIGURATIONS,
                mAllColumns, null, null, null, null, null);
        cursor.moveToFirst();

        while (!cursor.isAfterLast()) {
            final MoPubSampleAdUnit adConfiguration = cursorToAdConfiguration(cursor);
            if (adConfiguration != null) {
                adConfigurations.add(adConfiguration);
            }
            cursor.moveToNext();
        }

        cursor.close();
        database.close();
        return adConfigurations;
    }

    private void populateDefaultSampleAdUnits() {
        final HashSet<MoPubSampleAdUnit> allAdUnits = new HashSet<>(getAllAdUnits());

        for (final MoPubSampleAdUnit defaultAdUnit :
                SampleAppAdUnits.Defaults.getAdUnits(mContext)) {
            if (!allAdUnits.contains(defaultAdUnit)) {
                createDefaultSampleAdUnit(defaultAdUnit);
            }
        }
    }

    private MoPubSampleAdUnit cursorToAdConfiguration(final Cursor cursor) {
        final long id = cursor.getLong(0);
        final String adUnitId = cursor.getString(1);
        final String description = cursor.getString(2);
        final int userGenerated = cursor.getInt(3);
        final AdType adType = AdType.fromFragmentClassName(cursor.getString(4));
        final String keywords = cursor.getString(5);

        if (adType == null) {
            return null;
        }

        return new MoPubSampleAdUnit.Builder(adUnitId, adType)
                .description(description)
                .isUserDefined(userGenerated == 1)
                .keywords(keywords)
                .id(id)
                .build();
    }
}
