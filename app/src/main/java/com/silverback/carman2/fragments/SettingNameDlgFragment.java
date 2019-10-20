package com.silverback.carman2.fragments;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.preference.PreferenceDialogFragmentCompat;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.silverback.carman2.R;
import com.silverback.carman2.SettingPreferenceActivity;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.FragmentSharedModel;
import com.silverback.carman2.utils.Constants;
import com.silverback.carman2.views.NameDialogPreference;

import java.io.File;

import static android.content.DialogInterface.BUTTON_POSITIVE;

public class SettingNameDlgFragment extends PreferenceDialogFragmentCompat implements View.OnClickListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(SettingNameDlgFragment.class);

    // Objects
    private SharedPreferences mSettings;
    private FirebaseFirestore firestore;
    private NameDialogPreference namePreference;

    // UIs
    private ConstraintLayout rootView;
    private EditText etName;
    private Button btnVerify;

    // Fields
    private String userName;

    // Default constructor
    public SettingNameDlgFragment() {

    }

    // Method for singleton instance
    static SettingNameDlgFragment newInstance(String key, String summary) {

        final SettingNameDlgFragment fm = new SettingNameDlgFragment();
        final Bundle args = new Bundle(2);
        args.putString("key", key); // ARG_KEY internally defined in onBindDialogView()
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
        userName = getArguments().getString("username");
    }

    @SuppressWarnings("ConstantConditions")
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);
        View localView = View.inflate(getContext(), R.layout.dialog_setting_edit_name, null);
        etName = localView.findViewById(R.id.et_user_name);
        rootView = localView.findViewById(R.id.rootview);
        btnVerify = localView.findViewById(R.id.btn_verify);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(localView)
                .setTitle(R.string.setting_nickname)
                .setPositiveButton("Confirm", this)
                .setNegativeButton("Cancel", this);

        AlertDialog dialog = builder.create();
        dialog.show();

        // Initial state of the buttons.
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);

        namePreference = (NameDialogPreference)getPreference();
        etName.setText(userName);
        etName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after){}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                btnVerify.setEnabled(true);
                //dialog.getButton(BUTTON_POSITIVE).setEnabled(true);
            }
        });

        btnVerify.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            // Query the name to check if there exists the same name in Firestore
            Query queryName = firestore.collection("users").whereEqualTo("user_name", name);
            queryName.get().addOnSuccessListener(snapshot -> {
                // A given name already exists
                if(snapshot.size() > 0) {
                    dialog.getButton(BUTTON_POSITIVE).setEnabled(false);
                    btnVerify.setEnabled(false);
                    Snackbar.make(rootView, "The same name is already occupied", Snackbar.LENGTH_SHORT).show();

                } else {
                    dialog.getButton(BUTTON_POSITIVE).setEnabled(true);
                    Snackbar.make(rootView, "Available", Snackbar.LENGTH_SHORT).show();
                }

            }).addOnFailureListener(e -> log.e("Query failed"));
        });



        return dialog;
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if(positiveResult) {
            log.i("onDialogClosed");
            //mSettings.edit().putString(Constants.USER_NAME, etName.getText().toString()).apply();
            namePreference.callChangeListener(etName.getText());
        }
    }


    @Override
    public void onClick(View v) {
    }
}
