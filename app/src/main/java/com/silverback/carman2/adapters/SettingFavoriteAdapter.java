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
import com.google.firebase.firestore.FirebaseFirestore;
import com.silverback.carman2.R;
import com.silverback.carman2.database.FavoriteProviderEntity;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.utils.Constants;
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
    public SettingFavoriteAdapter(
            List<FavoriteProviderEntity> favoriteList,
            SparseArray<DocumentSnapshot> snapshotArray,
            OnFavoriteAdapterListener listener) {

        mListener = listener;
        this.favoriteList = favoriteList;
        this.snapshotArray = snapshotArray;
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

        // Bind the favorite name and address
        holder.bindToFavorite(provider);

        // Bind the favorite registration number and the rating if any.
        if(snapshotArray.size() > 0 && snapshotArray.get(position) != null) holder.bindToEval(snapshotArray.get(position));
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onBindViewHolder(
            @NonNull FavoriteItemHolder holder, int position, @NonNull List<Object> payloads) {

        // Indicate the first-row favorite with difference color.
        if (position == 0) holder.itemView.setBackgroundColor(Color.parseColor("#B0C4DE"));
        else holder.itemView.setBackgroundColor(Color.WHITE);
        
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads);
        }

        for(Object obj : payloads) {
            DocumentSnapshot snapshot = (DocumentSnapshot) obj;
            if(snapshotArray.size() > 0 && snapshotArray.get(position) != null) {
                holder.bindToEval(snapshot);
            }
        }
    }

    @Override
    public int getItemCount() {
        return favoriteList.size();
    }


    // The following 2 callback methods
    // ATTENTION: REFACTOR REQUIRED.
    @Override
    public void onDragItem(int from, int to) {

        // Swap the list elements of FavoriteProviderEntity
        if (from < to) for (int i = from; i < to; i++) Collections.swap(favoriteList, i, i + 1);
        else for (int i = from; i > to; i--) Collections.swap(favoriteList, i, i - 1);

        // Change the SparseArray key of DocumentSnapshot.
        DocumentSnapshot fromSnapshot = snapshotArray.get(from);
        DocumentSnapshot toSnapshot = snapshotArray.get(to);
        snapshotArray.remove(from);
        snapshotArray.remove(to);

        // Change a new key if the snapshot is not null.
        if(fromSnapshot != null) snapshotArray.put(to, fromSnapshot);
        if(toSnapshot != null) snapshotArray.put(from, toSnapshot);

        notifyItemMoved(from, to);

        // Retain the eval data when dragging.
        notifyItemChanged(from, toSnapshot);
        notifyItemChanged(to, fromSnapshot);
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

    private void swapSpaseArrayKey(int from, int to) {

    }

    // Retrieve the first row gas station, invoked by SettingFavor which is set to the Favorite in SettingPreferenceFragment
    // and its price information is to display in the main page,
    public List<FavoriteProviderEntity> getFavoriteList() {
        for(FavoriteProviderEntity entity : favoriteList) {
            log.i("FavoriteList: %s", entity.providerName);
        }
        return favoriteList;
    }

    public void addSnapshotList(int position, DocumentSnapshot snapshot) {
        snapshotArray.put(position, snapshot);
    }
}
