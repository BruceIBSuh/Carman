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
import com.silverback.carman2.utils.Constants;
import com.silverback.carman2.models.Opinet;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.List;

public class SidoPriceView extends OpinetPriceView {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(SidoPriceView.class);

    // Objects
    private TextView tvSidoName, tvSidoPrice;


    // Constructors of 3 different types. Here, it mainly uses the second one.

    public SidoPriceView(Context context) {
        super(context);
    }

    public SidoPriceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getAttributes(context, attrs);
    }

    public SidoPriceView(Context context, AttributeSet attrs, int defaultStyle) {
        super(context, attrs, defaultStyle);
        getAttributes(context, attrs);
    }


    @SuppressWarnings("ConstantConditions")
    protected void getAttributes(Context context, AttributeSet attrs) {

        //LayoutInflater inflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        //linearLayout = (LinearLayout)inflater.inflate(R.layout.view_avg_price, this, true);
        LayoutInflater.from(context).inflate(R.layout.view_sido_price, this, true);
        tvSidoName = findViewById(R.id.tv_sido_name);
        tvSidoPrice = findViewById(R.id.tv_sido_price);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.sidoPriceView);

        try {
            priceUpColor = typedArray.getColor(R.styleable.sidoPriceView_sidoPriceUp, 0);
            priceDownColor = typedArray.getColor(R.styleable.sidoPriceView_sidoPriceDown, 0);
            log.i("Color: %d, %d", priceUpColor, priceDownColor);

        } finally {
            typedArray.recycle();
        }

    }

    @SuppressWarnings("unchecked")
    public void addPriceView(String fuelCode) {

        File sidoFile = new File(getContext().getCacheDir(), Constants.FILE_CACHED_SIDO_PRICE);
        Uri avgUri = Uri.fromFile(sidoFile);

        try(InputStream is = getContext().getContentResolver().openInputStream(avgUri);
            ObjectInputStream ois = new ObjectInputStream(is)){
            List<Opinet.SidoPrice> sidoPrice = (List<Opinet.SidoPrice>)ois.readObject();

            for (Opinet.SidoPrice opinet : sidoPrice) {
                if (opinet.getProductCd().matches(fuelCode)) {
                    String sidoName = opinet.getSidoName();
                    float price = opinet.getPrice();
                    float diff = opinet.getDiff();
                    log.i("SidoPriceView: %s, %s, %s", sidoName, price, diff);
                    tvSidoName.setText(sidoName);
                    setColoredTextView(tvSidoPrice, price, diff);
                    break;
                }
            }

        } catch(FileNotFoundException e) {
            log.e("FileNotFoundException: %s", e);
        } catch(IOException e) {
            log.e("IOException: %s", e);
        } catch(ClassNotFoundException e) {
            log.e("ClassNotFoundException: %s", e);
        }

    }
}
