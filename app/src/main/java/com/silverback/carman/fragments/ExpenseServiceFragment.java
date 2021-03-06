package com.silverback.carman.fragments;


import static com.silverback.carman.SettingActivity.PREF_ODOMETER;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.silverback.carman.BaseActivity;
import com.silverback.carman.R;
import com.silverback.carman.adapters.ExpServiceItemAdapter;
import com.silverback.carman.database.CarmanDatabase;
import com.silverback.carman.database.ExpenseBaseEntity;
import com.silverback.carman.database.ServiceManagerEntity;
import com.silverback.carman.database.ServicedItemEntity;
import com.silverback.carman.databinding.FragmentServiceManagerBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.Constants;
import com.silverback.carman.utils.FavoriteGeofenceHelper;
import com.silverback.carman.utils.RecyclerDividerUtil;
import com.silverback.carman.viewmodels.FragmentSharedModel;

import org.json.JSONArray;
import org.json.JSONException;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class ExpenseServiceFragment extends Fragment implements
        ExpServiceItemAdapter.OnParentFragmentListener {

    // Constants
    private static final LoggingHelper log = LoggingHelperFactory.create(ExpenseServiceFragment.class);
    //private static final int SVC_CENTER = 2;

    // Objects
    //private SparseArray<ServiceManagerDao.LatestServiceData> sparseServiceArray;
    private FragmentServiceManagerBinding binding;
    private SharedPreferences mSettings;
    private CarmanDatabase mDB;
    private FirebaseFirestore firestore;
    private Calendar calendar;

    private FragmentSharedModel fragmentModel;
    //private PagerAdapterViewModel pagerAdapterModel;
    //private LocationViewModel locationModel;
    //private ServiceCenterViewModel svcCenterModel;
    //private StationListViewModel stationModel;
    //private ServiceCenterTask serviceCenterTask;
    //private Location location;
    private FavoriteGeofenceHelper geofenceHelper;
    private FavoriteGeofenceHelper.OnGeofenceListener geofenceListener;
    private ExpServiceItemAdapter mAdapter;
    private DecimalFormat df;
    private SimpleDateFormat sdf;
    private JSONArray jsonServiceArray;
    private Location svcLocation;
    //private String svcAddress;
    //private String svcCompany;

    // Fields
    //private String distCode;
    private int itemPos;
    private int serviceTotal;
    private boolean isGeofenceIntent; // check if this has been launched by Geofence.
    private boolean isSvcFavorite;
    private String userId;
    private String svcId;
    private String svcName;
    //private String svcComment;
    private int svcPeriod;
    private String geoSvcName;
    //private float svcRating;
    private long geoTime;
    private int category;
    //private Location mPrevLocation;

    public ExpenseServiceFragment() {
        // Required empty public constructor
    }

    //@SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // In case the activity is initiated by tabbing the notification, which sent the intent w/
        // action and extras for the geofance data.
        getGeofenceIntent();

        // Instantiate objects.
        //mSettings = ((BaseActivity)requireActivity()).getSharedPreferernces();
        mSettings = PreferenceManager.getDefaultSharedPreferences(requireContext());
        mDB = CarmanDatabase.getDatabaseInstance(requireActivity().getApplicationContext());
        firestore = FirebaseFirestore.getInstance();
        calendar = Calendar.getInstance(Locale.getDefault());
        sdf = new SimpleDateFormat(getString(R.string.date_format_1), Locale.getDefault());
        df = BaseActivity.getDecimalFormatInstance();
        if(geofenceHelper == null) geofenceHelper = new FavoriteGeofenceHelper(requireContext());

        // Get the service periond unit from SharedPreferences and pass it to the adapter as int type.
        String period = mSettings.getString(Constants.SERVICE_PERIOD, getString(R.string.pref_svc_period_mileage));
        //if(period.equals(getString(R.string.pref_svc_period_mileage))) svcPeriod = 0;
        //else if(period.equals(getString(R.string.pref_svc_period_month))) svcPeriod = 1;
        svcPeriod = (Objects.equals(period, getString(R.string.pref_svc_period_mileage)))? 0 : 1;

        // ViewModels
        fragmentModel = new ViewModelProvider(requireActivity()).get(FragmentSharedModel.class);
        //svcCenterModel = new ViewModelProvider(this).get(ServiceCenterViewModel.class);
        //pagerAdapterModel = new ViewModelProvider(requireActivity()).get(PagerAdapterViewModel.class);
        //locationModel = new ViewModelProvider(requireActivity()).get(LocationViewModel.class);
        //stationModel = new ViewModelProvider(requireActivity()).get(StationListViewModel.class);

        // Attach the listener which implements the following callback methods when a location is added
        // to or removed from the favorite provider as well as geofence list.
        geofenceHelper.setGeofenceListener(getGeofenceListener());

    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentServiceManagerBinding.inflate(inflater);

        long visitTime = (isGeofenceIntent)? geoTime : System.currentTimeMillis();
        binding.tvServiceDate.setText(sdf.format(visitTime));
        binding.tvSvcPayment.setText("0");
        binding.tvSvcMileage.setText(mSettings.getString(PREF_ODOMETER, "0"));
        binding.btnSvcFavorite.setBackgroundResource(R.drawable.btn_favorite);
        createRecyclerServiceItemView();

        // Set event listeners.
        binding.btnRegisterService.setOnClickListener(v -> registerFavorite());
        binding.radioGroup.setOnCheckedChangeListener(this::switchServiceSpanType);
        //binding.btnSvcFavorite.setOnClickListener(v -> addServiceFavorite());

        // Fill in the form automatically with the data transferred from the PendingIntent of Geofence
        // only if the parent activity gets started by the notification and its category should be
        // Constants.SVC
        if(isGeofenceIntent && category == Constants.SVC) setGeofenceIntent();
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Update the time to the current time.
        binding.tvServiceDate.setText(sdf.format(System.currentTimeMillis()));
        // To sync the fragment w/ the viewpager in the top frame. MUST be declared here, not in
        // onViewCreated.
        fragmentModel.getCurrentFragment().setValue(Constants.SVC);

    }

    @Override
    public void onPause() {
        super.onPause();
        log.i("ExpenseServiceFragment in Pause state");
        // When clicking the save button, prevent the observer from invoking the method, which
        // occasionally
        fragmentModel.getCurrentFragment().removeObservers(this);
    }


    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        addViewModelObserver(fragmentModel);
        // Show the service data and animate the progressbar to indicate when to check an item.
        try { createServiceProgressBar();}
        catch (JSONException e) { e.printStackTrace(); }
    }


    // Implement ExpServiceItemAdapter.OnParentFragmentListener to pop up NumberPadFragmnet and
    // intput the amount of expense in each service item.
    // setItemPosition():
    // getCurrentMileage():
    // subtractCost():
    @Override
    public void setItemPosition(int position) {
        itemPos = position;
    }
    @Override
    public int getCurrentMileage() {
        try {
            Number num = df.parse(binding.tvSvcMileage.getText().toString());
            if(num != null) return num.intValue();
        } catch(ParseException e) { e.printStackTrace();}

        return 0;
    }
    @Override
    public void subtractCost(int value) {
        serviceTotal -= value;
        binding.tvSvcPayment.setText(df.format(serviceTotal));
    }

    // Attach the listener which implements the following callback methods when a location is added
    // to or removed from the favorite provider as well as geofence list.
    private FavoriteGeofenceHelper.OnGeofenceListener getGeofenceListener() {
        return new FavoriteGeofenceHelper.OnGeofenceListener() {
            @Override
            public void notifyAddGeofenceCompleted(int placeholder) {
                isSvcFavorite = true;
                Snackbar.make(binding.getRoot(), getString(R.string.svc_msg_add_favorite), Snackbar.LENGTH_SHORT).show();
            }
            @Override
            public void notifyRemoveGeofenceCompleted(int placeholder) {
                isSvcFavorite = false;
                Snackbar.make(binding.getRoot(), R.string.svc_snackbar_favorite_removed, Snackbar.LENGTH_SHORT).show();
            }
            @Override
            public void notifyAddGeofenceFailed() {
                log.e("Failed to add the service center to Geofence");
            }
        };
    }


    // Check if the parent activity gets started by the geofence intent and get extras.
    private void getGeofenceIntent(){
        String action = requireActivity().getIntent().getAction();
        if(action != null && action.equals(Constants.NOTI_GEOFENCE)) {
            //if(getActivity().getIntent().getAction().equals(Constants.NOTI_GEOFENCE)) {
            isGeofenceIntent = true;
            geoSvcName = requireActivity().getIntent().getStringExtra(Constants.GEO_NAME);
            geoTime = requireActivity().getIntent().getLongExtra(Constants.GEO_TIME, -1);
            category = requireActivity().getIntent().getIntExtra(Constants.GEO_CATEGORY, -1);
            //}
        }
    }

    private void setGeofenceIntent() {
        binding.etServiceProvider.setText(geoSvcName);
        binding.etServiceProvider.clearFocus();
        isSvcFavorite = true;

        binding.btnSvcFavorite.setBackgroundResource(R.drawable.btn_favorite_selected);
        binding.btnResetDatetime.setVisibility(View.GONE);
    }

    private void addViewModelObserver(ViewModel model) {
        if(model instanceof FragmentSharedModel) {
            // To notify ExpensePagerFragment of the current fragment to show its recent expenses
            //fragmentModel.getCurrentFragment().setValue(Constants.SVC);

            // Share a value b/w fragments  w/ DatePickerDialogFragment
            fragmentModel.getCustomDateAndTime().observe(getViewLifecycleOwner(), calendar -> {
                this.calendar = calendar;
                binding.tvServiceDate.setText(sdf.format(calendar.getTimeInMillis()));
            });

            // Share a value b/w  fragments w/ NumberPadFragment
            fragmentModel.getNumpadValue().observe(getViewLifecycleOwner(), data -> {
                final int viewId = data.keyAt(0);
                final int value = data.valueAt(0);
                if(viewId == R.id.tv_svc_mileage) {
                    binding.tvSvcMileage.setText(df.format(value));
                }else if(viewId == R.id.tv_item_cost) {
                    mAdapter.notifyItemChanged(itemPos, data);
                    serviceTotal += data.valueAt(0);
                    binding.tvSvcPayment.setText(df.format(serviceTotal));
                }
            });

            // Share a value b/w fragments w/ MemoPadFragment
            fragmentModel.getMemoPadValue().observe(getViewLifecycleOwner(), data -> {
                mAdapter.notifyItemChanged(itemPos, data);
            });

            // Get the params for removeGeofence() which are passed from FavroiteListFragment
            fragmentModel.getFavoriteSvcEntity().observe(getViewLifecycleOwner(), entity -> {
                svcName = entity.providerName;
                svcId = entity.providerId;
                //etServiceName.setText(svcName);
                binding.etServiceProvider.setText(svcName);
                binding.btnSvcFavorite.setBackgroundResource(R.drawable.btn_favorite_selected);
                isSvcFavorite = true;
            });

            // Get the id of a registered
            fragmentModel.getRegisteredServiceId().observe(getViewLifecycleOwner(), svcId -> {
                log.i("Registered: %s", svcId);
                this.svcId = svcId;
            });
        }
    }

    private void createRecyclerServiceItemView() {
        String jsonServiceItems = mSettings.getString(Constants.SERVICE_ITEMS, null);
        try {
            jsonServiceArray = new JSONArray(jsonServiceItems);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        binding.recyclerServiceItems.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerServiceItems.setHasFixedSize(true);
        RecyclerDividerUtil divider = new RecyclerDividerUtil(Constants.DIVIDER_HEIGHT_EXPENSE, 0,
                ContextCompat.getColor(requireContext(), R.color.recyclerDivider));

        binding.recyclerServiceItems.addItemDecoration(divider);
        mAdapter = new ExpServiceItemAdapter(jsonServiceArray, svcPeriod, this);
        binding.recyclerServiceItems.setAdapter(mAdapter);
    }

    // Show the lastest service and and animate the progress bar with it
    private void createServiceProgressBar() throws JSONException {
        for(int i = 0; i < jsonServiceArray.length(); i++) {
            final int pos = i;
            final String name = jsonServiceArray.optJSONObject(pos).getString("name");
            // Sync issue!!!!
            mDB.serviceManagerModel().loadServiceData(name).observe(getViewLifecycleOwner(), data -> {
                if (data != null) {
                    mAdapter.setServiceData(pos, data);
                    //mAdapter.notifyItemChanged(pos, data);
                }
            });
        }
    }

    private void registerFavorite() {
        svcName = binding.etServiceProvider.getText().toString();
        if(svcName.isEmpty()) {
            Snackbar.make(binding.getRoot(), R.string.svc_msg_empty_name, Snackbar.LENGTH_SHORT).show();
            return;
        }

        if(isSvcFavorite || svcLocation != null) {
            Snackbar.make(binding.getRoot(), "Already Registered", Snackbar.LENGTH_SHORT).show();

        } else {
            String district = mSettings.getString(Constants.DISTRICT, null);
            String distCode;
            try {
                JSONArray jsonArray = new JSONArray(district);
                distCode = (String)jsonArray.get(2);
            } catch(JSONException e) {
                log.e("JSONException: %s", e.getMessage());
                distCode = "0101";
            }

            RegisterDialogFragment.newInstance(svcName, distCode)
                    .show(getChildFragmentManager(), "registerDialog");
        }

    }



    // Register the service center with the favorite list and the geofence.
    public void addServiceFavorite() {
        // if(isGeofenceIntent) return;
        // Retrieve a service center from the favorite list, the value of which is sent via
        // fragmentSharedModel.getFavoriteName()
        if(TextUtils.isEmpty(binding.etServiceProvider.getText())) {
            String title = "Favorite Service Center";
            FavoriteListFragment.newInstance(title, Constants.SVC)
                    .show(requireActivity().getSupportFragmentManager(), null);

        // Remove the center out of the favorite list and the geofence
        } else if(isSvcFavorite) {
            Snackbar snackbar = Snackbar.make(
                    binding.getRoot(), getString(R.string.svc_snackbar_alert_remove_favorite), Snackbar.LENGTH_SHORT);
            snackbar.setAction(R.string.popup_msg_confirm, view -> {
                geofenceHelper.removeFavoriteGeofence(svcName, svcId, Constants.SVC);
                binding.btnSvcFavorite.setBackgroundResource(R.drawable.btn_favorite);
            });
            snackbar.show();
            //firestore.collection("svc_eval").document(svcId).update("favorite_num", FieldValue.increment(-1));

        // Add the service center with the favorite list and geofence as far as it has been
        // already registered in RegisterDialogFragment.
        } else {
            log.i("Add favorite: %s", svcId);
            if (TextUtils.isEmpty(svcId)) {
                InputMethodManager imm = (InputMethodManager)requireActivity()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(binding.etServiceProvider.getWindowToken(), 0);
                binding.etServiceProvider.clearFocus();
                Snackbar.make(binding.getRoot(), R.string.svc_msg_registration, Snackbar.LENGTH_SHORT).show();

            } else {
                // Check if the total number of the service favorites are out of the max limit.
                final int placeholder = mDB.favoriteModel().countFavoriteNumber(Constants.SVC);
                if (placeholder == Constants.MAX_FAVORITE) {
                    Snackbar.make(binding.getRoot(), getString(R.string.exp_snackbar_favorite_limit), Snackbar.LENGTH_SHORT).show();

                } else {
                    // Query the data of a station from Firestore and pass the data to FavoriteGeofenceHelper
                    // for adding the favoirte list to the local db and Firestore as well.
                    firestore.collection("svc_station").document(svcId).get().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot snapshot = task.getResult();
                            if (snapshot != null && snapshot.exists()) {
                                binding.btnSvcFavorite.setBackgroundResource(R.drawable.btn_favorite_selected);
                                geofenceHelper.addFavoriteGeofence(snapshot, placeholder, Constants.SVC);
                            }
                        }
                    });

                }
            }
        }

    }
    // Implement RadioGroup.setOnCheckedChangeListener.
    private void switchServiceSpanType(RadioGroup group, int checkedId) {
        if(checkedId == R.id.radio1) mAdapter.setServiceOption(0);
        else if(checkedId == R.id.radio2) mAdapter.setServiceOption(1);
    }

    public void saveServiceData(String userId) {
        if(!doEmptyCheck()) return;
        //String dateFormat = getString(R.string.date_format_1);
        //long milliseconds = BaseActivity.parseDateTime(dateFormat, binding.tvServiceDate.getText().toString());
        //log.i("service data saved: %s", milliseconds);
        int mileage = 0;
        try {
            mileage = Objects.requireNonNull(df.parse(binding.tvSvcMileage.getText().toString())).intValue();
        } catch(ParseException e) {
            log.e("ParseException: %s", e.getMessage());
        }

        ExpenseBaseEntity baseEntity = new ExpenseBaseEntity();
        ServiceManagerEntity serviceEntity = new ServiceManagerEntity();
        ServicedItemEntity checkedItem;
        List<ServicedItemEntity> itemEntityList = new ArrayList<>();

        baseEntity.dateTime = calendar.getTimeInMillis();
        baseEntity.mileage = mileage;
        baseEntity.category = Constants.SVC;
        baseEntity.totalExpense = serviceTotal;

        serviceEntity.serviceCenter = binding.etServiceProvider.getText().toString();
        serviceEntity.serviceAddrs = "seoul, korea"; //temp coding

        for(int i = 0; i < mAdapter.arrCheckedState.length; i++) {
            if(mAdapter.arrCheckedState[i]) {
                checkedItem = new ServicedItemEntity();
                checkedItem.itemName = jsonServiceArray.optJSONObject(i).optString("name");
                checkedItem.itemPrice = mAdapter.arrItemCost[i];
                checkedItem.itemMemo = mAdapter.arrItemMemo[i];
                itemEntityList.add(checkedItem);
            }
        }

        // Insert data into both ExpenseBaseEntity and ServiceManagerEntity at the same time
        // using @Transaction in ServiceManagerDao.
        int rowId = mDB.serviceManagerModel().insertAll(baseEntity, serviceEntity, itemEntityList);
        if(rowId > 0) {
            mSettings.edit().putString(PREF_ODOMETER, binding.tvSvcMileage.getText().toString()).apply();
            //SparseIntArray sparseArray = new SparseIntArray();
            //sparseArray.put(Constants.SVC, totalExpense);
            fragmentModel.getTotalExpenseByCategory().setValue(serviceTotal);

        } //else totalExpenseLive.setValue(0);
    }

    // Service rating should be uploaded to Firestore, which is to programmed soon.
    // At the moment, the service station should be filled in  in the dialog fragment and the data
    // should be uploaded to Firestore in the dialog, which has to be refactored.
    private void uploadSvcDataToFirestore(String userId, int total) {
        log.i("upload service data to Firestore: %s", total);

    }

    private boolean doEmptyCheck() {
        if(TextUtils.isEmpty(binding.etServiceProvider.getText())) {
            String msg = getString(R.string.svc_snackbar_stnname);
            Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
            binding.etServiceProvider.requestFocus();
            return false;
        }

        if(TextUtils.isEmpty(binding.tvSvcMileage.getText())) {
            Snackbar.make(binding.getRoot(), R.string.svc_snackbar_mileage, Snackbar.LENGTH_SHORT).show();
            binding.tvSvcMileage.requestFocus();
            return false;
        }

        if(binding.tvSvcPayment.getText().toString().equals("0")) {
            Snackbar.make(binding.getRoot(), R.string.svc_snackbar_cost, Snackbar.LENGTH_SHORT).show();

        }

        return true;
    }
}
