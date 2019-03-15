package com.silverback.carman2.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

public class StationPriceView extends LinearLayout {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(SidoPriceView.class);

    // Objects
    private int priceUpColor, priceDownColor;

    // Constructors of 3 different types. Here, it mainly uses the second one.
    /*
    public StationPriceView(Context context) {
        super(context);
    }
    */
    public StationPriceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getAttributes(context, attrs);
    }
    /*
    public StationPriceView(Context context, AttributeSet attrs, int defaultStyle) {
        super(context, attrs, defaultStyle);
        getAttributes(context, attrs);
    }
    */

    @SuppressWarnings("ConstantConditions")
    private void getAttributes(Context context, AttributeSet attrs) {

        //LayoutInflater inflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        //linearLayout = (LinearLayout)inflater.inflate(R.layout.view_avg_price, this, true);
        LayoutInflater.from(context).inflate(R.layout.view_station_price, this, true);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.StationPriceView);

        try {
            priceUpColor = typedArray.getColor(R.styleable.StationPriceView_stationPriceUp, 0);
            priceDownColor = typedArray.getColor(R.styleable.StationPriceView_stationPriceDown, 0);
            log.i("Color: %d, %d", priceUpColor, priceDownColor);

        } finally {
            typedArray.recycle();
        }

    }

    public void addPriceView(String fuelCode) {

    }
}
