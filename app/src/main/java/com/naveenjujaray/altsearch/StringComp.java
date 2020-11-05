package com.naveenjujaray.altsearch;

import java.util.ArrayList;
import java.util.Arrays;

public class StringComp {

    public static final double TRESHOLD_VALUE = 0.3;

    public static boolean stringFound(String arr[], String search){
        for(String str: arr){
            str = str.trim();
            String[] words = str.split(" ");
            for(String x: words){
                if(stringSimilar(x, search)){
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean stringSimilar(String x, String y) {
        if(x.contains("-")){
            if(!y.contains("-")){
                String[] arr = x.split("-");
                if(!allDigits(arr)) {
                    for (String str : arr) {
                        if (stringSimilar(str, y)) {
                            return true;
                        }
                    }
                }
                return false;
            }
        }

        if(x.contains("/")){
            if(!y.contains("/")){
                String[] arr = x.split("/");
                if(!allDigits(arr)) {
                    for (String str : arr) {
                        if (stringSimilar(str, y)) {
                            return true;
                        }
                    }
                }
                return false;
            }
        }

        if(strDigit(x) || strDigit(y)){
            return x.equals(y);
        }


        String x_mod = x.replaceAll("[^A-Za-z]+", "").toLowerCase();
        String y_mod = y.replaceAll("[^A-Za-z]+", "").toLowerCase();

        // If two strings are equal
        if(x_mod.equals(y_mod)) return true;

        //If one string’altsearch length is twice the length of the other string
        if(Math.max(x_mod.length(), y_mod.length())/2.0 > Math.min(x.length(), y.length())) return false;

        //If neither of the above two case is true
        double val = StringComp.Distance(x_mod,y_mod)/ Math.min(x_mod.length(), y_mod.length());
        return val<StringComp.TRESHOLD_VALUE;
    }

    private static boolean allDigits(String[] arr) {
        for(String x: arr){
            if(!strDigit(x)){
                return false;
            }
        }return true;
    }

    public static boolean strDigit(String s) {
        if (s != null && s.length()>0) {
            for (char c : s.toCharArray()) {
                if (Character.isDigit(c)) {
                    return true;
                }
            }
        }
        return false;
    }


    //edit distance
    //deletion and insertion cost 1
    //substitution costs 1
    public static double Distance(String x, String y){

        double[][] distance = new double[x.length()+1][y.length()+1];

        for(int i = 0; i<= x.length(); i++){
            distance[i][0] = i;
        }

        for(int i = 0; i<= y.length(); i++){
            distance[0][i] = i;
        }

        for(int i = 0; i <x.length(); i++){
            for(int j = 0; j <y.length(); j++){
                int edit = 1;
                if(x.charAt(i) == y.charAt(j)) edit = 0;

                distance[i+1][j+1] = min3(distance[i][j+1]+1, distance[i+1][j]+1, distance[i][j]+edit);
            }
        }
        return distance[x.length()][y.length()];
    }

    public static double min3(double a, double b, double c) {
        return Math.min(a, Math.min(b, c));
    }

    public static String print2DArray(double[][] arr) {
        String str = "";
        for(double[] x: arr) {
            str = str + Arrays.toString(x) + "\n";
        }
        return str;
    }

    public static String[] splitBackspace(String str) {
        String backslash = ((char)92) + "";
        if (str.contains(backslash)) {
            ArrayList<String> parts = new ArrayList<>();
            int start = 0;
            int end = 0;
            for ( int c : str.toCharArray() ) {
                if (c == 92) {
                    parts.add(str.substring(start, end));
                    start = end + 1;
                }
                end++;
            }
            parts.add(str.substring(start));
            return parts.toArray( new String[parts.size()] );
        }
        return str.split("\\\\");
    }

}