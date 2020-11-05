package com.naveenjujaray.altsearch;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.support.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.label.FirebaseVisionLabel;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextDetector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Class used to process final frame captured for the image view
 */

public class FrameProcessor {

    MainActivity mainActivity;
    private boolean multiWord;
    private Rect rect;
    private ArrayList<Rect> rects;

    private boolean finished = false;
    private List<FirebaseVisionText.Block> blocks;
    private Object[] arr;
    private ArrayList<Point[]> points;
    private ArrayList<Point[]> wordPoints;


    /**
     * Constructor
     *
     * @param activity  activity of origination for access to variables
     * @param multiWord whether processing multiple words
     */
    public FrameProcessor(MainActivity activity, boolean multiWord) {
        this.mainActivity = activity;
        this.multiWord = multiWord;
    }

    /**
     * Runs OCR on the bitmap of the zoomable imageview
     *
     * @param bmp   last frame'altsearch bitmap
     * @param query word to look for
     */
    public void runTextRecognition(Bitmap bmp, final String query) {
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bmp);
        FirebaseVisionTextDetector detector = FirebaseVision.getInstance()
                .getVisionTextDetector();
        detector.detectInImage(image).addOnSuccessListener(
                new OnSuccessListener<FirebaseVisionText>() {
                    @Override
                    public void onSuccess(FirebaseVisionText texts) {
                        if (!multiWord) {
                            processTextRecognitionResult(texts, query);
                        } else {
                            processTextRecognitionResultMulti(texts, query);
                        }
                        finished = true;
                    }
                })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                e.printStackTrace();
                                finished = true;
                            }
                        });
    }



    /**
     * Multiple words detection
     * ========================
     * - do not break out of the for loop after first instance found like in {@link OcrProcessor}
     * - adds to arraylist
     * - also accounts for multiple word query
     *
     * @param t     text from the bitmap
     * @param query
     */
    private void processTextRecognitionResultMulti(FirebaseVisionText t, String query) {
        String[] words = query.split(" ");
        if (words.length > 1) {
            processTextRecognitionResultMulti(t, words[0], words[1]);
            return;
        }

        blocks = t.getBlocks();
        if (blocks.size() == 0) {
            return;
        }
        rects = new ArrayList<Rect>();
        points = new ArrayList<Point[]>();
        wordPoints = new ArrayList<Point[]>();
        Point[] points;
        for (int i = 0; i < blocks.size(); i++) {
            List<FirebaseVisionText.Line> lines = blocks.get(i).getLines();
            for (int j = 0; j < lines.size(); j++) {
                List<FirebaseVisionText.Element> elements = lines.get(j).getElements();
                for (FirebaseVisionText.Element e : elements) {
                    points = e.getCornerPoints();
                    if(points!=null){
                        this.points.add(points);
                    }

                    if (StringComp.stringSimilar(e.getText(), query)) {
                        mainActivity.setWordFound(true);
                        rects.add(e.getBoundingBox());
                        wordPoints.add(points);
                    }
                }
            }
        }
    }

    /**
     * If the there is a multiple word query, runs levenshtein distance on two words (the current
     * word and the next word)
     *
     * @param texts text from the bitmap
     * @param word1 first word in query
     * @param word2 second word in query
     */
    private void processTextRecognitionResultMulti(FirebaseVisionText texts, String word1, String word2) {
        blocks = texts.getBlocks();
        if (blocks.size() == 0) {
            return;
        }

        rects = new ArrayList<Rect>();
        points = new ArrayList<Point[]>();
        wordPoints = new ArrayList<Point[]>();
        Point[] points;
        for (int x = 0; x < blocks.size(); x++) {
            List<FirebaseVisionText.Line> lines = blocks.get(x).getLines();
            for (int j = 0; j < lines.size(); j++) {
                List<FirebaseVisionText.Element> list = lines.get(j).getElements();
                for (int i = 0; i < list.size() - 1; i++) {
                    FirebaseVisionText.Element e = list.get(i);
                    FirebaseVisionText.Element e_next = list.get(i + 1);
                    points = e.getCornerPoints();
                    if(points!=null){
                        this.points.add(points);
                    }
                    if (StringComp.stringSimilar(e.getText(), word1) && StringComp.stringSimilar(e_next.getText(), word2)) {
                        mainActivity.setWordFound(true);
                        rects.add(new Rect(e.getBoundingBox().left, e.getBoundingBox().top, e_next.getBoundingBox().right, e_next.getBoundingBox().bottom));
                        wordPoints.add(new Point[]{e.getCornerPoints()[0], e_next.getCornerPoints()[1], e_next.getCornerPoints()[2], e.getCornerPoints()[3]});
                    }
                }
            }
        }
    }


    private String processTextRecognitionResult(List<FirebaseVisionLabel> labels) {
        String str = "";
        int i = 1;
        for(FirebaseVisionLabel l:labels){
            str += i + ") " + l.getLabel() + " : " + l.getConfidence() + "\n";
            i++;
        }
        return str;
    }

    /**
     * Only single word, single detection OCR
     *
     * @param texts see above
     * @param query see above
     */
    private void processTextRecognitionResult(FirebaseVisionText texts, String query) {
        blocks = texts.getBlocks();
        if (blocks.size() == 0) {
            return;
        }
        points = new ArrayList<Point[]>();
        wordPoints = new ArrayList<Point[]>();
        Point[] points;
        for (int i = 0; i < blocks.size(); i++) {
            List<FirebaseVisionText.Line> lines = blocks.get(i).getLines();
            for (int j = 0; j < lines.size(); j++) {
                List<FirebaseVisionText.Element> elements = lines.get(j).getElements();
                for (FirebaseVisionText.Element e : elements) {
                    points = e.getCornerPoints();
                    if(points!=null){
                        this.points.add(points);
                    }
                    if (StringComp.stringSimilar(e.getText(), query)) {
                        mainActivity.setWordFound(true);
                        rect = e.getBoundingBox();
                        wordPoints.add(points);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Updates the recognition for a new query (multiple detections)
     *
     * @param query new search words
     */
    public void updateRecogMulti(String query) {
        if (blocks.size() == 0) {
            return;
        }

        String[] str = query.split(" ");
        if (str.length > 1) {
            updateRecogMulti(str[0], str[1]);
            return;
        }

        rects = new ArrayList<Rect>();
        wordPoints = new ArrayList<Point[]>();
        for (int i = 0; i < blocks.size(); i++) {
            List<FirebaseVisionText.Line> lines = blocks.get(i).getLines();
            for (int j = 0; j < lines.size(); j++) {
                List<FirebaseVisionText.Element> elements = lines.get(j).getElements();
                for (FirebaseVisionText.Element e : elements) {
                    if (StringComp.stringSimilar(e.getText(), query)) {
                        mainActivity.setWordFound(true);
                        rects.add(e.getBoundingBox());
                        wordPoints.add(e.getCornerPoints());
                    }
                }
            }
        }
    }

    /**
     * Updates the recognition for a new query
     *
     * @param word1 new search words' first word
     * @param word2 new search words' second word
     */
    public void updateRecogMulti(String word1, String word2) {
        rects = new ArrayList<Rect>();
        wordPoints = new ArrayList<Point[]>();
        for (int x = 0; x < blocks.size(); x++) {
            List<FirebaseVisionText.Line> lines = blocks.get(x).getLines();
            for (int j = 0; j < lines.size(); j++) {
                List<FirebaseVisionText.Element> list = lines.get(j).getElements();
                for (int i = 0; i < list.size() - 1; i++) {
                    FirebaseVisionText.Element e = list.get(i);
                    FirebaseVisionText.Element e_next = list.get(i + 1);
                    if (StringComp.stringSimilar(e.getText(), word1) && StringComp.stringSimilar(e_next.getText(), word2)) {
                        mainActivity.setWordFound(true);
                        rects.add(new Rect(e.getBoundingBox().left, e.getBoundingBox().top, e_next.getBoundingBox().right, e_next.getBoundingBox().bottom));
                        wordPoints.add(new Point[]{e.getCornerPoints()[0], e_next.getCornerPoints()[1], e_next.getCornerPoints()[2], e.getCornerPoints()[3]});
                    }
                }
            }
        }
    }

    /**
     * Updates the recognition for a new query (single detection)
     *
     * @param query new search words
     */
    public void updateRecog(String query) {
        if (blocks.size() == 0) {
            return;
        }
        for (int i = 0; i < blocks.size(); i++) {
            List<FirebaseVisionText.Line> lines = blocks.get(i).getLines();
            for (int j = 0; j < lines.size(); j++) {
                List<FirebaseVisionText.Element> elements = lines.get(j).getElements();
                for (FirebaseVisionText.Element e : elements) {
                    if (StringComp.stringSimilar(e.getText(), query)) {
                        mainActivity.setWordFound(true);
                        rect = e.getBoundingBox();
                        break;
                    }
                }
            }
        }
    }

    // =============================================================================================
    // Getter/Setter Methods
    // =============================================================================================

    public boolean isFinished() {
        return finished;
    }

    public void setFinished() {
        finished = false;
    }

    public Rect getR() {
        return rect;
    }

    public ArrayList<Rect> getRects() {
        return rects;
    }

    public String getText(){
        if(arr == null) {
            ArrayList<String> str = new ArrayList<>();
            for (int i = 0; i < blocks.size(); i++) {
                List<FirebaseVisionText.Line> lines = blocks.get(i).getLines();
                for (int j = 0; j < lines.size(); j++) {
                    List<FirebaseVisionText.Element> elements = lines.get(j).getElements();
                    for (FirebaseVisionText.Element e : elements) {
                        str.add(e.getText() + " : " + e.getBoundingBox().toString() + "\n");
                    }
                }
            }
            arr = str.toArray();
        }
        return Arrays.toString(arr);
    }

    public ArrayList<Point[]> getPoints() {
        return points;
    }

    public ArrayList<Point[]> getWordPoints() {
        return wordPoints;
    }

    public void sort(){
        for(int i = 0; i<rects.size(); i++){
            for(int j = i+1; j<rects.size(); j++){
                if(rects.get(i).bottom > rects.get(j).bottom){
                    swap(i,j);
                }
            }
        }
    }

    private void swap(int i, int j){
        Rect r = rects.get(i);
        rects.set(i, rects.get(j));
        rects.set(j, r);
    }
}
