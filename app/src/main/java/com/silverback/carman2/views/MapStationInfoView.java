package com.silverback.carman2.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import java.lang.ref.WeakReference;

public class MapStationInfoView extends LinearLayout {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(MapStationInfoView.class);

    // Objects
    private TextView tvName, tvAddress;

    // Constructor
    public MapStationInfoView(Context context) {
        super(context);
    }

    public MapStationInfoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getAttributes(context, attrs);
    }

    public MapStationInfoView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        getAttributes(context, attrs);
    }

    private void getAttributes(Context context, AttributeSet attrs) {
        LayoutInflater.from(context).inflate(R.layout.cardview_map, this, false);
        tvName = findViewById(R.id.name);
        tvAddress = findViewById(R.id.address);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.SidoPriceView);


    }

    public void initView() {
        tvName.setText("Test Station");
        tvAddress.setText("Test Address");
    }

}
