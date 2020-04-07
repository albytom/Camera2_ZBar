package com.example.android.camera2basic;

import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;

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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        goButton = findViewById(R.id.button_go);
        Bundle extras = getIntent().getExtras();
        mDataStore = DataStore.getInstance();
        RecyclerView recyclerView = findViewById(R.id.rv_data);
        int numberOfColumns = 4;
        recyclerView.setLayoutManager(new GridLayoutManager(this, numberOfColumns));
        if (extras != null) {
            if(extras.getString("data")!=null && extras.getString("data").equals("data")){
                mItemGridAdapter = new ItemGridAdapter(this, mDataStore.getItemDataPickedList());
                goButton.setText(getResources().getString(R.string.finish));
                STATUS = getResources().getString(R.string.finish);
            }else{
                mDataStore.setItemDataArrayList(getDataFromJSON());
                mItemGridAdapter = new ItemGridAdapter(this, mDataStore.getItemDataArrayList());
                goButton.setText(getResources().getString(R.string.go));
                STATUS = getResources().getString(R.string.go);
            }
        }
        recyclerView.setAdapter(mItemGridAdapter);

        goButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (STATUS.equalsIgnoreCase(getResources().getString(R.string.go))) {
                    Intent intent = new Intent(HomeActivity.this, CameraActivity.class);
                    startActivity(intent);
                    finish();
                } else {

                }
            }
        });
    }

    private ArrayList<ItemData> getDataFromJSON(){
        ArrayList<ItemData> itemDataList = new ArrayList<>();
        try {
            JSONObject obj = new JSONObject(loadJSONFromAsset());
            JSONArray m_jArry = obj.getJSONArray("Data");

            for (int i = 0; i < m_jArry.length(); i++) {
                JSONObject jo_inside = m_jArry.getJSONObject(i);
                Log.d("Details-->", jo_inside.getString("item"));
                String itemName = jo_inside.getString("item");
                String itemLoc = jo_inside.getString("location");

                //Add your values in your `ArrayList` as below:
                itemDataList.add(new ItemData(i, itemName, itemLoc));
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
