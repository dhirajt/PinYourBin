package com.pinyourbin.pinyourbin;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.UUID;

/**
 * Created by dhiraj on 11/3/15.
 */

public class DeviceInterface {
    private static String uniqueID = null;
    private static String active = null;
    private static final String PREF_DEVICE_ID = "PINYOURBIN_DEVICE_ID";
    private static final String PREF_DEVICE_ACTIVE = "PINYOURBIN_DEVICE_ACTIVE";
    private static final String PREF_DEVICE_SYNC_DB_ID = "PINYOURBIN_DEVICE_SYNC_DB_ID";


    private DeviceInterface(){}
    private static DeviceInterface singleton = new DeviceInterface();

    public static DeviceInterface getDeviceInterface() {
        return singleton;
    }

    protected synchronized static String id(Context context) {
        if (uniqueID == null) {
            SharedPreferences sharedPrefs = context.getSharedPreferences(
                    PREF_DEVICE_ID, Context.MODE_PRIVATE);
            uniqueID = sharedPrefs.getString(PREF_DEVICE_ID, null);
            if (uniqueID == null) {
                uniqueID = UUID.randomUUID().toString();
                SharedPreferences.Editor editor = sharedPrefs.edit();
                editor.putString(PREF_DEVICE_ID, uniqueID);
                editor.commit();
            }
        }
        return uniqueID;
    }
    protected synchronized static boolean is_active(Context context) {
        if (active == null) {
            SharedPreferences sharedPrefs = context.getSharedPreferences(
                    PREF_DEVICE_ACTIVE, Context.MODE_PRIVATE);
            active = sharedPrefs.getString(PREF_DEVICE_ACTIVE, null);
            if (active == null) {
                active = "true";
                SharedPreferences.Editor editor = sharedPrefs.edit();
                editor.putString(PREF_DEVICE_ACTIVE, active);
                editor.commit();
            }
        }
        if (active == "true") {
            return true;
        }
        return false;
    }
    protected synchronized static void deactivate(Context context) {
        SharedPreferences sharedPrefs = context.getSharedPreferences(
                    PREF_DEVICE_ACTIVE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(PREF_DEVICE_ACTIVE, "false");
        editor.commit();
    }
    protected synchronized static long getLastSyncedDbId(Context context) {
        SharedPreferences sharedPrefs = context.getSharedPreferences(
                PREF_DEVICE_ACTIVE, Context.MODE_PRIVATE);

        long dbId = 0;
        dbId = sharedPrefs.getLong(PREF_DEVICE_SYNC_DB_ID,0);
        return dbId;
    }
    protected synchronized static void setLastSyncedDbId(Context context,long dbId) {
        SharedPreferences sharedPrefs = context.getSharedPreferences(
                PREF_DEVICE_ACTIVE, Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putLong(PREF_DEVICE_SYNC_DB_ID, dbId);
        editor.commit();
    }
}
