package com.silverback.carman.fragments;

import static android.content.DialogInterface.BUTTON_POSITIVE;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceDialogFragmentCompat;
import androidx.preference.PreferenceManager;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.silverback.carman.R;
import com.silverback.carman.databinding.DialogSettingNameBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.views.NameDialogPreference;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class SettingNameDlgFragment extends PreferenceDialogFragmentCompat {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(SettingNameDlgFragment.class);

    private FirebaseFirestore mDB;
    private NameDialogPreference namePreference;
    private DialogSettingNameBinding binding;
    private String currentName, newName;

    // Default constructor
    private SettingNameDlgFragment() {
        // default private constructor
    }

    // Method for singleton instance
    static SettingNameDlgFragment newInstance(String key, String summary) {
        SettingNameDlgFragment fm = new SettingNameDlgFragment();
        Bundle args = new Bundle();
        args.putString("key", key); // ARG_KEY internally defined in onBindDialogView()
        args.putString("username", summary);
        fm.setArguments(args);

        return fm;
    }

    //@SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDB = FirebaseFirestore.getInstance();
        assert getArguments() != null;
        currentName = getArguments().getString("username");
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);
        binding = DialogSettingNameBinding.inflate(LayoutInflater.from(getContext()));

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setView(binding.getRoot())
                //.setTitle(R.string.pref_title_username)
                .setPositiveButton("Confirm", this)
                .setNegativeButton("Cancel", this);

        AlertDialog dialog = builder.create();
        dialog.show();

        // Initial state of the buttons.
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        namePreference = (NameDialogPreference)getPreference();

        binding.etUserName.setText(currentName);
        binding.etUserName.setOnFocusChangeListener((v, hasFocus) -> {
            if(hasFocus) binding.etUserName.setText("");
            else binding.etUserName.setHint(R.string.pref_hint_username);
        });
        binding.etUserName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after){}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                binding.btnVerify.setEnabled(true);
            }
        });

        binding.btnVerify.setOnClickListener(v -> {
            newName = binding.etUserName.getText().toString().trim();
            // Query the name to check if there exists the same name in Firestore
            //final Query queryName = mDB.collection("users").whereEqualTo("user_name", newName);
            Query queryName = mDB.collection("users").whereArrayContains("user_name", newName).limit(1);
            queryName.get().addOnSuccessListener(querySnapshot -> {
                if(querySnapshot.size() > 0) {
                    dialog.getButton(BUTTON_POSITIVE).setEnabled(false);
                    binding.btnVerify.setEnabled(false);
                    Snackbar.make(binding.getRoot(), getString(R.string.pref_username_msg_invalid), Snackbar.LENGTH_SHORT).show();
                } else {
                    dialog.getButton(BUTTON_POSITIVE).setEnabled(true);
                    Snackbar.make(binding.getRoot(), getString(R.string.pref_username_msg_available), Snackbar.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(e -> log.e("Query failed"));
        });

        return dialog;
    }

    //@SuppressWarnings("ConstantConditions")
    @Override
    public void onDialogClosed(boolean positiveResult) {
        if(positiveResult) {
            // Call this method after the user changes the preference, but before the internal state
            // is set. This allows the client to ignore the user value.
            //mSettings.edit().putString(Constants.USER_NAME, binding.etUserName.getText().toString()).apply();
            namePreference.callChangeListener(newName);

            // When a new username has replaced the current name, update the new name in Firestore.
            try (FileInputStream fis = requireActivity().openFileInput("userId");
                 BufferedReader br = new BufferedReader(new InputStreamReader(fis))) {
                String userId = br.readLine();
                final DocumentReference docref = mDB.collection("users").document(userId);
                Map<String, FieldValue> map = new HashMap<>();
                map.put(newName, FieldValue.serverTimestamp());
                docref.update("user_name", FieldValue.arrayUnion(newName), "rename_date", map)
                        .addOnSuccessListener(aVoid -> log.i("update done"))
                        .addOnFailureListener(Throwable::printStackTrace);
            }catch(IOException | NullPointerException e) { e.printStackTrace(); }
        }
    }

    /*
    private static class UserNames {
        @PropertyName("user_names")
        private List<String> userNames;

        public UserNames () {}
        public UserNames(List<String> userNames) {
            this.userNames = userNames;
        }
        @PropertyName("user_names")
        public List<String> getUserNames() {
            return userNames;
        }
        @PropertyName("usre_names")
        public void setUserNames(List<String> userNames) {
            this.userNames = userNames;
        }
    }

     */

}
