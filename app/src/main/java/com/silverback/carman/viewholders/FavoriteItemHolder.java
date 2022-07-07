package com.silverback.carman.viewholders;

import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.silverback.carman.BaseActivity;
import com.silverback.carman.R;
import com.silverback.carman.database.FavoriteProviderEntity;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;

public class FavoriteItemHolder extends RecyclerView.ViewHolder {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(FavoriteItemHolder.class);

    // UIs
    private TextView tvName;
    private TextView tvAddress;
    private ImageView imgLogo;
    private TextView tvFavoriteNum;
    private TextView tvEvalNum;
    private RatingBar rbFavorite;

    // Constructor
    public FavoriteItemHolder(CardView cardView) {
        super(cardView);

        tvName = cardView.findViewById(R.id.tv_providerName);
        tvAddress = cardView.findViewById(R.id.tv_providerAddrs);
        tvFavoriteNum = cardView.findViewById(R.id.tv_value_register_favorite);
        tvEvalNum = cardView.findViewById(R.id.tv_value_number);
        rbFavorite = cardView.findViewById(R.id.rb_favorite);
        imgLogo = cardView.findViewById(R.id.img_logo);


    }

    public void bindToFavorite(FavoriteProviderEntity favorite) {
        tvName.setText(favorite.stationName);
        tvAddress.setText(favorite.addrsNew);
        if(favorite.company != null) {
            int imgResource = BaseActivity.getGasStationImage(favorite.company);
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
            tvEvalNum.setText(String.valueOf(snapshot.getLong("eval_num")));

            long evalNum = snapshot.getLong("eval_num");
            long evalSum = snapshot.getLong("eval_sum");

            float rating = evalSum / evalNum;
            rbFavorite.setStepSize(0.5f);
            rbFavorite.setRating(rating);
        }
    }

}
