package com.silverback.carman.fragments;


import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.silverback.carman.BaseActivity;
import com.silverback.carman.ExpenseActivity;
import com.silverback.carman.R;
import com.silverback.carman.database.CarmanDatabase;
import com.silverback.carman.database.ExpenseBaseEntity;
import com.silverback.carman.database.GasManagerEntity;
import com.silverback.carman.databinding.FragmentGasManagerBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.threads.FavoritePriceTask;
import com.silverback.carman.threads.LocationTask;
import com.silverback.carman.threads.StationListTask;
import com.silverback.carman.threads.ThreadManager;
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
public class GasManagerFragment extends Fragment {//implements View.OnClickListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(GasManagerFragment.class);
    // Constants
    private static final int REQUEST_PERM_BACKGROUND_LOCATION = 1000;
    // Objects
    private FragmentGasManagerBinding binding;
    private CarmanDatabase mDB;
    private FirebaseFirestore firestore;

    private StationListViewModel stnListModel;
    private LocationViewModel locationModel;
    private FragmentSharedModel fragmentModel;
    private OpinetViewModel opinetViewModel;

    private FavoriteGeofenceHelper geofenceHelper;
    private LocationTask locationTask;
    private StationListTask stnListTask;
    private FavoritePriceTask favPriceTask;
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
    private String userName;
    private String geoStnName, geoStnId;
    private String permBackLocation;
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
            //defaultParams = getArguments().getStringArray("defaultParams");
            //userId = getArguments().getString("userId");
        }

        // The parent activity gets started by tabbing the Geofence notification w/ the pendingintent
        // that contains the station id, name, geofencing time, and category. The data will fill out
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
        mSettings = ((BaseActivity)getActivity()).getSharedPreferernces();
        geofenceHelper = new FavoriteGeofenceHelper(getContext());
        calendar = Calendar.getInstance(Locale.getDefault());
        df = ((BaseActivity)getActivity()).getDecimalFormat();
        sdf = new SimpleDateFormat(getString(R.string.date_format_1), Locale.getDefault());

        // ViewModels: reconsider why the models references the ones defined in the parent activity;
        // it would rather  be better to redefine them here.
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

                Snackbar.make(binding.getRoot(), R.string.gas_snackbar_favorite_removed, Snackbar.LENGTH_SHORT).show();
                isFavoriteGas = false;

            }

            @Override
            public void notifyAddGeofenceFailed() {
                log.i("Failed to add the gas station to Geofence");
            }
        });

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
        binding.tvGasMileage.setText(mSettings.getString(Constants.ODOMETER, "0"));
        binding.tvGasPayment.setText(mSettings.getString(Constants.PAYMENT, "0"));

        // Attach the event listeners
        binding.etGasUnitPrice.addTextChangedListener(new NumberTextWatcher(binding.etGasUnitPrice));
        binding.btnResetRatingbar.setOnClickListener(view -> binding.rbGasStation.setRating(0f));

        // Check Geofencing permission(Background Location permission) first, then add a provider
        // not only to Geofencing list but alos favorite provider in the room.
        binding.btnGasFavorite.setOnClickListener(view -> {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                permBackLocation = Manifest.permission.ACCESS_BACKGROUND_LOCATION;
                checkBackgroundLocationPermission();
            } else addGasFavorite();
        });

        binding.imgbtnGasSearch.setOnClickListener(view -> {
            locationTask = ThreadManager2.getInstance().fetchLocationTask(requireActivity(), locationModel);
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
                stnListTask = ThreadManager.startStationListTask(stnListModel, location, defaultParams);
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
        // FavoritePriceTask gets started to have the gas price.
        fragmentModel.getFavoriteGasEntity().observe(getViewLifecycleOwner(), data -> {
            binding.tvStationName.setText(data.providerName);
            binding.btnGasFavorite.setBackgroundResource(R.drawable.btn_favorite_selected);
            stnId = data.providerId;
            isFavoriteGas = true;

            favPriceTask = ThreadManager.startFavoritePriceTask(
                    requireActivity(), opinetViewModel, data.providerId, false);
        });

        // Fetch the price info of a favorite gas station selected from FavoriteListFragment.
        opinetViewModel.getFavoritePriceData().observe(getViewLifecycleOwner(), data -> {
            log.i("Favorite price data: %s", data.get(defaultParams[0]));
            binding.etGasUnitPrice.setText(String.valueOf(data.get(defaultParams[0])));
            binding.etGasUnitPrice.setCursorVisible(false);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        fragmentModel.setCurrentFragment(this);
        binding.tvGasDatetime.setText(sdf.format(System.currentTimeMillis()));
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

    private void setCurrentStation() {
        if(isGeofenceIntent) return;

        stnListModel.getCurrentStation().observe(getViewLifecycleOwner(), curStn -> {
            log.i("current station");
            if(curStn != null) {
                stnName = curStn.getStnName();
                stnId = curStn.getStnId();
                binding.tvStationName.setText(stnName);
                binding.etGasUnitPrice.setText(String.valueOf(curStn.getStnPrice()));
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
        mDB.favoriteModel().findFavoriteGasName(name, id, Constants.GAS)
                .observe(getViewLifecycleOwner(), stnName -> {
                    if (TextUtils.isEmpty(stnName)) {
                        isFavoriteGas = false;
                        binding.btnGasFavorite.setBackgroundResource(R.drawable.btn_favorite);
                    } else {
                        isFavoriteGas = true;
                        binding.btnGasFavorite.setBackgroundResource(R.drawable.btn_favorite_selected);
                    }
                });
    }


    //@SuppressWarnings("ConstantConditions")
    private void addGasFavorite() {
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
            log.i("favorite button clicked to remove");
            Snackbar snackbar = Snackbar.make(
                    binding.getRoot(), getString(R.string.gas_snackbar_alert_remove_favorite), Snackbar.LENGTH_SHORT);
            snackbar.setAction(R.string.popup_msg_confirm, view -> {
                geofenceHelper.removeFavoriteGeofence(stnName, stnId, Constants.GAS);
                binding.getRoot().setBackgroundResource(R.drawable.btn_favorite);
            }).show();

        } else {
            // Add a station both to the local db and Firestore as far as the number of favorite stations
            // is less than Constants.MAX_FAVORITE, currently set to 10.
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

    // Save the gas-related data in Room database, which is called from the parent activity.
    //@SuppressWarnings("ConstantConditions")
    public LiveData<Integer> saveGasData(String userId){

        MutableLiveData<Integer> liveTotalData = new MutableLiveData<>();
        if(!doEmptyCheck())  liveTotalData.setValue(-1);

        fragmentModel.setCurrentFragment(this);

        // Create Entity instances both of which are correlated by Foreinkey
        ExpenseBaseEntity basicEntity = new ExpenseBaseEntity();
        GasManagerEntity gasEntity = new GasManagerEntity();

        basicEntity.dateTime = calendar.getTimeInMillis();
        basicEntity.category = Constants.GAS;

        gasEntity.stnName = binding.tvStationName.getText().toString();
        gasEntity.stnId = stnId;
        gasEntity.extraExpense = binding.tvExtraPayment.getText().toString();


        try {
            basicEntity.mileage = Objects.requireNonNull(df.parse(binding.tvGasMileage.getText().toString())).intValue();
            gasEntity.gasPayment = Objects.requireNonNull(df.parse(binding.tvGasPayment.getText().toString())).intValue();
            gasEntity.gasAmount = Objects.requireNonNull(df.parse(binding.tvGasAmount.getText().toString())).intValue();
            gasEntity.unitPrice = Objects.requireNonNull(df.parse(binding.etGasUnitPrice.getText().toString())).intValue();
            gasEntity.washPayment = Objects.requireNonNull(df.parse(binding.tvCarwash.getText().toString())).intValue();
            gasEntity.extraPayment = Objects.requireNonNull(df.parse(binding.tvExtraPayment.getText().toString())).intValue();

        } catch(ParseException | NullPointerException e) {
            e.printStackTrace();
        }

        basicEntity.totalExpense = gasEntity.gasPayment + gasEntity.washPayment + gasEntity.extraPayment;
        log.i("gas payment: %s" , basicEntity.totalExpense);
        mDB.gasManagerModel().insertBoth(basicEntity, gasEntity);

        mSettings.edit().putString(Constants.ODOMETER, binding.tvGasMileage.getText().toString()).apply();
        //Toast.makeText(getActivity(), getString(R.string.toast_save_success), Toast.LENGTH_SHORT).show();
        liveTotalData.postValue(basicEntity.totalExpense);

        // FireStore Process to upload the rating and comments with Station ID.
        if(binding.rbGasStation.getRating() > 0) {
            log.i("RatingBar: %s", binding.rbGasStation.getRating());
            Map<String, Object> ratingData = new HashMap<>();
            ratingData.put("eval_num", FieldValue.increment(1));
            ratingData.put("eval_sum", FieldValue.increment(binding.rbGasStation.getRating()));

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

        // Add rating or comments to the Firestore, if any.
        if(!TextUtils.isEmpty(binding.etGasComment.getText())) {
            log.i("comment and rating data");
            Map<String, Object> commentData = new HashMap<>();
            commentData.put("timestamp", FieldValue.serverTimestamp());
            commentData.put("name", userName);
            commentData.put("comments", binding.etGasComment.getText().toString());
            commentData.put("rating", binding.rbGasStation.getRating());

            firestore.collection("gas_eval").document(stnId).collection("comments").document(userId)
                    .set(commentData).addOnCompleteListener(task -> {
                if(task.isSuccessful()) {
                    log.e("Commments successfully uploaded");
                } else {
                    log.e("Comments upload failed: %s", task.getException());
                }
            });
        }

        return liveTotalData;
    }

    // Method to make an empty check. When successfully fetching the gas station and the price,
    // those 2 values automatically fill the views. In this case, only the payment value will be
    // checked.
    private boolean doEmptyCheck() {
        // Check if the gas station name is empty
        if(TextUtils.isEmpty(binding.tvStationName.getText())) {
            String msg = getResources().getString(R.string.toast_station_name);
            //Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
            Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_SHORT).show();
            return false;
        }

        // Check if the gas unit price is empty
        if(TextUtils.isEmpty(binding.etGasUnitPrice.getText())) {
            String msg = getResources().getString(R.string.toast_unit_price);
            //Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
            Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_SHORT).show();
            binding.etGasUnitPrice.requestFocus();
            return false;
        }

        // Check if the payment is empty.
        if(binding.tvGasPayment.getText().toString().matches("0")) {
            String msg = getResources().getString(R.string.toast_payment);
            //Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
            Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    // Calculate an gas amount loaded with a given payment and unit price.  Unless a unit price is
    // given, toast a message to ask for inputting the price.
    @SuppressWarnings("ConstantConditions")
    private void calculateGasAmount() {
        if(TextUtils.isEmpty(binding.etGasUnitPrice.getText())) {
            Toast.makeText(getActivity(), R.string.toast_unit_price, Toast.LENGTH_SHORT).show();
        } else if(!TextUtils.isEmpty(binding.etGasUnitPrice.getText()) &&
                !TextUtils.isEmpty(binding.tvGasPayment.getText())) {
            try {
                // Convert Number to the primitive int type.
                int price = df.parse(binding.etGasUnitPrice.getText().toString()).intValue();
                int paid = df.parse(binding.tvGasPayment.getText().toString()).intValue();
                String gasAmount = String.valueOf(paid/price);
                binding.tvGasAmount.setText(gasAmount);
            } catch(ParseException | NullPointerException e) {
                e.printStackTrace();
            }
        }
    }

    // Permission check for ACCESS_BACKGROUND_LOCATION
    @SuppressWarnings("ConstantConditions")
    private void checkBackgroundLocationPermission(){
        if(ContextCompat.checkSelfPermission(getContext(), permBackLocation) == PackageManager.PERMISSION_GRANTED) {
            addGasFavorite();

        } else if(ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), permBackLocation)) {
            PermRationaleFragment permDialog = new PermRationaleFragment();
            permDialog.setContents("Hell", "World");
            permDialog.show(getActivity().getSupportFragmentManager(), null);

        } else requestPermissions(new String[]{permBackLocation}, REQUEST_PERM_BACKGROUND_LOCATION);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permission, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERM_BACKGROUND_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                addGasFavorite();
            } else {

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
                favPriceTask = ThreadManager.startFavoritePriceTask(getActivity(), opinetViewModel, stnId, false);

                break;
            case Constants.SVC:
                binding.pbSearchStation.setVisibility(View.GONE);
                binding.imgbtnGasSearch.setVisibility(View.VISIBLE);
                break;
            default: break;
        }
    }
}
