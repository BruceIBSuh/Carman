package com.silverback.carman2.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.lifecycle.ViewModelProviders;

import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Opinet;
import com.silverback.carman2.models.OpinetViewModel;
import com.silverback.carman2.threads.PriceFavoriteTask;
import com.silverback.carman2.threads.ThreadManager;
import com.silverback.carman2.utils.Constants;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.Map;

public class OpinetStationPriceView extends LinearLayout {

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

    private void getAttributes(Context context, AttributeSet attrs) {

        //LayoutInflater inflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        //linearLayout = (LinearLayout)inflater.inflate(R.layout.view_avg_price, this, true);
        LayoutInflater.from(context).inflate(R.layout.view_station_price, this, true);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.OpinetStationPriceView);

        tvStnName = findViewById(R.id.tv_station_name);
        tvStnPrice = findViewById(R.id.tv_station_price);

        try {
            int priceUpColor = typedArray.getColor(R.styleable.OpinetStationPriceView_stationPriceUp, 0);
            int priceDownColor = typedArray.getColor(R.styleable.OpinetStationPriceView_stationPriceDown, 0);
            log.i("Color: %d, %d", priceUpColor, priceDownColor);

        } finally {
            typedArray.recycle();
        }

    }

    public void addPriceView(String fuelCode) {
        log.i("addPriceView");
        File stnFile = new File(getContext().getCacheDir(), Constants.FILE_CACHED_STATION_PRICE);
        Uri stnUri = Uri.fromFile(stnFile);

        try(InputStream is = getContext().getContentResolver().openInputStream(stnUri);
            ObjectInputStream ois = new ObjectInputStream(is)){
            Opinet.StationPrice stnPrice = (Opinet.StationPrice)ois.readObject();

            String stnName = stnPrice.getStnName();
            Map<String, Float> price = stnPrice.getStnPrice();
            tvStnName.setText(stnName);
            tvStnPrice.setText(String.valueOf(price.get(fuelCode)));

        } catch(FileNotFoundException e) {
            log.e("FileNotFoundException: %s", e);
        } catch(IOException e) {
            log.e("IOException: %s", e);
        } catch(ClassNotFoundException e) {
            log.e("ClassNotFoundException: %s", e);
        }
    }

    // Set the station price views to be void when the favorite is left empty.
    public void removePriceView() {
        tvStnName.setText("");
        tvStnPrice.setText("");
    }

}

