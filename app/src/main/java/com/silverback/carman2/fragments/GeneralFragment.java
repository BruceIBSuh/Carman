package com.silverback.carman2.fragments;


import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.silverback.carman2.BaseActivity;
import com.silverback.carman2.R;
import com.silverback.carman2.StationMapActivity;
import com.silverback.carman2.adapters.StationListAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Constants;
import com.silverback.carman2.models.Opinet;
import com.silverback.carman2.threads.ClockTask;
import com.silverback.carman2.threads.LocationTask;
import com.silverback.carman2.threads.PriceTask;
import com.silverback.carman2.threads.StationInfoTask;
import com.silverback.carman2.threads.StationListTask;
import com.silverback.carman2.threads.ThreadManager;
import com.silverback.carman2.views.AvgPriceView;
import com.silverback.carman2.views.SidoPriceView;
import com.silverback.carman2.views.SigunPriceView;
import com.silverback.carman2.views.StationPriceView;
import com.silverback.carman2.views.StationRecyclerView;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import static android.content.Context.POWER_SERVICE;
import static com.silverback.carman2.BaseActivity.formatMilliseconds;

/**
 * A simple {@link Fragment} subclass.
 */
public class GeneralFragment extends Fragment implements
        View.OnClickListener,
        RecyclerView.OnItemTouchListener, StationListAdapter.OnRecyclerItemClickListener,
        AdapterView.OnItemSelectedListener,
        ThreadManager.OnCompleteTaskListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(GeneralFragment.class);

    // Objects
    private Handler clockHandler;
    private ClockTask clockTask;
    private LocationTask locationTask;
    private PriceTask priceTask;
    private StationListTask stationListTask;
    private StationInfoTask stationInfoTask;
    private Thread timeThread;
    private AvgPriceView avgPriceView;
    private SidoPriceView sidoPriceView;
    private SigunPriceView sigunPriceView;
    private StationPriceView stationPriceView;
    private ConstraintLayout rootLayout;

    // FirebaseFirestore
    private FirebaseFirestore db;

    //private FrameLayout frameRecycler;
    private StationRecyclerView stationRecyclerView;
    private RecyclerView.LayoutManager layoutManager;
    private StationListAdapter mAdapter;

    private List<Opinet.GasStnParcelable> mStationList;

    //private Uri uriStationList;
    private Location mCurrentLocation, mPrevLocation;

    // UI's
    private TextView tvDate, tvStationsOrder;
    private Spinner fuelSpinner;
    private FrameLayout frameAvgPrice;
    private FloatingActionButton fabLocation;

    // Fields
    //private String tmpStationName;
    private String today;
    private boolean isLocationFetched = false;//prevent StationListTask from repaeating when adding the fragment.
    private String[] defaults; //defaults[0]:fuel defaults[1]:radius default[2]:sorting
    private boolean bStationsOrder = true;//true: distance order(value = 2) false: price order(value =1);

    public GeneralFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        if(savedInstanceState != null) {
            isLocationFetched = savedInstanceState.getBoolean("isLocationFetched");
            log.i("SavedInstanceState: %s", isLocationFetched);
        }

        View childView = inflater.inflate(R.layout.fragment_general, container, false);

        tvDate = childView.findViewById(R.id.tv_today);
        tvStationsOrder = childView.findViewById(R.id.tv_stations_order);
        fuelSpinner = childView.findViewById(R.id.spinner_fuel);
        avgPriceView = childView.findViewById(R.id.avgPriceView);
        sidoPriceView = childView.findViewById(R.id.sidoPriceView);
        sigunPriceView = childView.findViewById(R.id.sigunPriceView);
        stationPriceView = childView.findViewById(R.id.stationPriceView);
        stationRecyclerView = childView.findViewById(R.id.stationRecyclerView);
        fabLocation = childView.findViewById(R.id.fab_relocation);

        // Attach event listeners
        childView.findViewById(R.id.imgbtn_expense).setOnClickListener(this);
        childView.findViewById(R.id.imgbtn_stations).setOnClickListener(this);
        stationRecyclerView.addOnItemTouchListener(this);

        // Display the current time. Refactor required to show the real time using a worker thread.
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
        fabLocation.setSize(FloatingActionButton.SIZE_AUTO);
        stationRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener(){
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0 || dy < 0 && fabLocation.isShown()) fabLocation.hide();
            }
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) fabLocation.show();
                super.onScrollStateChanged(recyclerView, newState);
            }
        });

        fabLocation.setOnClickListener(this);

        /*
         * Initiates LocationTask to fetch the current location only when the flag, isLocationFetched
         * is set to false, in case of which may be at the first time and when the fab is clicked.
         * StationListTask should be launched only if the current location is newly retrieved in the
         * call back method, onLocationFetched().
         */
        if(!isLocationFetched) {
            log.i("No location fetched");
            locationTask = ThreadManager.fetchLocationTask(this);
        } else {
            stationRecyclerView.setAdapter(mAdapter);
        }

        return childView;
    }

    @Override
    public void onResume() {
        super.onResume();
        log.i("onResume");

        // Update the current time using worker thread every 1 minute.
        clockTask = ThreadManager.startClockTask(getContext(), tvDate);

    }

    @Override
    public void onPause() {
        super.onPause();

        // Refactor required as to how to finish worker threads.
        if(clockTask != null) clockTask = null;
        if(locationTask != null) locationTask = null;
        if(stationListTask != null) stationListTask = null;
        if(priceTask != null) priceTask = null;
        if(stationInfoTask != null) stationInfoTask = null;
    }

    @Override
    public void onStop(){
        super.onStop();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("isLocationFetched", isLocationFetched);
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

            case R.id.fab_relocation:
                isLocationFetched = true;
                locationTask = ThreadManager.fetchLocationTask(GeneralFragment.this);

                break;

        }
    }

    // The following 2 abstract methods are invoked by AdapterView.OnItemSelectedListener for Spinner,
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

        /* Initiates StationListTask to retreive a new station list with a fuel set by Spinner.
        if(isLocationFetched) {
            log.i("stationListTask: %s", stationListTask);
            //stationRecyclerView.initView(defaults, mCurrentLocation);
        }
        */
    }
    @Override
    public void onNothingSelected(AdapterView<?> parent) {}

    /**
     * The following 3 methods are invoked by RecyclerView.OnItemTouchListener
     */
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


    // StationListAdapter.OnRecyclerItemClickListener invokes this when clicking
    // a cardview item, passing a position of the item.
    @Override
    public void onItemClicked(int position) {
        //tmpStationName = mStationList.get(position).getStnName();
        stationInfoTask = ThreadManager.startStationInfoTask(getContext(),
                mStationList.get(position).getStnName(), mStationList.get(position).getStnId());
    }

    /**
     * The following methods are callbacks invoked by ThreadManager.OnCompleteTaskListener.
     * onLocationFetched():
     * onStationListTaskComplete():
     * onTaskFailure():
     * onStationInfoTaskComplete():
     */
    // ThreadManager.OnCompleteTaskListener invokes the following callback methods
    // to pass a location fetched by ThreadManager.fetchLocationTask() at first,
    // then, initializes another thread to download a station list based upon the location.
    @SuppressWarnings("ConstantConditions")
    @Override
    public void onLocationFetched(Location location){
        //isLocationFetched = true;
        mCurrentLocation = location;
        stationRecyclerView.initView(defaults, location);

        // On clicking the FAB, check if the current location outbounds the distance set in
        // Constants.UPDATE_DISTANC compared with the previous location.
        // If so, retrieve a new near station list and invalidate StationRecyclerView with new data.
        if(!isLocationFetched || mCurrentLocation.distanceTo(location) > Constants.UPDATE_DISTANCE) {
            mCurrentLocation = location;
            stationRecyclerView.initView(defaults, mCurrentLocation);

        // Show Snackbar if it is within the UPDATE_DISTANCE
        } else {
            log.i("Inbounds");
            CoordinatorLayout layout = getActivity().findViewById(R.id.vg_main);
            Snackbar snackbar = Snackbar.make(
                    layout, getString(R.string.general_snackkbar_inbounds), Snackbar.LENGTH_SHORT);
            snackbar.setAction("Action", null);
            snackbar.show();

        }
    }

    // The following 2 callback methods are invoked by ThreadManager.OnCompleteTaskListener
    // on having StationListTask completed or failed.
    @Override
    public void onStationListTaskComplete(List<Opinet.GasStnParcelable> stnList) {
        log.i("StationList: %s", stnList.size());
        mStationList = stnList;
        mAdapter = new StationListAdapter(stnList, this);
        stationRecyclerView.showStationListRecyclerView();
        stationRecyclerView.setAdapter(mAdapter);
    }

    // Invoked when StationInfoRunnable has retrieved Opinet.GasStationInfo, which initiated
    // clicking a cardview item of StationRecyclerView set with StationListAdapter.
    @Override
    public void onStationInfoTaskComplete(Opinet.GasStationInfo info) {

        Bundle bundle = new Bundle();
        bundle.putString("stationName", info.getStationName());
        bundle.putString("stationAddrs", info.getNewAddrs());
        bundle.putString("stationTel", info.getTelNo());
        bundle.putString("isCarWash", info.getIsCarWash());
        bundle.putString("isService", info.getIsService());
        bundle.putString("isCVS", info.getIsCVS());
        bundle.putString("xCoord", info.getXcoord());
        bundle.putString("yCoord", info.getYcoord());

        Intent intent = new Intent(getActivity(), StationMapActivity.class);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    @Override
    public void onTaskFailure() {
        log.i("onTaskFailure");
        stationRecyclerView.showTextView("No Stations");
    }


}
