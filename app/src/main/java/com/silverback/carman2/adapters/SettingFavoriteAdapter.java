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
import com.silverback.carman2.utils.ItemTouchHelperCallback;
import com.silverback.carman2.viewholders.FavoriteItemHolder;

import java.util.Collections;
import java.util.List;

public class SettingFavoriteAdapter extends RecyclerView.Adapter<FavoriteItemHolder> implements
        ItemTouchHelperCallback.RecyclerItemMoveListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(SettingFavoriteAdapter.class);

    // Objects
    private Context context;
    private List<FavoriteProviderEntity> favoriteList;

    // Constructor
    public SettingFavoriteAdapter(List<FavoriteProviderEntity> favorites) {
        log.i("SettingFavoriteAdapter constructor");
        favoriteList = favorites;
    }


    @NonNull
    @Override
    public FavoriteItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        CardView cardView = (CardView) LayoutInflater.from(context)
                .inflate(R.layout.cardview_setting_favorite, parent, false);

        return new FavoriteItemHolder(cardView);
    }

    @Override
    public void onBindViewHolder(@NonNull FavoriteItemHolder holder, int position) {
        log.i("onBindViewHolder");

        final FavoriteProviderEntity provider = favoriteList.get(position);
        log.i("FavoriteProviderEntity in Adapter: %s, %s", provider.providerName, provider.address);
        holder.bindToFavorite(provider);
    }


    @Override
    public int getItemCount() {
        return favoriteList.size();
    }


    @Override
    public void onItemMove(int from, int to) {
        if (from < to) {
            for (int i = from; i < to; i++) {
                Collections.swap(favoriteList, i, i + 1);
            }
        } else {
            for (int i = from; i > to; i--) {
                Collections.swap(favoriteList, i, i - 1);
            }
        }

        notifyItemMoved(from, to);
    }

    @Override
    public void onItemRemove(int pos) {

    }
}
