package com.example.wgj.myapplication;


import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 */
public class ForecastFragment extends Fragment {


    private ForecastAdapter foreCastAdapter;
    public ForecastFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        String locationSetting = Utility.getPreferredLocation(getActivity());
        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + "ASC";
        Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWIthStartDate(
                locationSetting, System.currentTimeMillis());
        Log.d("weatherForLocationUri", weatherForLocationUri+ "    fdfdfd");
        Cursor cur = getActivity().getContentResolver().query(weatherForLocationUri,
                null, null, null, sortOrder);
        foreCastAdapter = new ForecastAdapter(getActivity(),cur, 0);
        View rootView = inflater.inflate(R.layout.fragment_main,container,false);

        ListView listview = (ListView) rootView.findViewById(R.id.listview_forecast);
        listview.setAdapter(foreCastAdapter);
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String forecastDetail = (String) foreCastAdapter.getItem(position);
                Intent intent = new Intent(getActivity(), DetailActivity.class);
                intent.putExtra("detail",forecastDetail);
                startActivity(intent);
            }
        });
        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.action_refresh){
            updateWeather();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateWeather(){
        FetchWeatherTask weatherTask = new FetchWeatherTask(getActivity(), foreCastAdapter);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String location = prefs.getString(getString(R.string.pref_location_key),
                getString(R.string.pref_location_default));
        weatherTask.execute(location);

    }

    @Override
    public void onStart() {
        super.onStart();
        updateWeather();
    }
    //    public  class FetchWeatherTask extends AsyncTask<String, Void, String[]>{
//        private final String LOG_TAG = "FetchWeatherTask";
//
//        private String getReadableDateFormat(long time){
//            SimpleDateFormat shortDateFormate = new SimpleDateFormat("EEE MMM dd");
//            return shortDateFormate.format(time);
//        }
//
//        private String formatHighLows(double high, double low){
//            long roundHigh = Math.round(high);
//            long roundLow = Math.round(low);
//            String highLowStr = roundHigh + "/" + roundLow;
//            return highLowStr;
//        }
//
//        private String[] getWeatherDataFromJson(String forecastJsonStr, int numDay) throws JSONException {
//
//            final String OWM_LIST = "list";
//            final String OWM_WEATHER = "weather";
//            final String OWM_TEMPERATURE = "temp";
//            final String OWM_MAX = "temp_max";
//            final String OWM_MIN = "temp_min";
//            final String OWM_DESCRIPTION = "main";
//            JSONObject forecastJson = new JSONObject(forecastJsonStr);
//            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);
//
//            Time dayTime = new Time();
//            dayTime.setToNow();
//            int julianStartDay = Time.getJulianDay(System.currentTimeMillis(),dayTime.gmtoff);
//            String[] resultStrs = new String[numDay];
//            for (int i = 0; i < weatherArray.length(); i++){
//
//                String day;
//                String description;
//                String hightAndLow;
//                JSONObject dayForecast = weatherArray.getJSONObject(i);
//                long dateTime;
//                dateTime = dayTime.setJulianDay(julianStartDay+i);
//                day = getReadableDateFormat(dateTime);
//
//                JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
//                description = weatherObject.getString(OWM_DESCRIPTION);
//                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_DESCRIPTION);
//                double high = temperatureObject.getDouble(OWM_MAX);
//                double low = temperatureObject.getDouble(OWM_MIN);
//                hightAndLow = formatHighLows(high, low);
//                resultStrs[i] = day +"-" + description +"-" + hightAndLow;
//            }
//            Log.e(LOG_TAG, "Forecast entry:" + resultStrs);
//            for (String s: resultStrs){
//                Log.e(LOG_TAG, "Forecast entry:" + s);
//            }
//           return resultStrs;
//        }
//
//
//        @Override
//        protected String[] doInBackground(String... location) {
//            HttpURLConnection urlConnection = null;
//            BufferedReader reader = null;
//
//            // Will contain the raw JSON response as a string.
//            String forecastJsonStr = null;
//            Log.e(LOG_TAG, "forecastJsonStr"+ forecastJsonStr);
//            try {
//                // Construct the URL for the OpenWeatherMap query
//                // Possible parameters are avaiable at OWM's forecast API page, at
//                // http://openweathermap.org/API#forecast
//                String baseUrl = "http://api.openweathermap.org/data/2.5/forecast?q="+ location+",cn&mode=json&units=metric&cnt=7";
//                String apiKey = "&APPID=" + "08299cf1e680c644938d6a8da4fc871c";
//                URL url = new URL(baseUrl.concat(apiKey));
//
//                // Create the request to OpenWeatherMap, and open the connection
//                urlConnection = (HttpURLConnection) url.openConnection();
//                urlConnection.setRequestMethod("GET");
//                urlConnection.connect();
//
//                // Read the input stream into a String
//                InputStream inputStream = urlConnection.getInputStream();
//                StringBuffer buffer = new StringBuffer();
//                if (inputStream == null) {
//                    // Nothing to do.
//                    return null;
//                }
//                reader = new BufferedReader(new InputStreamReader(inputStream));
//
//                String line;
//                while ((line = reader.readLine()) != null) {
//                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
//                    // But it does make debugging a *lot* easier if you print out the completed
//                    // buffer for debugging.
//                    buffer.append(line + "\n");
//                }
//
//                if (buffer.length() == 0) {
//                    // Stream was empty.  No point in parsing.
//                    return null;
//                }
//
//                forecastJsonStr = buffer.toString();
//                Log.e(LOG_TAG, "forecastJsonStr"+ forecastJsonStr);
//            } catch (IOException e) {
//                Log.e(LOG_TAG, "Error ", e);
//                // If the code didn't successfully get the weather data, there's no point in attemping
//                // to parse it.
//                return null;
//            } finally {
//                if (urlConnection != null) {
//                    urlConnection.disconnect();
//                }
//                if (reader != null) {
//                    try {
//                        reader.close();
//                    } catch (final IOException e) {
//                        Log.e(LOG_TAG, "Error closing stream", e);
//                    }
//                }
//            }
//            try {
//                return getWeatherDataFromJson(forecastJsonStr, 7);
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//            return null;
//        }
//
//        @Override
//        protected void onPostExecute(String[] strings) {
//
//            foreCastAdapter.clear();
//            for (String dayForeCast: strings){
//                foreCastAdapter.add(dayForeCast);
//            }
//
//            super.onPostExecute(strings);
//        }
//    }

}
