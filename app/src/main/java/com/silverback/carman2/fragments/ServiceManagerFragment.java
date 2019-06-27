package com.silverback.carman2.fragments;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.silverback.carman2.BaseActivity;
import com.silverback.carman2.ExpenseActivity;
import com.silverback.carman2.R;
import com.silverback.carman2.adapters.ServiceItemListAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Constants;
import com.silverback.carman2.models.FragmentSharedModel;
import com.silverback.carman2.models.ServiceCheckListModel;
import com.silverback.carman2.utils.FavoriteGeofenceHelper;
import com.silverback.carman2.views.ServiceItemRecyclerView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.Calendar;
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


    private SharedPreferences mSettings;
    private FragmentSharedModel fragmentSharedModel;

    private ServiceCheckListModel checkListModel;
    private SparseBooleanArray sbArrCheckbox;
    private SparseArray sArrItemCost, sArrItemMemo;

    private InputPadFragment numPad;
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
    private TextView targetView; //reference to a clicked view which is used in ViewModel
    private String jsonServiceitem;
    private int itemPos;
    private int totalCost;
    private boolean isGeofenceIntent; // check if this has been launched by Geofence.
    private boolean isFavorite;

    public ServiceManagerFragment() {
        // Required empty public constructor
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSettings = ((ExpenseActivity)getActivity()).getSettings();

        // Instantiate ViewModels.
        fragmentSharedModel = ViewModelProviders.of(getActivity()).get(FragmentSharedModel.class);
        checkListModel = ViewModelProviders.of(getActivity()).get(ServiceCheckListModel.class);

        sbArrCheckbox = new SparseBooleanArray();
        sArrItemCost = new SparseArray();
        sArrItemMemo = new SparseArray();

        geofenceHelper = new FavoriteGeofenceHelper(getContext());
        df = BaseActivity.getDecimalFormatInstance();
        calendar = Calendar.getInstance(Locale.getDefault());
        numPad = new InputPadFragment();


        arrServiceItem = getResources().getStringArray(R.array.service_item_list);


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
        mAdapter = new ServiceItemListAdapter(this, checkListModel, arrServiceItem);
        serviceItemRecyclerView.setAdapter(mAdapter);


        // Receive values from InputPadFragment using LiveData defined in FragmentSharedModel as
        // SparseArray.
        fragmentSharedModel.getSelectedValue().observe(this, data -> {

            final int viewId = data.keyAt(0);
            final String value = (String)data.valueAt(0);

            switch(viewId) {
                case R.id.tv_mileage:
                    targetView = localView.findViewById(viewId);
                    targetView.setText(value);
                    break;

                case R.id.tv_value_cost:
                    mAdapter.notifyItemChanged(itemPos, value);
                    try {
                        totalCost += df.parse(value).intValue();
                        tvTotalCost.setText(df.format(totalCost));
                    } catch(ParseException e) {
                        log.e("ParseException: %s", e.getMessage());
                    }

                    break;
            }

        });




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

        if(getFragmentManager() != null) numPad.show(getFragmentManager(), "numPad");

    }

    @Override
    public void inputItemMemo(String title, TextView targetView, int position) {
        ServiceItemMemoFragment memoFragment = ServiceItemMemoFragment.newInstance(title, position);
        if(getFragmentManager() != null) memoFragment.show(getFragmentManager(), "memoPad");
    }

    @Override
    public void subtractCost(String value) {
        try {
            totalCost -= df.parse(value).intValue();
            tvTotalCost.setText(df.format(totalCost));
        } catch(ParseException e) {
            log.e("ParseException: %s", e.getMessage());
        }

    }

    public boolean saveServiceData() {

        if(!doEmptyCheck()) return false;

        String dateFormat = getString(R.string.date_format_1);
        long milliseconds = BaseActivity.parseDateTime(dateFormat, tvDate.getText().toString());


        /*
        BasicManagerEntity basicEntity = new BasicManagerEntity();
        ServiceManagerEntity serviceEntity = new ServiceManagerEntity();

        basicEntity.dateTime = milliseconds;
        basicEntity.category = 2;

        try {
            basicEntity.mileage = df.parse(tvMileage.getText().toString()).intValue();
        } catch(ParseException e) {
            log.e("ParseException: %s", e.getMessage());
            return false;
        }
        */

        return true;
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
