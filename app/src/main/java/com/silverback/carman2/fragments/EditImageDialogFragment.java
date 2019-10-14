package com.silverback.carman2.fragments;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProviders;

import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.FragmentSharedModel;

public class EditImageDialogFragment extends DialogFragment {

    private static final LoggingHelper log = LoggingHelperFactory.create(EditImageDialogFragment.class);

    // Objects
    private FragmentSharedModel sharedModel;
    private int selected;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getActivity() != null)
            sharedModel = ViewModelProviders.of(getActivity()).get(FragmentSharedModel.class);
    }

    @SuppressWarnings("ConstantConditions")
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.pref_dialog_title_edit_image)
                .setSingleChoiceItems(R.array.pref_edit_image, 0, (dialog, which) -> {
                    selected = which;
                })
                .setPositiveButton("Confirm", (didalog, which) -> {
                    sharedModel.getImageItemSelected().setValue(selected);
                })
                .setNegativeButton("Cancel", (dialog, id) -> {});

        return builder.create();

    }
}
