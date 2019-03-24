package com.silverback.carman2.fragments;


import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.silverback.carman2.R;
import com.silverback.carman2.adapters.StationListAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Constants;
import com.silverback.carman2.models.Opinet;
import com.silverback.carman2.threads.LocationTask;
import com.silverback.carman2.threads.OpinetPriceTask;
import com.silverback.carman2.threads.StationTask;
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
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.silverback.carman2.BaseActivity.formatMilliseconds;

/**
 * A simple {@link Fragment} subclass.
 */
public class GeneralFragment extends Fragment implements
        View.OnClickListener,
        AdapterView.OnItemSelectedListener,
        ThreadManager.OnCompleteTaskListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(GeneralFragment.class);

    // Objects
    private OpinetPriceTask priceTask;
    private AvgPriceView avgPriceView;
    private SidoPriceView sidoPriceView;
    private SigunPriceView sigunPriceView;
    private StationPriceView stationPriceView;

    private StationRecyclerView recyclerView;
    private RecyclerView.LayoutManager layoutManager;
    private StationListAdapter mAdapter;

    private List<Opinet.GasStnParcelable> mStationList;

    private Uri uriStationList;

    // UI's
    private TextView tvStationsOrder;
    private Spinner fuelSpinner;
    private FrameLayout frameAvgPrice;

    // Fields
    private String[] defaults;
    private String jsonString;
    private boolean bStationsOrder;

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
        recyclerView = childView.findViewById(R.id.stationRecyclerView);

        childView.findViewById(R.id.imgbtn_expense).setOnClickListener(this);
        childView.findViewById(R.id.imgbtn_stations).setOnClickListener(this);

        String date = formatMilliseconds(getString(R.string.date_format_1), System.currentTimeMillis());
        tvDate.setText(date);

        // Set the spinner_stat default value if it is saved in SharedPreference.Otherwise, sets it to 0.
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.spinner_fuel_name, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(R.layout.spinner_dropdown);
        fuelSpinner.setAdapter(spinnerAdapter);
        fuelSpinner.setOnItemSelectedListener(this);

        // Set the spinner to the default value that's fetched from SharedPreferences
        String[] code = getResources().getStringArray(R.array.spinner_fuel_code);
        defaults = getArguments().getStringArray("defaults");
        log.i("Default fuel: %s", defaults[0]);
        // Set the initial spinner value with the default from SharedPreferences
        for(int i = 0; i < code.length; i++) {
            if(code[i].matches(defaults[0])){
                fuelSpinner.setSelection(i);
                break;
            }
        }

        // Fetch the current location by using FusedLocationProviderClient on a work thread
        LocationTask locationTask = ThreadManager.fetchLocationTask(this);

        return childView;
    }


    @Override
    public void onClick(View view) {
        switch(view.getId()) {

            case R.id.imgbtn_expense:
                break;

            case R.id.imgbtn_stations:
                mAdapter.sortStationList(uriStationList, bStationsOrder);
                bStationsOrder = !bStationsOrder;
                break;

        }
    }

    // Abstract methods of AdapterView.OnItemSelectedListener for Spinner,
    // which intially invokes at
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        switch(position){
            case 0: defaults[0] = "B027"; break; // gasoline
            case 1: defaults[0] = "D047"; break; // diesel
            case 2: defaults[0] = "K015"; break; // LPG
            case 3: defaults[0] = "B034"; break; // premium gasoline
            case 4: defaults[0] = "B027"; break; // temporarily set to gasoline
            default: break;
        }

        avgPriceView.addPriceView(defaults[0]);
        sidoPriceView.addPriceView(defaults[0]);
        sigunPriceView.addPriceView(defaults[0]);
        stationPriceView.addPriceView(defaults[0]);

        avgPriceView.invalidate();
        sidoPriceView.invalidate();
        sigunPriceView.invalidate();
        stationPriceView.invalidate();

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {}


    // ThreadManager.OnCompleteTaskListener invokes the following callback methods
    // to pass a location fetched by ThreadManager.fetchLocationTask() at first,
    // then, initializes another thread to download a station list based upon the location.
    @Override
    public void callbackLocation(Location location){
        log.i("Location: %s, %s", location.getLongitude(), location.getLatitude());
        StationTask stationTask = ThreadManager.startStationListTask(getContext(), defaults, location);
    }
    @Override
    public void callbackStationList(List<Opinet.GasStnParcelable> stnList) {
        saveNearStationInfo(stnList);
        mAdapter = new StationListAdapter(stnList);
        recyclerView.showStationListRecyclerView();
        recyclerView.setAdapter(mAdapter);
    }
    @Override
    public void onTaskFailure() {
        log.i("onTaskFailure");
    }

    @SuppressWarnings("ConstantConditions")
    private void saveNearStationInfo(List<Opinet.GasStnParcelable> list) {

        File file = new File(getContext().getCacheDir(), Constants.FILE_CACHED_STATION_AROUND);
        if(!file.exists()) {
            log.e("File doesn't exist");
            return;
        }

        try(FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream oos = new ObjectOutputStream(fos)) {

            oos.writeObject(list);

        } catch (FileNotFoundException e) {
            log.e("FileNotFoundException: %s", e.getMessage());

        } catch (IOException e) {
            log.e("IOException: %s", e.getMessage());

        }

        uriStationList = Uri.fromFile(file);
    }


}
