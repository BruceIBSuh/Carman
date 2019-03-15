package com.silverback.carman2.views;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

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

        // To set font size, use the px value in dimen.xml, then covert it to sp using TypedValue.
        // COMPLEX_UNIT_SP
        //tvAvgPrice.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        textView.setText(ssb);

    }


}
