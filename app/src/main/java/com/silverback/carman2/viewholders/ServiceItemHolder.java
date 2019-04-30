package com.silverback.carman2.viewholders;

import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

public class ServiceItemHolder extends RecyclerView.ViewHolder implements CompoundButton.OnCheckedChangeListener{

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(ServiceItemHolder.class);

    // Objects
    private TextView tvItemName;
    private ConstraintLayout constraintLayout;


    // Constructor
    public ServiceItemHolder(CardView view) {
        super(view);

        tvItemName = view.findViewById(R.id.tv_name);
        constraintLayout = view.findViewById(R.id.constraint_stmts);
        CheckBox chkbox = view.findViewById(R.id.chkbox);

        chkbox.setOnCheckedChangeListener(this);

    }

    public void bindItemToHolder(String item) {
        tvItemName.setText(item);
    }


    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if(isChecked) constraintLayout.setVisibility(View.VISIBLE);
        else constraintLayout.setVisibility(View.GONE);

    }
}
