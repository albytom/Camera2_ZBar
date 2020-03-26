package com.example.android.camera2basic;

import android.graphics.Rect;

public class ZbarData {
    String mData;
    String mType;
    Rect mRect;

    public ZbarData(String mData, String mType) {
        this.mData = mData;
        this.mType = mType;
    }

    public ZbarData(String mData, String mType, Rect mRect) {
        this.mData = mData;
        this.mType = mType;
        this.mRect = mRect;
    }

    public String getmData() {
        return mData;
    }

    public void setmData(String mData) {
        this.mData = mData;
    }

    public String getmType() {
        return mType;
    }

    public void setmType(String mType) {
        this.mType = mType;
    }

    public Rect getmRect() {
        return mRect;
    }

    public void setmRect(Rect mRect) {
        this.mRect = mRect;
    }
}
