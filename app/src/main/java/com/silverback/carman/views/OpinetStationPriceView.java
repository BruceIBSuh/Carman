package com.silverback.carman.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.net.Uri;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.TextView;

import com.silverback.carman.R;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.viewmodels.Opinet;
import com.silverback.carman.utils.Constants;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.Map;

/**
 * Custom view extending OpinetPriceView which is an abstract class having setColoredTextView() as
 * an abstract method.
 */
public class OpinetStationPriceView extends OpinetPriceView {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(OpinetStationPriceView.class);

    // UIs
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

    public void addPriceView(String fuelCode) {
        File stnFile = new File(getContext().getFilesDir(), Constants.FILE_FAVORITE_PRICE);
        Uri stnUri = Uri.fromFile(stnFile);
        Float price = null;
        Float diff = null;
        try(InputStream is = getContext().getContentResolver().openInputStream(stnUri);
            ObjectInputStream ois = new ObjectInputStream(is)){
            Opinet.StationPrice stnPrice = (Opinet.StationPrice)ois.readObject();

            String stnName = stnPrice.getStnName();
            tvStnName.setText(stnName);

            Map<String, Float> mapPrice = stnPrice.getStnPrice();
            Map<String, Float> mapDiff = stnPrice.getPriceDiff();
            price = mapPrice.get(fuelCode);
            diff = mapDiff.get(fuelCode);

            if(price == null || diff == null) throw new NullPointerException();
            else setColoredTextView(tvStnPrice, price, diff);

        } catch(IOException | ClassNotFoundException | NullPointerException e) {
            if(price == null) setColoredTextView(tvStnPrice, 0, 0);
            else if(diff == null) setColoredTextView(tvStnPrice, price, 0);
            e.printStackTrace();
        }
    }

    // Set the station price views to be void when the favorite is left empty.
    public void removePriceView(String msg) {
        tvStnName.setText(msg);
        tvStnPrice.setText("");
    }

}

