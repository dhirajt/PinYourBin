package com.pinyourbin.pinyourbin;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

/**
 * Created by dhiraj on 8/3/15.
 */
public class GoogleLocationApiClientBuilder implements GoogleApiClient.OnConnectionFailedListener {
    private GoogleApiClient client;

    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;
    public boolean deviceSupported = true;

    protected synchronized GoogleApiClient buildGoogleApiClient(Activity activity) {
        client = new com.google.android.gms.common.api.GoogleApiClient.Builder(activity)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();
        return client;
    }


    public boolean checkPlayServices(Activity activity) {
        int resultCode = GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(activity);

        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, activity,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                deviceSupported = false;
                Toast.makeText(activity,
                        "This device is not supported.", Toast.LENGTH_LONG)
                        .show();
                activity.finish();
            }
            return false;
        }
        return true;
    }

    public void onConnectionFailed(ConnectionResult result) {
        Log.e("Activity Error:","Connection failed: ConnectionResult.getErrorCode() = "
                + result.getErrorCode());
    }

    public void onConnectionSuspended(int arg0) {
        client.connect();
    }
}
