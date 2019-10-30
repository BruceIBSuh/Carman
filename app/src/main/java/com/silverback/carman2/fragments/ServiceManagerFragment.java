package com.silverback.carman2.fragments;


import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
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
import com.silverback.carman2.database.ExpenseBaseEntity;
import com.silverback.carman2.database.CarmanDatabase;
import com.silverback.carman2.database.ServiceManagerEntity;
import com.silverback.carman2.database.ServicedItemEntity;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.LocationViewModel;
import com.silverback.carman2.models.PagerAdapterViewModel;
import com.silverback.carman2.models.ServiceCenterViewModel;
import com.silverback.carman2.threads.ServiceCenterTask;
import com.silverback.carman2.threads.ThreadManager;
import com.silverback.carman2.utils.Constants;
import com.silverback.carman2.models.FragmentSharedModel;
import com.silverback.carman2.utils.FavoriteGeofenceHelper;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
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
    private FragmentSharedModel fragmentSharedModel;
    private PagerAdapterViewModel adapterModel;
    private LocationViewModel locationModel;
    private ServiceCenterViewModel svcCenterModel;
    private ServiceCenterTask serviceCenterTask;
    private Location location, currentLocation;
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
    private RelativeLayout relativeLayout;
    private RecyclerView recyclerServiceItems;
    private ProgressBar progbar;
    private EditText etServiceName;
    private TextView tvDate, tvMileage, tvTotalCost;
    private ImageButton btnFavorite;


    // Fields
    private String distCode;
    private int itemPos;
    private int totalExpense;
    private boolean isGeofenceIntent; // check if this has been launched by Geofence.
    private boolean isSvcFavorite;

    private String svcId;
    private float svcRating;
    private String svcName;
    private String svcComment;
    private String userId;

    public ServiceManagerFragment() {
        // Required empty public constructor
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(getArguments() != null) {
            distCode = getArguments().getString("distCode");
            userId = getArguments().getString("userId");

            /*
            log.i("District Code: %s", distCode);
            try {
                JSONArray jsonArray = new JSONArray(getArguments().getString("district"));
                distCode = jsonArray.optString(2);

            } catch(JSONException e) {

            }

             */
        }

        // Instantiate objects.
        mSettings = ((ExpenseActivity)getActivity()).getSettings();
        mDB = CarmanDatabase.getDatabaseInstance(getActivity().getApplicationContext());
        firestore = FirebaseFirestore.getInstance();

        fragmentSharedModel = ((ExpenseActivity)getActivity()).getFragmentSharedModel();
        adapterModel = ((ExpenseActivity)getActivity()).getPagerAdapterViewModel();
        locationModel = ((ExpenseActivity) getActivity()).getLocationViewModel();
        svcCenterModel = ViewModelProviders.of(this).get(ServiceCenterViewModel.class);


        if(geofenceHelper == null) geofenceHelper = new FavoriteGeofenceHelper(getContext());
        df = BaseActivity.getDecimalFormatInstance();
        numPad = new NumberPadFragment();
        memoPad = new MemoPadFragment();

        // The service item data is saved in SharedPreferences as String type, which should be
        // converted to JSONArray.

        try {
            String json = mSettings.getString(Constants.SERVICE_ITEMS, null);
            jsonServiceArray = new JSONArray(json);
            mAdapter = new ExpServiceItemAdapter(jsonServiceArray, this);
        } catch(JSONException e) {
            log.e("JSONException: %s", e.getMessage());
        }

        for(int i = 0; i < jsonServiceArray.length(); i++) {
            try {
                final int pos = i;
                final String name = jsonServiceArray.optJSONObject(pos).getString("name");
                mDB.serviceManagerModel().loadServiceData(name).observe(this, data -> {
                    if(data != null) {
                        log.i("Service Data: %s, %s", pos, data.itemName);
                        mAdapter.setServiceData(pos, data);
                        mAdapter.notifyItemChanged(pos, data);
                    } else {
                        log.i("No service data: %s, %s", pos, name);
                        mAdapter.setServiceData(pos, null);
                    }
                });
            } catch(JSONException e) {
                log.e("JSONException: %s", e.getMessage());
            }
        }



        // Attach the listener for callback methods invoked by addGeofence or removeGeofence
        geofenceHelper.setGeofenceListener(new FavoriteGeofenceHelper.OnGeofenceListener() {
            @Override
            public void notifyAddGeofenceCompleted() {
                isSvcFavorite = !isSvcFavorite;
                Snackbar.make(relativeLayout, R.string.svc_msg_add_favorite, Snackbar.LENGTH_SHORT).show();

                // Add a new geofence to Geofence list saved in SharedPreferences
                // as type of JSONString in order to reload in GeofenceResetService when rebooting.
            }

            @Override
            public void notifyRemoveGeofenceCompleted() {
                isSvcFavorite = !isSvcFavorite;
                Snackbar.make(relativeLayout, "Successfully removed", Snackbar.LENGTH_SHORT).show();
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
        View boxview = localView.findViewById(R.id.view_boxing);
        log.i("BoxView height: %s %s", boxview.getHeight(), boxview.getMeasuredHeight());

        progbar = localView.findViewById(R.id.pb_service_items);
        progbar.setVisibility(View.VISIBLE);

        relativeLayout = localView.findViewById(R.id.rl_service);
        recyclerServiceItems = localView.findViewById(R.id.recycler_service);
        tvDate = localView.findViewById(R.id.tv_service_date);
        etServiceName = localView.findViewById(R.id.et_service_provider);
        tvMileage = localView.findViewById(R.id.tv_service_mileage);
        Button btnDate = localView.findViewById(R.id.btn_date);
        Button btnReg = localView.findViewById(R.id.btn_register);
        btnFavorite = localView.findViewById(R.id.imgbtn_favorite);
        tvTotalCost = localView.findViewById(R.id.tv_total_cost);

        tvMileage.setOnClickListener(this);
        btnDate.setOnClickListener(this);
        btnReg.setOnClickListener(this);
        btnFavorite.setOnClickListener(view -> addServiceFavorite());

        svcName = etServiceName.getText().toString();
        String date = BaseActivity.formatMilliseconds(getString(R.string.date_format_1), System.currentTimeMillis());
        tvDate.setText(date);
        tvTotalCost.setText("0");
        // Set the mileage value retrieved from SharedPreferences first
        tvMileage.setText(mSettings.getString(Constants.ODOMETER, ""));
        btnFavorite.setBackgroundResource(R.drawable.btn_favorite);

        recyclerServiceItems.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerServiceItems.setHasFixedSize(true);
        //recyclerServiceItems.setAdapter(mAdapter);

        return localView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);


        // Attach an observer to fetch a current location from LocationTask, then initiate
        // StationListTask based on the value.
        locationModel.getLocation().observe(this, location -> {
            log.i("Service Location: %s", location);
            this.location = location;
            serviceCenterTask = ThreadManager.startServiceCenterTask(getContext(), svcCenterModel, location);

        });

        // Get the dataset of the item list using ServiceRecyclerTask, which intends to manage the
        // loadweight of the memory
        adapterModel.getJsonServiceArray().observe(this, jsonServiceArray -> {
            this.jsonServiceArray = jsonServiceArray;
            mAdapter = new ExpServiceItemAdapter(jsonServiceArray, this);
            if(recyclerServiceItems != null) recyclerServiceItems.setAdapter(mAdapter);
            progbar.setVisibility(View.GONE);

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
                    log.e("JSONException: %s", e.getMessage());
                }
            }
        });

        // Communcate w/ NumberPadFragment
        fragmentSharedModel.getSelectedValue().observe(this, data -> {
            final int viewId = data.keyAt(0);
            final int value = data.valueAt(0);
            switch(viewId) {
                case R.id.tv_service_mileage:
                    //targetView = localView.findViewById(viewId);
                    //targetView.setText(df.format(value));
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
        fragmentSharedModel.getSelectedMenu().observe(this, data -> mAdapter.notifyItemChanged(itemPos, data));

        // Fetch the service name selected from FavoriteListFragment
        fragmentSharedModel.getFavoriteSvcEntity().observe(this, entity -> {
            etServiceName.setText(entity.providerName);
            btnFavorite.setBackgroundResource(R.drawable.btn_favorite_selected);
            isSvcFavorite = true;
        });


        // Communicate w/ RegisterDialogFragment, retrieving the eval and comment data and set or
        // update the data in Firestore.
        // Retrieving the evaluation and the comment, set or update the data with the passed id.
        fragmentSharedModel.getServiceLocation().observe(this, sparseArray -> {
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
        // Must define FragmentSharedModel.setCurrentFragment() in onCreate , not onActivityCreated()
        // because the value of ragmentSharedModel.getCurrentFragment() is retrieved in onCreateView()
        // in ExpensePagerFragment. Otherwise, an error occurs due to asyncronous lifecycle.
        fragmentSharedModel.setCurrentFragment(this);


    }



    @SuppressWarnings("ConstantConditions")
    @Override
    public void onClick(View v) {
        // Indicate which TextView is clicked, then put a value retrieved from InputNumberPad
        // via FragmentViewModel in the textview.
        switch(v.getId()) {
            case R.id.tv_service_mileage:
                Bundle args = new Bundle();
                args.putString("title", getString(R.string.svc_label_mileage));
                args.putString("initValue", tvMileage.getText().toString());
                args.putInt("viewId", v.getId());
                numPad.setArguments(args);
                if(getFragmentManager() != null) numPad.show(getFragmentManager(), null);
                break;

            case R.id.btn_register:
                svcName = etServiceName.getText().toString();
                if(etServiceName.getText().toString().isEmpty()) {
                    Snackbar.make(relativeLayout, R.string.svc_msg_empty_name, Snackbar.LENGTH_SHORT).show();
                    return;
                }

                if(isSvcFavorite || svcLocation != null) {
                    Snackbar.make(relativeLayout, "Already Registered", Snackbar.LENGTH_SHORT).show();
                    return;
                } else {
                    RegisterDialogFragment.newInstance(svcName, distCode).show(getFragmentManager(), null);
                }

                break;

            case R.id.btn_date:
                break;

        }

    }

    // ExpServiceItemAdapter.OnParentFragmentListener invokes the following 4 methods
    // to pop up NumberPadFragment and input the amount of expense in a service item.
    @Override
    public void inputItemCost(String title, TextView targetView, int position) {
        itemPos = position;

        Bundle args = new Bundle();
        args.putString("title", title);
        args.putString("initValue", targetView.getText().toString());
        args.putInt("viewId", targetView.getId());
        numPad.setArguments(args);

        if(getFragmentManager() != null) numPad.show(getFragmentManager(), null);

    }

    @Override
    public void inputItemMemo(String title, TextView targetView, int position) {
        log.i("Item Info: %s %s %s", title, targetView.getId(), position);
        itemPos = position;

        Bundle args = new Bundle();
        args.putString("title", title);
        args.putInt("viewId", targetView.getId());
        memoPad.setArguments(args);

        if(getFragmentManager() != null) memoPad.show(getFragmentManager(), null);
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

    // Query Favorite with the fetched station name or station id to tell whether the station
    // has registered with Favorite, and the result is notified as a LiveData.
    private void checkSvcFavorite(String name, int category) {
        mDB.favoriteModel().findFavoriteSvcName(name, category).observe(this, favorite -> {
            if (TextUtils.isEmpty(favorite)) {
                isSvcFavorite = false;
                btnFavorite.setBackgroundResource(R.drawable.btn_favorite);
            } else {
                isSvcFavorite = true;
                btnFavorite.setBackgroundResource(R.drawable.btn_favorite_selected);
            }
        });
    }

    // Register the service center with the favorite list and the geofence.
    private void addServiceFavorite() {

        if(isGeofenceIntent || getFragmentManager() == null) return;

        // Retrieve a service center from the favorite list, the value of which is sent via
        // fragmentSharedModel.getFavoriteName()
        if(TextUtils.isEmpty(etServiceName.getText())) {
            String title = "Favorite Service Center";
            FavoriteListFragment.newInstance(title, Constants.SVC).show(getFragmentManager(), null);

        // Remove the center out of the favorite list and the geofence
        } else if(isSvcFavorite) {
            /*
            String title = getString(R.string.svc_register_title);
            String msg = "Your're about to remove the service center out of the favorite";
            AlertDialogFragment alertFragment = AlertDialogFragment.newInstance("Alert", msg, Constants.SVC);
            if(getFragmentManager() != null) alertFragment.show(getFragmentManager(), null);
            */
            Snackbar.make(relativeLayout, getString(R.string.gas_snackbar_alert_remove_favorite), Snackbar.LENGTH_LONG).show();
            geofenceHelper.removeFavoriteGeofence(userId, svcName, svcId);
            btnFavorite.setBackgroundResource(R.drawable.btn_favorite);
            isSvcFavorite = false;

            DocumentReference favorite = firestore.collection("svc_eval").document(svcId);
            favorite.get().addOnSuccessListener(snapshot -> {
                if((int)snapshot.get("favorite_num") > 0)
                    favorite.update("favorite_num", "favorite_num", FieldValue.increment(-1));
            });

        // Add the service center with the favorite list and geofence as far as it has been
        // already registered in RegisterDialogFragment.
        } else if(svcId != null){
            // Check if the totla number of the service favorites are out of the max limit.
            final int placeholder = mDB.favoriteModel().countFavoriteNumber(Constants.SVC);
            if(placeholder == Constants.MAX_FAVORITE) {
                Snackbar.make(relativeLayout, getString(R.string.exp_snackbar_favorite_limit), Snackbar.LENGTH_SHORT).show();
            } else {
                // Query the data of a station from Firestore
                firestore.collection("svc_center").document(svcId).get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot snapshot = task.getResult();
                        if (snapshot != null && snapshot.exists()) {
                            btnFavorite.setBackgroundResource(R.drawable.btn_favorite_selected);
                            geofenceHelper.addFavoriteGeofence(userId, snapshot, placeholder, SVC_CENTER);


                        }
                    }
                });

                // Increase the number of the favorite registration.
                firestore.collection("svc_eval").document(svcId).get().addOnCompleteListener(task -> {
                    if(task.isSuccessful()) {
                        DocumentSnapshot snapshot = task.getResult();
                        if(snapshot != null && snapshot.exists()) {
                            firestore.collection("svc_eval").document(svcId).update("favorite_num", FieldValue.increment(1));
                        } else {
                            Map<String, Integer> favorite = new HashMap<>();
                            favorite.put("favorite_num", 1);
                            firestore.collection("gas_eval").document(svcId).set(favorite);
                        }
                    }
                });
            }


        // Requires ther registration process in RegisterDialogFragment before adding the favorite
        // list and the geofence.
        } else {
            //Toast.makeText(getActivity(), R.string.svc_msg_registration, Toast.LENGTH_SHORT).show();
            etServiceName.clearFocus();
            Snackbar.make(relativeLayout, R.string.svc_msg_registration, Snackbar.LENGTH_SHORT).show();
        }


    }

    // Invoked by OnOptions
    public boolean saveServiceData() {

        if(!doEmptyCheck()) return false;

        String dateFormat = getString(R.string.date_format_1);
        long milliseconds = BaseActivity.parseDateTime(dateFormat, tvDate.getText().toString());
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
        basicEntity.category = 2;
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

        for(ServicedItemEntity obj : itemEntityList) {
            log.i("ServicedItemEntity: %s", obj.itemName);
        }


        // Insert data into both ExpenseBaseEntity and ServiceManagerEntity at the same time
        // using @Transaction in ServiceManagerDao.
        int rowId = mDB.serviceManagerModel().insertAll(basicEntity, serviceEntity, itemEntityList);

        if(rowId > 0) {
            // Save the current mileage in the SharedPreferences to keep it in common.
            mSettings.edit().putString(Constants.ODOMETER, tvMileage.getText().toString()).apply();
            Toast.makeText(getActivity(), getString(R.string.toast_save_success), Toast.LENGTH_SHORT).show();

            return true;

        } else return false;

    }

    private boolean doEmptyCheck() {

        if(TextUtils.isEmpty(etServiceName.getText())) {
            String msg = getString(R.string.toast_stn_name);
            Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
            etServiceName.requestFocus();
            return false;
        }

        if(TextUtils.isEmpty(tvMileage.getText())) {
            String msg = getString(R.string.toast_mileage);
            Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
            tvMileage.requestFocus();
            return false;
        }

        return true;
    }

    @SuppressWarnings("ConstantConditions")
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
