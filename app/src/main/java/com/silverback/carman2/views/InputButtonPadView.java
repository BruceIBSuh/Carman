package com.silverback.carman2.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

public class InputButtonPadView extends ConstraintLayout {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(InputButtonPadView.class);

    // Constructor
    public InputButtonPadView(Context context) {
        super(context);
        getAttribute(context, null);
    }

    public InputButtonPadView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getAttribute(context, attrs);
    }

    public InputButtonPadView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        getAttribute(context, attrs);
    }

    private void getAttribute(Context context, AttributeSet attrs) {
        LayoutInflater.from(context).inflate(R.layout.view_button_pad, this, true);
    }
}
