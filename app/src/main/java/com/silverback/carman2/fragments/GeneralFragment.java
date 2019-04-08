package com.silverback.carman2.fragments;


import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.silverback.carman2.R;
import com.silverback.carman2.StationMapActivity;
import com.silverback.carman2.adapters.StationListAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Constants;
import com.silverback.carman2.models.Opinet;
import com.silverback.carman2.threads.LocationTask;
import com.silverback.carman2.threads.PriceTask;
import com.silverback.carman2.threads.StationMapInfoTask;
import com.silverback.carman2.threads.StationTask;
import com.silverback.carman2.threads.ThreadManager;
import com.silverback.carman2.views.AvgPriceView;
import com.silverback.carman2.views.SidoPriceView;
import com.silverback.carman2.views.SigunPriceView;
import com.silverback.carman2.views.StationPriceView;
import com.silverback.carman2.views.StationRecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import static com.silverback.carman2.BaseActivity.formatMilliseconds;

/**
 * A simple {@link Fragment} subclass.
 */
public class GeneralFragment extends Fragment implements
        View.OnClickListener, RecyclerView.OnItemTouchListener,
        AdapterView.OnItemSelectedListener,
        ThreadManager.OnCompleteTaskListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(GeneralFragment.class);

    // Objects
    private LocationTask locationTask;
    private PriceTask priceTask;
    private StationTask stationTask;
    private StationMapInfoTask mapInfoTask;
    private AvgPriceView avgPriceView;
    private SidoPriceView sidoPriceView;
    private SigunPriceView sigunPriceView;
    private StationPriceView stationPriceView;

    //private FrameLayout frameRecycler;
    private StationRecyclerView stationRecyclerView;
    private RecyclerView.LayoutManager layoutManager;
    private StationListAdapter mAdapter;

    private List<Opinet.GasStnParcelable> mStationList;

    //private Uri uriStationList;
    private Location mCurrentLocation;

    // UI's
    private TextView tvStationsOrder;
    private Spinner fuelSpinner;
    private FrameLayout frameAvgPrice;
    private FloatingActionButton fab;

    // Fields
    private String[] defaults; // defaults[0]:fuel defaults[1]:radius default[2]:sorting
    private boolean bStationsOrder = true;//true: distance order(value = 2) false: price order(value =1);
    private boolean isLocationUpdated;

    public GeneralFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //String[] district = getResources().getStringArray(R.array.default_district);

    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View childView = inflater.inflate(R.layout.fragment_general, container, false);

        TextView tvDate = childView.findViewById(R.id.tv_date);
        tvStationsOrder = childView.findViewById(R.id.tv_stations_order);
        fuelSpinner = childView.findViewById(R.id.spinner_fuel);
        avgPriceView = childView.findViewById(R.id.avgPriceView);
        sidoPriceView = childView.findViewById(R.id.sidoPriceView);
        sigunPriceView = childView.findViewById(R.id.sigunPriceView);
        stationPriceView = childView.findViewById(R.id.stationPriceView);
        stationRecyclerView = childView.findViewById(R.id.stationRecyclerView);
        fab = childView.findViewById(R.id.fab_reload);

        // Attach event listeners
        childView.findViewById(R.id.imgbtn_expense).setOnClickListener(this);
        childView.findViewById(R.id.imgbtn_stations).setOnClickListener(this);
        stationRecyclerView.addOnItemTouchListener(this);

        // Indicate the current time. Refactor required to show the real time using a worker thread.
        String date = formatMilliseconds(getString(R.string.date_format_1), System.currentTimeMillis());
        tvDate.setText(date);

        // Sets the spinner_stat default value if it is saved in SharedPreference.Otherwise, sets it to 0.
        fuelSpinner.setOnItemSelectedListener(this);
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.spinner_fuel_name, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(R.layout.spinner_dropdown);
        fuelSpinner.setAdapter(spinnerAdapter);

        // Set the spinner to the default value that's fetched from SharedPreferences
        String[] code = getResources().getStringArray(R.array.spinner_fuel_code);
        defaults = getArguments().getStringArray("defaults");
        log.i("Default fuel: %s, %s, %s", defaults[0], defaults[1], defaults[2]);
        // Set the initial spinner value with the default from SharedPreferences
        for(int i = 0; i < code.length; i++) {
            if(code[i].matches(defaults[0])){
                fuelSpinner.setSelection(i);
                break;
            }
        }

        // Set Floating Action Button
        // RecycerView.OnScrollListener is an abstract class which shows/hides the floating action
        // button when scolling/idling
        fab.setSize(FloatingActionButton.SIZE_AUTO);
        stationRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener(){
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0 || dy < 0 && fab.isShown()) fab.hide();
            }
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) fab.show();
                super.onScrollStateChanged(recyclerView, newState);
            }
        });

        return childView;
    }

    /*
    @Override
    public void onSaveInstanceState(@NonNull Bundle state) {
        super.onSaveInstanceState(state);
        state.putBoolean("isLocated", isLocationFetched);
    }
    */

    @Override
    public void onResume() {
        super.onResume();
        // Fetch the current location by using FusedLocationProviderClient on a work thread
        locationTask = ThreadManager.fetchLocationTask(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        if(locationTask != null) locationTask = null;
        if(stationTask != null) stationTask = null;
    }


    @Override
    public void onClick(View view) {
        switch(view.getId()) {

            case R.id.imgbtn_expense:
                break;

            case R.id.imgbtn_stations:
                mAdapter.sortStationList(bStationsOrder);
                String sort = (bStationsOrder)?getString(R.string.general_stations_price):
                        getString(R.string.general_stations_distance);
                tvStationsOrder.setText(sort);
                bStationsOrder = !bStationsOrder;
                mAdapter.notifyDataSetChanged();
                break;

        }
    }

    // Abstract methods of AdapterView.OnItemSelectedListener for Spinner,
    // which intially invokes at
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

        log.i("onItemSelected");
        switch(position){
            case 0: defaults[0] = "B027"; break; // gasoline
            case 1: defaults[0] = "D047"; break; // diesel
            case 2: defaults[0] = "K015"; break; // LPG
            case 3: defaults[0] = "B034"; break; // premium gasoline
            case 4: defaults[0] = "B027"; break; // temporarily set to gasoline
            default: break;
        }

        // Retrives the data respectively saved in the cache directory with a fuel selected by the
        // spinner.
        avgPriceView.addPriceView(defaults[0]);
        sidoPriceView.addPriceView(defaults[0]);
        sigunPriceView.addPriceView(defaults[0]);
        stationPriceView.addPriceView(defaults[0]);

        /*
        if(mCurrentLocation != null) {
            log.i("stationTask: %s", stationTask);
            stationRecyclerView.initView(defaults, mCurrentLocation);
        }
        */

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {}

    // The following 3 methods are invoked by RecyclerView.OnItemTouchListener
    @Override
    public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
        log.i("onInterceptTouchEvent");
        return false;
    }

    @Override
    public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
        log.i("onTouchEvent: %s", rv);
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        log.i("onRequestDisallowInterceptTouchEvent");
    }



    // ThreadManager.OnCompleteTaskListener invokes the following callback methods
    // to pass a location fetched by ThreadManager.fetchLocationTask() at first,
    // then, initializes another thread to download a station list based upon the location.
    @Override
    public void onLocationFetched(Location location){

        if(mCurrentLocation == null) mCurrentLocation = location;
        else if(mCurrentLocation.distanceTo(location) < Constants.OPINET_UPDATE_DISTANCE) {
            log.i("Distance is too short to refresh");
            return;
        }

        isLocationUpdated = !isLocationUpdated;
        stationRecyclerView.initView(defaults, location);
    }


    /*
     * The following callback methods are invoked by ThreadManager.OnCompleteTaskListener
     * on having StationTask completed or failed.
     */

    @Override
    public void onStationTaskComplete(List<Opinet.GasStnParcelable> stnList) {
        log.i("StationInfoList: %s", stnList.size());
        mAdapter = new StationListAdapter(stnList);
        stationRecyclerView.showStationListRecyclerView();
        stationRecyclerView.setAdapter(mAdapter);
    }

    @Override
    public void onTaskFailure() {
        log.i("onTaskFailure");
        stationRecyclerView.showTextView("No Stations");
    }

    // On fetching the detailed information of a specific station by picking it in RecyclerView.
    @SuppressWarnings("unchecked")
    @Override
    public void onStatonMapInfoTaskComplete(Opinet.GasStationInfo mapInfo) {

        List<String> mapInfoList = Arrays.asList(mapInfo.getStationName(), mapInfo.getNewAddrs());
        log.i("MapInfo: %s, %s", mapInfo.getStationName(), mapInfo.getNewAddrs());

        Intent intent = new Intent(getActivity(), StationMapActivity.class);
        //intent.putStringArrayListExtra("station_mapinfo", (ArrayList)mapInfoList);
        if(getActivity() != null) getActivity().startActivity(intent);
    }
}
