package com.silverback.carman2.fragments;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.silverback.carman2.BaseActivity;
import com.silverback.carman2.ExpenseActivity;
import com.silverback.carman2.R;
import com.silverback.carman2.adapters.ExpenseSvcRecyclerAdapter;
import com.silverback.carman2.database.BasicManagerEntity;
import com.silverback.carman2.database.CarmanDatabase;
import com.silverback.carman2.database.ServiceManagerEntity;
import com.silverback.carman2.database.ServicedItemEntity;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.AdapterViewModel;
import com.silverback.carman2.models.Constants;
import com.silverback.carman2.models.FragmentSharedModel;
import com.silverback.carman2.utils.FavoriteGeofenceHelper;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class ServiceManagerFragment extends Fragment implements
        View.OnClickListener, ExpenseSvcRecyclerAdapter.OnParentFragmentListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(ServiceManagerFragment.class);

    // Objects
    private List<String> serviceItemList;
    private SharedPreferences mSettings;
    private CarmanDatabase mDB;
    private FragmentSharedModel fragmentSharedModel;
    private AdapterViewModel adapterModel;

    private NumberPadFragment numPad;
    private MemoPadFragment memoPad;

    private FavoriteGeofenceHelper geofenceHelper;
    //private Calendar calendar;
    private ExpenseSvcRecyclerAdapter mAdapter;
    private DecimalFormat df;


    // UIs
    private RecyclerView recyclerServiceItems;
    private ProgressBar pbServiceItems;
    private EditText etStnName;
    private TextView tvDate, tvMileage, tvTotalCost;
    private ImageButton btnFavorite;

    // Fields
    private TextView targetView; //reference to a clicked view which is used in ViewModel
    private int itemPos;
    private int totalExpense;
    private boolean isGeofenceIntent; // check if this has been launched by Geofence.
    private boolean isFavorite;

    public ServiceManagerFragment() {
        // Required empty public constructor
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Instantiate objects.
        mSettings = ((ExpenseActivity)getActivity()).getSettings();
        mDB = CarmanDatabase.getDatabaseInstance(getActivity().getApplicationContext());
        fragmentSharedModel = ViewModelProviders.of(getActivity()).get(FragmentSharedModel.class);
        adapterModel = ViewModelProviders.of(getActivity()).get(AdapterViewModel.class);


        geofenceHelper = new FavoriteGeofenceHelper(getContext());
        df = BaseActivity.getDecimalFormatInstance();
        //calendar = Calendar.getInstance(Locale.getDefault());

        numPad = new NumberPadFragment();
        memoPad = new MemoPadFragment();

        adapterModel.getServiceAdapter().observe(this, adapter -> {
            log.i("AdapterModel observe: %s", adapter);
            mAdapter = adapter;
            pbServiceItems.setVisibility(View.GONE);
            //recyclerServiceItems.setVisibility(View.VISIBLE);

            mAdapter.setParentFragmentListener(this);
            recyclerServiceItems.setAdapter(adapter);
        });

        adapterModel.getServicedItem().observe(this, checklist -> {
            log.i("Serviced Item: %s", checklist.size());
            serviceItemList = checklist;

            for(int i = 0; i < checklist.size(); i++) {
                //log.i("servicedItemList: %s", serviceItemList.get(i));
                final int pos = i;
                mDB.serviceManagerModel().loadServicedItem(checklist.get(pos)).observe(this, itemData -> {
                    if(itemData != null) {
                        log.i("Queried data: %s, %s", pos, itemData.itemName);
                        mAdapter.notifyItemChanged(pos, itemData);
                    }
                });

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
                case R.id.tv_mileage:
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

        // Communicate b/w RecyclerView.ViewHolder and item memo in MemoPadFragment
        fragmentSharedModel.getSelectedMenu().observe(this, data ->
                mAdapter.notifyItemChanged(itemPos, data));

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        View localView = inflater.inflate(R.layout.fragment_service_manager, container, false);
        View boxview = localView.findViewById(R.id.view_boxing);
        log.i("BoxView height: %s %s", boxview.getHeight(), boxview.getMeasuredHeight());

        pbServiceItems = localView.findViewById(R.id.pb_checklist);
        pbServiceItems.setVisibility(View.VISIBLE);

        recyclerServiceItems = localView.findViewById(R.id.recycler_service);
        tvDate = localView.findViewById(R.id.tv_service_date);
        etStnName = localView.findViewById(R.id.et_service_provider);
        tvMileage = localView.findViewById(R.id.tv_mileage);
        Button btnDate = localView.findViewById(R.id.btn_date);
        btnFavorite = localView.findViewById(R.id.imgbtn_favorite);
        tvTotalCost = localView.findViewById(R.id.tv_total_cost);
        tvTotalCost.setText("0");

        recyclerServiceItems.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerServiceItems.setHasFixedSize(true);

        tvMileage.setOnClickListener(this);
        btnDate.setOnClickListener(this);
        btnFavorite.setBackgroundResource(R.drawable.btn_favorite);
        btnFavorite.setOnClickListener(this);

        String date = BaseActivity.formatMilliseconds(getString(R.string.date_format_1), System.currentTimeMillis());
        tvDate.setText(date);

        // Set the mileage value retrieved from SharedPreferences first
        tvMileage.setText(mSettings.getString(Constants.ODOMETER, ""));

        // Inflate the layout for this fragment
        return localView;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Notify ExpensePagerFragment of the current fragment to load the recent 5 expense data from
        // ServiceTable.
        fragmentSharedModel.setCurrentFragment(this);

    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onClick(View v) {
        // Indicate which TextView is clicked, then put a value retrieved from InputNumberPad
        // via FragmentViewModel in the textview.
        switch(v.getId()) {
            case R.id.tv_mileage:
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

                String providerName = etStnName.getText().toString();
                String providerId = BaseActivity.formatMilliseconds("yyMMddHHmm", System.currentTimeMillis());

                if(TextUtils.isEmpty(providerName)) return;

                if(isFavorite) {
                    geofenceHelper.removeFavoriteGeofence(providerName, providerId);
                    btnFavorite.setBackgroundResource(R.drawable.btn_favorite);
                    Toast.makeText(getActivity(), getString(R.string.toast_remove_favorite_service), Toast.LENGTH_SHORT).show();

                } else {
                    AddFavoriteDialogFragment.newInstance(providerName, 2).show(getFragmentManager(), null);
                }

                break;

            case R.id.btn_date:
                break;

        }

    }

    // ServiceItemList.OnParentFragmentListener invokes this method
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

        BasicManagerEntity basicEntity = new BasicManagerEntity();
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
                //checkedItem.itemName = jsonSvcItemArray.optJSONObject(i).optString("name");
                checkedItem.itemName = serviceItemList.get(i);
                checkedItem.itemPrice = mAdapter.arrItemCost[i];
                checkedItem.itemMemo = mAdapter.arrItemMemo[i];
                log.i("Serviced Item: %s", checkedItem.itemName);
                itemEntityList.add(checkedItem);
            }
        }

        for(ServicedItemEntity obj : itemEntityList) {
            log.i("ServicedItemEntity: %s", obj.itemName);
        }

        // Insert data into both BasicManagerEntity and ServiceManagerEntity at the same time
        // using @Transaction in ServiceManagerDao.
        int rowId = mDB.serviceManagerModel().insertAll(basicEntity, serviceEntity, itemEntityList);

        if(rowId > 0) {
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



}
