package com.example.erufe.gmaps;
import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;

import android.location.LocationManager;

import android.os.AsyncTask;
import android.os.Build;

import android.support.annotation.NonNull;

import android.support.v4.app.ActivityCompat;

import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;

import android.view.animation.RotateAnimation;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;

import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;


import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import android.os.Vibrator;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener,
        IBaseGpsListener,
        SensorEventListener {

    private GoogleMap mMap;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    Marker mCurrLocationMarker;
    LocationRequest mLocationRequest;
    ArrayList<LatLng> markerPoints;
    TextView tvDistanceDuration;
    double speed;
    // define the display assembly compass picture
    private ImageView image;
    // record the compass picture angle turned
    private float currentDegree = 0f;
    TextView tvHeading;
    TextView tvDistanceLeft;
    TextView tvTimeLeft;
    // The following are used for the shake detection
    private Sensor mAccelerometer;
    private SensorManager mSensorManager;
    private ShakeDetector mShakeDetector;
    ArrayList<LatLng> shakeMarkers;

    String crrCity="";
    double distanceLeft=0;
    LatLng currentLocation;
    boolean deviceShooked;

    //navigation mode
    String navigMode="mode=walking";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        // Find the toolbar view inside the activity layout
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        fadeIn(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
//        getSupportActionBar().setTitle("new title");

        //ask user for permissions
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkLocationPermission();
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        tvDistanceDuration = (TextView) findViewById(R.id.tv_distance_time);

        // Initializing markers arrays
        markerPoints = new ArrayList<LatLng>();
        shakeMarkers = new ArrayList<LatLng>();

        //Speed
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        this.updateSpeed(null);

        CheckBox chkUseMetricUntis = (CheckBox) this.findViewById(R.id.chkMetricUnits);
        chkUseMetricUntis.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                MainActivity.this.updateSpeed(null);
            }
        });

        //bearing
        image = (ImageView) findViewById(R.id.imageViewCompass);
        // TextView that will tell the user what degree is he heading
        tvHeading = (TextView) findViewById(R.id.tvHeading);
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);


        //distance left until destination
        tvDistanceLeft = (TextView)findViewById(R.id.tvDistanceLeft);
        //time left until destination
        tvTimeLeft = (TextView) findViewById(R.id.tvTimeleft);

        // ShakeDetector initialization
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager
                .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mShakeDetector = new ShakeDetector();
        mShakeDetector.setOnShakeListener(new ShakeDetector.OnShakeListener() {

            @Override
            public void onShake(int count) {

                handleShakeEvent(count);
            }
        });
    }

    //Done:Handle ShakeEvent
    public void handleShakeEvent(int count){
            Toast.makeText(MainActivity.this,"Bumpy road!",Toast.LENGTH_SHORT).show();
            deviceShooked=true;
    }

    //Done:Speed
    public void finish()
    {
        super.finish();
        System.exit(0);
    }
    //for timeleft
    float nCurrentSpeed;
    private void updateSpeed(CLocation location) {
        nCurrentSpeed=0;
        if(location != null)
        {
            location.setUseMetricunits(this.useMetricUnits());
            nCurrentSpeed = location.getSpeed();
        }

        Formatter fmt = new Formatter(new StringBuilder());
        fmt.format(Locale.US, "%5.1f", nCurrentSpeed);
        String strCurrentSpeed = fmt.toString();
        strCurrentSpeed = strCurrentSpeed.replace(' ', '0');

        String strUnits = "km/h";
        if(this.useMetricUnits())
        {
            strUnits = "m/s";
        }

        TextView txtCurrentSpeed = (TextView) this.findViewById(R.id.txtCurrentSpeed);
        txtCurrentSpeed.setText("Speed:" + strCurrentSpeed + " " + strUnits);
    }

    private boolean useMetricUnits() {
        CheckBox chkUseMetricUnits = (CheckBox) this.findViewById(R.id.chkMetricUnits);
        return chkUseMetricUnits.isChecked();
    }

    //EndSpeed
    // Menu icons are inflated just as they were with actionbar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    // Respond to menu item clicks
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.maptypeHYBRID:
                if (mMap != null) {
                    mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                    return true;
                }
            case R.id.maptypeNONE:
                if (mMap != null) {
                    mMap.setMapType(GoogleMap.MAP_TYPE_NONE);
                    return true;
                }
            case R.id.maptypeNORMAL:
                if (mMap != null) {
                    mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                    return true;
                }
            case R.id.maptypeSATELLITE:
                if (mMap != null) {
                    mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                    return true;
                }
            case R.id.maptypeTERRAIN:
                if (mMap != null) {
                    mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                    return true;
                }
            case R.id.about_us:
                aboutUs();
                return true;
            case R.id.Search:
                DialogRoute();
                return true;
            case R.id.Info:
                RouteInfo();
                return true;
            case R.id.driving:
                navigMode="mode=driving";
                DrawMarkersAndRoute();
                dist = CalculationByDistance(latLngS, latLngD);
                return true;
            case R.id.walking:
                navigMode="mode=walking";
                DrawMarkersAndRoute();
                dist = CalculationByDistance(latLngS, latLngD);
                return true;
            case R.id.transit:
                navigMode="mode=transit";
                DrawMarkersAndRoute();
                dist = CalculationByDistance(latLngS, latLngD);
                return true;
            case R.id.cycling:
                navigMode="mode=cycling";
                DrawMarkersAndRoute();
                dist = CalculationByDistance(latLngS, latLngD);
                return true;
            case R.id.recalibrate:
                imLost();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void fadeIn(View view) {
        AlphaAnimation anim = new AlphaAnimation(0.0f, 1.0f);
        anim.setDuration(1500);
        view.startAnimation(anim);
        view.setVisibility(View.VISIBLE);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        //Initialize Google Play Services
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                buildGoogleApiClient();
                mMap.setMyLocationEnabled(true);
            }
        } else {
            buildGoogleApiClient();
            mMap.setMyLocationEnabled(true);
        }

        // Setting onclick event listener for the map
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {

            @Override
            public void onMapClick(LatLng point) {

                // Already two locations
                if (markerPoints.size() > 1) {
                    markerPoints.clear();
                    mMap.clear();
                }

                // Adding new item to the ArrayList
                markerPoints.add(point);

                // Creating MarkerOptions
                MarkerOptions options = new MarkerOptions();

                // Setting the position of the marker
                options.position(point);

                /**
                 * For the start location, the color of marker is GREEN and
                 * for the end location, the color of marker is RED.
                 */
                if (markerPoints.size() == 1) {
                    options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                } else if (markerPoints.size() == 2) {
                    options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                }


                // Add new marker to the Google Map Android API V2
                mMap.addMarker(options);
                LatLng origin = new LatLng(0, 0);
                LatLng dest = new LatLng(0, 0);
                // Checks, whether start and end locations are captured
                if (markerPoints.size() >= 2) {
                    origin = markerPoints.get(0);
                    dest = markerPoints.get(1);

                    // Getting URL to the Google Directions API
                    String url = getUrl(origin, dest);
                    Log.d("onMapClick", url.toString());
                    FetchUrl FetchUrl = new FetchUrl();

                    // Start downloading json data from Google Directions API
                    FetchUrl.execute(url);
                    //move map camera
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(origin));
                    mMap.animateCamera(CameraUpdateFactory.zoomTo(13));
                }
                double dist = CalculationByDistance(origin, dest);
//                Toast.makeText(MainActivity.this, "Distance: " + String.format("%.2f", dist) + " km", Toast.LENGTH_SHORT).show();
            }
        });
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }



    @Override
    public void onLocationChanged(Location location) {

        mLastLocation = location;

        //Place current location marker
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        if (mCurrLocationMarker == null) {
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(latLng);
            markerOptions.title("You are here!");
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
            mCurrLocationMarker = mMap.addMarker(markerOptions);
        }
        //shake event
        if(deviceShooked) {
            currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
            shakeMarkers.add(currentLocation);
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(currentLocation);
            markerOptions.title("Bumpy road!");
            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_bump));
            mMap.addMarker(markerOptions);
        }
        deviceShooked=false;

        //rotate arrow into direction you need to go
        Location temp = new Location(LocationManager.GPS_PROVIDER);
        if(latLngD != null) {
            temp.setLatitude(latLngD.latitude);
            temp.setLongitude(latLngD.longitude);
        }
        else{
            temp.setLongitude(0);
            temp.setLongitude(0);
        }

//        float bearing = mLastLocation.bearingTo(temp);//implement
//        MarkerOptions markerOptions = new MarkerOptions();
//        markerOptions.position(latLng);
//        markerOptions.title("You are here!");
//        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_arrow));
//        markerOptions.anchor(0.5f,0.5f);
//        markerOptions.rotation(bearing);
//        markerOptions.flat(true);
//        mCurrLocationMarker = mMap.addMarker(markerOptions);

        //move map camera
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(18));

        //stop location updates
        if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }

        //getCurrentSpeed
        speed= getspeed(location);

        //speed
        if(location != null)
        {
            CLocation myLocation = new CLocation(location, this.useMetricUnits());
            this.updateSpeed(myLocation);
        }

        //getCurrentLocation
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        double MyLat = mLastLocation.getLatitude();
        double MyLong = mLastLocation.getLongitude();
        try {
            List<Address> addresses = geocoder.getFromLocation(MyLat, MyLong, 1);
            crrCity= addresses.get(0).getAddressLine(0);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //updateCamera
        updateCameraBearing(mMap, location.getBearing());

        //getDistanceLeft
        LatLng crrLoc = new LatLng(location.getLatitude(),location.getLongitude());

        if(latLngD!=null)
            distanceLeft = CalculationByDistance(crrLoc, latLngD);
//        Toast.makeText(MainActivity.this, String.format("Distance left: %.2f",distanceLeft ), Toast.LENGTH_SHORT).show();
        tvDistanceLeft.setText(String.format("Distance: %.2f km",distanceLeft ));

        //get
        float timeleft = TimeLeft();
        tvTimeLeft.setText(String.format("Time left: %.2f mins",timeleft));

        //when user is close to destination
        if(latLngD!=null) {
            Location youThere = new Location(LocationManager.GPS_PROVIDER);
            youThere.setLongitude(latLngD.longitude);
            youThere.setLatitude(latLngD.latitude);
            float d = mLastLocation.distanceTo(youThere);//  in meters
            if (d < 50) {
                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                // Vibrate for 1000 milliseconds
                v.vibrate(1000);
                Toast.makeText(MainActivity.this, "You are close to your destination", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onGpsStatusChanged(int event) {

    }

    @Override
    public void onConnected(Bundle bundle) {

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(100);
        mLocationRequest.setFastestInterval(100);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    public boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted. Do the
                    // contacts-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient();
                        }
                        mMap.setMyLocationEnabled(true);
                    }

                } else {

                    // Permission denied, Disable the functionality that depends on this permission.
                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }

    //Dialog for the route
    String sourceL;
    String destL;
    double dist;
    public void DialogRoute() {
        //create custom LinearLayout programmatically
        LinearLayout layout = new LinearLayout(MainActivity.this);
        layout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        layout.setOrientation(LinearLayout.VERTICAL);

        //draw the source EditText
        final EditText sourceLocation = new EditText(MainActivity.this);
        sourceLocation.setInputType(InputType.TYPE_CLASS_TEXT);
        sourceLocation.setHint("Current location");

        //populate first line with current location
//        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
//        double MyLat = mLastLocation.getLatitude();
//        double MyLong = mLastLocation.getLongitude();
//        String cityName = "";
//        try {
//            List<Address> addresses = geocoder.getFromLocation(MyLat, MyLong, 1);
//            cityName = addresses.get(0).getAddressLine(0);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        sourceLocation.setText(cityName);

        //draw the destination EditText
        final EditText destinationLocation = new EditText(MainActivity.this);
        destinationLocation.setHint("Destination");
        destinationLocation.setInputType(InputType.TYPE_CLASS_TEXT);

        layout.addView(sourceLocation);
        layout.addView(destinationLocation);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Find route");
        builder.setView(layout);
        AlertDialog alertDialog = builder.create();

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
//                boolean parsable = true;
//                Double lat = null, lon = null;
                sourceL = sourceLocation.getText().toString();
                destL = destinationLocation.getText().toString();

                //Done:draw stuff on the canvas
                DrawMarkersAndRoute();
                dist = CalculationByDistance(latLngS, latLngD);
//                Toast.makeText(MainActivity.this, "Distance: " + String.format("%.2f", dist) + " km", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);

        builder.show();
    }

    LatLng latLngD, latLngS;

    void DrawMarkersAndRoute() {
        //avoid drawing more then 2 markers
        if (markerPoints.size() > 1) {
            markerPoints.clear();
            mMap.clear();
        }

        List<Address> sourceList = null;
        List<Address> destList = null;
        if (!sourceL.equals("")) {
            Geocoder geocoder = new Geocoder(this);
            try {
                sourceList = geocoder.getFromLocationName(sourceL, 1);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else{
            Geocoder geocoder = new Geocoder(this);
            try{
                sourceList = geocoder.getFromLocation(mLastLocation.getLatitude(),mLastLocation.getLongitude(),1);
            }catch (IOException e){
                e.printStackTrace();
            }
        }

        if (!destL.equals("")) {
            Geocoder geocoder = new Geocoder(this);
            try {
                destList = geocoder.getFromLocationName(destL, 1);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else
        {
            Geocoder geocoder = new Geocoder(this);
            try {
                destList = geocoder.getFromLocation(mLastLocation.getLatitude(),mLastLocation.getLongitude(), 1);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Address sourceAdd = sourceList.get(0);
        Address destAdd = destList.get(0);
        latLngS = new LatLng(sourceAdd.getLatitude(), sourceAdd.getLongitude());
        markerPoints.add(latLngS);
        latLngD = new LatLng(destAdd.getLatitude(), destAdd.getLongitude());
        markerPoints.add(latLngD);
        MarkerOptions mo = new MarkerOptions();
        mo.position(latLngS);
        MarkerOptions mo1 = new MarkerOptions();
        mo1.position(latLngD);
        if (markerPoints.size() == 1) {
            mo.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
            mo1.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
        } else if (markerPoints.size() == 2) {
            mo.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
            mo1.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
        }
        mMap.addMarker(mo);
        mMap.addMarker(mo1);

        // Checks, whether start and end locations are captured
        if (markerPoints.size() >= 2) {
            LatLng origin = latLngS;
            LatLng dest = latLngD;

            // Getting URL to the Google Directions API
            String url = getUrl(origin, dest);
            Log.d("onMapClick", url.toString());
            FetchUrl FetchUrl = new FetchUrl();

            // Start downloading json data from Google Directions API
            FetchUrl.execute(url);
            //move map camera
            mMap.moveCamera(CameraUpdateFactory.newLatLng(origin));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(13));

        }
    }

    //DONE:calculate distance with accuracy;
    double meter;
    public double CalculationByDistance(LatLng StartP, LatLng EndP) {
        int Radius = 6371;// radius of earth in Km
        double lat1 = StartP.latitude;
        double lat2 = EndP.latitude;
        double lon1 = StartP.longitude;
        double lon2 = EndP.longitude;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2)
                * Math.sin(dLon / 2);
        double c = 2 * Math.asin(Math.sqrt(a));
        double valueResult = Radius * c;
        double km = valueResult / 1;
        DecimalFormat newFormat = new DecimalFormat("####");
        int kmInDec = Integer.valueOf(newFormat.format(km));
        meter = valueResult % 1000;
        int meterInDec = Integer.valueOf(newFormat.format(meter));
        Log.i("Radius Value", "" + valueResult + "   KM  " + kmInDec
                + " Meter   " + meterInDec);
        return Radius * c;
    }


    private String getUrl(LatLng origin, LatLng dest) {

        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;
        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;
        //Sensor
        String sensor = "sensor=false";
        //Get units
        String units = "units=metric";
        //Get mode
        String mode = navigMode;
        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + sensor + "&"+units+"&"+mode;
        // Output format
        String output = "json";
        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;
        return url;
    }

    //download JSON from url
    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();
            Log.d("downloadUrl", data.toString());
            br.close();

        } catch (Exception e) {
            Log.d("Exception", e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }


    // Fetches data from url passed
    private class FetchUrl extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... url) {

            // For storing data from web service
            String data = "";

            try {
                // Fetching the data from web service
                data = downloadUrl(url[0]);
                Log.d("Background Task data", data.toString());
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();

            // Invokes the thread for parsing the JSON data
            parserTask.execute(result);

        }
    }
    String duration = "";
    //Parser class
    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try {
                jObject = new JSONObject(jsonData[0]);
                Log.d("ParserTask", jsonData[0].toString());
                DataParser parser = new DataParser();
                Log.d("ParserTask", parser.toString());

                // Starts parsing data
                routes = parser.parse(jObject);
                Log.d("ParserTask", "Executing routes");
                Log.d("ParserTask", routes.toString());

            } catch (Exception e) {
                Log.d("ParserTask", e.toString());
                e.printStackTrace();
            }
            return routes;
        }

        // Executes in UI thread, after the parsing process
        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList<LatLng> points = null;
            PolylineOptions lineOptions = null;
            MarkerOptions markerOptions = new MarkerOptions();
            String distance = "";

            if (result.size() < 1) {
                Toast.makeText(getBaseContext(), "No Points", Toast.LENGTH_SHORT).show();
                return;
            }

            // Traversing through all the routes
            for (int i = 0; i < result.size(); i++) {
                points = new ArrayList<LatLng>();
                lineOptions = new PolylineOptions();

                // Fetching i-th route
                List<HashMap<String, String>> path = result.get(i);

                // Fetching all the points in i-th route
                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);

                    if (j == 0) {    // Get distance from the list
                        distance = (String) point.get("distance");
                        continue;
                    } else if (j == 1) { // Get duration from the list
                        duration = (String) point.get("duration");
                        continue;
                    }

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                // Adding all the points in the route to LineOptions
                lineOptions.addAll(points);
                lineOptions.width(25);
                lineOptions.color(Color.BLUE);//edit this
            }
//            Toast.makeText(MainActivity.this, "Duration time: " + duration + "\nDistance: " + distance, Toast.LENGTH_LONG).show();
            // Drawing polyline in the Google Map for the i-th route
            mMap.addPolyline(lineOptions);

        }
    }

    //DONE:about us section
    void aboutUs() {

        AlertDialog.Builder aboutDialogBuilder =
                new AlertDialog.Builder(MainActivity.this);
        aboutDialogBuilder.setTitle("About Us")
                .setMessage("SDBIS Team 2017");
        aboutDialogBuilder.setNegativeButton("Got it",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        AlertDialog aboutDialog = aboutDialogBuilder.create();
        aboutDialog.show();
    }
    //DONE:Route Info
    void RouteInfo(){
        LinearLayout layout = new LinearLayout(MainActivity.this);
        layout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        layout.setOrientation(LinearLayout.VERTICAL);

        //draw the source EditText
        final TextView whereTo = new TextView(MainActivity.this);
        whereTo.setText("\n\nFrom " +sourceL + " to " + destL);
        final TextView currL = new TextView(MainActivity.this);
        currL.setText("Your current location is: " + crrCity);
        final TextView Getspeed = new TextView(MainActivity.this);
        Getspeed .setText("Current speed: " + String.format("%.2f m/s",nCurrentSpeed));
        final TextView distance = new TextView(MainActivity.this);
        distance.setText("Distance until arrival: " + String.format("%.2f km",dist));
        final TextView time = new TextView(MainActivity.this);
        time.setText("Time until arrival: " +duration );
        //populate first line with current location

        layout.addView(whereTo);
        layout.addView(currL);
        layout.addView(Getspeed );
        layout.addView(distance);
        layout.addView(time);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Route information");
        builder.setView(layout);
        AlertDialog alertDialog = builder.create();

        builder.setPositiveButton("Got it", null);
        builder.show();
    }

    //DONE: Get speed
    double curTime= 0;
    double oldLat = 0.0;
    double oldLon = 0.0;

    private double getspeed(Location location){
        double newTime= System.currentTimeMillis();
        double newLat = location.getLatitude();
        double newLon = location.getLongitude();
        double speed=0.0f;
        if(location.hasSpeed()){
            speed = location.getSpeed();
//            Toast.makeText(getApplication(),"SPEED : "+String.format("%.2f m/s",speed),Toast.LENGTH_SHORT).show();
        } else {
            double distance = calculationBydistance(newLat,newLon,oldLat,oldLon);
            double timeDifferent = newTime - curTime;
            speed = distance/timeDifferent;
            curTime = newTime;
            oldLat = newLat;
            oldLon = newLon;
//            Toast.makeText(getApplication(),"SPEED : "+String.format("%.2f m/s",speed),Toast.LENGTH_SHORT).show();
        }
        return speed;
    }

    public double calculationBydistance(double lat1, double lon1, double lat2, double lon2){
        double radius = 6371;
        double dLat = Math.toRadians(lat2-lat1);
        double dLon = Math.toRadians(lon2-lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.asin(Math.sqrt(a));
        return radius * c;
    }

    //DONE:User's azimuth
    @Override
    protected void onResume() {
        super.onResume();
        // for the system's orientation sensor registered listeners
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(mShakeDetector, mAccelerometer,	SensorManager.SENSOR_DELAY_UI);
    }
    @Override
    protected void onPause() {
        mSensorManager.unregisterListener(mShakeDetector);
        super.onPause();
        // to stop the listener and save battery
        mSensorManager.unregisterListener(this);

    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
// get the angle around the z-axis rotated
        float degree = Math.round(sensorEvent.values[0]);
//        tvHeading.setText("Heading: " + Float.toString(degree) + " degrees");
        // create a rotation animation (reverse turn degree degrees)
        RotateAnimation ra = new RotateAnimation(
                currentDegree,
                        -degree,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF,
        0.5f);
        // how long the animation will take place
        ra.setDuration(210);
        // set the animation after the end of the reservation status
        ra.setFillAfter(true);
        // Start the animation
        image.startAnimation(ra);
        currentDegree = -degree;

    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        //TODO:what to do here?
    }

    private void updateCameraBearing(GoogleMap googleMap, float bearing) {
        if ( googleMap == null) return;
        CameraPosition camPos = CameraPosition
                .builder(
                        googleMap.getCameraPosition() // current Camera
                )
                .bearing(bearing)
                .build();
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(camPos));
    }
    
    //TODO: get time left until arriving
    //// TODO: 21.05.2017  test this function
    public float TimeLeft(){
        //time = dist/speed
//        float timeLeft = (float) (distanceLeft/nCurrentSpeed)/60;
          float timeLeft = (float) ((distanceLeft*1000)/nCurrentSpeed)/60;
        timeLeft=Math.abs(timeLeft);
//        Toast.makeText(MainActivity.this,String.format("%.2f",timeLeft),Toast.LENGTH_SHORT).show();
        return timeLeft;
    }
    private void imLost()
    {
        DrawMarkersAndRoute();
        LatLng mLast = new LatLng(mLastLocation.getLatitude(),mLastLocation.getLongitude());
        dist = CalculationByDistance(mLast, latLngD);
    }
}

