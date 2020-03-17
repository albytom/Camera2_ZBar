package com.example.android.camera2basic;

import org.opencv.core.Mat;

public class CvUtil {

    public static void processMat(Mat mat) {
         processMat(mat.getNativeObjAddr());
    }


    public static native void processMat(long matAddr);

}
