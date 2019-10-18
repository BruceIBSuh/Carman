package com.silverback.carman2.fragments;

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
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.FragmentSharedModel;
import com.silverback.carman2.views.NameDialogPreference;

import static android.content.DialogInterface.BUTTON_POSITIVE;

public class SettingNameDlgFragment extends PreferenceDialogFragmentCompat {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(SettingNameDlgFragment.class);

    // Objects
    private FirebaseFirestore firestore;
    private NameDialogPreference namePreference;
    // UIs
    private ConstraintLayout rootView;
    private EditText etName;
    private Button btnVerify;

    // Fields
    private boolean isVerified = false;
    private String username;
    private boolean isNameExist;


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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firestore = FirebaseFirestore.getInstance();
    }

    @SuppressWarnings("ConstantConditions")
    @NonNull
    @Override
    public View onCreateDialogView(Context context) {
        log.i("onCreateDialogView");
        View localView = View.inflate(getContext(), R.layout.dialog_setting_edit_name, null);
        etName = localView.findViewById(R.id.et_user_name);
        rootView = localView.findViewById(R.id.rootview);
        btnVerify = localView.findViewById(R.id.btn_verify);

        namePreference = (NameDialogPreference)getPreference();

        etName.setText(getArguments().getString("username"));

        btnVerify.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            isVerified = true;

            // Query the name to check if there exists the same name in Firestore
            Query queryName = firestore.collection("users").whereEqualTo("user_name", name);
            queryName.get().addOnSuccessListener(snapshot -> {
                // A given name already exists
                if(snapshot.size() > 0) {
                    isNameExist = true;
                    ((AlertDialog)getDialog()).getButton(BUTTON_POSITIVE).setEnabled(false);
                    btnVerify.setEnabled(false);

                    Snackbar.make(rootView, "The same name is already occupied", Snackbar.LENGTH_SHORT).show();

                } else {
                    isNameExist = false;
                    ((AlertDialog)getDialog()).getButton(BUTTON_POSITIVE).setEnabled(true);
                    Snackbar.make(rootView, "Available", Snackbar.LENGTH_SHORT).show();
                }

            }).addOnFailureListener(e -> log.e("Query failed"));
        });

        return localView;

    }


    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        log.i("onBindDialogView: %s", view);

        etName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                btnVerify.setEnabled(true);
                ((AlertDialog)getDialog()).getButton(BUTTON_POSITIVE).setEnabled(true);
            }
        });
    }



    @Override
    public void onDialogClosed(boolean positiveResult) {

        if(positiveResult) {
            log.i("onDialogClosed");
            namePreference.callChangeListener(etName.getText());
        }
    }


}
