package com.example.android.camera2basic;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.example.android.camera2basic.bluetooth.BluetoothClient;
import com.example.android.camera2basic.callback.CameraCallBackListener;
import com.example.android.camera2basic.callback.NavigateCallBackListener;
import com.example.android.camera2basic.location.AndroidLocationProvider;
import com.example.android.camera2basic.ui.beaconview.pathplanning.BeaconNavigateFragment;
import com.example.android.camera2basic.util.GlobalConstants;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.nexenio.bleindoorpositioning.location.Location;

import org.opencv.android.OpenCVLoader;

import java.util.ArrayList;
import java.util.Collections;

public class PathPlanActivity extends AppCompatActivity implements CameraCallBackListener, NavigateCallBackListener {

    private static final String TAG = "beacon";

    private FrameLayout mFrameLayout;
    public static final int REQUEST_CODE_LOCATION_PERMISSIONS = 1;
    public static final int REQUEST_CODE_LOCATION_SETTINGS = 2;

    static {
        if (!OpenCVLoader.initDebug()) {
            Log.d("pahPlanning", "Unable to load OpenCV");
        } else {
            System.loadLibrary("localize-lib");
            System.loadLibrary("trilateration-lib");
            System.loadLibrary("pathplan-lib");
            System.loadLibrary("deblur-lib");
            System.loadLibrary("opencv_java3");

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_path_plan);
        Log.d(TAG, "App started");
        Log.d(TAG, BluetoothClient.class.getSimpleName());
        // setup UI
        mFrameLayout = findViewById(R.id.container);
        /*bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setOnNavigationItemSelectedListener(this);
        bottomNavigationView.setSelectedItemId(R.id.navigation_radar);*/
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        GlobalConstants.REAL_SCR_HEIGHT = displayMetrics.heightPixels;
        GlobalConstants.REAL_SCR_WIDTH = displayMetrics.widthPixels;
        float density = displayMetrics.densityDpi;
        Log.e("Density", " densityDpi: " + density + " W: " + GlobalConstants.REAL_SCR_WIDTH + " H: " + GlobalConstants.REAL_SCR_HEIGHT);
        // setup locationc
        //AndroidLocationProvider.initialize(this);
        setupLocationService();

        // setup bluetooth
        BluetoothClient.initialize(this);

        // GetLocation ();
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 400);

        getSupportFragmentManager().beginTransaction().replace(R.id.container, new BeaconNavigateFragment()).commit();
        GlobalConstants.MODE = "navigateFragment";
    }

    private void GetLocation() {
        // Step 1: Outdoor reference location
        double measuredLatitude = 10.019895668819222;
        double measuredLongitude = 76.35076547277124;
        Location outdoorReferenceLocation = new Location(measuredLatitude, measuredLongitude);

// Step 2: Indoor reference or beacon location
        double distance = 7.05; // in meters
        double angle = 30; // in degrees (0° is North)
        Location indoorReferenceLocation = outdoorReferenceLocation.getShiftedLocation(distance, angle);
        Log.d("beacon", "co-ordinates are :" + indoorReferenceLocation); // print latitude and longitude
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_filter:
                Log.d(TAG, "BeaconFilter");
                return true;
            default:
                break;
        }
        return false;
    }


    @Override
    protected void onResume() {
        super.onResume();

        if (GlobalConstants.MODE.equalsIgnoreCase("navigateFragment")) {
            // observe location
            if (!hasLocationPermission(this)) {
                requestLocationPermission(this);
            } else if (!isLocationEnabled(this)) {
                requestLocationServices();
            }
            //AndroidLocationProvider.startRequestingLocationUpdates();
            //AndroidLocationProvider.requestLastKnownLocation();
            // observe bluetooth
            if (!BluetoothClient.isBluetoothEnabled()) {
                requestBluetooth();
            }
            BluetoothClient.startScanning();
        }
        if (GlobalConstants.MODE.equalsIgnoreCase("cameraFragment")) {
            //AndroidLocationProvider.stopRequestingLocationUpdates();
            Log.d(TAG, "on pause to stop scan");
            // stop observing bluetooth
            BluetoothClient.stopScanning();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, Camera2BasicFragment.newInstance())
                    .commit();
        }
    }

    @Override
    protected void onPause() {
        // stop observing location
        //AndroidLocationProvider.stopRequestingLocationUpdates();
        Log.d(TAG, "on pause to stop scan");
        // stop observing bluetooth
        BluetoothClient.stopScanning();

        super.onPause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            /*case AndroidLocationProvider.REQUEST_CODE_LOCATION_PERMISSIONS: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Location permission granted");
                    AndroidLocationProvider.startRequestingLocationUpdates();
                } else {
                    Log.d(TAG, "Location permission not granted. Wut?");
                }
                break;
            }*/
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case BluetoothClient.REQUEST_CODE_ENABLE_BLUETOOTH: {
                if (resultCode == RESULT_OK) {
                    Log.d(TAG, "Bluetooth enabled, starting to scan");
                    BluetoothClient.startScanning();
                } else {
                    Log.d(TAG, "Bluetooth not enabled, invoking new request");
                    BluetoothClient.requestBluetoothEnabling(this);
                }
                break;
            }
        }
    }

    private void requestLocationServices() {
        Snackbar snackbar = Snackbar.make(
                mFrameLayout,
                R.string.error_location_disabled,
                Snackbar.LENGTH_INDEFINITE
        );
        snackbar.setAction(R.string.action_enable, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //AndroidLocationProvider.requestLocationEnabling(PathPlanActivity.this);
            }
        });
        snackbar.show();
    }

    private void requestBluetooth() {
        Snackbar snackbar = Snackbar.make(
                mFrameLayout,
                R.string.error_bluetooth_disabled,
                Snackbar.LENGTH_INDEFINITE
        );
        snackbar.setAction(R.string.action_enable, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BluetoothClient.requestBluetoothEnabling(PathPlanActivity.this);
            }
        });
        snackbar.show();
    }

    public void findNearestPixel() {
        int[] input = new int[]{76, 76};
        double[][] position_list = new double[][]{{50, 50}, {100, 100}, {0, 0}};
        ArrayList<Double> distances = new ArrayList<>();
        for (double[] pos : position_list) {
            distances.add(Math.sqrt(Math.pow((pos[0] - input[0]), 2) + Math.pow((pos[1] - input[1]), 2)));
        }
        int indexOfMinimum = distances.indexOf(Collections.min(distances));
        Log.d("beacon", "nearest co-ordinate at =" + indexOfMinimum);
        Log.d("beacon", "distance =" + distances.get(indexOfMinimum));

    }

    @Override
    public void onCameraCallBack() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag("cameraFragment");
        if(fragment != null)
            getSupportFragmentManager().beginTransaction().remove(fragment).commit();
        BeaconNavigateFragment nextFrag = new BeaconNavigateFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, nextFrag, "navigateFragment")
                .addToBackStack("navigateFragment")
                .commit();
        GlobalConstants.MODE = "navigateFragment";
        // observe bluetooth
        if (!BluetoothClient.isBluetoothEnabled()) {
            requestBluetooth();
        }
        BluetoothClient.startScanning();
    }

    @Override
    public void onNavigateCallBack() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag("navigateFragment");
        if(fragment != null)
            getSupportFragmentManager().beginTransaction().remove(fragment).commit();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, Camera2BasicFragment.newInstance(), "cameraFragment")
                .addToBackStack("cameraFragment")
                .commit();
        GlobalConstants.MODE = "cameraFragment";
        // stop observing bluetooth
        BluetoothClient.stopScanning();
    }

    public static boolean hasLocationPermission(@NonNull Context context) {
        boolean fineLocation = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarseLocation = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        return fineLocation && coarseLocation;
    }

    public static void requestLocationPermission(@NonNull Activity activity) {
        Log.d(TAG, "Requesting location permission");
        ActivityCompat.requestPermissions(activity, new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        }, REQUEST_CODE_LOCATION_PERMISSIONS);
    }

    public static boolean isLocationEnabled(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                int locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);
                return locationMode != Settings.Secure.LOCATION_MODE_OFF;
            } catch (Settings.SettingNotFoundException e) {
                Log.e(TAG, "Unable to get location mode");
                e.printStackTrace();
                return false;
            }
        } else {
            String locationProviders = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            return !TextUtils.isEmpty(locationProviders);
        }
    }

    private void setupLocationService() {
        Log.v(TAG, "Setting up location service");
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        SettingsClient client = LocationServices.getSettingsClient(PathPlanActivity.this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(PathPlanActivity.this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                Log.v(TAG, "Location settings satisfied");
            }
        });

        task.addOnFailureListener(PathPlanActivity.this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                int statusCode = ((ApiException) e).getStatusCode();
                switch (statusCode) {
                    case CommonStatusCodes.RESOLUTION_REQUIRED:
                        Log.w(TAG, "Location settings not satisfied, attempting resolution intent");
                        try {
                            ResolvableApiException resolvable = (ResolvableApiException) e;
                            resolvable.startResolutionForResult(PathPlanActivity.this, REQUEST_CODE_LOCATION_SETTINGS);
                        } catch (IntentSender.SendIntentException sendIntentException) {
                            Log.e(TAG, "Unable to start resolution intent");
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        Log.w(TAG, "Location settings not satisfied and can't be changed");
                        break;
                }
            }
        });
    }
}
