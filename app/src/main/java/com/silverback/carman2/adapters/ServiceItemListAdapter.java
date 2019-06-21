package com.silverback.carman2.adapters;

import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.viewholders.ServiceItemHolder;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class ServiceItemListAdapter extends RecyclerView.Adapter<ServiceItemHolder> {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(ServiceItemListAdapter.class);

    // Objects
    private OnServiceItemClickListener itemListener;
    //private SparseBooleanArray chkboxStateArray; //to retain the state of the checkbox in each item.
    private boolean[] arrCheckedState;
    private String[] arrItemCost;
    private String[] arrItemMemo;
    private ArrayList<String> serviceList;

    // UIs
    private TextView tvItemName, tvItemCost;
    private CheckBox checkBox;

    public interface OnServiceItemClickListener {
        void inputItemCost(String itemName, TextView view, int position);
    }

    // Constructor
    public ServiceItemListAdapter(String jsonItems, OnServiceItemClickListener listener) {
        super();

        itemListener = listener;
        serviceList = new ArrayList<>();

        // Covert Json string transferred from ServiceManagerFragment to JSONArray which is bound to
        // the viewholder.
        try {
            JSONArray jsonArray = new JSONArray(jsonItems);
            for(int i = 0; i < jsonArray.length(); i++) {
                serviceList.add(jsonArray.getString(i));
            }

            arrCheckedState = new boolean[serviceList.size()];
            arrItemCost = new String[serviceList.size()];
            arrItemMemo = new String[serviceList.size()];

        } catch(JSONException e) {
            log.e("JSONException: %s", e.getMessage());
        }

        //chkboxStateArray = new SparseBooleanArray();

    }

    @NonNull
    @Override
    public ServiceItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        CardView cardView = (CardView)LayoutInflater.from(parent.getContext())
                .inflate(R.layout.view_card_serviceitem, parent, false);

        tvItemName = cardView.findViewById(R.id.tv_item_name);
        //tvItemCost = cardView.findViewById(R.id.tv_value_cost);
        checkBox = cardView.findViewById(R.id.chkbox);

        return new ServiceItemHolder(cardView);
    }

    @Override
    public void onBindViewHolder(@NonNull ServiceItemHolder holder, int position) {

        holder.bindItemToHolder(serviceList.get(position), arrCheckedState[position]);

        tvItemCost = holder.itemView.findViewById(R.id.tv_value_cost);

        // itemView is given as a field in ViewHolder.
        // Attach an event handler when clicking an item of RecyclerView
        tvItemCost.setOnClickListener(v ->
                itemListener.inputItemCost(tvItemName.getText().toString(), tvItemCost, position));

        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            arrCheckedState[position] = isChecked;
            holder.doCheckBoxAction(isChecked);
            log.i("checkbox: %s", arrCheckedState[position]);

        });


    }



    // notifyItemChanged of RecyclerView.Adapter invokes this which has payloads param.
    @Override
    public void onBindViewHolder(@NonNull ServiceItemHolder holder, int pos, @NonNull List<Object> payloads){
        if(payloads.isEmpty()) {
            super.onBindViewHolder(holder, pos, payloads);

        } else {
            for(Object payload : payloads) {
                if(payload instanceof String) {
                    log.i("payloads: %s", payload);
                    TextView tvCost = holder.itemView.findViewById(R.id.tv_value_cost);
                    tvCost.setBackgroundResource(android.R.color.white);
                    tvCost.setText((String)payload);
                    arrItemCost[pos] = tvCost.getText().toString();
                    log.i("Item Cost: %s", arrItemCost[pos]);
                }
            }
        }
    }


    @Override
    public int getItemCount() {
        return serviceList.size();
    }

    public boolean[] getCheckedState() {
        return arrCheckedState;
    }




}
