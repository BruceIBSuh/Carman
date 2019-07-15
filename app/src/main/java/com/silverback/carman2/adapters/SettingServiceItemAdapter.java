package com.silverback.carman2.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import java.util.List;

public class SettingServiceItemAdapter extends RecyclerView.Adapter<SettingServiceItemAdapter.SettingServiceItemHolder> {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(SettingServiceItemAdapter.class);

    // Objects & UIs
    private List<String> serviceItems;
    public EditText etMileage, etMonth;
    private ImageButton btnUp, btnDown, btnDel;

    // Fields
    private String mileage, month;
    private boolean isEditMode;

    public SettingServiceItemAdapter(List<String> items) {
        log.i("SettingServiceItemAdapter");
        serviceItems = items;
    }



    @NonNull
    @Override
    public SettingServiceItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        log.i("onCreateViewHolder");
        /*
        CardView cardView = (CardView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.cardview_setting_chklist, parent, false);
        */

        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.view_setting_svc_items, parent, false);



        return new SettingServiceItemHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull SettingServiceItemHolder holder, int position) {
        log.i("onBindViewHolder");

        if(isEditMode) {
            holder.editLayout.setVisibility(View.VISIBLE);
            holder.setLayout.setVisibility(View.GONE);
        } else {
            holder.editLayout.setVisibility(View.GONE);
            holder.setLayout.setVisibility(View.VISIBLE);
        }

        holder.tvItemName.setText(serviceItems.get(position));
    }

    @Override
    public int getItemCount() {
        return serviceItems.size();
    }


    public void setEditMode(boolean b) {
        isEditMode = b;
    }




    // RecyclerView.ViewHolder
    static class SettingServiceItemHolder extends RecyclerView.ViewHolder {

        TextView tvItemName;
        EditText etMileage, etMonth;
        RelativeLayout setLayout, editLayout;

        SettingServiceItemHolder(View v) {
            super(v);

            editLayout = v.findViewById(R.id.layout_mode_edit);
            setLayout = v.findViewById(R.id.layout_mode_set);
            tvItemName = v.findViewById(R.id.tv_setting_item);
            etMileage = v.findViewById(R.id.et_default_mileage);
            etMonth = v.findViewById(R.id.et_default_month);

        }

    }
}
