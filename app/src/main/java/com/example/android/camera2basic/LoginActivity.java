package com.example.android.camera2basic;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.android.camera2basic.adapter.ItemGridAdapter;
import com.example.android.camera2basic.data.DataStore;
import com.example.android.camera2basic.data.ItemData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    TextView johnDoeTv, janeDaleTv;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        johnDoeTv = findViewById(R.id.jon_tv);
        janeDaleTv = findViewById(R.id.jan_tv);

        johnDoeTv.setOnClickListener(this);
        janeDaleTv.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.jan_tv:
                goToHome(getResources().getString(R.string.jane));
                break;
            case R.id.jon_tv:
                goToHome(getResources().getString(R.string.john));
                break;
            default:
                break;
        }
    }

    private void goToHome(String pName){
        Intent homeIntent = new Intent(LoginActivity.this, HomeActivity.class);
        homeIntent.putExtra("name", pName);
        startActivity(homeIntent);
        finish();
    }
}
