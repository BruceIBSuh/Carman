package com.silverback.carman2.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.TextView;

import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Opinet;
import com.silverback.carman2.utils.Constants;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.Map;

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
        tvStnName = findViewById(R.id.tv_station_name);
        tvStnPrice = findViewById(R.id.tv_station_price);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.OpinetStationPriceView);
        try {
            priceUpColor = typedArray.getColor(R.styleable.OpinetStationPriceView_stnPriceUp, 0);
            priceDownColor = typedArray.getColor(R.styleable.OpinetStationPriceView_stnPriceDown, 0);
            log.i("Color: %d, %d", priceUpColor, priceDownColor);

        } finally {
            typedArray.recycle();
        }

    }

    public void addPriceView(String fuelCode) {
        File stnFile = new File(getContext().getCacheDir(), Constants.FILE_CACHED_STATION_PRICE);
        Uri stnUri = Uri.fromFile(stnFile);

        Float price = null;
        Float diff = null;
        try(InputStream is = getContext().getContentResolver().openInputStream(stnUri);
            ObjectInputStream ois = new ObjectInputStream(is)){
            Opinet.StationPrice stnPrice = (Opinet.StationPrice)ois.readObject();

            log.i("First Favorite Station: %s, %s", stnPrice.getStnName(), stnPrice.getDiff());

            String stnName = stnPrice.getStnName();
            tvStnName.setText(stnName);

            Map<String, Float> mapPrice = stnPrice.getStnPrice();
            Map<String, Float> mapDiff = stnPrice.getDiff();
            price = mapPrice.get(fuelCode);
            diff = mapDiff.get(fuelCode);

            if(price == null || diff == null) throw new NullPointerException();
            else setColoredTextView(tvStnPrice, price, diff);


        } catch(FileNotFoundException e) {
            log.e("FileNotFoundException: %s", e);
        } catch(IOException e) {
            log.e("IOException: %s", e);
        } catch(ClassNotFoundException e) {
            log.e("ClassNotFoundException: %s", e);
        } catch(NullPointerException e) {
            log.e("NullPointerException: %s", e);
            if(price == null) setColoredTextView(tvStnPrice, 0, 0);
            else if(diff == null) setColoredTextView(tvStnPrice, price, 0);
        }
    }

    // Set the station price views to be void when the favorite is left empty.
    public void removePriceView() {
        tvStnName.setText("");
        tvStnPrice.setText("");
    }

}

