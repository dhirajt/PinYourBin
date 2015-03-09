package com.pinyourbin.pinyourbin;

import android.app.Activity;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity implements LocationListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private Location pybLocation;

    private GoogleLocationApiClientBuilder pybGoogleApiClientBuilder;
    private GoogleApiClient pybGoogleApiClient;

    private TextView locationTopHeading;
    private TextView locationText;
    private TextView locationAddressText;
    private Button locationTapButton;

    private LocationRequest mLocationRequest;
    private PYBLocationManager manager;

    // Location updates intervals in sec
    private static int UPDATE_INTERVAL = 10000; // 10 sec
    private static int FATEST_INTERVAL = 5000; // 5 sec
    private static int DISPLACEMENT = 10; // 10 meters

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        locationTopHeading = (TextView) findViewById(R.id.locationTopHeading);
        locationText = (TextView) findViewById(R.id.locationText);
        locationAddressText = (TextView) findViewById(R.id.locationAddressText);
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
        pybLocation = LocationServices.FusedLocationApi
                .getLastLocation(pybGoogleApiClient);


        mLocationRequest = LocationRequest.create();
            mLocationRequest.setNumUpdates(1);
            mLocationRequest.setInterval(UPDATE_INTERVAL);
            mLocationRequest.setFastestInterval(FATEST_INTERVAL);
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            mLocationRequest.setSmallestDisplacement(DISPLACEMENT);

        LocationServices.FusedLocationApi.requestLocationUpdates(
                pybGoogleApiClient, mLocationRequest, this);

        if (pybLocation != null) {
            double latitude = pybLocation.getLatitude();
            double longitude = pybLocation.getLongitude();

            locationTopHeading.setText("You want a bin at");

            String address = getAddressFromLocation(pybLocation);

            locationText.setText("(" + latitude + ", " + longitude + ")");

            if (address != null) {
                locationAddressText.setText(address);
            }
        } else if (manager.isGPSEnabled) {
            locationTopHeading.setText("You want a bin at");
            locationText
                    .setText("Waiting for location ...");
        } else {
            locationText.setText("Couldn't get your location.\nMake sure location is enabled on the device");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        manager = new PYBLocationManager(this);
        if (pybGoogleApiClient != null) {
            pybGoogleApiClient.connect();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        pybGoogleApiClientBuilder.checkPlayServices(this);
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            Toast.makeText(getApplicationContext(), "Location changed!",
                    Toast.LENGTH_SHORT).show();
            this.pybLocation = location;
            String address = getAddressFromLocation(location);

            locationText.setText("("+location.getLatitude() + ", " + location.getLongitude()+")");
            if (address != null) {
                locationAddressText.setText(address);
            }
        }
    }

    public String getAddressFromLocation(Location location){
        String humanReadableAddress = null;
        Geocoder gcd = new Geocoder(getBaseContext(), Locale.getDefault());
        List<Address> addresses;

        try {
            addresses = gcd.getFromLocation(location.getLatitude(),
                    location.getLongitude(), 1);

            if (!(addresses == null || addresses.isEmpty())) {
                Address address = addresses.get(0);

                humanReadableAddress = "";
                int maxLines = address.getMaxAddressLineIndex();
                for (int lineNum = 0; lineNum<=maxLines; lineNum++){
                    humanReadableAddress += address.getAddressLine(lineNum);
                    if (lineNum != maxLines) {
                        humanReadableAddress +=  ", ";
                    }
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return humanReadableAddress;
    }
}