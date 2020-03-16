package com.example.android.camera2basic;

import android.util.Log;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

public class CvUtil {

    static {
        if (!OpenCVLoader.initDebug()) {
            Log.d("Error", "Unable to load OpenCV");
        } else {
            System.loadLibrary("CvDeblur");
        }
    }


    public static void processMat(Mat mat) {
         processMat(mat.getNativeObjAddr());
    }


    public static native void processMat(long matAddr);

}
