package com.silverback.carman.fragments;


import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
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
import com.silverback.carman.threads.ServiceCenterTask;
import com.silverback.carman.utils.Constants;
import com.silverback.carman.utils.FavoriteGeofenceHelper;
import com.silverback.carman.viewmodels.FragmentSharedModel;
import com.silverback.carman.viewmodels.LocationViewModel;
import com.silverback.carman.viewmodels.PagerAdapterViewModel;
import com.silverback.carman.viewmodels.ServiceCenterViewModel;
import com.silverback.carman.viewmodels.StationListViewModel;

import org.json.JSONArray;
import org.json.JSONException;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * A simple {@link Fragment} subclass.
 */
public class ServiceManagerFragment extends Fragment implements
        //View.OnClickListener,
        ExpServiceItemAdapter.OnParentFragmentListener {

    // Constants
    private static final LoggingHelper log = LoggingHelperFactory.create(ServiceManagerFragment.class);
    private static final int SVC_CENTER = 2;

    // Objects
    //private SparseArray<ServiceManagerDao.LatestServiceData> sparseServiceArray;
    private FragmentServiceManagerBinding binding;
    private SharedPreferences mSettings;
    private CarmanDatabase mDB;
    private FirebaseFirestore firestore;
    private Calendar calendar;

    private FragmentSharedModel fragmentModel;
    private PagerAdapterViewModel pagerAdapterModel;
    private LocationViewModel locationModel;
    private ServiceCenterViewModel svcCenterModel;
    private StationListViewModel stationModel;

    private ServiceCenterTask serviceCenterTask;
    private Location location;


    private FavoriteGeofenceHelper geofenceHelper;
    private ExpServiceItemAdapter mAdapter;
    private DecimalFormat df;
    private SimpleDateFormat sdf;
    private JSONArray jsonServiceArray;
    private Location svcLocation;
    private String svcAddress;
    private String svcCompany;

    // Fields
    private String distCode;
    private int itemPos;
    private int totalExpense;
    private boolean isGeofenceIntent; // check if this has been launched by Geofence.
    private boolean isSvcFavorite;
    private String userId;
    private String svcId;
    private String svcName;
    private String svcComment;
    private int svcPeriod;
    private String geoSvcName;
    private float svcRating;
    private long geoTime;
    private int category;
    private Location mPrevLocation;

    public ServiceManagerFragment() {
        // Required empty public constructor
    }

    //@SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // In case the activity is initiated by tabbing the notification, which sent the intent w/
        // action and extras for the geofance data.
        setGeofenceIntent();

        // userId will be used when svc_eval is prepared.
        /*
        if(getArguments() != null) {
            distCode = getArguments().getString("distCode");
            userId = getArguments().getString("userId");
        }
         */

        // Instantiate objects.
        mSettings = ((BaseActivity)requireActivity()).getSharedPreferernces();
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
        svcCenterModel = new ViewModelProvider(this).get(ServiceCenterViewModel.class);
        pagerAdapterModel = new ViewModelProvider(requireActivity()).get(PagerAdapterViewModel.class);
        locationModel = new ViewModelProvider(requireActivity()).get(LocationViewModel.class);
        stationModel = new ViewModelProvider(requireActivity()).get(StationListViewModel.class);


        // Attach an observer to fetch a current location from LocationTask, then initiate
        // StationListTask based on the value.
        /*
        locationModel.getLocation().observe(getActivity(), location -> {
            this.location = location;
            //serviceCenterTask = ThreadManager.startServiceCenterTask(getContext(), svcCenterModel, location);
        });
         */

        // Attach the listener which invokes the following callback methods when a location is added
        // to or removed from the favorite provider as well as geofence list.
        geofenceHelper.setGeofenceListener(new FavoriteGeofenceHelper.OnGeofenceListener() {
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
        });

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentServiceManagerBinding.inflate(inflater);

        long visitTime = (isGeofenceIntent)? geoTime : System.currentTimeMillis();
        binding.tvServiceDate.setText(sdf.format(visitTime));
        binding.tvSvcPayment.setText("0");
        binding.tvSvcMileage.setText(mSettings.getString(Constants.ODOMETER, "n/a"));
        binding.btnSvcFavorite.setBackgroundResource(R.drawable.btn_favorite);

        // Set event listeners.
        binding.btnRegisterService.setOnClickListener(v -> registerFavoriteServiceProvider());
        binding.btnSvcFavorite.setOnClickListener(v -> addServiceFavorite());

        // Fill in the form automatically with the data transferred from the PendingIntent of Geofence
        // only if the parent activity gets started by the notification and its category should be
        // Constants.SVC
        if(isGeofenceIntent && category == Constants.SVC) {
            binding.etServiceProvider.setText(geoSvcName);
            binding.etServiceProvider.setText(geoSvcName);
            binding.etServiceProvider.clearFocus();
            isSvcFavorite = true;

            binding.btnSvcFavorite.setBackgroundResource(R.drawable.btn_favorite_selected);
            binding.btnResetDatetime.setVisibility(View.GONE);
            binding.btnResetDatetime.setVisibility(View.GONE);
        }

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        try { createRecyclerServiceItemView(); }
        catch(JSONException e) { e.printStackTrace(); }

        // LiveData custom time from Date and Time picker DialogFragment
        fragmentModel.getCustomDateAndTime().observe(getViewLifecycleOwner(), calendar -> {
            this.calendar = calendar;
            binding.tvServiceDate.setText(sdf.format(calendar.getTimeInMillis()));
        });


        // Communcate w/ NumberPadFragment to put a number selected in the num pad into the textview
        // in this fragment.
        fragmentModel.getNumpadValue().observe(getViewLifecycleOwner(), data -> {
            final int viewId = data.keyAt(0);
            final int value = data.valueAt(0);
            if(viewId == R.id.tv_svc_mileage) {
                binding.tvSvcMileage.setText(df.format(value));
            } else if(viewId == R.id.tv_value_cost) {
                mAdapter.notifyItemChanged(itemPos, data);
                totalExpense += data.valueAt(0);
                binding.tvSvcPayment.setText(df.format(totalExpense));
            }
        });

        // Communicate b/w  RecyclerView.ViewHolder and MemoPadFragment
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


        // Communicate w/ RegisterDialogFragment, retrieving the eval and comment data and set or
        // update the data in Firestore.
        // Retrieving the evaluation and the comment, set or update the data with the passed id.
        fragmentModel.getServiceLocation().observe(getViewLifecycleOwner(), sparseArray -> {
            svcId = (String)sparseArray.get(RegisterDialogFragment.SVC_ID);
            svcLocation = (Location)sparseArray.get(RegisterDialogFragment.LOCATION);
            svcAddress = (String)sparseArray.get(RegisterDialogFragment.ADDRESS);
            svcCompany = (String)sparseArray.get(RegisterDialogFragment.COMPANY);
            svcRating = (Float)sparseArray.get(RegisterDialogFragment.RATING);
            svcComment = (String)sparseArray.get(RegisterDialogFragment.COMMENT);
            log.i("Service Locaiton: %s, %s, %s, %s, %s", svcId, svcLocation, svcAddress, svcRating, svcComment);

            uploadServiceEvaluation(svcId);

        });


    }

    @Override
    public void onResume() {
        super.onResume();
        // ******** MORE RESEARCH REQUIRED ********
        // Must define FragmentSharedModel.setCurrentFragment() in onCreate, not onActivityCreated()
        // because the value of fragmentSharedModel.getCurrentFragment() is retrieved in onCreateView()
        // in ExpensePagerFragment. Otherwise, an error occurs due to asyncronous lifecycle.
        fragmentModel.setCurrentFragment(this);
        //fragmentModel.getExpenseSvcFragment().setValue(this);

        // Update the time to the current time.
        binding.tvServiceDate.setText(sdf.format(System.currentTimeMillis()));

    }

    // Implement ExpServiceItemAdapter.OnParentFragmentListener to pop up NumberPadFragmnet and
    // intput the amount of expense in each service item.
    @Override
    public void setItemPosition(int position) {
        itemPos = position;
    }

    @Override
    public int getCurrentMileage() {
        return 0;
    }

    @Override
    public void subtractCost(int value) {
        totalExpense -= value;
        binding.tvSvcPayment.setText(df.format(totalExpense));
    }

    // Check if the parent activity gets started by the geofence intent and get extras.
    private void setGeofenceIntent(){
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

    private void createRecyclerServiceItemView() throws JSONException {
        binding.recyclerServiceItems.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerServiceItems.setHasFixedSize(true);

        String jsonServiceItems = mSettings.getString(Constants.SERVICE_ITEMS, null);
        jsonServiceArray = new JSONArray(jsonServiceItems);
        mAdapter = new ExpServiceItemAdapter(jsonServiceArray, svcPeriod, this);
        binding.recyclerServiceItems.setAdapter(mAdapter);

        for(int i = 0; i < jsonServiceArray.length(); i++) {
            final int pos = i;
            final String name = jsonServiceArray.optJSONObject(pos).getString("name");
            mDB.serviceManagerModel().loadServiceData(name).observe(getViewLifecycleOwner(), data -> {
                if(data != null) {
                    mAdapter.setServiceData(pos, data);
                    mAdapter.notifyItemChanged(pos, data);
                } else mAdapter.setServiceData(pos, null);
            });
        }
    }

    private void registerFavoriteServiceProvider() {
        svcName = binding.etServiceProvider.getText().toString();
        if(svcName.isEmpty()) {
            Snackbar.make(binding.getRoot(), R.string.svc_msg_empty_name, Snackbar.LENGTH_SHORT).show();
            return;
        }

        if(isSvcFavorite || svcLocation != null) {
            Snackbar.make(binding.getRoot(), "Already Registered", Snackbar.LENGTH_SHORT).show();
        } else {
            RegisterDialogFragment.newInstance(svcName, distCode).show(
                    Objects.requireNonNull(requireActivity()).getSupportFragmentManager(), null);
        }

    }


    // Register the service center with the favorite list and the geofence.
    @SuppressWarnings("ConstantConditions")
    private void addServiceFavorite() {
        // if(isGeofenceIntent) return;
        // Retrieve a service center from the favorite list, the value of which is sent via
        // fragmentSharedModel.getFavoriteName()
        if(TextUtils.isEmpty(binding.etServiceProvider.getText())) {
            String title = "Favorite Service Center";
            FavoriteListFragment.newInstance(title, Constants.SVC).show(getActivity().getSupportFragmentManager(), null);

        // Remove the center out of the favorite list and the geofence
        } else if(isSvcFavorite) {
            Snackbar snackbar = Snackbar.make(
                    binding.getRoot(), getString(R.string.svc_snackbar_alert_remove_favorite), Snackbar.LENGTH_SHORT);
            snackbar.setAction(R.string.popup_msg_confirm, view -> {
                geofenceHelper.removeFavoriteGeofence(svcName, svcId, Constants.SVC);
                //btnSvcFavorite.setBackgroundResource(R.drawable.btn_favorite);
            });

            snackbar.show();

            //firestore.collection("svc_eval").document(svcId).update("favorite_num", FieldValue.increment(-1));

        // Add the service center with the favorite list and geofence as far as it has been
        // already registered in RegisterDialogFragment.
        } else {
            if (TextUtils.isEmpty(svcId)) {
                InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(binding.etServiceProvider.getWindowToken(), 0);
                binding.etServiceProvider.clearFocus();
                Snackbar.make(binding.getRoot(), R.string.svc_msg_registration, Snackbar.LENGTH_SHORT).show();

            } else {
                // Check if the totla number of the service favorites are out of the max limit.
                final int placeholder = mDB.favoriteModel().countFavoriteNumber(Constants.SVC);
                if (placeholder == Constants.MAX_FAVORITE) {
                    Snackbar.make(binding.getRoot(), getString(R.string.exp_snackbar_favorite_limit), Snackbar.LENGTH_SHORT).show();
                } else {
                    // Query the data of a station from Firestore and pass the data to FavoriteGeofenceHelper
                    // for adding the favoirte list to the local db and Firestore as well.
                    firestore.collection("svc_center").document(svcId).get().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot snapshot = task.getResult();
                            if (snapshot != null && snapshot.exists()) {
                                //btnSvcFavorite.setBackgroundResource(R.drawable.btn_favorite_selected);
                                geofenceHelper.addFavoriteGeofence(snapshot, placeholder, Constants.SVC);
                            }
                        }
                    });

                }
            }
        }

    }

    // Invoked by OnOptions
    public LiveData<Integer> saveServiceData() {
        MutableLiveData<Integer> liveServiceTotal = new MutableLiveData<>();
        if(!doEmptyCheck()) liveServiceTotal.setValue(-1);

        //String dateFormat = getString(R.string.date_format_1);
        //long milliseconds = BaseActivity.parseDateTime(dateFormat, binding.tvServiceDate.getText().toString());
        //log.i("service data saved: %s", milliseconds);
        int mileage = 0;

        try {
            mileage = Objects.requireNonNull(df.parse(binding.tvSvcMileage.getText().toString())).intValue();
        } catch(ParseException e) {
            log.e("ParseException: %s", e.getMessage());
        }

        ExpenseBaseEntity basicEntity = new ExpenseBaseEntity();
        ServiceManagerEntity serviceEntity = new ServiceManagerEntity();
        ServicedItemEntity checkedItem;
        List<ServicedItemEntity> itemEntityList = new ArrayList<>();

        basicEntity.dateTime = calendar.getTimeInMillis();
        basicEntity.mileage = mileage;
        basicEntity.category = Constants.SVC;
        basicEntity.totalExpense = totalExpense;

        liveServiceTotal.setValue(totalExpense);

        serviceEntity.serviceCenter = binding.etServiceProvider.getText().toString();
        serviceEntity.serviceAddrs = "seoul, korea";

        for(int i = 0; i < mAdapter.arrCheckedState.length; i++) {
            if(mAdapter.arrCheckedState[i]) {
                checkedItem = new ServicedItemEntity();
                checkedItem.itemName = jsonServiceArray.optJSONObject(i).optString("name");
                //checkedItem.itemName = serviceItemList.get(i);
                checkedItem.itemPrice = mAdapter.arrItemCost[i];
                checkedItem.itemMemo = mAdapter.arrItemMemo[i];
                log.i("Serviced Item: %s", checkedItem.itemName);
                itemEntityList.add(checkedItem);
            }
        }

        // Insert data into both ExpenseBaseEntity and ServiceManagerEntity at the same time
        // using @Transaction in ServiceManagerDao.
        int rowId = mDB.serviceManagerModel().insertAll(basicEntity, serviceEntity, itemEntityList);
        if(rowId > 0) {
            mSettings.edit().putString(Constants.ODOMETER, binding.tvSvcMileage.getText().toString()).apply();
            Toast.makeText(getActivity(), getString(R.string.toast_save_success), Toast.LENGTH_SHORT).show();

        }

        return liveServiceTotal;
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

    private void uploadServiceEvaluation(String svcId) {
        if(svcRating > 0) {
            Map<String, Object> ratingData = new HashMap<>();
            ratingData.put("eval_num", FieldValue.increment(1));
            ratingData.put("eval_sum", FieldValue.increment(svcRating));

            DocumentReference docRef = firestore.collection("svc_eval").document(svcId);
            docRef.get().addOnSuccessListener(snapshot -> {
                if(snapshot.exists() && snapshot.get("eval_num") != null) {
                    log.i("update rating");
                    docRef.update(ratingData);

                } else {
                    log.i("set rating if no rating field exists");
                    docRef.set(ratingData);
                }
            });
        }

        if(!svcComment.isEmpty()) {
            Map<String, Object> commentData = new HashMap<>();
            commentData.put("timestamp", FieldValue.serverTimestamp());
            commentData.put("name", mSettings.getString(Constants.USER_NAME, null));
            commentData.put("comments", svcComment);
            commentData.put("rating", svcRating);

            firestore.collection("svc_eval").document(svcId).collection("comments").add(commentData)
                    .addOnCompleteListener(task -> {
                        if(task.isSuccessful()) {
                            log.e("Commments successfully uploaded");
                            //isCommentUploaded = true;
                        } else {
                            log.e("Comments upload failed: %s", task.getException());
                        }
                    });
        }
    }
}
