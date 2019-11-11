package com.silverback.carman2.viewholders;

import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.silverback.carman2.BaseActivity;
import com.silverback.carman2.R;
import com.silverback.carman2.database.ExpenseBaseDao;

public class ExpenseStatHolder extends RecyclerView.ViewHolder {

    // Objects
    private String dateFormat;
    private TextView tvDate, tvMileage, tvExpense, tvLocation;

    // Constructor
    public ExpenseStatHolder(CardView cardView) {
        super(cardView);

        dateFormat = cardView.getResources().getString(R.string.date_format_8);

        tvDate = cardView.findViewById(R.id.tv_stat_date);
        tvMileage = cardView.findViewById(R.id.tv_stat_mileage);
        tvExpense = cardView.findViewById(R.id.tv_stat_expense);
        tvLocation = cardView.findViewById(R.id.tv_stat_location);
    }

    // Bind the queried data to the viewholder in BindViewHolder of ExpStatStmtsAdapter
    public void bindToExpenseStat(ExpenseBaseDao.ExpenseStatements entity) {
        tvDate.setText(BaseActivity.formatMilliseconds(dateFormat, entity.dateTime));
        tvMileage.setText(BaseActivity.getDecimalFormatInstance().format(entity.mileage));
        tvExpense.setText(BaseActivity.getDecimalFormatInstance().format(entity.totalExpense));
        tvLocation.setText((entity.stnName != null)?entity.stnName : entity.serviceCenter);
    }
}
