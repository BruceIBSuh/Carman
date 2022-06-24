package com.silverback.carman.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.TextView;

import com.silverback.carman.R;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.Constants;
import com.silverback.carman.viewmodels.Opinet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

public class OpinetSidoPriceView extends OpinetPriceView {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(OpinetSidoPriceView.class);
    // Objects
    private TextView tvSidoName, tvSidoPrice;
    // Constructors of 3 different types. Here, it mainly uses the second one.
    public OpinetSidoPriceView(Context context) {
        super(context);
    }
    public OpinetSidoPriceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getAttributes(context, attrs);
    }
    public OpinetSidoPriceView(Context context, AttributeSet attrs, int defaultStyle) {
        super(context, attrs, defaultStyle);
        getAttributes(context, attrs);
    }

    protected void getAttributes(Context context, AttributeSet attrs) {
        //LayoutInflater inflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        //linearLayout = (LinearLayout)inflater.inflate(R.layout.view_avg_price, this, true);
        LayoutInflater.from(context).inflate(R.layout.view_sido_price, this, true);
        tvSidoName = findViewById(R.id.tv_sido_name);
        tvSidoPrice = findViewById(R.id.tv_sido_price);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.OpinetSidoPriceView);
        try {
            priceUpColor = typedArray.getColor(R.styleable.OpinetSidoPriceView_sidoPriceUp, 0);
            priceDownColor = typedArray.getColor(R.styleable.OpinetSidoPriceView_sidoPriceDown, 0);

        } finally {
            typedArray.recycle();
        }

    }

    public void addPriceView(String fuelCode) {
        File sidoFile = new File(getContext().getCacheDir(), Constants.FILE_CACHED_SIDO_PRICE);
        Uri uriSido = Uri.fromFile(sidoFile);
        try(InputStream is = getContext().getContentResolver().openInputStream(uriSido);
            ObjectInputStream ois = new ObjectInputStream(is)){
            //List<Opinet.SidoPrice> sidoPrice = (List<Opinet.SidoPrice>)ois.readObject();
            Object obj = ois.readObject();
            Iterable<?> itr = (Iterable<?>)obj;
            for(Object x : itr) {
                Opinet.SidoPrice sido = (Opinet.SidoPrice) x;
                if(sido.getProductCd().matches(fuelCode)) {
                    String sidoName = sido.getSidoName();
                    float price = sido.getPrice();
                    float diff = sido.getDiff();
                    tvSidoName.setText(sidoName);
                    setColoredTextView(tvSidoPrice, price, diff);
                }
            }
        } catch(IOException | ClassNotFoundException e) { e.printStackTrace();}

    }
}
