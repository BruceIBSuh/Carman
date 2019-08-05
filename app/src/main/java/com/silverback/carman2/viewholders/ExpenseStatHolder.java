package com.silverback.carman2.viewholders;

import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.silverback.carman2.R;

public class ExpenseStatHolder extends RecyclerView.ViewHolder {

    public ExpenseStatHolder(CardView cardView) {
        super(cardView);

        TextView tvDate = cardView.findViewById(R.id.tv_stat_date);
        TextView tvMileage = cardView.findViewById(R.id.tv_stat_mileage);
        TextView tvExpense = cardView.findViewById(R.id.tv_stat_expense);
        TextView tvLocation = cardView.findViewById(R.id.tv_stat_location);

        tvDate.setText("190805");
        tvMileage.setText("100");
        tvExpense.setText("50000");
        tvLocation.setText("Text Auto Service");
    }
}
