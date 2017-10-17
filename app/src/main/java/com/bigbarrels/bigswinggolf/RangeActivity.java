package com.bigbarrels.bigswinggolf;

import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.maps.android.SphericalUtil;

import java.util.ArrayList;
import java.util.List;

public class RangeActivity extends FragmentActivity implements
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnMapClickListener,
        OnMapReadyCallback
{
    private static final String TAG = RangeActivity.class.getSimpleName();
    private GoogleMap mMap;
    private CameraPosition mCameraPosition;

    // Entry point point for fused location provider and device's current/last known location received
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private Location mLastKnownLocation;

    // Keys for storing activity state.
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";

    // Default location (Big Swing, center stall) to be used when location permission not granted.
    //private Marker mDefaultLocation;
    private static final LatLng mDefaultLocation = new LatLng(39.767274, -75.111720);
    private static final int DEFAULT_ZOOM = 17;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean mLocationPermissionGranted;

    // List to hold the custom target markers
    private List<Marker> mCustomFlags = new ArrayList<>();

    // Target locations by color
    private static final LatLng BLACK = new LatLng(39.767766, -75.111540);
    private static final LatLng ORANGE = new LatLng(39.767847, -75.111071);
    private static final LatLng RED = new LatLng(39.768276, -75.110948);
    private static final LatLng GREEN = new LatLng(39.768355, -75.111921);
    private static final LatLng BLUE = new LatLng(39.768913, -75.111594);
    private static final LatLng YELLOW = new LatLng(39.768602, -75.111228);

    // Driving Range boundaries
    private static final LatLngBounds DRIVING_RANGE = new LatLngBounds(
            new LatLng(39.766414, -75.113393), new LatLng(39.770170, -75.109622));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve location and camera position from saved instance state.
        if (savedInstanceState != null) {
            mLastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            mCameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
        }

        setContentView(R.layout.activity_range);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // For ad initialization.
        MobileAds.initialize(this, "ca-app-pub-3940256099942544~3347511713");
        AdView mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
    }

    /**
     * Saves the state of the map when the activity is paused.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mMap != null) {
            outState.putParcelable(KEY_CAMERA_POSITION, mMap.getCameraPosition());
            outState.putParcelable(KEY_LOCATION, mLastKnownLocation);
            super.onSaveInstanceState(outState);
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        mMap = googleMap;

        // Prompt the user for permission.
        getLocationPermission();

        // Set default map controls
        mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        mMap.setLatLngBoundsForCameraTarget(DRIVING_RANGE);
        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.getUiSettings().setZoomControlsEnabled(true);

        // Turn on the My Location layer and the related control on the map.
        updateLocationUI();

        // Get the current location of the device and set the position of the map.
        getDeviceLocation();

        // Set min zoom level
        //mMap.setMinZoomPreference(17.0f);

        // Enable/Disable map controls.
        /*UiSettings mUiSettings = mMap.getUiSettings();
        mUiSettings.setMapToolbarEnabled(false);
        mUiSettings.setZoomControlsEnabled(true);
        mUiSettings.setMyLocationButtonEnabled(true);
        mUiSettings.setCompassEnabled(true);*/

        // Add default location marker
        //mBigSwing = mMap.addMarker(new MarkerOptions().position(BIG_SWING).title("Me"));

        // Add target markers
        mMap.addMarker(new MarkerOptions()
                 .position(BLACK)
                 .icon(BitmapDescriptorFactory.fromResource(R.drawable.blackflag)));
        mMap.addMarker(new MarkerOptions()
                .position(ORANGE)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.orangeflag)));
        mMap.addMarker(new MarkerOptions()
                .position(RED)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.redflag)));
        mMap.addMarker(new MarkerOptions()
                .position(GREEN)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.greenflag)));
        mMap.addMarker(new MarkerOptions()
                .position(BLUE)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.blueflag)));
        mMap.addMarker(new MarkerOptions()
                .position(YELLOW)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.yellowflag)));

        // Center screen on your location
        //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(BIG_SWING, 19));

        // Set a listener for marker click map click.
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapClickListener(this);
    }

    /**
     * Called when a user clicks a marker.
     */
    @Override
    public boolean onMarkerClick(Marker marker)
    {
        // Set the title of the marker
        marker.setTitle(getDistance(marker.getPosition()) + " yds");

        return false;
    }

    /**
     * Called when a user clicks an empty spot on the map.
     */
    @Override
    public void onMapClick(LatLng latLng)
    {
        // Check to see if user is picking a target on the range and not, lets say, the parking lot
        if (latLng.latitude < 39.767200)
        {
            Toast.makeText(this, "Target must be on the range.", Toast.LENGTH_SHORT).show();
        }
        // Target must be 1<=x<=500
        else if (getDistance(latLng) < 1)
        {
            Toast.makeText(this, "Minimum target distance is 1 yard.", Toast.LENGTH_SHORT).show();
        }
        else if (getDistance(latLng) > 500)
        {
            Toast.makeText(this, "Maximum target distance is 600 yards.", Toast.LENGTH_SHORT).show();
        }
        else
        {
            // Add a temporary marker for a user's custom target.
            Marker mCustomFlag = mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.customflag)));
            mCustomFlags.add(mCustomFlag);
        }
    }

    /**
     * Called when the user clicks "Remove Flags." Clears custom markers.
     */
    public void removeFlagsClicked(View view)
    {
        // Loop through each custom marker
        for (Marker marker : mCustomFlags)
        {
            marker.remove();
        }

        // Remove custom markers from list
        mCustomFlags.clear();
    }

    /**
     * Prompts the user for permission to use the device location.
     */
    private void getLocationPermission()
    {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED)
        {
            mLocationPermissionGranted = true;
        }
        else
        {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    /**
     * Handles the result of the request for location permissions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults)
    {
        mLocationPermissionGranted = false;
        switch (requestCode)
        {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION:
            {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    mLocationPermissionGranted = true;
                }
            }
        }
        updateLocationUI();
    }

    /**
     * Updates the map's UI settings based on whether the user has granted location permission.
     */
    private void updateLocationUI()
    {
        if (mMap == null)
        {
            return;
        }

        try
        {
            if (mLocationPermissionGranted)
            {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
                mMap.getUiSettings().setCompassEnabled(true);
            }
            else
            {
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                mMap.getUiSettings().setCompassEnabled(false);
                mLastKnownLocation = null;
                getLocationPermission();
            }
        }
        catch (SecurityException e)
        {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    /**
     * Gets the current location of the device, and positions the map's camera.
     */
    private void getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            if (mLocationPermissionGranted) {
                Task<Location> locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            mLastKnownLocation = task.getResult();

                            // Check to see if device location is within the map bounds.
                            // If not then close the app.
                            /*LatLng mCoordinates = new LatLng(
                                    mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude());
                            if (!DRIVING_RANGE.contains(mCoordinates)) {
                                new AlertDialog.Builder(RangeActivity.this)
                                        .setTitle("Location Error")
                                        .setMessage("You are not at Big Swing Golf Center.")
                                        .setPositiveButton("Close", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                finish();
                                            }
                                        })
                                        .show();
                            }*/

                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                    new LatLng(mLastKnownLocation.getLatitude(),
                                            mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                        } else {
                            Log.d(TAG, "Current location is null. Using defaults.");
                            Log.e(TAG, "Exception: %s", task.getException());
                            mMap.moveCamera(CameraUpdateFactory
                                    .newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM));
                            mMap.addMarker(new MarkerOptions().position(mDefaultLocation));
                            mMap.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                    }
                });
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    /**
     * Gets the target distance.
     */
    private int getDistance(LatLng latLng)
    {
        // Gets the distance between your location and the target in meters(m)
        double distance = SphericalUtil.computeDistanceBetween(mDefaultLocation, latLng);
        // Convert distance to yards(yds)
        return (int)Math.round(distance / 0.9144);
    }
}
