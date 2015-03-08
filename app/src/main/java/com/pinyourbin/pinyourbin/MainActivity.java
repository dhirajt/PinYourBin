package com.pinyourbin.pinyourbin;

import android.app.Activity;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private Location mLastLocation;

    private GoogleLocationApiClientBuilder pybGoogleApiClientBuilder;
    private GoogleApiClient pybGoogleApiClient;

    private TextView locationTopHeading;
    private TextView locationText;
    private Button locationTapButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        locationTopHeading = (TextView) findViewById(R.id.locationTopHeading);
        locationText = (TextView) findViewById(R.id.locationText);
        locationTapButton = (Button) findViewById(R.id.locationTapButton);

        pybGoogleApiClientBuilder = new GoogleLocationApiClientBuilder();

        // First we check play services availabilty and then we build a client
        if (pybGoogleApiClientBuilder.checkPlayServices(this)) {
            pybGoogleApiClient = pybGoogleApiClientBuilder.buildGoogleApiClient(this);
        }

        // PYB tap button click listener
        locationTapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayLocation();
            }
        });
    }


    // Method to display location in UI
    private void displayLocation() {
        mLastLocation = LocationServices.FusedLocationApi
                .getLastLocation(pybGoogleApiClient);

        if (mLastLocation != null) {
            double latitude = mLastLocation.getLatitude();
            double longitude = mLastLocation.getLongitude();

            locationTopHeading.setText("You want a bin at");
            locationText.setText(latitude + ", " + longitude);

        } else {
            locationText
                    .setText("(Couldn't get the location. Make sure location is enabled on the device)");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (pybGoogleApiClient != null) {
            pybGoogleApiClient.connect();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        pybGoogleApiClientBuilder.checkPlayServices(this);
    }
}