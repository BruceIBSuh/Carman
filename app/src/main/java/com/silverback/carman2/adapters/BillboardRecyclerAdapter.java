package com.silverback.carman2.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.viewholders.BillboardItemHolder;

public class BillboardRecyclerAdapter extends RecyclerView.Adapter<BillboardItemHolder> {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(BillboardRecyclerAdapter.class);

    // Objects
    private CardView cardView;

    // Constructor
    public BillboardRecyclerAdapter() {
        super();
    }


    @NonNull
    @Override
    public BillboardItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        cardView = (CardView)LayoutInflater.from(parent.getContext())
                .inflate(R.layout.view_card_billbaord, parent, false);


        return new BillboardItemHolder(cardView);
    }

    @Override
    public void onBindViewHolder(@NonNull BillboardItemHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return 0;
    }
}
