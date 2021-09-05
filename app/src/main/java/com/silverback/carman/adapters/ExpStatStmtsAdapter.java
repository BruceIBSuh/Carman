package com.silverback.carman.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.silverback.carman.BaseActivity;
import com.silverback.carman.R;
import com.silverback.carman.database.ExpenseBaseDao;
import com.silverback.carman.databinding.CardviewExpenseStmtsBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.Constants;

import java.util.List;

public class ExpStatStmtsAdapter extends RecyclerView.Adapter<ExpStatStmtsAdapter.ExpenseStatHolder> {

    private static final LoggingHelper log = LoggingHelperFactory.create(ExpStatStmtsAdapter.class);

    // Objects
    private List<ExpenseBaseDao.ExpenseStatements> expList;

    // Constructor
    public ExpStatStmtsAdapter(List<ExpenseBaseDao.ExpenseStatements> expList){
        super();
        this.expList = expList;
    }

    @NonNull
    @Override
    public ExpenseStatHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        CardviewExpenseStmtsBinding binding = CardviewExpenseStmtsBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);

        return new ExpenseStatHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseStatHolder holder, int position) {
        holder.bindToExpenseStat(expList.get(position));
    }

    @Override
    public void onBindViewHolder(
            @NonNull ExpenseStatHolder holder, int pos, @NonNull List<Object> payloads) {
        if(payloads.isEmpty()) super.onBindViewHolder(holder, pos, payloads);
        else log.i("payloads: %s", payloads.size());
    }

    @Override
    public int getItemCount() {
        return expList.size();
    }

    public void setStatsStmtList(List<ExpenseBaseDao.ExpenseStatements> expList) {
        this.expList = expList;
    }

    static class ExpenseStatHolder extends RecyclerView.ViewHolder {
        // Objects
        private final CardviewExpenseStmtsBinding binding;
        private final String dateFormat;

        // Constructor
        ExpenseStatHolder(CardviewExpenseStmtsBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            ViewGroup.MarginLayoutParams params =
                    new ViewGroup.MarginLayoutParams(binding.getRoot().getLayoutParams());
            params.setMargins(0, 0, 0, Constants.DIVIDER_HEIGHT_EXPENSE);
            binding.getRoot().setLayoutParams(params);

            dateFormat = binding.getRoot().getResources().getString(R.string.date_format_8);

        }

        // Bind the queried data to the viewholder in BindViewHolder of ExpStatStmtsAdapter
        void bindToExpenseStat(ExpenseBaseDao.ExpenseStatements entity) {
            binding.tvStatDate.setText(BaseActivity.formatMilliseconds(dateFormat, entity.dateTime));
            binding.tvStatMileage.setText(BaseActivity.getDecimalFormatInstance().format(entity.mileage));
            binding.tvStatExpense.setText(BaseActivity.getDecimalFormatInstance().format(entity.totalExpense));
            binding.tvStatLocation.setText((entity.stnName != null)?entity.stnName : entity.serviceCenter);
        }
    }
}
