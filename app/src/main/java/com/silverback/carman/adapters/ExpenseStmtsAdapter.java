package com.silverback.carman.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.silverback.carman.BaseActivity;
import com.silverback.carman.R;
import com.silverback.carman.database.ExpenseBaseDao;
import com.silverback.carman.databinding.CardviewExpenseStmtsBinding;
import com.silverback.carman.databinding.RecyclerExpenseStmtsBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.Constants;

import java.util.List;

public class ExpenseStmtsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final LoggingHelper log = LoggingHelperFactory.create(ExpenseStmtsAdapter.class);

    // Objects
    private List<ExpenseBaseDao.ExpenseStatements> expList;
    private RecyclerExpenseStmtsBinding binding;
    private String dateFormat;

    public ExpenseStmtsAdapter(List<ExpenseBaseDao.ExpenseStatements> expList){
        super();
        this.expList = expList;
    }

    // ViewHolder
    private static class ExpenseStmtsHolder extends RecyclerView.ViewHolder {
        public ExpenseStmtsHolder(View view) {
            super(view);
            ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(view.getLayoutParams());
            params.setMargins(0, 0, 0, Constants.DIVIDER_HEIGHT_EXPENSE);
            view.setLayoutParams(params);
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        binding = RecyclerExpenseStmtsBinding.inflate(inflater, parent, false);
        dateFormat = parent.getContext().getResources().getString(R.string.date_format_8);
        return new ExpenseStmtsHolder(binding.getRoot());
    }


    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        binding.tvStmtsDate.setText(BaseActivity.formatMilliseconds(dateFormat, expList.get(position).dateTime));
        binding.tvStmtsMileage.setText(BaseActivity.getDecimalFormatInstance().format(expList.get(position).mileage));
        binding.tvStmtsExpense.setText(BaseActivity.getDecimalFormatInstance().format(expList.get(position).totalExpense));
        binding.tvStmtsProvider.setText((expList.get(position).stnName != null)?expList.get(position).stnName : expList.get(position).serviceCenter);

    }

    @Override
    public void onBindViewHolder(
            @NonNull RecyclerView.ViewHolder holder, int pos, @NonNull List<Object> payloads) {
        if(payloads.isEmpty()) super.onBindViewHolder(holder, pos, payloads);
        else {
            log.i("payloads: %s", payloads.size());
        }
    }

    @Override
    public int getItemCount() {
        return expList.size();
    }

    public void setStatsStmtList(List<ExpenseBaseDao.ExpenseStatements> expList) {
        this.expList = expList;
    }


}
