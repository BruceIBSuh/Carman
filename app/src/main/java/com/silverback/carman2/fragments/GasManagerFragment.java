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
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

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
import com.silverback.carman2.models.OpinetViewModel;
import com.silverback.carman2.threads.LocationTask;
import com.silverback.carman2.threads.PriceFavoriteTask;
import com.silverback.carman2.utils.Constants;
import com.silverback.carman2.models.FragmentSharedModel;
import com.silverback.carman2.models.LocationViewModel;
import com.silverback.carman2.models.StationListViewModel;
import com.silverback.carman2.threads.StationInfoTask;
import com.silverback.carman2.threads.StationListTask;
import com.silverback.carman2.threads.ThreadManager;
import com.silverback.carman2.utils.FavoriteGeofenceHelper;
import com.silverback.carman2.utils.NumberTextWatcher;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * A simple {@link Fragment} subclass.
 */
public class GasManagerFragment extends Fragment implements View.OnClickListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(GasManagerFragment.class);

    // Objects
    private LocationTask locationTask;
    private CarmanDatabase mDB;
    private FirebaseFirestore firestore;
    private LocationViewModel locationModel;
    private StationListViewModel stnListModel;
    private FragmentSharedModel fragmentSharedModel;
    private OpinetViewModel opinetViewModel;

    private FavoriteGeofenceHelper geofenceHelper;
    private StationListTask stationListTask;
    private StationInfoTask stationInfoTask;
    private SharedPreferences mSettings;
    private DecimalFormat df;

    private Calendar calendar;
    private SimpleDateFormat sdf;
    private NumberPadFragment numPad;
    private Location location;

    // UIs
    private View localView;
    private ConstraintLayout constraintLayout;
    private TextView tvStnName, tvOdometer, tvDateTime, tvGasPaid, tvGasLoaded, tvCarwashPaid, tvExtraPaid;
    private EditText etUnitPrice, etExtraExpense, etGasComment;
    private ImageButton btnFavorite;
    private RatingBar ratingBar;
    private ProgressBar pbStation;
    private ImageButton imgRefresh;

    // Fields
    private String[] defaultParams;
    private TextView targetView; //reference to a clicked view which is used in ViewModel
    private String dateFormat;
    private String stnName, stnId, stnCode, stnAddrs;
    private String date;
    private String nickname;
    private String userId;
    private boolean isCommentUploaded;

    private boolean isGeofenceIntent, isFavoriteGas;


    // Constructor
    public GasManagerFragment() {
        // Required empty public constructor
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(getArguments() != null) {
            defaultParams = getArguments().getStringArray("defaultParams");
            userId = getArguments().getString("userId");
        }

        firestore = FirebaseFirestore.getInstance();

        // Instantiate the ViewModels
        // Refactor required using getViewLifecyclerOwnerLiveData()
        fragmentSharedModel = ((ExpenseActivity)getActivity()).getFragmentSharedModel();
        locationModel = ((ExpenseActivity) getActivity()).getLocationViewModel();
        stnListModel = ViewModelProviders.of(getActivity()).get(StationListViewModel.class);
        opinetViewModel = ViewModelProviders.of(getActivity()).get(OpinetViewModel.class);

        // Entity to retrieve list of favorite station to compare with a fetched current station
        // to tell whether it has registered with Favorite.
        mDB = CarmanDatabase.getDatabaseInstance(getActivity().getApplicationContext());
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

    @SuppressWarnings("ConstantConditions")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        // Inflate the layout for this fragment
        localView = inflater.inflate(R.layout.fragment_gas_manager, container, false);

        constraintLayout = localView.findViewById(R.id.constraint_gas);
        tvDateTime = localView.findViewById(R.id.tv_date_time);
        tvStnName = localView.findViewById(R.id.tv_station_name);
        pbStation = localView.findViewById(R.id.pb_search_station);
        imgRefresh = localView.findViewById(R.id.imgbtn_refresh);
        btnFavorite = localView.findViewById(R.id.imgbtn_favorite);
        etUnitPrice = localView.findViewById(R.id.et_unit_price);
        tvOdometer = localView.findViewById(R.id.tv_gas_mileage);
        tvGasPaid = localView.findViewById(R.id.tv_total_cost);
        tvGasLoaded = localView.findViewById(R.id.tv_amount);
        tvCarwashPaid = localView.findViewById(R.id.tv_carwash);
        tvExtraPaid = localView.findViewById(R.id.tv_extra);
        etExtraExpense = localView.findViewById(R.id.et_extra_expense);
        ratingBar = localView.findViewById(R.id.ratingBar);
        etGasComment = localView.findViewById(R.id.et_service_comment);
        Button btnResetRating = localView.findViewById(R.id.btn_reset_ratingbar);

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
        btnResetRating.setOnClickListener(view -> ratingBar.setRating(0f));
        btnFavorite.setOnClickListener(view -> addGasFavorite());


        // Manager the comment and the rating bar which should be allowed to make as far as the
        // nick name(vehicle name) has been created.
        nickname = mSettings.getString(Constants.USER_NAME, null);
        // In case of writing the ratingbar and comment, it is required to have a registered nickname.
        ratingBar.setOnRatingBarChangeListener((rb, rating, user) -> {
            if((nickname == null || nickname.isEmpty()) && rating > 0) {
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


        // FavoriteGeofenceHelper.OnGeofenceListener().
        // FavoriteGeofenceHelper class notifies whether a specific station is successfully added to
        // or removed out of the favorite list.
        geofenceHelper.setGeofenceListener(new FavoriteGeofenceHelper.OnGeofenceListener() {
            @Override
            public void notifyAddGeofenceCompleted() {
                Snackbar.make(constraintLayout, R.string.gas_snackbar_favorite_added, Snackbar.LENGTH_SHORT).show();
                isFavoriteGas = true;
            }
            @Override
            public void notifyRemoveGeofenceCompleted() {
                Snackbar.make(constraintLayout, R.string.gas_snackbar_favorite_removed, Snackbar.LENGTH_SHORT).show();
                isFavoriteGas = false;
            }
            @Override
            public void notifyAddGeofenceFailed() {
                log.i("Failed to add the gas station to Geofence");
            }
        });

        return localView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Attach an observer to fetch a current location from LocationTask, then initiate
        // StationListTask based on the value.
        locationModel.getLocation().observe(getViewLifecycleOwner(), location -> {
            log.i("Location fetched: %s", location);
            this.location = location;
            stationListTask = ThreadManager.startStationListTask(
                    getContext(), stnListModel, location, defaultParams);
        });

        // Check if a fetched current station has registered with Favorite right after StationListModel
        // is notified to retrieve a current station. Then, get StationInfoTask started to get
        // its address, completion of which is notified by the same ViewModel.
        stnListModel.getCurrentStationLiveData().observe(getViewLifecycleOwner(), curStn -> {
            if(curStn != null) {
                log.i("Current Station");
                stnName = curStn.getStnName();
                stnId = curStn.getStnId();
                tvStnName.setText(stnName);
                etUnitPrice.setText(String.valueOf(curStn.getStnPrice()));
                etUnitPrice.setCursorVisible(false);

                // Query Favorite with the fetched station name or station id to tell whether the station
                // has registered with Favorite.
                checkGasFavorite(stnName, stnId);
            }

            pbStation.setVisibility(View.GONE);
            imgRefresh.setVisibility(View.VISIBLE);
        });

        // Communicate w/ FavoriteListFragment to retrieve a favorite station picked out of the
        // dialogfragment.
        fragmentSharedModel.getFavoriteGasEntity().observe(getViewLifecycleOwner(), entity -> {
            tvStnName.setText(entity.providerName);
            btnFavorite.setBackgroundResource(R.drawable.btn_favorite_selected);
            stnId = entity.providerId;
            isFavoriteGas = true;

            PriceFavoriteTask mTask = ThreadManager.startFavoritePriceTask(
                    getActivity(), opinetViewModel, entity.providerId, false);
        });

        // Fetch the price info of a favorite gas station selected from FavoriteListFragment.
        opinetViewModel.getFavoritePriceData().observe(getViewLifecycleOwner(), data -> {
            log.i("Map data: %s", data.get(defaultParams[0]));
            etUnitPrice.setText(String.valueOf(data.get(defaultParams[0])));
            etUnitPrice.setCursorVisible(false);
        });

        // ViewModels to share data b/w fragments(GasManager, ServiceManager, and NumberPadFragment)
        // Return value is of SparseArray type the key of which is the view id of a clicked view.
        fragmentSharedModel.getSelectedValue().observe(getViewLifecycleOwner(), data -> {
            targetView = localView.findViewById(data.keyAt(0));
            if(targetView != null) {
                targetView.setText(df.format(data.valueAt(0)));
                calculateGasAmount();
            }
        });


    }

    @Override
    public void onResume() {
        super.onResume();
        // Must define FragmentSharedModel.setCurrentFragment() in onResume() , not onActivityCreated()
        // because the value of ragmentSharedModel.getCurrentFragment() is retrieved in onCreateView()
        // in ExpensePagerFragment. Otherwise, an error occurs due to asyncronous lifecycle.
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
            case R.id.tv_gas_mileage:
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
    private void checkGasFavorite(String name, String id) {
        mDB.favoriteModel().findFavoriteGasName(name, id, Constants.GAS).observe(this, stnName -> {
            if (TextUtils.isEmpty(stnName)) {
                isFavoriteGas = false;
                btnFavorite.setBackgroundResource(R.drawable.btn_favorite);
            } else {
                isFavoriteGas = true;
                btnFavorite.setBackgroundResource(R.drawable.btn_favorite_selected);
            }
        });
    }


    // Method to register with or unregister from the favorite list which is invoked by
    // the favorite button.
    @SuppressWarnings("ConstantConditions")
    private void addGasFavorite() {
        // In case the fragment is called up by GeofenceTransitionService, which should take a
        // separate process.
        if(isGeofenceIntent) return;

        // Pop up the dialog to select a favorite station out of the list with the empty TextView,
        // the result of which is handled in FragmentSharedModel.getFavoriteGasEntity(),
        // initiating the task to fetch the oil proce at the same time.
        if(TextUtils.isEmpty(tvStnName.getText())) {
            FavoriteListFragment favoriteFragment = FavoriteListFragment.newInstance(getString(R.string.exp_title_gas), Constants.GAS);
            favoriteFragment.show(getFragmentManager(), null);

        // Remove the station both from the local db and Firestore, the result of which is notified
        // by FavoriteGeofenceHelper.OnGeofenceListener, changing the boolean value of isFavoriteGas;
        } else if(isFavoriteGas) {
            //Snackbar.make(constraintLayout, getString(R.string.gas_snackbar_alert_remove_favorite), Snackbar.LENGTH_LONG).show();
            geofenceHelper.removeFavoriteGeofence(userId, stnName, stnId, Constants.GAS);
            btnFavorite.setBackgroundResource(R.drawable.btn_favorite);

            //firestore.collection("gas_eval").document(stnId).update("favorite_num", FieldValue.increment(-1));

        } else {
            // First, check if the favorite is up to the limit.
            final int placeholder = mDB.favoriteModel().countFavoriteNumber(Constants.GAS);
            if(placeholder == Constants.MAX_FAVORITE) {
                Snackbar.make(constraintLayout, R.string.exp_snackbar_favorite_limit, Snackbar.LENGTH_SHORT).show();

            } else {
                // Then, retrieve the station data which pass to FavoriteGeofenceHelper to register with
                // Geofence.
                firestore.collection("gas_station").document(stnId).get().addOnCompleteListener(task -> {
                    if(task.isSuccessful()) {
                        DocumentSnapshot snapshot = task.getResult();
                        if(snapshot != null && snapshot.exists()) {
                            btnFavorite.setBackgroundResource(R.drawable.btn_favorite_selected);
                            geofenceHelper.addFavoriteGeofence(userId, snapshot, placeholder, Constants.GAS);

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

        //ContentValues values = new ContentValues();
        long milliseconds = BaseActivity.parseDateTime(dateFormat, tvDateTime.getText().toString());

        // Create Entity instances both of which are correlated by Foreinkey
        ExpenseBaseEntity basicEntity = new ExpenseBaseEntity();
        GasManagerEntity gasEntity = new GasManagerEntity();

        basicEntity.dateTime = milliseconds;
        basicEntity.category = 1;
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

                firestore.collection("gas_eval").document(stnId).collection("comments")
                        .document(userId)
                        .set(commentData)
                        .addOnCompleteListener(task -> {
                            if(task.isSuccessful()) {
                                log.e("Commments successfully uploaded");
                                isCommentUploaded = true;
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
            Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
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
