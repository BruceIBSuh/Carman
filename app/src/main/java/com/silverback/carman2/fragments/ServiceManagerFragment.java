package com.silverback.carman2.fragments;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.silverback.carman2.BaseActivity;
import com.silverback.carman2.ManagementActivity;
import com.silverback.carman2.R;
import com.silverback.carman2.adapters.ServiceItemListAdapter;
import com.silverback.carman2.database.BasicManagerEntity;
import com.silverback.carman2.database.CarmanDatabase;
import com.silverback.carman2.database.ServiceManagerDao;
import com.silverback.carman2.database.ServicedItemEntity;
import com.silverback.carman2.database.ServiceManagerEntity;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Constants;
import com.silverback.carman2.models.FragmentSharedModel;
import com.silverback.carman2.utils.FavoriteGeofenceHelper;
import com.silverback.carman2.views.ServiceItemRecyclerView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import org.json.JSONArray;
import org.json.JSONException;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * A simple {@link Fragment} subclass.
 */
public class ServiceManagerFragment extends Fragment implements
        View.OnClickListener, ServiceItemListAdapter.OnParentFragmentListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(ServiceManagerFragment.class);

    // Objects
    private String[] arrServiceItem;
    private List<String> svcItemList;
    private JSONArray jsonServiceItemArray;


    private SharedPreferences mSettings;
    private CarmanDatabase mDB;
    private FragmentSharedModel fragmentSharedModel;

    private InputPadFragment numPad;
    private ServiceItemMemoFragment memoPad;

    private FavoriteGeofenceHelper geofenceHelper;
    private Calendar calendar;
    private ServiceItemListAdapter mAdapter;
    private ServiceItemRecyclerView serviceItemRecyclerView;
    private DecimalFormat df;

    // UIs
    private EditText etStnName;
    private TextView tvDate, tvMileage, tvTotalCost;
    private ImageButton btnFavorite;

    // Fields
    private SparseArray<ServiceManagerDao.ServicedItemData> servicedItemArray;
    private TextView targetView; //reference to a clicked view which is used in ViewModel
    private int itemPos;
    private int totalServiceExpense;
    private boolean isGeofenceIntent; // check if this has been launched by Geofence.
    private boolean isFavorite;

    public ServiceManagerFragment() {
        // Required empty public constructor
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSettings = ((ManagementActivity)getActivity()).getSettings();
        arrServiceItem = getResources().getStringArray(R.array.service_item_list);

        String jsonServiceItem = mSettings.getString(Constants.SERVICE_ITEMS, null);
        try {
            jsonServiceItemArray = new JSONArray(jsonServiceItem);

        } catch(JSONException e) {
            log.e("JSONException: %s", e.getMessage());
        }


        // Entity to retrieve list of favorite station to compare with a fetched current station
        // to tell whether it has registered with Favorite.
        mDB = CarmanDatabase.getDatabaseInstance(getActivity().getApplicationContext());
        fragmentSharedModel = ViewModelProviders.of(getActivity()).get(FragmentSharedModel.class);

        geofenceHelper = new FavoriteGeofenceHelper(getContext());
        df = BaseActivity.getDecimalFormatInstance();
        calendar = Calendar.getInstance(Locale.getDefault());

        numPad = new InputPadFragment();
        memoPad = new ServiceItemMemoFragment();
        servicedItemArray = new SparseArray<>();

        // Query the history of service for each item using ServiceManagerDao with itemName
        // as key
        for(int i = 0; i < jsonServiceItemArray.length(); i++) {
            log.i("Service Item name: %s", jsonServiceItemArray.optJSONObject(i).optString("name"));
            ServiceManagerDao.ServicedItemData data =
                    mDB.serviceManagerModel().loadServicedItem(jsonServiceItemArray.optJSONObject(i).optString("name"));

            if(data != null) {
                //int pos = Arrays.asList(arrServiceItem).indexOf(data.itemName);
                log.i("Serviced Item: %s, %s, %s", i, data.itemName, data.mileage);
                servicedItemArray.put(i, data);
            }
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        View localView = inflater.inflate(R.layout.fragment_service_manager, container, false);
        View boxview = localView.findViewById(R.id.view);
        log.i("BoxView height: %s %s", boxview.getHeight(), boxview.getMeasuredHeight());

        tvDate = localView.findViewById(R.id.tv_service_date);
        etStnName = localView.findViewById(R.id.et_service_provider);
        tvMileage = localView.findViewById(R.id.tv_mileage);
        Button btnDate = localView.findViewById(R.id.btn_date);
        btnFavorite = localView.findViewById(R.id.imgbtn_favorite);
        tvTotalCost = localView.findViewById(R.id.tv_total_cost);
        tvTotalCost.setText("0");

        tvMileage.setOnClickListener(this);
        btnDate.setOnClickListener(this);
        btnFavorite.setBackgroundResource(R.drawable.btn_favorite);
        btnFavorite.setOnClickListener(this);

        String date = BaseActivity.formatMilliseconds(getString(R.string.date_format_1), System.currentTimeMillis());
        tvDate.setText(date);

        // Set the mileage value retrieved from SharedPreferences first
        tvMileage.setText(mSettings.getString(Constants.ODOMETER, ""));

        serviceItemRecyclerView = localView.findViewById(R.id.recycler_service);
        serviceItemRecyclerView.setHasFixedSize(true);


        mAdapter = new ServiceItemListAdapter(jsonServiceItemArray, servicedItemArray, this);
        serviceItemRecyclerView.setAdapter(mAdapter);

        // LiveData Return value: List<ServicedItemData>
        /*
        mDB.serviceManagerModel().loadServicedItemData(arrServiceItem).observe(this, data -> {
            mAdapter = new ServiceItemListAdapter(arrServiceItem, data, this);
            serviceItemRecyclerView.setAdapter(mAdapter);
        });
        */

        /*
         * ViewModel to share data b/w Fragments(this and InputPadFragment)
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
                    totalServiceExpense += data.valueAt(0);
                    tvTotalCost.setText(df.format(totalServiceExpense));
                    break;
            }
        });

        // Communicate b/w RecyclerView.ViewHolder and item memo in ServiceItemMemoFragment
        fragmentSharedModel.getSelectedMenu().observe(this, data ->
                mAdapter.notifyItemChanged(itemPos, data));



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
                if(getFragmentManager() != null) numPad.show(getFragmentManager(), "InputPadDialog");
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
                    AddFavoriteDialogFragment.newInstance(providerName, 2).show(getFragmentManager(), "favoriteDialog");
                }

                break;

            case R.id.btn_date:
                break;

        }

    }

    // ServiceItemList.OnParentFragmentListener invokes this method
    // to pop up InputPadFragment and input the amount of expense in a service item.
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
        totalServiceExpense -= value;
        tvTotalCost.setText(df.format(totalServiceExpense));
    }

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
        ServicedItemEntity servicedItem;
        List<ServicedItemEntity> itemEntityList = new ArrayList<>();

        basicEntity.dateTime = milliseconds;
        basicEntity.mileage = mileage;
        basicEntity.category = 2;
        basicEntity.totalExpense = totalServiceExpense;

        //serviceEntity.basicId = basicEntity._id;
        serviceEntity.serviceCenter = etStnName.getText().toString();
        serviceEntity.serviceAddrs = "seoul, korea";

        for(int i = 0; i < mAdapter.arrCheckedState.length; i++) {

            if(mAdapter.arrCheckedState[i]) {
                servicedItem = new ServicedItemEntity();
                servicedItem.itemName = mAdapter.arrItems[i];
                servicedItem.itemPrice = mAdapter.arrItemCost[i];
                servicedItem.itemMemo = mAdapter.arrItemMemo[i];
                log.i("Serviced Item: %s", servicedItem.itemName);
                itemEntityList.add(servicedItem);
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
