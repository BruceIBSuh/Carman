package com.silverback.carman.fragments;


import android.app.Dialog;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.LayoutInflater;
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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.silverback.carman.BaseActivity;
import com.silverback.carman.R;
import com.silverback.carman.adapters.SigunSpinnerAdapter;
import com.silverback.carman.databinding.DialogRegisterProviderBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.threads.DistCodeSpinnerTask;
import com.silverback.carman.threads.GeocoderReverseTask;
import com.silverback.carman.threads.GeocoderTask;
import com.silverback.carman.threads.ThreadManager;
import com.silverback.carman.threads.ThreadManager2;
import com.silverback.carman.threads.ThreadTask;
import com.silverback.carman.utils.Constants;
import com.silverback.carman.viewmodels.FragmentSharedModel;
import com.silverback.carman.viewmodels.LocationViewModel;
import com.silverback.carman.viewmodels.OpinetViewModel;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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
    private DialogRegisterProviderBinding binding;
    private SharedPreferences mSettings;
    private FirebaseFirestore firestore;
    private FragmentSharedModel fragmentModel;
    private DistCodeSpinnerTask spinnerTask;
    private GeocoderReverseTask geocoderReverseTask;
    private GeocoderTask geocoderTask;
    private ThreadTask locationTask;
    //private SpinnerDistrictModel distModel;
    private OpinetViewModel opinetModel;
    private LocationViewModel locationModel;
    private SigunSpinnerAdapter sigunAdapter;
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
    private int mSidoItemPos, mSigunItemPos, tmpSigunPos;
    private boolean isCurrentLocation;
    private boolean isRegistered;

    // Default constructor
    private RegisterDialogFragment() {
        // Required empty public constructor
    }


    // Instantiate DialogFragment as a SingleTon
    static RegisterDialogFragment newInstance(String name, String distCode) {
        RegisterDialogFragment dialogFragment = new RegisterDialogFragment();
        Bundle args = new Bundle();
        args.putString("favoriteName", name);
        args.putString("distCode", distCode);
        log.i("distCode: %s", distCode);
        //args.putInt("category", category);
        dialogFragment.setArguments(args);

        return dialogFragment;
    }



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        providerName = getArguments().getString("favoriteName");
        distCode = getArguments().getString("distCode");
        //category = getArguments().getInt("category");
        mSettings = ((BaseActivity)Objects.requireNonNull(requireActivity())).getSharedPreferernces();

        // Instantiate FirebaseFirestore
        firestore = FirebaseFirestore.getInstance();

        // ViewModel to fetch the sigun list of a given sido name
        fragmentModel = new ViewModelProvider(this).get(FragmentSharedModel.class);
        //distModel = new ViewModelProvider(this).get(SpinnerDistrictModel.class);
        opinetModel = new ViewModelProvider(this).get(OpinetViewModel.class);
        locationModel = new ViewModelProvider(this).get(LocationViewModel.class);
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        binding = DialogRegisterProviderBinding.inflate(inflater);
        setCancelable(false);

        binding.tvNumpadTitle.setText(providerName);

        // Event Handlers
        binding.spinnerSido.setOnItemSelectedListener(this);
        binding.spinnerSigun.setOnItemSelectedListener(this);
        binding.btnCurrentLocation.setOnClickListener(view -> {
            isCurrentLocation = true;
            locationTask = ThreadManager2.getInstance().fetchLocationTask(getContext(), locationModel);
        });

        binding.btnCurrentLocation.setOnClickListener(view -> {
            isCurrentLocation = true;
            locationTask = ThreadManager2.getInstance().fetchLocationTask(requireContext(), locationModel);
        });

        nickname = mSettings.getString(Constants.USER_NAME, null);
        binding.expenseBtnResetRatingbar.setOnClickListener(view -> binding.rbService.setRating(0f));
        binding.rbService.setOnRatingBarChangeListener((rb, rating, user) -> {
            if(TextUtils.isEmpty(nickname) && rating > 0) {
                binding.rbService.setRating(0f);
                Snackbar.make(binding.getRoot(), "Nickname required", Snackbar.LENGTH_SHORT).show();
            }
        });

        binding.expenseEtServiceComment.setOnFocusChangeListener((view, b) -> {
            if(b && TextUtils.isEmpty(nickname)) {
                Snackbar.make(binding.getRoot(), "Nickname required", Snackbar.LENGTH_SHORT).show();
                view.clearFocus();
            }
        });

        // Create ProgressBar dynamically
        createRegisterProgressBar();
        createDistrictSpinners();


//        String sidoCode = distCode.substring(0, 2);
//        String sigunCode = distCode.substring(2, 4);
//        mSidoItemPos = Integer.parseInt(sidoCode) - 1; // "01" is translated into 1 as Integer.
//        mSigunItemPos = Integer.parseInt(sigunCode) -1;
//
//        // Create the spinners for sido and sigun name.
//        ArrayAdapter<CharSequence> sidoAdapter = ArrayAdapter.createFromResource(
//                getContext(), R.array.sido_name, R.layout.spinner_district_entry);
//        sidoAdapter.setDropDownViewResource(R.layout.spinner_district_dropdown);
//        sidoSpinner.setAdapter(sidoAdapter);
//        sidoSpinner.setSelection(mSidoItemPos);
//
//        sigunAdapter = new SigunSpinnerAdapter(getContext());
//
//        // Create the spinner for Comany list.
//        ArrayAdapter<CharSequence> companyAdapter = ArrayAdapter.createFromResource(
//                getContext(), R.array.svc_company, R.layout.spinner_district_entry);
//        companyAdapter.setDropDownViewResource(R.layout.spinner_district_dropdown);
//        companySpinner.setAdapter(companyAdapter);


        //createDistrictSpinners();

        dialog = new AlertDialog.Builder(requireContext())
                .setView(binding.getRoot())
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
                    //log.i("Current Location process: %s, %s", mLocation, mAddress);
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
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        log.i("onViewCreated invoked?");
        // Enlist the sigun names in SigunSpinner based upon a given sigun name.
        opinetModel.getSpinnerDataList().observe(this, dataList -> {
            if(sigunAdapter.getCount() > 0) sigunAdapter.removeAll();
            sigunAdapter.addSigunList(dataList);
//            for(Opinet.DistrictCode obj : dataList) {
//                //sigunAdapter.addItem(obj);
//            }
            sigunSpinner.setAdapter(sigunAdapter);
            sigunSpinner.setSelection(mSigunItemPos);
        });

        // Fetch the current location using LocationTask and LocationViewModel, with which
        // GeocoderReverseTask is initiated to get the current address.
        locationModel.getLocation().observe(this, location->{
            //log.i("Current Location: %s", location);
            geocoderReverseTask = ThreadManager.startReverseGeocoderTask(getContext(), locationModel, location);
            mLocation = location;

        });

        // Fetch the current address and split it for inputting sido and sigun name respectively into
        // its TextViews which replace the Spinners. Using StringBuffer, insertAutoMaker the space between
        // the remaining address names.
        locationModel.getAddress().observe(this, address -> {
            mAddress = address;

            final String[] arrAddrs = TextUtils.split(address, "\\s+");
            final StringBuilder strbldr = new StringBuilder();
            for(int i = 2; i < arrAddrs.length; i++) strbldr.append(arrAddrs[i]).append(" ");

            binding.tvSido.setText(arrAddrs[0]);
            binding.tvSigun.setText(arrAddrs[1]);
            binding.etAddrsDetail.setText(strbldr.toString());

            binding.tvSido.setVisibility(View.VISIBLE);
            binding.tvSigun.setVisibility(View.VISIBLE);
            binding.spinnerSido.setVisibility(View.GONE);
            binding.spinnerSigun.setVisibility(View.GONE);
        });

        // Fetch the Location based on a given address name by using Geocoder, then pass the value
        // to ServiceManagerFragment and close the dialog.
        locationModel.getGeocoderLocation().observe(this, location -> {
            mLocation = location;
            // Pass the location and address of an service provider to ServiceManagerFragment
            // using FragmentSharedModel which enables Fragments to communicate each other.
            //log.i("Geocoder Location: %s, %s", mLocation, mAddress);
            registerService();
        });


    }


    @Override
    public void onPause() {
        if(spinnerTask != null) spinnerTask = null;
        if(locationTask != null) locationTask = null;
        if(geocoderReverseTask != null) geocoderReverseTask = null;
        if(geocoderTask != null) geocoderTask = null;

        super.onPause();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if(parent == binding.spinnerSido) {
            log.i("onItemSelected");
            //spinnerTask = ThreadManager.loadDistCodeSpinnerTask(getContext(), opinetModel, position);
            spinnerTask = ThreadManager2.getInstance().loadDistSpinnerTask(getContext(), opinetModel, position);
            if(position != mSidoItemPos) mSigunItemPos = 0;
            //int tmpSidoPos = position;
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
                            //log.i("Registered: %s", isRegistered);
                        }

                    } else isRegistered = false;

                    // In case isRegistered is set to false, add the service data, then pass the evaluation
                    // data including the id as SparseArray to ServiceManagerFragment to upload.
                    if(!isRegistered) {
                        firestore.collection("svc_center").add(svcData).addOnSuccessListener(docRef -> {
                            // Getting the ID, pass the data as SparseArray to ServiceManagerFragment
                            // to upload them to Firestore.`
                            String generatedId = docRef.getId();
                            SparseArray<Object> sparseArray = new SparseArray<>();
                            sparseArray.put(SVC_ID, generatedId);
                            sparseArray.put(LOCATION, mLocation);
                            sparseArray.put(ADDRESS, mAddress);
                            sparseArray.put(COMPANY, companySpinner.getSelectedItem().toString());
                            sparseArray.put(RATING, ratingBar.getRating());
                            sparseArray.put(COMMENT, etServiceComment.getText().toString());

                            fragmentModel.setServiceLocation(sparseArray);
                            dialog.dismiss();
                        }).addOnFailureListener(Exception::printStackTrace);
                    }
                }).addOnFailureListener(Exception::printStackTrace);
    }

    private void createRegisterProgressBar() {
        ProgressBar pbRegister = new ProgressBar(requireContext(), null, android.R.attr.progressBarStyleSmall);
        pbRegister.setIndeterminate(true);
        pbRegister.setVisibility(View.VISIBLE);
        ConstraintLayout.LayoutParams  params = new ConstraintLayout.LayoutParams(20, 20);
        params.horizontalBias = 0.5f;
        params.verticalBias = 0.5f;
        binding.rootviewRegister.addView(pbRegister, params);

    }

    // Create the spinners
    private void createDistrictSpinners() {
        String sidoCode = distCode.substring(0, 2);
        String sigunCode = distCode.substring(2, 4);
        mSidoItemPos = Integer.parseInt(sidoCode) - 1; // "01" is translated into 1 as Integer.
        mSigunItemPos = Integer.parseInt(sigunCode) -1;

        // Create the spinners for sido and sigun name.
        ArrayAdapter<CharSequence> sidoAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.sido_name, R.layout.spinner_district_entry);
        sidoAdapter.setDropDownViewResource(R.layout.spinner_district_dropdown);
        binding.spinnerSido.setAdapter(sidoAdapter);
        binding.spinnerSido.setSelection(mSidoItemPos);

        sigunAdapter = new SigunSpinnerAdapter(getContext());

        // Create the spinner for Comany list.
        ArrayAdapter<CharSequence> companyAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.svc_company, R.layout.spinner_district_entry);
        companyAdapter.setDropDownViewResource(R.layout.spinner_district_dropdown);
        binding.spinnerCompany.setAdapter(companyAdapter);
    }

    // After querying the document with a service name, retrieve the geopoint to compare the current
    // location, whatever it gets from the reverse geocoder or LocationServices, with the geopoint
    // location. Then, if the distance is out of the preset distance, set the data to Firestore.
    private boolean checkArea(Location location, GeoPoint geoPoint) {
        if(location == null || geoPoint == null) {
            //log.e("Incorrect address or location data");
            return false;
        }

        Location geoLocation = new Location("geopoint");
        geoLocation.setLatitude(geoPoint.getLatitude());
        geoLocation.setLongitude(geoPoint.getLongitude());

        return location.distanceTo(geoLocation) < Constants.UPDATE_DISTANCE;
    }
}
