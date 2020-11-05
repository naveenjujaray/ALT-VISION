package com.naveenjujaray.altsearch.helper;

import java.util.Comparator;

/*
 * Created by Gautam on 7/18/17.
 */

public class MyRect implements Comparator<MyRect> {

    private int topEdge;
    private int leftEdge;
    private int width;
    private int height;

    public MyRect(int leftEdge, int topEdge, int width, int height) {
        this.topEdge = topEdge;
        this.leftEdge = leftEdge;
        this.width = width;
        this.height = height;
    }

    public MyRect() {
    }

    @Override
    public int compare(MyRect r1, MyRect r2) {
        if (r1.topEdge > r2.topEdge)
            return 1;
        else if (r1.topEdge < r2.topEdge)
            return -1;
        else
            return 0;
    }

    public int getTopEdge() {
        return topEdge;
    }

    public int getLeftEdge() {
        return leftEdge;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    public String toString() {
        return leftEdge + "," + topEdge + "," + (leftEdge + width) + "," + (topEdge + height) + " ";
    }

    public void invert(int w, int h) {

        int temp = topEdge;
        topEdge = leftEdge;
        leftEdge = temp;

        temp = width;
        width = height;
        height = temp;

        // Coordinates are being counted from portrait mode
        // Make it to count from landscape mode

        leftEdge = w - leftEdge - width;

    }

    public MyPointF getMidPointAsMyPointF() {

        float x, y;
        MyPointF mPF = new MyPointF();

        x = leftEdge + (width / 2);
        y = topEdge + (height / 2);

        mPF.set(x, y);
        return mPF;
    }
}
