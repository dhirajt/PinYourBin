package com.pinyourbin.pinyourbin;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.pinyourbin.pinyourbin.PYBDbSchema.BinEntry;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity implements LocationListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    public static final MediaType MEDIA_TYPE_JSON
            = MediaType.parse("application/json; charset=utf-8");

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
    private static int FASTEST_INTERVAL = 5000; // 5 sec
    private static int DISPLACEMENT = 10; // 10 meters

    private static String PYBBackendURL = "http://10.0.2.2:5000/save/location/"; //host url (android studio->localhost)
    private final OkHttpClient client = new OkHttpClient();

    DeviceInterface device = DeviceInterface.getDeviceInterface();
    private PYBDbHelper dbHelper = null;

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

        dbHelper = new PYBDbHelper(getApplicationContext());
    }


    // Method to display location in UI
    private void displayLocation() {
        pybLocation = LocationServices.FusedLocationApi
                .getLastLocation(pybGoogleApiClient);


        mLocationRequest = LocationRequest.create();
            mLocationRequest.setNumUpdates(1);
            mLocationRequest.setInterval(UPDATE_INTERVAL);
            mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
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
            } else {
                locationAddressText.setText("");
            }

            saveLocation(latitude,longitude);

        } else if (manager.isGPSEnabled) {
            locationTopHeading.setText("You want a bin at");
            locationText.setText("Waiting for location ...");
        } else {
            locationText.setText("Couldn't get your location.\nMake sure location is enabled on the device");
        }
    }

    private void saveLocation(double latitude,double longitude) {
        DeviceInterface device = DeviceInterface.getDeviceInterface();

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        long insertID = -1L;

        long unixTime = System.currentTimeMillis() / 1000L;

        ContentValues values = new ContentValues();
        values.put(BinEntry.COLUMN_NAME_LATITUDE, latitude);
        values.put(BinEntry.COLUMN_NAME_LONGITUDE, longitude);
        values.put(BinEntry.COLUMN_NAME_UNIX_TIMESTAMP, unixTime);
        values.put(BinEntry.COLUMN_NAME_DEVICE_ID, device.id(getApplicationContext()));

        String query = dbHelper.getIdIfRowExistsQuery(latitude,longitude);
        Cursor cursor = db.rawQuery(query,null);

        while (cursor.moveToNext()) {  // If no element in cursor it should exit
            insertID = cursor.getColumnIndex(BinEntry.COLUMN_NAME_DB_ID);
        }
        cursor.close();

        if (insertID == -1L) {
            insertID = db.insert(BinEntry.TABLE_NAME, null, values);
        }

        try {
            if (insertID >= device.getLastSyncedDbId(getApplicationContext())+5) {
                // Sync only if saved bin id is at least 5 more than last insert (to cut down network calls)
                // TODO: find a better strategy later
                syncBinsToServer();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        db.close();
    }

    public void syncBinsToServer() throws IOException {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        long syncFromId = device.getLastSyncedDbId(getApplicationContext());

        String query = dbHelper.getRowsFromIdQuery(syncFromId);

        Cursor cursor = db.rawQuery(query,null);

        ArrayList listOfBins = new ArrayList();

        long lastSyncedId = 0;
        String deviceId = null;

        while (cursor.moveToNext()) {
            HashMap<String,String> bin = new HashMap<String,String>();
            int id = Integer.parseInt(cursor.getString(cursor.getColumnIndex(BinEntry.COLUMN_NAME_DB_ID)));

            if (id > lastSyncedId) {
                lastSyncedId = id;
            }
            bin.put(BinEntry.COLUMN_NAME_LATITUDE,
                    cursor.getString(cursor.getColumnIndex(BinEntry.COLUMN_NAME_LATITUDE)));
            bin.put(BinEntry.COLUMN_NAME_LONGITUDE,
                    cursor.getString(cursor.getColumnIndex(BinEntry.COLUMN_NAME_LONGITUDE)));
            bin.put(BinEntry.COLUMN_NAME_UNIX_TIMESTAMP,
                    cursor.getString(cursor.getColumnIndex(BinEntry.COLUMN_NAME_UNIX_TIMESTAMP)));
            bin.put(BinEntry.COLUMN_NAME_DEVICE_ID,
                    cursor.getString(cursor.getColumnIndex(BinEntry.COLUMN_NAME_DEVICE_ID)));

            if (bin.get(BinEntry.COLUMN_NAME_DEVICE_ID)!=null) {
                deviceId = bin.get(BinEntry.COLUMN_NAME_DEVICE_ID);
            }
            listOfBins.add(bin);
        }
        cursor.close();

        String json = null;

        ObjectMapper mapper=new ObjectMapper();
        json = mapper.writeValueAsString(listOfBins);

        Request request = new Request.Builder()
                .url(PYBBackendURL)
                .header("User-Agent", "OkHttp PinYourBin "+ deviceId)
                .addHeader("Accept", "application/json;")
                .post(RequestBody.create(MEDIA_TYPE_JSON,json))
                .build();

        final long finalLastSyncedId = lastSyncedId;
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException exception) {
                exception.printStackTrace();
            }

            @Override
            public void onResponse(Response response) throws IOException {
                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

                device.setLastSyncedDbId(getApplicationContext(), finalLastSyncedId);
            }
        });
        db.close();
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
                saveLocation(location.getLatitude(),location.getLongitude());
            } else {
                locationAddressText.setText("");
            }
        }
    }

    public String getAddressFromLocation(Location location){
        String humanReadableAddress = null;
        Geocoder gcd = new Geocoder(getApplicationContext(), Locale.getDefault());
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