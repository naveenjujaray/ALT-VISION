package com.naveenjujaray.altsearch;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Handler;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.widget.ImageView;

import com.naveenjujaray.altsearch.helper.MyRect;

@SuppressLint("AppCompatCustomView")
public class ZoomableImageView extends ImageView {

    private static final String TAG = "ZoomableImageView";

    public static final int DEFAULT_SCALE_FIT_INSIDE = 0;
    private static final float DEFAULT_MAX_SCALE = 8.0f;

    private Bitmap imgBitmap = null;

    public int containerWidth;
    public int containerHeight;

    private Paint background;

    //Matrices will be used to move and zoom image
    private Matrix matrix = new Matrix();
    private Matrix savedMatrix = new Matrix();

    private PointF start = new PointF();

    private float currentScale;
    private float curX;
    private float curY;

    //We can be in one of these 3 states
    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;
    private int mode = NONE;

    //For animating stuff
    private float targetX;
    private float targetY;
    private float targetScale;
    private float targetScaleX;
    private float targetScaleY;
    private float scaleChange;

    private boolean isAnimating = false;

    //For pinch and zoom
    private float oldDist = 1f;
    private PointF mid = new PointF();

    private Handler mHandler = new Handler();

    private float minScale;
    private float maxScale = DEFAULT_MAX_SCALE;

    private GestureDetector gestureDetector;
    private MyGestureDetector myGestureDetector;

    private int defaultScale;
    public boolean zoomChanged;

    // GETTER & SETTERS
    public float getMaxScale() {
        return maxScale;
    }

    public void setMaxScale(float maxScale) {
        this.maxScale = maxScale;
    }

    public void resetMaxScale() {
        this.maxScale = DEFAULT_MAX_SCALE;
    }

    public float getCurrentScale() {
        return currentScale;
    }

    public boolean isEmpty() {
        return imgBitmap == null;
    }

    public boolean isAnimating() {
        return isAnimating;
    }

    public ZoomableImageView(Context context) {
        super(context);
        setFocusable(true);
        setFocusableInTouchMode(true);

        initPaints();
        myGestureDetector = new MyGestureDetector();
        gestureDetector = new GestureDetector(context, myGestureDetector);
    }

    public ZoomableImageView(Context context, AttributeSet attrs) {
        super(context, attrs);

        initPaints();
        myGestureDetector = new MyGestureDetector();
        gestureDetector = new GestureDetector(context, myGestureDetector);

        defaultScale = ZoomableImageView.DEFAULT_SCALE_FIT_INSIDE;
    }

    private void initPaints() {
        background = new Paint();
        background.setColor(Color.BLACK);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);

        //Reset the width and height. Will draw bitmap and change
        containerWidth = width;
        containerHeight = height;

        if (imgBitmap != null) {
            int imgHeight = imgBitmap.getHeight();
            int imgWidth = imgBitmap.getWidth();

            float scale;
            int initX = 0;
            int initY = 0;

            if (defaultScale == ZoomableImageView.DEFAULT_SCALE_FIT_INSIDE) {
                if (imgWidth > containerWidth) {
                    scale = (float) containerWidth / imgWidth;
                    float newHeight = imgHeight * scale;
                    initY = (containerHeight - (int) newHeight) / 2;

                    matrix.setScale(scale, scale);
                    matrix.postTranslate(0, initY);
                } else {
                    scale = (float) containerHeight / imgHeight;
                    float newWidth = imgWidth * scale;
                    initX = (containerWidth - (int) newWidth) / 2;

                    matrix.setScale(scale, scale);
                    matrix.postTranslate(initX, 0);
                }

                curX = initX;
                curY = initY;

                currentScale = scale;
                minScale = scale;
            } else {
                if (imgWidth > containerWidth) {
                    initY = (containerHeight - imgHeight) / 2;
                    matrix.postTranslate(0, initY);
                } else {
                    initX = (containerWidth - imgWidth) / 2;
                    matrix.postTranslate(initX, 0);
                }

                curX = initX;
                curY = initY;

                currentScale = 1.0f;
                minScale = 1.0f;
            }


            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (imgBitmap != null && canvas != null) {
            canvas.drawBitmap(imgBitmap, matrix, background);
        }
    }

    //Checks and sets the target image x and y co-ordinates if out of bounds
    private void checkImageConstraints() {
        if (imgBitmap == null) {
            return;
        }

        float[] mvals = new float[9];
        matrix.getValues(mvals);

        currentScale = mvals[0];

        if (currentScale < minScale) {
            float deltaScale = minScale / currentScale;
            float px = containerWidth / 2;
            float py = containerHeight / 2;
            matrix.postScale(deltaScale, deltaScale, px, py);
            invalidate();
        }

        matrix.getValues(mvals);
        currentScale = mvals[0];
        curX = mvals[2];
        curY = mvals[5];

        int rangeLimitX = containerWidth - (int) (imgBitmap.getWidth() * currentScale);
        int rangeLimitY = containerHeight - (int) (imgBitmap.getHeight() * currentScale);


        boolean toMoveX = false;
        boolean toMoveY = false;

        if (rangeLimitX < 0) {
            if (curX > 0) {
                targetX = 0;
                toMoveX = true;
            } else if (curX < rangeLimitX) {
                targetX = rangeLimitX;
                toMoveX = true;
            }
        } else {
            targetX = rangeLimitX / 2;
            toMoveX = true;
        }

        if (rangeLimitY < 0) {
            if (curY > 0) {
                targetY = 0;
                toMoveY = true;
            } else if (curY < rangeLimitY) {
                targetY = rangeLimitY;
                toMoveY = true;
            }
        } else {
            targetY = rangeLimitY / 2;
            toMoveY = true;
        }

        if (toMoveX || toMoveY) {
            if (!toMoveY) {
                targetY = curY;
            }
            if (!toMoveX) {
                targetX = curX;
            }

            //Disable touch event actions
            isAnimating = true;
            //Initialize timer
            mHandler.removeCallbacks(mUpdateImagePositionTask);
            mHandler.postDelayed(mUpdateImagePositionTask, 100);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        this.zoomChanged = true;
        if (gestureDetector.onTouchEvent(event)) {
            return true;
        }

        if (isAnimating) {
            return true;
        }

        //Handle touch events here
        float[] mvals = new float[9];
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                if (!isAnimating) {
                    savedMatrix.set(matrix);
                    start.set(event.getX(), event.getY());
                    mode = DRAG;
                }
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                oldDist = spacing(event);
                if (oldDist > 10f) {
                    savedMatrix.set(matrix);
                    midPoint(mid, event);
                    mode = ZOOM;
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                mode = NONE;

                matrix.getValues(mvals);
                curX = mvals[2];
                curY = mvals[5];
                currentScale = mvals[0];

                if (!isAnimating) {
                    checkImageConstraints();
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (mode == DRAG && !isAnimating) {
                    matrix.set(savedMatrix);
                    float diffX = event.getX() - start.x;
                    float diffY = event.getY() - start.y;

                    matrix.postTranslate(diffX, diffY);

                    matrix.getValues(mvals);
                    curX = mvals[2];
                    curY = mvals[5];
                    currentScale = mvals[0];
                } else if (mode == ZOOM && !isAnimating) {

                    resetMaxScale();
                    float newDist = spacing(event);
                    if (newDist > 10f) {
                        matrix.set(savedMatrix);
                        float scale = newDist / oldDist;
                        matrix.getValues(mvals);
                        currentScale = mvals[0];

                        if (currentScale * scale <= minScale) {
                            matrix.postScale(minScale / currentScale, minScale / currentScale, mid.x, mid.y);
                        } else if (currentScale * scale >= maxScale) {
                            matrix.postScale(maxScale / currentScale, maxScale / currentScale, mid.x, mid.y);
                        } else {
                            matrix.postScale(scale, scale, mid.x, mid.y);
                        }


                        matrix.getValues(mvals);
                        curX = mvals[2];
                        curY = mvals[5];
                        currentScale = mvals[0];
                    }
                }

                break;
        }

        //Calculate the transformations and then invalidate
        invalidate();
        return true;
    }

    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    private void midPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }

    public void setImageBitmap(Bitmap b) {
        if (b != null) {
            imgBitmap = b;

            containerWidth = getWidth();
            containerHeight = getHeight();

            int imgHeight = imgBitmap.getHeight();
            int imgWidth = imgBitmap.getWidth();

            float scale;
            int initX = 0;
            int initY = 0;

            matrix.reset();

            if (defaultScale == ZoomableImageView.DEFAULT_SCALE_FIT_INSIDE) {
                if (imgWidth > containerWidth) {
                    scale = (float) containerWidth / imgWidth;
                    float newHeight = imgHeight * scale;
                    initY = (containerHeight - (int) newHeight) / 2;

                    matrix.setScale(scale, scale);
                    matrix.postTranslate(0, initY);
                } else {
                    scale = (float) containerHeight / imgHeight;
                    float newWidth = imgWidth * scale;
                    initX = (containerWidth - (int) newWidth) / 2;

                    matrix.setScale(scale, scale);
                    matrix.postTranslate(initX, 0);
                }

                curX = initX;
                curY = initY;

                currentScale = scale;
                minScale = scale;
            } else {
                if (imgWidth > containerWidth) {
                    initX = 0;
                    if (imgHeight > containerHeight) {
                        initY = 0;
                    } else {
                        initY = (containerHeight - imgHeight) / 2;
                    }

                    matrix.postTranslate(0, initY);
                } else {
                    initX = (containerWidth - imgWidth) / 2;
                    if (imgHeight > containerHeight) {
                        initY = 0;
                    } else {
                        initY = (containerHeight - imgHeight) / 2;
                    }
                    matrix.postTranslate(initX, 0);
                }

                curX = initX;
                curY = initY;

                currentScale = 1.0f;
                minScale = 1.0f;
            }

            invalidate();
        } else {
            Log.d(TAG, "bitmap is null");
        }
    }

    public void setPhotoBitmap(Bitmap imgBitmap) {
        this.imgBitmap = imgBitmap;
        invalidate();
    }

    private Runnable mUpdateImagePositionTask = new Runnable() {
        public void run() {
            float[] mValues;

            if (Math.abs(targetX - curX) < 5 && Math.abs(targetY - curY) < 5) {
                isAnimating = false;
                mHandler.removeCallbacks(mUpdateImagePositionTask);

                mValues = new float[9];
                matrix.getValues(mValues);

                currentScale = mValues[0];
                curX = mValues[2];
                curY = mValues[5];

                //Set the image parameters and invalidate display
                float diffX = (targetX - curX);
                float diffY = (targetY - curY);

                matrix.postTranslate(diffX, diffY);
            } else {
                isAnimating = true;
                mValues = new float[9];
                matrix.getValues(mValues);

                currentScale = mValues[0];
                curX = mValues[2];
                curY = mValues[5];

                //Set the image parameters and invalidate display
                float diffX = (targetX - curX) * 0.3f;
                float diffY = (targetY - curY) * 0.3f;

                matrix.postTranslate(diffX, diffY);
                mHandler.postDelayed(this, 25);
            }

            invalidate();
        }
    };

    private Runnable mUpdateImageScale = new Runnable() {
        public void run() {
            float transitionalRatio = targetScale / currentScale;
            float dx;
            if (Math.abs(transitionalRatio - 1) > 0.05) {
                isAnimating = true;
                if (targetScale > currentScale) {
                    dx = transitionalRatio - 1;
                    scaleChange = 1 + dx * 0.2f;

                    currentScale *= scaleChange;

                    if (currentScale > targetScale) {
                        currentScale = currentScale / scaleChange;
                        scaleChange = 1;
                    }
                } else {
                    dx = 1 - transitionalRatio;
                    scaleChange = 1 - dx * 0.5f;
                    currentScale *= scaleChange;

                    if (currentScale < targetScale) {
                        currentScale = currentScale / scaleChange;
                        scaleChange = 1;
                    }
                }


                if (scaleChange != 1) {
                    matrix.postScale(scaleChange, scaleChange, targetScaleX, targetScaleY);
                    mHandler.postDelayed(mUpdateImageScale, 70);
                    invalidate();
                } else {
                    isAnimating = false;
                    scaleChange = 1;
                    matrix.postScale(targetScale / currentScale, targetScale / currentScale, targetScaleX, targetScaleY);
                    currentScale = targetScale;
                    mHandler.removeCallbacks(mUpdateImageScale);
                    invalidate();
                    checkImageConstraints();
                }
            } else {
                isAnimating = false;
                scaleChange = 1;
                matrix.postScale(targetScale / currentScale, targetScale / currentScale, targetScaleX, targetScaleY);
                currentScale = targetScale;
                mHandler.removeCallbacks(mUpdateImageScale);
                invalidate();
                checkImageConstraints();
            }

        }
    };


    public void externalDoubleTapGestureGenerator(float x, float y) {

        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis();

        MotionEvent e = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_DOWN, x, y, 0);
        myGestureDetector.onDoubleTap(e);
        Log.d(TAG, "Simulated double tap " + e.getX() + "," + e.getY());
        Log.d(TAG, "Zoom level" + maxScale);

    }


    public void colorThisRect(MyRect myRect) {

        Paint mPaint = new Paint();
        mPaint.setColor(Color.YELLOW);
        mPaint.setAlpha(150);
        Canvas canvas = new Canvas(imgBitmap);
        canvas.save();
        canvas.drawRect(convertRect(myRect), mPaint);
        canvas.save();
    }

    private Rect convertRect(MyRect r) {
        return new Rect(r.getLeftEdge(), r.getTopEdge(), (r.getLeftEdge() + r.getWidth()), (r.getTopEdge() + r.getHeight()));
    }

    private class MyGestureDetector extends SimpleOnGestureListener {

        @Override
        public boolean onDoubleTap(MotionEvent event) {
            if (isAnimating) {
                return true;
            }

            scaleChange = 1;
            isAnimating = true;
            targetScaleX = event.getX();
            targetScaleY = event.getY();

            if (Math.abs(currentScale - maxScale) > 0.1) {
                targetScale = maxScale;
            } else {
                targetScale = minScale;
            }

            mHandler.removeCallbacks(mUpdateImageScale);
            mHandler.post(mUpdateImageScale);

            Log.d(TAG, "Normal double tap " + event.getX() + "," + event.getY());

            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return super.onFling(e1, e2, velocityX, velocityY);
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return false;
        }
    }

    public boolean isNotZoomed() {
        return currentScale == minScale;
    }

}