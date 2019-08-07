package com.silverback.carman2.fragments;


import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProviders;

import com.silverback.carman2.BaseActivity;
import com.silverback.carman2.ExpenseActivity;
import com.silverback.carman2.R;
import com.silverback.carman2.adapters.RecentExpensePagerAdapter;
import com.silverback.carman2.database.ExpenseBaseEntity;
import com.silverback.carman2.database.CarmanDatabase;
import com.silverback.carman2.database.GasManagerEntity;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Constants;
import com.silverback.carman2.models.FragmentSharedModel;
import com.silverback.carman2.models.LocationViewModel;
import com.silverback.carman2.models.StationListViewModel;
import com.silverback.carman2.threads.StationInfoTask;
import com.silverback.carman2.threads.StationListTask;
import com.silverback.carman2.threads.ThreadManager;
import com.silverback.carman2.utils.CustomPagerIndicator;
import com.silverback.carman2.utils.FavoriteGeofenceHelper;
import com.silverback.carman2.utils.NumberTextWatcher;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * A simple {@link Fragment} subclass.
 */
public class GasManagerFragment extends Fragment implements View.OnClickListener {
        //ThreadManager.OnStationInfoListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(GasManagerFragment.class);

    // Constants
    private static final int GasStation = 1; //favorite gas provider category

    // Objects
    private CarmanDatabase mDB;
    private LocationViewModel locationModel;
    private StationListViewModel stnModel;
    private FragmentSharedModel fragmentSharedModel;

    private FavoriteGeofenceHelper geofenceHelper;
    private StationListTask stationListTask;
    private StationInfoTask stationInfoTask;
    private SharedPreferences mSettings;
    private DecimalFormat df;

    private RecentExpensePagerAdapter viewPagerAdapter;
    private CustomPagerIndicator indicator;
    private Calendar calendar;
    private SimpleDateFormat sdf;
    private NumberPadFragment numPad;
    private Location location;

    // UIs
    private TextView tvOdometer, tvDateTime, tvGasPaid, tvGasLoaded, tvCarwashPaid, tvExtraPaid;
    private EditText etStnName, etUnitPrice, etExtraExpense;
    private ImageButton btnFavorite;

    // Fields
    private String[] defaultParams;
    private int defMileage, defPayment;
    //private String defMileage, defPayment;
    private TextView targetView; //reference to a clicked view which is used in ViewModel
    private String dateFormat;
    private String stnName, stnId, stnCode, stnAddrs;
    private String date;

    private boolean isGeofenceIntent, isFavorite;


    // Constructor
    public GasManagerFragment() {
        // Required empty public constructor
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        defaultParams = getArguments().getStringArray("defaultParams");

        /*
         * Create the instances of the ViewModels
         * FragmentSharedModel: communicate b/w GasManagerFragment and NumberPadFragment
         * LocationViewModel: fetch a current location asynchnonously to have a station, if any.
         * StationListViewModel: fetch a station within a MIN_RADIUS
         */
        fragmentSharedModel = ViewModelProviders.of(getActivity()).get(FragmentSharedModel.class);
        locationModel = ViewModelProviders.of(getActivity()).get(LocationViewModel.class);
        stnModel = ViewModelProviders.of(getActivity()).get(StationListViewModel.class);


        // Entity to retrieve list of favorite station to compare with a fetched current station
        // to tell whether it has registered with Favorite.
        //mDB = CarmanDatabase.getDatabaseInstance(getActivity().getApplicationContext());

        // Create FavoriteGeofenceHelper instance to add or remove a station to Favorte and
        // Geofence list when the favorite button clicks.
        geofenceHelper = new FavoriteGeofenceHelper(getContext());
        mSettings = ((ExpenseActivity)getActivity()).getSettings();
        df = BaseActivity.getDecimalFormatInstance();
        numPad = new NumberPadFragment();

        // Fetch the current location using worker thread, the result of which is returned to
        // getLocation() of ViewModel.LocationViewModel as a LiveData
        //locationTask = ThreadManager.fetchLocationTask(this);
        dateFormat = getString(R.string.date_format_1);
        calendar = Calendar.getInstance(Locale.getDefault());
        sdf = new SimpleDateFormat(dateFormat, Locale.getDefault());
        date = BaseActivity.formatMilliseconds(dateFormat, System.currentTimeMillis());

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        final View localView = inflater.inflate(R.layout.fragment_gas_manager, container, false);

        tvDateTime = localView.findViewById(R.id.tv_date_time);
        etStnName = localView.findViewById(R.id.et_station_name);
        ProgressBar pbStation = localView.findViewById(R.id.pb_search_station);
        ImageButton imgRefresh = localView.findViewById(R.id.imgbtn_refresh);
        btnFavorite = localView.findViewById(R.id.imgbtn_favorite);
        etUnitPrice = localView.findViewById(R.id.et_unit_price);
        tvOdometer = localView.findViewById(R.id.tv_mileage);
        tvGasPaid = localView.findViewById(R.id.tv_total_cost);
        tvGasLoaded = localView.findViewById(R.id.tv_amount);
        tvCarwashPaid = localView.findViewById(R.id.tv_carwash);
        tvExtraPaid = localView.findViewById(R.id.tv_extra);
        etExtraExpense = localView.findViewById(R.id.et_extra_expense);

        tvDateTime.setText(date);
        tvOdometer.setText(mSettings.getString(Constants.ODOMETER, "0"));
        tvGasPaid.setText(mSettings.getString(Constants.PAYMENT, "0"));
        pbStation.setVisibility(View.VISIBLE);

        // Attach event handlers
        etUnitPrice.addTextChangedListener(new NumberTextWatcher(etUnitPrice));
        tvOdometer.setOnClickListener(this);
        tvGasPaid.setOnClickListener(this);
        tvCarwashPaid.setOnClickListener(this);
        tvExtraPaid.setOnClickListener(this);
        btnFavorite.setOnClickListener(view -> registerFavorite());


        // ViewModels to share data b/w fragments(GasManager, ServiceManager, and NumberPadFragment)
        // Return value is of SparseArray type the key of which is the view id of a clicked view.
        fragmentSharedModel.getSelectedValue().observe(this, data -> {
            targetView = localView.findViewById(data.keyAt(0));
            if(targetView != null) {
                targetView.setText(df.format(data.valueAt(0)));
                calculateGasAmount();
            }
        });

        // Attach an observer to fetch a current location from LocationTask, then initiate
        // StationListTask based on the value.
        locationModel.getLocation().observe(this, location -> {
            log.i("location fetched");
            this.location = location;
            stationListTask = ThreadManager.startStationListTask(getContext(), stnModel, location, defaultParams);
        });

        // Check if a fetched current station has registered with Favorite right after StationListModel
        // is notified to retrieve a current station. Then, get StationInfoTask started to get
        // its address, completion of which is notified by the same ViewModel.
        stnModel.getCurrentStationLiveData().observe(this, currentStation -> {
            stnName = currentStation.getStnName();
            stnId = currentStation.getStnId();
            log.i("Current Station: %s, %s: ", stnName, stnId);

            etStnName.setText(stnName);
            etUnitPrice.setText(String.valueOf(currentStation.getStnPrice()));
            etStnName.setCursorVisible(false);
            etUnitPrice.setCursorVisible(false);

            // Query Favorite with the fetched station name or station id to tell whether the station
            // has registered with Favorite.
            //checkFavorite(stnName, stnId);

            pbStation.setVisibility(View.GONE);
            imgRefresh.setVisibility(View.VISIBLE);
        });

        // When fetching the address, the station is registered with Favorite passing detailed info
        // to FavoriteGeofenceHelper as params
        stnModel.getStationInfoLiveData().observe(this, stnInfo -> {
            log.i("onStationInfoTaskComplete: %s", stnInfo.getNewAddrs());
            stnAddrs = stnInfo.getNewAddrs();
            stnCode = stnInfo.getStationCode();

            // Once a current station is fetched, retrieve the station info(station address) which
            // is passed over to addFavoriteGeofence() in FavoriteGeofenceHelper.
            geofenceHelper.setGeofenceParam(GasStation, stnId, location);
            geofenceHelper.addFavoriteGeofence(stnName, stnCode, stnAddrs);
            btnFavorite.setBackgroundResource(R.drawable.btn_favorite_selected);


        });


        return localView;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Notify ExpensePagerFragment of the current fragment to load the recent 5 data from
        // GasTable.
        fragmentSharedModel.setCurrentFragment(this);

    }

    @Override
    public void onPause() {
        super.onPause();
        if(stationListTask != null) stationListTask = null;
        if(stationInfoTask != null) stationInfoTask = null;
    }

    // InputNumPad handler
    @Override
    public void onClick(final View v) {

        Bundle args = new Bundle();
        String initValue = null;
        targetView = (TextView)v;

        // Pass the current saved value to NumberPadFragment
        switch(v.getId()) {
            case R.id.tv_mileage:
                //title = tvOdometer.getText().toString();
                initValue = tvOdometer.getText().toString();
                break;

            case R.id.tv_total_cost:
                //title = tvGasPaid.getText().toString();
                initValue = tvGasPaid.getText().toString();
                break;

            case R.id.tv_carwash:
                //title = tvCarwashPaid.getText().toString();
                initValue = tvCarwashPaid.getText().toString();
                break;

            case R.id.tv_extra:
                //title = tvExtraPaid.getText().toString();
                initValue = tvExtraPaid.getText().toString();
                break;
        }

        // Pass the id of TextView to NumberPadFragment for which TextView is being focused to wait
        // for a new value.
        //NumberPadFragment.newInstance(null, initValue, v.getId()).show(getFragmentManager(), "numPad");

        args.putString("title", null);
        args.putInt("viewId", v.getId());
        args.putString("initValue", initValue);
        numPad.setArguments(args);

        if(getFragmentManager() != null) numPad.show(getFragmentManager(), "InputPadDialog");

    }

    // Query Favorite with the fetched station name or station id to tell whether the station
    // has registered with Favorite, and the result is notified as a LiveData.
    private void checkFavorite(String name, String id) {
        LiveData<String> favoriteName = mDB.favoriteModel().findFavoriteName(name, id);
        favoriteName.observe(this, favorite -> {
            if (TextUtils.isEmpty(favorite)) {
                log.i("favorite not found");
                isFavorite = false;
                btnFavorite.setBackgroundResource(R.drawable.btn_favorite);
            } else {
                log.i("favorite not found");
                isFavorite = true;
                btnFavorite.setBackgroundResource(R.drawable.btn_favorite_selected);
            }
        });
    }


    // Invoked when Favorite button clicks in order to add or remove the current station to or out of
    // Favorite and Geofence list.
    private void registerFavorite() {
        log.i("registerFavorite");
        if(TextUtils.isEmpty(etStnName.getText())) return;

        if(isFavorite) {
            geofenceHelper.removeFavoriteGeofence(stnName, stnId);
            btnFavorite.setBackgroundResource(R.drawable.btn_favorite);

        } else {
            // Initiate StationInfoTask to fetch an address of the current station.
            stationInfoTask = ThreadManager.startStationInfoTask(this, stnName, stnId);
        }

        isFavorite = !isFavorite;
    }

    // Method for inserting data to SQLite database
    public boolean saveGasData(){

        // Null check for the parent activity
        if(!doEmptyCheck()) return false;

        //ContentValues values = new ContentValues();
        long milliseconds = BaseActivity.parseDateTime(dateFormat, tvDateTime.getText().toString());
        //int gas, wash, extra;

        // Create Entity instances both of which are correlated by Foreinkey
        ExpenseBaseEntity basicEntity = new ExpenseBaseEntity();
        GasManagerEntity gasEntity = new GasManagerEntity();

        basicEntity.dateTime = milliseconds;
        basicEntity.category = 1;
        gasEntity.stnName = etStnName.getText().toString();
        gasEntity.stnAddrs = stnAddrs;
        gasEntity.stnId = stnId;
        gasEntity.extraExpense = etExtraExpense.getText().toString();
        //gasEntity.basicId = basicEntity.serviceId;

        try {
            basicEntity.mileage = df.parse(tvOdometer.getText().toString()).intValue();
            gasEntity.gasPayment = df.parse(tvGasPaid.getText().toString()).intValue();
            gasEntity.gasAmount = df.parse(tvGasLoaded.getText().toString()).intValue();
            gasEntity.unitPrice = df.parse(etUnitPrice.getText().toString()).intValue();
            gasEntity.washPayment = df.parse(tvCarwashPaid.getText().toString()).intValue();
            gasEntity.extraPayment = df.parse(tvExtraPaid.getText().toString()).intValue();

        } catch(ParseException e) {
            log.e("ParseException: %s", e);
        }

        basicEntity.totalExpense = gasEntity.gasPayment + gasEntity.washPayment + gasEntity.extraPayment;

        int rowId = mDB.gasManagerModel().insertBoth(basicEntity, gasEntity);

        if(rowId > 0) {
            mSettings.edit().putString(Constants.ODOMETER, tvOdometer.getText().toString()).apply();
            Toast.makeText(getActivity(), getString(R.string.toast_save_success), Toast.LENGTH_SHORT).show();
            return true;
        } else return false;

    }

    // Method to make an empty check. When successfully fetching the gas station and the price,
    // those 2 values automatically fill the views. In this case, only the payment value will be
    // checked.
    private boolean doEmptyCheck() {

        // Check if the gas station name is empty
        if(TextUtils.isEmpty(etStnName.getText())) {
            String msg = getResources().getString(R.string.toast_station_name);
            Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
            etStnName.requestFocus();
            return false;
        }

        // Check if the gas unit price is empty
        if(TextUtils.isEmpty(etUnitPrice.getText())) {
            String msg = getResources().getString(R.string.toast_unit_price);
            Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
            etUnitPrice.requestFocus();
            return false;
        }

        // Check if the payment is empty.
        if(tvGasPaid.getText().toString().matches("0")) {
            String msg = getResources().getString(R.string.toast_payment);
            Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    // Calculate an gas amount loaded with a given payment and unit price.  Unless a unit price is
    // given, toast a message to ask for inputting the price.
    private void calculateGasAmount() {

        if(TextUtils.isEmpty(etUnitPrice.getText())) {
            Toast.makeText(getActivity(), R.string.toast_unit_price, Toast.LENGTH_SHORT).show();

        } else if(!TextUtils.isEmpty(etUnitPrice.getText()) && !TextUtils.isEmpty(tvGasPaid.getText())) {
            try {
                int price = df.parse(etUnitPrice.getText().toString()).intValue();
                int paid = df.parse(tvGasPaid.getText().toString()).intValue();
                String gasAmount = String.valueOf(paid/price);
                tvGasLoaded.setText(gasAmount);
            } catch(ParseException e) {
                log.e("ParseException: %s", e.getMessage());
            }
        }
    }



}
