package com.silverback.carman2.fragments;


import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.silverback.carman2.BaseActivity;
import com.silverback.carman2.ExpenseActivity;
import com.silverback.carman2.R;
import com.silverback.carman2.adapters.ExpServiceItemAdapter;
import com.silverback.carman2.database.CarmanDatabase;
import com.silverback.carman2.database.ExpenseBaseEntity;
import com.silverback.carman2.database.ServiceManagerEntity;
import com.silverback.carman2.database.ServicedItemEntity;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.viewmodels.FragmentSharedModel;
import com.silverback.carman2.viewmodels.LocationViewModel;
import com.silverback.carman2.viewmodels.PagerAdapterViewModel;
import com.silverback.carman2.viewmodels.ServiceCenterViewModel;
import com.silverback.carman2.threads.ServiceCenterTask;
import com.silverback.carman2.threads.ThreadManager;
import com.silverback.carman2.utils.Constants;
import com.silverback.carman2.utils.FavoriteGeofenceHelper;

import org.json.JSONArray;
import org.json.JSONException;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A simple {@link Fragment} subclass.
 */
public class ServiceManagerFragment extends Fragment implements
        View.OnClickListener, ExpServiceItemAdapter.OnParentFragmentListener {

    // Constants
    private static final LoggingHelper log = LoggingHelperFactory.create(ServiceManagerFragment.class);
    private static final int SVC_CENTER = 2;

    // Objects
    //private SparseArray<ServiceManagerDao.LatestServiceData> sparseServiceArray;
    private SharedPreferences mSettings;
    private CarmanDatabase mDB;
    private FirebaseFirestore firestore;
    private FragmentSharedModel fragmentModel;
    private PagerAdapterViewModel pagerAdapterModel;
    private LocationViewModel locationModel;
    private ServiceCenterViewModel svcCenterModel;
    private ServiceCenterTask serviceCenterTask;
    private Location location;
    private NumberPadFragment numPad;
    private MemoPadFragment memoPad;

    private FavoriteGeofenceHelper geofenceHelper;
    private ExpServiceItemAdapter mAdapter;
    private DecimalFormat df;
    private JSONArray jsonServiceArray;
    private Location svcLocation;
    private String svcAddress;
    private String svcCompany;

    // UIs
    private RelativeLayout parentLayout;
    private RecyclerView recyclerServiceItems;
    private ProgressBar progbar;
    private EditText etServiceName;
    private TextView tvDate, tvMileage, tvTotalCost;
    private ImageButton btnSvcFavorite;
    private TextView targetView;


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

    public ServiceManagerFragment() {
        // Required empty public constructor
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // In case the activity is initiated by tabbing the notification, which sent the intent w/
        // action and extras for the geofance data.
        String action = getActivity().getIntent().getAction();
        if(action != null && action.equals(Constants.NOTI_GEOFENCE)) {
            if(getActivity().getIntent().getAction().equals(Constants.NOTI_GEOFENCE)) {
                isGeofenceIntent = true;
                geoSvcName = getActivity().getIntent().getStringExtra(Constants.GEO_NAME);
                geoTime = getActivity().getIntent().getLongExtra(Constants.GEO_TIME, -1);
                category = getActivity().getIntent().getIntExtra(Constants.GEO_CATEGORY, -1);
            }
        }

        // userId will be used when svc_eval is prepared.
        if(getArguments() != null) {
            distCode = getArguments().getString("distCode");
            userId = getArguments().getString("userId");
        }

        // Instantiate objects.
        mSettings = ((BaseActivity)getActivity()).getSharedPreferernces();
        mDB = CarmanDatabase.getDatabaseInstance(getActivity().getApplicationContext());
        firestore = FirebaseFirestore.getInstance();

        // Get the service periond unit from SharedPreferences and pass it to the adapter as int type.
        String period = mSettings.getString(Constants.SERVICE_PERIOD, getString(R.string.pref_svc_period_mileage));
        if(period.equals(getString(R.string.pref_svc_period_mileage))) svcPeriod = 0;
        else if(period.equals(getString(R.string.pref_svc_period_month))) svcPeriod = 1;

        fragmentModel = new ViewModelProvider(requireActivity()).get(FragmentSharedModel.class);
        svcCenterModel = new ViewModelProvider(this).get(ServiceCenterViewModel.class);
        pagerAdapterModel = ((ExpenseActivity)getActivity()).getPagerModel();
        locationModel = ((ExpenseActivity) getActivity()).getLocationViewModel();


        if(geofenceHelper == null) geofenceHelper = new FavoriteGeofenceHelper(getContext());
        df = BaseActivity.getDecimalFormatInstance();
        numPad = new NumberPadFragment();
        memoPad = new MemoPadFragment();

        // The service items are saved in SharedPreferences as JSONString type, which should be
        // converted to JSONArray. The service period retrieved from SharedPrefernces is passed to
        // the adapter as well.
        /*
        try {
            String json = mSettings.getString(Constants.SERVICE_ITEMS, null);
            jsonServiceArray = new JSONArray(json);
            svcPeriod = mSettings.getString(Constants.SERVICE_PERIOD, getString(R.string.pref_svc_period_mileage));
            mAdapter = new ExpServiceItemAdapter(jsonServiceArray, svcPeriod, this);
        } catch(JSONException e) {e.printStackTrace();}

        for(int i = 0; i < jsonServiceArray.length(); i++) {
            try {
                final int pos = i;
                final String name = jsonServiceArray.optJSONObject(pos).getString("name");
                mDB.serviceManagerModel().loadServiceData(name).observe(this, data -> {
                    if(data != null) {
                        mAdapter.setServiceData(pos, data);
                        mAdapter.notifyItemChanged(pos, data);
                    } else {
                        log.i("No service data: %s, %s", pos, name);
                        mAdapter.setServiceData(pos, null);
                    }
                });
            } catch(JSONException e) {
                //log.e("JSONException: %s", e.getMessage());
                e.printStackTrace();
            }
        }

         */

        // Attach the listener for callback methods invoked by addGeofence or removeGeofence
        geofenceHelper.setGeofenceListener(new FavoriteGeofenceHelper.OnGeofenceListener() {
            @Override
            public void notifyAddGeofenceCompleted(int placeholder) {
                isSvcFavorite = true;
                Snackbar.make(parentLayout, getString(R.string.svc_msg_add_favorite), Snackbar.LENGTH_SHORT).show();
            }
            @Override
            public void notifyRemoveGeofenceCompleted(int placeholder) {
                isSvcFavorite = false;
                Snackbar.make(parentLayout, R.string.svc_snackbar_favorite_removed, Snackbar.LENGTH_SHORT).show();
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

        View localView = inflater.inflate(R.layout.fragment_service_manager, container, false);

        parentLayout = localView.findViewById(R.id.constraintLayout_root);
        recyclerServiceItems = localView.findViewById(R.id.recycler_service);
        tvDate = localView.findViewById(R.id.tv_service_date);
        etServiceName = localView.findViewById(R.id.et_service_provider);
        tvMileage = localView.findViewById(R.id.tv_mileage);
        Button btnDate = localView.findViewById(R.id.btn_svc_date);
        Button btnReg = localView.findViewById(R.id.btn_register);
        btnSvcFavorite = localView.findViewById(R.id.btn_svc_favorite);
        tvTotalCost = localView.findViewById(R.id.tv_gas_payment);
        TextView tvPeriod = localView.findViewById(R.id.tv_period);

        tvMileage.setOnClickListener(this);
        btnDate.setOnClickListener(this);
        btnReg.setOnClickListener(this);
        btnSvcFavorite.setOnClickListener(view -> addServiceFavorite());

        svcName = etServiceName.getText().toString();

        long visitTime = (isGeofenceIntent)? geoTime : System.currentTimeMillis();
        String date = BaseActivity.formatMilliseconds(getString(R.string.date_format_1), visitTime);
        tvDate.setText(date);
        tvTotalCost.setText("0");
        // Set the mileage value retrieved from SharedPreferences first
        tvMileage.setText(mSettings.getString(Constants.ODOMETER, ""));
        tvPeriod.setText(mSettings.getString(Constants.SERVICE_PERIOD, getString(R.string.pref_svc_period_month)));
        btnSvcFavorite.setBackgroundResource(R.drawable.btn_favorite);

        recyclerServiceItems.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerServiceItems.setHasFixedSize(true);
        //recyclerServiceItems.setAdapter(mAdapter);

        // Fill in the form automatically with the data transferred from the PendingIntent of Geofence
        // only if the parent activity gets started by the notification and its category should be
        // Constants.SVC
        if(isGeofenceIntent && category == Constants.SVC) {
            log.i("Handling isGeofenceIntent");
            etServiceName.setText(geoSvcName);
            etServiceName.clearFocus();
            isSvcFavorite = true;

            btnSvcFavorite.setBackgroundResource(R.drawable.btn_favorite_selected);
            btnDate.setVisibility(View.GONE);
        }

        return localView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Attach an observer to fetch a current location from LocationTask, then initiate
        // StationListTask based on the value.
        locationModel.getLocation().observe(requireActivity(), location -> {
            this.location = location;
            serviceCenterTask = ThreadManager.startServiceCenterTask(getContext(), svcCenterModel, location);
        });

        // ExpenseTabPagerTask initiated in the parent activity runs ExpenseSvcItemsRunnable which
        // notifies this of receiving the livedata JSONArray containing the service items.
        pagerAdapterModel.getJsonServiceArray().observe(requireActivity(), jsonServiceArray -> {
            this.jsonServiceArray = jsonServiceArray;
            mAdapter = new ExpServiceItemAdapter(jsonServiceArray, svcPeriod, this);
            if(recyclerServiceItems != null) recyclerServiceItems.setAdapter(mAdapter);

            // Query the latest service history from ServiceManagerEntity and update the adapter, making
            // partial bindings to RecycerView.
            for(int i = 0; i < jsonServiceArray.length(); i++) {
                try {
                    final int pos = i;
                    final String name = jsonServiceArray.optJSONObject(pos).getString("name");
                    mDB.serviceManagerModel().loadServiceData(name).observe(getViewLifecycleOwner(), data -> {
                        if(data != null) {
                            mAdapter.setServiceData(pos, data);
                            mAdapter.notifyItemChanged(pos, data);
                        } else mAdapter.setServiceData(pos, null);
                    });
                } catch(JSONException e) { e.printStackTrace(); }
            }
        });

        // Codes should be added in accordance to the progress of the service centre database as like
        // in the gas station db.
        svcCenterModel.getCurrentSVC().observe(getViewLifecycleOwner(), svcData -> {
            //checkSvcFavorite(svcData, Constants.SVC);
        });

        // Communcate w/ NumberPadFragment
        fragmentModel.getSelectedValue().observe(getViewLifecycleOwner(), data -> {
            final int viewId = data.keyAt(0);
            final int value = data.valueAt(0);
            switch(viewId) {
                case R.id.tv_mileage:
                    tvMileage.setText(df.format(value));
                    break;

                case R.id.tv_value_cost:
                    mAdapter.notifyItemChanged(itemPos, data);
                    totalExpense += data.valueAt(0);
                    tvTotalCost.setText(df.format(totalExpense));
                    break;
            }
        });

        // Communicate b/w RecyclerView.ViewHolder and MemoPadFragment
        fragmentModel.getSelectedMenu().observe(getViewLifecycleOwner(),
                data -> mAdapter.notifyItemChanged(itemPos, data));

        // Get the params for removeGeofence() which are passed from FavroiteListFragment
        fragmentModel.getFavoriteSvcEntity().observe(getViewLifecycleOwner(), entity -> {
            svcName = entity.providerName;
            svcId = entity.providerId;
            etServiceName.setText(svcName);
            btnSvcFavorite.setBackgroundResource(R.drawable.btn_favorite_selected);
            isSvcFavorite = true;
        });


        // Communicate w/ RegisterDialogFragment, retrieving the eval and comment data and set or
        // update the data in Firestore.
        // Retrieving the evaluation and the comment, set or update the data with the passed id.
        fragmentModel.getServiceLocation().observe(requireActivity(), sparseArray -> {
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
        log.i("servicemanagerfragment");
        fragmentModel.setCurrentFragment(this);
        //fragmentModel.getExpenseSvcFragment().setValue(this);

    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onClick(View v) {
        // Indicate which TextView is clicked, then put a value retrieved from InputNumberPad
        // via FragmentViewModel in the textview.
        switch(v.getId()) {
            case R.id.tv_mileage:
                Bundle args = new Bundle();
                //args.putString("itemLabel", getString(R.string.svc_label_mileage));
                args.putString("initValue", tvMileage.getText().toString());
                args.putInt("viewId", v.getId());
                numPad.setArguments(args);
                if(getActivity().getSupportFragmentManager() != null)
                    numPad.show(getActivity().getSupportFragmentManager(), null);
                break;

            case R.id.btn_register:
                svcName = etServiceName.getText().toString();
                if(etServiceName.getText().toString().isEmpty()) {
                    Snackbar.make(parentLayout, R.string.svc_msg_empty_name, Snackbar.LENGTH_SHORT).show();
                    return;
                }

                if(isSvcFavorite || svcLocation != null) {
                    Snackbar.make(parentLayout, "Already Registered", Snackbar.LENGTH_SHORT).show();
                    return;
                } else {
                    RegisterDialogFragment.newInstance(svcName, distCode)
                            .show(getActivity().getSupportFragmentManager(), null);
                }

                break;

            case R.id.btn_svc_date:
                break;

        }

    }

    // ExpServiceItemAdapter.OnParentFragmentListener invokes the following 4 methods
    // to pop up NumberPadFragment and input the amount of expense in a service item.
    @Override
    public void inputItemCost(String label, TextView targetView, int position) {
        itemPos = position;

        Bundle args = new Bundle();
        args.putString("itemLabel", label);
        args.putString("initValue", targetView.getText().toString());
        args.putInt("viewId", targetView.getId());
        numPad.setArguments(args);

        if(getActivity() != null) numPad.show(getActivity().getSupportFragmentManager(), null);

    }

    @Override
    public void inputItemMemo(String title, TextView targetView, int position) {
        log.i("Item Info: %s %s %s", title, targetView.getId(), position);
        itemPos = position;

        Bundle args = new Bundle();
        args.putString("itemLabel", title);
        args.putInt("viewId", targetView.getId());
        memoPad.setArguments(args);

        if(getActivity() != null) memoPad.show(getActivity().getSupportFragmentManager(), null);
    }

    @Override
    public void subtractCost(int value) {
        log.i("Calculate Total Cost");
        totalExpense -= value;
        tvTotalCost.setText(df.format(totalExpense));
    }


    @Override
    public int getCurrentMileage() {
        try {
            return df.parse(tvMileage.getText().toString()).intValue();
        } catch(ParseException e) {
            log.e("ParseException: %s", e.getMessage());
        }

        return -1;
    }

    // Register the service center with the favorite list and the geofence.
    @SuppressWarnings("ConstantConditions")
    private void addServiceFavorite() {

        //if(isGeofenceIntent) return;

        // Retrieve a service center from the favorite list, the value of which is sent via
        // fragmentSharedModel.getFavoriteName()
        if(TextUtils.isEmpty(etServiceName.getText())) {
            String title = "Favorite Service Center";
            FavoriteListFragment.newInstance(title, Constants.SVC).show(getActivity().getSupportFragmentManager(), null);

        // Remove the center out of the favorite list and the geofence
        } else if(isSvcFavorite) {
            Snackbar snackbar = Snackbar.make(
                    parentLayout, getString(R.string.svc_snackbar_alert_remove_favorite), Snackbar.LENGTH_SHORT);
            snackbar.setAction(R.string.popup_msg_confirm, view -> {
                geofenceHelper.removeFavoriteGeofence(svcName, svcId, Constants.SVC);
                btnSvcFavorite.setBackgroundResource(R.drawable.btn_favorite);
            });

            snackbar.show();

            //firestore.collection("svc_eval").document(svcId).update("favorite_num", FieldValue.increment(-1));

        // Add the service center with the favorite list and geofence as far as it has been
        // already registered in RegisterDialogFragment.
        } else {
            if (TextUtils.isEmpty(svcId)) {
                InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(etServiceName.getWindowToken(), 0);
                etServiceName.clearFocus();
                Snackbar.make(parentLayout, R.string.svc_msg_registration, Snackbar.LENGTH_SHORT).show();

            } else {
                // Check if the totla number of the service favorites are out of the max limit.
                final int placeholder = mDB.favoriteModel().countFavoriteNumber(Constants.SVC);
                if (placeholder == Constants.MAX_FAVORITE) {
                    Snackbar.make(parentLayout, getString(R.string.exp_snackbar_favorite_limit), Snackbar.LENGTH_SHORT).show();
                } else {
                    // Query the data of a station from Firestore and pass the data to FavoriteGeofenceHelper
                    // for adding the favoirte list to the local db and Firestore as well.
                    firestore.collection("svc_center").document(svcId).get().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot snapshot = task.getResult();
                            if (snapshot != null && snapshot.exists()) {
                                btnSvcFavorite.setBackgroundResource(R.drawable.btn_favorite_selected);
                                geofenceHelper.addFavoriteGeofence(snapshot, placeholder, Constants.SVC);
                            }
                        }
                    });

                }
            }
        }

    }

    // Invoked by OnOptions
    public boolean saveServiceData() {

        if(!doEmptyCheck()) return false;

        String dateFormat = getString(R.string.date_format_1);
        long milliseconds = BaseActivity.parseDateTime(dateFormat, tvDate.getText().toString());
        log.i("service data saved: %s", milliseconds);
        int mileage;

        try {
            mileage = df.parse(tvMileage.getText().toString()).intValue();
        } catch(ParseException e) {
            log.e("ParseException: %s", e.getMessage());
            return false;
        }

        ExpenseBaseEntity basicEntity = new ExpenseBaseEntity();
        ServiceManagerEntity serviceEntity = new ServiceManagerEntity();
        ServicedItemEntity checkedItem;
        List<ServicedItemEntity> itemEntityList = new ArrayList<>();

        basicEntity.dateTime = milliseconds;
        basicEntity.mileage = mileage;
        basicEntity.category = Constants.SVC;
        basicEntity.totalExpense = totalExpense;

        serviceEntity.serviceCenter = etServiceName.getText().toString();
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
            mSettings.edit().putString(Constants.ODOMETER, tvMileage.getText().toString()).apply();
            Toast.makeText(getActivity(), getString(R.string.toast_save_success), Toast.LENGTH_SHORT).show();
            return true;

        } else return false;

    }

    private boolean doEmptyCheck() {

        if(TextUtils.isEmpty(etServiceName.getText())) {
            String msg = getString(R.string.svc_snackbar_stnname);
            Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
            etServiceName.requestFocus();
            return false;
        }

        if(TextUtils.isEmpty(tvMileage.getText())) {
            Snackbar.make(parentLayout, R.string.svc_snackbar_mileage, Snackbar.LENGTH_SHORT).show();
            tvMileage.requestFocus();
            return false;
        }

        if(tvTotalCost.getText().toString().equals("0")) {
            Snackbar.make(parentLayout, R.string.svc_snackbar_cost, Snackbar.LENGTH_SHORT).show();

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
