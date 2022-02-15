package com.silverback.carman.fragments;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.silverback.carman.databinding.DialogImageChooserBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.Constants;
import com.silverback.carman.viewmodels.FragmentSharedModel;


/**
 * This dialog fragmnet is to select which media to use b/w camera and gallery for picking images,
 * which would be attached to the board and updated with Firebase Storage.
 *
 */
public class ImageChooserFragment extends DialogFragment {

    private static final LoggingHelper log = LoggingHelperFactory.create(ImageChooserFragment.class);

    private FragmentSharedModel fragmentModel;
    public ImageChooserFragment() {
        // Required empty public constructor which might be invoked by FragmentFacotry.
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fragmentModel = new ViewModelProvider(requireActivity()).get(FragmentSharedModel.class);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getLayoutInflater();
        DialogImageChooserBinding binding = DialogImageChooserBinding.inflate(inflater);
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setView(binding.getRoot());

        binding.tvGallery.setOnClickListener(view -> {
            log.i("Gallery selected");
            fragmentModel.getImageChooser().setValue(Constants.GALLERY);
            dismiss();
        });
        binding.tvCamera.setOnClickListener(view -> {
            log.i("Camera selected");
            //mListener.selectMedia(BoardWritingActivity.CAMERA);
            fragmentModel.getImageChooser().setValue(Constants.CAMERA);
            dismiss();
        });

        binding.tvNoImg.setOnClickListener(view -> {
            fragmentModel.getImageChooser().setValue(-1);
            dismiss();
        });

        new Dialog(requireActivity(), getTheme()) {
            @Override
            public void onBackPressed() {
                dismiss();
            }
        };

        return builder.create();
    }

}
