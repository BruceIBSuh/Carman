package com.silverback.carman2.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.silverback.carman2.R;
import com.silverback.carman2.database.ExpenseBaseDao;
import com.silverback.carman2.database.ExpenseBaseEntity;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.viewholders.ExpenseStatHolder;

import java.util.List;

public class ExpenseStatRecyclerAdapter extends RecyclerView.Adapter<ExpenseStatHolder> {

    private static final LoggingHelper log = LoggingHelperFactory.create(ExpenseStatRecyclerAdapter.class);

    // Objects
    private List<ExpenseBaseDao.ExpenseStatements> expList;

    // Constructor
    public ExpenseStatRecyclerAdapter(List<ExpenseBaseDao.ExpenseStatements> list) {
        super();
        expList = list;
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
        holder.bindToExpenseStat(expList.get(position));
    }

    @Override
    public int getItemCount() {
        return expList.size();
    }
}
