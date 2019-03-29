package com.silverback.carman2.fragments;


import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Spinner;

import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceDialogFragmentCompat;

/**
 * A simple {@link Fragment} subclass.
 */
public class SpinnerPrefDlgFragment extends PreferenceDialogFragmentCompat {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(SpinnerPrefDlgFragment.class);

    // Constants
    private static final String ARG_KEY = "com.silverback.carman2.setting.district";

    public SpinnerPrefDlgFragment() {
        // Required empty public constructor
    }

    // Method for singleton instance
    static SpinnerPrefDlgFragment newInstance(String key) {

        log.i("SpinnerPrefDlgFragment: %s", key);
        final SpinnerPrefDlgFragment fm = new SpinnerPrefDlgFragment();
        final Bundle bundle = new Bundle(1);
        bundle.putString(ARG_KEY, key);
        fm.setArguments(bundle);
        return fm;
    }


    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        log.i("onBindDialogView");
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        log.i("AlertDialog");
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(LayoutInflater.from(getContext()).inflate(R.layout.dialogpref_spinner, null));
        return builder.create();
    }




    @Override
    public void onDialogClosed(boolean positiveResult) {

    }

}
