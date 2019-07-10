package com.silverback.carman2.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

public class SettingServiceItemAdapter extends RecyclerView.Adapter<SettingServiceItemAdapter.SettingServiceItemHolder> {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(SettingServiceItemAdapter.class);

    private String[] serviceItems;

    public SettingServiceItemAdapter(String[] items) {
        serviceItems = items;
    }


    @NonNull
    @Override
    public SettingServiceItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        CardView cardView = (CardView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.cardview_setting_chklist, parent, false);

        return new SettingServiceItemHolder(cardView);
    }

    @Override
    public void onBindViewHolder(@NonNull SettingServiceItemHolder holder, int position) {
        log.i("onBindViewHolder");
        holder.tvItemName.setText(serviceItems[position]);
    }

    @Override
    public int getItemCount() {
        return 0;
    }

    static class SettingServiceItemHolder extends RecyclerView.ViewHolder {

        TextView tvItemName;

        SettingServiceItemHolder(View v) {
            super(v);

            tvItemName = v.findViewById(R.id.tv_setting_item);
        }

    }
}
