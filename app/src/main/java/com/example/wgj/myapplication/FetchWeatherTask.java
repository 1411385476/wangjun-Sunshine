package com.example.wgj.myapplication;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.format.Time;
import android.util.Log;

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
import java.util.Vector;

/**
 * Created by wgj on 16-9-8.
 */
public  class FetchWeatherTask extends AsyncTask<String, Void, Void> {
    private final String LOG_TAG = "FetchWeatherTask";
    private ForecastAdapter foreCastAdapter;
    private  Context mContext;

    public FetchWeatherTask(Context context){
        mContext = context;
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
                value.put(WeatherContract.WeatherEntry.COLUMN_LOC_KEY, locationId);
                value.put(WeatherContract.WeatherEntry.COLUMN_DATE, dateTime);
                value.put(WeatherContract.WeatherEntry.COLUMN_HUMIDITY, humidity);
                value.put(WeatherContract.WeatherEntry.COLUMN_PRESSURE, pressure);
                value.put(WeatherContract.WeatherEntry.COLUMN_WIND_SPEED, windSpeed);
                value.put(WeatherContract.WeatherEntry.COLUMN_DEGREES, windDirection);
                value.put(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP, high);
                value.put(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP, low);
                value.put(WeatherContract.WeatherEntry.COLUMN_SHORT_DESC, description);
                value.put(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID, weatherId);
                cvVecator.add(value);
            }
//            String[] resultStrs = convertContentValuesToUXFormat(cvVecator);
//            for (int i = 0; i < resultStrs.length; i++){
//
//                Log.d(LOG_TAG, "resultStrs  " +resultStrs[i]);
//            }
//            for (ContentValues value:cvVecator) {
//                Log.d(LOG_TAG, value+"  jdjfj");
//            }
            int inseted = 0;
            if(cvVecator.size() > 0){
                ContentValues[] cvArray = new ContentValues[cvVecator.size()];
                cvVecator.toArray(cvArray);
//                for (int i = 0; i < cvArray.length;i++) {
//                    Log.d(LOG_TAG, "cvArray  = " + cvArray[i]);
//
//                }
                inseted = mContext.getContentResolver().bulkInsert(WeatherContract.WeatherEntry.CONTENT_URI, cvArray);
            }
            Log.d(LOG_TAG, "inseted    " + inseted);
        }catch (Exception e){
            Log.e(LOG_TAG, e.getMessage(), e);
        }
    }

    long addLocation(String locationSetting, String cityName, double lat, double lon){
        long locationId;
        Log.d(LOG_TAG,"mContext = " + mContext);
        Cursor locationCursor = mContext.getContentResolver().query(
                WeatherContract.LocationEntry.CONTENT_URI,
                new String[]{WeatherContract.LocationEntry._ID},
                WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ?",
                new String[]{locationSetting},
                null);

        Log.d(LOG_TAG,"locationCursor = " + locationCursor+ "locationSetting = " + locationSetting);
        if (locationCursor.moveToFirst()) {
            int locationIdIndex = locationCursor.getColumnIndex(WeatherContract.LocationEntry._ID);
            locationId = locationCursor.getLong(locationIdIndex);
        } else {
            // Now that the content provider is set up, inserting rows of data is pretty simple.
            // First create a ContentValues object to hold the data you want to insert.
            ContentValues locationValues = new ContentValues();

            // Then add the data, along with the corresponding name of the data type,
            // so the content provider knows what kind of value is being inserted.
            locationValues.put(WeatherContract.LocationEntry.COLUMN_CITY_NAME, cityName);
            locationValues.put(WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING, locationSetting);
            locationValues.put(WeatherContract.LocationEntry.COLUMN_COORD_LAT, lat);
            locationValues.put(WeatherContract.LocationEntry.COLUMN_COORD_LONG, lon);

            // Finally, insert location data into the database.
            Uri insertedUri = mContext.getContentResolver().insert(
                    WeatherContract.LocationEntry.CONTENT_URI,
                    locationValues
            );

            // The resulting URI contains the ID for the row.  Extract the locationId from the Uri.
            locationId = ContentUris.parseId(insertedUri);
        }

        locationCursor.close();
        // Wait, that worked?  Yes!
        return locationId;
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
            getWeatherDataFromJson(forecastJsonStr, locationQuery);

        } catch (Exception e) {
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
        return null;
    }

}
