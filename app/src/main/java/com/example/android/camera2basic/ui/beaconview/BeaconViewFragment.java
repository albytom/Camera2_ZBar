package com.example.android.camera2basic.ui.beaconview;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.CallSuper;
import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;

import com.example.android.camera2basic.R;
import com.example.android.camera2basic.location.AndroidLocationProvider;
import com.nexenio.bleindoorpositioning.IndoorPositioning;
import com.nexenio.bleindoorpositioning.ble.advertising.IndoorPositioningAdvertisingPacket;
import com.nexenio.bleindoorpositioning.ble.beacon.Beacon;
import com.nexenio.bleindoorpositioning.ble.beacon.BeaconManager;
import com.nexenio.bleindoorpositioning.ble.beacon.BeaconUpdateListener;
import com.nexenio.bleindoorpositioning.ble.beacon.filter.BeaconFilter;
import com.nexenio.bleindoorpositioning.ble.beacon.filter.IBeaconFilter;
import com.nexenio.bleindoorpositioning.location.LocationListener;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class BeaconViewFragment extends Fragment {

    protected BeaconManager beaconManager = BeaconManager.getInstance();
    protected LocationListener deviceLocationListener;
    protected BeaconUpdateListener beaconUpdateListener;
    protected List<BeaconFilter> beaconFilters = new ArrayList<>();

    // TODO: Remove legacy uuid once all beacons are updated
   // protected IBeaconFilter uuidFilter = new IBeaconFilter(IndoorPositioningAdvertisingPacket.INDOOR_POSITIONING_UUID);
    protected IBeaconFilter uuidFilter = new IBeaconFilter(IndoorPositioningAdvertisingPacket.INDOOR_POSITIONING_UUID, UUID.fromString("fda50693-a4e2-4fb1-afcf-c6eb07647825"));

    protected FrameLayout mFrameLayout;

    @ColorUtil.ColoringMode
    protected int coloringMode = ColorUtil.COLORING_MODE_INSTANCES;

    public BeaconViewFragment() {
        deviceLocationListener = createDeviceLocationListener();
        beaconUpdateListener = createBeaconUpdateListener();
    }

    protected abstract LocationListener createDeviceLocationListener();

    protected abstract BeaconUpdateListener createBeaconUpdateListener();

    @LayoutRes
    protected abstract int getLayoutResourceId();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("beacon","Inside on create of BeaconViewFragment");
        setHasOptionsMenu(true);

    }

    @CallSuper
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View inflatedView = inflater.inflate(getLayoutResourceId(), container, false);
        mFrameLayout = inflatedView.findViewById(R.id.container);
        return inflatedView;
    }

    @CallSuper
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        IndoorPositioning.getInstance().setIndoorPositioningBeaconFilter(uuidFilter);
        IndoorPositioning.registerLocationListener(deviceLocationListener);
        //AndroidLocationProvider.registerLocationListener(deviceLocationListener);
        //AndroidLocationProvider.requestLastKnownLocation();
        BeaconManager.registerBeaconUpdateListener(beaconUpdateListener);
    }

    @CallSuper
    @Override
    public void onDetach() {
        IndoorPositioning.unregisterLocationListener(deviceLocationListener);
        //AndroidLocationProvider.unregisterLocationListener(deviceLocationListener);
        BeaconManager.unregisterBeaconUpdateListener(beaconUpdateListener);
        super.onDetach();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.beacon_view, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_color_by_instance: {
                onColoringModeSelected(ColorUtil.COLORING_MODE_INSTANCES, item);
                return true;
            }
            case R.id.menu_color_by_type: {
                onColoringModeSelected(ColorUtil.COLORING_MODE_TYPES, item);
                return true;
            }
            case R.id.menu_color_by_property: {
                onColoringModeSelected(ColorUtil.COLORING_MODE_PROPERTIES, item);
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    public IBeaconFilter getUuidFilter() {
        return uuidFilter;
    }

    protected void onColoringModeSelected(@ColorUtil.ColoringMode int coloringMode, MenuItem menuItem) {
        menuItem.setChecked(true);
        this.coloringMode = coloringMode;
    }

    protected List<Beacon> getBeacons() {
        if (beaconFilters.isEmpty()) {
            return new ArrayList<>(beaconManager.getBeaconMap().values());
        }
        List<Beacon> beacons = new ArrayList<>();
        for (Beacon beacon : beaconManager.getBeaconMap().values()) {
            for (BeaconFilter beaconFilter : beaconFilters) {
                if (beaconFilter.matches(beacon)) {
                    beacons.add(beacon);
                    break;
                }
            }
        }
        return beacons;
    }

   /* protected List<Beacon> getBeacons() {
        //Jasir edited
       //Log.d("beacon","Inside get beacon");
      //  if (beaconFilters.isEmpty()) {
         //   Log.d("beacon","empty filter");
          //  return new ArrayList<>(beaconManager.getBeaconMap().values());
       // }
        List<Beacon> beacons = new ArrayList<>();
      //  Log.d("beacon","-----------------------------");
        for (Beacon beacon : beaconManager.getBeaconMap().values()) {
            //for (BeaconFilter beaconFilter : beaconFilters) {
             //   if (beaconFilter.matches(beacon)) {


                   // Log.d("beacon","MAC Address ="+beacon.getMacAddress());
                   // Log.d("beacon","Distance ="+beacon.getDistance());

                    beacons.add(beacon);
                  //  Log.d("beacon","Tx ="+beacon.getTransmissionPower());
                //    break;
             //   }
           // }
        }
       // Log.d("beacon","-----------------------------");
        return beacons;
    }*/

    public void setUuidFilter(IBeaconFilter uuidFilter) {
        this.uuidFilter = uuidFilter;
    }
}
