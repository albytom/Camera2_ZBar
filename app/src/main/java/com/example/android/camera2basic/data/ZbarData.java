package com.example.android.camera2basic.data;

import android.graphics.RectF;

public class ZbarData {
    String mData;
    String mType;
    RectF mRect;

    public ZbarData(String mData, String mType) {
        this.mData = mData;
        this.mType = mType;
    }

    public ZbarData(String mData, String mType, RectF mRect) {
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

    public RectF getmRect() {
        return mRect;
    }

    public void setmRect(RectF mRect) {
        this.mRect = mRect;
    }
}
