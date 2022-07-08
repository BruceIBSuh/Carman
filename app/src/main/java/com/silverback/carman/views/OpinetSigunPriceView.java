package com.silverback.carman.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.TextView;

import com.silverback.carman.R;
import com.silverback.carman.utils.Constants;
import com.silverback.carman.viewmodels.Opinet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

public class OpinetSigunPriceView extends OpinetPriceView {

    // Logging
    //private static final LoggingHelper log = LoggingHelperFactory.create(OpinetSidoPriceView.class);
    // Objects
    private TextView tvSigunName, tvSigunPrice;

    // Constructors of 3 different types. Here, it mainly uses the second one.
    public OpinetSigunPriceView(Context context) {
        super(context);
    }
    public OpinetSigunPriceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getAttributes(context, attrs);
    }
    public OpinetSigunPriceView(Context context, AttributeSet attrs, int defaultStyle) {
        super(context, attrs, defaultStyle);
        getAttributes(context, attrs);
    }

    //@SuppressWarnings("ConstantConditions")
    protected void getAttributes(Context context, AttributeSet attrs) {

        //LayoutInflater inflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        //linearLayout = (LinearLayout)inflater.inflate(R.layout.view_avg_price, this, true);
        LayoutInflater.from(context).inflate(R.layout.view_sigun_price, this, true);
        tvSigunName = findViewById(R.id.tv_sigun_name);
        tvSigunPrice = findViewById(R.id.tv_sigun_price);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.OpinetSigunPriceView);
        try {
            priceUpColor = typedArray.getColor(R.styleable.OpinetSigunPriceView_sigunPriceUp, 0);
            priceDownColor = typedArray.getColor(R.styleable.OpinetSigunPriceView_sigunPriceDown, 0);
        } finally {
            typedArray.recycle();
        }

    }

    public void addPriceView(String fuelCode) {
        File sigunFile = new File(getContext().getCacheDir(), Constants.FILE_CACHED_SIGUN_PRICE);
        Uri sigunUri = Uri.fromFile(sigunFile);
        try(InputStream is = getContext().getContentResolver().openInputStream(sigunUri);
            ObjectInputStream ois = new ObjectInputStream(is)) {
            //List<Opinet.SigunPrice> sigunPrice = (List<Opinet.SigunPrice>)ois.readObject();
            Object obj = ois.readObject();
            Iterable<?> itr = (Iterable<?>)obj;
            for(Object o : itr) {
                Opinet.SigunPrice sigun = (Opinet.SigunPrice) o;
                if(sigun.getProductCd().matches(fuelCode)) {
                    String sigunName = sigun.getSigunName();
                    float price = sigun.getPrice();
                    float diff = sigun.getDiff();
                    tvSigunName.setText(sigunName);
                    setColoredTextView(tvSigunPrice, price, diff);
                }
            }
        } catch(IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

}
