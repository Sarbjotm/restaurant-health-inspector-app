package com.example.restauranthealthinspector;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.Toast;


import com.example.restauranthealthinspector.model.Inspection;
import com.example.restauranthealthinspector.model.Restaurant;
import com.example.restauranthealthinspector.model.RestaurantsManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_GREEN;
import static com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_ORANGE;
import static com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_RED;


public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private static final String TAG = "MapsActivity";
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final float DEFAULT_ZOOM = 15f;
    private boolean resume = false;

    private Boolean mLocationPermissionsGranted = false;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private RestaurantsManager myRestaurants;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private ClusterManager<ClusterPin> mClusterManger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        getLocationPermission();
        try {
            populateRestaurants();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void pinRestaurants(){

        //new code
        initClusterManager();

        for (Restaurant restaurant : myRestaurants) {

            mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter(MapsActivity.this));
            double lat = restaurant.getAddress().getLatitude();
            double lng = restaurant.getAddress().getLongitude();
            final LatLng latLng = new LatLng(lat, lng);
            String name = restaurant.getRestaurantName();
            String address = restaurant.getAddress().getStreetAddress();
            ArrayList<Inspection> inspections = restaurant.getInspectionsManager().getInspectionList();
            String hazardLevel = "No inspections recorded";
            if (inspections.size() != 0) {
                Inspection inspection = inspections.get(0);
                hazardLevel = inspection.getHazardRating();
            }
            String snippet = name + "\n" + address + "\nhazard lv : " + hazardLevel;
            float colour;
            if (hazardLevel.equals("High")) {
                colour = HUE_RED;
            } else if (hazardLevel.equals("Moderate")) {
                colour = HUE_ORANGE;
            } else {
                colour = HUE_GREEN;
            }


            MarkerOptions options = new MarkerOptions().position(latLng).title(name).snippet(snippet).icon(BitmapDescriptorFactory.defaultMarker(colour));
            Marker mMarker = mMap.addMarker(options
                .title(name)
                .snippet(address + "\n" + hazardLevel));

            mMarker.setVisible(false);


            //new code
            mClusterManger.addItem(new ClusterPin(name, snippet, latLng,1));
        }

        mClusterManger.cluster();
    }

    public void initClusterManager(){
        mClusterManger = new ClusterManager<ClusterPin>(this,mMap);

        CustomClusterRenderer renderer = new CustomClusterRenderer(this, mMap, mClusterManger);
        mClusterManger.setRenderer(renderer);

        mClusterManger.setOnClusterClickListener(new ClusterManager.OnClusterClickListener<ClusterPin>() {
            @Override
            public boolean onClusterClick(Cluster<ClusterPin> cluster) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(cluster.getPosition(),100.0f));
                Toast.makeText(MapsActivity.this, "Cluster click", Toast.LENGTH_SHORT).show();
                return false;
            }
        });

        mClusterManger.setOnClusterItemClickListener(new ClusterManager.OnClusterItemClickListener<ClusterPin>() {
            @Override
            public boolean onClusterItemClick(ClusterPin clusterItem) {
                Toast.makeText(MapsActivity.this, "Cluster item click", Toast.LENGTH_SHORT).show();
                return false;
            }
        });
        mClusterManger.getMarkerCollection().setInfoWindowAdapter(new CustomInfoViewAdapter(LayoutInflater.from(MapsActivity.this)));

        mClusterManger.setOnClusterItemInfoWindowClickListener(new ClusterManager.OnClusterItemInfoWindowClickListener<ClusterPin>() {
            @Override
            public void onClusterItemInfoWindowClick(ClusterPin clusterItem) {
                Toast.makeText(MapsActivity.this, "Clicked info window: " + clusterItem.getTitle(), Toast.LENGTH_SHORT).show();
            }
        });

        mMap.setOnCameraIdleListener(mClusterManger);
        mMap.setOnMarkerClickListener(mClusterManger);
        mMap.setInfoWindowAdapter(mClusterManger.getMarkerManager());
        mMap.setOnInfoWindowClickListener(mClusterManger);
    }



    private void getDeviceLocation() {
        Log.d(TAG, "getDeviceLocation: getting the devices current location");
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        try {
            if (mLocationPermissionsGranted) {
                final Task location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "onComplete: found location!");
                            Location currentLocation = (Location) task.getResult();
                            double lat = currentLocation.getLatitude();
                            double lng = currentLocation.getLongitude();
                            LatLng latLng = new LatLng(lat, lng);
                            moveCamera(latLng, DEFAULT_ZOOM);
                        } else {
                            Log.d(TAG, "onComplete: current location is null");
                            Toast.makeText(MapsActivity.this, "unable to get current location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        } catch (SecurityException e) {
            Log.e(TAG, "getDeviceLocation: SecurityException: " + e.getMessage());
        }
    }

    private void moveCamera(LatLng latLng, float zoom) {
        Log.d(TAG, "moveCamera: moving the camera to: lat: " + latLng.latitude + "lng: " + latLng.longitude);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
    }

    private void initMap() {
        Log.d(TAG, "initMap: initializing map");
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    private void populateRestaurants() throws IOException {
        InputStream inputRestaurant = getResources().openRawResource(R.raw.restaurants_itr1);
        BufferedReader readerRestaurants = new BufferedReader(
                new InputStreamReader(inputRestaurant, StandardCharsets.UTF_8)
        );

        InputStream inputInspections = getResources().openRawResource(R.raw.inspectionreports_itr1);
        BufferedReader readerInspections = new BufferedReader(
                new InputStreamReader(inputInspections, StandardCharsets.UTF_8)
        );

        myRestaurants = RestaurantsManager.getInstance(readerRestaurants, readerInspections);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Toast.makeText(this, "Map is Ready", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "onMapReady: map is ready");
        mMap = googleMap;
        if (mLocationPermissionsGranted) {
            getDeviceLocation();
            if (ActivityCompat.checkSelfPermission(
                    this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mMap.setMyLocationEnabled(true);

            pinRestaurants();
        }
    }

    private void getLocationPermission(){
        Log.d(TAG, "getLocationPermission: getting location permissions");
        String[] permissions = {FINE_LOCATION, COARSE_LOCATION};

        if(ContextCompat.checkSelfPermission(this.getApplicationContext(),
                FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            if(ContextCompat.checkSelfPermission(this.getApplicationContext(),
                    COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                mLocationPermissionsGranted = true;
                initMap();
            }
            else{
                ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
            }
        }
        else{
            ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED){

            locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    //get the location name from latitude and longitude
                    Geocoder geocoder = new Geocoder(getApplicationContext());
                    try {
                        List<Address> addresses =
                                geocoder.getFromLocation(latitude, longitude, 1);
                        LatLng latLng = new LatLng(latitude, longitude);

                        mMap.setMaxZoomPreference(20);

                        if(resume == false){
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15.0f));
                            resume = true;
                        }


                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {

                }

                @Override
                public void onProviderEnabled(String provider) {

                }

                @Override
                public void onProviderDisabled(String provider) {

                }
            };
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        locationManager.removeUpdates(locationListener);
    }
}