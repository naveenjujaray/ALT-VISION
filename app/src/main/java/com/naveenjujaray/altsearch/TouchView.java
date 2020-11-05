package com.naveenjujaray.altsearch;

import android.content.Context;
import android.hardware.Camera;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class TouchView extends View {

    MainActivity activity;
    private float mDist = 0;

    public TouchView(Context context) {
        super(context);
    }

    public TouchView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public TouchView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public TouchView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        Camera.Parameters params = activity.cameraSource.camera.getParameters();
        Log.e("Finger Spacing", "(" + event.getX(0) + " , " + event.getY(0) + ")");
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_POINTER_DOWN:
                mDist = getFingerSpacing(event);
                break;
            case MotionEvent.ACTION_MOVE:
                activity.cameraSource.camera.cancelAutoFocus();
                handleZoom(event, params);
                break;
        }
        return false;
    }

    public void setActivity(MainActivity activity) {
        this.activity = activity;
    }

    public void handleZoom(MotionEvent event, Camera.Parameters params) {
        int maxZoom = params.getMaxZoom();
        int zoom = params.getZoom();
        float newDist = getFingerSpacing(event);
        if (newDist > mDist) {
            //zoom in
            if (zoom < maxZoom)
                zoom += 2;
        } else if (newDist < mDist) {
            //zoom out
            if (zoom > 0)
                zoom -= 2;
        }
        mDist = newDist;
        params.setZoom(zoom);
        try {
            activity.cameraSource.camera.setParameters(params);
            activity.updateZoomBar(zoom);
        } catch (Exception ignore) {
        }
    }

    public float getFingerSpacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }
}
