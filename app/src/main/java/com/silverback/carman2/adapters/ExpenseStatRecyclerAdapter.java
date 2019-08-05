package com.silverback.carman2.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.viewholders.ExpenseStatHolder;

public class ExpenseStatRecyclerAdapter extends RecyclerView.Adapter<ExpenseStatHolder> {

    private static final LoggingHelper log = LoggingHelperFactory.create(ExpenseStatRecyclerAdapter.class);

    // Constructor
    public ExpenseStatRecyclerAdapter() {
        super();

    }

    @NonNull
    @Override
    public ExpenseStatHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        CardView cardView = (CardView)LayoutInflater.from(parent.getContext())
                .inflate(R.layout.view_card_statements, parent, false);

        return new ExpenseStatHolder(cardView);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseStatHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return 20;
    }
}
