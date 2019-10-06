package com.silverback.carman2.adapters;

import android.content.Context;
import android.text.TextUtils;
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
    private OnServiceItemAdapterCallback mCallback;
    //private OnAdapterCallback mListener;

    private ViewGroup parent;


    // Interface
    public interface OnServiceItemAdapterCallback {
        void dragServiceItem(int from, int to);
        void delServiceItem(int position);
    }

    // Constructor
    public SettingServiceItemAdapter(JSONArray jsonArray, OnServiceItemAdapterCallback callback) {

        jsonSvcItemArray = jsonArray;
        mCallback = callback;
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

        log.i("Partial Binding:%s", payloads.size());
        // Invalidte the textview for changing the number accroding to a new position as a result of
        // dragging.
        if(payloads.size() == 0) {
            super.onBindViewHolder(holder, position, payloads);

        // When dragging, change the postion number betweein from and to position.
        } else if(payloads.get(0) instanceof Boolean) {
            log.i("partial binding - dragging: %s", position);
            holder.tvNumber.setText(String.valueOf((position + 1)));
        // When adding a new item, add the item to the list.
        } else if(payloads.get(0) instanceof JSONObject) {
            log.i("partial binding - adding serviceItem: %s, %s", position, payloads.get(0));
            svcItemList.add((JSONObject)payloads.get(0));
        }

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

        // Partial Binding for changing the number ahead of each item when dragging the item.
        if(from < to) notifyItemRangeChanged(from, Math.abs(from - to) + 1, true);
        else notifyItemRangeChanged(to, Math.abs(from - to) + 1, true);

        //mCallback.dragServiceItem(from, to);
    }

    @Override
    public void onDeleteItem(final int pos) {
        Snackbar snackbar = Snackbar.make(parent, "Do you really remove this item?", Snackbar.LENGTH_LONG);
        snackbar.setAction("REMOVE", v -> {
            mCallback.delServiceItem(pos);
            /*
            svcItemList.remove(pos);
            notifyItemRemoved(pos);
            notifyItemRangeChanged(pos, svcItemList.size() - pos, true);
            */
            snackbar.dismiss();

        }).addCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar snackkbar, int event) {
                if(event == Snackbar.Callback.DISMISS_EVENT_TIMEOUT) {
                    notifyItemChanged(pos);
                }
            }
        });

        snackbar.show();

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
