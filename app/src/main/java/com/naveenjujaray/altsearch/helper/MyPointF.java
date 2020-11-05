package com.naveenjujaray.altsearch.helper;

import android.graphics.PointF;

import java.util.Comparator;

/*
 * Created by Gautam on 7/18/17.
 */

public class MyPointF extends PointF implements Comparator<MyPointF> {

    @Override
    public int compare(MyPointF pOne, MyPointF pTwo) {
        if (pOne.y > pTwo.y)
            return 1;
        else if (pOne.y < pTwo.y)
            return -1;
        else
            return 0;
    }

}