package com.silverback.carman.fragments;


import static com.silverback.carman.SettingActivity.PREF_ODOMETER;

import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.silverback.carman.BaseActivity;
import com.silverback.carman.R;
import com.silverback.carman.database.CarmanDatabase;
import com.silverback.carman.database.ExpenseBaseEntity;
import com.silverback.carman.database.GasManagerEntity;
import com.silverback.carman.databinding.FragmentGasManagerBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.threads.FavStationTaskk;
import com.silverback.carman.threads.LocationTask;
import com.silverback.carman.threads.StationGasTask;
import com.silverback.carman.threads.ThreadManager2;
import com.silverback.carman.utils.Constants;
import com.silverback.carman.utils.FavoriteGeofenceHelper;
import com.silverback.carman.utils.NumberTextWatcher;
import com.silverback.carman.viewmodels.FragmentSharedModel;
import com.silverback.carman.viewmodels.LocationViewModel;
import com.silverback.carman.viewmodels.OpinetViewModel;
import com.silverback.carman.viewmodels.StationListViewModel;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * This fragment provides the form to fill in the gas expense.
 */
public class ExpenseGasFragment extends Fragment {//implements View.OnClickListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(ExpenseGasFragment.class);

    private static final int REQUEST_PERM_BACKGROUND_LOCATION = 1000;
    private FragmentGasManagerBinding binding;
    private CarmanDatabase mDB;
    private FirebaseFirestore firestore;

    private StationListViewModel stnListModel;
    private LocationViewModel locationModel;
    private FragmentSharedModel fragmentModel;
    private OpinetViewModel opinetViewModel;

    private FavoriteGeofenceHelper geofenceHelper;
    private LocationTask locationTask;
    private StationGasTask stnListTask;
    private FavStationTaskk favPriceTask;
    private SharedPreferences mSettings;
    private SimpleDateFormat sdf;
    private DecimalFormat df;
    private Calendar calendar;

    // Fields
    private Location mPrevLocation;
    private String[] defaultParams;
    private TextView targetView; //reference to a clicked view which is used in ViewModel
    private String dateFormat;
    private String stnName, stnId;// stnCode, stnAddrs;
    private String date;
    private String userName, userId;
    private String geoStnName, geoStnId;
    private long geoTime;
    private boolean isGeofenceIntent, isFavoriteGas;
    private int category;

    // Constructor
    public ExpenseGasFragment() {
        // Required empty public constructor
    }

    //@SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Argument(s) from the parent activity which ExpenseTabPagerTask has set to notify the defaults.
        if(getArguments() != null) {
            //defaultParams = getArguments().getStringArray("defaultParams");
            userId = getArguments().getString("userId");
        }

        // The parent activity gets started by tabbing the Geofence notification w/ the pendingintent
        // that contains the station id, name, geofencing time, and category. The data will fill out
        // the gas or service form acoording to the transferred category.
        String action = requireActivity().getIntent().getAction();
        if(action != null && action.equals(Constants.NOTI_GEOFENCE)) {
            isGeofenceIntent = true;
            geoStnName = requireActivity().getIntent().getStringExtra(Constants.GEO_NAME);
            geoStnId = requireActivity().getIntent().getStringExtra(Constants.GEO_ID);
            geoTime = requireActivity().getIntent().getLongExtra(Constants.GEO_TIME, -1);
            category = requireActivity().getIntent().getIntExtra(Constants.GEO_CATEGORY, -1);
        }

        // Instantiate objects
        firestore = FirebaseFirestore.getInstance();
        mDB = CarmanDatabase.getDatabaseInstance(requireContext());
        mSettings = PreferenceManager.getDefaultSharedPreferences(requireContext());
        geofenceHelper = new FavoriteGeofenceHelper(requireContext());
        calendar = Calendar.getInstance(Locale.getDefault());
        df = ((BaseActivity)requireActivity()).getDecimalFormat();
        sdf = new SimpleDateFormat(getString(R.string.date_format_1), Locale.getDefault());

        // ViewModels: reconsider why  models shouldn't reference the ones defined in the parent
        // activity. it would rather  be better to redefine.
        fragmentModel = new ViewModelProvider(requireActivity()).get(FragmentSharedModel.class);
        stnListModel = new ViewModelProvider(requireActivity()).get(StationListViewModel.class);
        opinetViewModel = new ViewModelProvider(requireActivity()).get(OpinetViewModel.class);
        locationModel = new ViewModelProvider(requireActivity()).get(LocationViewModel.class);

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
                Snackbar.make(binding.getRoot(),
                        R.string.gas_snackbar_favorite_added, Snackbar.LENGTH_SHORT).show();
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
                Snackbar.make(binding.getRoot(),
                        R.string.gas_snackbar_favorite_removed, Snackbar.LENGTH_SHORT).show();
                isFavoriteGas = false;
            }

            @Override
            public void notifyAddGeofenceFailed() {
                log.e("Failed to add the gas station to Geofence:");
            }
        });

        // Initialize fields
        userName = mSettings.getString(Constants.USER_NAME, null);
    }

    //@SuppressWarnings("ConstantConditions")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentGasManagerBinding.inflate(inflater, container, false);
        // Check if it's possible to change the soft input mode on the fragment basis. Seems not work.
        Objects.requireNonNull(requireActivity()).getWindow()
                .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        long visitTime = (isGeofenceIntent)? geoTime : System.currentTimeMillis();
        binding.tvGasDatetime.setText(sdf.format(visitTime));
        binding.tvGasMileage.setText(mSettings.getString(PREF_ODOMETER, "0"));
        binding.tvGasPayment.setText(mSettings.getString(Constants.PAYMENT, "0"));

        // Attach the event listeners
        binding.etGasUnitPrice.addTextChangedListener(new NumberTextWatcher(binding.etGasUnitPrice));
        binding.btnResetRatingbar.setOnClickListener(view -> binding.rbGasStation.setRating(0f));
        binding.imgbtnGasSearch.setOnClickListener(view -> {
            locationTask = ThreadManager2.fetchLocationTask(requireActivity(), locationModel);
            binding.pbSearchStation.setVisibility(View.VISIBLE);
            binding.imgbtnGasSearch.setVisibility(View.GONE);
        });
        // Manager the comment and the rating bar which should be allowed to make as far as the
        // nick name(vehicle name) has been created.
        binding.rbGasStation.setOnRatingBarChangeListener((rb, rating, user) -> {
            if(TextUtils.isEmpty(userName) && rating > 0) {
                binding.rbGasStation.setRating(0f);
                Snackbar.make(binding.getRoot(), "Nickname required", Snackbar.LENGTH_SHORT).show();
            }
        });
        binding.etGasComment.setOnFocusChangeListener((view, b) -> {
            if(b && TextUtils.isEmpty(userName)) {
                Snackbar.make(binding.getRoot(), "Nickname required", Snackbar.LENGTH_SHORT).show();
                view.clearFocus();
            }
        });
        // In case the activity and this fragment get started by tabbing the geofence notification,
        // the pendingintent passes the name, id, time.
        if(isGeofenceIntent) getGeofenceIntentAction();

        return binding.getRoot();
    }

    @Override
    //public void onActivityCreated(Bundle savedInstanceState) {
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setCurrentStation();

        locationModel.getLocation().observe(getViewLifecycleOwner(), location -> {
            if(isGeofenceIntent) return;
            if(mPrevLocation == null || location.distanceTo(mPrevLocation) > Constants.UPDATE_DISTANCE) {
                String[] defaultParams = ((BaseActivity)requireActivity()).getNearStationParams();
                defaultParams[1] = Constants.MIN_RADIUS;
                stnListTask = ThreadManager2.startGasStationListTask(stnListModel, location, defaultParams);
                mPrevLocation = location;
            } else {
                binding.pbSearchStation.setVisibility(View.GONE);
                binding.imgbtnGasSearch.setVisibility(View.VISIBLE);
            }
        });

        fragmentModel.getNumpadValue().observe(getViewLifecycleOwner(), data -> {
            targetView = binding.getRoot().findViewById(data.keyAt(0));
            if(targetView != null) {
                targetView.setText(df.format(data.valueAt(0)));
                calculateGasAmount();
            }
        });

        fragmentModel.getCustomDateAndTime().observe(getViewLifecycleOwner(), calendar -> {
            this.calendar = calendar;
            //long customTime = calendar.getTimeInMillis();
            binding.tvGasDatetime.setText(sdf.format(calendar.getTimeInMillis()));
        });


        // Under the condition that no currnet station is fetched because of not within GEOFENCE_RADIUS,
        // the user can have the favorite station list when clicking the favorite button.
        // In doing so, this fragment communicates w/ FavoriteListFragment to retrieve a favorite
        // station picked out of FavoriteListFragment using FragmentSharedModel. With the station id,
        // FavStationTaskk gets started to have the gas price.
        fragmentModel.getFavoriteGasEntity().observe(getViewLifecycleOwner(), data -> {
            binding.tvStationName.setText(data.providerName);
            binding.btnGasFavorite.setBackgroundResource(R.drawable.btn_favorite_selected);
            stnId = data.providerId;
            isFavoriteGas = true;

            favPriceTask = ThreadManager2.startFavoriteStationTask(
                    requireActivity(), opinetViewModel, data.providerId, false);

        });

        // Fetch the price info of a favorite gas station selected from FavoriteListFragment.
        opinetViewModel.getFavoritePriceData().observe(getViewLifecycleOwner(), data -> {
            String fuelCode = mSettings.getString(Constants.FUEL, "B027");
            binding.etGasUnitPrice.setText(String.valueOf(data.get(fuelCode)));
            binding.etGasUnitPrice.setCursorVisible(false);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // To sync the fragment w/ the viewpager in the top frame.
        fragmentModel.getCurrentFragment().setValue(Constants.GAS);
        binding.tvGasDatetime.setText(sdf.format(System.currentTimeMillis()));
    }

    @Override
    public void onPause() {
        super.onPause();
        log.i("ExpesneGasFragment in Pause state");
        // When clicking the save button, prevent the observer from invoking the method, which
        // occasionally
        fragmentModel.getCurrentFragment().removeObservers(this);

        //if(locationTask != null) locationTask = null;
        if(favPriceTask != null) favPriceTask = null;
        if(stnListTask != null) stnListTask = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void setCurrentStation() {
        if(isGeofenceIntent) return;
        stnListModel.getCurrentStation().observe(getViewLifecycleOwner(), curStn -> {
            log.i("current station");
            if(curStn != null) {
                stnName = curStn.getStnName();
                stnId = curStn.getStnId();
                binding.tvStationName.setText(stnName);
                binding.etGasUnitPrice.setText(String.valueOf(curStn.getGasPrice()));
                binding.etGasUnitPrice.setCursorVisible(false);

                // Query Favorite with the fetched station name or station id to tell whether the station
                // has registered with Favorite.
                checkGasFavorite(stnName, stnId);
            }
            binding.pbSearchStation.setVisibility(View.GONE);
            binding.imgbtnGasSearch.setVisibility(View.VISIBLE);
        });

    }

    // Query FavoriteProviderEntity with the fetched station name or station id to tell whether the
    // fetched current station has registered with the entity.
    private void checkGasFavorite(String name, String id) {
        mDB.favoriteModel().findFavoriteGasName(name, id, Constants.GAS).observe(getViewLifecycleOwner(), stnName -> {
            if (TextUtils.isEmpty(stnName)) {
                isFavoriteGas = false;
                binding.btnGasFavorite.setBackgroundResource(R.drawable.btn_favorite);
            } else {
                isFavoriteGas = true;
                binding.btnGasFavorite.setBackgroundResource(R.drawable.btn_favorite_selected);
            }
        });
    }
    
    public void addGasFavorite() {
        // Disable the button when the parent activity gets started by the geofence notification.
        log.i("addGasFavorite: %s", isGeofenceIntent);
        if(isGeofenceIntent) return;

        // Pop up FavoriteListFragment when clicking the favorite button.
        if(TextUtils.isEmpty(binding.tvStationName.getText())) {
            FavoriteListFragment.newInstance(getString(R.string.exp_title_gas), Constants.GAS)
                    .show(requireActivity().getSupportFragmentManager(), null);

        // Remove the station both from the local db and Firestore, the result of which is notified
        // to FavoriteGeofenceHelper.OnGeofenceListener which implements the boolean value set to false.
        } else if(isFavoriteGas) {
            Snackbar snackbar = Snackbar.make(
                    binding.getRoot(), getString(R.string.gas_snackbar_alert_remove_favorite), Snackbar.LENGTH_SHORT);
            snackbar.setAction(R.string.popup_msg_confirm, view -> {
                geofenceHelper.removeFavoriteGeofence(stnName, stnId, Constants.GAS);
            }).show();

        // Add a station both to the local db and Firestore as far as the number of favorite stations
        // is less than Constants.MAX_FAVORITE, currently set to 10.
        } else {
            final int placeholder = mDB.favoriteModel().countFavoriteNumber(Constants.GAS);
            if(placeholder == Constants.MAX_FAVORITE) {
                Snackbar.make(binding.getRoot(), R.string.exp_snackbar_favorite_limit, Snackbar.LENGTH_SHORT).show();
            } else {
                // Then, retrieve the station data which pass to FavoriteGeofenceHelper to register with
                // Geofence.
                firestore.collection("gas_station").document(stnId).get().addOnCompleteListener(task -> {
                    if(task.isSuccessful()) {
                        DocumentSnapshot snapshot = task.getResult();
                        if (snapshot != null && snapshot.exists()) {
                            binding.btnGasFavorite.setBackgroundResource(R.drawable.btn_favorite_selected);
                            geofenceHelper.addFavoriteGeofence(snapshot, placeholder, Constants.GAS);
                        }
                    }
                });

            }
        }

    }

    // Not only save the form data in the Room, but also upload the station rating and comment data
    // to Firestor. The local db and Firestore should be refactored to sync as long as the user gets
    // logged in.
    public void saveGasData(String userId) {
        if(!doEmptyCheck()) return;
        // CreateEntity instances both of which are correlated with ForeignKe
        ExpenseBaseEntity baseEntity = new ExpenseBaseEntity();
        GasManagerEntity gasEntity = new GasManagerEntity();

        baseEntity.dateTime = calendar.getTimeInMillis();
        baseEntity.category = Constants.GAS;
        gasEntity.stnName = binding.tvStationName.getText().toString();
        gasEntity.stnId = stnId;
        //gasEntity.extraExpense = binding.tvExtraPayment.getText().toString();

        baseEntity.mileage = Integer.parseInt(binding.tvGasMileage.getText().toString().replaceAll(",", ""));
        gasEntity.gasPayment = Integer.parseInt(binding.tvGasPayment.getText().toString().replaceAll(",", ""));
        gasEntity.gasAmount = Integer.parseInt(binding.tvGasAmount.getText().toString().replaceAll(",", ""));
        gasEntity.unitPrice = Integer.parseInt(binding.etGasUnitPrice.getText().toString().replaceAll(",", ""));
        gasEntity.washPayment = Integer.parseInt(binding.tvCarwash.getText().toString().replaceAll(",", ""));
        gasEntity.extraPayment = Integer.parseInt(binding.tvExtraPayment.getText().toString().replaceAll(",", ""));

        baseEntity.totalExpense = gasEntity.gasPayment + gasEntity.washPayment + gasEntity.extraPayment;

        // Insert the data to the db.
        long rowId = mDB.gasManagerModel().insertBoth(baseEntity, gasEntity);
        if(rowId > 0) {
            mSettings.edit().putString(PREF_ODOMETER, binding.tvGasMileage.getText().toString()).apply();
            uploadGasDataToFirestore(userId, baseEntity.totalExpense);
        }

    }

    // Batch to upload the data of rating and comment to Firestore.
    public void uploadGasDataToFirestore(String userId, int gasTotal) {
        WriteBatch gasBatch = firestore.batch();
        if(binding.rbGasStation.getRating() > 0) {
            Map<String, Object> ratingData = new HashMap<>();
            ratingData.put("eval_num", FieldValue.increment(1));
            ratingData.put("eval_sum", FieldValue.increment(binding.rbGasStation.getRating()));

            DocumentReference ratingRef = firestore.collection("gas_eval").document(stnId);
            gasBatch.set(ratingRef, ratingData, SetOptions.merge());
        }

        // Add rating or comments to the Firestore, if any.
        if(!TextUtils.isEmpty(binding.etGasComment.getText())) {
            Map<String, Object> commentData = new HashMap<>();
            commentData.put("timestamp", FieldValue.serverTimestamp());
            commentData.put("userId", userId);
            commentData.put("name", userName);
            commentData.put("comments", binding.etGasComment.getText().toString());
            commentData.put("rating", binding.rbGasStation.getRating());

            DocumentReference commentRef = firestore.collection("gas_eval").document(stnId)
                    .collection("comments").document(userId);
            gasBatch.set(commentRef, commentData);
        }

        gasBatch.commit().addOnCompleteListener(task -> {
            if(task.isSuccessful()) {
                log.i("Total GasExpense: %s", gasTotal);
                fragmentModel.getTotalExpenseByCategory().setValue(gasTotal);
            } else log.e("Failed to save the form data");
        });
    }

    // Method to make an empty check. When successfully fetching the gas station and the price,
    // those 2 values automatically fill the views. In this case, only the payment value will be
    // checked.
    private boolean doEmptyCheck() {
        // Check if the gas station name is empty
        if(TextUtils.isEmpty(binding.tvStationName.getText())) {
            String msg = getResources().getString(R.string.toast_station_name);
            Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_SHORT).show();
            return false;
        }

        // Check if the gas unit price is empty
        if(TextUtils.isEmpty(binding.etGasUnitPrice.getText())) {
            String msg = getResources().getString(R.string.toast_unit_price);
            Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_SHORT).show();
            binding.etGasUnitPrice.requestFocus();
            return false;
        }

        // Check if the payment is empty.
        if(binding.tvGasPayment.getText().toString().matches("0")) {
            String msg = getResources().getString(R.string.toast_payment);
            Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    // Calculate an gas amount loaded with a given payment and unit price.  Unless a unit price is
    // given, toast a message to ask for inputting the price.
    //@SuppressWarnings("ConstantConditions")
    private void calculateGasAmount() {
        if(TextUtils.isEmpty(binding.etGasUnitPrice.getText())) {
            Toast.makeText(getActivity(), R.string.toast_unit_price, Toast.LENGTH_SHORT).show();
        } else if(!TextUtils.isEmpty(binding.etGasUnitPrice.getText()) &&
                !TextUtils.isEmpty(binding.tvGasPayment.getText())) {
            try {
                // Convert Number to the primitive int type.
                final String unitPrice = binding.etGasUnitPrice.getText().toString();
                final String payment = binding.tvGasPayment.getText().toString();
                int price = Objects.requireNonNull(df.parse(unitPrice)).intValue();
                int paid = Objects.requireNonNull(df.parse(payment)).intValue();
                String gasAmount = String.valueOf(paid/price);
                binding.tvGasAmount.setText(gasAmount);
            } catch(ParseException | NullPointerException e) {
                e.printStackTrace();
            }
        }
    }

    // In case the parent activity get started by GeofenceIntent which the notification has sent.
    private void getGeofenceIntentAction() {
        switch(category) {
            case Constants.GAS:
                binding.tvStationName.setText(geoStnName);
                stnId = geoStnId;
                isFavoriteGas = true;

                binding.btnGasFavorite.setBackgroundResource(R.drawable.btn_favorite_selected);
                binding.pbSearchStation.setVisibility(View.GONE);
                //btnChangeDate.setVisibility(View.GONE);

                // Task to fetch the gas price of a station with the station ID.
                favPriceTask = ThreadManager2.startFavoriteStationTask(
                        requireActivity(), opinetViewModel, stnId, false);
                break;
            case Constants.SVC:
                binding.pbSearchStation.setVisibility(View.GONE);
                binding.imgbtnGasSearch.setVisibility(View.VISIBLE);
                break;

            default: break;
        }
    }
}
