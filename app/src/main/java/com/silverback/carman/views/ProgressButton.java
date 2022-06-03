package com.silverback.carman.views;

import android.Manifest;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import androidx.core.content.ContextCompat;

import com.silverback.carman.MainActivity;
import com.silverback.carman.R;
import com.silverback.carman.databinding.ViewProgressButtonBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.threads.ThreadManager2;

public class ProgressButton extends LinearLayout {

    private static final LoggingHelper log = LoggingHelperFactory.create(ProgressButton.class);

    private ViewProgressButtonBinding binding;
    private Context context;
    private int pbColorRef;
    private int buttonRef;
    private boolean isActive;
    private int offColor, onColor;

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

    public void setProgressColor(boolean isStateOn) {
        if(!isStateOn) {
            log.i("state on");
            binding.progressBar.setIndeterminate(true);
            binding.progressBar.setScaleY(5f);
            binding.button.setClickable(false);

        } else {
            log.i("invoked when the location fetched: %s", buttonRef);
            binding.progressBar.setIndeterminate(false);
            pbColorRef = ContextCompat.getColor(context, android.R.color.holo_red_light);
            //pbColorRef = (pbColorRef == 0)?ContextCompat.getColor(context, android.R.color.holo_red_light):0;
            binding.progressBar.setBackgroundColor(pbColorRef);
            binding.progressBar.setScaleY(1f);
            binding.button.setClickable(true);
        }

    }

    public void resetButton() {
        log.i("resetButton");
        pbColorRef = ContextCompat.getColor(context, android.R.color.white);
        binding.progressBar.setBackgroundColor(pbColorRef);
        binding.progressBar.setScaleY(1f);
        this.isActive = !isActive;
    }

    private void setEvent(int type, boolean isActive){
        log.i("setEvent");
        ((MainActivity)context).locateStations(type, isActive);
        this.isActive = !isActive;


    }

}
