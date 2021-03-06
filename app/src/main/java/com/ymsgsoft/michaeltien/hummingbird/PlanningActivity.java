package com.ymsgsoft.michaeltien.hummingbird;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.jjobes.slidedatetimepicker.SlideDateTimeListener;
import com.github.jjobes.slidedatetimepicker.SlideDateTimePicker;
import com.ymsgsoft.michaeltien.hummingbird.data.PrefUtils;
import com.ymsgsoft.michaeltien.hummingbird.data.RouteColumns;
import com.ymsgsoft.michaeltien.hummingbird.data.RoutesProvider;
import com.ymsgsoft.michaeltien.hummingbird.playservices.RouteAdapter;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;

public class PlanningActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {
    final String LOG_TAG = PlanningActivity.class.getSimpleName();

//    private SimpleDateFormat mFormatter = new SimpleDateFormat("MMMM dd yyyy hh:mm aa");
    public static final int DIRECTION_LOADER = 0;
    public static final String PLAN_FROM_ID = "com.ymsgsoft.michaeltien.hummingbird.SAVE_FROM_ID";
    public static final String PLAN_TO_ID = "com.ymsgsoft.michaeltien.hummingbird.SAVE_TO_ID";
    public static final String PLAN_TIME_ID = "com.ymsgsoft.michaeltien.hummingbird.SAVE_TIME_ID";
//    final String PLAN_LIST_VISIBLE_ID = "com.ymsgsoft.michaeltien.hummingbird.PLAN_LIST_VISIBLE_ID";
    private final int SEARCH_FROM_REQUEST_ID = 1;
    private final int SEARCH_TO_REQUEST_ID = 2;
    @Bind(R.id.fromTextView) TextView mFromTextView;
    @Bind(R.id.toTextView) TextView mToTextView;
    @Bind(R.id.departTextView) TextView mDepartView;
    @Bind(R.id.routeListView) ListView mRouteListView;
    @Bind(R.id.fragment_planning_id) LinearLayout mListLayout;
    @Bind(R.id.empty_view) TextView mEmptyView;
    @Bind(R.id.marker_progress)  ProgressBar mSpinner;
    protected PlaceObject mFromObject;
    protected PlaceObject mToObject;
    protected RouteAdapter mRouteAdapter;
    protected long mQueryTime;
    @Override
    public void onSaveInstanceState(Bundle outState) {
        if ( mFromObject != null) {
            outState.putParcelable(PLAN_FROM_ID, mFromObject);
            PrefUtils.savePlaceParcelableToPref(this, PlanningActivity.PLAN_FROM_ID, mFromObject);
        }
        if ( mToObject != null) {
            outState.putParcelable(PLAN_TO_ID, mToObject);
            PrefUtils.savePlaceParcelableToPref(this, PlanningActivity.PLAN_TO_ID, mToObject);
        }
//        outState.putBoolean(PLAN_LIST_VISIBLE_ID, mListLayout.getVisibility() == View.VISIBLE);
        outState.putLong(PLAN_TIME_ID, mQueryTime);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();
//        editor.putBoolean(PLAN_LIST_VISIBLE_ID, mListLayout.getVisibility() == View.VISIBLE );
        editor.putLong(PLAN_TIME_ID, mQueryTime);
//        editor.commit();
        editor.apply();
        super.onSaveInstanceState(outState);
    }
    private void updateSearchText(){
        mFromTextView.setText(mFromObject.title);
        mToTextView.setText(mToObject.title);
    }
    private void tryQueryRoutes(long query_time) {
        if ( mFromObject.placeId.isEmpty() || mToObject.placeId.isEmpty()) return;
        String origin = mFromObject.placeId;
        String destination = mToObject.placeId;
        clearDirectionStatus(this);
//        mListLayout.setVisibility(View.INVISIBLE); //no need to set because updateEmptyView will set it
        mSpinner.setVisibility(View.VISIBLE);
        DirectionService.startActionQueryDirection(this, origin, destination, query_time);
        getSupportLoaderManager().restartLoader(DIRECTION_LOADER, null, this);
    }

    private void tryQueryRoutesNow() {
        tryQueryRoutes(System.currentTimeMillis() / 1000);
    }
    private boolean isSameYear( long date1, long date2) {
        Calendar c1 = Calendar.getInstance();
        Calendar c2 = Calendar.getInstance();
        c1.setTime(new Date( date1));
        c2.setTime(new Date( date2));
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR);
    }
    private void showDepartureTime(long query_time) {
        mQueryTime = query_time;
        long now = System.currentTimeMillis();
        // check if today, one day is 60 * 60 * 24 = 86400 seconds
        String time_string;
        String format_string;
        if ((query_time / 86400000) == (now / 86400000)) { // same date, only show time
            format_string = getString(R.string.time_format_simple);
        } else if ( isSameYear(now, query_time)) {
            format_string = getString(R.string.time_format_with_date);
        } else{
            format_string = getString(R.string.time_format_with_year);
        }
        time_string = new SimpleDateFormat(format_string).format(query_time);
        String departLabel = getString(R.string.depart_view_title) + time_string;
        mDepartView.setText(departLabel);
    }
    protected SlideDateTimeListener mListener = new SlideDateTimeListener() {
        @Override
        public void onDateTimeSet(Date date)
        {
            long seconds = date.getTime() / 1000;
            showDepartureTime(date.getTime());
            tryQueryRoutes(seconds);
        }
        // Optional cancel listener
//        @Override
//        public void onDateTimeCancel()
//        {
//            Toast.makeText(PlanningActivity.this,
//                    "Canceled", Toast.LENGTH_SHORT).show();
//        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_planning);
        ButterKnife.bind(this);
//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mRouteAdapter = new RouteAdapter( this, null, 0 );
        mRouteListView.setAdapter(mRouteAdapter);
        mRouteListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // retrieve routeId
                RouteAdapter.RouteHolder selected = (RouteAdapter.RouteHolder) view.getTag();
                mRouteAdapter.selectedRouteId = selected.mData.routeId;
                // launch detail activity
                Intent intent = new Intent(PlanningActivity.this, DetailRouteActivity.class);
                intent.putExtra(DetailRouteActivity.ARG_ROUTE_KEY,selected.mData);
                intent.putExtra(PlanningActivity.PLAN_FROM_ID, mFromObject);
                intent.putExtra(PlanningActivity.PLAN_TO_ID, mToObject);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    // shared element transition
                    Bundle bundle = ActivityOptions
                            .makeSceneTransitionAnimation(PlanningActivity.this, view,
                                    getString(R.string.route_summary_transition))
                            .toBundle();
                    startActivity(intent, bundle);
                } else
                    startActivity(intent);
            }
        });
        if ( savedInstanceState != null) {
            mFromObject = savedInstanceState.getParcelable(PLAN_FROM_ID);
            mToObject = savedInstanceState.getParcelable(PLAN_TO_ID);
//            if (savedInstanceState.getBoolean(PLAN_LIST_VISIBLE_ID))
//              getSupportLoaderManager().initLoader(DIRECTION_LOADER, null, this);
            mQueryTime = savedInstanceState.getLong(PLAN_TIME_ID);
            showDepartureTime(mQueryTime);
        } else {
            Intent intent = getIntent();
            mFromObject = intent.getParcelableExtra(PLAN_FROM_ID);
            if ( mFromObject != null) {
                mToObject = intent.getParcelableExtra(PLAN_TO_ID);
                if (mToObject != null) {
                    tryQueryRoutesNow();
                    showDepartureTime(System.currentTimeMillis());
                }
            } else {
                // from back or up
                getSupportLoaderManager().initLoader(DIRECTION_LOADER, null, this);
                // restore values
                mFromObject = PrefUtils.restorePlaceParcelableFromPref(this, PlanningActivity.PLAN_FROM_ID);
                mToObject = PrefUtils.restorePlaceParcelableFromPref(this, PlanningActivity.PLAN_TO_ID);
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
                mQueryTime = preferences.getLong(PLAN_TIME_ID, 0);
                showDepartureTime(mQueryTime);
//                if ( preferences.getBoolean(PLAN_LIST_VISIBLE_ID, false))
//                    getSupportLoaderManager().initLoader(DIRECTION_LOADER, null, this);
            }
        }
        if ( mFromObject == null) {
            mFromObject = new PlaceObject();
            mFromObject.title = getString(R.string.init_search_here);
            mFromObject.placeId = "";
        }
        if ( mToObject == null) {
            mToObject = new PlaceObject();
            mToObject.title = "";
            mToObject.placeId = "";
        }
        updateSearchText();
//        AdView mAdView = (AdView) findViewById(R.id.adView);
//        AdRequest.Builder builder = new AdRequest.Builder()
//                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR);        // All emulators
////                .addTestDevice("AC98C820A50B4AD8A2106EDE96FB87D4")  // An example device ID
////                .setLocation(currentLocation)
//        AdRequest adRequest = builder.build();
//        mAdView.loadAd(adRequest);

        mDepartView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDateTimeDialog();
            }
        });
        getSupportLoaderManager().initLoader(DIRECTION_LOADER, null, this);
    }
    private void showDateTimeDialog() {
        new SlideDateTimePicker.Builder(getSupportFragmentManager())
                .setListener(mListener)
                .setInitialDate(new Date())
                .setMinDate(Calendar.getInstance().getTime())
                        //.setMaxDate(maxDate)
                        //.setIs24HourTime(true)
                .setTheme(SlideDateTimePicker.HOLO_LIGHT)
                .setIndicatorColor(ResourcesCompat.getColor(getResources(), R.color.colorAccent, null))
                .build()
                .show();
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch( requestCode ){
            case SEARCH_FROM_REQUEST_ID:
                if ( resultCode == Activity.RESULT_OK) {
                    String place_id = data.getStringExtra(PlaceActivity.PLACE_ID);
                    //String place_name = data.getStringExtra(PlaceActivity.PLACE_TEXT);
                    CharSequence place_name = data.getCharSequenceExtra(PlaceActivity.PLACE_TEXT);
                    mFromObject.title = place_name.toString();
                    mFromObject.placeId = place_id;
                    DirectionService.startActionSavePlace(this, mFromObject, System.currentTimeMillis());
                    updateSearchText();
                    tryQueryRoutesNow();
                    showDepartureTime(System.currentTimeMillis());
                }

                break;
            case SEARCH_TO_REQUEST_ID:
                if ( resultCode == Activity.RESULT_OK) {
                    String place_id = data.getStringExtra(PlaceActivity.PLACE_ID);
                    //String place_name = data.getStringExtra(PlaceActivity.PLACE_TEXT);
                    CharSequence place_name = data.getCharSequenceExtra(PlaceActivity.PLACE_TEXT);
                    mToObject.title = place_name.toString();
                    mToObject.placeId = place_id;
                    DirectionService.startActionSavePlace(this, mToObject, System.currentTimeMillis());
                    updateSearchText();
                    tryQueryRoutesNow();
                    showDepartureTime(System.currentTimeMillis());
                }
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader loader) {
        mRouteAdapter.swapCursor(null);
    }

    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        // set filter for current
        return new CursorLoader(this,
                RoutesProvider.Routes.CONTENT_URI,
                null, RouteColumns.IS_ARCHIVE + "=?", new String[]{"0"},null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        int status = updateEmptyView();
        if ( status !=  DirectionService.DIRECTION_STATUS_UNKNOWN)
            mSpinner.setVisibility(View.INVISIBLE);
        mRouteAdapter.swapCursor(cursor);
    }
//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_planning, menu);
//        return true;
//    }
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//
//        //noinspection SimplifiableIfStatement
//        if ( id == R.id.action_settings) {
//            //performSearch();
//            return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }
    @OnClick(R.id.action_up)
    public void backPressed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            supportFinishAfterTransition();
        } else {
            onSupportNavigateUp();
        }
    }
    @OnClick(R.id.from_container)
    public void searchFromPlace() {
        Intent intent = new Intent(PlanningActivity.this, PlaceActivity.class);
        String searchText = mFromObject.title;
        if (!searchText.equals(getString(R.string.init_search_here))) {
            intent.putExtra(PlaceActivity.PLACE_TEXT, searchText);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Bundle bundle = ActivityOptions.makeSceneTransitionAnimation(PlanningActivity.this
            ).toBundle();
            startActivityForResult(intent, SEARCH_FROM_REQUEST_ID, bundle);
        } else
            startActivityForResult(intent, SEARCH_FROM_REQUEST_ID);
    }
    @OnClick(R.id.to_container)
    public void searchToPlace() {
        Intent intent = new Intent(PlanningActivity.this, PlaceActivity.class);
        String searchText = mToObject.title;
        if (!searchText.equals("")) {
            intent.putExtra(PlaceActivity.PLACE_TEXT, searchText);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Bundle bundle = ActivityOptions.makeSceneTransitionAnimation(PlanningActivity.this
            ).toBundle();
            startActivityForResult(intent, SEARCH_TO_REQUEST_ID, bundle);
        } else
            startActivityForResult(intent, SEARCH_TO_REQUEST_ID);
    }
    static private void clearDirectionStatus(Context c) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        SharedPreferences.Editor spe = sp.edit();
        spe.putInt(c.getString(R.string.pref_direction_status_key), DirectionService.DIRECTION_STATUS_UNKNOWN);
        spe.apply();
//        spe.commit();
    }
    @SuppressWarnings("ResourceType")
    static public @DirectionService.DirectionStatus
    int getDirectionStatus(Context c){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        return sp.getInt(c.getString(R.string.pref_direction_status_key), DirectionService.DIRECTION_STATUS_UNKNOWN);
    }
    public int updateEmptyView() {
        Timber.d(LOG_TAG, "updateEmptyView: ");
        @DirectionService.DirectionStatus int status = getDirectionStatus(this);
        int message;
        switch (status) {
            case DirectionService.DIRECTION_STATUS_NO_ROUTE_FOUND:
                Timber.d(LOG_TAG, "DIRECTION_STATUS_NO_ROUTE_FOUND");
                message = R.string.empty_route_no_route;
                break;
            case DirectionService.DIRECTION_STATUS_SERVER_DOWN:
                message = R.string.empty_route_no_server_down;
                break;
            case DirectionService.DIRECTION_STATUS_SERVER_INVALID:
                message = R.string.empty_route_no_server_down;
                break;
            case DirectionService.DIRECTION_STATUS_NO_NETWORK:
                message = R.string.empty_route_network_error;
                break;
            case DirectionService.DIRECTION_STATUS_UNKNOWN:
                Timber.d(LOG_TAG, "DIRECTION_STATUS_UNKNOWN");
                message = R.string.empty_route_unknown;
                break;
            default:
                if ( !Utils.isOnline(this)) {
                    Timber.d(LOG_TAG, "not online");
                    message = R.string.empty_route_network_error;
                    break;
                } else {
                    Timber.d(LOG_TAG, "not empty view");
                    mEmptyView.setVisibility(View.INVISIBLE);
                    mListLayout.setVisibility(View.VISIBLE);
                    return status;
                }
        } // switch
        Timber.d(LOG_TAG, "show empty view");
        mEmptyView.setText(message);
        mEmptyView.setVisibility(View.VISIBLE);
        mListLayout.setVisibility(View.INVISIBLE);
        return status;
    }
}
