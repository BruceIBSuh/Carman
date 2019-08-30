package com.silverback.carman2.fragments;


import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.silverback.carman2.R;
import com.silverback.carman2.StationMapActivity;
import com.silverback.carman2.adapters.StationListAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Constants;
import com.silverback.carman2.models.LocationViewModel;
import com.silverback.carman2.models.Opinet;
import com.silverback.carman2.models.StationListViewModel;
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.silverback.carman2.BaseActivity.formatMilliseconds;

/**
 * A simple {@link Fragment} subclass.
 */
public class GeneralFragment extends Fragment implements
        View.OnClickListener,
        RecyclerView.OnItemTouchListener,
        StationListAdapter.OnRecyclerItemClickListener,
        AdapterView.OnItemSelectedListener {
        //ThreadManager.OnStationTaskListener,
        //ThreadManager.OnStationInfoListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(GeneralFragment.class);

    // Objects
    private LocationViewModel locationModel;
    private StationListViewModel stnListModel;
    private LocationTask locationTask;
    private PriceTask priceTask;
    private StationListTask stationListTask;
    //private StationInfoTask stationInfoTask;
    private AvgPriceView avgPriceView;
    private SidoPriceView sidoPriceView;
    private SigunPriceView sigunPriceView;
    private StationPriceView stationPriceView;

    private StationRecyclerView stationRecyclerView;
    private StationListAdapter mAdapter;
    private List<Opinet.GasStnParcelable> mStationList;

    private Location mPrevLocation;

    // UI's
    private TextView tvStationsOrder;
    private FloatingActionButton fabLocation;

    // Fields
    private String[] defaults; //defaults[0]:fuel defaults[1]:radius default[2]:sorting
    private boolean bStationsOrder = true;//true: distance order(value = 2) false: price order(value =1);
    private static int count = 0;

    public GeneralFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Create ViewModels
        locationModel = ViewModelProviders.of(this).get(LocationViewModel.class);
        stnListModel = ViewModelProviders.of(this).get(StationListViewModel.class);

        // Fetch the current location using the worker thread and return the value via ViewModel
        // as the type of LiveData, on the basis of which the near stations is to be retrieved.
        locationTask = ThreadManager.fetchLocationTask(getContext(), locationModel);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View childView = inflater.inflate(R.layout.fragment_general, container, false);
        TextView tvDate = childView.findViewById(R.id.tv_today);
        Spinner fuelSpinner = childView.findViewById(R.id.spinner_fuel);
        tvStationsOrder = childView.findViewById(R.id.tv_stations_order);
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
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(
                getContext(), R.array.spinner_fuel_name, R.layout.spinner_main_fuel);
        spinnerAdapter.setDropDownViewResource(R.layout.spinner_main_dropdown);
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
        fabLocation.setOnClickListener(this);
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

        /*
         * ViewModels
         * LocationViewModel: data as to the current location using LocationTask
         * StationListViewModel: data as to the stations within a given radius using StationListTask
         */

        // On fetching the current location, attempt to get the near station list based on the value.
        locationModel.getLocation().observe(this, location -> {
            // If the fragment is first created or the current location outbounds UPDATE_DISTANCE,
            // attempt to retreive a new station list based on the location.
            if(mPrevLocation == null || mPrevLocation.distanceTo(location) > Constants.UPDATE_DISTANCE) {
                mPrevLocation = location;
                stationRecyclerView.initView(stnListModel, location, defaults);

            // If a distance b/w a new location and the previous location is within UPDATE_DISTANCE,
            // the station recyclerView should not be refreshed, showing the snackbar message.
            } else {
                CoordinatorLayout layout = getActivity().findViewById(R.id.vg_main);
                Snackbar snackbar = Snackbar.make(
                        layout, getString(R.string.general_snackkbar_inbounds), Snackbar.LENGTH_SHORT);
                snackbar.setAction("Action", null);
                snackbar.show();
            }

        });

        // Receive the LiveData of station list within a given radius based on
        stnListModel.getStationListLiveData().observe(this, stnList -> {
            mStationList = stnList;
            mAdapter = new StationListAdapter(mStationList, this);
            stationRecyclerView.showStationListRecyclerView();
            stationRecyclerView.setAdapter(mAdapter);
        });

        stnListModel.getStationCarWashInfo().observe(this, sparseArray -> {
            // Update the carwash info to StationList and notify the data change to Adapter.
            // Adapter should not assume that the payload will always be passed to onBindViewHolder()
            // e.g. when the view is not attached.
            log.i("mStationList: %s", mStationList.size());
            for(int i = 0; i < sparseArray.size(); i++) {
                mStationList.get(i).setIsWash(sparseArray.valueAt(i));
                mAdapter.notifyItemChanged(sparseArray.keyAt(i), sparseArray.valueAt(i));
            }

            //Uri uri = saveNearStationList(mStationList);
            //log.i("Saved StationList: %s", uri);

        });

        return childView;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Update the current time using worker thread every 1 minute.
        //clockTask = ThreadManager.startClockTask(getContext(), tvDate);
    }

    @Override
    public void onPause() {
        super.onPause();

        // Refactor required as to how to finish worker threads.
        //if(clockTask != null) clockTask = null;
        if(locationTask != null) locationTask = null;
        if(stationListTask != null) stationListTask = null;
        if(priceTask != null) priceTask = null;
        //if(stationInfoTask != null) stationInfoTask = null;
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()) {

            case R.id.imgbtn_expense:
                break;

            case R.id.imgbtn_stations:
                // Save the station list to prevent reordering from retasking the station list.
                Uri uri = saveNearStationList(mStationList);
                if(uri != null) {
                    mAdapter.sortStationList(bStationsOrder);
                    String sort = (bStationsOrder) ?
                            getString(R.string.general_stations_price) : getString(R.string.general_stations_distance);
                    tvStationsOrder.setText(sort);
                    bStationsOrder = !bStationsOrder;
                    mAdapter.notifyDataSetChanged();
                }

                break;

            case R.id.fab_relocation:
                locationTask = ThreadManager.fetchLocationTask(getContext(), locationModel);
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


    // Callback invoked by StationListAdapter.OnRecyclerItemClickListener
    // on clicking an RecyclerView item with fetched station name and id passing to the activity.
    @Override
    public void onItemClicked(final int position) {
        Intent intent = new Intent(getActivity(), StationMapActivity.class);
        intent.putExtra("stationId", mStationList.get(position).getStnId());
        startActivity(intent);
    }

    @SuppressWarnings("ConstantConditions")
    private Uri saveNearStationList(List<Opinet.GasStnParcelable> list) {

        File file = new File(getContext().getCacheDir(), Constants.FILE_CACHED_NEAR_STATIONS);

        // Delete the file before saving a new list.
        if(file.exists()) {
            boolean delete = file.delete();
            if(delete) log.i("cache cleared");
        }

        try(FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(list);

            return Uri.fromFile(file);

        } catch (FileNotFoundException e) {
            log.e("FileNotFoundException: %s", e.getMessage());

        } catch (IOException e) {
            log.e("IOException: %s", e.getMessage());

        }

        return null;
    }


}
