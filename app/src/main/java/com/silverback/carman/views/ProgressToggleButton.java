package com.silverback.carman.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ToggleButton;

import com.silverback.carman.R;
import com.silverback.carman.databinding.ViewProgressToggleBinding;

public class ProgressToggleButton extends View {

    private ViewProgressToggleBinding binding;

    public ProgressToggleButton(Context context) {
        super(context);
    }

    public ProgressToggleButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        getAttribute(context, attrs);
    }

    public ProgressToggleButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        getAttribute(context, attrs);
    }

    private void getAttribute(Context context, AttributeSet attrs) {
        LayoutInflater inflater = LayoutInflater.from(context);
        binding = ViewProgressToggleBinding.inflate(inflater);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ProgressToggleButton);
        try {

        } finally {
            a.recycle();
        }
    }

}
