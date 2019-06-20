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
import android.widget.TextView;
import android.widget.Toast;

import com.silverback.carman2.BaseActivity;
import com.silverback.carman2.ExpenseActivity;
import com.silverback.carman2.R;
import com.silverback.carman2.adapters.ServiceItemListAdapter;
import com.silverback.carman2.database.BasicManagerEntity;
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

import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;

/**
 * A simple {@link Fragment} subclass.
 */
public class ServiceManagerFragment extends Fragment implements
        View.OnClickListener, ServiceItemListAdapter.OnServiceItemClickListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(ServiceManagerFragment.class);

    // Objects
    private SharedPreferences mSettings;
    private FragmentSharedModel viewModel;
    private FavoriteGeofenceHelper geofenceHelper;
    private Calendar calendar;
    private ServiceItemListAdapter mAdapter;
    private ServiceItemRecyclerView serviceItemRecyclerView;
    private DecimalFormat df;

    // UIs
    private EditText etStnName;
    private TextView tvDate, tvMileage, tvTotalCost, tvItemCost;
    private TextView itemCost;
    private ImageButton btnFavorite;

    // Fields
    private TextView targetView; //reference to a clicked view which is used in ViewModel
    private String jsonServiceitem;
    private int itemPosition;
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
        viewModel = ViewModelProviders.of(getActivity()).get(FragmentSharedModel.class);
        geofenceHelper = new FavoriteGeofenceHelper(getContext());
        df = BaseActivity.getDecimalFormatInstance();
        calendar = Calendar.getInstance(Locale.getDefault());

        String[] serviceItems = getResources().getStringArray(R.array.service_item_list);
        JSONArray jsonArray = new JSONArray(Arrays.asList(serviceItems));
        jsonServiceitem = jsonArray.toString();

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
        tvTotalCost = localView.findViewById(R.id.tv_value_payment);

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
        mAdapter = new ServiceItemListAdapter(jsonServiceitem, this);
        serviceItemRecyclerView.setAdapter(mAdapter);

        // Receive a value from InputPadFragment using LiveData defined in FragmentSharedModel.
        viewModel.getSelectedValue().observe(this, data -> {
            int viewId = data.keyAt(0);
            String value = (String)data.valueAt(0);

            targetView = localView.findViewById(viewId);
            if(targetView == null) return;
            targetView.setText(value);

            if(data.keyAt(0) == R.id.tv_value_cost) {
                mAdapter.notifyItemChanged(itemPosition, value);
                try {
                    totalCost += df.parse(value).intValue();
                    tvTotalCost.setText(df.format(totalCost));
                } catch(ParseException e) {
                    log.e("ParseException: %s", e.getMessage());
                }
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
        viewModel.setCurrentFragment(this);

    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onClick(View v) {
        // Indicate which TextView is clicked, then put a value retrieved from InputNumberPad
        // via FragmentViewModel in the textview.
        switch(v.getId()) {
            case R.id.tv_mileage:
                //targetView = (TextView)v;
                InputPadFragment.newInstance(null, tvMileage.getText().toString(), v.getId())
                        .show(getFragmentManager(), "numPad");
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

    // ServiceItemList.OnServiceItemClickListener invokes this method
    // to pop up InputPadFragment and input the amount of expense in a service item.
    @SuppressWarnings("ConstantConditions")
    @Override
    public void inputItemCost(String itemName, TextView view, int position) {
        itemPosition = position;
        InputPadFragment.newInstance(itemName, "0", view.getId()).show(getFragmentManager(), "numPad");
    }

    public boolean saveServiceData() {
        if(!doEmptyCheck()) return false;
        String dateFormat = getString(R.string.date_format_1);
        long milliseconds = BaseActivity.parseDateTime(dateFormat, tvDate.getText().toString());

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
