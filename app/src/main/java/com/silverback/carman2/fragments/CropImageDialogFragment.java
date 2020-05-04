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
 * This class is a custom DialogFragment for choosing which media to pick out of Gallery and Camera,
 * or remove the user image. The selection is notified to the parent activity, SettingPreferenceActivity
 * with the listener which passes the position of a selected item.
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
                .setSingleChoiceItems(R.array.pref_edit_image, 0, (dialog, which) -> selected = which)
                .setPositiveButton(getString(R.string.btn_dialog_confirm), (didalog, which) ->
                        mListener.onSelectImageMedia(selected))
                .setNegativeButton(getString(R.string.btn_dialog_cancel), (dialog, id) -> {});

        return builder.create();
    }


    // How to implement Interface
    // 1. onAttach() in the caller fragment
    // 2. onAttachFragmetn() in the callee(parent) activity
    // 3. setter defined in the caller and invoke it in the callee.
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
