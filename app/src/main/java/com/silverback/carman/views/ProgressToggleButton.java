package com.silverback.carman.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ToggleButton;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.silverback.carman.R;
import com.silverback.carman.databinding.ViewProgressToggleBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;

public class ProgressToggleButton extends LinearLayout {

    private static final LoggingHelper log = LoggingHelperFactory.create(ProgressToggleButton.class);

    private ViewProgressToggleBinding binding;
    private int pbBackground;
    private int pbProgress;

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
        log.i("custom view");
        LayoutInflater inflater = LayoutInflater.from(context);
        binding = ViewProgressToggleBinding.inflate(inflater, this, true);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ProgressToggleButton);
        try {
            pbBackground = a.getColor(R.styleable.ProgressToggleButton_pbBackground, 0);
            pbProgress = a.getColor(R.styleable.ProgressToggleButton_pbProgress, 0);
        } finally {
            a.recycle();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public void setProgressColor(boolean b) {
        int color = b?pbProgress:pbBackground;
        binding.progressBar.setBackgroundColor(color);
        binding.progressBar.setIndeterminate(!b);
    }

}
