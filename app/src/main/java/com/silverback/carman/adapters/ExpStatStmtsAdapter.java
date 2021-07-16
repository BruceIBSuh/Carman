package com.silverback.carman.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.silverback.carman.BaseActivity;
import com.silverback.carman.R;
import com.silverback.carman.database.ExpenseBaseDao;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;

import java.util.List;

public class ExpStatStmtsAdapter extends RecyclerView.Adapter<ExpStatStmtsAdapter.ExpenseStatHolder> {

    private static final LoggingHelper log = LoggingHelperFactory.create(ExpStatStmtsAdapter.class);

    // Objects
    private final List<ExpenseBaseDao.ExpenseStatements> expList;

    // Constructor
    public ExpStatStmtsAdapter(List<ExpenseBaseDao.ExpenseStatements> list) {
        super();
        expList = list;
    }

    @NonNull
    @Override
    public ExpenseStatHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        CardView cardView = (CardView)LayoutInflater.from(parent.getContext())
                .inflate(R.layout.cardview_service_stmts, parent, false);

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

    
    static class ExpenseStatHolder extends RecyclerView.ViewHolder {
        // Objects
        private final String dateFormat;
        private final TextView tvDate;
        private final TextView tvMileage;
        private final TextView tvExpense;
        private final TextView tvLocation;

        // Constructor
        ExpenseStatHolder(CardView cardView) {
            super(cardView);

            dateFormat = cardView.getResources().getString(R.string.date_format_8);

            tvDate = cardView.findViewById(R.id.tv_stat_date);
            tvMileage = cardView.findViewById(R.id.tv_stat_mileage);
            tvExpense = cardView.findViewById(R.id.tv_stat_expense);
            tvLocation = cardView.findViewById(R.id.tv_stat_location);
        }

        // Bind the queried data to the viewholder in BindViewHolder of ExpStatStmtsAdapter
        void bindToExpenseStat(ExpenseBaseDao.ExpenseStatements entity) {
            tvDate.setText(BaseActivity.formatMilliseconds(dateFormat, entity.dateTime));
            tvMileage.setText(BaseActivity.getDecimalFormatInstance().format(entity.mileage));
            tvExpense.setText(BaseActivity.getDecimalFormatInstance().format(entity.totalExpense));
            tvLocation.setText((entity.stnName != null)?entity.stnName : entity.serviceCenter);
        }
    }
}
