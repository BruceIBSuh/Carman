package com.silverback.carman2.fragments;


import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;


import com.silverback.carman2.BaseActivity;
import com.silverback.carman2.R;
import com.silverback.carman2.adapters.ExpensePagerAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Constants;
import com.silverback.carman2.models.DataProviderContract;
import com.silverback.carman2.models.FragmentSharedModel;
import com.silverback.carman2.models.Opinet;
import com.silverback.carman2.threads.LocationTask;
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
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProviders;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.viewpager.widget.ViewPager;

/**
 * A simple {@link Fragment} subclass.
 */
public class GasManagerFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor>,
        View.OnClickListener,
        ThreadManager.OnLocationTaskListener,
        ThreadManager.OnStationTaskListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(GasManagerFragment.class);

    // Constants
    private static final int GasStation = 1;

    // Objects
    private FavoriteGeofenceHelper geofenceHelper;
    private LoaderManager loaderManager;
    private LocationTask locationTask;
    private StationListTask stationListTask;
    private StationInfoTask stationInfoTask;
    private SharedPreferences mSettings;
    private DecimalFormat df;
    private FragmentSharedModel viewModel;
    private ExpensePagerAdapter viewPagerAdapter;
    private CustomPagerIndicator indicator;
    private Calendar calendar;
    private SimpleDateFormat sdf;
    private InputPadFragment padDialog;
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

    private boolean isGeofenceIntent, isFavorite;


    // Constructor
    public GasManagerFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // ViewModel instance
        if(getActivity() != null) {
            viewModel = ViewModelProviders.of(getActivity()).get(FragmentSharedModel.class);
        }

        // Get Default Params(FuelCode, Searching Radius, Sorting order);
        if(getArguments() != null) {
            defaultParams = getArguments().getStringArray("defaultParams");
        }

        // Create FavoriteGeofenceHelper instance to add or remove a station to Favorte and
        // Geofence list when the favorite button clicks.
        geofenceHelper = new FavoriteGeofenceHelper(getContext());

        mSettings = BaseActivity.getSharedPreferenceInstance(getActivity());
        df = BaseActivity.getDecimalFormatInstance();

        // Inflate the layout for this fragment
        final View localView = inflater.inflate(R.layout.fragment_gas, container, false);
        tvDateTime = localView.findViewById(R.id.tv_date_time);
        etStnName = localView.findViewById(R.id.et_station_name);
        btnFavorite = localView.findViewById(R.id.imgbtn_favorite);
        etUnitPrice = localView.findViewById(R.id.et_unit_price);
        tvOdometer = localView.findViewById(R.id.tv_mileage);
        tvGasPaid = localView.findViewById(R.id.tv_payment);
        tvGasLoaded = localView.findViewById(R.id.tv_amount);
        tvCarwashPaid = localView.findViewById(R.id.tv_carwash);
        tvExtraPaid = localView.findViewById(R.id.tv_extra);
        etExtraExpense = localView.findViewById(R.id.et_extra_expense);

        dateFormat = getString(R.string.date_format_1);
        calendar = Calendar.getInstance(Locale.getDefault());
        sdf = new SimpleDateFormat(dateFormat, Locale.getDefault());
        String date = BaseActivity.formatMilliseconds(dateFormat, System.currentTimeMillis());
        tvDateTime.setText(date);

        tvOdometer.setText(mSettings.getString(Constants.ODOMETER, "0"));
        tvGasPaid.setText(mSettings.getString(Constants.PAYMENT, "50,000"));


        etUnitPrice.addTextChangedListener(new NumberTextWatcher(etUnitPrice));
        tvOdometer.setOnClickListener(this);
        tvGasPaid.setOnClickListener(this);
        tvGasLoaded.setOnClickListener(this);
        tvCarwashPaid.setOnClickListener(this);
        tvExtraPaid.setOnClickListener(this);

        btnFavorite.setOnClickListener(view -> {
            handleFavorite();
        });


        /*
         * Introduce ViewModel to communicate between parent Fragment and AlertFragment
         * Set Observier to ViewModel(Lamda expression available, instead).
         */
        /*
        viewModel.getInputValue().observe(this, new Observer<String>(){
            @Override
            public void onChanged(String data) {
                log.i("viewMode value:%s", data);
                targetView.setText(data);
            }
        });
        */
        // Pass a value in InputPad to Fragment by using Lambda expresstion
        // Pass a current fragment to a ExpensePagerFragment
        viewModel.getInputValue().observe(this, data -> targetView.setText(data));
        viewModel.setCurrentFragment(this);

        return localView;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Fetch the current location on a worker thread.
        locationTask = ThreadManager.fetchLocationTask(this);

        /*
         * ViewModel to communicate b/w frgments directly
         * setCurrentFrgment(): pass the current fragment to ExpensePagerFragment
         * getInputValue().observe(): get a number input on the number pad.
         */
        viewModel.setCurrentFragment(this);
        viewModel.getInputValue().observe(this, data -> targetView.setText(data));

    }

    @Override
    public void onPause() {
        super.onPause();
        if(locationTask != null) locationTask = null;
        if(stationListTask != null) stationListTask = null;
        if(stationInfoTask != null) stationInfoTask = null;
    }

    // Handles the click event of InputPadFragment
    @Override
    public void onClick(final View v) {
        Bundle args = new Bundle();
        padDialog = new InputPadFragment();
        targetView = (TextView)v;

        // Pass the current saved value to InputPadFragment
        switch(v.getId()) {
            case R.id.tv_mileage:
                args.putString("value", tvOdometer.getText().toString());
                break;

            case R.id.tv_payment:
                args.putString("value", tvGasPaid.getText().toString());
                break;

            case R.id.tv_amount:
                args.putString("value", tvGasLoaded.getText().toString());
                break;

            case R.id.tv_carwash:
                args.putString("value", tvCarwashPaid.getText().toString());
                break;

            case R.id.tv_extra:
                args.putString("value", tvExtraPaid.getText().toString());
                break;
        }

        // Pass the id of TextView to InputPadFragment for which TextView is being focused to wait
        // for a new value.
        args.putInt("viewId", v.getId());
        padDialog.setArguments(args);

        if(getFragmentManager() != null) padDialog.show(getFragmentManager(), "InputPadDialog");

    }

    // ThreadManager.OnLocationTaskListener invokes
    @Override
    public void onLocationFetched(Location location) {
        log.i("GasManagerFragment Location: %s", defaultParams[1]);
        this.location = location;
        stationListTask = ThreadManager.startStationListTask(this, location, defaultParams);
    }

    // ThreadManager.OnStationTaskListener invokes the following 3 callback methods
    // to get the current statin located within MIN_RADIUS.
    @Override
    public void onStationListTaskComplete(List<Opinet.GasStnParcelable> result) {

        if(result.size() == 0) return;

        stnName = result.get(0).getStnName();
        stnId = result.get(0).getStnId();
        stnCode = result.get(0).getStnCode();

        etStnName.setText(stnName);
        etUnitPrice.setText(String.valueOf(result.get(0).getStnPrice()));
        etStnName.setCursorVisible(false);
        etUnitPrice.setCursorVisible(false);

        // Check if the curren station has registered with Favorite.
        Bundle bundle = new Bundle();
        bundle.putString("stnName", result.get(0).getStnName());
        loaderManager = LoaderManager.getInstance(this);
        loaderManager.initLoader(0, bundle, this);

        // Initiate StationInfoTask to fetch an address of the current station.
        //stationInfoTask = ThreadManager.startStationInfoTask(this, stnName, stnId);

    }

    @Override
    public void onStationInfoTaskComplete(Opinet.GasStationInfo mapInfo) {
        log.i("onStationInfoTaskComplete");
    }

    @Override
    public void onTaskFailure() {
        log.i("onTaskFailure");
    }

    /**
     * LoaderManager.LoaderCallback<Cursor> invokes the following 3 overriding methods
     * to find if a fetched current station within MIN_RADIUS has already registered w/ Favorite.
     *
     * onCreateLoader()
     * @param id id for recognizing each loader when multiple loaders exist.
     * @param args passed from loader instance when creating using initLoader()
     *
     * onLoadFinished()
     * onLoadResete()
     */
    @SuppressWarnings("ConstantConditions")
    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {

        Uri uriFavorite = DataProviderContract.FAVORITE_TABLE_URI;
        String stationName = args.getString("stnName");

        final String[] projection = {
                DataProviderContract.FAVORITE_ID,
                DataProviderContract.FAVORITE_PROVIDER_NAME
        };

        String selection = DataProviderContract.FAVORITE_PROVIDER_NAME + " = '" + stationName + "';";

        return new CursorLoader(getContext(), uriFavorite, projection, selection, null, null);
    }
    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {

        if(cursor.moveToLast()) {
            isFavorite = true;
            btnFavorite.setBackgroundResource(R.drawable.btn_favorite_selected);

        } else {
            isFavorite = false;
            btnFavorite.setBackgroundResource(R.drawable.btn_favorite);
        }

    }
    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {}

    // Favorite button click event handler for adding or removing the current station to or out of
    // Favorite and Geofence list.
    private void handleFavorite() {
        log.i("handleFavorite");
        if(TextUtils.isEmpty(etStnName.getText())) return;

        if(isFavorite) {
            geofenceHelper.removeFavoriteGeofence(stnName, stnId);
            btnFavorite.setBackgroundResource(R.drawable.btn_favorite);

        } else {
            geofenceHelper.setGeofenceParam(GasStation, stnId, location);
            geofenceHelper.addFavoriteGeofence(stnName, stnCode, stnAddrs);
            btnFavorite.setBackgroundResource(R.drawable.btn_favorite_selected);
        }

        isFavorite = !isFavorite;
    }

    // Method for inserting data to SQLite database
    public void saveData(){

        if(!doEmptyCheck()) return;

        ContentValues values = new ContentValues();
        long milliseconds = BaseActivity.parseDateTime(dateFormat, tvDateTime.getText().toString());
        int gas, wash, extra;

        try {
            values.put(DataProviderContract.TABLE_CODE, DataProviderContract.GAS_TABLE_CODE);
            values.put(DataProviderContract.DATE_TIME_COLUMN, milliseconds);
            values.put(DataProviderContract.MILEAGE_COLUMN, df.parse(tvOdometer.getText().toString()).intValue());

            values.put(DataProviderContract.GAS_STATION_COLUMN, etStnName.getText().toString());
            values.put(DataProviderContract.GAS_STATION_ADDRESS_COLUMN, stnAddrs);
            values.put(DataProviderContract.GAS_STATION_ID_COLUMN, stnId);
            values.put(DataProviderContract.GAS_PRICE_COLUMN, df.parse(etUnitPrice.getText().toString()).intValue());
            values.put(DataProviderContract.GAS_PAYMENT_COLUMN, gas = df.parse(tvGasPaid.getText().toString()).intValue());
            values.put(DataProviderContract.GAS_AMOUNT_COLUMN, df.parse(tvGasLoaded.getText().toString()).intValue());
            values.put(DataProviderContract.WASH_PAYMENT_COLUMN, wash = df.parse(tvCarwashPaid.getText().toString()).intValue());
            values.put(DataProviderContract.EXTRA_EXPENSE_COLUMN, etExtraExpense.getText().toString());
            values.put(DataProviderContract.EXTRA_PAYMENT_COLUMN, extra = df.parse(tvExtraPaid.getText().toString()).intValue());
            values.put(DataProviderContract.GAS_TOTAL_PAYMENT_COLUMN, gas + wash + extra);

            // Null check for the parent activity
            if(getActivity() == null) return;

            Uri mNewUri = getActivity().getContentResolver().insert(DataProviderContract.GAS_TABLE_URI, values);

            // Set the value of mileage in the SharedPreferences in order to sync it with ServiceManagerActivity
            mSettings.edit().putString(Constants.ODOMETER, tvOdometer.getText().toString()).apply();

            if(mNewUri != null) {
                Toast.makeText(getActivity(), getString(R.string.toast_save_success), Toast.LENGTH_SHORT).show();
                /*
                Intent intent = new Intent(this, GeofenceTransitionService.class);
                intent.putExtra("Geofence_Saved", true);
                startService(intent);
                */
            }

        } catch (NumberFormatException e) {
            //Log.d(LOG_TAG, "NumberFormatException: " + e.getMessage());
        } catch (SQLiteException e) {
            //Log.d(LOG_TAG, "SQLiteException: " + e.getMessage());
        } catch (ParseException e) {
            e.printStackTrace();
        }

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



    // Calculate what amount of gas is filled as putting amount of payment.
    // Whenever clicking each of the payment button, the gas amount is renewed.
    private void calculateGasAmount(int paid) throws ParseException {
        //Log.d(LOG_TAG, "price: " + etUnitPrice.getText().toString());
        //int price = Integer.parseInt(etUnitPrice.getText().toString().replaceAll("[^0-9]", ""));
        int price = df.parse(etUnitPrice.getText().toString()).intValue();
        String amount = String.valueOf(paid/price);
        //Log.d(LOG_TAG, "amount: " + (paid));
        tvGasLoaded.setText(amount);
    }


}
