package com.example.wgj.myapplication;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.text.format.Time;
import android.util.Log;
import android.widget.ArrayAdapter;

import com.example.wgj.myapplication.data.WeatherContract;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Vector;

import com.example.wgj.myapplication.data.WeatherContract.*;

/**
 * Created by wgj on 16-9-8.
 */
public  class FetchWeatherTask extends AsyncTask<String, Void, Void> {
    private final String LOG_TAG = "FetchWeatherTask";
    private ForecastAdapter foreCastAdapter;
    private  Context mContext;

    public FetchWeatherTask(Context context, ForecastAdapter foreCastAdapter){
        mContext = context;
        this.foreCastAdapter = foreCastAdapter;
    }

    private String getReadableDateFormat(long time){
        SimpleDateFormat shortDateFormate = new SimpleDateFormat("EEE MMM dd");
        return shortDateFormate.format(time);
    }

    private String formatHighLows(double high, double low){
        long roundHigh = Math.round(high);
        long roundLow = Math.round(low);
        String highLowStr = roundHigh + "/" + roundLow;
        return highLowStr;
    }

    private void getWeatherDataFromJson(String forecastJsonStr,
                                            String locationSetting) throws JSONException {

        final String OWM_CITY = "city";
        final String OWM_CITY_NAME = "name";
        final String OWM_COORD = "coord";

        final String OWM_LATITUDE = "lat";
        final String OWM_LONGITUDE = "lon";

        final String OWM_LIST = "list";
        final String OWM_HUMIDITY = "humidity";
        final String OWM_PRESSURE = "pressure";
        final String OWM_WEATHER = "weather";
        final String OWM_TEMPERATURE = "temp";
        final String OWM_MAX = "max";
        final String OWM_MIN = "min";
        final String OWM_WIND_DIRECTION = "deg";

        final String OWM_DESCRIPTION = "main";

        final String OWM_WEATHER_ID = "id";
        try {
            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

            JSONObject cityJson = forecastJson.getJSONObject(OWM_CITY);
            String cityName = cityJson.getString(OWM_CITY_NAME);

            JSONObject cityCoord = cityJson.getJSONObject(OWM_COORD);
            double cityLatitude = cityCoord.getDouble(OWM_LATITUDE);
            double cityLongitude = cityCoord.getDouble(OWM_LONGITUDE);

            long locationId = addLocation(locationSetting, cityName, cityLatitude, cityLongitude);
            Vector<ContentValues> cvVecator = new Vector<ContentValues>();
            Time dayTime = new Time();
            dayTime.setToNow();
            int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);


            for (int i = 0; i < weatherArray.length(); i++) {

                long dateTime;
                double pressure;
                int humidity;
                double windSpeed;
                double windDirection;

                double high;
                double low;

                String description;
                int weatherId;

                JSONObject dayForecast = weatherArray.getJSONObject(i);
                dateTime = dayTime.setJulianDay(julianStartDay + i);

                pressure = dayForecast.getDouble(OWM_PRESSURE);
                humidity = dayForecast.getInt(OWM_HUMIDITY);
                windSpeed = dayForecast.getDouble(OWM_PRESSURE);
                windDirection = dayForecast.getDouble(OWM_WIND_DIRECTION);

                JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);
                weatherId = weatherObject.getInt(OWM_WEATHER_ID);
                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                high = temperatureObject.getDouble(OWM_MAX);
                low = temperatureObject.getDouble(OWM_MIN);
                ContentValues value = new ContentValues();
                value.put(WeatherEntry.COLUMN_LOC_KEY, locationSetting);
                value.put(WeatherEntry.COLUMN_DATE, dateTime);
                value.put(WeatherEntry.COLUMN_HUMIDITY, humidity);
                value.put(WeatherEntry.COLUMN_PRESSURE, pressure);
                value.put(WeatherEntry.COLUMN_WIND_SPEED, windSpeed);
                value.put(WeatherEntry.COLUMN_DEGREES, windDirection);
                value.put(WeatherEntry.COLUMN_MAX_TEMP, high);
                value.put(WeatherEntry.COLUMN_MIN_TEMP, low);
                value.put(WeatherEntry.COLUMN_SHORT_DESC, description);
                value.put(WeatherEntry.COLUMN_WEATHER_ID, weatherId);
                cvVecator.add(value);
            }
//            String[] resultStrs = convertContentValuesToUXFormat(cvVecator);
//            for (int i = 0; i < resultStrs.length; i++){
//
//                Log.d(LOG_TAG, "resultStrs  " +resultStrs[i]);
//            }
            for (ContentValues value:cvVecator) {
                Log.d(LOG_TAG, value+"  jdjfj");
            }
            int inseted = 0;
            if(cvVecator.size() > 0){
                ContentValues[] cvArray = new ContentValues[cvVecator.size()];
                cvVecator.toArray();
                inseted = mContext.getContentResolver().bulkInsert(WeatherEntry.CONTENT_URI, cvArray);
            }
            Log.d(LOG_TAG, "inseted    " + inseted);
        }catch (Exception e){
            Log.e(LOG_TAG, e.getMessage(), e);
        }
    }

    long addLocation(String locationSetting, String cityName, double lat, double lon){
        return -1;
    }

    String[] convertContentValuesToUXFormat(Vector<ContentValues> cvv){
        String[] resultStr = new String[cvv.size()];
        for (int i = 0; i < cvv.size(); i++){
            Log.d(LOG_TAG, "convertContentValuesToUXFormat" + resultStr[i]);
            ContentValues weatherValues = cvv.elementAt(i);
            String highAndLow = formatHighAndLows(weatherValues.getAsDouble(WeatherEntry.COLUMN_MAX_TEMP),
                    weatherValues.getAsDouble(WeatherEntry.COLUMN_MIN_TEMP));

        }
        return resultStr;
    }

    private String formatHighAndLows(double high, double low){
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        String unitType = sharedPrefs.getString(mContext.getString(R.string.pref_units_key),
                mContext.getString(R.string.pref_units_metric));
        if(unitType.equals(mContext.getString(R.string.pref_units_imperial))){
            high = (high * 1.8) + 32;
            low = (low * 1.8) + 32;
        }else if(!unitType.equals(mContext.getString(R.string.pref_units_metric))){

        }
        long roundHigh = Math.round(high);
        long roundLow = Math.round(low);
        String highLowStr = roundHigh + "/" + roundLow;
        return highLowStr;
    }

    @Override
    protected Void doInBackground(String... location) {

        if(location.length == 0){
            return null;
        }

        String locationQuery = location[0];
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        // Will contain the raw JSON response as a string.
        String forecastJsonStr = null;

        String format = "json";
        String units = "metric";
        int numDays = 14;
        try {
            // Construct the URL for the OpenWeatherMap query
            // Possible parameters are avaiable at OWM's forecast API page, at
            // http://openweathermap.org/API#forecast
            final String FORECAST_BASE_URL = "http://api.openweathermap.org/data/2.5/forecast/daily";
            final String QUERY_PARAM = "q";
            final String FORMA_PARAM = "mode";
            final String UNITS_PARAM = "units";
            final String DAYS_PARAM = "cnt";
            final String APPID_PARAM = "APPID";
            Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon().
                    appendQueryParameter(QUERY_PARAM, location[0]).
                    appendQueryParameter(FORMA_PARAM, format).
                    appendQueryParameter(UNITS_PARAM, units)
                    .appendQueryParameter(DAYS_PARAM, Integer.toString(numDays))
                    .appendQueryParameter(APPID_PARAM, "08299cf1e680c644938d6a8da4fc871c")
                    .build();


            URL url = new URL(builtUri.toString());

            // Create the request to OpenWeatherMap, and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do.
                return null;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                return null;
            }

            forecastJsonStr = buffer.toString();
            Log.e(LOG_TAG, "forecastJsonStr              "+ forecastJsonStr);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
            // If the code didn't successfully get the weather data, there's no point in attemping
            // to parse it.
            return null;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }
        try {
             getWeatherDataFromJson(forecastJsonStr, locationQuery);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

}
