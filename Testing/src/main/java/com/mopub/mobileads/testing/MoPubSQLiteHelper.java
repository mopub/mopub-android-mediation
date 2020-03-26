// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads.testing;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

class MoPubSQLiteHelper extends SQLiteOpenHelper {
    public static final String TABLE_AD_CONFIGURATIONS = "adConfigurations";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_AD_UNIT_ID = "adUnitId";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_USER_GENERATED = "userGenerated";
    public static final String COLUMN_AD_TYPE = "adType";
    public static final String COLUMN_KEYWORDS = "keywords";

    private static final String DATABASE_NAME = "savedConfigurations.db";
    private static final int DATABASE_VERSION = 4;

    private static final String DATABASE_CREATE = "create table " + TABLE_AD_CONFIGURATIONS
            + " ("
            + COLUMN_ID + " integer primary key autoincrement, "
            + COLUMN_AD_UNIT_ID + " text not null, "
            + COLUMN_DESCRIPTION + " text not null, "
            + COLUMN_USER_GENERATED + " integer not null, "
            + COLUMN_AD_TYPE + " text not null,"
            + COLUMN_KEYWORDS + " text not null"
            + ");";

    public MoPubSQLiteHelper(final Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(DATABASE_CREATE);
    }

    @Override
    public void onDowngrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        Log.w(MoPubSQLiteHelper.class.getName(),
                "Downgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data"
        );
        recreateDb(database);
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        if (oldVersion == 3 && newVersion == 4) {
            addStringColumn(database, COLUMN_KEYWORDS, "");
        } else {
            Log.w(MoPubSQLiteHelper.class.getName(),
                    "Upgrading database from version " + oldVersion + " to "
                            + newVersion + ", which will destroy all old data"
            );
            recreateDb(database);
        }
    }

    private void recreateDb(SQLiteDatabase database) {
        database.execSQL("DROP TABLE IF EXISTS " + TABLE_AD_CONFIGURATIONS);
        onCreate(database);
    }

    private void addStringColumn(final SQLiteDatabase database,
                                 final String columnName,
                                 final String defaultValue) {
        database.execSQL("alter table " + TABLE_AD_CONFIGURATIONS
                + " add column " + columnName
                + " text default \"" + defaultValue + "\"");
    }
}
