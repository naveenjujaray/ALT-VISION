package com.naveenjujaray.altsearch;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

public class ClassificationOverlayGraphic extends GraphicOverlay.Graphic {
    //variables
    private static final float STROKE_WIDTH = 8.0f;
    private final Paint rectPaint;
    private final int Rect_COLOR = Color.RED;
    private ClassificationScheme scheme = ClassificationScheme.FIND_AT_CENTER;

    /**
     * Constructor
     * @param overlay GraphicOverlay to display on
     * @param scheme initial classification scheme chosen
     */

    public ClassificationOverlayGraphic(GraphicOverlay overlay, ClassificationScheme scheme) {
        super(overlay);

        //color and style set
        rectPaint = new Paint();
        rectPaint.setColor(Rect_COLOR);
        rectPaint.setStyle(Paint.Style.STROKE);
        rectPaint.setStrokeWidth(STROKE_WIDTH);
        update(scheme);
        postInvalidate();
    }

    /**
     * Setter method that changes scheme of the Overlay
     * @param scheme updated scheme
     */
    public void update(ClassificationScheme scheme){
        this.scheme = scheme;
    }

    /**
     * //draws rectangle in canvas in certain location on screen
     * @param canvas drawing canvas
     */
    @Override
    public void draw(Canvas canvas) {
        Rect r;
        if(scheme == ClassificationScheme.FIND_AT_BOTTOM){
            r = new Rect(0, canvas.getHeight()/2, canvas.getWidth(),canvas.getHeight());
        }else if(scheme == ClassificationScheme.FIND_AT_TOP){
            r = new Rect(0, 0, canvas.getWidth(),canvas.getHeight()/2);
        }else if(scheme == ClassificationScheme.FIND_AT_CENTER){
            r = new Rect(canvas.getWidth()/4, 0, 3*canvas.getWidth()/4,canvas.getHeight());
        }else {
            r = new Rect(0, 0, canvas.getWidth(),canvas.getHeight());
        }

        RectF rect = new RectF(r);
        canvas.drawRect(rect, rectPaint);
    }
}
