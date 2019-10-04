package com.silverback.carman2.adapters;

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
import com.silverback.carman2.utils.ItemTouchHelperCallback;
import com.silverback.carman2.viewholders.FavoriteItemHolder;

import java.util.Collections;
import java.util.List;

public class SettingFavoriteAdapter extends RecyclerView.Adapter<FavoriteItemHolder> implements
        ItemTouchHelperCallback.RecyclerItemMoveListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(SettingFavoriteAdapter.class);

    // Objects
    private List<FavoriteProviderEntity> favoriteList;
    private OnFavoriteAdapterListener mListener;
    private ViewGroup parent;


    public interface OnFavoriteAdapterListener {
        void addFavorite(FavoriteProviderEntity entity);
        void deleteFavorite(FavoriteProviderEntity entity);
    }

    // Constructor
    public SettingFavoriteAdapter(List<FavoriteProviderEntity> favoriteList,
                                  OnFavoriteAdapterListener listener) {

        mListener = listener;
        this.favoriteList = favoriteList;
    }


    @NonNull
    @Override
    public FavoriteItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.parent = parent;
        CardView cardView = (CardView)LayoutInflater.from(parent.getContext())
                .inflate(R.layout.cardview_setting_favorite, parent, false);

        return new FavoriteItemHolder(cardView);
    }

    @Override
    public void onBindViewHolder(@NonNull FavoriteItemHolder holder, int position) {
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

        notifyItemChanged(from, null);
        notifyItemChanged(to, null);
    }

    @Override
    public void onDeleteItem(final int pos) {
        final FavoriteProviderEntity deletedItem = favoriteList.get(pos);
        favoriteList.remove(pos);
        mListener.deleteFavorite(deletedItem);
        notifyItemRemoved(pos);

        Snackbar snackbar = Snackbar.make(parent, "Do you really remove this item?", Snackbar.LENGTH_INDEFINITE);
        snackbar.setAction("UNDO", v -> {
            favoriteList.add(pos, deletedItem);
            mListener.addFavorite(deletedItem);
            notifyItemInserted(pos);
            snackbar.dismiss();
        });

        snackbar.show();
    }

    // Retrieve the first row gas station, invoked by SettingFavor which is set to the Favorite in SettingPreferenceFragment
    // and its price information is to display in the main page,
    public List<FavoriteProviderEntity> getFavoriteList() {
        for(FavoriteProviderEntity entity : favoriteList) {
            log.i("FavoriteList: %s", entity.providerName);
        }
        return favoriteList;
    }
}
