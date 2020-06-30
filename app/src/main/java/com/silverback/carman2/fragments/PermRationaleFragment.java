package com.silverback.carman2.fragments;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.silverback.carman2.viewmodels.FragmentSharedModel;

/**
 * This dialogfragmnet is to show an educational UI for explaning the user why this app requires a
 * speicif permission in response to ActivityCompat.shouldShowRequestPermissionRationale().
 * In this UI, cancel or no thank button should be included so that the user keeps using the app
 * w/o granting the permission.
 */
public class PermRationaleFragment extends DialogFragment {

    // Object
    private FragmentSharedModel fragmentModel;
    private String msg, title;

    // Empty contructor and overriding onCreate() are required to attach this fragment to the activity.
    // Otherwise, it may incur IllegalStateException.
    public PermRationaleFragment() {
        // requires empty contructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fragmentModel = new ViewModelProvider(requireActivity()).get(FragmentSharedModel.class);
    }

    @SuppressWarnings("ConstantConditions")
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(title).setMessage(msg)
                .setPositiveButton("Confirm", (dialog, id) -> {
                    fragmentModel.getPermission().setValue(true);
                }).setNegativeButton("Cancel", (dialog, id) -> {
                    fragmentModel.getPermission().setValue(false);
                    dismiss();
                });

        return builder.create();
    }

    // Set the title and message in the dialog.
    public void setContents(String title, String message) {
        this.title = title;
        this.msg = message;
    }
}
