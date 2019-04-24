package com.silverback.carman2.viewholders;

import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

public class ServiceItemHolder extends RecyclerView.ViewHolder {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(ServiceItemHolder.class);

    // Objects
    private TextView tvItemName;

    // Constructor
    public ServiceItemHolder(CardView view) {
        super(view);

        tvItemName = view.findViewById(R.id.tv_name);

    }

    public void bindItemToHolder(String item) {
        tvItemName.setText(item);
    }


}
