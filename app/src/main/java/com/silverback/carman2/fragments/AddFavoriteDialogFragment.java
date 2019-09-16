package com.silverback.carman2.fragments;


import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.silverback.carman2.R;
import com.silverback.carman2.adapters.DistrictSpinnerAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.LocationViewModel;
import com.silverback.carman2.models.Opinet;
import com.silverback.carman2.models.SpinnerDistrictModel;
import com.silverback.carman2.threads.GeocoderReverseTask;
import com.silverback.carman2.threads.GeocoderTask;
import com.silverback.carman2.threads.LoadDistCodeTask;
import com.silverback.carman2.threads.LocationTask;
import com.silverback.carman2.threads.ThreadManager;

/**
 * A simple {@link Fragment} subclass.
 */
public class AddFavoriteDialogFragment extends DialogFragment implements
        AdapterView.OnItemSelectedListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(AddFavoriteDialogFragment.class);

    // Objects
    private LoadDistCodeTask spinnerTask;
    private GeocoderReverseTask geocoderReverseTask;
    private GeocoderTask geocoderTask;
    private LocationTask locationTask;
    private SpinnerDistrictModel distModel;
    private LocationViewModel locationModel;
    private DistrictSpinnerAdapter sigunAdapter;

    private String providerName;
    private String distCode;

    // UIs
    private Spinner sidoSpinner, sigunSpinner;
    private TextView tvSido, tvSigun;
    private EditText etAddrs;

    // Fields
    private int category;
    private int mSidoItemPos, mSigunItemPos, tmpSidoPos, tmpSigunPos;
    private boolean isCurrentLocation;

    // Default constructor
    private AddFavoriteDialogFragment() {
        // Required empty public constructor
    }

    // Instantiate DialogFragment as a SingleTon
    static AddFavoriteDialogFragment newInstance(String name, String distCode, int category) {
        AddFavoriteDialogFragment favoriteDialog = new AddFavoriteDialogFragment();
        Bundle args = new Bundle();
        args.putString("favoriteName", name);
        args.putString("distCode", distCode);
        args.putInt("category", category);
        favoriteDialog.setArguments(args);

        return favoriteDialog;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        providerName = getArguments().getString("favoriteName");
        distCode = getArguments().getString("distCode");
        category = getArguments().getInt("category");

        // ViewModel to fetch the sigun list of a given sido name
        distModel = ViewModelProviders.of(this).get(SpinnerDistrictModel.class);
        locationModel = ViewModelProviders.of(this).get(LocationViewModel.class);

        distModel.getSpinnerDataList().observe(this, dataList -> {
            if(sigunAdapter.getCount() > 0) sigunAdapter.removeAll();
            for(Opinet.DistrictCode obj : dataList) {
                sigunAdapter.addItem(obj);
            }
            sigunSpinner.setAdapter(sigunAdapter);
            sigunSpinner.setSelection(mSigunItemPos);
        });

        // Fetch the current location using LocationTask and LocationViewModel, with which
        // GeocoderReverseTask is initiated to get the current address.
        locationModel.getLocation().observe(this, location->{
            log.i("Current Location: %s", location);
            geocoderReverseTask = ThreadManager.startReverseGeocoderTask(getContext(), locationModel, location);

        });

        // Fetch the current address and split it for inputting sido and sigun name respectively into
        // its TextViews which replace the Spinners. Using StringBuffer, insert the space between
        // the remaining address names.
        locationModel.getAddress().observe(this, address -> {
            log.i("Address: %s", address);
            final String[] arrAddrs = TextUtils.split(address, "\\s+");
            final StringBuilder strbldr = new StringBuilder();
            for(int i = 2; i < arrAddrs.length; i++) strbldr.append(arrAddrs[i]).append(" ");
            tvSido.setText(arrAddrs[0]);
            tvSigun.setText(arrAddrs[1]);
            etAddrs.setText(strbldr.toString());

            tvSido.setVisibility(View.VISIBLE);
            tvSigun.setVisibility(View.VISIBLE);
            sidoSpinner.setVisibility(View.GONE);
            sigunSpinner.setVisibility(View.GONE);
        });

        locationModel.getGeocoderLocation().observe(this, location -> {
            log.i("Geocoder Location: %s, %s", location.getLatitude(), location.getLongitude());
        });

    }

    @SuppressWarnings("ConstantConditions")
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        //LayoutInflater inflater = requireActivity().getLayoutInflater();
        View localView = View.inflate(getContext(), R.layout.dialog_add_favorite, null);
        //View localView = inflater.inflate(R.layout.dialog_add_favorite, null);
        setCancelable(false);

        TextView tvTitle = localView.findViewById(R.id.tv_title);
        TextView tvCurrentLocation = localView.findViewById(R.id.tv_label_location);
        tvSido = localView.findViewById(R.id.tv_sido);
        tvSigun = localView.findViewById(R.id.tv_sigun);
        sidoSpinner = localView.findViewById(R.id.spinner_sido);
        sigunSpinner= localView.findViewById(R.id.spinner_sigun);
        etAddrs = localView.findViewById(R.id.et_addrs_detail);

        sidoSpinner.setOnItemSelectedListener(this);
        sigunSpinner.setOnItemSelectedListener(this);
        tvCurrentLocation.setOnClickListener(view -> {
            isCurrentLocation = true;
            locationTask = ThreadManager.fetchLocationTask(getContext(), locationModel);
        });

        tvTitle.setText(providerName);

        String sidoCode = distCode.substring(0, 2);
        String sigunCode = distCode.substring(2, 4);

        mSidoItemPos = Integer.valueOf(sidoCode) - 1; // "01" is translated into 1 as Integer.
        mSigunItemPos = Integer.valueOf(sigunCode) -1;


        // sidoSpinner and adapter
        ArrayAdapter sidoAdapter = ArrayAdapter.createFromResource(
                getContext(), R.array.sido_name, android.R.layout.simple_spinner_item);
        sidoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sidoSpinner.setAdapter(sidoAdapter);
        sidoSpinner.setSelection(mSidoItemPos);

        // Sigun Spinner and Adapter
        sigunAdapter = new DistrictSpinnerAdapter(getContext());
        //sigunSpinner.setAdapter(sigunAdapter);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(localView)
                .setNegativeButton(R.string.dialog_btn_cancel, (dialog, which) -> {})
                .setPositiveButton(R.string.dialog_btn_confirm, (dialog, which) -> {

                    // Refactor required b/c the dialog closes before receiving the locaiton value.
                    if(!isCurrentLocation) {
                        StringBuilder strbldr = new StringBuilder();

                        strbldr.append(sidoSpinner.getSelectedItem()).append(" ")
                                .append(sigunAdapter.getItem(tmpSigunPos).getDistrictName()).append(" ")
                                .append(etAddrs.getText());

                        geocoderTask = ThreadManager.startGeocoderTask(getContext(), locationModel, strbldr.toString());
                    }
                });


        return builder.create();
    }

    @Override
    public void onStop() {
        log.i("DialogFragment");
        if(spinnerTask != null) spinnerTask = null;
        if(locationTask != null) locationTask = null;
        if(geocoderReverseTask != null) geocoderReverseTask = null;
        if(geocoderTask != null) geocoderTask = null;

        super.onStop();
    }


    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if(parent == sidoSpinner) {
            spinnerTask = ThreadManager.loadSpinnerDistCodeTask(getContext(), distModel, position);
            if(position != mSidoItemPos) mSigunItemPos = 0;
            tmpSidoPos = position;
        } else {
            tmpSigunPos = position;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {}
}
