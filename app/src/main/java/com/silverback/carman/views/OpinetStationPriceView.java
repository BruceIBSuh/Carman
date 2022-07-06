package com.silverback.carman.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.TextView;

import com.silverback.carman.R;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.threads.StationFavRunnable;

import java.util.Map;

/**
 * Custom view extending OpinetPriceView which is an abstract class having setColoredTextView() as
 * an abstract method.
 */
public class OpinetStationPriceView extends OpinetPriceView {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(OpinetStationPriceView.class);

    // UIs
    private StationFavRunnable.Info favInfo;
    private TextView tvStnName, tvStnPrice;

    // Constructors of 3 different types. Here, it mainly uses the second one.
    public OpinetStationPriceView(Context context) {
        super(context);
    }
    public OpinetStationPriceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getAttributes(context, attrs);
    }
    public OpinetStationPriceView(Context context, AttributeSet attrs, int defaultStyle) {
        super(context, attrs, defaultStyle);
        getAttributes(context, attrs);
    }

    protected void getAttributes(Context context, AttributeSet attrs) {
        LayoutInflater.from(context).inflate(R.layout.view_station_price, this, true);
        tvStnName = findViewById(R.id.expense_tv_station_name);
        tvStnPrice = findViewById(R.id.tv_station_price);

        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.OpinetStationPriceView);
        try {
            priceUpColor = ta.getColor(R.styleable.OpinetStationPriceView_stnPriceUp, 0);
            priceDownColor = ta.getColor(R.styleable.OpinetStationPriceView_stnPriceDown, 0);
        } finally {
            ta.recycle();
        }

    }

    public void setFavStaationInfo(StationFavRunnable.Info info) {
        this.favInfo = info;
    }

    public void addPriceView(String fuelCode) {
        tvStnName.setText(favInfo.getStationName());
        log.i("favorite: %s, %s", favInfo.getStationName(), favInfo.getPriceDiff());

        for(StationFavRunnable.OilPrice price : favInfo.getOliPriceList()) {
            if(price.getOilCode().matches(fuelCode)) {
                //Object priceDiff = favInfo.getPriceDiff().get(fuelCode);
                //float diff = (priceDiff != null)? (float)priceDiff : 0;
                setColoredTextView(tvStnPrice, price.getPrice(), 0);
                break;
            }
        }
    }

    // Set the station price views to be void when the favorite is left empty.
    public void removePriceView(String msg) {
        tvStnName.setText(msg);
        tvStnPrice.setText("");
    }

}

