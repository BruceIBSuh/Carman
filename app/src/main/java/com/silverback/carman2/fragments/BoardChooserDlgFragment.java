package com.silverback.carman2.fragments;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.silverback.carman2.BoardWritingActivity;
import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;



/**
 * Dialog to select which media to use b/w camera and gallery for picking images which post to
 * the billboard, uploading to Firebase storage.
 */
public class BoardChooserDlgFragment extends DialogFragment {

    private static final LoggingHelper log = LoggingHelperFactory.create(BoardChooserDlgFragment.class);


    private OnMediaSelectListener mListener;

    public interface OnMediaSelectListener {
        void selectMedia(int which);
    }

    public BoardChooserDlgFragment() {
        // Required empty public constructor
    }


    @SuppressWarnings("ConstantConditions")
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        View localView = View.inflate(getContext(), R.layout.dialog_board_chooser, null);
        TextView tvGallery = localView.findViewById(R.id.tv_gallery);
        TextView tvCamera = localView.findViewById(R.id.tv_camera);

        tvGallery.setOnClickListener(view -> {
            log.i("Gallery selected");
            mListener.selectMedia(BoardWritingActivity.GALLERY);
            dismiss();
        });

        tvCamera.setOnClickListener(view -> {
            log.i("Camera selected");
            mListener.selectMedia(BoardWritingActivity.CAMERA);
            dismiss();
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Select Image Media")
                .setView(localView);

        return builder.create();
    }

    // Override the Fragment.onAttach() method to instantiate the OnMediaSelectListener
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        try {
            mListener = (OnMediaSelectListener) context;
        } catch(ClassCastException e) {
            throw new ClassCastException(getActivity().toString() + "must implement OnMediaSelectListener");
        }
    }

}