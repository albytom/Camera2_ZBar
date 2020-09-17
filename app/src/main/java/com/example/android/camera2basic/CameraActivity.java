/*
 * Copyright 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.camera2basic;

import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.example.android.camera2basic.util.GlobalConstants;

import org.opencv.android.OpenCVLoader;

public class CameraActivity extends AppCompatActivity {

    static {
        if (!OpenCVLoader.initDebug()) {
            Log.d("Error", "Unable to load OpenCV");
        } else {
            System.loadLibrary("deblur-lib");
            System.loadLibrary("opencv_java3");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        if (null == savedInstanceState) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, Camera2BasicFragment.newInstance())
                    .commit();
        }
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        GlobalConstants.REAL_SCR_HEIGHT = displayMetrics.heightPixels;
        GlobalConstants.REAL_SCR_WIDTH = displayMetrics.widthPixels;
        float density = displayMetrics.densityDpi;
        Log.e("Density", " densityDpi: " + density + " W: " + GlobalConstants.REAL_SCR_WIDTH + " H: " + GlobalConstants.REAL_SCR_HEIGHT);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, Camera2BasicFragment.newInstance())
                .commit();
    }
}
