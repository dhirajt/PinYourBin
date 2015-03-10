package com.pinyourbin.pinyourbin;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.pinyourbin.pinyourbin.PYBDbSchema.BinEntry;
/**
 * Created by dhiraj on 10/3/15.
 */

public class PYBDbHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "PinYourBin.db";

    private static final String COORDINATE_TYPE = " DECIMAL(8,5)";
    private static final String COMMA_SEP = ",";
    private static final String TIMESTAMP_TYPE = " INTEGER";

    private static final String BIN_ENTRY_SQL_CREATE_ENTRIES =
            "CREATE TABLE " + BinEntry.TABLE_NAME + " (" +
                    BinEntry.COLUMN_NAME_DB_ID + " INTEGER PRIMARY KEY AUTOINCREMENT" + COMMA_SEP +
                    BinEntry.COLUMN_NAME_LATITUDE + COORDINATE_TYPE + COMMA_SEP +
                    BinEntry.COLUMN_NAME_LONGITUDE + COORDINATE_TYPE + COMMA_SEP +
                    BinEntry.COLUMN_NAME_UNIX_TIMESTAMP + TIMESTAMP_TYPE + COMMA_SEP +
                    " UNIQUE (" +BinEntry.COLUMN_NAME_LATITUDE+COMMA_SEP+BinEntry.COLUMN_NAME_LATITUDE+
                    ") ON CONFLICT IGNORE )";

    private static final String BIN_ENTRY_SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + BinEntry.TABLE_NAME;

    public PYBDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(BIN_ENTRY_SQL_CREATE_ENTRIES);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(BIN_ENTRY_SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}
