package com.silverback.carman2.fragments;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import com.silverback.carman2.R;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

/**
 * A simple {@link DialogFragment} subclass.
 */
public class FinishAppDialogFragment extends DialogFragment {


    // Constants
    //private static final String TAG = "FinishAppDialog";

    // Object
    NoticeDialogListener mListener;

    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it. */
    public interface NoticeDialogListener {
        void onDialogPositiveClick(DialogFragment dialog);
        void onDialogNegativeClick(DialogFragment dialog);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.msg_finish_app)
                .setPositiveButton(R.string.btn_finish_confirm, new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int id){
                        mListener.onDialogPositiveClick(FinishAppDialogFragment.this);
                    }
                })
                .setNegativeButton(R.string.btn_finish_cancel, new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int id){
                        mListener.onDialogNegativeClick(FinishAppDialogFragment.this);
                    }
                });

        return builder.create();
    }

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (NoticeDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement NoticeDialogListener");
        }
    }

}
