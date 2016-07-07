package com.example.android.sunshine.app;

import android.annotation.TargetApi;
//import android.app.LoaderManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
//import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
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

import com.example.android.sunshine.app.data.WeatherContract;
import com.example.android.sunshine.app.service.SunshineService;
import com.example.android.sunshine.app.sync.SunshineSyncAdapter;

import android.support.v4.app.Fragment;
import android.support.v4.content.CursorLoader;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.widget.TextView;

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

/**
 * Created by khantilchoksi on 09/03/16.
 */

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class ForecastFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>,
        SharedPreferences.OnSharedPreferenceChangeListener{

    private ForecastAdapter mForecastAdapter;
    private TextView mNoWeatherInfoTextView;
    private final String LOG_TAG = ForecastFragment.class.getSimpleName();

    private AlarmManager alarmMgr;
    private PendingIntent alarmIntent;

    private boolean mUseTodayLayout;

    private static final int FORECAST_LOADER = 0;

    private static final String[] FORECAST_COLUMNS = {
            // In this case the id needs to be fully qualified with a table name, since
            // the content provider joins the location & weather tables in the background
            // (both have an _id column)
            // On the one hand, that's annoying.  On the other, you can search the weather table
            // using the location set by the user, which is only in the Location table.
            // So the convenience is worth it.
            WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_DATE,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.LocationEntry.COLUMN_COORD_LAT,
            WeatherContract.LocationEntry.COLUMN_COORD_LONG
    };

    // These indices are tied to FORECAST_COLUMNS.  If FORECAST_COLUMNS changes, these
    // must change.
    static final int COL_WEATHER_ID = 0;
    static final int COL_WEATHER_DATE = 1;
    static final int COL_WEATHER_DESC = 2;
    static final int COL_WEATHER_MAX_TEMP = 3;
    static final int COL_WEATHER_MIN_TEMP = 4;
    static final int COL_LOCATION_SETTING = 5;
    static final int COL_WEATHER_CONDITION_ID = 6;
    static final int COL_COORD_LAT = 7;
    static final int COL_COORD_LONG = 8;

    private ListView mListView;
    private int mPosition = ListView.INVALID_POSITION;
    static final String STATE_POSITION = "STATE_POSITION";

    @Override
    public void onSaveInstanceState(Bundle outState) {
        //Save the current position
        if(mPosition != ListView.INVALID_POSITION){
            outState.putInt(STATE_POSITION,mPosition);
        }

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(key.equals(getString(R.string.pref_location_status_key))){
            //location status preference has been changed
            updateEmptyView();
        }

    }

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callback {
        /**
         * DetailFragmentCallback for when an item has been selected.
         */
        public void onItemSelected(Uri dateUri);
    }

    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
/*        if (id == R.id.action_refresh) {
            //FetchWeatherTask weatherTask = new FetchWeatherTask();
            //weatherTask.execute();
            updateWeather();
            return true;
        }*/
        if(id == R.id.action_location) {
            openPreferredLocationInMap();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        getLoaderManager().initLoader(FORECAST_LOADER, null,this);
        super.onActivityCreated(savedInstanceState);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        /*String locationSetting = Utility.getPreferredLocation(getActivity());

        // Sort order:  Ascending, by date.
        String sortingOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";

        Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(locationSetting, System.currentTimeMillis());

        Cursor cur = getActivity().getContentResolver().query(
                weatherForLocationUri,
                null,
                null,
                null,
                sortingOrder
        );

        // The CursorAdapter will take data from our cursor and populate the ListView
         // However, we cannot use FLAG_AUTO_REQUERY since it is deprecated, so we will end
         // up with an empty list the first time we run.

        forecastAdapter = new ForecastAdapter(getActivity(),cur, 0);*/

        if(savedInstanceState!= null && savedInstanceState.containsKey(STATE_POSITION)){
            mPosition = savedInstanceState.getInt(STATE_POSITION);
        }

        //Leverages loader lesson
        mForecastAdapter = new ForecastAdapter(getActivity(), null, 0);


        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        /* Previous Static data
        String[] data = {
                "Mon 6/23 - Sunny - 31/17",
                "Tue 6/24 - Foggy - 21/8",
                "Wed 6/25 - Cloudy - 22/17",
                "Thurs 6/26 - Rainy - 18/11",
                "Fri 6/27 - Foggy - 21/10",
                "Sat 6/28 - TRAPPED IN WEATHERSTATION - 23/18",
                "Sun 6/29 - Sunny - 20/7"
        };

        List<String> weekForecast = new ArrayList(Arrays.asList(data));*/

        /*forecastAdapter = new ArrayAdapter<String>(
                // Context
                getActivity(),
                //layout id
                R.layout.list_item_forecast,
                //text view id
                R.id.list_item_forecast_textview,
                //list of string that is forecast data
                new ArrayList<String>());*/

        mListView = (ListView) rootView.findViewById(R.id.listView_forecast);
        mNoWeatherInfoTextView = (TextView) rootView.findViewById(R.id.noWeatherInfo);
        mListView.setEmptyView(mNoWeatherInfoTextView);

        mListView.setAdapter(mForecastAdapter);
        mForecastAdapter.setUseTodayLayout(mUseTodayLayout);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {


                /*String forecast = forecastAdapter.getItem(position);

                CharSequence text = (CharSequence) forecastAdapter.getItem(position);
                int duration = Toast.LENGTH_SHORT;

                Toast toast = Toast.makeText(getActivity(),text,duration);
                toast.show();

                Intent detailActivityIntent = new Intent(getActivity(), DetailActivity.class).putExtra(Intent.EXTRA_TEXT, forecast);
                startActivity(detailActivityIntent);*/

                Cursor cursor = (Cursor) parent.getItemAtPosition(position);

                if (cursor != null) {
                    String locationSetting = Utility.getPreferredLocation(getActivity());
                    /*Intent intent = new Intent(getActivity(), DetailActivity.class)
                            .setData(WeatherContract.WeatherEntry.buildWeatherLocationWithDate(locationSetting, cursor.getLong(COL_WEATHER_DATE)));
                    startActivity(intent);*/
                    ((Callback) getActivity()).
                            onItemSelected(WeatherContract.WeatherEntry.
                                    buildWeatherLocationWithDate(locationSetting, cursor.getLong(COL_WEATHER_DATE)
                                    ));
                }

                //Also save the position to the member variable
                mPosition = position;

            }
        });



        return rootView;
    }
    public void onLocationChanged(){
        updateWeather();
        getLoaderManager().restartLoader(FORECAST_LOADER, null, this);
    }

    public void setUseTodayLayout(boolean useTodayLayout){
        mUseTodayLayout = useTodayLayout;
        if(mForecastAdapter != null){
            mForecastAdapter.setUseTodayLayout(mUseTodayLayout);
        }
    }

    private void updateWeather(){
        /*SharedPreferences locationPreference = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String loc = locationPreference.getString(getString(R.string.pref_location_key),
                getString(R.string.pref_location_default));
        FetchWeatherTask weatherTask = new FetchWeatherTask(getActivity(),forecastAdapter);*/


/*        FetchWeatherTask weatherTask = new FetchWeatherTask(getActivity());
        String location = Utility.getPreferredLocation(getActivity());
        weatherTask.execute(location);*/

/*        //After learning services
        Intent intent = new Intent(getActivity(), SunshineService.class);
        intent.putExtra(SunshineService.LOCATION_QUERY_EXTRA,
                Utility.getPreferredLocation(getActivity()));
        getActivity().startService(intent);*/

/*        //For alarm receiver
        alarmMgr = (AlarmManager)getActivity().getSystemService(Context.ALARM_SERVICE);
        Intent alarmIntent = new Intent(getActivity(), SunshineService.AlarmReceiver.class);
        alarmIntent.putExtra(SunshineService.LOCATION_QUERY_EXTRA, Utility.getPreferredLocation(getActivity()));

        PendingIntent pi = PendingIntent.getBroadcast(getActivity(),0,alarmIntent,PendingIntent.FLAG_ONE_SHOT);
        //Flag indicating that this PendingIntent can be used only once - FLAG_ONE_SHOT

        //Set the AlarmManager to wake up the system
        alarmMgr.set(AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis()+5000, pi);*/

        //Implementing sync adapter
        SunshineSyncAdapter.syncImmediately(getActivity());
    }

    @Override
    public void onStart(){
        super.onStart();
        updateWeather();
    }

    @Override
    public void onResume() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        sp.registerOnSharedPreferenceChangeListener(this);
        super.onResume();
    }

    @Override
    public void onPause() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        sp.unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }


    private void openPreferredLocationInMap(){
        /*SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String loc = pref.getString(getString(R.string.pref_location_key), getString(R.string.pref_location_default));*/

/*        String loc = Utility.getPreferredLocation(getActivity());

        Uri geoLocation = Uri.parse("geo:0,0?").buildUpon().
                appendQueryParameter("q",loc).build();

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(geoLocation);
        if(intent.resolveActivity(getActivity().getPackageManager()) != null){
            startActivity(intent);
        }*/

        if ( null != mForecastAdapter ) {
            Cursor c = mForecastAdapter.getCursor();
            if ( null != c ) {
                c.moveToPosition(0);
                String posLat = c.getString(COL_COORD_LAT);
                String posLong = c.getString(COL_COORD_LONG);
                Uri geoLocation = Uri.parse("geo:" + posLat + "," + posLong);


                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(geoLocation);

                if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                    startActivity(intent);
                } else {
                    Log.d(LOG_TAG, "Couldn't call " + geoLocation.toString() + ", no receiving apps installed!");
                }
            }

        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String locationSetting = Utility.getPreferredLocation(getActivity());

        //Sorting order
        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";
        Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(
                locationSetting, System.currentTimeMillis()
        );



        return new CursorLoader(
                getActivity(),              //Context
                weatherForLocationUri,      //Uri
                FORECAST_COLUMNS,           //Projection
                null,                       //Selection
                null,                       //Selection Args
                sortOrder                   //Sorting Order
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mForecastAdapter.swapCursor(data);
        if(mPosition != ListView.INVALID_POSITION){
            mListView.smoothScrollToPosition(mPosition);
        }

        updateEmptyView();
    }

    private void updateEmptyView() {
        if(mForecastAdapter.isEmpty()){
            if(null!= mNoWeatherInfoTextView){
                int message = R.string.no_weather_info;

                @SunshineSyncAdapter.LocationStatus int locationStatus = Utility.getLocationStatus(getActivity());
                switch (locationStatus){
                    case SunshineSyncAdapter.LOCATION_STATUS_SERVER_DOWN:
                        message = R.string.empty_forecast_list_server_down;
                        break;
                    case SunshineSyncAdapter.LOCATION_STATUS_SERVER_INVALID:
                        message = R.string.empty_forecast_list_server_error;
                        break;
                    default:
                        if(!Utility.isNetworkAvailable(getActivity())){
                            message = R.string.no_internet_connection;
                        }
                }

                mNoWeatherInfoTextView.setText(message);
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
            mForecastAdapter.swapCursor(null);
    }


    /*public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {

        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();

        @Override
        protected String[] doInBackground(String... params) {

            //If no zip code entered
            if (params.length == 0) {
                return null;
            }

            // These two need to be declared outside the try/catch
// so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String forecastJsonStr = null;

            String format = "json";
            String units = "metric";
            int days = 7;

            try

            {
                // Construct the URL for the OpenWeatherMap query
                // Possible parameters are available at OWM's forecast API page, at
                // http://openweathermap.org/API#forecast
                //URL url = new URL("http://api.openweathermap.org/data/2.5/forecast/daily?q=94043&mode=json&units=metric&cnt=7");

                final String FORECAST_BASE_URL = "http://api.openweathermap.org/data/2.5/forecast/daily?";
                final String QUERY_PARAM = "q";
                final String FORMAT_PARAM = "mode";
                final String UNITS_PARAM = "units";
                final String DAYS_PARAM = "cnt";
                final String APPID_PARAM = "APPID";

                Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                        .appendQueryParameter(QUERY_PARAM, params[0])
                        .appendQueryParameter(FORMAT_PARAM, format)
                        .appendQueryParameter(UNITS_PARAM, units)
                        .appendQueryParameter(DAYS_PARAM, Integer.toString(days))
                        .appendQueryParameter(APPID_PARAM, BuildConfig.OPEN_WEATHER_MAP_API_KEY)
                        .build();


                URL url = new URL(builtUri.toString());

                Log.v(LOG_TAG, "BUILT URI " + builtUri.toString());

                //String baseUrl = "http://api.openweathermap.org/data/2.5/forecast/daily?q=94043&mode=json&units=metric&cnt=7";
                //String apiKey = "&APPID=" + BuildConfig.OPEN_WEATHER_MAP_API_KEY;

                //URL url = new URL(baseUrl.concat(apiKey));

                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    //forecastJsonStr = null;
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
                    //forecastJsonStr = null;
                    return null;
                }

                forecastJsonStr = buffer.toString();

                Log.v(LOG_TAG, "Forecast JSON String : " + forecastJsonStr);

            } catch (IOException e) {
                Log.e(LOG_TAG, "Error", e);
                // If the code didn't successfully get the weather data, there's no point in attempting
                // to parse it.
                //forecastJsonStr = null;
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
                return getWeatherDataFromJson(forecastJsonStr, days);
            } catch (JSONException je) {
                Log.e(LOG_TAG, "JSON Exception", je);
                je.printStackTrace();
            }

            return null;
        }


        /* The date/time conversion code is going to be moved outside the asynctask later,
         * so for convenience we're breaking it out into its own method now.

        private String getReadableDateString(long time) {
            // Because the API returns a unix timestamp (measured in seconds),
            // it must be converted to milliseconds in order to be converted to valid date.
            SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
            return shortenedDateFormat.format(time);
        }

        /**
         * Prepare the weather high/lows for presentation.

        private String formatHighLows(double high, double low, String unitType) {
            // For presentation, assume the user doesn't care about tenths of a degree.
            if(unitType.equals(getString(R.string.pref_units_imperial))){
                high = (high*1.8) + 32;
                low = (low*1.8) + 32;
            } else if(!unitType.equals(getString(R.string.pref_units_metric))){
                Log.d(LOG_TAG, "Units Type Not Found : " + unitType);
            }
            long roundedHigh = Math.round(high);
            long roundedLow = Math.round(low);
            String highLowStr = roundedHigh + "/" + roundedLow;
            return highLowStr;
        }

        /**
         * Take the String representing the complete forecast in JSON Format and
         * pull out the data we need to construct the Strings needed for the wireframes.
         * <p/>
         * Fortunately parsing is easy:  constructor takes the JSON string and converts it
         * into an Object hierarchy for us.

        private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays)
                throws JSONException {
            // These are the names of the JSON objects that need to be extracted.
            final String OWM_LIST = "list";
            final String OWM_WEATHER = "weather";
            final String OWM_TEMPERATURE = "temp";
            final String OWM_MAX = "max";
            final String OWM_MIN = "min";
            final String OWM_DESCRIPTION = "main";
            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);
            // OWM returns daily forecasts based upon the local time of the city that is being
            // asked for, which means that we need to know the GMT offset to translate this data
            // properly.
            // Since this data is also sent in-order and the first day is always the
            // current day, we're going to take advantage of that to get a nice
            // normalized UTC date for all of our weather.
            Time dayTime = new Time();
            dayTime.setToNow();
            // we start at the day returned by local time. Otherwise this is a mess.
            int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);
            // now we work exclusively in UTC
            dayTime = new Time();
            String[] resultStrs = new String[numDays];

            SharedPreferences unitsPreference = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String unitType = unitsPreference.getString(getString(R.string.pref_units_key),
                                                getString(R.string.pref_units_metric));

            for (int i = 0; i < weatherArray.length(); i++) {
                // For now, using the format "Day, description, hi/low"
                String day;
                String description;
                String highAndLow;
                // Get the JSON object representing the day
                JSONObject dayForecast = weatherArray.getJSONObject(i);
                // The date/time is returned as a long.  We need to convert that
                // into something human-readable, since most people won't read "1400356800" as
                // "this saturday".
                long dateTime;
                // Cheating to convert this to UTC time, which is what we want anyhow
                dateTime = dayTime.setJulianDay(julianStartDay + i);
                day = getReadableDateString(dateTime);
                // description is in a child array called "weather", which is 1 element long.
                JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);
                // Temperatures are in a child object called "temp".  Try not to name variables
                // "temp" when working with temperature.  It confuses everybody.
                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                double high = temperatureObject.getDouble(OWM_MAX);
                double low = temperatureObject.getDouble(OWM_MIN);
                highAndLow = formatHighLows(high, low, unitType);
                resultStrs[i] = day + " - " + description + " - " + highAndLow;
            }
            for (String s : resultStrs) {
                Log.v(LOG_TAG, "Forecast entry: " + s);
            }
            return resultStrs;
        }

        //@Override
        protected void onPostExecute(String[] result) {
            if (result != null) {
                forecastAdapter.clear();

                for (String dayForecastString : result) {
                    forecastAdapter.add(dayForecastString);
                }
            }
        }

    }*/

}