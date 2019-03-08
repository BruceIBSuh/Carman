package com.silverback.carman2.opinet;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
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

    // Constructor of 3 different types. Here, it mainly uses the second one.
    public AvgPriceView(Context context) {
        super(context);
    }
    public AvgPriceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getAttributes(attrs);
    }
    public AvgPriceView(Context context, AttributeSet attrs, int defaultStyle) {
        super(context, attrs, defaultStyle);
        getAttributes(attrs);
    }

    // Get Attributes
    @SuppressWarnings("ConstantConditions")
    private void getAttributes(AttributeSet attrs) {

        TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.AvgPriceView);
        priceTextSize = typedArray.getDimensionPixelSize(R.styleable.AvgPriceView_avgPriceTextSize, 0);
        priceUpColor = typedArray.getColor(R.styleable.AvgPriceView_avgPriceUpColor, 0);
        priceDownColor = typedArray.getColor(R.styleable.AvgPriceView_avgPriceDownColor, 0);

        String title = typedArray.getString(R.styleable.AvgPriceView_avgTitle);

        // Inflates LinearLayout
        LayoutInflater inflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        linearLayout = (LinearLayout)inflater.inflate(R.layout.custom_avg, this, true);
        tvTitle = linearLayout.findViewById(R.id.tv_custom_title);

        Log.i(LOG_TAG, "Title: " + title);


        tvTitle.setText(title);

        typedArray.recycle();
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }


}
