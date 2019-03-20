package com.silverback.carman2.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.TextView;

import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Constants;
import com.silverback.carman2.models.Opinet;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.List;

public class AvgPriceView extends OpinetPriceView {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(AvgPriceView.class);

    // Objects
    private TextView tvAvgPrice;

    // Fields
    //private int priceUpColor, priceDownColor;

    // Constructors of 3 different types. Here, it mainly uses the second one.

    public AvgPriceView(Context context) {
        super(context);
    }

    public AvgPriceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getAttributes(context, attrs);
    }

    public AvgPriceView(Context context, AttributeSet attrs, int defaultStyle) {
        super(context, attrs, defaultStyle);
        getAttributes(context, attrs);
    }

    @SuppressWarnings("ConstantConditions")
    protected void getAttributes(Context context, AttributeSet attrs) {

        //LayoutInflater inflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        //linearLayout = (LinearLayout)inflater.inflate(R.layout.view_avg_price, this, true);
        LayoutInflater.from(context).inflate(R.layout.view_avg_price, this, true);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.AvgPriceView);

        try {
            priceUpColor = typedArray.getColor(R.styleable.AvgPriceView_avgPriceUp, 0);
            priceDownColor = typedArray.getColor(R.styleable.AvgPriceView_avgPriceDown, 0);
            log.i("Color: %d, %d", priceUpColor, priceDownColor);
        } finally {
            typedArray.recycle();
        }

        TextView tvAvgTitle = findViewById(R.id.tv_avg_title);
        tvAvgPrice = findViewById(R.id.tv_avg_price);

        tvAvgTitle.setText(getResources().getString(R.string.general_opinet_subtitle_avgPrice));
    }

    @SuppressWarnings("unchecked")
    public void addPriceView(String fuelCode){

        File avgFile = new File(getContext().getCacheDir(), Constants.FILE_CACHED_AVG_PRICE);
        Uri avgUri = Uri.fromFile(avgFile);

        try(InputStream is = getContext().getContentResolver().openInputStream(avgUri);
            ObjectInputStream ois = new ObjectInputStream(is)){
            List<Opinet.OilPrice> avgPrice = (List<Opinet.OilPrice>)ois.readObject();

            for (Opinet.OilPrice opinet : avgPrice) {
                if (opinet.getProductCode().matches(fuelCode)) {
                    float price = opinet.getPrice();
                    float diff = opinet.getDiff();
                    log.i("AvgPriceView: %s, %s, %s", opinet.getProductCode(), price, diff);

                    setColoredTextView(tvAvgPrice, price, diff);
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

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }


}