package com.example.wgj.myapplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.text.DateFormat;
import java.util.Date;

/**
 * Created by wgj on 16-9-13.
 */
public class Utility {
    public static String getPreferredLocation(Context context){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getString("location","beijing");
    }
    public static boolean isMetric(Context context){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return  preferences.getString("units", "metric").equals("metric");
    }
    static String formatTemperature(double temperature, boolean isMetric){
        double temp;
        if(!isMetric){
            temp = 9*temperature/5 + 32;
        }else{
            temp = temperature;
        }
        return String.format("%.0f", temp);
    }
    static String formatDate(long dateInMillis){
        Date date = new Date();
        return DateFormat.getInstance().format(date);
    }
}
