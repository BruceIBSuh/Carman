package com.silverback.carman2.fragments;


import android.app.Dialog;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
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
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.silverback.carman2.ExpenseActivity;
import com.silverback.carman2.R;
import com.silverback.carman2.adapters.DistrictSpinnerAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.threads.ThreadTask;
import com.silverback.carman2.utils.Constants;
import com.silverback.carman2.models.FragmentSharedModel;
import com.silverback.carman2.models.LocationViewModel;
import com.silverback.carman2.models.SpinnerDistrictModel;
import com.silverback.carman2.threads.GeocoderReverseTask;
import com.silverback.carman2.threads.GeocoderTask;
import com.silverback.carman2.threads.DistCodeSpinnerTask;
import com.silverback.carman2.threads.ThreadManager;

import java.util.HashMap;
import java.util.Map;

/**
 * A simple {@link Fragment} subclass.
 */
public class RegisterDialogFragment extends DialogFragment implements
        AdapterView.OnItemSelectedListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(RegisterDialogFragment.class);

    // Constants
    static final int SVC_ID = 0;
    static final int LOCATION = 1;
    static final int ADDRESS = 2;
    static final int COMPANY = 3;
    static final int RATING = 4;
    static final int COMMENT = 5;


    // Objects
    private SharedPreferences mSettings;
    private FirebaseFirestore firestore;
    private FragmentSharedModel fragmentModel;
    private DistCodeSpinnerTask spinnerTask;
    private GeocoderReverseTask geocoderReverseTask;
    private GeocoderTask geocoderTask;
    private ThreadTask locationTask;
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
    private Spinner sidoSpinner, sigunSpinner, companySpinner;
    private TextView tvSido, tvSigun;
    private EditText etAddrs, etPhone, etServiceComment;
    private RatingBar ratingBar;

    // Fields
    private int category;
    private int mSidoItemPos, mSigunItemPos, tmpSidoPos, tmpSigunPos;
    private boolean isCurrentLocation;
    private boolean isRegistered;

    // Default constructor
    private RegisterDialogFragment() {
        // Required empty public constructor
    }

    // Instantiate DialogFragment as a SingleTon
    static RegisterDialogFragment newInstance(String name, String distCode) {

        RegisterDialogFragment favoriteDialog = new RegisterDialogFragment();
        Bundle args = new Bundle();
        args.putString("favoriteName", name);
        args.putString("distCode", distCode);
        //args.putInt("category", category);
        favoriteDialog.setArguments(args);

        return favoriteDialog;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        providerName = getArguments().getString("favoriteName");
        distCode = getArguments().getString("distCode");
        //category = getArguments().getInt("category");
        mSettings = ((ExpenseActivity)getActivity()).getSettings();

        // Instantiate FirebaseFirestore
        firestore = FirebaseFirestore.getInstance();

        // ViewModel to fetch the sigun list of a given sido name
        fragmentModel = ViewModelProviders.of(getActivity()).get(FragmentSharedModel.class);
        distModel = ViewModelProviders.of(this).get(SpinnerDistrictModel.class);
        locationModel = ViewModelProviders.of(this).get(LocationViewModel.class);


    }

    @SuppressWarnings("ConstantConditions")
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        //LayoutInflater inflater = requireActivity().getLayoutInflater();
        //View localView = inflater.inflate(R.layout.dialog_register_provider, null);
        View localView = View.inflate(getContext(), R.layout.dialog_register_provider, null);
        setCancelable(false);

        TextView tvTitle = localView.findViewById(R.id.tv_title);
        ImageButton btnLocation = localView.findViewById(R.id.btn_current_location);
        //pbRegister = localView.findViewById(R.id.pb_register);
        tvSido = localView.findViewById(R.id.tv_sido);
        tvSigun = localView.findViewById(R.id.tv_sigun);
        sidoSpinner = localView.findViewById(R.id.spinner_sido);
        sigunSpinner= localView.findViewById(R.id.spinner_sigun);
        etAddrs = localView.findViewById(R.id.et_addrs_detail);
        etPhone = localView.findViewById(R.id.et_phone);
        companySpinner = localView.findViewById(R.id.spinner_company);
        ratingBar = localView.findViewById(R.id.rb_service);
        Button resetRating = localView.findViewById(R.id.btn_reset_ratingbar);
        etServiceComment = localView.findViewById(R.id.et_service_comment);

        // Create ProgressBar
        ConstraintLayout layout = localView.findViewById(R.id.rootview_register);
        ProgressBar pbRegister = new ProgressBar(getContext(), null, android.R.attr.progressBarStyleSmall);
        pbRegister.setIndeterminate(true);
        pbRegister.setVisibility(View.VISIBLE);
        ConstraintLayout.LayoutParams  params = new ConstraintLayout.LayoutParams(20, 20);
        params.horizontalBias = 0.5f;
        params.verticalBias = 0.5f;
        layout.addView(pbRegister, params);


        tvTitle.setText(providerName);

        // Event Handlers
        sidoSpinner.setOnItemSelectedListener(this);
        sigunSpinner.setOnItemSelectedListener(this);
        btnLocation.setOnClickListener(view -> {
            isCurrentLocation = true;
            locationTask = ThreadManager.fetchLocationTask(getContext(), locationModel);
        });

        // RatingBar and Comment are required to have a nickname set in SettingPreferenceActivity.
        nickname = mSettings.getString(Constants.USER_NAME, null);
        resetRating.setOnClickListener(view -> ratingBar.setRating(0f));
        ratingBar.setOnRatingBarChangeListener((rb, rating, user) -> {
            log.i("Nickname required: %s", nickname);
            if(TextUtils.isEmpty(nickname) && rating > 0) {
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


        // Create the spinners for sido and sigun name.
        ArrayAdapter sidoAdapter = ArrayAdapter.createFromResource(
                getContext(), R.array.sido_name, R.layout.spinner_svc_dialog);
        sidoAdapter.setDropDownViewResource(R.layout.spinner_svc_dlg_dropdown);
        sidoSpinner.setAdapter(sidoAdapter);
        sidoSpinner.setSelection(mSidoItemPos);

        sigunAdapter = new DistrictSpinnerAdapter(getContext());

        // Create the spinner for Comany list.
        ArrayAdapter companyAdapter = ArrayAdapter.createFromResource(
                getContext(), R.array.svc_company, R.layout.spinner_svc_dialog);
        companyAdapter.setDropDownViewResource(R.layout.spinner_svc_dlg_dropdown);
        companySpinner.setAdapter(companyAdapter);

        dialog = new AlertDialog.Builder(getActivity())
                .setView(localView)
                .setPositiveButton(R.string.dialog_btn_confirm, null)
                .setNegativeButton(R.string.dialog_btn_cancel, null)
                .create();

        // Separately handle the button actions to prevent the dialog from closing when pressed
        // to receive the location by getGeocoderLocation of LocationViewModel. On fetching the value,
        // close the dialog using dismiss();
        dialog.setOnShowListener(dialogInterface -> {
            Button btn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            btn.setOnClickListener(view -> {

                if(isCurrentLocation && mLocation != null && mAddress != null) {
                    log.i("Current Location process: %s, %s", mLocation, mAddress);
                    registerService();
                    //dialog.dismiss();

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
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Enlist the sigun names in SigunSpinner based upon a given sigun name.
        distModel.getSpinnerDataList().observe(this, dataList -> {
            if(sigunAdapter.getCount() > 0) sigunAdapter.removeAll();
            sigunAdapter.addSigunList(dataList);
            /*
            for(Opinet.DistrictCode obj : dataList) {
                //sigunAdapter.addItem(obj);
            }
             */
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
            registerService();
        });


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
            spinnerTask = ThreadManager.loadDistCodeSpinnerTask(getContext(), distModel, position);
            if(position != mSidoItemPos) mSigunItemPos = 0;
            tmpSidoPos = position;
        } else {
            tmpSigunPos = position;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {}

    // Share the data of the dialog with ServiceManagerFragment via FragmentSharedModel;
    private void registerService() {

        Map<String, Object> svcData = new HashMap<>();
        svcData.put("svc_name", providerName);
        svcData.put("svc_code", companySpinner.getSelectedItem().toString());
        svcData.put("address", mAddress);
        svcData.put("phone", etPhone.getText().toString());
        svcData.put("geopoint", new GeoPoint(mLocation.getLatitude(), mLocation.getLongitude()));

        firestore.collection("svc_center")
                .whereEqualTo("svcName", providerName)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if(snapshot != null && snapshot.size() > 0) {
                        for(int i = 0; i < snapshot.size(); i++) {
                            QueryDocumentSnapshot doc = (QueryDocumentSnapshot)snapshot.getDocuments().get(i);
                            // If a service center with the same name is queried and its location is within
                            // Constants.UPDATE_DISTANCE. isRegistered is set to true, and if no service
                            // center is queried or the same name is queried but the location is out of
                            // Constant.UPDATE_DISTANCE, isRegistered is set to false.
                            isRegistered = checkArea(mLocation, doc.getGeoPoint("location"));
                            log.i("Registered: %s", isRegistered);
                        }

                    } else isRegistered = false;

                    // In case isRegistered is set to false, add the service data, then pass the evaluation
                    // data including the id as SparseArray to ServiceManagerFragment to upload.
                    if(!isRegistered) {
                        firestore.collection("svc_center").add(svcData)
                                .addOnSuccessListener(docRef -> {

                                    // Getting the ID, pass the data as SparseArray to ServiceManagerFragment
                                    // to upload them to Firestore.`
                                    String generatedId = docRef.getId();
                                    log.i("Service ID: %s", generatedId);
                                    SparseArray<Object> sparseArray = new SparseArray<>();
                                    sparseArray.put(SVC_ID, generatedId);
                                    sparseArray.put(LOCATION, mLocation);
                                    sparseArray.put(ADDRESS, mAddress);
                                    sparseArray.put(COMPANY, companySpinner.getSelectedItem().toString());
                                    sparseArray.put(RATING, ratingBar.getRating());
                                    sparseArray.put(COMMENT, etServiceComment.getText().toString());

                                    fragmentModel.setServiceLocation(sparseArray);
                                    dialog.dismiss();

                                })
                                .addOnFailureListener(e -> log.e("Add service data failed: %s", e.getMessage()));
                    }


                }).addOnFailureListener(e -> {
                    log.e("Query failed: %s", e.getMessage());
                });
    }

    // After querying the document with a service name, retrieve the geopoint to compare the current
    // location, whatever it gets from the reverse geocoder or LocationServices, with the geopoint
    // location. Then, if the distance is out of the preset distance, set the data to Firestore.
    private boolean checkArea(Location location, GeoPoint geoPoint) {
        if(location == null || geoPoint == null) {
            log.e("Incorrect address or location data");
            return false;
        }

        Location geoLocation = new Location("geopoint");
        geoLocation.setLatitude(geoPoint.getLatitude());
        geoLocation.setLongitude(geoPoint.getLongitude());

        return location.distanceTo(geoLocation) < Constants.UPDATE_DISTANCE;
    }

    private void handleProgressDialog() {

    }

}
