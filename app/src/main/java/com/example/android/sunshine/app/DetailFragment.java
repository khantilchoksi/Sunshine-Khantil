package com.example.android.sunshine.app;

/**
 * Created by Khantil on 25-05-2016.
 */

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.android.sunshine.app.data.WeatherContract;

/**
 * A placeholder fragment containing a simple view.
 */
public class DetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>{


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
            WeatherContract.WeatherEntry.COLUMN_HUMIDITY,
            WeatherContract.WeatherEntry.COLUMN_PRESSURE,
            WeatherContract.WeatherEntry.COLUMN_WIND_SPEED,
            WeatherContract.WeatherEntry.COLUMN_DEGREES,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            // This works because the WeatherProvider returns location data joined with
            // weather data, even though they're stored in two different tables.
            WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING
    };

    //these constants corresponds to the projection defined above and must change if the projection changes

    private static final int COL_WEATHER_ID = 0;
    private static final int COL_WEATHER_DATE = 1;
    private static final int COL_WEATHER_DESC = 2;
    private static final int COL_WEATHER_MAX_TEMP = 3;
    private static final int COL_WEATHER_MIN_TEMP = 4;
    public static final int COL_WEATHER_HUMIDITY = 5;
    public static final int COL_WEATHER_PRESSURE = 6;
    public static final int COL_WEATHER_WIND_SPEED = 7;
    public static final int COL_WEATHER_DEGREES = 8;
    public static final int COL_WEATHER_CONDITION_ID = 9;

    private ImageView mIconView;
    private TextView mDateView;
    private TextView mDescriptionView;
    private TextView mHighTempView;
    private TextView mLowTempView;
    private TextView mHumidityView;
    private TextView mWindView;
    private TextView mPressureView;
    private TextView mHumidityLabelView;
    private TextView mPressureLabelView;
    private TextView mWindLabelView;

    final int DETAIL_LOADER = 0;

    static final String DETAIL_URI = "URI";
    static final String DETAIL_TRANSITION_ANIMATION = "DTA";
    private Uri mUri;
    private boolean mTransitionAnimation;

//    private ShareActionProvider mShareActionProvider;
    private static final String LOG_TAG = DetailFragment.class.getSimpleName();

    String mForecast;
    private static final String FORECAST_SHARE_HASHTAG = " #SunshineApp";


    public DetailFragment() {
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Bundle arguments = getArguments();
        if(arguments != null){
            mUri = arguments.getParcelable(DetailFragment.DETAIL_URI);
            mTransitionAnimation = arguments.getBoolean(DetailFragment.DETAIL_TRANSITION_ANIMATION, false);
        }

        View rootView = inflater.inflate(R.layout.fragment_detail_start, container, false);
        mIconView = (ImageView) rootView.findViewById(R.id.detail_icon);
        mDateView = (TextView) rootView.findViewById(R.id.detail_date_textview);
        mDescriptionView = (TextView) rootView.findViewById(R.id.detail_forecast_textview);
        mHighTempView = (TextView) rootView.findViewById(R.id.detail_high_textview);
        mLowTempView = (TextView) rootView.findViewById(R.id.detail_low_textview);
        mHumidityView = (TextView) rootView.findViewById(R.id.detail_humidity_textview);
        mHumidityLabelView = (TextView) rootView.findViewById(R.id.detail_humidity_label_textview);
        mWindView = (TextView) rootView.findViewById(R.id.detail_wind_textview);
        mWindLabelView = (TextView) rootView.findViewById(R.id.detail_wind_label_textview);
        mPressureView = (TextView) rootView.findViewById(R.id.detail_pressure_textview);
        mPressureLabelView = (TextView) rootView.findViewById(R.id.detail_pressure_label_textview);

        return rootView;
    }

    public void onLocationChanged(String newLocation){
        Uri uri = mUri;
        if( uri != null){
            long date = WeatherContract.WeatherEntry.getDateFromUri(uri);
            Uri updateUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(newLocation, date);
            mUri = updateUri;
            getLoaderManager().restartLoader(DETAIL_LOADER,null, this);
        }
    }

    private void finishCreatingMenu(Menu menu) {

        // Retrieve the share menu item
        MenuItem menuItem = menu.findItem(R.id.action_share);
        menuItem.setIntent(createShareForecastIntent());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if ( getActivity() instanceof DetailActivity ) {
            // Inflate the menu; this adds items to the action bar if it is present.
            inflater.inflate(R.menu.detailfragment, menu);
            finishCreatingMenu(menu);
        }
    }

    private Intent createShareForecastIntent() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, mForecast + FORECAST_SHARE_HASHTAG);
        return shareIntent;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(DETAIL_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        /*Log.v(LOG_TAG, "In onCreateLoader");
        Intent intent = getActivity().getIntent();
        if (intent == null || intent.getData() == null) {
            return null;
        }*/
        if( mUri != null)
        return new CursorLoader(
                getActivity(),
                mUri,
                FORECAST_COLUMNS,
                null,
                null,
                null
        );

//        getView().setVisibility(View.INVISIBLE);
        //For cardview
        ViewParent vp = getView().getParent();
        if(vp instanceof CardView){
            ((View) vp).setVisibility(View.INVISIBLE);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Log.v(LOG_TAG,"In on load finished");
        if(!data.moveToFirst()){
            Log.v(LOG_TAG," Not fetched any rows");
            return;
        }

//        getView().setVisibility(View.VISIBLE);
        ViewParent vp = getView().getParent();
        if(vp instanceof CardView){
            ((View) vp).setVisibility(View.VISIBLE);
        }
        // Read weather condition ID from cursor
        int weatherId = data.getInt(COL_WEATHER_CONDITION_ID);

        // Use placeholder Image
//        mIconView.setImageResource(R.drawable.ic_launcher);
//        mIconView.setImageResource(Utility.getArtResourceForWeatherCondition(weatherId));

        if ( Utility.usingLocalGraphics(getActivity()) ) {
            mIconView.setImageResource(Utility.getArtResourceForWeatherCondition(weatherId));
        } else {
            Glide.with(this)
                    .load(Utility.getArtUrlForWeatherCondition(getActivity(), weatherId))
                    .error(Utility.getArtResourceForWeatherCondition(weatherId))
                    .crossFade()
                    .into(mIconView);
        }

        long date = data.getLong(COL_WEATHER_DATE);
        String dateText = Utility.getFullFriendlyDayString(getActivity(), date);
        mDateView.setText(dateText);

        //Read weather description
        String description = data.getString(COL_WEATHER_DESC);
        mDescriptionView.setText(description);

//        mIconView.setContentDescription(description);
        // For accessibility, add a content description to the icon field. Because the ImageView
        // is independently focusable, it's better to have a description of the image. Using
        // null is appropriate when the image is purely decorative or when the image already
        // has text describing it in the same UI component.
        mIconView.setContentDescription(getString(R.string.a11y_forecast_icon,description));


        boolean isMetric = Utility.isMetric(getActivity());

        double high = data.getDouble(COL_WEATHER_MAX_TEMP);
        String hightTempreture = Utility.formatTemperature(getActivity(), high);
        mHighTempView.setText(hightTempreture);
        mHighTempView.setContentDescription(getString(R.string.a11y_high_temp,hightTempreture));

        double low = data.getDouble(COL_WEATHER_MIN_TEMP);
        String lowTempreture = Utility.formatTemperature(getActivity(), low);
        mLowTempView.setText(lowTempreture);
        mLowTempView.setContentDescription(getString(R.string.a11y_low_temp,lowTempreture));

        float humidity = data.getFloat(COL_WEATHER_HUMIDITY);
        mHumidityView.setText(getActivity().getString(R.string.format_humidity,humidity));
        mHumidityView.setContentDescription(getString(R.string.a11y_humidity, mHumidityView.getText()));
        mHumidityLabelView.setContentDescription(mHumidityView.getContentDescription());

        float windSpeedDir = data.getFloat(COL_WEATHER_WIND_SPEED);
        float windDirStr = data.getFloat(COL_WEATHER_DEGREES);
        mWindView.setText(Utility.getFormattedWind(getActivity(),windSpeedDir, windDirStr));
        mWindView.setContentDescription(getString(R.string.a11y_wind, mWindView.getText()));
        mWindLabelView.setContentDescription(mWindView.getContentDescription());

        float pressure = data.getFloat(COL_WEATHER_PRESSURE);
        mPressureView.setText(getString(R.string.format_pressure, pressure));
        mPressureView.setContentDescription(getString(R.string.a11y_pressure, mPressureView.getText()));
        mPressureLabelView.setContentDescription(mPressureView.getContentDescription());

        // We still need this for the share intent
        mForecast = String.format("%s - %s - %s/%s", dateText, description, high, low);
        Log.v(LOG_TAG,mForecast);

        /*// If onCreateOptionsMenu has already happened, we need to update the share intent now.
        if (mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(createShareForecastIntent());
        }*/

        AppCompatActivity activity = (AppCompatActivity)getActivity();
        Toolbar toolbarView = (Toolbar) getView().findViewById(R.id.toolbar);

        // We need to start the enter transition after the data has loaded
        if (mTransitionAnimation) {
            activity.supportStartPostponedEnterTransition();

            if ( null != toolbarView ) {
                activity.setSupportActionBar(toolbarView);

                activity.getSupportActionBar().setDisplayShowTitleEnabled(false);
                activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        } else {
            if ( null != toolbarView ) {
                Menu menu = toolbarView.getMenu();
                if ( null != menu ) menu.clear();
                toolbarView.inflateMenu(R.menu.detailfragment);
                finishCreatingMenu(toolbarView.getMenu());
            }
        }

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}
