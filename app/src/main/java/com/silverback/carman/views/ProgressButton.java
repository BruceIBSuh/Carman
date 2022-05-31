package com.silverback.carman.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.core.content.ContextCompat;

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
    private int eventRef;
    private boolean isClicked;
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
        Drawable bgButtonRef;

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.ProgressButton);
        try {
            pbColorRef = typedArray.getColor(R.styleable.ProgressButton_progBgColor, 0);
            bgButtonRef = typedArray.getDrawable(R.styleable.ProgressButton_bgToggle);
            eventRef = typedArray.getInt(R.styleable.ProgressButton_onEvent, -1);
        } finally {
            typedArray.recycle();
        }

        binding.progressBar.setBackgroundColor(pbColorRef);
        binding.button.setBackground(bgButtonRef);
        binding.button.setOnClickListener(view -> setEvent(eventRef));

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }


    public void setProgressColor(boolean isVisible) {
        if(!isVisible) {
            binding.progressBar.setIndeterminate(true);
            binding.progressBar.setScaleY(5f);
        } else {
            binding.progressBar.setIndeterminate(false);
            pbColorRef = (pbColorRef == 0) ? ContextCompat.getColor(context, android.R.color.holo_red_light) : 0;
            binding.progressBar.setBackgroundColor(pbColorRef);
            binding.progressBar.setScaleY(1f);
        }
    }

    public int getPbColorRef() {
        return pbColorRef;
    }

    private void setEvent(int type){
        ((MainActivity)context).locateStations(type);
        /*
        switch(eventRef){
            case 0: ((MainActivity)context).locateNearStations(code); break;
            case 1: ((MainActivity)context).locateNearServices(code); break;
            case 2: ((MainActivity)context).locateElecStations(code); break;
        }

         */
    }

}
