package com.pinyourbin.pinyourbin;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.GpsStatus;
import android.location.LocationManager;

/**
 * Created by dhiraj on 8/3/15.
 */
public class PYBLocationManager {

    public boolean isGPSEnabled = true;

    public PYBLocationManager (Activity activity) {
        final LocationManager manager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);

        if ( !manager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
            isGPSEnabled = false;
            buildAlertMessageNoGps(activity,manager);
        }

        manager.addGpsStatusListener(new GpsStatus.Listener()
        {
            public void onGpsStatusChanged(int event)
            {
                switch(event)
                {
                    case GpsStatus.GPS_EVENT_STARTED:
                        isGPSEnabled = true;
                        break;
                    case GpsStatus.GPS_EVENT_STOPPED:
                        isGPSEnabled = false;
                        break;
                }
            }
        });
    }

    private void buildAlertMessageNoGps(final Activity activity,final LocationManager manager) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage("Your GPS seems to be disabled. PinYourBin requires your location to " +
                "accurately pin the place. Enable the GPS?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        activity.startActivity(new Intent(
                                android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        isGPSEnabled = manager.isProviderEnabled( LocationManager.GPS_PROVIDER );
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }
}
