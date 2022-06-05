package com.silverback.carman.fragments;


import android.app.Dialog;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.silverback.carman.R;
import com.silverback.carman.adapters.SigunSpinnerAdapter;
import com.silverback.carman.databinding.DialogRegisterProviderBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.threads.DistCodeSpinnerTask;
import com.silverback.carman.threads.GeocoderReverseTask;
import com.silverback.carman.threads.GeocoderTask;
import com.silverback.carman.threads.ThreadManager2;
import com.silverback.carman.threads.ThreadTask;
import com.silverback.carman.utils.Constants;
import com.silverback.carman.viewmodels.FragmentSharedModel;
import com.silverback.carman.viewmodels.LocationViewModel;
import com.silverback.carman.viewmodels.OpinetViewModel;

import java.util.HashMap;
import java.util.Map;

/**
 * A simple {@link Fragment} subclass.
 */
public class RegisterDialogFragment extends DialogFragment implements
        AdapterView.OnItemSelectedListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(RegisterDialogFragment.class);

    // Objects
    private DialogRegisterProviderBinding binding;
    private SharedPreferences mSettings;
    private FirebaseFirestore firestore;
    private FragmentSharedModel fragmentModel;
    private DistCodeSpinnerTask spinnerTask;
    private GeocoderReverseTask geoReverseTask;
    private GeocoderTask geocoderTask;
    private ThreadTask locationTask;
    private OpinetViewModel opinetModel;
    private LocationViewModel locationModel;
    private SigunSpinnerAdapter sigunAdapter;
    private AlertDialog dialog;
    private Location mLocation;
    private String mAddress;
    private String svcName;
    private String distCode;
    private String nickname;

    // Fields
    private int mSidoItemPos, mSigunItemPos, tmpSigunPos;
    private boolean isLocationFetched;
    private boolean isRegistered;

    // Default constructor
    private RegisterDialogFragment() {
        // Required empty public constructor
    }

    // Instantiate DialogFragment as a SingleTon which passes args to onCreate() as Bundle
    static RegisterDialogFragment newInstance(String name, String distCode) {
        RegisterDialogFragment dialogFragment = new RegisterDialogFragment();
        Bundle args = new Bundle();
        args.putString("provider", name);
        args.putString("distCode", distCode);
        dialogFragment.setArguments(args);
        return dialogFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        svcName = requireArguments().getString("provider");
        distCode = requireArguments().getString("distCode");
        mSettings = PreferenceManager.getDefaultSharedPreferences(requireContext());
        firestore = FirebaseFirestore.getInstance();

        // ViewModel to fetch the sigun list of a given sido name
        fragmentModel = new ViewModelProvider(requireActivity()).get(FragmentSharedModel.class);
        opinetModel = new ViewModelProvider(this).get(OpinetViewModel.class);
        locationModel = new ViewModelProvider(this).get(LocationViewModel.class);
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getLayoutInflater();
        binding = DialogRegisterProviderBinding.inflate(inflater);
        setCancelable(false);

        nickname = mSettings.getString(Constants.USER_NAME, null);
        binding.tvRegisterTitle.setText(svcName);
        createDistrictSpinners();// Create the spinners for Sido, Sigun and entity.

        // Event Handlers
        binding.spinnerSido.setOnItemSelectedListener(this);
        binding.spinnerSigun.setOnItemSelectedListener(this);
        binding.btnCurrentLocation.setOnClickListener(view -> setReverseGeocoderAddress());
        binding.expenseBtnResetRatingbar.setOnClickListener(view -> binding.rbService.setRating(0f));
        binding.rbService.setOnRatingBarChangeListener((rb, rating, user) -> {
            if(TextUtils.isEmpty(nickname) && rating > 0) {
                binding.rbService.setRating(0f);
                Snackbar.make(binding.getRoot(), "Nickname required", Snackbar.LENGTH_SHORT).show();
            }
        });

        binding.etServiceComment.setOnFocusChangeListener((view, b) -> {
            if(b && TextUtils.isEmpty(nickname)) {
                Snackbar.make(binding.getRoot(), "Nickname required", Snackbar.LENGTH_SHORT).show();
                view.clearFocus();
            }
        });

        // Create AlertDialog with a custom view
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
            btn.setOnClickListener(view -> setGeocoderLocation());
        });

        return dialog;
    }

    // Without onCreateView() defined in DialogFragment, onViewCreated will not be invokked!!!
    /*
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }
     */

    @Override
    public void onPause() {
        if(spinnerTask != null) spinnerTask = null;
        if(locationTask != null) locationTask = null;
        if(geoReverseTask != null) geoReverseTask = null;
        if(geocoderTask != null) geocoderTask = null;
        super.onPause();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        if(parent == binding.spinnerSido) {
            spinnerTask = ThreadManager2.loadDistrictSpinnerTask(getContext(), opinetModel, pos);
            if(pos != mSidoItemPos) mSigunItemPos = 0;
        } else tmpSigunPos = pos;
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {}

    // Create ProgressBar when clicking the confirm button on the Dialog.
    private void setRegisterProgressBar() {
        ProgressBar progressBar = new ProgressBar(getContext(), null, android.R.attr.progressBarStyleSmall);
        progressBar.setIndeterminate(true);
        ConstraintLayout.LayoutParams  params = new ConstraintLayout.LayoutParams(100, 100);
        params.topToTop = binding.getRoot().getId();
        params.bottomToBottom = binding.getRoot().getId();
        params.leftToLeft = binding.getRoot().getId();
        params.rightToRight = binding.getRoot().getId();
        params.horizontalBias = 0.5f;
        params.verticalBias = 0.5f;
        binding.rootviewRegister.addView(progressBar, params);
    }

    // Create the spinners which display the sido, sigun and company names respectively. The sigun
    // name list depend on which sido to select, which works on a worker thread.
    private void createDistrictSpinners() {
        String sidoCode = distCode.substring(0, 2);
        String sigunCode = distCode.substring(2, 4);
        mSidoItemPos = Integer.parseInt(sidoCode) - 1; // "01" is translated into 1 as Integer.
        mSigunItemPos = Integer.parseInt(sigunCode) -1;

        // Create the spinners for the Sido names.
        ArrayAdapter<CharSequence> sidoAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.sido_name, R.layout.spinner_settings_entry);
        sidoAdapter.setDropDownViewResource(R.layout.spinner_settings_dropdown);
        binding.spinnerSido.setAdapter(sidoAdapter);
        binding.spinnerSido.setSelection(mSidoItemPos);

        opinetModel.getSpinnerDataList().observe(this, sigunList -> {
            sigunAdapter = new SigunSpinnerAdapter(getContext());
            sigunAdapter.addSigunList(sigunList);
            binding.spinnerSigun.setAdapter(sigunAdapter);
        });

        // Create the spinner for Comany list.
        ArrayAdapter<CharSequence> companyAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.svc_company, R.layout.spinner_settings_entry);
        companyAdapter.setDropDownViewResource(R.layout.spinner_settings_dropdown);
        binding.spinnerCompany.setAdapter(companyAdapter);
    }

    // Make te reverse geocoding process to get the official address of an service station based on
    // the current geo-location (latitude and longitude)
    private void setReverseGeocoderAddress() {
        //setRegisterProgressBar(true);
        isLocationFetched = true;
        locationTask = ThreadManager2.fetchLocationTask(getContext(), locationModel);
        locationModel.getLocation().observe(this, location -> {
            mLocation = location;
            geoReverseTask = ThreadManager2.startReverseGeocoderTask(getContext(), locationModel, location);
        });

        // Fetch the current address and split it for inputting sido and sigun name respectively into
        // its TextViews which replace the Spinners. Using StringBuffer, insertAutoMaker the space between
        // the remaining address names.
        locationModel.getAddress().observe(this, address -> {
            log.i("Address: %s", address);
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


    }

    // Make the geocoding process to get a location based on the address.
    private void setGeocoderLocation() {
        setRegisterProgressBar();
        if(isLocationFetched && mLocation != null && mAddress != null) {
            log.i("Current Location process: %s, %s", mLocation, mAddress);
            registerService();
        } else {
            mAddress = binding.spinnerSido.getSelectedItem() + " "
                    + sigunAdapter.getItem(tmpSigunPos).getDistrictName() + " "
                    + binding.etAddrsDetail.getText();
            // Initiate Geocoder to fetch Location based upon a given address name, the reuslt
            // of which is to be sent to getGeocoderLocation of LocationViewModel as a LiveData.
            geocoderTask = ThreadManager2.getInstance().startGeocoderTask(getContext(), locationModel, mAddress);

            // Fetch the Location based on a given address name by using Geocoder, then pass the value
            // to ExpenseServiceFragment and close the dialog.
            locationModel.getGeocoderLocation().observe(this, location -> {
                mLocation = location;
                // Pass the location and address of an service provider to ExpenseServiceFragment
                // using FragmentSharedModel which enables Fragments to communicate each other.
                log.i("Geocoder Location: %s, %s", mLocation, mAddress);
                registerService();
            });

        }
    }

    // After querying the document with a service name, retrieve the geopoint to compare the current
    // location, whatever it gets from the reverse geocoder or LocationServices, with the geopoint
    // location. Then, if the distance is out of the preset distance, set the data to Firestore.
    private boolean checkNearArea(GeoPoint geoPoint) {
        if(mLocation == null || geoPoint == null) {
            log.e("Incorrect address or location data");
            return false;
        }

        Location geoLocation = new Location("geopoint");
        geoLocation.setLatitude(geoPoint.getLatitude());
        geoLocation.setLongitude(geoPoint.getLongitude());
        return mLocation.distanceTo(geoLocation) < Constants.UPDATE_DISTANCE;
    }


    private void registerService() {
        // Do empty check.
        isRegistered = false;
        if(TextUtils.isEmpty(binding.etAddrsDetail.getText())) {
            Snackbar.make(binding.getRoot(), "Empty address", Snackbar.LENGTH_SHORT).show();
            return;
        }

        // Query the Service Station with service name. If the same name exists, check if the station
        // is within the preset distance(50m), it should be regarded as the same one.
        // Pay attention to the usage of whereGreaterThanOrEqualTo w/ String query.
        final CollectionReference colRef = firestore.collection("svc_station");
        colRef.whereGreaterThanOrEqualTo("svc_name", svcName).get().addOnSuccessListener(snapshot -> {
            if(snapshot.size() > 0) {
                for(QueryDocumentSnapshot doc : snapshot) {
                    GeoPoint geoPoint = (GeoPoint)doc.get("geopoint");
                    isRegistered = checkNearArea(geoPoint);
                    uploadServiceComment(doc.getId());
                    break;
                }

            } //else {

            if(!isRegistered) {
                Map<String, Object> svcData = new HashMap<>();
                svcData.put("svc_name", svcName);
                svcData.put("svc_entity", binding.spinnerCompany.getSelectedItem().toString());
                svcData.put("address", mAddress);
                svcData.put("phone", binding.etPhone.getText().toString());
                svcData.put("geopoint", new GeoPoint(mLocation.getLatitude(), mLocation.getLongitude()));

                firestore.collection("svc_station").add(svcData).addOnCompleteListener(docRef -> {
                    String docId = docRef.getResult().getId();
                    uploadServiceComment(docId);
                });
            }

        }).addOnFailureListener(Throwable::printStackTrace);

    }

    // On having queried or set a service station, upload any rating or comments to the difference
    // collection by WriteBatch.
    private void uploadServiceComment(String docId) {
        WriteBatch batch = firestore.batch();
        DocumentReference docRef = firestore.collection("svc_eval").document(docId);

        if(binding.rbService.getRating() > 0) {
            Map<String, Object> ratingData = new HashMap<>();
            ratingData.put("eval_num", FieldValue.increment(1));
            ratingData.put("eval_sum", FieldValue.increment(binding.rbService.getRating()));
            batch.set(docRef, ratingData, SetOptions.merge());
        }

        if(!binding.etServiceComment.getText().toString().isEmpty()) {
            DocumentReference commentRef = docRef.collection("comment").document();
            Map<String, Object> commentData = new HashMap<>();
            commentData.put("timestamp", FieldValue.serverTimestamp());
            commentData.put("name", mSettings.getString(Constants.USER_NAME, null));
            commentData.put("comments", binding.etServiceComment.getText().toString());
            commentData.put("rating", binding.rbService.getRating());
            batch.set(commentRef, commentData);
        }

        batch.commit().addOnCompleteListener(task -> {
            if(task.isSuccessful()) {
                fragmentModel.getRegisteredServiceId().setValue(docId);
                dialog.dismiss();
            } else Snackbar.make(binding.getRoot(), "Fail to upload", Snackbar.LENGTH_SHORT).show();
        });
    }
}
