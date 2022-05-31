package com.silverback.carman.fragments;

import static android.content.Context.MEDIA_COMMUNICATION_SERVICE;
import static com.silverback.carman.BoardActivity.CAMERA;
import static com.silverback.carman.BoardActivity.GALLERY;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.silverback.carman.R;
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
    private final int NO_IMAGE = 0;
    private final int MEDIA_GALLERY = 1;
    private final int MEDIA_CAMERA = 2;

    //private FragmentSharedModel fragmentModel;
    public ImageChooserFragment() {
        // Required empty public constructor which might be invoked by FragmentFacotry.
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //fragmentModel = new ViewModelProvider(requireActivity()).get(FragmentSharedModel.class);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getLayoutInflater();
        DialogImageChooserBinding binding = DialogImageChooserBinding.inflate(inflater);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setView(binding.getRoot())
                .setTitle(getString(R.string.pref_userpic_title));

        Bundle result = new Bundle();
        binding.tvGallery.setOnClickListener(view -> {
            //fragmentModel.getImageChooser().setValue(MEDIA_GALLERY);
            result.putInt("mediaType", MEDIA_GALLERY);
            getParentFragmentManager().setFragmentResult("selectMedia", result);
            dismiss();
        });
        binding.tvCamera.setOnClickListener(view -> {
            //fragmentModel.getImageChooser().setValue(MEDIA_CAMERA);
            result.putInt("mediaType", MEDIA_CAMERA);
            getParentFragmentManager().setFragmentResult("selectMedia", result);
            dismiss();
        });

        binding.tvNoImg.setOnClickListener(view -> {
            //fragmentModel.getImageChooser().setValue(-1);
            result.putInt("mediaType", NO_IMAGE);
            getParentFragmentManager().setFragmentResult("selectMedia", result);
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
