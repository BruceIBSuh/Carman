package com.silverback.carman.adapters;

import android.graphics.Color;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentSnapshot;
import com.silverback.carman.R;
import com.silverback.carman.database.FavoriteProviderEntity;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.Constants;
import com.silverback.carman.utils.ItemTouchHelperCallback;
import com.silverback.carman.viewholders.FavoriteItemHolder;

import java.util.Collections;
import java.util.List;

/**
 *
 */

public class SettingFavoriteAdapter extends RecyclerView.Adapter<FavoriteItemHolder> implements
        ItemTouchHelperCallback.RecyclerItemMoveListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(SettingFavoriteAdapter.class);

    // Objects
    private List<FavoriteProviderEntity> favoriteList;
    private SparseArray<DocumentSnapshot> sparseSnapshotArray;
    private OnFavoriteAdapterListener mListener;
    private ViewGroup parent;


    public interface OnFavoriteAdapterListener {
        void setFirstPlaceholder(int category, String stnId);
        void deleteFavorite(int cqtegory, int position);
    }

    // Constructor
    public SettingFavoriteAdapter(
            List<FavoriteProviderEntity> favoriteList,
            SparseArray<DocumentSnapshot> snapshotArray,
            OnFavoriteAdapterListener listener) {

        mListener = listener;
        this.favoriteList = favoriteList;
        this.sparseSnapshotArray = snapshotArray;
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
        if(sparseSnapshotArray.size() > 0 && sparseSnapshotArray.get(position) != null)
            holder.bindToEval(sparseSnapshotArray.get(position));
    }

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
            if(sparseSnapshotArray.size() > 0 && sparseSnapshotArray.get(position) != null) {
                holder.bindToEval(snapshot);
            }
        }
    }

    @Override
    public int getItemCount() {
        return favoriteList.size();
    }


    // The following 2 callback methods
    @Override
    public void onDragItem(int from, int to) {

        DocumentSnapshot fromSnapshot;
        DocumentSnapshot toSnapshot;

        // Drag an item down, swapping the positions.
        if (from < to) {
            fromSnapshot = sparseSnapshotArray.get(from);
            toSnapshot = sparseSnapshotArray.get(to);
            sparseSnapshotArray.remove(from);
            sparseSnapshotArray.remove(to);

            for (int i = from; i < to; i++) {
                // Swap the FavorteProviderEntity positioned at from with that positioned at to.
                Collections.swap(favoriteList, i, i + 1);
                // Swap the DocumentSnapshot positioned at the key of from and the key of to.
                if(fromSnapshot != null) sparseSnapshotArray.put(to, fromSnapshot);
                if(toSnapshot != null) sparseSnapshotArray.put(from, toSnapshot);
            }

        } else {
            fromSnapshot = sparseSnapshotArray.get(from);
            toSnapshot = sparseSnapshotArray.get(to);
            sparseSnapshotArray.remove(from);
            sparseSnapshotArray.remove(to);

            for (int i = from; i > to; i--) {
                Collections.swap(favoriteList, i, i - 1);
                // Change a new key if the snapshot is not null.
                if(fromSnapshot != null) sparseSnapshotArray.put(to, fromSnapshot);
                if(toSnapshot != null) sparseSnapshotArray.put(from, toSnapshot);
            }
        }

        notifyItemMoved(from, to);

        // Retain the eval data when dragging.
        notifyItemChanged(from, toSnapshot);
        notifyItemChanged(to, fromSnapshot);

        // If an item is dragged up to the first place in the list, it is notified that a new favorite
        // is set up to the first placeholder, ready to display the price in MainActivity.
        if(to == 0) {
            log.i("First set favorite: %s", favoriteList.get(to).providerId);
            mListener.setFirstPlaceholder(Constants.GAS, favoriteList.get(to).providerId);
        }
    }

    @Override
    public void onDeleteItem(final int pos) {
        Snackbar snackbar = Snackbar.make(parent, "Do you really remove this item?", Snackbar.LENGTH_SHORT);
        snackbar.setAction("REMOVE", v -> {
            mListener.deleteFavorite(Constants.GAS, pos);

            //favoriteList.remove(pos);
            //notifyItemRemoved(pos);
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
        return favoriteList;
    }


    // Invoked from SettingFavorGasFragment/SettingFavorSvcFragment as a provider has retrieved
    // any evaluation data from Firestore.
    public void addSparseSnapshotArray(int position, DocumentSnapshot snapshot) {
        sparseSnapshotArray.put(position, snapshot);
    }
}
