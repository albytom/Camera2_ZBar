package com.example.android.camera2basic;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class BarcodeRectDrawView extends android.support.v7.widget.AppCompatImageView {

    private RectF mRect;
    private int radius;

    public BarcodeRectDrawView(Context context, AttributeSet attrs) {
        super(context, attrs);
        //In versions > 3.0 need to define layer Type
        if (android.os.Build.VERSION.SDK_INT >= 11)
        {
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
    }

    public void setBarcodeRect(RectF rect) {
        this.mRect = rect;
        //Redraw after defining rect
        postInvalidate();
    }

    public void clearScreen(){
        this.mRect = null;
        //Redraw after clearing rect
        postInvalidate();
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(mRect != null) {
            Paint paint = new Paint();
            paint.setColor(getResources().getColor(android.R.color.holo_green_dark));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(5);
            canvas.drawRect(mRect, paint);
        }else{
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        }
    }
}