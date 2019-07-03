package com.silverback.carman2.viewholders;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.silverback.carman2.BaseActivity;
import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Opinet;

import java.text.DecimalFormat;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

public class StationListHolder extends RecyclerView.ViewHolder {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(StationListHolder.class);

    // Objects
    private Context context;
    private static DecimalFormat df;
    private ImageView imgLogo;
    private TextView tvName, tvPrice, tvDistance, tvWashValue, tvWashLabel;
    private String price, distance, carwash;
    private String stnName, stnId;

    static {
        df = BaseActivity.getDecimalFormatInstance();
    }


    // Constructor
    public StationListHolder(CardView cardView) {
        super(cardView);
        this.context = cardView.getContext();
        //this.cardView = cardView;
        imgLogo = cardView.findViewById(R.id.img_logo);
        tvName = cardView.findViewById(R.id.tv_station_name);
        tvPrice = cardView.findViewById(R.id.tv_value_price);
        tvDistance = cardView.findViewById(R.id.tv_value_distance);
        tvWashLabel = cardView.findViewById(R.id.tv_label_carwash);
        tvWashValue = cardView.findViewById(R.id.tv_value_carwash);
    }

    public void bindToStationList(Opinet.GasStnParcelable data) {
        this.stnId = data.getStnId(); // Pass Station ID when clicking a cardview item.
        this.stnName = data.getStnName();
        int resLogo = getGasStationImage(data.getStnCode());
        imgLogo.setImageResource(resLogo);

        log.i("price and distance: %s, %s", data.getStnPrice(), data.getStnDistance());

        // TEST CODING FOR CHECKING IF A STATION HAS BEEN VISITED!!
        tvName.setText(String.format("%s%8s%5s", data.getStnName(), "---", data.getHasVisited()));
        tvPrice.setText(String.format("%s%2s", df.format(data.getStnPrice()), context.getString(R.string.unit_won)));
        tvDistance.setText(String.format("%s%4s", df.format(data.getStnDistance()), context.getString(R.string.unit_meter)));

        if(data.getIsWash()) {
            tvWashLabel.setVisibility(View.VISIBLE);
            tvWashValue.setText(String.valueOf(data.getIsWash()));
            tvWashValue.setVisibility(View.VISIBLE);
        }

        log.i("price and distance: %s, %s", data.getStnPrice(), data.getStnDistance());
    }

    private static int getGasStationImage(String name) {

        int resId = -1;
        switch(name) {
            case "SKE": resId = R.drawable.logo_sk; break;
            case "GSC": resId = R.drawable.logo_gs; break;
            case "HDO": resId = R.drawable.logo_hyundai; break;
            case "SOL": resId = R.drawable.logo_soil; break;
            case "RTO": resId = R.drawable.logo_pb; break;
            case "RTX": resId = R.drawable.logo_express; break;
            case "NHO": resId = R.drawable.logo_nonghyup; break;
            case "E1G": resId = R.drawable.logo_e1g; break;
            case "SKG": resId = R.drawable.logo_skg; break;
            case "ETC": resId = R.drawable.logo_anonym; break;
            default: break;
        }

        return resId;
    }


}