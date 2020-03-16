package com.silverback.carman2.fragments;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

public class ProgbarDialogFragment extends DialogFragment {

    private static final LoggingHelper log = LoggingHelperFactory.create(ProgbarDialogFragment.class);

    // Fields
    private String progressMsg;

    public ProgbarDialogFragment() {}

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