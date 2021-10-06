package com.silverback.carman.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.silverback.carman.R;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;

public class ProgressBarDialogFragment extends DialogFragment {

    private static final LoggingHelper log = LoggingHelperFactory.create(ProgressBarDialogFragment.class);

    // Fields
    private String progressMsg;
    public ProgressBarDialogFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View childView = inflater.inflate(R.layout.dialog_progbar_general, container, false);
        TextView tvMessage = childView.findViewById(R.id.tv_progbar_msg);
        tvMessage.setText(progressMsg);

        return childView;
    }

    public void setProgressMsg(final String msg) {
        progressMsg = msg;
    }

}
