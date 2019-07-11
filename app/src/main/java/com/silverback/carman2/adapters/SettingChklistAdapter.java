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

public class SettingChklistAdapter extends RecyclerView.Adapter<SettingChklistAdapter.SettingChklistHolder> {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(SettingChklistAdapter.class);

    private String[] serviceItems;

    public SettingChklistAdapter(String[] items) {
        log.i("SettingChklistAdapter");
        serviceItems = items;
    }


    @NonNull
    @Override
    public SettingChklistHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        log.i("onCreateViewHolder");
        CardView cardView = (CardView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.cardview_setting_chklist, parent, false);

        return new SettingChklistHolder(cardView);
    }

    @Override
    public void onBindViewHolder(@NonNull SettingChklistHolder holder, int position) {
        log.i("onBindViewHolder");
        holder.tvItemName.setText(serviceItems[position]);
    }

    @Override
    public int getItemCount() {
        return serviceItems.length;
    }

    static class SettingChklistHolder extends RecyclerView.ViewHolder {

        TextView tvItemName;

        SettingChklistHolder(View v) {
            super(v);

            tvItemName = v.findViewById(R.id.tv_setting_item);
        }

    }
}
