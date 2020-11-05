package com.naveenjujaray.altsearch;

import android.content.Context;
import android.util.AttributeSet;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;

public class RotatableZoomableImageView extends ZoomableImageView {

    private static final long ROTATE_ANIMATION_DURATION = 100;

    public RotatableZoomableImageView(Context context) {
        super(context);
    }

    public RotatableZoomableImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void startAnimation(int angle) {
        clearAnimation();

        RotateAnimation rotate = new RotateAnimation(
                0, angle,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        rotate.setFillAfter(true);
        rotate.setDuration(ROTATE_ANIMATION_DURATION);
        rotate.setInterpolator(new AccelerateDecelerateInterpolator());
        startAnimation(rotate);
    }
}
