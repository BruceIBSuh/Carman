package com.silverback.carman2.adapters;

import android.graphics.Color;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentSnapshot;
import com.silverback.carman2.R;
import com.silverback.carman2.database.FavoriteProviderEntity;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.utils.ItemTouchHelperCallback;
import com.silverback.carman2.viewholders.FavoriteItemHolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SettingFavoriteAdapter extends RecyclerView.Adapter<FavoriteItemHolder> implements
        ItemTouchHelperCallback.RecyclerItemMoveListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(SettingFavoriteAdapter.class);

    // Objects
    private List<FavoriteProviderEntity> favoriteList;
    private SparseArray<DocumentSnapshot> snapshotArray;
    private OnFavoriteAdapterListener mListener;
    private ViewGroup parent;


    public interface OnFavoriteAdapterListener {
        //void addFavorite(FavoriteProviderEntity entity);
        void deleteFavorite(FavoriteProviderEntity entity);
    }

    // Constructor
    public SettingFavoriteAdapter(List<FavoriteProviderEntity> favoriteList,
                                  OnFavoriteAdapterListener listener) {

        mListener = listener;
        this.favoriteList = favoriteList;
        snapshotArray = new SparseArray<>();
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

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onBindViewHolder(
            @NonNull FavoriteItemHolder holder, int position, @NonNull List<Object> payloads) {

        // Indicate the first-row favorite with difference color.
        if (position == 0) holder.itemView.setBackgroundColor(Color.parseColor("#99FF99"));
        else holder.itemView.setBackgroundColor(Color.WHITE);
        
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads);
        }

        for(Object obj : payloads) {
            //if (obj instanceof DocumentSnapshot) {
                DocumentSnapshot snapshot = (DocumentSnapshot) obj;
                if(snapshot.getLong("favorite_num") != null || snapshot.getLong("eval_num") != null){
                    snapshotArray.put(position, snapshot);
                }

                // Retrieve the number of registration with Favorite
                if (snapshot.getLong("favorite_num") != null) {
                    int value = snapshot.getLong("favorite_num").intValue();
                    log.i("Favorite Number:%s", value);
                    holder.tvFavoriteNum.setText(String.valueOf(value));
                } //else log.w("Favorite field not existing");

                // Retrieve the ratingbar data
                if (snapshot.getLong("eval_num") != null) {
                    int evalNum = snapshot.getLong("eval_num").intValue();
                    int evalSum = snapshot.getLong("eval_sum").intValue();
                    float avg = (float) (evalSum / evalNum);
                    log.i("Rating average: %s", avg);
                    holder.rbFavorite.setRating(avg);
                } //else log.w("eval fields not existing");
            //}
        }
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

        log.i("Snapshot: %s, %s", snapshotArray.valueAt(from), snapshotArray.valueAt(to));

        // Retain the eval data when dragging.
        notifyItemChanged(from, snapshotArray.valueAt(to));
        notifyItemChanged(to, snapshotArray.valueAt(from));

        snapshotArray.setValueAt(from, snapshotArray.valueAt(to));
        snapshotArray.setValueAt(to, snapshotArray.valueAt(from));
    }

    @Override
    public void onDeleteItem(final int pos) {

        final FavoriteProviderEntity deletedItem = favoriteList.get(pos);

        Snackbar snackbar = Snackbar.make(parent, "Do you really remove this item?", Snackbar.LENGTH_SHORT);
        snackbar.setAction("REMOVE", v -> {
            favoriteList.remove(pos);
            notifyItemRemoved(pos);
            mListener.deleteFavorite(deletedItem);
            snackbar.dismiss();

        }).addCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar snackkbar, int event) {
                if(event == Snackbar.Callback.DISMISS_EVENT_TIMEOUT) {
                    notifyItemChanged(pos);
                }
            }
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
