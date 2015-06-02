package com.pinyourbin.pinyourbin;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
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

public class MainActivity extends ActionBarActivity implements LocationListener,OnMapReadyCallback,GoogleMap.OnMyLocationChangeListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    public static final MediaType MEDIA_TYPE_JSON
            = MediaType.parse("application/json; charset=utf-8");

    private Location pybLocation;

    private GoogleLocationApiClientBuilder pybGoogleApiClientBuilder;
    private GoogleApiClient pybGoogleApiClient;

    private TextView locationText;
    private TextView locationAddressText;
    private Button locationTapButton;

    private LocationRequest mLocationRequest;
    private PYBLocationManager locationManager;

    private Handler mHandler;

    // Location updates intervals in sec
    private static int UPDATE_INTERVAL = 10000; // 10 sec
    private static int FASTEST_INTERVAL = 5000; // 5 sec
    private static int DISPLACEMENT = 10; // 10 meters

    private static String PYBBackendURL = "http://128.199.93.193//save/location/"; //host url (android studio->localhost)
    private final OkHttpClient client = new OkHttpClient();

    DeviceInterface device = DeviceInterface.getDeviceInterface();
    private PYBDbHelper dbHelper = null;

    private MapFragment mapFragment;
    private GoogleMap map;

    // UI handler codes.
    private static final int UPDATE_ADDRESS = 1;

    // Saved  locations
    ArrayList<Location> locations = new ArrayList<Location>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(false);
        // actionBar.setDisplayShowHomeEnabled(true);
        // actionBar.setIcon(R.drawable.ic_action_bar_icon);

        locationText = (TextView) findViewById(R.id.locationText);
        locationAddressText = (TextView) findViewById(R.id.locationAddressText);
        locationTapButton = (Button) findViewById(R.id.locationTapButton);

        pybGoogleApiClientBuilder = new GoogleLocationApiClientBuilder();

        // First we check play services availability and then we build a client
        if (pybGoogleApiClientBuilder.checkPlayServices(this)) {
            pybGoogleApiClient = pybGoogleApiClientBuilder.buildGoogleApiClient(this);
        }

        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.mapfragment);
        mapFragment.getMapAsync(this);

        // PYB tap button click listener
        locationTapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayLocation();
            }
        });

        dbHelper = new PYBDbHelper(getApplicationContext());

        mHandler = new Handler() {
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case UPDATE_ADDRESS:
                        locationAddressText.setText((String) msg.obj);
                        break;
                }
            }
        };
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        LatLng indiaGate = new LatLng(28.6129,77.2293);

        if (map != null) {
            this.map = map;
        }

        map.setMyLocationEnabled(true);
        map.setOnMyLocationChangeListener(this);
        map.getUiSettings().setZoomControlsEnabled(true);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(indiaGate, 11));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_help:
                showHowItWorks();
                return true;
            case R.id.action_about:
                showAbout();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showAbout() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.about_title)
                .setMessage(R.string.about_message)
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        dialog.cancel();
                    }
                })
                .setIcon(R.drawable.ic_action_bar_icon);
        final AlertDialog alert = builder.create();
        alert.show();
    }

    private void showHowItWorks() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.howitworks_title)
                .setMessage(R.string.howitworks_message)
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        dialog.cancel();
                    }
                })
                .setIcon(R.drawable.ic_action_bar_icon);
        final AlertDialog alert = builder.create();
        alert.show();
    }

    // Method to display location in UI
    private void displayLocation() {
        pybLocation = LocationServices.FusedLocationApi
                .getLastLocation(pybGoogleApiClient);

        LocationRequest tmpLocationRequest = LocationRequest.create();
        tmpLocationRequest.setNumUpdates(1);
        tmpLocationRequest.setInterval(UPDATE_INTERVAL);
        tmpLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        tmpLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        tmpLocationRequest.setSmallestDisplacement(DISPLACEMENT);

        LocationServices.FusedLocationApi.requestLocationUpdates(
                pybGoogleApiClient, tmpLocationRequest, this);

        if (pybLocation != null) {
            double latitude = pybLocation.getLatitude();
            double longitude = pybLocation.getLongitude();

            LatLng latlang = new LatLng(pybLocation.getLatitude(), pybLocation.getLongitude());
            map.moveCamera(CameraUpdateFactory.newLatLng(latlang));

            if (Geocoder.isPresent() && hasNetworkConnection()) {
                new ReverseGeocodingTask().execute(pybLocation);
            }
            locationText.setText(latitude + ", " + longitude);
        } else if (locationManager.isGPSEnabled) {
            locationText.setText("");
        } else {
            Toast.makeText(getApplicationContext(),
                    "Couldn't get your location. Make sure location is enabled on the device",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private class saveLocation extends AsyncTask<Location,Void,Location> {

        private AlertDialog dialog;
        boolean showDialog = false;

        private AlertDialog makeDistanceDialog() {
            String message = "Please place your next bin at least 50m away from previous bins.";
            final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setMessage(message)
                    .setCancelable(true)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                            dialog.cancel();
                        }
                    });
            return builder.create();
        }

        @Override
        protected void onPostExecute(Location location) {
            if (showDialog) {
                dialog = makeDistanceDialog();
                dialog.show();
            } else {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();

                Toast.makeText(getApplicationContext(),
                        "Saving location",
                        Toast.LENGTH_SHORT).show();

                map.addMarker(new MarkerOptions()
                        .position(new LatLng(latitude, longitude))
                        .draggable(false)
                        .title(Double.toString(latitude) + ", " + Double.toString(longitude))
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_action_bar_icon)));
            }
        }

        @Override
        protected Location doInBackground(Location... newLocations) {
            Location location = null;
            if (newLocations.length > 1) {
                location = newLocations[0];
            } else {
                return location;
            }

            if (locations != null && !(locations.isEmpty())) {
                for(Location loc: locations){
                    if (loc.distanceTo(location)<50.0) {
                        showDialog=true;
                        return location;
                    }
                }
            }

            double latitude = location.getLatitude();
            double longitude = location.getLongitude();

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
            locations.add(location);
            return location;
        }
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

    private boolean hasNetworkConnection() {
        boolean hasConnectedWifi = false;
        boolean hasConnectedMobile = false;

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] netInfo = cm.getAllNetworkInfo();
        for (NetworkInfo ni : netInfo) {
            if (ni.getTypeName().equalsIgnoreCase("WIFI"))
                if (ni.isConnected())
                    hasConnectedWifi = true;
            if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
                if (ni.isConnected())
                    hasConnectedMobile = true;
        }
        return hasConnectedWifi || hasConnectedMobile;
    }

    @Override
    protected void onStart() {
        super.onStart();
        locationManager = new PYBLocationManager(this);

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
            LatLng latlang = new LatLng(location.getLatitude(), location.getLongitude());
            map.moveCamera(CameraUpdateFactory.newLatLng(latlang));

            this.pybLocation = location;
            locationText.setText(location.getLatitude() + ", " + location.getLongitude());

            new saveLocation().execute(location,null,location);
            if (Geocoder.isPresent() && hasNetworkConnection()) {
                new ReverseGeocodingTask().execute(location);
            }
        }
    }
    @Override
    public void onMyLocationChange(Location location) {
        if (location != null) {
            this.pybLocation = location;

            LatLng latlang = new LatLng(location.getLatitude(), location.getLongitude());
            map.moveCamera(CameraUpdateFactory.newLatLng(latlang));

            locationText.setText(location.getLatitude() + ", " + location.getLongitude());
            if (Geocoder.isPresent() && hasNetworkConnection()) {
                new ReverseGeocodingTask().execute(location);
            }
        }
    }

    private class ReverseGeocodingTask extends AsyncTask<Location, Void, Void> {

        @Override
        protected Void doInBackground(Location... params) {
            Location location = params[0];

            String humanReadableAddress = null;
            Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
            List<Address> addresses = null;

            try {
                // Call the synchronous getFromLocation() method by passing in the lat/long values.
                addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            } catch (IOException e) {
                e.printStackTrace();
                // Update UI field with the exception.
                Message.obtain(mHandler, UPDATE_ADDRESS, "Waiting for address ...").sendToTarget();
            }
            if (addresses != null && addresses.size() > 0) {
                Address address = addresses.get(0);

                humanReadableAddress = "";
                int maxLines = address.getMaxAddressLineIndex();
                for (int lineNum = 0; lineNum<=maxLines; lineNum++){
                    humanReadableAddress += address.getAddressLine(lineNum);
                    if (lineNum != maxLines) {
                        humanReadableAddress +=  ", ";
                    }
                }
                // Update the UI via a message handler.
                Message.obtain(mHandler, UPDATE_ADDRESS, humanReadableAddress).sendToTarget();
            }
            return null;

        }
    }
}