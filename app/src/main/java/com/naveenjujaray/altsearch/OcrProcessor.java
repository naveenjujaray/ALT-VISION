package com.naveenjujaray.altsearch;

import android.graphics.Rect;
import android.support.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextDetector;

import java.util.List;

public class OcrProcessor extends VisionProcessorBase<FirebaseVisionText> {
    // =============================================================================================
    // Variables
    // =============================================================================================
    private final FirebaseVisionTextDetector detector;
    private MainActivity mainActivity;
    private boolean foundOnce = false;
    private Rect r;
    private Rect transR;
    private ClassificationOverlayGraphic textGraphic;

    // =============================================================================================
    // Constructor
    // =============================================================================================
    public OcrProcessor(MainActivity activity) {
        detector = FirebaseVision.getInstance().getVisionTextDetector();
        mainActivity = activity;
    }

    // =============================================================================================
    // Methods
    // =============================================================================================
    @Override
    protected Task<FirebaseVisionText> detectInImage(FirebaseVisionImage image) {
        return detector.detectInImage(image);
    }

    /**
     * Analyzing found words
     * @param results The text found in the image
     * @param frameMetadata
     * @param graphicOverlay
     */
    @Override
    protected void onSuccess(@NonNull FirebaseVisionText results, @NonNull FrameMetadata frameMetadata, @NonNull GraphicOverlay graphicOverlay) {
        //prevent lag from processing after found once
        if (mainActivity.success) return;

        //method to detect location of specific word on the screen
        float[] location = processText(results);

        //scheme of classification chosen
        ClassificationScheme scheme = mainActivity.getScheme();

        //adds overlay rectangle of specific type of classification
        graphicOverlay.clear();
        textGraphic = new ClassificationOverlayGraphic(graphicOverlay, scheme);
        graphicOverlay.add(textGraphic);

        if (location != null) {

            int h = frameMetadata.getHeight();
            int w = frameMetadata.getWidth();
            float x = location[0];
            float y = location[1];

            //Adjusts a horizontal value of the supplied value from the preview scale to the view scale.
            x = textGraphic.translateX(x);
            transR = new Rect((int) textGraphic.translateX(r.left), (int) textGraphic.translateY(r.top), (int) textGraphic.translateX(r.right), (int) textGraphic.translateY(r.bottom));

            String str = "Location" + "( " + x + " , " + y + " )" + "\n";

            // only vibrates if in correct location of screen
            // sets flag found once to true if in correct location to stop running thread
            if (mainActivity.getWordFound()) {
                if (scheme == ClassificationScheme.FIND_AT_CENTER) {
                    str = str + "Bounds for x = {" + w / 4.0 + " , " + (3 * w / 4.0) + "}";
                    if (x > w / 4.0 && x < ((3 * w) / 4.0)) {
                        mainActivity.vibratePhone();
                        foundOnce = true;
                    }
                } else if (scheme == ClassificationScheme.FIND_AT_BOTTOM) {
                    str = str + "Bounds for y = { y > " + h / 2 + "}";
                    if (y > h / 2) {
                        mainActivity.vibratePhone();
                        foundOnce = true;
                    }
                } else if (scheme == ClassificationScheme.FIND_AT_TOP) {
                    str = str + "Bounds for y = { y < " + h / 2 + "}";
                    if (y < h / 2) {
                        mainActivity.vibratePhone();
                        foundOnce = true;
                    }
                } else {
                    mainActivity.vibratePhone();
                    foundOnce = true;
                }
            }

            Logging.log("OCR APP :): ", str);
        }

        if (mainActivity.secondTime) {
            mainActivity.processingDone = true;
        }
    }

    /**
     * Processes text with a query
     * @param firebaseVisionText text captured from ocr image
     * @return location of the rectangle of the surrounding word (fallback for final procesing)
     */
    private float[] processText(FirebaseVisionText firebaseVisionText) {
        List<FirebaseVisionText.Block> blocks = firebaseVisionText.getBlocks();

        //checks if there is any text
        int size = blocks.size();
        if (size == 0) return null;

        //checks if there is a multiword input
        String str = mainActivity.getText();
        String[] q = str.split(" ");

        if (q.length > 1) {
            //processes multiple word query
            return processText(blocks, q);
        } else {
            //if only one word query
            float x = -1, y = -1;

            search:
            {
                for (FirebaseVisionText.Block b : blocks) {
                    for (FirebaseVisionText.Line l : b.getLines()) {
                        for (FirebaseVisionText.Element e : l.getElements()) {
                            //checks if word from ocr is similar to the query (using levenshtein distance)

                            if (StringComp.stringSimilar(e.getText(), str)) {
                                mainActivity.setWordFound(true);
                                //stores the rectangle of the surrounding word
                                r = e.getBoundingBox();
                                x = r.exactCenterX();
                                y = r.exactCenterY();
                                break search;
                            }
                        }
                    }
                }
            }

            if (x != -1 && y != -1) {
                //return location array
                return new float[]{x, y};
            } else {
                mainActivity.setWordFound(false);
                return null; //if not found
            }
        }
    }

    /**
     * Multiple word query processing
     * @param blocks the paragraphs in the text
     * @param q the query (each word is one element of the array)
     * @return location of the center of the word
     */
    private float[] processText(List<FirebaseVisionText.Block> blocks, String[] q) {
        float x = -1, y = -1;

        search:
        {
            for (FirebaseVisionText.Block b : blocks) {
                for (FirebaseVisionText.Line l : b.getLines()) {
                    List<FirebaseVisionText.Element> list = l.getElements();
                    for (int i = 0; i < list.size() - 1; i++) {
                        FirebaseVisionText.Element e = list.get(i);
                        FirebaseVisionText.Element e_next = list.get(i + 1);
                        if (StringComp.stringSimilar(e.getText(), q[0]) && StringComp.stringSimilar(e_next.getText(), q[1])) {
                            mainActivity.setWordFound(true);
                            //creates a new rectangle that is a combination of the current word and
                            // next word'altsearch rectangles for accurate location
                            r = new Rect(e.getBoundingBox().left, e.getBoundingBox().top, e_next.getBoundingBox().right, e_next.getBoundingBox().bottom);
                            x = r.exactCenterX();
                            y = r.exactCenterY();
                            break search;
                        }
                    }
                }
            }
        }

        if (x != -1 && y != -1) {
            //return location array
            return new float[]{x, y};
        } else {
            mainActivity.setWordFound(false);
            return null; //if not found
        }
    }

    @Override
    protected void onFailure(@NonNull Exception e) {
        Logging.log("Failure!", "Unable to detect anything");
        if (mainActivity.secondTime) {
            mainActivity.processingDone = true;
        }
    }


    //processing getter and setters
    public boolean isFoundOnce() {
        return foundOnce;
    }

    public void setFoundOnce(boolean foundOnce) {
        this.foundOnce = foundOnce;
    }


    public Rect getBoundingRect() {
        return r;
    }

    public Rect getBoundingRectTrans() {
        return transR;
    }


    //translation due to scaling in x and y
    public int translateX(int x) {
        if (textGraphic == null) textGraphic = new ClassificationOverlayGraphic(null, null);
        return (int) textGraphic.translateX(x);
    }

    public int translateY(int y) {
        if (textGraphic == null) textGraphic = new ClassificationOverlayGraphic(null, null);
        return (int) textGraphic.translateY(y);
    }
}
