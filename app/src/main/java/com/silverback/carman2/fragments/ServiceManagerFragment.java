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
        View.OnClickListener, ServiceItemListAdapter.ServiceItemListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(ServiceManagerFragment.class);

    // Objects
    private SharedPreferences mSettings;
    private FragmentSharedModel viewModel;
    private InputPadFragment numPad;
    private Calendar calendar;
    private ServiceItemListAdapter mAdapter;
    private ServiceItemRecyclerView serviceItemRecyclerView;
    private DecimalFormat df;

    // UIs
    private EditText etStnName;
    private TextView tvDate, tvMileage, tvTotalCost, tvItemCost;
    private TextView itemCost;

    // Fields
    private int itemResId;

    public ServiceManagerFragment() {
        // Required empty public constructor
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSettings = ((ExpenseActivity)getActivity()).getSettings();
        viewModel = ViewModelProviders.of(getActivity()).get(FragmentSharedModel.class);
        numPad = new InputPadFragment();
        df = BaseActivity.getDecimalFormatInstance();
        calendar = Calendar.getInstance(Locale.getDefault());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        String[] serviceItems = getResources().getStringArray(R.array.service_item_list);
        JSONArray jsonArray = new JSONArray(Arrays.asList(serviceItems));
        String json = jsonArray.toString();

        View localView = inflater.inflate(R.layout.fragment_service_manager, container, false);
        View boxview = localView.findViewById(R.id.view);
        log.i("BoxView height: %s %s", boxview.getHeight(), boxview.getMeasuredHeight());

        tvDate = localView.findViewById(R.id.tv_service_date);
        etStnName = localView.findViewById(R.id.et_station_name);
        tvMileage = localView.findViewById(R.id.tv_mileage);
        Button btnDate = localView.findViewById(R.id.btn_date);
        ImageButton favorite = localView.findViewById(R.id.imgbtn_favorite);
        tvTotalCost = localView.findViewById(R.id.tv_value_payment);

        tvMileage.setOnClickListener(this);
        btnDate.setOnClickListener(this);
        favorite.setOnClickListener(this);

        String date = BaseActivity.formatMilliseconds(getString(R.string.date_format_1), System.currentTimeMillis());
        tvDate.setText(date);
        favorite.setBackgroundResource(R.drawable.btn_favorite);

        // Set the mileage value retrieved from SharedPreferences first
        tvMileage.setText(mSettings.getString(Constants.ODOMETER, ""));
        viewModel.getValue().observe(this, data -> tvMileage.setText(data));

        serviceItemRecyclerView = localView.findViewById(R.id.recycler_service);
        serviceItemRecyclerView.setHasFixedSize(true);
        //String jsonItems = getArguments().getString("serviceItems");
        mAdapter = new ServiceItemListAdapter(json);
        serviceItemRecyclerView.setAdapter(mAdapter);

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

    @Override
    public void onClick(View v) {

        switch(v.getId()) {
            // Show InputPadFragment
            case R.id.tv_mileage:
                Bundle args = new Bundle();
                args.putString("value", tvMileage.getText().toString());
                args.putInt("viewId", v.getId());
                numPad.setArguments(args);
                if(getFragmentManager() != null) numPad.show(getFragmentManager(), "numPad");
                break;

            case R.id.imgbtn_favorite:
                break;

            case R.id.btn_date:
                break;

        }

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


    @Override
    public void inputItemCost(String itemName, View view) {
        Bundle args = new Bundle();
        args.putString("title", itemName);

        if(getFragmentManager() != null) numPad.show(getFragmentManager(), "numPad");
    }
}
