package com.silverback.carman2.viewholders;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Opinet;

import java.util.Locale;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

public class StationsViewHolder extends RecyclerView.ViewHolder {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(StationsViewHolder.class);

    // UI's
    private ImageView imgLogo;
    private TextView tvName, tvPrice, tvDistance;
    private String price, distance, carwash;
    private String stnCode;

    // Constructor
    public StationsViewHolder(CardView cardView) {
        super(cardView);

        imgLogo = cardView.findViewById(R.id.img_logo);
        tvName = cardView.findViewById(R.id.tv_station_name);
        tvPrice = cardView.findViewById(R.id.tv_price);
        tvDistance = cardView.findViewById(R.id.tv_distance);
        //tvWash = cardView.findViewById(R.id.tv_carwash);

        price = cardView.getResources().getString(R.string.general_station_price);
        distance = cardView.getResources().getString(R.string.general_station_distance);
        carwash = cardView.getResources().getString(R.string.general_carwash);
    }

    public void bindToStationList(Opinet.GasStnParcelable data) {
        this.stnCode = data.getStnId(); // Pass Station ID when clicking a cardview item.

        int resLogo = getGasStationImage(data.getStnCode());
        imgLogo.setImageResource(resLogo);
        tvName.setText(data.getStnName());
        tvPrice.setText(String.format(Locale.getDefault(),"%s:%5d%2s", price, (int)data.getStnPrice(), "원"));
        tvDistance.setText(String.format(Locale.getDefault(),"%s:%5d%2s", distance, (int)data.getDist(), "m"));
        //tvWash.setText(String.format(Locale.getDefault(), "%s:%5s", carwash, data.getIsCarWash()));
    }

    public String getStationCode() {
        return stnCode;
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
