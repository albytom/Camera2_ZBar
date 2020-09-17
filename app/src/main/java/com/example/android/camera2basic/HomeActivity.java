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
import com.example.android.camera2basic.data.DataStore;
import com.example.android.camera2basic.data.ItemData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class HomeActivity extends AppCompatActivity {

    ItemGridAdapter mItemGridAdapter;
    DataStore mDataStore;
    Button goButton;
    private static String STATUS = "";
    private Dialog mPickPendingDialog;
    private RecyclerView mRecyclerView;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        goButton = findViewById(R.id.button_go);
        Bundle extras = getIntent().getExtras();
        mDataStore = DataStore.getInstance();
        mRecyclerView = findViewById(R.id.rv_data);
        int numberOfColumns = 4;
        mRecyclerView.setLayoutManager(new GridLayoutManager(this, numberOfColumns));
        if (extras != null) {
            if (extras.getString("data") != null && extras.getString("data").equals("data")) {
                mItemGridAdapter = new ItemGridAdapter(this, mDataStore.getItemDataPickedList());
                mRecyclerView.setAdapter(mItemGridAdapter);
                goButton.setText(getResources().getString(R.string.finish));
                STATUS = getResources().getString(R.string.finish);
            } else if (extras.getString("name") != null) {
                showDialog_LoadData();
            }
        }


        goButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (STATUS.equalsIgnoreCase(getResources().getString(R.string.go))) {
                    Intent intent = new Intent(HomeActivity.this, PathPlanActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    if (mDataStore.isItemPending()) {
                        showDialog_PickPending();
                    } else {
                        mDataStore.clearData();
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
                mDataStore.clearData();
                goToLogin();
                mPickPendingDialog.dismiss();
            }
        });
        mPickPendingDialog.show();
    }


    private void loadDataToGrid() {
        mDataStore.setItemDataArrayList(getDataFromJSON());
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mItemGridAdapter = new ItemGridAdapter(HomeActivity.this, mDataStore.getItemDataArrayList());
                mRecyclerView.setAdapter(mItemGridAdapter);
            }
        });

        goButton.setText(getResources().getString(R.string.go));
        STATUS = getResources().getString(R.string.go);
    }

    private ArrayList<ItemData> getDataFromJSON() {
        ArrayList<ItemData> itemDataList = new ArrayList<>();
        try {
            JSONObject obj = new JSONObject(loadJSONFromAsset());
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

    private String loadJSONFromAsset() {
        String json = null;
        try {
            InputStream is = HomeActivity.this.getAssets().open("data.json");
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
}
