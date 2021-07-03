package com.silverback.carman.adapters;

import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.silverback.carman.R;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.ItemTouchHelperCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This RecyclerView adapter
 */
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

    private SparseArray<String> sparseItemArray;
    private int average;
    //private ViewGroup parent;


    // Interface
    public interface OnServiceItemAdapterCallback {
        void dragServiceItem(int from, int to);
        void delServiceItem(int position);
        void changeServicePeriod(int position, SparseArray<String> value);
    }

    // Constructor
    public SettingServiceItemAdapter(JSONArray jsonArray, int average, OnServiceItemAdapterCallback callback) {

        mCallback = callback;
        jsonSvcItemArray = jsonArray;
        this.average = average;
        svcItemList = new ArrayList<>();
        sparseItemArray = new SparseArray<>();

        for(int i = 0; i < jsonSvcItemArray.length(); i++)
            svcItemList.add(jsonSvcItemArray.optJSONObject(i));
    }


    @NonNull
    @Override
    public SettingServiceItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //this.parent = parent;
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.cardview_setting_service, parent, false);
        return new SettingServiceItemHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SettingServiceItemHolder holder, int position) {

        try {
            holder.tvNumber.setText(String.valueOf(position + 1));
            holder.tvItemName.setText(jsonSvcItemArray.getJSONObject(position).getString("name"));
            holder.etMileage.setHint(jsonSvcItemArray.getJSONObject(position).getString("mileage"));
            holder.etMonth.setHint(jsonSvcItemArray.getJSONObject(position).getString("month"));

            // The edittexts gains focus, call
            holder.etMileage.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) setServiceMileage(holder.etMileage, holder.etMonth, position);
            });

           holder.etMonth.setOnFocusChangeListener((v, hasFocus) -> {
               if(hasFocus) setServiceMonth(holder.etMonth, holder.etMileage, position);
           });

        } catch(JSONException e) { e.printStackTrace();}
    }

    @Override
    public void onBindViewHolder(
            @NonNull SettingServiceItemHolder holder, int position, @NonNull List<Object> payloads) {

        // Invalidte the textview for changing the number accroding to a new position as a result of
        // dragging.
        if(payloads.size() == 0 ) {
            super.onBindViewHolder(holder, position, payloads);
        // When dragging, change the postion number betweein from and to position.
        } else if(payloads.get(0) instanceof Boolean) {
            holder.tvNumber.setText(String.valueOf((position + 1)));
        }

    }

    @Override
    public int getItemCount() {
        // Kind of cheat coding to add a new item to ServiceItemList.
        svcItemList.add(jsonSvcItemArray.optJSONObject(jsonSvcItemArray.length()));
        return jsonSvcItemArray.length();
    }

    // The following 2 callback methods are invoked by ItemTouchHelperCallback.RecyclerItemMoveListener
    // to drag or swipe of the RecycerView items.
    @Override
    public void onDragItem(int from, int to) {

        if (from < to) for (int i = from; i < to; i++) Collections.swap(svcItemList, i, i + 1);
        else for (int i = from; i > to; i--) Collections.swap(svcItemList, i, i - 1);
        //notifyItemRangeChanged(to, Math.abs(from - to) + 1, true);

        mCallback.dragServiceItem(from, to);
    }

    @Override
    public void onDeleteItem(final int pos) {
        mCallback.delServiceItem(pos);
        /*
        Snackbar snackbar = Snackbar.make(parent, "Do you really remove this item?", Snackbar.LENGTH_LONG);
        snackbar.setAction("REMOVE", v -> {
            mCallback.delServiceItem(pos);
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
        */
    }

    private void setServiceMileage(EditText mileage, EditText month, int position) {
        mileage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void afterTextChanged(Editable editable) {
                int mileagePeriod = Integer.parseInt(editable.toString());
                // (mileage/average) * 12 not workng due to int casting.
                int monthPeriod = mileagePeriod * 12 / average;
                if(monthPeriod > 0) {
                    month.setText(String.valueOf(monthPeriod));
                    sparseItemArray.put(0, editable.toString());
                    sparseItemArray.put(1, month.getText().toString());
                    mCallback.changeServicePeriod(position, sparseItemArray);
                }
            }
        });
    }
    
    private void setServiceMonth(EditText month, EditText mileage, int position) {
        month.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void afterTextChanged(Editable editable) {
                log.i("mileage hint: %s", mileage.getHint().toString());
                CharSequence period = (TextUtils.isEmpty(editable)? month.getHint() : month.getText());
                sparseItemArray.put(0, mileage.getHint().toString());
                sparseItemArray.put(1, period.toString());
                mCallback.changeServicePeriod(position, sparseItemArray);
            }
        });
    }


    static class SettingServiceItemHolder extends RecyclerView.ViewHolder {

        TextView tvNumber;
        TextView tvItemName;
        EditText etMileage, etMonth;

        SettingServiceItemHolder(View v) {
            super(v);
            tvNumber = v.findViewById(R.id.tv_number);
            tvItemName = v.findViewById(R.id.tv_name);
            etMileage = v.findViewById(R.id.et_default_mileage);
            etMonth = v.findViewById(R.id.et_default_month);
        }
    }
}
