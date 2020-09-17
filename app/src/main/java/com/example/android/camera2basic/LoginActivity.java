package com.example.android.camera2basic;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;


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
