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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
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
import java.util.HashMap;
import java.util.Map;

/**
 * This fragment provides the form to fill in the gas expense.
 */
public class GasManagerFragment extends Fragment implements View.OnClickListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(GasManagerFragment.class);

    // Constants
    private static final int REQUEST_PERM_BACKGROUND_LOCATION = 1000;


    // Objects
    private FragmentGasManagerBinding binding;
    private CarmanDatabase mDB;
    private FirebaseFirestore firestore;
    private StationListViewModel stnListModel;
    private FragmentSharedModel fragmentModel;
    private OpinetViewModel opinetViewModel;

    private FavoriteGeofenceHelper geofenceHelper;
    private LocationTask locationTask;
    private StationListTask stnListTask;
    private FavoritePriceTask favPriceTask;
    private SharedPreferences mSettings;
    private DecimalFormat df;
    private NumberPadFragment numPad;

    // Fields
    private String[] defaultParams;
    private TextView targetView; //reference to a clicked view which is used in ViewModel
    private String dateFormat;
    private String stnName, stnId;// stnCode, stnAddrs;
    private String date;
    private String nickname;
    private String userId;
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
        mSettings = ((BaseActivity)getActivity()).getSharedPreferernces();

        // ViewModels: reconsider why the models references the ones defined in the parent activity;
        // it would rather  be better to redefine them here.
        fragmentModel = new ViewModelProvider(requireActivity()).get(FragmentSharedModel.class);
        stnListModel = new ViewModelProvider(requireActivity()).get(StationListViewModel.class);
        opinetViewModel = new ViewModelProvider(requireActivity()).get(OpinetViewModel.class);

        // Create FavoriteGeofenceHelper instance to add or remove a station to Favorte and
        // Geofence list when the favorite button clicks.
        geofenceHelper = new FavoriteGeofenceHelper(getContext());

        // Instantiate other miscellanies
        //df = BaseActivity.getDecimalFormatInstance();
        df = ((BaseActivity)getActivity()).getDecimalFormat();
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

                Snackbar.make(binding.getRoot(), R.string.gas_snackbar_favorite_removed, Snackbar.LENGTH_SHORT).show();
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

        binding = FragmentGasManagerBinding.inflate(inflater, container, false);
        View localView = binding.getRoot();

        // Check if it's possible to change the soft input mode on the fragment basis. Seems not work.
        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        binding.tvDateTime.setText(date);
        binding.tvMileage.setText(mSettings.getString(Constants.ODOMETER, "0"));
        binding.tvGasPayment.setText(mSettings.getString(Constants.PAYMENT, "0"));

        // TEST CODING REQUIRED.
        binding.pbSearchStation.setVisibility(View.VISIBLE);

        // Attach the event listeners
        binding.etUnitPrice.addTextChangedListener(new NumberTextWatcher(binding.etUnitPrice));
        binding.tvMileage.setOnClickListener(this);
        binding.tvGasPayment.setOnClickListener(this);
        binding.tvCarwash.setOnClickListener(this);
        binding.tvExtraPayment.setOnClickListener(this);
        binding.btnResetRatingbar.setOnClickListener(view -> binding.ratingBar.setRating(0f));

        // Check Geofencing permission(Background Location permission) first, then add a provider
        // not only to Geofencing list but alos favorite provider in the room.
        binding.btnGasFavorite.setOnClickListener(view -> {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                permBackLocation = Manifest.permission.ACCESS_BACKGROUND_LOCATION;
                checkBackgroundLocationPermission();
            } else addGasFavorite();
        });

        binding.imgbtnRefresh.setOnClickListener(view -> {
//            locationTask = ThreadManager2.fetchLocationTask(getContext(), locationModel);
//            binding.pbSearchStation.setVisibility(View.VISIBLE);
//            binding.imgbtnRefresh.setVisibility(View.GONE);
        });


        // Manager the comment and the rating bar which should be allowed to make as far as the
        // nick name(vehicle name) has been created.
        nickname = mSettings.getString(Constants.USER_NAME, null);

        // In case of writing the ratingbar and comment, it is required to have a registered nickname.
        binding.ratingBar.setOnRatingBarChangeListener((rb, rating, user) -> {
            if(TextUtils.isEmpty(nickname) && rating > 0) {
                binding.ratingBar.setRating(0f);
                Snackbar.make(localView, "Nickname required", Snackbar.LENGTH_SHORT).show();
            }
        });

        binding.etServiceComment.setOnFocusChangeListener((view, b) -> {
            if(b && TextUtils.isEmpty(nickname)) {
                Snackbar.make(localView, "Nickname required", Snackbar.LENGTH_SHORT).show();
                view.clearFocus();
            }
        });

        // In case the activity and this fragment get started by tabbing the geofence notification,
        // the pendingintent passes the name, id, time.
        if(isGeofenceIntent) {
            if(category == Constants.GAS) {
                binding.tvStationName.setText(geoStnName);
                stnId = geoStnId;
                isFavoriteGas = true;

                // Hanldinthe UIs
                binding.btnGasFavorite.setBackgroundResource(R.drawable.btn_favorite_selected);
                binding.pbSearchStation.setVisibility(View.GONE);
                //btnChangeDate.setVisibility(View.GONE);

                // Task to fetch the gas price of a station with the station ID.
                favPriceTask = ThreadManager.startFavoritePriceTask(getActivity(), opinetViewModel, stnId, false);

            } else if(category == Constants.SVC) {
                binding.pbSearchStation.setVisibility(View.GONE);
                binding.imgbtnRefresh.setVisibility(View.VISIBLE);
            }
        }

        return localView;
    }

    @Override
    //public void onActivityCreated(Bundle savedInstanceState) {
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Share a value input in NumberPadFragment as the view id passes to NumberPadFragment when
        // clicking, then returns an input value as SparseArray which contains the view id, which
        // may identify the view invoking NumberPadFragment and fill in the value into the view.
        fragmentModel.getSelectedValue().observe(getViewLifecycleOwner(), data -> {
            log.i("View ID: %s", data.keyAt(0));
            //targetView = localView.findViewById(data.keyAt(0));
            if(targetView != null) {
                targetView.setText(df.format(data.valueAt(0)));
                calculateGasAmount();
            }
        });

        stnListModel.getCurrentStation().observe(getViewLifecycleOwner(), curStn -> {
            log.i("current station");
            if(curStn != null) {
                stnName = curStn.getStnName();
                stnId = curStn.getStnId();
                binding.tvStationName.setText(stnName);
                binding.etUnitPrice.setText(String.valueOf(curStn.getStnPrice()));
                binding.etUnitPrice.setCursorVisible(false);

                // Query Favorite with the fetched station name or station id to tell whether the station
                // has registered with Favorite.
                checkGasFavorite(stnName, stnId);
            }

            binding.pbSearchStation.setVisibility(View.GONE);
            binding.imgbtnRefresh.setVisibility(View.VISIBLE);
        });

        // Under the condition that no currnet station is fetched because of not within GEOFENCE_RADIUS,
        // the user can have the favorite station list when clicking the favorite button.
        // In doing so, this fragment communicates w/ FavoriteListFragment to retrieve a favorite
        // station picked out of FavoriteListFragment using FragmentSharedModel. With the station id,
        // FavoritePriceTask gets started to have the gas price.
        fragmentModel.getFavoriteGasEntity().observe(getViewLifecycleOwner(), entity -> {
            binding.tvStationName.setText(entity.providerName);
            binding.btnGasFavorite.setBackgroundResource(R.drawable.btn_favorite_selected);
            stnId = entity.providerId;
            isFavoriteGas = true;

            favPriceTask = ThreadManager.startFavoritePriceTask(
                    getActivity(), opinetViewModel, entity.providerId, false);
        });

        // Fetch the price info of a favorite gas station selected from FavoriteListFragment.
        opinetViewModel.getFavoritePriceData().observe(getViewLifecycleOwner(), data -> {
            log.i("Favorite price data: %s", data.get(defaultParams[0]));
            binding.etUnitPrice.setText(String.valueOf(data.get(defaultParams[0])));
            binding.etUnitPrice.setCursorVisible(false);
        });

        fragmentModel.getPermission().observe(getViewLifecycleOwner(), isPermitted -> {
            log.i("rational dialog clicked");
            if(isPermitted) requestPermissions(new String[]{permBackLocation}, REQUEST_PERM_BACKGROUND_LOCATION);
            else log.i("DENIED");
        });


    }

    @Override
    public void onResume() {
        super.onResume();
        // Must define FragmentSharedModel.setCurrentFragment() in onResume(), not onActivityCreated()
        // because the value of FragmentSharedModel.getCurrentFragment() is retrieved in onCreateView()
        // of ExpensePagerFragment. Otherwise, an error occurs due to asyncronous lifecycle.
        fragmentModel.setCurrentFragment(this);
        //fragmentModel.getExpenseGasFragment().setValue(this);
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
                initValue = binding.tvMileage.getText().toString();
                break;

            case R.id.tv_gas_payment:
                initValue = binding.tvGasPayment.getHint().toString();
                break;

            case R.id.tv_carwash:
                initValue = binding.tvCarwash.getHint().toString();
                break;

            case R.id.tv_extra_payment:
                initValue = binding.tvExtraPayment.getHint().toString();
                break;
        }

        // Pass the id of TextView to NumberPadFragment for which TextView is being focused to wait
        // for a new value.
        //NumberPadFragment.newInstance(null, initValue, v.getId()).show(getFragmentManager(), "numPad");
        //args.putString("title", itemTitle);
        args.putInt("viewId", v.getId());
        args.putString("initValue", initValue);
        numPad.setArguments(args);

        if(getActivity() != null) numPad.show(getActivity().getSupportFragmentManager(), "numberPad");
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


    @SuppressWarnings("ConstantConditions")
    private void addGasFavorite() {
        // Disable the button when the parent activity gets started by the geofence notification.
        log.i("addGasFavorite: %s", isGeofenceIntent);
        if(isGeofenceIntent) return;

        // Pop up FavoriteListFragment when clicking the favorite button.
        if(TextUtils.isEmpty(binding.tvStationName.getText())) {
            FavoriteListFragment.newInstance(getString(R.string.exp_title_gas), Constants.GAS)
                    .show(getActivity().getSupportFragmentManager(), null);

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

    // Method for inserting data to SQLite database
    @SuppressWarnings("ConstantConditions")
    public boolean saveGasData(){
        // Null check for the parent activity
        if(!doEmptyCheck()) return false;

        fragmentModel.setCurrentFragment(this);
        long milliseconds = BaseActivity.parseDateTime(dateFormat, binding.tvDateTime.getText().toString());

        // Create Entity instances both of which are correlated by Foreinkey
        ExpenseBaseEntity basicEntity = new ExpenseBaseEntity();
        GasManagerEntity gasEntity = new GasManagerEntity();

        basicEntity.dateTime = milliseconds;
        basicEntity.category = Constants.GAS;
        gasEntity.stnName = binding.tvStationName.getText().toString();
        //gasEntity.stnAddrs = stnAddrs;
        gasEntity.stnId = stnId;
        gasEntity.extraExpense = binding.tvExtraPayment.getText().toString();

        try {
            basicEntity.mileage = df.parse(binding.tvMileage.getText().toString()).intValue();
            gasEntity.gasPayment = df.parse(binding.tvGasPayment.getText().toString()).intValue();
            gasEntity.gasAmount = df.parse(binding.tvGasAmount.getText().toString()).intValue();
            gasEntity.unitPrice = df.parse(binding.etUnitPrice.getText().toString()).intValue();
            // BUG!!
            // W/System.err:     at java.text.NumberFormat.parse(NumberFormat.java:351)
            // at com.silverback.carman2.fragments.GasManagerFragment.saveGasData(GasManagerFragment.java:546)
            gasEntity.washPayment = df.parse(binding.tvCarwash.getText().toString()).intValue();
            gasEntity.extraPayment = df.parse(binding.tvExtraPayment.getText().toString()).intValue();

        } catch(ParseException | NullPointerException e) {
            e.printStackTrace();
        }

        basicEntity.totalExpense = gasEntity.gasPayment + gasEntity.washPayment + gasEntity.extraPayment;

        // Insert the data to the local db.
        int rowId = mDB.gasManagerModel().insertBoth(basicEntity, gasEntity);
        if(rowId > 0) {
            mSettings.edit().putString(Constants.ODOMETER, binding.tvMileage.getText().toString()).apply();
            Toast.makeText(getActivity(), getString(R.string.toast_save_success), Toast.LENGTH_SHORT).show();

            // FireStore Process to upload the rating and comments with Station ID.
            if(binding.ratingBar.getRating() > 0) {
                log.i("RatingBar: %s", binding.ratingBar.getRating());
                Map<String, Object> ratingData = new HashMap<>();
                ratingData.put("eval_num", FieldValue.increment(1));
                ratingData.put("eval_sum", FieldValue.increment(binding.ratingBar.getRating()));

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
            if(!TextUtils.isEmpty(binding.etServiceComment.getText())) {

                Map<String, Object> commentData = new HashMap<>();
                commentData.put("timestamp", FieldValue.serverTimestamp());
                commentData.put("name", nickname);
                commentData.put("comments", binding.etServiceComment.getText().toString());
                commentData.put("rating", binding.ratingBar.getRating());

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
        if(TextUtils.isEmpty(binding.tvStationName.getText())) {
            String msg = getResources().getString(R.string.toast_station_name);
            //Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
            Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_SHORT).show();
            return false;
        }

        // Check if the gas unit price is empty
        if(TextUtils.isEmpty(binding.etUnitPrice.getText())) {
            String msg = getResources().getString(R.string.toast_unit_price);
            //Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
            Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_SHORT).show();
            binding.etUnitPrice.requestFocus();
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
        if(TextUtils.isEmpty(binding.etUnitPrice.getText())) {
            Toast.makeText(getActivity(), R.string.toast_unit_price, Toast.LENGTH_SHORT).show();
        } else if(!TextUtils.isEmpty(binding.etUnitPrice.getText()) &&
                !TextUtils.isEmpty(binding.tvGasPayment.getText())) {
            try {
                // Convert Number to the primitive int type.
                int price = df.parse(binding.etUnitPrice.getText().toString()).intValue();
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
}
