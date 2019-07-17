package com.silverback.carman2.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.List;

public class SettingServiceItemAdapter extends RecyclerView.Adapter<SettingServiceItemAdapter.SettingServiceItemHolder> {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(SettingServiceItemAdapter.class);


    // Objects & UIs
    private OnServiceItemClickListener mListener;
    private List<String> serviceItemsList;
    private JSONArray jsonSvcItemArray;
    public EditText etMileage, etMonth;
    private ImageButton btnUp, btnDown, btnDel;

    // Fields
    private String mileage, month;
    private boolean isEditMode;

    public interface OnServiceItemClickListener {
        void editServiceItem(int resId, int pos);
    }

    // Constructor
    public SettingServiceItemAdapter(Fragment fm, JSONArray jsonArray) {
        jsonSvcItemArray = jsonArray;
        mListener = (OnServiceItemClickListener)fm;
    }


    @NonNull
    @Override
    public SettingServiceItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.view_setting_svc_items, parent, false);

        return new SettingServiceItemHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull SettingServiceItemHolder holder, int position) {

        if(isEditMode) {
            //
            holder.editLayout.setVisibility(View.VISIBLE);
            holder.setLayout.setVisibility(View.GONE);

            holder.btnDel.setOnClickListener(v -> mListener.editServiceItem(R.id.btn_setting_del, position));
            holder.btnUp.setOnClickListener(v -> mListener.editServiceItem(R.id.btn_setting_up, position));
            holder.btnDown.setOnClickListener(v -> mListener.editServiceItem(R.id.btn_setting_down, position));

        } else {
            holder.editLayout.setVisibility(View.GONE);
            holder.setLayout.setVisibility(View.VISIBLE);
        }

        try {
            holder.tvItemName.setText(jsonSvcItemArray.getJSONObject(position).getString("name"));
            holder.etMileage.setHint(jsonSvcItemArray.getJSONObject(position).getString("mileage"));
            holder.etMonth.setHint(jsonSvcItemArray.getJSONObject(position).getString("month"));

        } catch(JSONException e) {
            log.e("JSONException: %s", e.getMessage());
        }

    }

    @Override
    public int getItemCount() {
        return jsonSvcItemArray.length();
    }

    public void setEditMode(boolean b) {
        isEditMode = b;
    }




    /**
     * RecyclerView.ViewHolder
     */
    static class SettingServiceItemHolder extends RecyclerView.ViewHolder {

        TextView tvItemName;
        EditText etMileage, etMonth;
        RelativeLayout setLayout, editLayout;
        Button btnUp, btnDown, btnDel;

        SettingServiceItemHolder(View v) {
            super(v);

            editLayout = v.findViewById(R.id.layout_mode_edit);
            setLayout = v.findViewById(R.id.layout_mode_set);
            tvItemName = v.findViewById(R.id.tv_setting_item);
            etMileage = v.findViewById(R.id.et_default_mileage);
            etMonth = v.findViewById(R.id.et_default_month);
            btnUp = v.findViewById(R.id.btn_setting_up);
            btnDown = v.findViewById(R.id.btn_setting_down);
            btnDel = v.findViewById(R.id.btn_setting_del);
        }

    }






}
