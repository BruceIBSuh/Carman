package com.silverback.carman2.fragments;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.DialogFragment;

public class PermissionDialogFragment extends DialogFragment {

    // Object
    private OnDialogListener mListener;
    private Context context;
    private String msg, title;

    public interface OnDialogListener {
        void onPositiveClick(DialogFragment dialog);
        void onNegativeClick(DialogFragment dialgo);
    }

    public PermissionDialogFragment(Context context, String title, String msg) {
        this.context = context;
        this.title = title;
        this.msg = msg;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title).setMessage(msg)
                .setPositiveButton("Confirm", (dialog, id) -> {
                    mListener.onPositiveClick(this);
                })
                .setNegativeButton("Cancel", (dialog, id) -> {
                    mListener.onNegativeClick(this);
                    dismiss();
                });

        return builder.create();
    }

    // Override Fragment.onAttach() method to instantiate OnDialogListener
    @SuppressWarnings("ConstantConditions")
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            mListener = (OnDialogListener)context;
        } catch(ClassCastException e) {
            throw new ClassCastException(getActivity().toString() + "must implement OnDialogListener");
        }
    }

}
