package com.silverback.carman2.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.utils.ItemTouchHelperCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SettingServiceItemAdapter
        extends RecyclerView.Adapter<SettingServiceItemAdapter.SettingServiceItemHolder>
        implements ItemTouchHelperCallback.RecyclerItemMoveListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(SettingServiceItemAdapter.class);

    // Objects & UIs
    private Context mContext;
    private List<JSONObject> svcItemList;
    private JSONArray jsonSvcItemArray;
    private OnAdapterCallback mListener;

    // Fields
    private String mileage, month;
    private boolean isEditMode;


    // Interface
    public interface OnAdapterCallback {
        void dragItem(int from, int to);
        void removeItem(int position);
    }

    // Constructor
    public SettingServiceItemAdapter(JSONArray jsonArray, OnAdapterCallback listener) {
        mListener = listener;
        jsonSvcItemArray = jsonArray;
        svcItemList = new ArrayList<>();
        for(int i = 0; i < jsonSvcItemArray.length(); i++)
            svcItemList.add(jsonSvcItemArray.optJSONObject(i));
    }


    @NonNull
    @Override
    public SettingServiceItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.cardview_setting_service, parent, false);

        return new SettingServiceItemHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull SettingServiceItemHolder holder, int position) {
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

    // The following 2 callback methods are invoked by ItemTouchHelperCallback.RecyclerItemMoveListener
    // to drag or swipe of the RecycerView items.
    @Override
    public void onItemMove(int from, int to) {
        if (from < to) {
            for (int i = from; i < to; i++) {
                Collections.swap(svcItemList, i, i + 1);
            }
        } else {
            for (int i = from; i > to; i--) {
                Collections.swap(svcItemList, i, i - 1);
            }
        }
        notifyItemMoved(from, to);
    }

    @Override
    public void onItemRemove(int pos) {
        log.i("onItemRemove: %s", pos);
        mListener.removeItem(pos);
    }


    /**
     * RecyclerView.ViewHolder
     */
    static class SettingServiceItemHolder extends RecyclerView.ViewHolder {

        TextView tvItemName;
        EditText etMileage, etMonth;

        SettingServiceItemHolder(View v) {
            super(v);
            tvItemName = v.findViewById(R.id.tv_service_item);
            etMileage = v.findViewById(R.id.et_default_mileage);
            etMonth = v.findViewById(R.id.et_default_month);
        }

    }






}
