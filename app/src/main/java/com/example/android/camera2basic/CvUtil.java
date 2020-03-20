package com.example.android.camera2basic;

import org.opencv.core.Mat;
import org.opencv.core.Rect;

public class CvUtil {

    public static Mat processMat(Mat mat) {
        Mat A  = mat.clone();
        int [] roi = processMat(mat.getNativeObjAddr());
        return new Mat(A, new Rect(roi[0], roi[1], roi[2], roi[3]));
    }


    public static native int[] processMat(long matAddr);

}
