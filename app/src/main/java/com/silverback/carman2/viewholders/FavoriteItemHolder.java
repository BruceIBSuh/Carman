package com.silverback.carman2.viewholders;

import android.widget.ImageView;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.silverback.carman2.R;
import com.silverback.carman2.database.FavoriteProviderEntity;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

public class FavoriteItemHolder extends RecyclerView.ViewHolder {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(FavoriteItemHolder.class);

    // UIs
    private TextView tvName;
    private TextView tvAddress;
    private ImageView imgLogo;

    // Constructor
    public FavoriteItemHolder(CardView cardView) {
        super(cardView);

        tvName = cardView.findViewById(R.id.tv_stnName);
        tvAddress = cardView.findViewById(R.id.tv_stnAddrs);
        imgLogo = cardView.findViewById(R.id.img_logo);
    }

    public void bindToFavorite(FavoriteProviderEntity favorite) {
        log.i("Address: %s", favorite.address);
        tvName.setText(favorite.providerName);
        tvAddress.setText(favorite.address);

        //int imgResource = BaseActivity.getGasStationImage(favorite.providerCode);
        log.i("image resource: %s", favorite.providerCode);
        //imgLogo.setImageResource(imgResource);
    }
}
