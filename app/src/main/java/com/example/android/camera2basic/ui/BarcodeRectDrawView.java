package com.example.android.camera2basic.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.appcompat.widget.AppCompatImageView;

import com.example.android.camera2basic.R;

import java.util.ArrayList;

public class BarcodeRectDrawView extends AppCompatImageView {

    private ArrayList<RectF> mRectArray;
    private int radius;

    public BarcodeRectDrawView(Context context, AttributeSet attrs) {
        super(context, attrs);
        //In versions > 3.0 need to define layer Type
        if (android.os.Build.VERSION.SDK_INT >= 11)
        {
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
        mRectArray = new ArrayList<>();
    }

    public void setBarcodeRect(RectF rect) {
        this.mRectArray.add(rect);
        //Redraw after defining rect
        postInvalidate();
    }

    public void clearScreen(){
        this.mRectArray.clear();
        //Redraw after clearing rect
        postInvalidate();
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(mRectArray != null && mRectArray.size() > 0) {
            Paint paint = new Paint();
            paint.setColor(getResources().getColor(R.color.shade_green));
            paint.setStyle(Paint.Style.FILL);
            //paint.setStrokeWidth(3);
            for (RectF mRect: mRectArray) {
                canvas.drawRect(mRect, paint);
            }
        }else{
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        }
    }
}