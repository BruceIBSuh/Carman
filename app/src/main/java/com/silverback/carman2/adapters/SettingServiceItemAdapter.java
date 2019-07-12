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

public class SettingServiceItemAdapter extends RecyclerView.Adapter<SettingServiceItemAdapter.SettingChklistHolder> {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(SettingServiceItemAdapter.class);

    // Objects & UIs
    private String[] serviceItems;
    private EditText etMileage, etMonth;
    private ImageButton btnUp, btnDown, btnDel;

    // Fields
    private boolean isEditMode;

    public SettingServiceItemAdapter(String[] items) {
        log.i("SettingServiceItemAdapter");
        serviceItems = items;
    }



    @NonNull
    @Override
    public SettingChklistHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        log.i("onCreateViewHolder");
        /*
        CardView cardView = (CardView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.cardview_setting_chklist, parent, false);
        */

        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.view_setting_svc_items, parent, false);


        return new SettingChklistHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull SettingChklistHolder holder, int position) {
        log.i("onBindViewHolder");

        if(isEditMode) {
            holder.editLayout.setVisibility(View.VISIBLE);
            holder.setLayout.setVisibility(View.GONE);
        } else {
            holder.editLayout.setVisibility(View.GONE);
            holder.setLayout.setVisibility(View.VISIBLE);
        }

        holder.tvItemName.setText(serviceItems[position]);
    }

    @Override
    public int getItemCount() {
        return serviceItems.length;
    }


    public void setEditMode(boolean b) {
        isEditMode = b;
    }




    // RecyclerView.ViewHolder
    static class SettingChklistHolder extends RecyclerView.ViewHolder {

        TextView tvItemName;
        RelativeLayout setLayout, editLayout;

        SettingChklistHolder(View v) {
            super(v);

            editLayout = v.findViewById(R.id.layout_mode_edit);
            setLayout = v.findViewById(R.id.layout_mode_set);
            tvItemName = v.findViewById(R.id.tv_setting_item);

        }

    }
}
