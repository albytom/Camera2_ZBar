package com.example.android.camera2basic;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android.camera2basic.adapter.ItemGridAdapter;
import com.example.android.camera2basic.bluetooth.BeaconLoc;
import com.example.android.camera2basic.bluetooth.BeaconStore;
import com.example.android.camera2basic.data.DataStore;
import com.example.android.camera2basic.data.ItemData;
import com.example.android.camera2basic.data.LocationItem;
import com.example.android.camera2basic.data.LocationStore;
import com.example.android.camera2basic.util.GlobalConstants;
import com.nexenio.bleindoorpositioning.location.Location;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class HomeActivity extends AppCompatActivity {

    ItemGridAdapter mItemGridAdapter;
    DataStore mDataStore;
    LocationStore mLocationStore;
    Button goButton;
    private static String STATUS = "";
    private Dialog mPickPendingDialog;
    private RecyclerView mRecyclerView;
    private HashMap<String, int[]> hm_location=new HashMap<String, int[]>();


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        goButton = findViewById(R.id.button_go);
        Bundle extras = getIntent().getExtras();
        mDataStore = DataStore.getInstance();
        mLocationStore = LocationStore.getInstance();
        mRecyclerView = findViewById(R.id.rv_data);
        int numberOfColumns = 4;
        mRecyclerView.setLayoutManager(new GridLayoutManager(this, numberOfColumns));
        if (extras != null) {
            if (extras.getString("data") != null && extras.getString("data").equals("data")) {
                mItemGridAdapter = new ItemGridAdapter(this, DataStore.getItemDataArrayList());
                mRecyclerView.setAdapter(mItemGridAdapter);
                goButton.setText(getResources().getString(R.string.finish));
                STATUS = getResources().getString(R.string.finish);
            } else if (extras.getString("name") != null) {
                showDialog_LoadData();
               BeaconStore.setBeaconArrayList(getBeaconsFromJSON());
            }
        }

        getLocationFromJSON();

        //printHmLocations();


        goButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (STATUS.equalsIgnoreCase(getResources().getString(R.string.go))) {
                    Intent intent = new Intent(HomeActivity.this, PathPlanActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    if (DataStore.isItemPending()) {
                        DataStore.updatePosition();
                        showDialog_PickPending();
                    } else {
                        DataStore.clearData();
                        goToLogin();
                    }
                }
            }
        });
    }

    /**
     * Dialog to show pick pending
     */
    private void showDialog_LoadData() {
        mPickPendingDialog = new Dialog(this);
        mPickPendingDialog.setContentView(R.layout.empty_list_dialog);
        TextView yesTv = mPickPendingDialog.findViewById(R.id.yes_tv);
        TextView noTv = mPickPendingDialog.findViewById(R.id.no_tv);
        yesTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        loadDataToGrid();
                        mPickPendingDialog.dismiss();
                    }
                });
            }
        });
        noTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToLogin();
                mPickPendingDialog.dismiss();
            }
        });
        mPickPendingDialog.show();
    }

    private void goToLogin() {
        Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    /**
     * Dialog to show pick pending
     */
    private void showDialog_PickPending() {
        mPickPendingDialog = new Dialog(this);
        mPickPendingDialog.setContentView(R.layout.pick_pending_dialog);
        TextView yesTv = mPickPendingDialog.findViewById(R.id.yes_tv);
        TextView noTv = mPickPendingDialog.findViewById(R.id.no_tv);
        yesTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(HomeActivity.this, PathPlanActivity.class);
                        GlobalConstants.PENDING = true;
                        startActivity(intent);
                        finish();
                        mPickPendingDialog.dismiss();
                    }
                });
            }
        });
        noTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DataStore.clearData();
                goToLogin();
                mPickPendingDialog.dismiss();
            }
        });
        mPickPendingDialog.show();
    }


    private void loadDataToGrid() {
        DataStore.setItemDataArrayList(getDataFromJSON());
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mItemGridAdapter = new ItemGridAdapter(HomeActivity.this, DataStore.getItemDataArrayList());
                mRecyclerView.setAdapter(mItemGridAdapter);
            }
        });

        goButton.setText(getResources().getString(R.string.go));
        STATUS = getResources().getString(R.string.go);
    }

    private ArrayList<ItemData> getDataFromJSON() {
        ArrayList<ItemData> itemDataList = new ArrayList<>();
        try {
            JSONObject obj = new JSONObject(loadJSONFromAsset("data.json"));
            JSONArray m_jArry = obj.getJSONArray("Data");

            for (int i = 0; i < m_jArry.length(); i++) {
                JSONObject jo_inside = m_jArry.getJSONObject(i);
                Log.d("Details-->", jo_inside.getString("item"));
                String itemName = jo_inside.getString("item");
                String itemLoc = jo_inside.getString("location");
                JSONArray array = jo_inside.optJSONArray("coordinates");
                // Create an int array to accomodate the numbers.
                int[] numbers = new int[2];
                // Deal with the case of a non-array value.
                if (array == null) {
                    numbers[0] = 0;
                    numbers[1] = 0;
                }
                // Extract numbers from JSON array.
                else {
                    for (int j = 0; j < array.length(); ++j) {
                        numbers[j] = array.optInt(j);
                    }
                }

                //Add your values in your `ArrayList` as below:
                itemDataList.add(new ItemData(i, itemName, itemLoc, numbers));
            }
            return itemDataList;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    private ArrayList<BeaconLoc> getBeaconsFromJSON() {
        ArrayList<BeaconLoc> vBeaconLocArrayList = new ArrayList<>();
        try {
            JSONObject obj = new JSONObject(loadJSONFromAsset("beacons.json"));
            JSONArray m_jArry = obj.getJSONArray("beacon");

            for (int i = 0; i < m_jArry.length(); i++) {
                JSONObject jo_inside = m_jArry.getJSONObject(i);
                Log.d("ID-->", "" + jo_inside.getInt("id"));
                int id = jo_inside.getInt("id");
                int ky = jo_inside.getInt("ky");
                int kx = jo_inside.getInt("kx");
                int minor = jo_inside.getInt("minor");
                int major = jo_inside.getInt("major");
                String uuid = jo_inside.getString("uuid");
                String name = jo_inside.getString("name");

                //Add your values in your `ArrayList` as below:
                vBeaconLocArrayList.add(new BeaconLoc(id, ky, kx, minor, major, uuid, name));
            }
            return vBeaconLocArrayList;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String loadJSONFromAsset(String fileName) {
        String json = null;
        try {
            InputStream is = HomeActivity.this.getAssets().open(fileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    private  void printHmLoactions()
    {
        for (Map.Entry<String, int[]> entry : hm_location.entrySet()) {
            System.out.println(entry.getKey() + ": X: " + entry.getValue()[0]+ ", Y: " + entry.getValue()[1]);
        }

    }
    private void getLocationFromJSON() {

        try {
            JSONObject obj = new JSONObject(loadJSONFromAsset("cubicles.json"));
            JSONArray m_jArry = obj.getJSONArray("locations");

            for (int i = 0; i < m_jArry.length(); i++) {
                JSONObject jo_inside = m_jArry.getJSONObject(i);
                Log.d("ID-->", "" + jo_inside.getString("Cubicle"));
                String cubicle = jo_inside.getString("Cubicle");
                int kX = jo_inside.getInt("X");
                int kY = jo_inside.getInt("Y");

                //Add your values in your `ArrayList` as below:
                //hm_location.put(cubicle,new int[]{kX, kY});
                LocationStore.addToLocArrayList(new LocationItem(kY, kX, cubicle));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

   /* private String loadJSONFromAsset(String fileName) {
        String json = null;
        try {
            InputStream is = getActivity().getAssets().open(fileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }*/
}
