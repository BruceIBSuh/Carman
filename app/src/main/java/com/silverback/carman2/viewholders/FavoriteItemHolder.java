package com.silverback.carman2.viewholders;

import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.silverback.carman2.BaseActivity;
import com.silverback.carman2.R;
import com.silverback.carman2.database.FavoriteProviderEntity;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import java.util.List;

public class FavoriteItemHolder extends RecyclerView.ViewHolder {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(FavoriteItemHolder.class);

    // UIs
    private TextView tvName;
    private TextView tvAddress;
    private ImageView imgLogo;
    public TextView tvFavoriteNum;
    public RatingBar rbFavorite;

    // Constructor
    public FavoriteItemHolder(CardView cardView) {
        super(cardView);

        tvName = cardView.findViewById(R.id.tv_providerName);
        tvAddress = cardView.findViewById(R.id.tv_providerAddrs);
        tvFavoriteNum = cardView.findViewById(R.id.tv_value_favorite);
        rbFavorite = cardView.findViewById(R.id.rb_favorite);
        imgLogo = cardView.findViewById(R.id.img_logo);


    }

    public void bindToFavorite(FavoriteProviderEntity favorite) {

        tvName.setText(favorite.providerName);
        tvAddress.setText(favorite.address);
        if(favorite.providerCode != null) {
            int imgResource = BaseActivity.getGasStationImage(favorite.providerCode);
            if (imgResource != -1) imgLogo.setImageResource(imgResource);
        }

    }

    @SuppressWarnings("ConstantConditions")
    public void bindToEval(DocumentSnapshot snapshot) {
        if(!snapshot.exists()) return;

        if(snapshot.getLong("favorite_num") != null) {
            tvFavoriteNum.setText(String.valueOf(snapshot.getLong("favorite_num")));
        }

        if(snapshot.getLong("eval_num") != null) {
            long evalNum = snapshot.getLong("eval_num");
            long evalSum = snapshot.getLong("eval_sum");

            float rating = evalSum / evalNum;
            rbFavorite.setRating(rating);
        }
    }

}
