package com.silverback.carman2.adapters;

import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

public class StationListAdapter extends RecyclerView.Adapter<StationListAdapter.StationViewHolder> {

    @NonNull
    @Override
    public StationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull StationViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return 0;
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class StationViewHolder extends RecyclerView.ViewHolder {

        public CardView cardView;
        public StationViewHolder(CardView cardView) {
            super(cardView);
            this.cardView = cardView;
        }

    }
}
