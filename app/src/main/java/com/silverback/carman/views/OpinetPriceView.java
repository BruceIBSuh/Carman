package com.silverback.carman.views;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;

/**
 * The abstract class is subclassed by OpinetAvgPriceView, OpinetSidoPriceView, OpinetSigunPriceView
 * and OpinetStationPriceView, inheriting setColoredTextView() which makes the color of price
 * difference colored according to whether the price is up or down.
 *
 *
 */
public abstract class OpinetPriceView extends LinearLayout {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(OpinetPriceView.class);

    // Objects
    protected int priceUpColor, priceDownColor;

    // Constructors of 3 different types. Here, it mainly uses the second one.
    public OpinetPriceView(Context context) {
        super(context);
    }

    public OpinetPriceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getAttributes(context, attrs);
    }
    public OpinetPriceView(Context context, AttributeSet attrs, int defaultStyle) {
        super(context, attrs, defaultStyle);
        getAttributes(context, attrs);
    }

    // Abstract Methods
    protected abstract void getAttributes(Context context, AttributeSet attrs);
    public abstract void addPriceView(String fuelCode);

    // Methods to switch the text color according to whether the price gets higher or lower.
    protected void setColoredTextView(TextView textView, float price, float diff) {

        int colorDiff = (diff > 0)? priceUpColor : priceDownColor;
        log.i("colorDiff: %s", colorDiff);

        // Create SpannableStringBuilder to change the text color in the spanned price difference.
        SpannableStringBuilder ssb = new SpannableStringBuilder();
        ssb.append(Float.toString(price)).append(" (");
        int start = ssb.length();
        ssb.append(Float.toString(diff));
        ssb.setSpan(new ForegroundColorSpan(colorDiff), start, ssb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        ssb.append(")");

        log.i("Diff: %s, %s", price, diff);
        textView.setText(ssb);
    }
}
