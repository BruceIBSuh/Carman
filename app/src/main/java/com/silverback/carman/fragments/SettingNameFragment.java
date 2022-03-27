package com.silverback.carman.fragments;

import static android.content.DialogInterface.BUTTON_POSITIVE;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;
import com.silverback.carman.R;
import com.silverback.carman.databinding.DialogSettingNameBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.Constants;
import com.silverback.carman.viewmodels.FragmentSharedModel;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;

public class SettingNameFragment extends DialogFragment {

    private static final LoggingHelper log = LoggingHelperFactory.create(SettingNameDlgFragment.class);
    private FirebaseFirestore firestore;
    private DialogSettingNameBinding binding;
    private FragmentSharedModel fragmentModel;
    private final Preference preference;
    private final String currentName;
    private String newName;

    public SettingNameFragment(Preference preference, String userName) {
        this.preference = preference;
        this.currentName = userName;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firestore = FirebaseFirestore.getInstance();
        fragmentModel = new ViewModelProvider(requireActivity()).get(FragmentSharedModel.class);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);
        binding = DialogSettingNameBinding.inflate(getLayoutInflater());

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        AlertDialog dialog = builder.setView(binding.getRoot())
                .setTitle(R.string.pref_title_username)
                .setPositiveButton(getString(R.string.dialog_btn_confirm), (v, which) -> updateUserProfile())
                .setNegativeButton(getString(R.string.dialog_btn_cancel), (v, which) -> dismiss())
                .create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);

        binding.etUserName.setText(currentName);
        binding.etUserName.setOnFocusChangeListener((v, hasFocus) -> {
            if(hasFocus) binding.etUserName.setText("");
            else binding.etUserName.setHint(R.string.pref_hint_username);
        });

        // Regular expression required to check if a user name should be valid.
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

        // Check if the same username exists. Keep it in mind that this query is case sensitive and
        // the user name policy should be researched.
        binding.btnVerify.setOnClickListener(v -> {
            newName = binding.etUserName.getText().toString().trim();
            if(TextUtils.isEmpty(newName) || newName.matches(currentName)) {
                final String msg = getString(R.string.pref_hint_username);
                Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_SHORT).show();
                return;
            }
            // Query the name to check if there exists the same name in Firestore
            // Firestore does not provide case insensitive query. To do so, make the full text search
            // using a dedicated thrid party service such as Elastic, Algolia, or Typesense.
            Query queryName = firestore.collection("users").whereArrayContains("user_name", newName).limit(1);
            queryName.get().addOnSuccessListener(querySnapshot -> {
                if(querySnapshot.size() > 0) {
                    dialog.getButton(BUTTON_POSITIVE).setEnabled(false);
                    binding.btnVerify.setEnabled(false);
                    Snackbar.make(binding.getRoot(), getString(R.string.pref_username_msg_invalid), Snackbar.LENGTH_SHORT).show();
                } else {
                    dialog.getButton(BUTTON_POSITIVE).setEnabled(true);
                    Snackbar.make(binding.getRoot(), getString(R.string.pref_username_msg_available), Snackbar.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(Throwable::printStackTrace);
        });

        return dialog;
    }

    private void updateUserProfile() {
        // When a new username has replaced the current name, update the new name in Firestore.
        //preference.callChangeListener(newName); //seems not working
        try (FileInputStream fis = requireActivity().openFileInput("userId");
             BufferedReader br = new BufferedReader(new InputStreamReader(fis))) {
            final String userId = br.readLine();
            DocumentReference docRef = firestore.collection("users").document(userId);

            WriteBatch batch = firestore.batch();
            Date regDate = Timestamp.now().toDate();
            batch.update(docRef, "user_name", FieldValue.arrayUnion(newName));
            batch.update(docRef, "reg_date", FieldValue.arrayUnion(regDate));
            batch.commit().addOnCompleteListener(task -> {
                if(task.isSuccessful()) fragmentModel.getUserName().setValue(newName);
                else Snackbar.make(binding.getRoot(), "failed to update", Snackbar.LENGTH_SHORT).show();
                dismiss();
            });
        } catch(IOException | NullPointerException e) { e.printStackTrace(); }
    }
}
