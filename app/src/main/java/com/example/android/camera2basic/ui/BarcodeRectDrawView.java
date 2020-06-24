package com.example.android.camera2basic.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.example.android.camera2basic.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BarcodeRectDrawView extends android.support.v7.widget.AppCompatImageView {

    //private ArrayList<RectF> mRectArray;
    private int radius;
    private List<RectF> mRectArray = Collections.synchronizedList(new ArrayList<RectF>());

    public BarcodeRectDrawView(Context context, AttributeSet attrs) {
        super(context, attrs);
        //In versions > 3.0 need to define layer Type
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        synchronized (mRectArray) {
            mRectArray = new ArrayList<>();
        }
    }

    public void setBarcodeRect(RectF rect) {
        synchronized (mRectArray) {
            this.mRectArray.add(rect);
        }
        //Redraw after defining rect
        postInvalidate();
    }

    public void clearScreen(){
        synchronized (mRectArray) {
            this.mRectArray.clear();
        }
        //Redraw after clearing rect
        postInvalidate();
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        synchronized (mRectArray) {
            if (mRectArray != null && mRectArray.size() > 0) {
                Paint paint = new Paint();
                paint.setColor(getResources().getColor(R.color.shade_green));
                paint.setStyle(Paint.Style.FILL);
                //paint.setStrokeWidth(3);
                for (RectF mRect : mRectArray) {
                    canvas.drawRect(mRect, paint);
                }
            } else {
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            }
        }
    }
}