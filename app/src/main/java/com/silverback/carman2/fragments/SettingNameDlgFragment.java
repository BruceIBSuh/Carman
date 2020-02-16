package com.silverback.carman2.fragments;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.preference.PreferenceDialogFragmentCompat;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.silverback.carman2.R;
import com.silverback.carman2.SettingPreferenceActivity;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.utils.Constants;
import com.silverback.carman2.views.NameDialogPreference;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import static android.content.DialogInterface.BUTTON_POSITIVE;

public class SettingNameDlgFragment extends PreferenceDialogFragmentCompat {

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
    private String currentName, newName;

    // Default constructor
    private SettingNameDlgFragment() {
        // default private constructor
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
        currentName = getArguments().getString("username");
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
        etName.setText(currentName);
        etName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after){}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                // Activate the verification button when inputting any character.
                btnVerify.setEnabled(true);
                //dialog.getButton(BUTTON_POSITIVE).setEnabled(true);
            }
        });

        btnVerify.setOnClickListener(v -> {
            newName = etName.getText().toString().trim();
            // Query the name to check if there exists the same name in Firestore
            Query queryName = firestore.collection("users").whereEqualTo("user_name", newName);
            queryName.get().addOnSuccessListener(snapshot -> {
                // A given name already exists
                if(snapshot.size() > 0) {
                    dialog.getButton(BUTTON_POSITIVE).setEnabled(false);
                    btnVerify.setEnabled(false);
                    Snackbar.make(rootView, getString(R.string.pref_username_msg_invalid), Snackbar.LENGTH_SHORT).show();

                } else {
                    dialog.getButton(BUTTON_POSITIVE).setEnabled(true);
                    Snackbar.make(rootView, getString(R.string.pref_username_msg_available), Snackbar.LENGTH_SHORT).show();
                }

            }).addOnFailureListener(e -> log.e("Query failed"));
        });



        return dialog;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onDialogClosed(boolean positiveResult) {
        if(positiveResult) {
            log.i("onDialogClosed");
            mSettings.edit().putString(Constants.USER_NAME, etName.getText().toString()).apply();
            namePreference.callChangeListener(newName);
            // Delete the previous username from Firestore
            if(TextUtils.isEmpty(currentName) || currentName.equals(newName)) return;

            // When a new username has replaced the current name, update the new name in Firestore.
            try (FileInputStream fis = getActivity().openFileInput("userId");
                 BufferedReader br = new BufferedReader(new InputStreamReader(fis))) {
                String userId = br.readLine();
                DocumentReference docRef = firestore.collection("users").document(userId);
                docRef.update("user_name", newName).addOnCompleteListener(task -> {
                    if(task.isSuccessful()) {
                        log.i("username is succeessfully updated");
                    }
                });

            } catch(IOException e) {
                log.e("IOException: %s", e.getMessage());
            }



        }
    }

}
