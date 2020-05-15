package com.silverback.carman2.fragments;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.utils.Constants;
import com.silverback.carman2.viewmodels.FragmentSharedModel;


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

    //private OnImageChooserListener mListener;
    private FragmentSharedModel fragmentModel;

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
        fragmentModel = new ViewModelProvider(requireActivity()).get(FragmentSharedModel.class);
    }

    @SuppressWarnings("ConstantConditions")
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        View localView = View.inflate(getContext(), R.layout.dialog_board_chooser, null);
        TextView tvGallery = localView.findViewById(R.id.tv_gallery);
        TextView tvCamera = localView.findViewById(R.id.tv_camera);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Select Image Media")
                .setView(localView);

        tvGallery.setOnClickListener(view -> {
            log.i("Gallery selected");
            //mListener.selectMedia(BoardWritingActivity.GALLERY);
            fragmentModel.getImageChooser().setValue(Constants.GALLERY);
            dismiss();
        });

        tvCamera.setOnClickListener(view -> {
            log.i("Camera selected");
            //mListener.selectMedia(BoardWritingActivity.CAMERA);
            fragmentModel.getImageChooser().setValue(Constants.CAMERA);
            dismiss();
        });



        return builder.create();
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
