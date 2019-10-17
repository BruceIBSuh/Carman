package com.silverback.carman2.fragments;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.silverback.carman2.R;
import com.silverback.carman2.SettingPreferenceActivity;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.utils.Constants;

import static android.content.DialogInterface.BUTTON_POSITIVE;

public class SettingEditNameFragment extends DialogFragment {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(SettingEditNameFragment.class);

    // UIs
    private FirebaseFirestore firestore;
    private SharedPreferences mSettings;
    private ConstraintLayout rootView;
    private EditText etName;
    private Button btnPositive, btnVerify;

    // Fields
    private String username;
    private boolean isNameExist;


    // Default constructor
    public SettingEditNameFragment() {

    }

    // Method for singleton instance
    static SettingEditNameFragment newInstance(String summary) {

        final SettingEditNameFragment fm = new SettingEditNameFragment();
        final Bundle args = new Bundle(2);
        //args.putString("key", key); // ARG_KEY internally defined in onBindDialogView()
        args.putString("username", summary);
        fm.setArguments(args);

        return fm;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        firestore = FirebaseFirestore.getInstance();
        mSettings = ((SettingPreferenceActivity)getActivity()).getSettings();

    }

    @SuppressWarnings("ConstantConditions")
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View localView = View.inflate(getContext(), R.layout.dialog_setting_edit_name, null);
        etName = localView.findViewById(R.id.et_user_name);
        rootView = localView.findViewById(R.id.rootview);
        btnVerify = localView.findViewById(R.id.btn_verify);
        etName.setText(getArguments().getString("username"));

        btnVerify.setOnClickListener(v -> {

            String name = etName.getText().toString().trim();
            log.i("Clicked: %s", name);

            // Query the name to check if there exists the same name in Firestore
            Query queryName = firestore.collection("users").whereEqualTo("user_name", name);
            queryName.get().addOnSuccessListener(snapshot -> {
                if(snapshot.size() > 0) {
                    isNameExist = true;
                    ((AlertDialog)getDialog()).getButton(BUTTON_POSITIVE).setEnabled(false);
                    Snackbar.make(rootView, "The same name is already occupied", Snackbar.LENGTH_SHORT).show();

                } else {
                    isNameExist = false;
                    ((AlertDialog)getDialog()).getButton(BUTTON_POSITIVE).setEnabled(true);
                    Snackbar.make(rootView, "Available", Snackbar.LENGTH_SHORT).show();
                }

            }).addOnFailureListener(e -> log.e("Query failed"));
        });



        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("user(vehicle) nickname")
                .setView(localView)
                .setPositiveButton("confirm", (dialog, which) -> {
                    mSettings.edit().putString(Constants.VEHICLE_NAME, etName.getText().toString()).apply();
                })

                .setNegativeButton("cancel", (dialog, which) -> dismiss());

        return builder.create();

    }

}
