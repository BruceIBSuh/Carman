package com.silverback.carman2.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.silverback.carman2.R;

import java.lang.ref.WeakReference;

public class AvgPriceView extends LinearLayout {

    // Constants
    private static final String LOG_TAG = "AvgPriceView";

    // Objects
    private WeakReference<View> mThisView;
    private LinearLayout linearLayout;
    private TextView tvTitle;
    private TextView tvPrice;

    // Fields
    private int priceTextSize;
    private int priceUpColor, priceDownColor;
    private String title, avgPrice;

    // Constructor of 3 different types. Here, it mainly uses the second one.
    /*
    public AvgPriceView(Context context) {
        super(context);
    }
    */
    public AvgPriceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        //LayoutInflater inflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        //linearLayout = (LinearLayout)inflater.inflate(R.layout.view_avg_price, this, true);
        LayoutInflater.from(context).inflate(R.layout.view_avg_price, this, true);
        getAttributes(context, attrs);
    }

    /*
    public AvgPriceView(Context context, AttributeSet attrs, int defaultStyle) {
        super(context, attrs, defaultStyle);
        getAttributes(attrs);
    }
    */

    // Get Attributes
    @SuppressWarnings("ConstantConditions")
    private void getAttributes(Context context, AttributeSet attrs) {

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.AvgPriceView);
        try {
            //priceTextSize = typedArray.getDimensionPixelSize(R.styleable.AvgPriceView_avgPriceTextSize, 0);
            priceUpColor = typedArray.getColor(R.styleable.AvgPriceView_avgPriceUpColor, 0);
            priceDownColor = typedArray.getColor(R.styleable.AvgPriceView_avgPriceDownColor, 0);

            //title = typedArray.getString(R.styleable.AvgPriceView_avgTitle);
            //avgPrice = typedArray.getString(R.styleable.AvgPriceView_avgPrice);
        } finally {
            typedArray.recycle();
        }

        //if(title == null) throw new RuntimeException("No title provided");
        //if(avgPrice == null) throw new RuntimeException("No price provided");

        TextView tvTitle = findViewById(R.id.tv_custom_title);
        TextView tvAvgPrice = findViewById(R.id.tv_custom_price);

        tvTitle.setText(getResources().getString(R.string.general_opinet_subtitle_avgPrice));
        tvAvgPrice.setText(getResources().getString(R.string.general_opinet_avgPrice));
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }


}