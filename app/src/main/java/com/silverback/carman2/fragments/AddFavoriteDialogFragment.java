package com.silverback.carman2.fragments;


import android.app.Dialog;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import android.text.TextUtils;
import android.util.SparseArray;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;
import com.silverback.carman2.ExpenseActivity;
import com.silverback.carman2.R;
import com.silverback.carman2.adapters.DistrictSpinnerAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.utils.Constants;
import com.silverback.carman2.models.FragmentSharedModel;
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

    // Constants
    static final int LOCATION = 0;
    static final int ADDRESS = 1;
    static final int RATING = 2;
    static final int COMMENT = 3;


    // Objects
    private SharedPreferences mSettings;
    private FragmentSharedModel fragmentModel;
    private LoadDistCodeTask spinnerTask;
    private GeocoderReverseTask geocoderReverseTask;
    private GeocoderTask geocoderTask;
    private LocationTask locationTask;
    private SpinnerDistrictModel distModel;
    private LocationViewModel locationModel;
    private DistrictSpinnerAdapter sigunAdapter;
    private AlertDialog dialog;
    private Location mLocation;
    private String mAddress;
    private String providerName;
    private String distCode;
    private String nickname;

    // UIs
    private Spinner sidoSpinner, sigunSpinner;
    private TextView tvSido, tvSigun;
    private EditText etAddrs, etServiceComment;
    private RatingBar ratingBar;

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
        mSettings = ((ExpenseActivity)getActivity()).getSettings();

        // ViewModel to fetch the sigun list of a given sido name
        fragmentModel = ViewModelProviders.of(getActivity()).get(FragmentSharedModel.class);
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
            mLocation = location;

        });

        // Fetch the current address and split it for inputting sido and sigun name respectively into
        // its TextViews which replace the Spinners. Using StringBuffer, insert the space between
        // the remaining address names.
        locationModel.getAddress().observe(this, address -> {
            log.i("Address: %s", address);
            mAddress = address;

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

        // Fetch the Location based on a given address name by using Geocoder, then pass the value
        // to ServiceManagerFragment and close the dialog.
        locationModel.getGeocoderLocation().observe(this, location -> {
            mLocation = location;
            // Pass the location and address of an service provider to ServiceManagerFragment
            // using FragmentSharedModel which enables Fragments to communicate each other.
            log.i("Geocoder Location: %s, %s", mLocation, mAddress);
            sendServiceLocation();

            dialog.dismiss();
        });

    }

    @SuppressWarnings("ConstantConditions")
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        //LayoutInflater inflater = requireActivity().getLayoutInflater();
        //View localView = inflater.inflate(R.layout.dialog_add_favorite, null);
        View localView = View.inflate(getContext(), R.layout.dialog_add_favorite, null);
        setCancelable(false);

        TextView tvTitle = localView.findViewById(R.id.tv_title);
        TextView tvCurrentLocation = localView.findViewById(R.id.tv_label_location);
        tvSido = localView.findViewById(R.id.tv_sido);
        tvSigun = localView.findViewById(R.id.tv_sigun);
        sidoSpinner = localView.findViewById(R.id.spinner_sido);
        sigunSpinner= localView.findViewById(R.id.spinner_sigun);
        etAddrs = localView.findViewById(R.id.et_addrs_detail);
        ratingBar = localView.findViewById(R.id.rb_service);
        Button resetRating = localView.findViewById(R.id.btn_reset_ratingbar);
        etServiceComment = localView.findViewById(R.id.et_service_comment);

        tvTitle.setText(providerName);

        // Event Handlers
        sidoSpinner.setOnItemSelectedListener(this);
        sigunSpinner.setOnItemSelectedListener(this);
        tvCurrentLocation.setOnClickListener(view -> {
            isCurrentLocation = true;
            locationTask = ThreadManager.fetchLocationTask(getContext(), locationModel);
        });

        nickname = mSettings.getString(Constants.VEHICLE_NAME, null);
        resetRating.setOnClickListener(view -> ratingBar.setRating(0f));
        ratingBar.setOnRatingBarChangeListener((rb, rating, user) -> {
            if(nickname.isEmpty() && rating > 0) {
                ratingBar.setRating(0f);
                Snackbar.make(localView, "Nickname required", Snackbar.LENGTH_SHORT).show();
            }
        });

        etServiceComment.setOnFocusChangeListener((view, b) -> {
            if(b && TextUtils.isEmpty(nickname)) {
                Snackbar.make(localView, "Nickname required", Snackbar.LENGTH_SHORT).show();
                view.clearFocus();
            }
        });


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

        dialog = new AlertDialog.Builder(getActivity())
                .setView(localView)
                .setPositiveButton(R.string.dialog_btn_confirm, null)
                .setNegativeButton(R.string.dialog_btn_cancel, null)
                .create();

        // Separately handle the positive button for preventing the dialog from closing when pressed
        // to receive the location by getGeocoderLocation of LocationViewModel. On fetching the value,
        // close the dialog using dismiss();
        dialog.setOnShowListener(dialogInterface -> {
            Button btn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

            btn.setOnClickListener(view -> {
                if(isCurrentLocation && mLocation != null && mAddress != null) {
                    // Pass the location and address of an service provider to ServiceManagerFragment
                    // using FragmentSharedModel which enables Fragments to communicate each other.
                    log.i("Current Location process: %s, %s", mLocation, mAddress);
                    sendServiceLocation();
                    dialog.dismiss();

                } else {
                    mAddress = sidoSpinner.getSelectedItem() + " "
                            + sigunAdapter.getItem(tmpSigunPos).getDistrictName() + " "
                            + etAddrs.getText();

                    // Initiate Geocoder to fetch Location based upon a given address name, the reuslt
                    // of which is to be sent to getGeocoderLocation of LocationViewModel as a LiveData.
                    geocoderTask = ThreadManager.startGeocoderTask(getContext(), locationModel, mAddress);

                }
            });
        });

        return dialog;
    }

    @Override
    public void onStop() {
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

    private void sendServiceLocation() {

        SparseArray<Object> sparseArray = new SparseArray<>();
        sparseArray.put(LOCATION, mLocation);
        sparseArray.put(ADDRESS, mAddress);
        sparseArray.put(RATING, ratingBar.getRating());
        sparseArray.put(COMMENT, etServiceComment.getText().toString());

        fragmentModel.setServiceLocation(sparseArray);
    }

}
