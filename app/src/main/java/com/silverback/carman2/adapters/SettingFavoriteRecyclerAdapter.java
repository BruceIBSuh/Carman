package com.silverback.carman2.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.silverback.carman2.R;
import com.silverback.carman2.database.FavoriteProviderEntity;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.viewholders.FavoriteItemHolder;

import java.util.List;

public class SettingFavoriteRecyclerAdapter extends RecyclerView.Adapter<FavoriteItemHolder> {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(SettingFavoriteRecyclerAdapter.class);

    // Objects
    private Context context;
    private List<FavoriteProviderEntity> favoriteList;

    // Constructor
    public SettingFavoriteRecyclerAdapter(List<FavoriteProviderEntity> favorites) {
        log.i("SettingFavoriteRecyclerAdapter constructor");
        favoriteList = favorites;
    }


    @NonNull
    @Override
    public FavoriteItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        CardView cardView = (CardView) LayoutInflater.from(context)
                .inflate(R.layout.view_card_favorite, parent, false);

        return new FavoriteItemHolder(cardView);
    }

    @Override
    public void onBindViewHolder(@NonNull FavoriteItemHolder holder, int position) {
        log.i("onBindViewHolder");

        final FavoriteProviderEntity provider = favoriteList.get(position);
        holder.bindToFavorite(provider);
    }


    @Override
    public int getItemCount() {
        return favoriteList.size();
    }
}
