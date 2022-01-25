package com.silverback.carman.fragments;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.silverback.carman.R;
import com.silverback.carman.databinding.DialogBoardChooserBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.Constants;
import com.silverback.carman.viewmodels.FragmentSharedModel;


/**
 * This dialog fragmnet is to select which media to use b/w camera and gallery for picking images,
 * which would be attached to the board and updated with Firebase Storage.
 *
 */
public class BoardChooserDlgFragment extends DialogFragment {

    private static final LoggingHelper log = LoggingHelperFactory.create(BoardChooserDlgFragment.class);

    // Constants
    private final int GALLERY = 1;
    private final int CAMERA = 2;

    //private DialogBoardChooserBinding binding;
    //private OnImageChooserListener mListener;
    //private FragmentSharedModel fragmentModel;

    /*
    public interface OnImageChooserListener {
        void selectMedia(int which);
    }
    */

    public BoardChooserDlgFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    //@SuppressWarnings("ConstantConditions")
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        DialogBoardChooserBinding binding = DialogBoardChooserBinding.inflate(LayoutInflater.from(requireActivity()));
        FragmentSharedModel fragmentModel = new ViewModelProvider(requireActivity()).get(FragmentSharedModel.class);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setTitle("Select Image Media").setView(binding.getRoot());
        binding.tvGallery.setOnClickListener(view -> {
            log.i("Gallery selected");
            //mListener.selectMedia(BoardWritingActivity.GALLERY);
            fragmentModel.getImageChooser().setValue(Constants.GALLERY);
            dismiss();
        });
        binding.tvCamera.setOnClickListener(view -> {
            log.i("Camera selected");
            //mListener.selectMedia(BoardWritingActivity.CAMERA);
            fragmentModel.getImageChooser().setValue(Constants.CAMERA);
            dismiss();
        });

        return builder.create();
    }

    // onViewCreated() is never called on a custom DialogFragment unless you've overridden
    // onCreateView() and provided a non-null view.
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        /*
        binding.tvGallery.setOnClickListener(v -> {
            log.i("Gallery selected");
            //mListener.selectMedia(BoardWritingActivity.GALLERY);
            fragmentModel.getImageChooser().setValue(GALLERY);
            dismiss();
        });

        binding.tvCamera.setOnClickListener(v -> {
            log.i("Camera selected");
            //mListener.selectMedia(BoardWritingActivity.CAMERA);
            fragmentModel.getImageChooser().setValue(CAMERA);
            dismiss();
        });
         */
    }

    // Override the Fragment.onAttach() method to instantiate the OnImageChooserListener
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        /*
        try {
            //mListener = (OnImageChooserListener) context;
        } catch(ClassCastException e) {
            throw new ClassCastException(getActivity().toString() + "must implement OnImageChooserListener");
        }

         */
    }

}
