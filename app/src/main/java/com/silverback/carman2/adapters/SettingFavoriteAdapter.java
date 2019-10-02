package com.silverback.carman2.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.silverback.carman2.R;
import com.silverback.carman2.database.FavoriteProviderEntity;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.utils.Constants;
import com.silverback.carman2.utils.ItemTouchHelperCallback;
import com.silverback.carman2.viewholders.FavoriteItemHolder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;

public class SettingFavoriteAdapter extends RecyclerView.Adapter<FavoriteItemHolder> implements
        ItemTouchHelperCallback.RecyclerItemMoveListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(SettingFavoriteAdapter.class);

    // Objects
    private List<FavoriteProviderEntity> favoriteList;

    // UIs
    private ViewGroup parent;
    private CardView cardView;

    // Constructor
    public SettingFavoriteAdapter(List<FavoriteProviderEntity> favorites) {
        log.i("SettingFavoriteAdapter constructor");
        favoriteList = favorites;
    }


    @NonNull
    @Override
    public FavoriteItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.parent = parent;
        cardView = (CardView)LayoutInflater.from(parent.getContext())
                .inflate(R.layout.cardview_setting_favorite, parent, false);

        return new FavoriteItemHolder(cardView);
    }

    @Override
    public void onBindViewHolder(@NonNull FavoriteItemHolder holder, int position) {
        log.i("onBindViewHolder");

        final FavoriteProviderEntity provider = favoriteList.get(position);
        holder.bindToFavorite(provider);
    }

    @Override
    public void onBindViewHolder(@NonNull FavoriteItemHolder holder, int position, @NonNull List<Object> payloads) {
        super.onBindViewHolder(holder, position, payloads);
        if(position == 0) holder.itemView.setBackgroundColor(Color.RED);
        else holder.itemView.setBackgroundColor(Color.WHITE);

    }

    @Override
    public int getItemCount() {
        return favoriteList.size();
    }


    // The following 2 callback methods
    @Override
    public void onDragItem(int from, int to) {
        if (from < to) for (int i = from; i < to; i++) Collections.swap(favoriteList, i, i + 1);
        else for (int i = from; i > to; i--) Collections.swap(favoriteList, i, i - 1);
        notifyItemMoved(from, to);

        for(FavoriteProviderEntity entity : favoriteList) {
            log.i("Entity : %s, %s", entity._id, entity.providerName);
        }

        notifyItemChanged(from, null);
        notifyItemChanged(to, null);
    }

    @Override
    public void onDeleteItem(final int pos) {
        log.i("onDeleteItem");
        final FavoriteProviderEntity deletedItem = favoriteList.get(pos);
        //final int deletedPos = pos;
        favoriteList.remove(pos);
        notifyItemRemoved(pos);

        Snackbar snackbar = Snackbar.make(parent, "Do you really remove this item?", Snackbar.LENGTH_INDEFINITE);
        snackbar.setAction("UNDO", v -> {
            favoriteList.add(pos, deletedItem);
            notifyItemInserted(pos);
            snackbar.dismiss();
        });

        snackbar.show();
    }

    // Retrieve the first row gas station, invoked by SettingFavor which is set to the Favorite in SettingPreferenceFragment
    // and its price information is to display in the main page,
    public List<FavoriteProviderEntity> getFavoriteList() {
        return favoriteList;
    }
}
