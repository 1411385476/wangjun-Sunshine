package com.example.wgj.myapplication.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

public class WeatherProvider extends ContentProvider {

    private static final UriMatcher sUriMather = buildMather();

    private WeatherDbHelper mOpenHelper;

    static final int WEATHER = 100;

    static final int WEATHER_WITH_LOCATION = 101;

    static final int WEATHER_WITH_LOCATION_AND_DATE = 102;

    static final int LOCATION = 300;

    private static final SQLiteQueryBuilder sWeatherByLocationSettingQueryBuilder;

    static {
        sWeatherByLocationSettingQueryBuilder = new SQLiteQueryBuilder();
        sWeatherByLocationSettingQueryBuilder.setTables(
                WeatherContract.WeatherEntry.TABLE_NAME + " INNER JOIN " +
                        WeatherContract.LocationEntry.TABLE_NAME +
                        " ON " + WeatherContract.WeatherEntry.TABLE_NAME +
                        "." + WeatherContract.WeatherEntry.COLUMN_LOC_KEY +
                        "=" + WeatherContract.LocationEntry.TABLE_NAME +
                        "." + WeatherContract.LocationEntry._ID);
    }

    private static final String sLocationSettingSelection =
            WeatherContract.LocationEntry.TABLE_NAME +
                    "." + WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING +
                    "=? ";

    private static final String sLocationSettingWithStartDateSelection =
            WeatherContract.LocationEntry.TABLE_NAME+
                    "." + WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ? AND " +
                    WeatherContract.WeatherEntry.COLUMN_DATE + " >= ? ";


    private static final String sLocationSettingAndDaySelection =
            WeatherContract.LocationEntry.TABLE_NAME +
                    "." + WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + "=? AND" +
                    WeatherContract.WeatherEntry.COLUMN_DATE + "=? ";


    private Cursor getWeatherByLocationSetting(Uri uri, String[] projection, String sortOrder){

        String locationSetting = WeatherContract.WeatherEntry.getLocationSettingFromUri(uri);
        long startDate = WeatherContract.WeatherEntry.getStartDateFromUri(uri);
        String[] selectionArgs;
        String selection;

        if(startDate == 0){
            selection = sLocationSettingSelection;
            selectionArgs = new String[]{locationSetting};
        }else{
            selectionArgs = new String[]{locationSetting, Long.toString(startDate)};
            selection = sLocationSettingWithStartDateSelection;
        }
        Log.d("getWeatherByLocation", "projection  -- " + projection +" selection  -- " + selection
        + "selectionArgs -- " +selectionArgs + "sortOrder" + sortOrder );
        return sWeatherByLocationSettingQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder);
    }

    public Cursor getWeatherByLocationSettingAndDate(Uri uri, String[] projection,String sortOrder){

        String locationSetting = WeatherContract.WeatherEntry.getLocationSettingFromUri(uri);
        long date = WeatherContract.WeatherEntry.getDateFromUri(uri);

        return sWeatherByLocationSettingQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                sLocationSettingAndDaySelection,
                new String[]{locationSetting, Long.toString(date)},
                null,
                null,
                sortOrder);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Implement this to handle requests to delete one or more rows.
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMather.match(uri);
        int rowsDeleted;
        // this makes delete all rows return the number of rows deleted
        if ( null == selection ) selection = "1";
        switch (match) {
            case WEATHER:
                rowsDeleted = db.delete(
                        WeatherContract.WeatherEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case LOCATION:
                rowsDeleted = db.delete(
                        WeatherContract.LocationEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        // Because a null deletes all rows
        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }

    @Override
    public String getType(Uri uri) {
        // TODO: Implement this to handle requests for the MIME type of the data
        // at the given URI.
        final int match = sUriMather.match(uri);
        switch (match){
            case WEATHER_WITH_LOCATION_AND_DATE:
                return WeatherContract.WeatherEntry.CONTENT_ITEM_TYPE;
            case WEATHER_WITH_LOCATION:
                return WeatherContract.WeatherEntry.CONTENT_TYPE;
            case WEATHER:
                return WeatherContract.WeatherEntry.CONTENT_TYPE;
            case LOCATION:
                return WeatherContract.LocationEntry.CONTENT_TYPE;

            default:throw new UnsupportedOperationException("Unknown uri:" + uri);

        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // TODO: Implement this to handle requests to insert a new row.
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int math = sUriMather.match(uri);
        Uri returnUri;
        switch (math){
            case WEATHER:
                normalizeDate(values);
                        long _id = db.insert(WeatherContract.WeatherEntry.TABLE_NAME, null, values);
                if(_id > 0){
                    returnUri = WeatherContract.WeatherEntry.buildWeatherUri(_id);
                }else{
                    throw new UnsupportedOperationException("Not yet implemented");
                }
                break;
            case LOCATION:
                long id = db.insert(WeatherContract.LocationEntry.TABLE_NAME, null, values);
                if ( id > 0 )
                    returnUri = WeatherContract.LocationEntry.buildLocationUri(id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;

            default:
                    throw new UnsupportedOperationException("Not yet implemented");
        }
        getContext().getContentResolver().notifyChange(uri, null);

        return returnUri;
    }

    private void normalizeDate(ContentValues values) {
        // normalize the date value
        if (values.containsKey(WeatherContract.WeatherEntry.COLUMN_DATE)) {
            long dateValue = values.getAsLong(WeatherContract.WeatherEntry.COLUMN_DATE);
            values.put(WeatherContract.WeatherEntry.COLUMN_DATE, WeatherContract.normalizeDate(dateValue));
        }
    }

    @Override
    public boolean onCreate() {
        // TODO: Implement this to initialize your content provider on startup.
        mOpenHelper = new WeatherDbHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        // TODO: Implement this to handle query requests from clients.
        Log.d("query", "query");
        Cursor retCursor;
        switch (sUriMather.match(uri)){

            case WEATHER_WITH_LOCATION_AND_DATE:
                Log.d("query", "WEATHER_WITH_LOCATION_AND_DATE");
                retCursor = getWeatherByLocationSettingAndDate(uri, projection, sortOrder);
                break;
            case WEATHER_WITH_LOCATION:
                Log.d("query", "WEATHER_WITH_LOCATION");
                retCursor = getWeatherByLocationSetting(uri,projection,sortOrder);
                break;
            case WEATHER:
                Log.d("query", "WEATHER");
                retCursor = mOpenHelper.getReadableDatabase().query(
                        WeatherContract.WeatherEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            case LOCATION:
                Log.d("query", "LOCATION");
                retCursor = mOpenHelper.getReadableDatabase().query(
                        WeatherContract.LocationEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            default:
                Log.d("query", "default");
                throw new UnsupportedOperationException("Not yet implemented");
        }
        retCursor.setNotificationUri(getContext().getContentResolver(),uri);
        return retCursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMather.match(uri);
        int rowsUpdated;

        switch (match) {
            case WEATHER:
                normalizeDate(values);
                rowsUpdated = db.update(WeatherContract.WeatherEntry.TABLE_NAME, values, selection,
                        selectionArgs);
                break;
            case LOCATION:
                rowsUpdated = db.update(WeatherContract.LocationEntry.TABLE_NAME, values, selection,
                        selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsUpdated;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {

        Log.d("bulkInsert", "uri   " + uri);
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMather.match(uri);
        switch (match){
            case WEATHER:
                db.beginTransaction();
                int returnCount = 0;
                try{
                    for (ContentValues value: values){
                        normalizeDate(value);
                        long _id = db.insert(WeatherContract.WeatherEntry.TABLE_NAME,null, value);
                        if(_id != -1){
                            returnCount++;
                        }
                    }
                    db.setTransactionSuccessful();
                }finally {
                    db.endTransaction();
                }
                getContext().getContentResolver().notifyChange(uri,null);
                return returnCount;
            default:
                return super.bulkInsert(uri, values);

        }

    }

    @Override
    public void shutdown() {
        mOpenHelper.close();
        super.shutdown();
    }

    static UriMatcher buildMather(){
        final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = WeatherContract.CONTENT_AUTHORITY;
        uriMatcher.addURI(authority, WeatherContract.PATH_WEATHER, WEATHER);
        uriMatcher.addURI(authority, WeatherContract.PATH_WEATHER + "/*", WEATHER_WITH_LOCATION);
        uriMatcher.addURI(authority, WeatherContract.PATH_WEATHER + "/*/#", WEATHER_WITH_LOCATION_AND_DATE);
        uriMatcher.addURI(authority, WeatherContract.PATH_LOCATION, LOCATION);
        return uriMatcher;
    }
}
