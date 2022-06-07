package com.silverback.carman.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;
import com.silverback.carman.MainActivity;
import com.silverback.carman.R;
import com.silverback.carman.databinding.ViewProgressButtonBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;

public class ProgressButton extends LinearLayout {

    private static final LoggingHelper log = LoggingHelperFactory.create(ProgressButton.class);

    private ViewProgressButtonBinding binding;
    private Context context;
    private int pbColorRef;
    private int buttonRef;
    private boolean isActive;

    public ProgressButton(Context context) {
        super(context);
    }

    public ProgressButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        getAttribute(context, attrs);
    }

    public ProgressButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        getAttribute(context, attrs);
    }

    private void getAttribute(Context context, AttributeSet attrs) {
        LayoutInflater inflater = LayoutInflater.from(context);
        binding = ViewProgressButtonBinding.inflate(inflater, this, true);
        this.context = context;
        this.isActive = false;
        Drawable btnBgRef;

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.ProgressButton);
        try {
            pbColorRef = typedArray.getColor(R.styleable.ProgressButton_pbColor, 0);
            btnBgRef = typedArray.getDrawable(R.styleable.ProgressButton_btnBg);
            buttonRef = typedArray.getInt(R.styleable.ProgressButton_onType, -1);
        } finally { typedArray.recycle();}

        binding.progressBar.setBackgroundColor(pbColorRef);
        binding.button.setBackground(btnBgRef);
        binding.button.setOnClickListener(view -> setEvent(buttonRef, isActive));

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public void setProgress() {
        binding.progressBar.setIndeterminate(true);
        binding.progressBar.setScaleY(5f);
        binding.button.setClickable(false);
    }

    public void stopProgress() {
        binding.progressBar.setIndeterminate(false);
        pbColorRef = ContextCompat.getColor(context, android.R.color.holo_red_light);
        binding.progressBar.setBackgroundColor(pbColorRef);
        binding.progressBar.setScaleY(1f);
        binding.button.setClickable(true);
    }

    public void resetProgress(){
        log.i("resetButton");
        pbColorRef = ContextCompat.getColor(context, android.R.color.white);
        binding.progressBar.setBackgroundColor(pbColorRef);
        binding.progressBar.setScaleY(1f);
        isActive = !isActive;
    }

    private void setEvent(int type, boolean isActive){
        if(type == 1 || type == 3) {
            String msg = context.getString(R.string.main_general_no_service);
            Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_SHORT).show();
            return; //exclude temporarily the service button
        }
        ((MainActivity)context).locateStations(type, isActive);
        this.isActive = !isActive;
    }

}
