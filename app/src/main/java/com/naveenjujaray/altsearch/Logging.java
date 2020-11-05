package com.naveenjujaray.altsearch;

import android.util.Log;

public class Logging {

    /**
     * Logs only if in debug mode
     * @param tag context
     * @param message text
     */
    public static void log(String tag, String message){
        if ((BuildConfig.DEBUG)) {
            Log.d(tag, message);
        }
    }

    /**
     * Logs Error only if in debug mode
     * @param tag context
     * @param message text
     */
    public static void logError(String tag, String message, Exception e){
        if ((BuildConfig.DEBUG)) {
            Log.e(tag, message, e);
        }
    }
}
