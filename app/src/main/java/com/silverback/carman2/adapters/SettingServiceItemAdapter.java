package com.silverback.carman2.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.silverback.carman2.R;
import com.silverback.carman2.database.FavoriteProviderEntity;
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
    //private OnAdapterCallback mListener;

    private ViewGroup parent;

    // Fields
    private String mileage, month;
    private boolean isEditMode;


    // Interface
    public interface OnAdapterCallback {
        void dragItem(int from, int to);
        void removeItem(int position);
    }

    // Constructor
    public SettingServiceItemAdapter(JSONArray jsonArray) {
        //mListener = listener;
        jsonSvcItemArray = jsonArray;
        svcItemList = new ArrayList<>();
        for(int i = 0; i < jsonSvcItemArray.length(); i++)
            svcItemList.add(jsonSvcItemArray.optJSONObject(i));
    }


    @NonNull
    @Override
    public SettingServiceItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.parent = parent;
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.cardview_setting_service, parent, false);

        return new SettingServiceItemHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull SettingServiceItemHolder holder, int position) {
        try {
            holder.tvNumber.setText(String.valueOf(position + 1));
            holder.tvItemName.setText(jsonSvcItemArray.getJSONObject(position).getString("name"));
            holder.etMileage.setHint(jsonSvcItemArray.getJSONObject(position).getString("mileage"));
            holder.etMonth.setHint(jsonSvcItemArray.getJSONObject(position).getString("month"));

        } catch(JSONException e) {
            log.e("JSONException: %s", e.getMessage());
        }
    }

    @Override
    public void onBindViewHolder(
            @NonNull SettingServiceItemHolder holder, int position, @NonNull List<Object> payloads) {

        if(payloads.isEmpty()) super.onBindViewHolder(holder, position, payloads);
        else holder.tvNumber.setText(String.valueOf(position + 1));

    }

    @Override
    public int getItemCount() {
        return jsonSvcItemArray.length();
    }

    // The following 2 callback methods are invoked by ItemTouchHelperCallback.RecyclerItemMoveListener
    // to drag or swipe of the RecycerView items.
    @Override
    public void onDragItem(int from, int to) {
        if (from < to) for (int i = from; i < to; i++) Collections.swap(svcItemList, i, i + 1);
        else for (int i = from; i > to; i--) Collections.swap(svcItemList, i, i - 1);

        notifyItemMoved(from, to);
        notifyItemChanged(to, Math.abs(to - from));
    }

    @Override
    public void onDeleteItem(int pos) {
        log.i("onDeleteItem: %s", pos);
        final JSONObject deletedObj = svcItemList.get(pos);
        svcItemList.remove(pos);
        notifyItemRemoved(pos);

        Snackbar snackbar = Snackbar.make(parent, "Do you really remove this item?", Snackbar.LENGTH_INDEFINITE);
        snackbar.setAction("UNDO", v -> {
            svcItemList.add(pos, deletedObj);
            notifyItemInserted(pos);
            snackbar.dismiss();
        });

        snackbar.show();
        //mListener.removeItem(pos);
    }


    /**
     * RecyclerView.ViewHolder
     */
    static class SettingServiceItemHolder extends RecyclerView.ViewHolder {

        TextView tvNumber;
        TextView tvItemName;
        EditText etMileage, etMonth;

        SettingServiceItemHolder(View v) {
            super(v);
            tvNumber = v.findViewById(R.id.tv_number);
            tvItemName = v.findViewById(R.id.tv_service_item);
            etMileage = v.findViewById(R.id.et_default_mileage);
            etMonth = v.findViewById(R.id.et_default_month);
        }

    }






}
