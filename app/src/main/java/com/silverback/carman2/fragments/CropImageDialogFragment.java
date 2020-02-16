package com.silverback.carman2.fragments;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

/**
 * This class is a custom DialogFragment to select which media to pick out of Gallery and Camera, or
 * remove the user image. The selection is notified to the parent activity, SettingPreferenceActivity
 * with the listener that paases the position of a selected item.
 */
public class CropImageDialogFragment extends DialogFragment {

    private static final LoggingHelper log = LoggingHelperFactory.create(CropImageDialogFragment.class);

    // Objects
    private OnSelectImageMediumListener mListener;
    private int selected;

    public interface OnSelectImageMediumListener {
        void onSelectImageMedia(int position);
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
                    mListener.onSelectImageMedia(selected);
                })
                .setNegativeButton("Cancel", (dialog, id) -> {});

        return builder.create();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            mListener = (OnSelectImageMediumListener) context;
        } catch(ClassCastException e) {
            log.i("ClassCastException: %s", e.getMessage());
        }
    }
}
