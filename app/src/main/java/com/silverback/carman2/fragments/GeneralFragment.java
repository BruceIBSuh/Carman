package com.silverback.carman2.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BulletSpan;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.silverback.carman2.BaseActivity;
import com.silverback.carman2.MainActivity;
import com.silverback.carman2.R;
import com.silverback.carman2.StationMapActivity;
import com.silverback.carman2.adapters.PricePagerAdapter;
import com.silverback.carman2.adapters.StationListAdapter;
import com.silverback.carman2.database.CarmanDatabase;
import com.silverback.carman2.database.FavoriteProviderDao;
import com.silverback.carman2.database.GasManagerDao;
import com.silverback.carman2.database.ServiceManagerDao;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.LocationViewModel;
import com.silverback.carman2.models.Opinet;
import com.silverback.carman2.models.OpinetViewModel;
import com.silverback.carman2.models.StationListViewModel;
import com.silverback.carman2.threads.LocationTask;
import com.silverback.carman2.threads.PriceDistrictTask;
import com.silverback.carman2.threads.StationListTask;
import com.silverback.carman2.threads.ThreadManager;
import com.silverback.carman2.utils.Constants;
import com.silverback.carman2.views.OpinetAvgPriceView;
import com.silverback.carman2.views.StationRecyclerView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.silverback.carman2.BaseActivity.formatMilliseconds;

/**
 * A simple {@link Fragment} subclass.
 */
public class GeneralFragment extends Fragment implements
        View.OnClickListener,
        RecyclerView.OnItemTouchListener,
        StationListAdapter.OnRecyclerItemClickListener,
        AdapterView.OnItemSelectedListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(GeneralFragment.class);
    //private static final int NUM_PAGES = 2;
    //private static String GAS_CODE;

    // Objects
    private CarmanDatabase mDB;
    private SharedPreferences mSettings;

    private LocationViewModel locationModel;
    private StationListViewModel stnListModel;
    private OpinetViewModel priceViewModel;

    private LocationTask locationTask;
    private PriceDistrictTask priceDistrictTask;
    private StationListTask stationListTask;
    private OpinetAvgPriceView opinetAvgPriceView;
    private StationRecyclerView stationRecyclerView;
    private StationListAdapter mAdapter;
    private PricePagerAdapter pricePagerAdapter;
    private List<Opinet.GasStnParcelable> mStationList;
    private Location mPrevLocation;
    //private Uri uriStnList;

    // UI's
    private View childView;
    private ViewPager priceViewPager;
    private TextView tvExpLabel, tvLatestExp;
    private TextView tvExpenseSort, tvStationsOrder;
    private FloatingActionButton fabLocation;

    // Fields
    private String stnId;
    private String[] defaults; //defaults[0]:fuel defaults[1]:radius default[2]:sorting
    private boolean bStationsOrder;//true: distance order(value = 2) false: price order(value =1);
    private boolean bExpenseSort;

    public GeneralFragment() {
        // Required empty public constructor
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSettings = ((MainActivity)getActivity()).getSettings();
        // Retrieve the Station ID of the favroite station to show the price.
        mDB = CarmanDatabase.getDatabaseInstance(getContext());
        pricePagerAdapter = new PricePagerAdapter(getChildFragmentManager());

        // Create ViewModels
        locationModel = ViewModelProviders.of(this).get(LocationViewModel.class);
        stnListModel = ViewModelProviders.of(this).get(StationListViewModel.class);
        priceViewModel = ViewModelProviders.of(this).get(OpinetViewModel.class);

        // Fetch the current location using the worker thread and return the value via ViewModel
        // as the type of LiveData, on the basis of which the near stations is to be retrieved.
        locationTask = ThreadManager.fetchLocationTask(getContext(), locationModel);

    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        childView = inflater.inflate(R.layout.fragment_general, container, false);

        TextView tvDate = childView.findViewById(R.id.tv_today);
        Spinner fuelSpinner = childView.findViewById(R.id.spinner_fuel);
        tvExpLabel = childView.findViewById(R.id.tv_label_exp);
        tvLatestExp = childView.findViewById(R.id.tv_exp_stmts);
        tvExpenseSort = childView.findViewById(R.id.tv_expenses_sort);
        tvStationsOrder = childView.findViewById(R.id.tv_stations_order);
        priceViewPager = childView.findViewById(R.id.pager_price);
        opinetAvgPriceView = childView.findViewById(R.id.avgPriceView);
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
        // Change the size of the Floating Action Button on scolling
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


        return childView;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onActivityCreated(Bundle savedStateInstance) {
        super.onActivityCreated(savedStateInstance);

        // Query the favorite provider set in the first place in SettingPreferenceActivity
        mDB.favoriteModel().queryFirstSetFavorite().observe(getViewLifecycleOwner(), data -> {
            for(FavoriteProviderDao.FirstSetFavorite provider : data) {
                if(provider.category == Constants.GAS) stnId = provider.providerId;
                log.i("Station ID: %s", stnId);
            }
        });

        // Retrieve the last gas data and set it in tvRecentExp.
        mDB.gasManagerModel().loadLastGasData().observe(getViewLifecycleOwner(), data -> {
            if(data != null) {
                setLatestExpense(0, data);
            }
        });


        // On fetching the current location, attempt to get the near stations based on the value.
        locationModel.getLocation().observe(getViewLifecycleOwner(), location -> {
            // If the fragment is first created or the current location outbounds UPDATE_DISTANCE,
            // attempt to retreive a new station list based on the location.
            // If a distance b/w a new location and the previous location is within UPDATE_DISTANCE,
            // the station recyclerView should not be refreshed, showing the snackbar message.
            if(mPrevLocation == null || mPrevLocation.distanceTo(location) > Constants.UPDATE_DISTANCE) {
                mPrevLocation = location;
                stationListTask = ThreadManager.startStationListTask(getContext(), stnListModel, location, defaults);
            } else {
                Snackbar.make(childView, getString(R.string.general_snackkbar_inbounds), Snackbar.LENGTH_SHORT).show();
            }
        });

        //priceFavoriteTask = ThreadManager.startFavoritePriceTask(getContext(), priceViewModel, stnId);
        // Invoked from SettingPreferenceActivity as a new region has set.
        priceViewModel.favoritePriceComplete().observe(getViewLifecycleOwner(), isComplete -> {
            // Save the saving time to prevent the regional price data from frequently updating.
            mSettings.edit().putLong(Constants.OPINET_LAST_UPDATE, System.currentTimeMillis()).apply();
            // Attach the pager adatepr with a fuel code set.
            //pricePagerAdapter = new PricePagerAdapter(getChildFragmentManager());
            pricePagerAdapter.setFuelCode(defaults[0]);
            priceViewPager.setAdapter(pricePagerAdapter);
        });

        // Receive the LiveData of station list within a given radius based on
        stnListModel.getStationListLiveData().observe(getViewLifecycleOwner(), stnList -> {
            mStationList = stnList;
            mAdapter = new StationListAdapter(mStationList, this);
            stationRecyclerView.showStationListRecyclerView();
            stationRecyclerView.setAdapter(mAdapter);
        });

        stnListModel.getStationCarWashInfo().observe(getViewLifecycleOwner(), sparseArray -> {
            // Update the carwash info to StationList and notify the data change to Adapter.
            // Adapter should not assume that the payload will always be passed to onBindViewHolder()
            // e.g. when the view is not attached.
            for(int i = 0; i < sparseArray.size(); i++) {
                mStationList.get(i).setIsWash(sparseArray.valueAt(i));
                mAdapter.notifyItemChanged(sparseArray.keyAt(i), sparseArray.valueAt(i));
            }

        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Update the current time using worker thread every 1 minute.
        //clockTask = ThreadManager.startClockTask(getContext(), tvDate);
        // ********** Refactor Required ************
        int numFavorite = mDB.favoriteModel().countFavoriteNumber(Constants.GAS);
        if(numFavorite == 0 || numFavorite == 1) {
            log.i("Special case");
            pricePagerAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        // Refactor required as to how to finish worker threads.
        //if(clockTask != null) clockTask = null;
        if(locationTask != null) locationTask = null;
        if(stationListTask != null) stationListTask = null;
        if(priceDistrictTask != null) priceDistrictTask = null;
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()) {

            case R.id.imgbtn_expense:
                bExpenseSort = !bExpenseSort;
                String sort = (bExpenseSort)?getString(R.string.general_expense_service):getString(R.string.general_expense_gas);
                tvExpenseSort.setText(sort);

                if(!bExpenseSort) {
                    mDB.gasManagerModel().loadLastGasData().observe(getViewLifecycleOwner(), data -> {
                        if(data != null) {
                            setLatestExpense(0, data);
                        } else {
                            tvLatestExp.setText(getString(R.string.general_no_gas_history));
                        }

                    });
                } else {
                    mDB.serviceManagerModel().loadLastSvcData().observe(getViewLifecycleOwner(), data -> {
                        if(data != null) {
                            setLatestExpense(1, data);
                        } else {
                            tvLatestExp.setText(getString(R.string.general_no_svc_history));
                        }
                    });
                }

                break;

            case R.id.imgbtn_stations:
                // Save the station list to prevent reordering from retasking the station list.
                Uri uri = saveNearStationList(mStationList);
                if(uri == null) return;

                bStationsOrder = !bStationsOrder;
                String order = (bStationsOrder)?
                        getString(R.string.general_stations_price):
                        getString(R.string.general_stations_distance);

                tvStationsOrder.setText(order);
                mStationList = mAdapter.sortStationList(bStationsOrder);

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

        switch(position){
            case 0: defaults[0] = "B027"; break; // gasoline
            case 1: defaults[0] = "D047"; break; // diesel
            case 2: defaults[0] = "K015"; break; // LPG
            case 3: defaults[0] = "B034"; break; // premium gasoline
            //case 4: defaults[0] = "B027"; break; // temporarily set to gasoline
            default: break;
        }


        // Retrives the data respectively saved in the cache directory with a fuel selected by the
        // spinner.
        opinetAvgPriceView.addPriceView(defaults[0]);

        // Attach the pager adatepr with a fuel code set.
        pricePagerAdapter = new PricePagerAdapter(getChildFragmentManager());
        pricePagerAdapter.setFuelCode(defaults[0]);
        priceViewPager.setAdapter(pricePagerAdapter);

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
        intent.putExtra("stnId", mStationList.get(position).getStnId());
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

        try {
            FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(list);

            return Uri.fromFile(file);

        } catch (FileNotFoundException e) {
            log.e("FileNotFoundException: %s", e.getMessage());

        } catch (IOException e) {
            log.e("IOException: %s", e.getMessage());

        }

        return null;
    }

    // Make String to display the latest expense information of gas and service as well
    private void setLatestExpense(int mode, Object obj) {
        SpannableStringBuilder sbLabel = new SpannableStringBuilder();
        StringBuilder sbStmts = new StringBuilder();

        switch(mode) {
            case 0: // Gas
                GasManagerDao.RecentGasData gasData = (GasManagerDao.RecentGasData)obj;
                String gasDate = BaseActivity.formatMilliseconds(getString(R.string.date_format_1), gasData.dateTime);

                sbLabel.append(getString(R.string.general_label_gas_date)).append("\n")
                        .append(getString(R.string.general_label_station)).append("\n")
                        .append(getString(R.string.general_label_mileage)).append("\n")
                        .append(getString(R.string.general_label_gas_amount)).append("\n")
                        .append(getString(R.string.general_label_gas_payment));

                sbStmts.append(gasDate).append("\n")
                        .append(gasData.stnName).append("\n")
                        .append(gasData.mileage).append(getString(R.string.unit_km)).append("\n")
                        .append(gasData.gasAmount).append(getString(R.string.unit_liter)).append("\n")
                        .append(gasData.gasPayment).append(getString(R.string.unit_won));

                break;

            case 1: // Service
                ServiceManagerDao.RecentServiceData svcData = (ServiceManagerDao.RecentServiceData)obj;
                String svcDate = BaseActivity.formatMilliseconds(getString(R.string.date_format_1), svcData.dateTime);

                sbLabel.append(getString(R.string.general_label_svc_date)).append("\n")
                        .append(getString(R.string.general_label_svc_provider)).append("\n")
                        .append(getString(R.string.general_label_mileage)).append("\n")
                        .append(getString(R.string.general_label_svc_payment));

                sbStmts.append(svcDate).append("\n")
                        .append(svcData.svcName).append("\n")
                        .append(svcData.mileage).append(getString(R.string.unit_km)).append("\n")
                        .append(svcData.totalExpense).append(getString(R.string.unit_won))
                        .append("\n");

                break;

        }

        final String REGEX_SEPARATOR = "\n";
        final Matcher m = Pattern.compile(REGEX_SEPARATOR).matcher(sbLabel);
        // Set the span for the first bullet
        int start = 0;
        while(m.find()) {
            log.i("set span:%s, %s", m.start(), sbLabel.length());
            sbLabel.setSpan(new BulletSpan(20, Color.RED), start, m.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            start = m.end();
        }

        // Set the span for the last bullet
        sbLabel.setSpan(new BulletSpan(20, Color.RED), start, sbLabel.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        tvExpLabel.setText(sbLabel);
        tvLatestExp.setText(sbStmts.toString());
    }
}
