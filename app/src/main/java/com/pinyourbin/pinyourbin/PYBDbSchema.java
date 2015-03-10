package com.pinyourbin.pinyourbin;

import android.provider.BaseColumns;

/**
 * Created by dhiraj on 10/3/15.
 */
public class PYBDbSchema {
    // To prevent someone from accidentally instantiating the contract class,
    // give it an empty constructor.
    public PYBDbSchema() {}

    public static abstract class BinEntry implements BaseColumns {
        public static final String TABLE_NAME = "Bins";
        public static final String COLUMN_NAME_DB_ID = "id";
        public static final String COLUMN_NAME_LATITUDE = "latitude";
        public static final String COLUMN_NAME_LONGITUDE = "longitude";
        public static final String COLUMN_NAME_UNIX_TIMESTAMP = "timestamp"; //utc timestamp
    }
}

