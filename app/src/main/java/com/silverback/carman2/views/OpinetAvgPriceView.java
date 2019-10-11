package com.silverback.carman2.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
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
import java.lang.ref.WeakReference;
import java.util.List;

public class OpinetAvgPriceView extends OpinetPriceView {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(OpinetAvgPriceView.class);

    // Objects
    private WeakReference<View> mThisView;
    private TextView tvAvgPrice;

    // Fields
    //private int priceUpColor, priceDownColor; //Inherited from OpinetPriceView

    // Constructors of 3 different types. Here, it mainly uses the second one.
    public OpinetAvgPriceView(Context context) {
        super(context);
    }

    public OpinetAvgPriceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getAttributes(context, attrs);
    }

    public OpinetAvgPriceView(Context context, AttributeSet attrs, int defaultStyle) {
        super(context, attrs, defaultStyle);
        getAttributes(context, attrs);
    }

    @SuppressWarnings("ConstantConditions")
    protected void getAttributes(Context context, AttributeSet attrs) {

        //LayoutInflater inflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        //linearLayout = (LinearLayout)inflater.inflate(R.layout.view_avg_price, this, true);
        LayoutInflater.from(context).inflate(R.layout.view_avg_price, this, true);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.OpinetAvgPriceView);

        try {
            priceUpColor = typedArray.getColor(R.styleable.OpinetAvgPriceView_avgPriceUp, 0);
            priceDownColor = typedArray.getColor(R.styleable.OpinetAvgPriceView_avgPriceDown, 0);
            log.i("Color: %d, %d", priceUpColor, priceDownColor);
        } finally {
            typedArray.recycle();
        }

        TextView tvAvgTitle = findViewById(R.id.tv_avg_title);
        tvAvgPrice = findViewById(R.id.tv_avg_price);

        tvAvgTitle.setText(getResources().getString(R.string.general_opinet_subtitle_avgPrice));
    }

    public void showAvgView() {

        mThisView = new WeakReference<View>(this);
        View localView = mThisView.get();

        if(localView != null) {
            localView.setVisibility(View.VISIBLE);
        }
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

    @Override
    protected void onDetachedFromWindow() {
        if(mThisView != null) {
            mThisView.clear();
            mThisView = null;
        }

        super.onDetachedFromWindow();
    }
}