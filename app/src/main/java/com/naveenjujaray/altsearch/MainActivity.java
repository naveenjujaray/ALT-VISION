package com.naveenjujaray.altsearch;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.ExifInterface;
import android.media.MediaPlayer;
import android.media.MediaScannerConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.os.ConfigurationCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.cketti.mailto.EmailIntentBuilder;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener, PopupMenu.OnMenuItemClickListener {
    // =============================================================================================
    // Constants
    // =============================================================================================
    private static final int ACT_CHECK_TTS_DATA = 1000;
    private static final int SPEECH_REQUEST_CODE = 0;
    public static final int PERMISSION_REQUESTS = 1;
    public static final String TAG = "OCR Activity";
    public static final int vibrateLength = 50;
    private static final int MEDIA_TYPE_IMAGE = 14;
    private static final int MEDIA_TYPE_VIDEO = 15;
    private static final float VIDEO_STROKE = 5;
    private static final float PICTURE_STROKE = 20;
    private String sharedPrefFile = "com.naveenjujaray.altsearch.release";
    private float mDist = 0;


    // =============================================================================================
    // Camera Variables
    // =============================================================================================
    private CameraSourcePreview preview;
    public CameraSource cameraSource;

    // =============================================================================================
    // OCR Variables
    // =============================================================================================
    private OcrProcessor processor;
    private FrameProcessor frameProcessor;
    private ClassificationScheme scheme = ClassificationScheme.NONE;
    private ArrayList<Rect> arr;
    private Bitmap bmp;
    private Bitmap bmpaltr;

    // =============================================================================================
    // View Variables
    // =============================================================================================
    private RotatableZoomableImageView ziv;
    private ImageButton zoom;
    private ImageView next, previous;
    private ImageView settings, info;
    private GraphicOverlay graphicOverlay;
    private ImageView download;
    private TextToSpeech mTTS;
    EditText search;
    SeekBar zoomBar;
    DecimalFormat format = new DecimalFormat("0.0");
    boolean debug = false;

    private int progress = 0;
    SeekBar.OnSeekBarChangeListener a = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            MainActivity.this.progress = progress;
            if (!compProg) {
                updateZoom();
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };
    private boolean flashlightOn = false;
    private boolean compProg = false;
    private boolean rot = false;
    private Bitmap bmphil;
    private float angle;
    private ImageView scan;
    private ImageButton restart;
    private int oldWidth;
    private SharedPreferences mPreferences;
    private Locale locale;
    private ImageButton flashlight;
    private boolean zoomIn = true;
    private int cameraPictureRotation = 0;
    private boolean checked = false;


    // =============================================================================================
    // Flags and other primitive values
    // =============================================================================================

    boolean secondTime = false;
    boolean processingDone = false;
    public boolean success = false;
    private int num = 0;
    private boolean multi = true;
    private boolean nextB = true;
    private boolean wordFound;


    // =============================================================================================
    // Listener Variables and Threads
    // =============================================================================================

    private Thread capture, t, zoomThread;
    private Handler mHandler;
    private static final int BLINK_INTERVAL = 1000;

    private boolean cameraCapture = false;
    private TextView.OnEditorActionListener listener = new TextView.OnEditorActionListener() {
        @Override
        /**
         * Method is called when there is a change in the status of the edittext used for input
         * If there is a change when the activity is in the second state (i.e. when the zoomable
         * imageview is present), then the update method is called to find a different word on the
         * screen.
         */

        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    event != null &&
                            event.getAction() == KeyEvent.ACTION_DOWN &&
                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if (event == null || !event.isShiftPressed()) {
                    if (search.getText().toString().equals(Holder.searchWord)) {
                        saySomething(getString(R.string.w1));
                    }else {
                        saySomething(getString(R.string.w2) + " " + search.getText().toString());
                    }

                    String query = v.getText().toString();
                    searchVal = query;
                    if (success && secondTime) {
                        update(query);

                        if (arr.size() != 1) {
                            if(arr.size() == 0){
                                saySomething(query + " " + getString(R.string.w3));
                            }else {
                                saySomething(query + " " + getString(R.string.w4) + " " + arr.size() + " " + getString(R.string.w5));
                            }
                        } else {
                            saySomething(query + " " + getString(R.string.w6));
                        }
                    } else if (cameraCapture) {
                        update(query);

                        if (arr.size() != 1) {
                            saySomething(query + " " + getString(R.string.w7) + " " + arr.size() + " " + getString(R.string.w8));
                        } else {
                            saySomething(query + " " + getString(R.string.w9));
                        }
                    }
                    hideKeyboard(MainActivity.this);
                    return true; // consume.
                }
            }
            hideKeyboard(MainActivity.this);
            return false; // pass on to other listeners.
        }
    };

    private int previewZoomLevel = 0;
    private boolean keyboardVisible;
    private Orientation orientation;
    private volatile String searchVal = "";
    public String sizes;
    private volatile boolean blinking = false;

    // =============================================================================================
    // Methods
    // =============================================================================================

    /**
     * Method called when application is created. This is used to initialize initial values such as
     * visibility and the views. Detection thread is started here too.
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        if (!allPermissionsGranted()) {
            getRuntimePermissions();
        }

        //superclass method called
        super.onCreate(savedInstanceState);

        //ensures that there is not title present on screen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        orientation = Orientation.PORTRAIT;
        setContentView(R.layout.activity_main);

        //initialized views from the xml file activity_main.xml
        search = (EditText) findViewById(R.id.search);
        oldWidth = search.getWidth();

        search.setOnEditorActionListener(listener);
        preview = (CameraSourcePreview) findViewById(R.id.preview);
        graphicOverlay = (GraphicOverlay) findViewById(R.id.overlay);
        ziv = (RotatableZoomableImageView) findViewById(R.id.ivMainImageMainActivity);
        zoom = (ImageButton) findViewById(R.id.zoomButton);
        next = (ImageView) findViewById(R.id.next);
        previous = (ImageView) findViewById(R.id.previous);
        settings = (ImageView) findViewById(R.id.settings);
        info = (ImageView) findViewById(R.id.info);
        zoomBar = (SeekBar) findViewById(R.id.seekBar);
        download = (ImageView) findViewById(R.id.download);
        scan = (ImageView) findViewById(R.id.scan);
        restart = (ImageButton) findViewById(R.id.restart);
        flashlight = (ImageButton) findViewById(R.id.flashlight);
        mHandler = new Handler();

        setImage(zoom, R.raw.zoomin);

        scan.setOnTouchListener(new View.OnTouchListener() {
            /**
             * When touch event occurs, handles lift and a simple touch & hold
             * @param v
             * @param event
             * @return
             */
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    //sets boolean touch to true
                    if(getText().equals(Holder.searchWord)){
                        saySomething("Please Enter Query");
                    }else {
                        cameraSource.touch = true;
                    }
                } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                    //sets boolean lift to true if the touch variable was true
                    if (cameraSource.touch) {
                        cameraSource.lift = true;
                    } else cameraSource.lift = false;
                    cameraSource.touch = false;
                }
                return true;
            }
        });

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        KeyboardUtils.addKeyboardToggleListener(this, new KeyboardUtils.SoftKeyboardToggleListener() {
            @Override
            public void onToggleSoftKeyboard(boolean isVisible) {
                Log.d("keyboard", "keyboard visible: " + isVisible);
                keyboardVisible = isVisible;
            }
        });

        search.setCursorVisible(true);


        //initialized camera source
        if (allPermissionsGranted()) {
            initCameraSource();
            startCameraSource();
        } else {
            getRuntimePermissions();
        }

        // Check to see if we have Text To Speech (TTS) voice data
        Intent ttsIntent = new Intent();
        ttsIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(ttsIntent, ACT_CHECK_TTS_DATA);

        //reset variable values
        wordFound = false;
        if (processor != null) {
            processor.setFoundOnce(false);
        }

        zoomBar.setOnSeekBarChangeListener(a);

        //set initial visibility of views
        ziv.setVisibility(View.GONE);
        zoom.setVisibility(View.GONE);
        next.setVisibility(View.GONE);
        previous.setVisibility(View.GONE);
        findViewById(R.id.download).setVisibility(View.GONE);
        preview.setVisibility(View.VISIBLE);
        settings.setVisibility(View.VISIBLE);
        findViewById(R.id.flashlight).setVisibility(View.VISIBLE);
        zoomBar.setVisibility(View.VISIBLE);
        scan.setVisibility(View.VISIBLE);
        findViewById(R.id.cameraPreview).setVisibility(View.VISIBLE);
        findViewById(R.id.blackLinear).setVisibility(View.GONE);
        findViewById(R.id.infoLayout).setVisibility(View.GONE);
        restart.setVisibility(View.GONE);
        search.setText(Holder.searchWord);
        cameraCapture = false;

        setImage(flashlight, R.raw.flashlight_invert);
        ziv.clearAnimation();
        //starts detection thread
        startFoundOnceThread();
        startZoomedThread();

        if (savedInstanceState != null) {
            mPreferences = getSharedPreferences(sharedPrefFile, MODE_PRIVATE);
            compProg = true;
            int zoomVal = mPreferences.getInt("zoom", 0);
            checked = mPreferences.getBoolean("checked", false);
            zoomBar.setProgress(zoomVal);
            updateZoom();
            compProg = false;
            scheme = ClassificationScheme.valueOf(mPreferences.getString("region", ClassificationScheme.NONE.name()));
            String val = mPreferences.getString("language","");
            locale = generateLocale(val);
            if(locale != null && mTTS != null) {
                changeLanguageLocale(locale);
                mTTS.setLanguage(locale);
            }
        } else {
            mPreferences = getSharedPreferences(sharedPrefFile, MODE_PRIVATE);
        }

    }

    private void updateZoom() {
        if(cameraSource.camera != null) {
            cameraSource.zoom(progress / 100.0);
        }
    }

    /**
     * Method that creates the overarching thread that waits until the word is found and highlights
     * the word when found.
     */
    private void startFoundOnceThread() {
        if(bmp != null) {
            bmp = null;
        }
        if (t != null) {
            t.interrupt();
            t = null;
            System.gc();
        }

        if (capture != null) {
            capture.interrupt();
            capture = null;
            System.gc();
        }

        t = new Thread() {
            @Override
            public void run() {

                String found = getText();
                while (!(processor.isFoundOnce() && cameraSource.lift)) {
                    if (cameraSource.lift) {
                        //in case of accidental lift
                        cameraSource.lift = false;
                    }

                    //updates search keyword
                    if (!keyboardVisible) {
                        found = getText();
                    }

                    if (found.equals(Holder.searchWord)) {
                        cameraSource.searching = false;
                    } else {
                        cameraSource.searching = true;
                    }
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        findViewById(R.id.capture).setVisibility(View.GONE);
                        findViewById(R.id.scan).setVisibility(View.GONE);
                    }
                });

                success = true;

                Log.d(TAG, "Found");


                getBitmap(camera);

                while (!pictureDone) {
                    Log.d(TAG, "taking picture");
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        MainActivity.this.playSound();
                    }
                });
                //rotates image 90 degrees
                if(orientation == Orientation.PORTRAIT) {
                    Matrix matrix = new Matrix();
                    matrix.postRotate(90);
                    bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
                }

                String xyz = bmp.getWidth() + "," + bmp.getHeight();
                Log.e("Bitmap Size", xyz);
                Log.e("Bitmap Size", xyz);
                Log.e("Bitmap Size", xyz);
                Log.e("Bitmap Size", xyz);
                Log.e("Bitmap Size", xyz);

                secondTime = true;
                boolean prevFrame = false;
                float angle = 0;
                if (!multi) {
                    //processes latest image for accuracy of the rectangle
                    Rect c = processBitmap(found);
                    if (c == null) {
                        //fallback to previous frame if not found
                        c = processor.getBoundingRect();
                        if (c == null) {
                            //so that app does not crash
                            bmpaltr = bmp;
                            return;
                        }
                        c = new Rect(translateX(c.left), translateY(c.top), translateX(c.right), translateY(c.bottom));
                        if(bmpaltr != null) {
                            bmpaltr = null;
                        }
                        System.gc();
                        bmpaltr = highlightWordFound(bmp, c);
                    }
                    //created highlighted bitmap
                    bmpaltr = highlightWordFound(bmp, c);
                } else {
                    arr = processBitmapMulti(found);
                    if (arr == null || arr.size() < 1) {
                        Rect c = processor.getBoundingRect();
                        if (c == null) {
                            //so that app does not crash
                            bmpaltr = bmp;
                            saySomething(found + getString(R.string.w10));
                        } else {
                            if(bmpaltr != null) {
                                bmpaltr = null;
                            }
                            System.gc();
                            bmpaltr = highlightWordFound(bmp, c);
                            saySomething(found + getString(R.string.w12));
                            prevFrame = true;
                        }
                    } else {
                        //user feedback
                        if(bmpaltr != null) {
                            bmpaltr = null;
                        }
                        System.gc();
                        frameProcessor.sort();
                        angle = showPoints();
                    }
                }

                final boolean foundRect = arr != null;
                if (arr != null) {
                    rot = angle != 0;
                    bmpaltr = highlightWordFound(bmp, arr);
                    final float angleSet = angle;
                    if (rot) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ziv.startAnimation(-(int) angleSet);
                            }
                        });
                    }

                    if(arr.size() == 0){
                        Rect c = processor.getBoundingRect();
                        if (c == null) {
                            //so that app does not crash
                            bmpaltr = bmp;
                            saySomething(found + getString(R.string.w10));
                        } else {
                            if(bmpaltr != null) {
                                bmpaltr = null;
                            }
                            System.gc();
                            bmpaltr = highlightWordFound(bmp, c);
                            saySomething(found + getString(R.string.w12));
                            prevFrame = true;
                        }
                    }else if (arr.size() != 1) {
                        saySomething(found + getString(R.string.w13) + arr.size() + getString(R.string.w14));
                    } else {
                        saySomething(found + getString(R.string.w15));
                    }
                } else {
                    if (!prevFrame) bmpaltr = bmp;
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //sets visibility of components in second part of the app
                        ziv.setImageBitmap(bmpaltr);
                        ziv.setVisibility(View.VISIBLE);
                        zoom.setVisibility(View.VISIBLE);
                        restart.setVisibility(View.VISIBLE);
                        if (debug) {
                            findViewById(R.id.download).setVisibility(View.VISIBLE);
                        }
                        settings.setVisibility(View.GONE);
                        findViewById(R.id.flashlight).setVisibility(View.GONE);
                        preview.setVisibility(View.GONE);
                        zoomBar.setVisibility(View.GONE);
                        scan.setVisibility(View.GONE);
                        findViewById(R.id.cameraPreview).setVisibility(View.GONE);
                        findViewById(R.id.blackLinear).setVisibility(View.VISIBLE);
                        cameraSource.torch(false);
                        startBlinking();
                    }
                });
                //resets values of button visibility
                showZoom();

                if (!foundRect && !prevFrame) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            zoom.setVisibility(View.GONE);
                            restart.setVisibility(View.VISIBLE);
                        }
                    });
                }
            }
        };

        // starts thread
        t.start();
    }

    private float showPoints() {
        ArrayList<Point[]> pointsS = frameProcessor.getPoints();
        if (pointsS != null) {
            ArrayList<Float> slope = new ArrayList<Float>();
            for (Point[] points : pointsS) {
                if (points.length >= 4) {
                    slope.add(slope(points[0], points[1]));
                    slope.add(slope(points[2], points[3]));
                }
            }
            return meanAngle(slope);
        }

        return 0;

    }

    private float meanAngle(ArrayList<Float> slope) {
        float t = 0;
        for (float d : slope) {
            t += d;
        }
        t /= slope.size();
        return (float) Math.toDegrees(Math.atan(t));
    }

    private float medianAngle(ArrayList<Float> slope) {
        float[] arr = new float[slope.size()];
        for (int i = 0; i < slope.size(); i++) {
            arr[i] = slope.get(i);
        }

        Arrays.sort(arr);

        if (arr.length % 2 == 0) {
            return (arr[arr.length / 2] + arr[arr.length / 2 + 1]) / 2;
        } else {
            return arr[arr.length / 2];
        }
    }

    private float slope(Point p1, Point p2) {
        return ((float) (p1.y - p2.y)) / (p1.x - p2.x);
    }

    /**
     * Method to look for a word in the bitmap present in the zoomable image view after initial word
     * is found in the location (for context)
     *
     * @param query
     */
    private void update(String query) {
        zoomIn = true;
        setImage(zoom, R.raw.zoomin);
        if (!multi) {
            //called update method
            frameProcessor.updateRecog(query);
            Rect c = frameProcessor.getR();
            if (c == null) {
                //fallback to previous frame if not found
                c = processor.getBoundingRect();
                if (c == null) {
                    //if word not found in current frame or previous frame
                    vibratePhone();
                    Toast.makeText(this, "Word not Found", Toast.LENGTH_LONG).show();
                    saySomething(getString(R.string.w16));
                }
            }
            if(bmpaltr != null) {
                bmpaltr = null;
            }
            System.gc();
            bmpaltr = highlightWordFound(bmp, c);
        } else {
            //called update method
            frameProcessor.updateRecogMulti(query);
            arr = frameProcessor.getRects();
            if (arr == null || arr.size() < 1) {
                //if word not found in current frame
                vibratePhone();
                Toast.makeText(this, "Word not Found", Toast.LENGTH_LONG).show();
                saySomething(getString(R.string.w17));
            } else {
                if(bmpaltr != null) {
                    bmpaltr = null;
                }
                System.gc();
                bmpaltr = highlightWordFound(bmp, arr);
                frameProcessor.sort();
            }
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //updating highlighting
                ziv.setImageBitmap(bmpaltr);
                ziv.setVisibility(View.VISIBLE);
                findViewById(R.id.blackLinear).setVisibility(View.VISIBLE);
            }
        });
        //updating visibility of buttons
        showZoom();
    }

    /**
     * Method to update visibilty of button
     */
    private void showZoom() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                zoom.setVisibility(View.VISIBLE);
                // if multiple occurrences found
                if (arr != null && arr.size() > 1) {
                    next.setVisibility(View.VISIBLE);
                    previous.setVisibility(View.VISIBLE);
                    zoom.setVisibility(View.GONE);
                }
                // if 1 occurrences found
                // if none found (shows previous highlight to prevent app from crashing)
                if (arr == null || arr.size() <= 1) {
                    zoom.setVisibility(View.VISIBLE);
                    next.setVisibility(View.GONE);
                    previous.setVisibility(View.GONE);
                }
                findViewById(R.id.capture).setVisibility(View.GONE);
                restart.setVisibility(View.VISIBLE);
            }
        });
    }

    /**
     * process last frame (only finds one occurrence)
     *
     * @param found word to look for
     * @return a single rectangle with the location of the found word
     */
    private Rect processBitmap(String found) {
        frameProcessor = new FrameProcessor(this, false);
        frameProcessor.runTextRecognition(bmp, found);
        while (!frameProcessor.isFinished()) {
            Log.w(TAG, "Processing new Frame");
        }
        return frameProcessor.getR();
    }

    /**
     * process last frame (finds one or multiple occurrences)
     *
     * @param found word to look for
     * @return multiple (or just one) rectangles with the location of the found word
     */
    private ArrayList<Rect> processBitmapMulti(String found) {
        frameProcessor = new FrameProcessor(this, true);
        frameProcessor.runTextRecognition(bmp, found);
        while (!frameProcessor.isFinished()) {
            Log.w(TAG, "Processing new Frame");
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return frameProcessor.getRects();
    }

    /**
     * Method to get bitmap from last frame
     *
     * @return bitmap from camera source
     */
    private void getBitmap() {
        byte[] arr = cameraSource.getData();
        YuvImage yuvImage = new YuvImage(arr, ImageFormat.NV21, cameraSource.getWidth(), cameraSource.getHeight(), null);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, cameraSource.getWidth(), cameraSource.getHeight()), 100, os);
        byte[] jpegByteArray = os.toByteArray();
        bmp = BitmapFactory.decodeByteArray(jpegByteArray, 0, jpegByteArray.length);

        if (cameraSource.getRotation() == 3) {
            Matrix matrix = new Matrix();
            matrix.postRotate(180);
            bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
        }
    }

    private void getBitmap(boolean camera) {
        if (!camera) {
            pictureDone = true;
            getBitmap();
        } else {
            cameraSource.camera.takePicture(null, null, new Camera.PictureCallback() {

                @Override
                public void onPictureTaken(byte[] data, Camera camera) {

                    File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
                    if (pictureFile == null) {
                        Log.d(TAG, "Error creating media file, check storage permissions: ");
                        return;
                    }

                    try {
                        FileOutputStream fos = new FileOutputStream(pictureFile);
                        fos.write(data);
                        fos.close();
                        String filepath = pictureFile.getAbsolutePath();
                        bmp = BitmapFactory.decodeFile(filepath);
                        pictureDone = true;
                        ExifInterface exif = new ExifInterface(pictureFile.getAbsolutePath());
                        if (exif != null) {
                            Log.d("EXIF value", exif.getAttribute(ExifInterface.TAG_ORIENTATION));
                            if (exif.getAttribute(ExifInterface.TAG_ORIENTATION).equalsIgnoreCase("6")) {
                                cameraPictureRotation = 90;
                            } else if (exif.getAttribute(ExifInterface.TAG_ORIENTATION).equalsIgnoreCase("8")) {
                                cameraPictureRotation = 270;
                            } else if (exif.getAttribute(ExifInterface.TAG_ORIENTATION).equalsIgnoreCase("3")) {
                                cameraPictureRotation = 180;
                            }
                        }
                    } catch (FileNotFoundException e) {
                        Log.d(TAG, "File not found: " + e.getMessage());
                    } catch (IOException e) {
                        Log.d(TAG, "Error accessing file: " + e.getMessage());
                    }
                }
            });
        }
    }

    /**
     * when app resumes
     */
    @Override
    public void onResume() {
        super.onResume();
        Logging.log(TAG, "onResume");
        startCameraSource();

        mPreferences = getSharedPreferences(sharedPrefFile, MODE_PRIVATE);
        compProg = true;
        int zoomVal = mPreferences.getInt("zoom", 0);
        checked = mPreferences.getBoolean("checked", false);
        zoomBar.setProgress(zoomVal);
        performInitialZoom();
        compProg = false;
        scheme = ClassificationScheme.valueOf(mPreferences.getString("region", ClassificationScheme.NONE.name()));

    }

    private void performInitialZoom() {

        if (cameraSource.camera == null)
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    performInitialZoom();
                }
            }, 50);
        else
            updateZoom();
    }

    /**
     * when app is paused
     */
    @Override
    protected void onPause() {
        Logging.log(TAG, "onPause");
        super.onPause();
        preview.stop();

        SharedPreferences.Editor preferencesEditor = mPreferences.edit();
        preferencesEditor.clear();
        preferencesEditor.putInt("zoom", zoomBar.getProgress());
        preferencesEditor.putString("region", scheme.name());
        if(locale != null){
            preferencesEditor.putString("language", locale.getDisplayLanguage().toLowerCase());
            Log.e("Pause", locale.getDisplayLanguage().toLowerCase());
        }
        preferencesEditor.putBoolean("checked", checked);
        preferencesEditor.apply();
    }

    /**
     * when app is destroyed
     */
    @Override
    public void onDestroy() {
        Logging.log(TAG, "destroyed");
        super.onDestroy();
        if (cameraSource != null) {
            cameraSource.release();
        }

        if (capture != null) {
            capture.interrupt();
            capture = null;
        }

        if (t != null) {
            t.interrupt();
            t = null;
        }

        if (zoomThread != null) {
            zoomThread.interrupt();
            zoomThread = null;
        }

        if (mTTS != null) {
            mTTS.stop();
            mTTS.shutdown();
        }

        mHandler.removeCallbacksAndMessages(null);

        finish();
    }

    /**
     * Checks whether all permisions are granted
     *
     * @return boolean whether the permisions are present
     */
    private boolean allPermissionsGranted() {
        for (String permission : getRequiredPermissions()) {
            if (!isPermissionGranted(this, permission)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if one permission is present
     *
     * @param context    the activity from which method is called
     * @param permission the permission to check
     * @return boolean whether the permisions are present
     */
    private static boolean isPermissionGranted(Context context, String permission) {
        if (ContextCompat.checkSelfPermission(context, permission)
                == PackageManager.PERMISSION_GRANTED) {
            Logging.log(TAG, "Permission granted: " + permission);
            return true;
        }
        Logging.log(TAG, "Permission NOT granted: " + permission);
        return false;
    }

    /**
     * Method to determine which permssions to ask for
     *
     * @return returns permissions that are necessary
     */
    private String[] getRequiredPermissions() {
        try {
            PackageInfo info =
                    this.getPackageManager()
                            .getPackageInfo(this.getPackageName(), PackageManager.GET_PERMISSIONS);
            String[] ps = info.requestedPermissions;
            if (ps != null && ps.length > 0) {
                return ps;
            } else {
                return new String[0];
            }
        } catch (Exception e) {
            return new String[0];
        }
    }

    /**
     * Method to get all required permssions for application
     */
    private void getRuntimePermissions() {
        List<String> allNeededPermissions = new ArrayList<>();
        for (String permission : getRequiredPermissions()) {
            if (!isPermissionGranted(this, permission)) {
                allNeededPermissions.add(permission);
            }
        }

        if (!allNeededPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this, allNeededPermissions.toArray(new String[0]), PERMISSION_REQUESTS);
        }
    }

    /**
     * After permissions are requested, this method initializes the camera source only if all of the
     * permissions for the application are granted
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        Logging.log(TAG, "Permission granted!");
        if (allPermissionsGranted()) {
            initCameraSource();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /**
     * Initializes the camera and the OCR Processor
     * Sets the OCR processor to analyze each frame of the camera
     */
    private void initCameraSource() {
        cameraSource = new CameraSource(this, graphicOverlay);
        processor = new OcrProcessor(this);
        cameraSource.setMachineLearningFrameProcessor(processor);
    }

    /**
     * Initializes the preview of the camera
     */
    private void startCameraSource() {
        if (cameraSource != null) {
            try {
                if (preview == null) {
                    Logging.log(TAG, "resume: Preview is null");
                }
                if (graphicOverlay == null) {
                    Logging.log(TAG, "resume: graphOverlay is null");
                }

                preview.setBackgroundColor(Color.BLACK);

                preview.start(cameraSource, graphicOverlay);

                DisplayMetrics displayMetrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

                ViewGroup.LayoutParams videoLayoutParams = preview.getLayoutParams();
                videoLayoutParams.width = displayMetrics.widthPixels;
                videoLayoutParams.height = displayMetrics.heightPixels;
                preview.setLayoutParams(videoLayoutParams);

                ViewGroup.LayoutParams videoParams = preview.surfaceView.getLayoutParams();
                videoParams.width = displayMetrics.widthPixels;
                videoParams.height = displayMetrics.heightPixels;
                preview.surfaceView.setLayoutParams(videoLayoutParams);

            } catch (IOException e) {
                Logging.logError(TAG, "Unable to start camera source.", e);
                cameraSource.release();
                cameraSource = null;
            }
        }
    }


    /**
     * Method to vibrate phone default amount
     */
    public void vibratePhone() {
        vibratePhone(MainActivity.vibrateLength);
    }

    /**
     * Method to vibrate phone a specified amount
     *
     * @param millis amount milliseconds vibrated
     */
    public void vibratePhone(long millis) {
        Vibrator v = this.getSystemService(Vibrator.class);
        if (v.hasVibrator())
            v.vibrate(millis);
    }


    //Processing get/set methods
    public void setWordFound(boolean b) {
        wordFound = b;
    }

    public boolean getWordFound() {
        return wordFound;
    }

    public ClassificationScheme getScheme() {
        return scheme;
    }

    public String getText() {
        return search.getText().toString();
    }

    /**
     * When restart button is pressed, this method is called
     *
     * @param view
     */
    public void restart(View view) {
        ziv.clearAnimation();
        restart();
    }

    /**
     * finishes current activity and creates new instance of the activity
     */

    private void forceRestart() {
        Intent intent= new Intent(this, MainActivity.class);
        overridePendingTransition(0, 0);
        finish();
        overridePendingTransition(0, 0);
        startActivity(intent);
    }

    private void restart() {
        stopBlinking();
        if (getText().equals(Holder.searchWord) && !secondTime) {
            return;
        }
        if (t != null) {
            t.interrupt();
            t = null;
        }

        //reset variable values
        wordFound = false;
        if (processor != null) {
            processor.setFoundOnce(false);
        }
        secondTime = false;
        processingDone = false;
        cameraCapture = false;
        success = false;
        num = 0;
        multi = true;
        nextB = true;
        if(bmp != null) {
            bmp = null;
        }
        bmphil = null;
        if(bmpaltr != null) {
            bmpaltr = null;
        }
        zoomIn = true;
        setImage(zoom, R.raw.zoomin);
        camera = false;

        if (frameProcessor != null) {
            frameProcessor.setFinished();
        }
        pictureDone = false;


        //set initial visibility of views
        ziv.setVisibility(View.GONE);
        zoom.setVisibility(View.GONE);
        next.setVisibility(View.GONE);
        findViewById(R.id.download).setVisibility(View.GONE);
        previous.setVisibility(View.GONE);
        preview.setVisibility(View.VISIBLE);
        settings.setVisibility(View.VISIBLE);
        findViewById(R.id.flashlight).setVisibility(View.VISIBLE);
        zoomBar.setVisibility(View.VISIBLE);
        scan.setVisibility(View.VISIBLE);
        findViewById(R.id.capture).setVisibility(View.VISIBLE);
        findViewById(R.id.cameraPreview).setVisibility(View.VISIBLE);
        findViewById(R.id.blackLinear).setVisibility(View.GONE);
        restart.setVisibility(View.GONE);
        findViewById(R.id.infoLayout).setVisibility(View.GONE);

        //initialized camera source
        if (allPermissionsGranted()) {
            initCameraSource();
            startCameraSource();
        } else {
            getRuntimePermissions();
        }

        updateZoom();

        cameraSource.torch(flashlightOn);
        if (flashlightOn) {
            setImage(flashlight, R.raw.flashlight);
        } else {
            setImage(flashlight, R.raw.flashlight_invert);
        }

        startFoundOnceThread();
    }

    /**
     * Method called to underline the word
     *
     * @param mutableBitmap bitmap to edit
     * @param rect          the rectangle of the word to underline
     * @return changed bitmap
     */
    Bitmap highlightWordFound(Bitmap mutableBitmap, Rect rect) {
        //set color and style of the paint
        Paint mPaint = new Paint();
        mPaint.setColor(Color.GREEN);
        mPaint.setStyle(Paint.Style.STROKE);
        if (camera) {
            mPaint.setStrokeWidth(PICTURE_STROKE);
        } else {
            mPaint.setStrokeWidth(VIDEO_STROKE);
        }

        //copy bitmap and access the canvas of the bitmap
        mutableBitmap = mutableBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBitmap);
        canvas.save();

        //coordinates for line of the bitmap
        int leftX = rect.centerX() - rect.width() / 2;
        int Y = rect.centerY() + rect.height() / 2;
        int rightX = rect.centerX() + rect.width() / 2;

        //underline for the word placed on canvas
        canvas.drawLine(leftX, Y, rightX, Y, mPaint);
        return mutableBitmap;
    }

    /**
     * Underlining multiple words
     *
     * @param mutableBitmap bitmap to edit
     * @param r             the list of rectangles of the word to underline
     * @return changed bitmap
     */
    Bitmap highlightWordFound(Bitmap mutableBitmap, ArrayList<Rect> r) {
        //set color and style of the paint
        Paint mPaint = new Paint();
        mPaint.setColor(Color.GREEN);
        mPaint.setStyle(Paint.Style.STROKE);

        if (camera) {
            mPaint.setStrokeWidth(PICTURE_STROKE);
        } else {
            mPaint.setStrokeWidth(VIDEO_STROKE);
        }

        //copy bitmap and access the canvas of the bitmap
        mutableBitmap = mutableBitmap.copy(Bitmap.Config.ARGB_8888, true);

        Canvas canvas = new Canvas(mutableBitmap);
        canvas.save();

        //For each rectangle in the list
        if (rot && frameProcessor.getWordPoints() != null && frameProcessor.getWordPoints().size() >= 1) {
            for (Point[] points : frameProcessor.getWordPoints()) {
                canvas.drawLine(points[2].x, points[2].y, points[3].x, points[3].y, mPaint);
            }
        } else {
            for (Rect rect : r) {
                //coordinates for line of the bitmap
                int leftX = rect.centerX() - rect.width() / 2;
                int Y = rect.centerY() + rect.height() / 2;
                int rightX = rect.centerX() + rect.width() / 2;
                //underline for the word placed on canvas
                canvas.drawLine(leftX, Y, rightX, Y, mPaint);
            }
        }

        return mutableBitmap;
    }

    /**
     * Adjusts zoom to certain width
     *
     * @param width width of the underlined word'altsearch rectangle
     */
    private void adjustZoomLevel(int width) {

        // If the next iteration in zoom level exceeds the with of the holder then return
        if(camera) {
            if ((ziv.getMaxScale() * (3 * width)) >= ziv.getWidth()) {
                ziv.setMaxScale(ziv.getMaxScale() - 0.1f);
                adjustZoomLevel(width);
            }
        }else{
            if ((ziv.getMaxScale() * width) >= ziv.getWidth()) {
                ziv.setMaxScale(ziv.getMaxScale() - 0.1f);
                adjustZoomLevel(width);
            }
        }

        if (ziv.getMaxScale() == 0)
            ziv.setMaxScale(1);
    }

    /**
     * Method to zoom into a certain rectangle on the screen
     *
     * @param r
     */
    void zoomWord(Rect r) {
        if (ziv.isNotZoomed()) {
            adjustZoomLevel(r.width());
            int x_tap = r.centerX();
            int y_tap = r.centerY();

            ziv.externalDoubleTapGestureGenerator(x_tap, y_tap);
            Log.w("Location: ", "( " + x_tap + " , " + y_tap + ")");
        } else {
            ziv.setMaxScale(ziv.getCurrentScale());
            ziv.externalDoubleTapGestureGenerator(ziv.getWidth() / 2, ziv.getHeight() / 2);

            ziv.resetMaxScale();
            adjustZoomLevel(r.width());
            int x_tap = r.centerX();
            int y_tap = r.centerY();

            performSecondTap(x_tap, y_tap);
            Log.w("Location: ", "( " + x_tap + " , " + y_tap + ")");
        }
    }

    private void performSecondTap(final float x, final float y) {

        if (ziv.isAnimating())
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    performSecondTap(x, y);
                }
            }, 500);
        else
            ziv.externalDoubleTapGestureGenerator(x, y);
    }

    /**
     * When the zoom button is clicked, this method is called
     *
     * @param view
     */
    public void zoom(View view) {
        zoom();
    }

    /**
     * Checks whether there are multiple words to zoom into and zooms in accordingly.
     */
    public void zoom() {
        if (processor.isFoundOnce()) {
            if (!multi) {
                //if only one word
                zoomWord(processor.getBoundingRectTrans(), true);
            } else {
                if (arr == null || arr.size() < 1) {
                    zoomWord(processor.getBoundingRectTrans(), true);
                } else {
                    //cyclic remainder
                    num = (num % arr.size() + arr.size()) % arr.size();
                    Rect r = arr.get(num);
                    //need to translate the position of the rectangle according to the bitmap displayed
                    Rect transR = new Rect(translateX(r.left), translateY(r.top), translateX(r.right), translateY(r.bottom));
                    //zooms to specific word
                    if (arr.size() == 1) {
                        zoomWord(transR, true);
                    } else {
                        zoomWord(transR, false);
                    }
                }
            }
        } else if (cameraCapture) {
            if (arr == null || arr.size() < 1) {
                saySomething(getString(R.string.w18));
            } else {
                //cyclic remainder
                num = (num % arr.size() + arr.size()) % arr.size();
                Rect r = arr.get(num);
                //need to translate the position of the rectangle according to the bitmap displayed
                Rect transR = new Rect(translateX(r.left), translateY(r.top), translateX(r.right), translateY(r.bottom));
                //zooms to specific word
                if (arr.size() == 1) {
                    zoomWord(transR, true);
                } else {
                    zoomWord(transR, false);
                }
            }
        }
    }

    private void toggleZoomIcon() {
        if (zoomIn) {
            setImage(zoom, R.raw.zoomout);
            zoomIn = false;
        } else {
            setImage(zoom, R.raw.zoomin);
            zoomIn = true;
        }
    }

    private void zoomWord(Rect r, boolean b) {
        if (b) {
            toggleZoomIcon();
            if (ziv.isNotZoomed()) {
                adjustZoomLevel(r.width());
                int x_tap = r.centerX();
                int y_tap = r.centerY();

                ziv.externalDoubleTapGestureGenerator(x_tap, y_tap);
                Log.w("Location: ", "( " + r.right + " , " + r.bottom + ")");
            } else {
                ziv.setMaxScale(ziv.getCurrentScale());
                ziv.externalDoubleTapGestureGenerator(ziv.getWidth() / 2, ziv.getHeight() / 2);
                ziv.resetMaxScale();
            }
        } else {
            zoomWord(r);
        }
    }

    public int translateX(int x) {
        float newWidth = ziv.getWidth();
        float oldWidth = bmp.getWidth();
        return (int) (x / oldWidth * newWidth);
    }

    public int translateY(int x) {
        float newHeight = ziv.getHeight();
        float oldHeight = bmp.getHeight();
        return (int) (x / oldHeight * newHeight);
    }

    /**
     * When the next button is clicked, this method is called
     *
     * @param view
     */
    public void next(View view) {
        zoomAndReset(1);
    }

    /**
     * When the previous button is clicked, this method is called
     *
     * @param view
     */
    public void previous(View view) {
        zoomAndReset(-1);
    }

    /**
     * zoom logic for next and previous buttons
     *
     * @param i increment by which to skip buy
     */
    public void zoomAndReset(int i) {
        zoom();
        num += i;
    }

    /**
     * Checks if there is voice input or voice output needed
     * If there is a change when the activity is in the second state (i.e. when the zoomable
     * imageview is present), then the update method is called to find a different word on the
     * screen.
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //voice input
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            List<String> results = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            //spoken text from google voice
            String spokenText = results.get(0);
            //set edit text'altsearch value to the spoken text
            search.setText(spokenText);
            searchVal = spokenText;
            //user output
            if (search.getText().toString().equals(Holder.searchWord)) {
                saySomething(getString(R.string.w19));
            }else {
                saySomething(getString(R.string.w20)+ " " + search.getText().toString());
            }
            if (success && secondTime) {
                // updates search query if on zoomable image view screen
                update(spokenText);

                if (arr.size() != 1) {
                    if (arr.size() == 0) {
                        saySomething(spokenText + " " + getString(R.string.w21));
                    } else {
                        saySomething(spokenText + " " + getString(R.string.w22) + " " + arr.size() + " " + getString(R.string.w23));
                    }
                } else {
                    saySomething(spokenText + " " + getString(R.string.w24));
                }
            } else if (cameraCapture) {

                update(spokenText);
                //TODO check if word is found
                if (arr != null && arr.size() != 1) {
                    if (arr.size() == 0) {
                        saySomething(spokenText + " " + getString(R.string.w25));
                    } else {
                        saySomething(spokenText + " " + getString(R.string.w26) + arr.size() + " " + getString(R.string.w27));
                    }
                } else {
                    saySomething(spokenText + " " + getString(R.string.w28));
                }
            }

            compProg = true;
            zoomBar.setProgress(0);
            flashlightOn = false;
            compProg = false;
        }
        //voice output
        if (requestCode == ACT_CHECK_TTS_DATA) {
            if (resultCode ==
                    TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                // Data exists, so we instantiate the TTS engine
                mTTS = new TextToSpeech(this, this);
            } else {
                // Data is missing, so we start the TTS installation
                // process
                Intent installIntent = new Intent();
                installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivity(installIntent);
            }
        }

        //called superclass method
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * When the voice input button is clicked, this method is called
     *
     * @param view
     */
    public void speech(View view) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        String val = locale.getLanguage();
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, val);
        startActivityForResult(intent, SPEECH_REQUEST_CODE);
    }

    /**
     * When the select classification scheme button is clicked, this method is called
     * Creates Alert dialog and forces you to click one of the four schemes of classification
     */
    public void setSchemeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Please Select Scanning Region of Interest");
        builder.setItems(new CharSequence[]
                        {"Whole Screen", "Center", "Top", "Bottom"},
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // The 'which' argument contains the index position of the selected item
                        switch (which) {
                            case 0:
                                scheme = ClassificationScheme.NONE;
                                saySomething(getString(R.string.w29));
                                break;
                            case 1:
                                scheme = ClassificationScheme.FIND_AT_CENTER;
                                saySomething(getString(R.string.w30));
                                break;
                            case 2:
                                scheme = ClassificationScheme.FIND_AT_TOP;
                                saySomething(getString(R.string.w31));
                                break;
                            case 3:
                                scheme = ClassificationScheme.FIND_AT_BOTTOM;
                                saySomething(getString(R.string.w32));
                                break;
                        }
                    }
                });
        builder.create().show();
    }

    public void setLanguageDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.Language);
        builder.setItems(new CharSequence[]
                        {getString(R.string.l1), getString(R.string.l2), getString(R.string.l3), getString(R.string.l4), getString(R.string.l5), getString(R.string.l6), getString(R.string.l7)},
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // The 'which' argument contains the index position of the selected item
                        switch (which) {
                            case 1:
                                locale = generateLocale("spanish");
                                break;
                            case 2:
                                locale = generateLocale("italian");
                                break;
                            case 3:
                                locale = generateLocale("german");
                                break;
                            case 4:
                                locale = generateLocale("french");
                                break;
                            case 5:
                                locale = generateLocale("danish");
                                break;
                            case 6:
                                locale = generateLocale("portuguese");
                                break;
                            default:
                                locale = generateLocale("english");
                                break;
                        }
                        mTTS.setLanguage(locale);
                        changeLanguageLocale(locale);
                        saySomething(getString(R.string.w33) + " " + locale.getDisplayLanguage());
                    }
                });
        builder.create().show();
    }

    public void setInfoDialog() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        WebView wv = new WebView(this);
        wv.loadUrl(getString(R.string.filename));

        alert.setView(wv);
        alert.setNegativeButton("Contact Us", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                EmailIntentBuilder.from(MainActivity.this).to("221710304022@gitam.in")
                        .subject("I love your app !!")
                        .body("Thanks for making this app available it makes my life so easy....").start();
            }
        });
        alert.setPositiveButton("Close", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        alert.show();
    }

    private void setOrientationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.m4));
        builder.setItems(new CharSequence[]
                        {"Portrait", "Landscape"},
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // The 'which' argument contains the index position of the selected item
                        switch (which) {
                            case 0:
                                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                                orientation = Orientation.PORTRAIT;
                                break;
                            case 1:
                                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                                orientation = Orientation.LANDSCAPE;
                                break;
                        }
                    }
                });
        builder.create().show();
    }

    public void cameraSizeInfoDialog(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Photo Resolution");
        if(sizes != null) {
            builder.setMessage(sizes);
        }else{
            sizes = "";
            ArrayList cameras = new ArrayList<Integer>();
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            for (int i = 0; i < Camera.getNumberOfCameras(); ++i) {
                Camera.getCameraInfo(i, cameraInfo);
                if (cameraInfo.facing == CameraSource.CAMERA_FACING_BACK) {
                    Camera c = Camera.open(i);
                    Camera.Parameters parameters = c.getParameters();
                    Camera.Size size = parameters.getSupportedPictureSizes().get(0);
                    int cameraNum = Camera.getNumberOfCameras();
                    sizes = sizes + size.width + " x " + size.height + "\n";
                }
            }
            builder.setMessage(sizes);
        }
        builder.show();
    }

    /**
     * Hides soft keyboard
     *
     * @param activity static method to be called from any activity
     */
    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = activity.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    /**
     * Initializes text to speech module
     *
     * @param status
     */
    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            if (mTTS != null) {
                if(locale == null){
                    locale = ConfigurationCompat.getLocales(Resources.getSystem().getConfiguration()).get(0);
                }
                Log.e("Language Selection", locale.getDisplayLanguage());
                int result;
                if(!isValidLanguage(locale)){
                    result = mTTS.setLanguage(Locale.US);
                    locale = Locale.US;
                }else{
                    result = mTTS.setLanguage(locale);
                }
                if (result == TextToSpeech.LANG_MISSING_DATA ||
                        result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(this, "TTS language is not supported", Toast.LENGTH_LONG).show();
                }
            }
        } else {
            Toast.makeText(this, "TTS initialization failed",
                    Toast.LENGTH_LONG).show();
        }
    }

    private boolean isValidLanguage(Locale locale) {
        switch (locale.getDisplayLanguage().toLowerCase()){
            case "english":
            case "spanish":
            case "italian":
            case "german":
            case "french":
                return true;
        }
        return false;
    }

    private Locale generateLocale(String str){
        switch (str){
            case "english":
                return Locale.US;
            case "spanish":
                return new Locale("es", "ES");
            case "italian":
                return Locale.ITALY;
            case "german":
                return Locale.GERMANY;
            case "french":
                return Locale.FRANCE;
            case "danish":
                return new Locale("da", "DK");
            case "portuguese":
                return new Locale("pt","BR");
        }
        return Locale.US;
    }

    /**
     * Method to speak something (not default)
     *
     * @param text  what to speak
     * @param qmode can either be an addition to the queue or a flush
     */
    private void saySomething(String text, int qmode) {
        if (qmode == 1)
            mTTS.speak(text, TextToSpeech.QUEUE_ADD, null);
        else
            mTTS.speak(text, TextToSpeech.QUEUE_FLUSH, null);
    }

    /**
     * Method to speak something (default)
     *
     * @param text what to speak
     */
    private void saySomething(String text) {
        mTTS.setLanguage(locale);
        saySomething(text, 0);
    }


    // =============================================================================================
    // Photo Capture Variables and Methods
    // =============================================================================================
    private boolean pictureDone = false;
    private boolean camera = false;

    /**
     * Create a File for saving an image or video
     */
    private File getOutputMediaFile(int type) {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(this.getApplicationContext().getExternalFilesDir(
                Environment.DIRECTORY_PICTURES), "Alt Vision");

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("Alt Vision", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_" + timeStamp + ".jpg");
        } else if (type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_" + timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }

    /**
     * Create a File for saving an image or video
     */
    private File getOutputMediaFile() {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), "Alt Vision");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("Alt Vision", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile = new File(mediaStorageDir.getPath() + File.separator + "OCR_" + timeStamp + ".txt");

        return mediaFile;
    }

    public void playSound() {
        MediaPlayer.create(this, R.raw.beep).start();
    }

    public void settings(View view) {
        ContextWrapper wrapper = new ContextThemeWrapper(this, R.style.popup);
        PopupMenu popup = new PopupMenu(wrapper, view);
        popup.setOnMenuItemClickListener(this);
        setForceShowIcon(popup);
        popup.inflate(R.menu.actions);
        popup.getMenu().getItem(0).setChecked(this.checked);
        popup.show();
    }

    public static void setForceShowIcon(PopupMenu popupMenu) {
        try {
            Field[] fields = popupMenu.getClass().getDeclaredFields();
            for (Field field : fields) {
                if ("mPopup".equals(field.getName())) {
                    field.setAccessible(true);
                    Object menuPopupHelper = field.get(popupMenu);
                    Class<?> classPopupHelper = Class.forName(menuPopupHelper
                            .getClass().getName());
                    Method setForceIcons = classPopupHelper.getMethod(
                            "setForceShowIcon", boolean.class);
                    setForceIcons.invoke(menuPopupHelper, true);
                    break;
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.setScheme:
                boolean checked = item.isChecked();
                if (checked) {
                    scheme = ClassificationScheme.NONE;
                    item.setChecked(false);
                    this.checked = false;
                    saySomething(getString(R.string.w34));
                    Toast.makeText(this, "Finding in the Whole Screen", Toast.LENGTH_LONG).show();
                } else {
                    scheme = ClassificationScheme.FIND_AT_CENTER;
                    item.setChecked(true);
                    this.checked = true;
                    saySomething(getString(R.string.w35));
                    Toast.makeText(this, "Finding in the Center Slice", Toast.LENGTH_LONG).show();
                }
                return true;
            case R.id.orientationScan:
                setOrientationDialog();
                return true;
            case R.id.language:
                setLanguageDialog();
                return true;
            case R.id.infoPopup:
                setInfoDialog();
                return true;
            default:
                return false;
        }
    }


    public void download(Bitmap bmp, String title, String description) {
        MediaStore.Images.Media.insertImage(getContentResolver(), bmp, title, description);
    }

    public void download(View view) {
        String str = frameProcessor.getText();
        if (str != null) {
            download(bmpaltr, "OCR PICTURE", str);

            File docFile = getOutputMediaFile();
            if (docFile == null) {
                Log.d(TAG, "Error creating media file, check storage permissions: ");
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(docFile);
                fos.write(str.getBytes());
                fos.close();
                String filepath = docFile.getAbsolutePath();
                MediaScannerConnection.scanFile(MainActivity.this, new String[]{filepath}, null, null);
            } catch (FileNotFoundException e) {
                Log.d(TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            }
            Toast.makeText(getApplicationContext(), "Saved Image to Camera Roll", Toast.LENGTH_LONG).show();
        }
    }

    public void capture(View view) {
        zoomIn = true;
        setImage(zoom, R.raw.zoomin);

        findViewById(R.id.capture).setVisibility(View.GONE);
        findViewById(R.id.scan).setVisibility(View.GONE);

        if(bmp != null) {
            bmp = null;
        }

        if (t != null) {
            t.interrupt();
            t = null;
            System.gc();
        }

        if (capture != null) {
            capture.interrupt();
            capture = null;
            System.gc();
        }

        capture = new Thread() {
            @Override
            public void run() {
                camera = true;
                getBitmap(true);

                while (!pictureDone) {
                    Log.d(TAG, "taking picture");
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                final String[] found = new String[]{searchVal};
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        MainActivity.this.playSound();
                    }
                });

                //rotates image 90 degrees
                if (bmp.getHeight() < bmp.getWidth() && orientation == Orientation.PORTRAIT) {
                    Matrix matrix = new Matrix();
                    matrix.postRotate(90);
                    bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
                }else if (bmp.getHeight() > bmp.getWidth() && orientation == Orientation.LANDSCAPE) {
                    Matrix matrix = new Matrix();
                    matrix.postRotate(90);
                    bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
                }

                boolean captureFailed = false;
                secondTime = true;
                float angle = 0;
                arr = processBitmapMulti(found[0]);
                if (arr == null || arr.size() < 1) {
                    bmpaltr = bmp;
                    if (found[0].equals(Holder.searchWord)) {
                        saySomething(getString(R.string.w36));
                    } else {
                        final boolean[] ui = {false};
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                update(found[0]);
                                if (arr == null) {
                                    saySomething(found[0] + " " + getString(R.string.w37));
                                } else {
                                    if (arr.size() != 1) {
                                        if (arr.size() == 0) {
                                            saySomething(found[0] + " " + getString(R.string.w38));
                                        } else {
                                            saySomething(found[0] + " " + getString(R.string.w39) + " " + arr.size() + " " + getString(R.string.w40));
                                        }
                                    } else {
                                        saySomething(found[0] + " " + getString(R.string.w41));
                                    }
                                }
                                ui[0] = true;
                                showZoom();
                            }
                        });
                        while (!ui[0]) {

                        }
                        if (arr != null) {
                            angle = showPoints();
                        }
                    }
                } else {
                    //user feedback
                    bmpaltr = bmp;
                    System.gc();
                    frameProcessor.sort();
                    angle = showPoints();
                }

                rot = true;
                if (arr != null) {
                    bmpaltr = highlightWordFound(bmp, arr);
                }
                final float angleSet = angle;
                if (rot) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ziv.startAnimation(-(int) angleSet);
                        }
                    });
                }

                if (arr == null) {
                    if (found[0].equals(Holder.searchWord)) {
                        saySomething(getString(R.string.w42));
                    } else {
                        saySomething(found[0] + " " + getString(R.string.w43));
                    }
                    captureFailed = true;
                } else {
                    if (arr.size() != 1) {
                        if (found[0].equals(Holder.searchWord)) {
                            saySomething(getString(R.string.w44));
                        } else {
                            if (arr.size() == 0) {
                                saySomething(found[0] + " " + getString(R.string.w45));
                                captureFailed = true;
                            } else {
                                saySomething(found[0] + " " + getString(R.string.w46) + arr.size() + " " + getString(R.string.w47));
                            }
                        }
                    } else {
                        saySomething(found[0] + " " + getString(R.string.w48));
                    }
                }

                cameraCapture = true;

                final boolean finalCaptureFailed = captureFailed;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //sets visibility of components in second part of the app
                        ziv.setImageBitmap(bmpaltr);
                        ziv.setVisibility(View.VISIBLE);
                        zoom.setVisibility(View.VISIBLE);
                        if (debug) {
                            findViewById(R.id.download).setVisibility(View.VISIBLE);
                        }
                        settings.setVisibility(View.GONE);
                        findViewById(R.id.flashlight).setVisibility(View.GONE);
                        preview.setVisibility(View.GONE);
                        zoomBar.setVisibility(View.GONE);
                        scan.setVisibility(View.GONE);
                        restart.setVisibility(View.VISIBLE);
                        findViewById(R.id.capture).setVisibility(View.GONE);
                        findViewById(R.id.cameraPreview).setVisibility(View.GONE);
                        findViewById(R.id.blackLinear).setVisibility(View.VISIBLE);
                        findViewById(R.id.infoLayout).setVisibility(View.VISIBLE);
                        cameraSource.torch(false);

                        zoom.setVisibility(View.VISIBLE);
                        // if multiple occurrences found
                        if (arr != null && arr.size() > 1) {
                            next.setVisibility(View.VISIBLE);
                            previous.setVisibility(View.VISIBLE);
                            zoom.setVisibility(View.GONE);
                        } else if (arr != null && arr.size() == 1) {
                            next.setVisibility(View.GONE);
                            previous.setVisibility(View.GONE);
                            zoom.setVisibility(View.VISIBLE);
                        } else {
                            next.setVisibility(View.GONE);
                            previous.setVisibility(View.GONE);
                            zoom.setVisibility(View.GONE);
                        }

                        if (finalCaptureFailed) {
                            if (arr == null || arr.size() <= 1) {
                                zoom.setVisibility(View.GONE);
                                next.setVisibility(View.GONE);
                                previous.setVisibility(View.GONE);
                                restart.setVisibility(View.VISIBLE);
                            }
                        }
                        restart.setVisibility(View.VISIBLE);
                        startBlinking();
                    }
                });

            }
        };
        capture.start();
    }

    public void toggleFlashlight(View view) {
        if (flashlightOn) {
            cameraSource.torch(false);
            flashlightOn = false;
            setImage(flashlight, R.raw.flashlight_invert);
        } else {
            cameraSource.torch(true);
            flashlightOn = true;
            setImage(flashlight, R.raw.flashlight);
        }
    }

    void setImage(ImageButton view, int imgId) {
        view.setImageResource(0);
        Drawable draw = getResources().getDrawable(imgId);
        view.setImageDrawable(draw);
        view.setScaleType(ImageView.ScaleType.FIT_CENTER);
        view.setAdjustViewBounds(true);
    }

    public void updateZoomBar(int zoom) {
        compProg = true;
        zoomBar.setProgress(zoom);
        compProg = false;
    }

    private void startZoomedThread() {
        if (zoomThread != null) {
            zoomThread.interrupt();
            zoomThread = null;
        }

        zoomThread = new Thread() {
            @Override
            public void run() {
                super.run();
                while (true) {
                    if (ziv.zoomChanged) {
                        if (ziv.isNotZoomed()) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    zoomIn = true;
                                    setImage(zoom, R.raw.zoomin);
                                }
                            });
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    zoomIn = false;
                                    setImage(zoom, R.raw.zoomout);
                                }
                            });
                        }
                        ziv.zoomChanged = false;
                    }
                }
            }
        };
        zoomThread.start();
    }

    private void changeLanguageLocale(Locale locale){
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.e("Orientation", "" + getResources().getConfiguration().orientation);

        int zoomVal = zoomBar.getProgress();
        String searchValue = getText();
        setContentView(R.layout.activity_main);
        initViews();
        updateZoomBar(zoomVal);
        search.setText(searchValue);
    }

    private void initViews() {
        search = (EditText) findViewById(R.id.search);
        oldWidth = search.getWidth();

        search.setOnEditorActionListener(listener);
        preview = (CameraSourcePreview) findViewById(R.id.preview);
        graphicOverlay = (GraphicOverlay) findViewById(R.id.overlay);
        ziv = (RotatableZoomableImageView) findViewById(R.id.ivMainImageMainActivity);
        zoom = (ImageButton) findViewById(R.id.zoomButton);
        next = (ImageView) findViewById(R.id.next);
        previous = (ImageView) findViewById(R.id.previous);
        settings = (ImageView) findViewById(R.id.settings);
        info = (ImageView) findViewById(R.id.info);
        zoomBar = (SeekBar) findViewById(R.id.seekBar);
        download = (ImageView) findViewById(R.id.download);
        scan = (ImageView) findViewById(R.id.scan);
        restart = (ImageButton) findViewById(R.id.restart);
        flashlight = (ImageButton) findViewById(R.id.flashlight);

        //set initial visibility of views
        ziv.setVisibility(View.GONE);
        zoom.setVisibility(View.GONE);
        next.setVisibility(View.GONE);
        previous.setVisibility(View.GONE);
        findViewById(R.id.download).setVisibility(View.GONE);
        preview.setVisibility(View.VISIBLE);
        settings.setVisibility(View.VISIBLE);
        findViewById(R.id.flashlight).setVisibility(View.VISIBLE);
        zoomBar.setVisibility(View.VISIBLE);
        scan.setVisibility(View.VISIBLE);
        findViewById(R.id.cameraPreview).setVisibility(View.VISIBLE);
        findViewById(R.id.blackLinear).setVisibility(View.GONE);
        findViewById(R.id.infoLayout).setVisibility(View.GONE);
        restart.setVisibility(View.GONE);
        search.setText(Holder.searchWord);
        cameraCapture = false;


        setImage(zoom, R.raw.zoomin);

        scan.setOnTouchListener(new View.OnTouchListener() {
            /**
             * When touch event occurs, handles lift and a simple touch & hold
             * @param v
             * @param event
             * @return
             */
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    //sets boolean touch to true
                    if(getText().equals(Holder.searchWord)){
                        saySomething("Please Enter Query");
                    }else {
                        cameraSource.touch = true;
                    }
                } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                    //sets boolean lift to true if the touch variable was true
                    if (cameraSource.touch) {
                        cameraSource.lift = true;
                    } else cameraSource.lift = false;
                    cameraSource.touch = false;
                }
                return true;
            }
        });

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        KeyboardUtils.addKeyboardToggleListener(this, new KeyboardUtils.SoftKeyboardToggleListener() {
            @Override
            public void onToggleSoftKeyboard(boolean isVisible) {
                Log.d("keyboard", "keyboard visible: " + isVisible);
                keyboardVisible = isVisible;
            }
        });


        //initialized camera source
        if (allPermissionsGranted()) {
            initCameraSource();
            startCameraSource();
        } else {
            getRuntimePermissions();
        }

        // Check to see if we have Text To Speech (TTS) voice data
        Intent ttsIntent = new Intent();
        ttsIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(ttsIntent, ACT_CHECK_TTS_DATA);

        //reset variable values
        wordFound = false;
        if (processor != null) {
            processor.setFoundOnce(false);
        }

        zoomBar.setOnSeekBarChangeListener(a);

        //set initial visibility of views
        ziv.setVisibility(View.GONE);
        zoom.setVisibility(View.GONE);
        next.setVisibility(View.GONE);
        previous.setVisibility(View.GONE);
        findViewById(R.id.download).setVisibility(View.GONE);
        preview.setVisibility(View.VISIBLE);
        settings.setVisibility(View.VISIBLE);
        findViewById(R.id.flashlight).setVisibility(View.VISIBLE);
        zoomBar.setVisibility(View.VISIBLE);
        scan.setVisibility(View.VISIBLE);
        findViewById(R.id.cameraPreview).setVisibility(View.VISIBLE);
        findViewById(R.id.blackLinear).setVisibility(View.GONE);
        restart.setVisibility(View.GONE);
        search.setText(Holder.searchWord);
        cameraCapture = false;

        setImage(flashlight, R.raw.flashlight_invert);
        ziv.clearAnimation();
        //starts detection thread
        startFoundOnceThread();
        startZoomedThread();
    }

    public void lockScreenOrientation() {
        switch (((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_90:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                break;
            case Surface.ROTATION_180:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                break;
            case Surface.ROTATION_270:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                break;
            default :
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    private boolean isHighlighted = false;
    Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            try {

                if (isHighlighted) {
                    ziv.setPhotoBitmap(bmp);
                    isHighlighted = false;
                } else {
                    ziv.setPhotoBitmap(bmpaltr);
                    isHighlighted = true;
                }
            } finally {
                // 100% guarantee that this always happens, even if
                // your update method throws an exception
                mHandler.postDelayed(mStatusChecker, BLINK_INTERVAL);
            }
        }
    };

    private void startBlinking() {
        if(!blinking) {
            mStatusChecker.run();
            blinking = true;
        }
    }

    private void stopBlinking() {
        if (mHandler != null) {
            mHandler.removeCallbacks(mStatusChecker);
            blinking = false;
        }
    }
}
