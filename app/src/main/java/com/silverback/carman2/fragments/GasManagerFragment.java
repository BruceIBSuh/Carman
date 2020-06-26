package com.silverback.carman2.fragments;


import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.silverback.carman2.BaseActivity;
import com.silverback.carman2.ExpenseActivity;
import com.silverback.carman2.R;
import com.silverback.carman2.database.CarmanDatabase;
import com.silverback.carman2.database.ExpenseBaseEntity;
import com.silverback.carman2.database.GasManagerEntity;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.viewmodels.FragmentSharedModel;
import com.silverback.carman2.viewmodels.LocationViewModel;
import com.silverback.carman2.viewmodels.OpinetViewModel;
import com.silverback.carman2.viewmodels.StationListViewModel;
import com.silverback.carman2.threads.FavoritePriceTask;
import com.silverback.carman2.threads.LocationTask;
import com.silverback.carman2.threads.StationListTask;
import com.silverback.carman2.threads.ThreadManager;
import com.silverback.carman2.utils.Constants;
import com.silverback.carman2.utils.FavoriteGeofenceHelper;
import com.silverback.carman2.utils.NumberTextWatcher;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

/**
 * This fragment provides the form to fill in the gas expense.
 */
public class GasManagerFragment extends Fragment implements View.OnClickListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(GasManagerFragment.class);

    // Objects
    private CarmanDatabase mDB;
    private FirebaseFirestore firestore;
    private LocationViewModel locationModel;
    private StationListViewModel stnListModel;
    private FragmentSharedModel sharedModel;
    private OpinetViewModel opinetViewModel;

    private FavoriteGeofenceHelper geofenceHelper;
    private LocationTask locationTask;
    private StationListTask stnListTask;
    private FavoritePriceTask favPriceTask;
    private SharedPreferences mSettings;
    private DecimalFormat df;
    private NumberPadFragment numPad;
    private Location mPrevLocation;

    // UIs
    private View localView;
    private TextView tvStnName, tvOdometer, tvDateTime, tvGasPaid, tvGasLoaded, tvCarwashPaid, tvExtraPaid;
    private EditText etUnitPrice, etExtraExpense, etGasComment;
    private RatingBar ratingBar;
    private ProgressBar stnProgbar;
    private ImageButton imgRefresh;
    private ImageButton btnStnFavorite;

    // Fields
    private String[] defaultParams;
    private TextView targetView; //reference to a clicked view which is used in ViewModel
    private String dateFormat;
    private String stnName, stnId, stnCode, stnAddrs;
    private String date;
    private String nickname;
    private String userId;
    private String geoStnName, geoStnId;
    private long geoTime;


    private boolean isGeofenceIntent, isFavoriteGas;
    private int category;


    // Constructor
    public GasManagerFragment() {
        // Required empty public constructor
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Argument(s) from the parent activity which ExpenseTabPagerTask has set to notify the defaults.
        if(getArguments() != null) {
            defaultParams = getArguments().getStringArray("defaultParams");
            userId = getArguments().getString("userId");
        }

        // The parent activity gets started by tabbing the Geofence notification w/ a PendingIntent
        // that contains the station id, name, geofencing time, and category. The data fill out
        // the gas or service form acoording to the transferred category.
        String action = getActivity().getIntent().getAction();
        if(action != null && action.equals(Constants.NOTI_GEOFENCE)) {
            isGeofenceIntent = true;
            geoStnName = getActivity().getIntent().getStringExtra(Constants.GEO_NAME);
            geoStnId = getActivity().getIntent().getStringExtra(Constants.GEO_ID);
            geoTime = getActivity().getIntent().getLongExtra(Constants.GEO_TIME, -1);
            category = getActivity().getIntent().getIntExtra(Constants.GEO_CATEGORY, -1);
        }


        // Instantiate the objects
        firestore = FirebaseFirestore.getInstance();
        mDB = CarmanDatabase.getDatabaseInstance(getContext());
        mSettings = ((ExpenseActivity)getActivity()).getSettings();

        // ViewModels: reconsider why the models references the ones defined in the parent activity;
        // it would rather  be better to redefine them here.
        sharedModel = new ViewModelProvider(getActivity()).get(FragmentSharedModel.class);
        locationModel = ((ExpenseActivity)getActivity()).getLocationViewModel();
        stnListModel = new ViewModelProvider(getActivity()).get(StationListViewModel.class);
        opinetViewModel = new ViewModelProvider(getActivity()).get(OpinetViewModel.class);

        // Create FavoriteGeofenceHelper instance to add or remove a station to Favorte and
        // Geofence list when the favorite button clicks.
        geofenceHelper = new FavoriteGeofenceHelper(getContext());

        // Instantiate other miscellanies
        df = BaseActivity.getDecimalFormatInstance();
        numPad = new NumberPadFragment();

        // Get the time that you have visited to the station. In case the parent activity gets started
        // by the stack builder, which means isGeofence is true, the PendingIntent contains the time.
        dateFormat = getString(R.string.date_format_1);
        //Calendar calendar = Calendar.getInstance(Locale.getDefault());
        //SimpleDateFormat sdf = new SimpleDateFormat(dateFormat, Locale.getDefault());
        long visitTime = (isGeofenceIntent)? geoTime : System.currentTimeMillis();
        date = BaseActivity.formatMilliseconds(dateFormat, visitTime);
        /*
         * Implements the interface of FavoriteGeofenceHelper.SetGefenceListener to notify the
         * favorite button of having the favroite station added or removed, re-setting the flag
         * and pop up the snackbar.
         *
         * At the same time, handle some exceptional conditions; the current station becomes the
         * first priority favorite it is added first time, or a removed station  is the first priority
         * one such that the second one becomes the first priority.
         */
        geofenceHelper.setGeofenceListener(new FavoriteGeofenceHelper.OnGeofenceListener() {
            // Count the number of the favorite provider to handle the number becomes one or zero.
            @Override
            public void notifyAddGeofenceCompleted(int placeholder) {
                log.i("Geofence added");
                Snackbar.make(getView(), R.string.gas_snackbar_favorite_added, Snackbar.LENGTH_SHORT).show();
                isFavoriteGas = true;
            }

            @Override
            public void notifyRemoveGeofenceCompleted(int placeholder) {
                /*
                if(placeholder == 0) {
                    log.i("First placeholder is removed");
                    mDB.favoriteModel().queryFavoriteProviders(Constants.GAS).observe(getViewLifecycleOwner(), favList -> {
                        int position = 0;
                        for(FavoriteProviderEntity entity : favList) {
                            log.i("Favorite placeholder: %s, %s", entity.providerName, entity.placeHolder);
                            entity.placeHolder = position;
                            position++;
                        }
                    });

                    //mDB.favoriteModel().updatePlaceHolder(favList);
                }
                */

                Snackbar.make(localView, R.string.gas_snackbar_favorite_removed, Snackbar.LENGTH_SHORT).show();
                isFavoriteGas = false;

            }

            @Override
            public void notifyAddGeofenceFailed() {
                log.i("Failed to add the gas station to Geofence");
            }
        });
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Check if it's possible to change the soft input mode on the fragment basis. Seems not work.
        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        // Inflate the layout for this fragment
        localView = inflater.inflate(R.layout.fragment_gas_manager, container, false);
        tvDateTime = localView.findViewById(R.id.tv_date_time);
        tvStnName = localView.findViewById(R.id.tv_station_name);
        stnProgbar = localView.findViewById(R.id.pb_search_station);
        imgRefresh = localView.findViewById(R.id.imgbtn_refresh);
        btnStnFavorite = localView.findViewById(R.id.btn_gas_favorite);
        etUnitPrice = localView.findViewById(R.id.et_unit_price);
        tvOdometer = localView.findViewById(R.id.tv_mileage);
        tvGasPaid = localView.findViewById(R.id.tv_gas_payment);
        tvGasLoaded = localView.findViewById(R.id.tv_gas_amount);
        tvCarwashPaid = localView.findViewById(R.id.tv_carwash_payment);
        tvExtraPaid = localView.findViewById(R.id.tv_extra_payment);
        etExtraExpense = localView.findViewById(R.id.et_extra_expense);
        ratingBar = localView.findViewById(R.id.ratingBar);
        etGasComment = localView.findViewById(R.id.et_service_comment);
        Button btnResetRating = localView.findViewById(R.id.btn_reset_ratingbar);

        tvDateTime.setText(date);
        tvOdometer.setText(mSettings.getString(Constants.ODOMETER, "0"));
        //tvGasPaid.setText(mSettings.getString(Constants.PAYMENT, "0"));
        stnProgbar.setVisibility(View.VISIBLE);

        // Attach the event listeners
        etUnitPrice.addTextChangedListener(new NumberTextWatcher(etUnitPrice));
        tvOdometer.setOnClickListener(this);
        tvGasPaid.setOnClickListener(this);
        tvCarwashPaid.setOnClickListener(this);
        tvExtraPaid.setOnClickListener(this);
        btnResetRating.setOnClickListener(view -> ratingBar.setRating(0f));
        btnStnFavorite.setOnClickListener(view -> addGasFavorite());
        imgRefresh.setOnClickListener(view -> {
            locationTask = ThreadManager.fetchLocationTask(getContext(), locationModel);
            stnProgbar.setVisibility(View.VISIBLE);
            imgRefresh.setVisibility(View.GONE);
        });


        // Manager the comment and the rating bar which should be allowed to make as far as the
        // nick name(vehicle name) has been created.
        nickname = mSettings.getString(Constants.USER_NAME, null);
        // In case of writing the ratingbar and comment, it is required to have a registered nickname.
        ratingBar.setOnRatingBarChangeListener((rb, rating, user) -> {
            if(TextUtils.isEmpty(nickname) && rating > 0) {
                ratingBar.setRating(0f);
                Snackbar.make(localView, "Nickname required", Snackbar.LENGTH_SHORT).show();
            }
        });

        etGasComment.setOnFocusChangeListener((view, b) -> {
            if(b && TextUtils.isEmpty(nickname)) {
                Snackbar.make(localView, "Nickname required", Snackbar.LENGTH_SHORT).show();
                view.clearFocus();
            }
        });

        // In case the activity and this fragment get started by tabbing the geofence notification,
        // the pendingintent passes the name, id, time.
        if(isGeofenceIntent) {
            if(category == Constants.GAS) {
                tvStnName.setText(geoStnName);
                stnId = geoStnId;
                isFavoriteGas = true;

                // Hanldinthe UIs
                btnStnFavorite.setBackgroundResource(R.drawable.btn_favorite_selected);
                stnProgbar.setVisibility(View.GONE);
                //btnChangeDate.setVisibility(View.GONE);

                // Task to fetch the gas price of a station with the station ID.
                favPriceTask = ThreadManager.startFavoritePriceTask(getActivity(), opinetViewModel, stnId, false);

            } else if(category == Constants.SVC) {
                stnProgbar.setVisibility(View.GONE);
                imgRefresh.setVisibility(View.VISIBLE);
            }
        }

        return localView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Share a value input in NumberPadFragment as the view id passes to NumberPadFragment when
        // clicking, then returns an input value as SparseArray which contains the view id, which
        // may identify the view invoking NumberPadFragment and fill in the value into the view.
        sharedModel.getSelectedValue().observe(getViewLifecycleOwner(), data -> {
            log.i("View ID: %s", data.keyAt(0));
            //targetView = localView.findViewById(data.keyAt(0));
            if(targetView != null) {
                targetView.setText(df.format(data.valueAt(0)));
                calculateGasAmount();
            }
        });

        // Attach an observer to fetch a current location from LocationTask, then initiate
        // StationListTask based on the value unless the parent activity gets started by the geo noti.
        // BTW, the location task is initiated in the parent activity.
        locationModel.getLocation().observe(getViewLifecycleOwner(), location -> {
            // Exclude the case that the fragment gets started by GeofenceIntent
            if(isGeofenceIntent) return;

            // Fetch the current station only if the current location is out of the update distance.
            if(mPrevLocation == null || location.distanceTo(mPrevLocation) > Constants.UPDATE_DISTANCE) {
                stnListTask = ThreadManager.startStationListTask(stnListModel, location, defaultParams);
                mPrevLocation = location;
            } else {
                stnProgbar.setVisibility(View.GONE);
                imgRefresh.setVisibility(View.VISIBLE);
            }
        });

        // Check if a fetched current station has registered with Favorite right after a current station
        // is retrieved by StationListViewModel.
        stnListModel.getCurrentStation().observe(getViewLifecycleOwner(), curStn -> {
            if(curStn != null) {
                stnName = curStn.getStnName();
                stnId = curStn.getStnId();
                tvStnName.setText(stnName);
                etUnitPrice.setText(String.valueOf(curStn.getStnPrice()));
                etUnitPrice.setCursorVisible(false);

                // Query Favorite with the fetched station name or station id to tell whether the station
                // has registered with Favorite.
                checkGasFavorite(stnName, stnId);
            }

            stnProgbar.setVisibility(View.GONE);
            imgRefresh.setVisibility(View.VISIBLE);
        });

        // Under the condition that no currnet station is fetched because of not within GEOFENCE_RADIUS,
        // the user can have the favorite station list when clicking the favorite button.
        // In doing so, this fragment communicates w/ FavoriteListFragment to retrieve a favorite
        // station picked out of FavoriteListFragment using FragmentSharedModel. With the station id,
        // FavoritePriceTask gets started to have the gas price.
        sharedModel.getFavoriteGasEntity().observe(getViewLifecycleOwner(), entity -> {
            tvStnName.setText(entity.providerName);
            btnStnFavorite.setBackgroundResource(R.drawable.btn_favorite_selected);
            stnId = entity.providerId;
            isFavoriteGas = true;

            favPriceTask = ThreadManager.startFavoritePriceTask(
                    getActivity(), opinetViewModel, entity.providerId, false);
        });

        // Fetch the price info of a favorite gas station selected from FavoriteListFragment.
        opinetViewModel.getFavoritePriceData().observe(getViewLifecycleOwner(), data -> {
            log.i("Favorite price data: %s", data.get(defaultParams[0]));
            etUnitPrice.setText(String.valueOf(data.get(defaultParams[0])));
            etUnitPrice.setCursorVisible(false);
        });


    }

    @Override
    public void onResume() {
        super.onResume();
        // Must define FragmentSharedModel.setCurrentFragment() in onResume(), not onActivityCreated()
        // because the value of FragmentSharedModel.getCurrentFragment() is retrieved in onCreateView()
        // of ExpensePagerFragment. Otherwise, an error occurs due to asyncronous lifecycle.
        sharedModel.setCurrentFragment(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        if(locationTask != null) locationTask = null;
        if(favPriceTask != null) favPriceTask = null;
        if(stnListTask != null) stnListTask = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onClick(final View v) {
        Bundle args = new Bundle();
        String initValue = null;
        targetView = (TextView)v;

        // Pass the current saved value to NumberPadFragment
        switch(v.getId()) {
            case R.id.tv_mileage:
                initValue = tvOdometer.getText().toString();
                break;

            case R.id.tv_gas_payment:
                initValue = tvGasPaid.getHint().toString();
                break;

            case R.id.tv_carwash_payment:
                initValue = tvCarwashPaid.getHint().toString();
                break;

            case R.id.tv_extra_payment:
                initValue = tvExtraPaid.getHint().toString();
                break;
        }

        // Pass the id of TextView to NumberPadFragment for which TextView is being focused to wait
        // for a new value.
        //NumberPadFragment.newInstance(null, initValue, v.getId()).show(getFragmentManager(), "numPad");
        //args.putString("title", itemTitle);
        args.putInt("viewId", v.getId());
        args.putString("initValue", initValue);
        numPad.setArguments(args);

        if(getActivity() != null)
            numPad.show(getActivity().getSupportFragmentManager(), "numberPad");

    }

    // Query FavoriteProviderEntity with the fetched station name or station id to tell whether the
    // fetched current station has registered with the entity.
    private void checkGasFavorite(String name, String id) {
        mDB.favoriteModel().findFavoriteGasName(name, id, Constants.GAS)
                .observe(getViewLifecycleOwner(), stnName -> {

                    if (TextUtils.isEmpty(stnName)) {
                    isFavoriteGas = false;
                    btnStnFavorite.setBackgroundResource(R.drawable.btn_favorite);
                    } else {
                        isFavoriteGas = true;
                    btnStnFavorite.setBackgroundResource(R.drawable.btn_favorite_selected);
                    }
                });
    }


    @SuppressWarnings("ConstantConditions")
    private void addGasFavorite() {
        // Disable the button when the parent activity gets started by the geofence notification.
        //if(isGeofenceIntent) return;

        // Pop up FavoriteListFragment when clicking the favorite button.
        if(TextUtils.isEmpty(tvStnName.getText())) {
            FavoriteListFragment.newInstance(getString(R.string.exp_title_gas), Constants.GAS)
                    .show(getActivity().getSupportFragmentManager(), null);
        // Remove the station both from the local db and Firestore, the result of which is notified
        // to FavoriteGeofenceHelper.OnGeofenceListener which implements the boolean value set to false.
        } else if(isFavoriteGas) {
            log.i("favorite button clicked to remove");
            Snackbar snackbar = Snackbar.make(
                    localView, getString(R.string.gas_snackbar_alert_remove_favorite), Snackbar.LENGTH_SHORT);
            snackbar.setAction(R.string.popup_msg_confirm, view -> {
                geofenceHelper.removeFavoriteGeofence(stnName, stnId, Constants.GAS);
                btnStnFavorite.setBackgroundResource(R.drawable.btn_favorite);
            }).show();

        } else {
            // Add a station both to the local db and Firestore as far as the number of favorite stations
            // is less than Constants.MAX_FAVORITE, currently set to 10.
            final int placeholder = mDB.favoriteModel().countFavoriteNumber(Constants.GAS);
            if(placeholder == Constants.MAX_FAVORITE) {
                Snackbar.make(localView, R.string.exp_snackbar_favorite_limit, Snackbar.LENGTH_SHORT).show();

            } else {
                // Then, retrieve the station data which pass to FavoriteGeofenceHelper to register with
                // Geofence.
                firestore.collection("gas_station").document(stnId).get().addOnCompleteListener(task -> {
                    if(task.isSuccessful()) {
                        DocumentSnapshot snapshot = task.getResult();
                        if(snapshot != null && snapshot.exists()) {
                            btnStnFavorite.setBackgroundResource(R.drawable.btn_favorite_selected);
                            geofenceHelper.addFavoriteGeofence(snapshot, placeholder, Constants.GAS);
                        }
                    }
                });

            }
        }

    }

    // Method for inserting data to SQLite database
    public boolean saveGasData(){
        // Null check for the parent activity
        if(!doEmptyCheck()) return false;

        sharedModel.setCurrentFragment(this);
        long milliseconds = BaseActivity.parseDateTime(dateFormat, tvDateTime.getText().toString());

        // Create Entity instances both of which are correlated by Foreinkey
        ExpenseBaseEntity basicEntity = new ExpenseBaseEntity();
        GasManagerEntity gasEntity = new GasManagerEntity();

        basicEntity.dateTime = milliseconds;
        basicEntity.category = Constants.GAS;
        gasEntity.stnName = tvStnName.getText().toString();
        gasEntity.stnAddrs = stnAddrs;
        gasEntity.stnId = stnId;
        gasEntity.extraExpense = etExtraExpense.getText().toString();

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

        // Insert the data to the local db.
        int rowId = mDB.gasManagerModel().insertBoth(basicEntity, gasEntity);
        if(rowId > 0) {
            mSettings.edit().putString(Constants.ODOMETER, tvOdometer.getText().toString()).apply();
            Toast.makeText(getActivity(), getString(R.string.toast_save_success), Toast.LENGTH_SHORT).show();

            // FireStore Process to upload the rating and comments with Station ID.
            if(ratingBar.getRating() > 0) {
                log.i("RatingBar: %s", ratingBar.getRating());
                Map<String, Object> ratingData = new HashMap<>();
                ratingData.put("eval_num", FieldValue.increment(1));
                ratingData.put("eval_sum", FieldValue.increment(ratingBar.getRating()));

                DocumentReference docRef = firestore.collection("gas_eval").document(stnId);
                docRef.get().addOnSuccessListener(snapshot -> {
                    if(snapshot.exists() && snapshot.get("eval_num") != null) {
                        log.i("update rating");
                        docRef.update(ratingData);

                    } else {
                        // In case of the favorite_num field existing, must set the option of
                        // SetOPtions.merge(). Otherwise, it may remove the existing field.
                        docRef.set(ratingData, SetOptions.merge());
                    }

                });
            }

            // Add ranting or comments to the Firestore, if any.
            if(!TextUtils.isEmpty(etGasComment.getText())) {

                Map<String, Object> commentData = new HashMap<>();
                commentData.put("timestamp", FieldValue.serverTimestamp());
                commentData.put("name", nickname);
                commentData.put("comments", etGasComment.getText().toString());
                commentData.put("rating", ratingBar.getRating());

                firestore.collection("gas_eval").document(stnId).collection("comments").document(userId)
                        .set(commentData).addOnCompleteListener(task -> {
                            if(task.isSuccessful()) {
                                log.e("Commments successfully uploaded");
                            } else {
                                log.e("Comments upload failed: %s", task.getException());
                            }
                        });
            }

            return true;

        } else return false;

    }

    // Method to make an empty check. When successfully fetching the gas station and the price,
    // those 2 values automatically fill the views. In this case, only the payment value will be
    // checked.
    private boolean doEmptyCheck() {
        // Check if the gas station name is empty
        if(TextUtils.isEmpty(tvStnName.getText())) {
            String msg = getResources().getString(R.string.toast_station_name);
            //Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
            Snackbar.make(localView, msg, Snackbar.LENGTH_SHORT).show();
            return false;
        }

        // Check if the gas unit price is empty
        if(TextUtils.isEmpty(etUnitPrice.getText())) {
            String msg = getResources().getString(R.string.toast_unit_price);
            //Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
            Snackbar.make(localView, msg, Snackbar.LENGTH_SHORT).show();
            etUnitPrice.requestFocus();
            return false;
        }

        // Check if the payment is empty.
        if(tvGasPaid.getText().toString().matches("0")) {
            String msg = getResources().getString(R.string.toast_payment);
            //Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
            Snackbar.make(localView, msg, Snackbar.LENGTH_SHORT).show();
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
