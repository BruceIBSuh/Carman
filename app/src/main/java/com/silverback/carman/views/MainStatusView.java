package com.silverback.carman.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import com.silverback.carman.R;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;

public class MainStatusView extends LinearLayout {

    private static final LoggingHelper log = LoggingHelperFactory.create(MainStatusView.class);
    private Context context;
    private int progbtnId;

    public MainStatusView(Context context) {
        super(context);
    }

    public MainStatusView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getAttributes(context, attrs);
    }

    public MainStatusView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        getAttributes(context, attrs);
    }

    protected void getAttributes(Context context, AttributeSet attrs) {
        //LayoutInflater.from(context).inflate(R.layout.main_collapsed_pricebar, this, true);
        this.context = context;
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.MainStatusView);
        try {
            progbtnId = typedArray.getInt(R.styleable.MainStatusView_progbtn, -1);
            log.i("probtn: %s", progbtnId);
        } finally {
            typedArray.recycle();
        }
    }

    public void setProgbtnId(String button) {
        switch(button) {
            case "gas":
                LayoutInflater.from(context).inflate(R.layout.main_collapsed_pricebar, this, true);
                break;
            case "ev":
                LayoutInflater.from(context).inflate(R.layout.main_collapsed_state, this, true);
                break;
        }
    }
}
