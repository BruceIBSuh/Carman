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
import com.silverback.carman2.models.PagerAdapterViewModel;
import com.silverback.carman2.utils.Constants;
import com.silverback.carman2.models.FragmentSharedModel;
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

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(ServiceManagerFragment.class);

    // Objects
    //private SparseArray<ServiceManagerDao.LatestServiceData> sparseServiceArray;
    private SharedPreferences mSettings;
    private CarmanDatabase mDB;
    private FirebaseFirestore fireStore;
    private FragmentSharedModel fragmentSharedModel;
    private PagerAdapterViewModel adapterModel;

    private NumberPadFragment numPad;
    private MemoPadFragment memoPad;

    private FavoriteGeofenceHelper geofenceHelper;
    private ExpServiceItemAdapter mAdapter;
    private DecimalFormat df;
    private JSONArray jsonServiceArray;
    private Location mLocation;
    private String mAddress;
    private String svcCompany;

    // UIs
    private RelativeLayout relativeLayout;
    private RecyclerView recyclerServiceItems;
    private ProgressBar progbar;
    private EditText etStnName;
    private TextView tvDate, tvMileage, tvTotalCost;
    private ImageButton btnFavorite;

    // Fields
    private String distCode;
    private int itemPos;
    private int totalExpense;
    private boolean isGeofenceIntent; // check if this has been launched by Geofence.
    private boolean isFavorite;

    private float svcRating;
    private String svcComment;
    private String svcDocumentId;

    public ServiceManagerFragment() {
        // Required empty public constructor
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(getArguments() != null) {
            distCode = getArguments().getString("distCode");
            log.i("District Code in ServiceManagerFragment: %s",distCode);
        }

        // Instantiate objects.
        mSettings = ((ExpenseActivity)getActivity()).getSettings();
        if(mDB == null) mDB = CarmanDatabase.getDatabaseInstance(getActivity().getApplicationContext());
        if(fireStore == null) fireStore = FirebaseFirestore.getInstance();

        fragmentSharedModel = ViewModelProviders.of(getActivity()).get(FragmentSharedModel.class);
        adapterModel = ViewModelProviders.of(getActivity()).get(PagerAdapterViewModel.class);

        if(geofenceHelper == null) geofenceHelper = new FavoriteGeofenceHelper(getContext());
        df = BaseActivity.getDecimalFormatInstance();
        numPad = new NumberPadFragment();
        memoPad = new MemoPadFragment();

        //sparseServiceArray = new SparseArray<>();

        //The service item data is saved in SharedPreferences as String type, which should be
        // converted to JSONArray.
        /*
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
        */





        // Get the dataset of the item list from ServiceRecyclerTask, which intends to manage
        // the loadweight of the memory
        adapterModel.getJsonServiceArray().observe(this, jsonServiceArray -> {
            this.jsonServiceArray = jsonServiceArray;
            mAdapter = new ExpServiceItemAdapter(jsonServiceArray, this);
            if(recyclerServiceItems != null) recyclerServiceItems.setAdapter(mAdapter);
            //progbar.setVisibility(View.GONE);

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
        });

        /*
         * ViewModel to share data b/w Fragments(this and NumberPadFragment)
         * @param: getSelectedValue(): SparseArray<Integer>
         * @param: getSelectedMemo(): SparseArray<String>
         */
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

        // Communicate b/w RecyclerView.ViewHolder and the item memo in MemoPadFragment
        fragmentSharedModel.getSelectedMenu().observe(this, data -> mAdapter.notifyItemChanged(itemPos, data));

        // Communicate b/w AddFavoriteDialogFragment and ServiceManagerFragment.
        fragmentSharedModel.getServiceLocation().observe(this, sparseArray -> {
            mLocation = (Location)sparseArray.get(AddFavoriteDialogFragment.LOCATION);
            mAddress = (String)sparseArray.get(AddFavoriteDialogFragment.ADDRESS);
            svcCompany = (String)sparseArray.get(AddFavoriteDialogFragment.COMPANY);
            svcRating = (Float)sparseArray.get(AddFavoriteDialogFragment.RATING);
            svcComment = (String)sparseArray.get(AddFavoriteDialogFragment.COMMENT);
            log.i("Service Locaiton: %s, %s, %s, %s", mLocation, mAddress, svcRating, svcComment);
        });

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        View localView = inflater.inflate(R.layout.fragment_service_manager, container, false);
        View boxview = localView.findViewById(R.id.view_boxing);
        log.i("BoxView height: %s %s", boxview.getHeight(), boxview.getMeasuredHeight());

        //progbar = localView.findViewById(R.id.pb_checklist);
        //progbar.setVisibility(View.VISIBLE);

        relativeLayout = localView.findViewById(R.id.rl_service);
        recyclerServiceItems = localView.findViewById(R.id.recycler_service);
        tvDate = localView.findViewById(R.id.tv_service_date);
        etStnName = localView.findViewById(R.id.et_service_provider);
        tvMileage = localView.findViewById(R.id.tv_service_mileage);
        Button btnDate = localView.findViewById(R.id.btn_date);
        btnFavorite = localView.findViewById(R.id.imgbtn_favorite);
        tvTotalCost = localView.findViewById(R.id.tv_total_cost);


        tvMileage.setOnClickListener(this);
        btnDate.setOnClickListener(this);
        btnFavorite.setBackgroundResource(R.drawable.btn_favorite);
        btnFavorite.setOnClickListener(this);

        String date = BaseActivity.formatMilliseconds(getString(R.string.date_format_1), System.currentTimeMillis());
        tvDate.setText(date);
        tvTotalCost.setText("0");
        // Set the mileage value retrieved from SharedPreferences first
        tvMileage.setText(mSettings.getString(Constants.ODOMETER, ""));

        recyclerServiceItems.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerServiceItems.setHasFixedSize(true);
        //recyclerServiceItems.setAdapter(mAdapter);

        return localView;
    }

    @Override
    public void onResume() {
        super.onResume();
        fragmentSharedModel.setCurrentFragment(this);
    }


    @SuppressWarnings("ConstantConditions")
    @Override
    public void onClick(View v) {
        // Indicate which TextView is clicked, then put a value retrieved from InputNumberPad
        // via FragmentViewModel in the textview.
        switch(v.getId()) {
            case R.id.tv_service_mileage:
                //targetView = (TextView)v;
                Bundle args = new Bundle();
                args.putString("title", getString(R.string.svc_label_mileage));
                args.putString("initValue", tvMileage.getText().toString());
                args.putInt("viewId", v.getId());
                numPad.setArguments(args);
                if(getFragmentManager() != null) numPad.show(getFragmentManager(), null);
                break;

            case R.id.imgbtn_favorite:
                // In case the activity(fragment) has been lauched by Geofence b/c the current location
                // is within the registered one.
                if(isGeofenceIntent) return;

                String serviceName = etStnName.getText().toString();
                String serivceId = BaseActivity.formatMilliseconds("yyMMddHHmm", System.currentTimeMillis());

                if(TextUtils.isEmpty(serviceName)) {
                    Snackbar.make(relativeLayout, R.string.svc_msg_empty_name, Snackbar.LENGTH_SHORT).show();
                    return;
                }

                if(isFavorite) {
                    geofenceHelper.removeFavoriteGeofence(serviceName, serivceId);
                    btnFavorite.setBackgroundResource(R.drawable.btn_favorite);
                    Toast.makeText(getActivity(), getString(R.string.toast_remove_favorite_service), Toast.LENGTH_SHORT).show();

                } else {
                    AddFavoriteDialogFragment.newInstance(serviceName, distCode, 2).show(getFragmentManager(), null);
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


    // Upload the data of an service center to FireStore
    private void registerFavorite() {
        // Upload the auto service center data to FireStore.
        Map<String, Object> svcData = new HashMap<>();
        svcData.put("svcName", etStnName.getText().toString());
        svcData.put("svcCode", "BluHands");
        svcData.put("address", mAddress);
        svcData.put("xCoord", mLocation.getLongitude());
        svcData.put("yCoord", mLocation.getLatitude());

        DocumentReference doc = fireStore.collection("svc_center").document();
        doc.set(svcData).addOnCompleteListener(task -> {
            if(task.isSuccessful()) {
                log.i("upload service data completed");
                uploadRatingComment(doc.getId());

            } else {
                log.i("upload service data failed");
            }
        });
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

        serviceEntity.serviceCenter = etStnName.getText().toString();
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

        if(TextUtils.isEmpty(etStnName.getText())) {
            String msg = getString(R.string.toast_stn_name);
            Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
            etStnName.requestFocus();
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
    private void uploadRatingComment(String svcId) {
        if(svcRating > 0) {
            Map<String, Object> ratingData = new HashMap<>();
            ratingData.put("eval_num", FieldValue.increment(1));
            ratingData.put("eval_sum", FieldValue.increment(svcRating));

            DocumentReference docRef = fireStore.collection("svc_eval").document(svcId);
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
            commentData.put("name", mSettings.getString(Constants.VEHICLE_NAME, null));
            commentData.put("comments", svcComment);
            commentData.put("rating", svcRating);

            fireStore.collection("svc_eval").document(svcId).collection("comments").add(commentData)
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
