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
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.google.type.Color;
import com.silverback.carman2.R;
import com.silverback.carman2.fragments.InputPadFragment;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.viewholders.ServiceItemHolder;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import static androidx.annotation.InspectableProperty.ValueType.COLOR;

public class ServiceItemListAdapter extends RecyclerView.Adapter<ServiceItemHolder> implements CompoundButton.OnCheckedChangeListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(ServiceItemListAdapter.class);

    // Objects
    private OnServiceItemClickListener itemListener;
    private SparseBooleanArray chkboxStateArray;
    private ArrayList<String> serviceList;

    // UIs
    private TextView tvItemName, tvItemCost;
    private CheckBox checkBox;

    // Fields
    private int itemPosition;

    public interface OnServiceItemClickListener {
        void inputItemCost(String itemName, TextView view, int position);
    }

    // Constructor
    public ServiceItemListAdapter(String jsonItems, OnServiceItemClickListener listener) {
        super();

        itemListener = listener;
        chkboxStateArray = new SparseBooleanArray();
        serviceList = new ArrayList<>();

        // Covert Json string transferred from ServiceManagerFragment to JSONArray which is bound to
        // the viewholder.
        try {
            JSONArray jsonArray = new JSONArray(jsonItems);
            for(int i = 0; i < jsonArray.length(); i++) {
                serviceList.add(jsonArray.getString(i));
            }
            //serviceList.add("test");
            //serviceList.add("test2");
        } catch(JSONException e) {
            log.e("JSONException: %s", e.getMessage());
        }

    }

    @NonNull
    @Override
    public ServiceItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        CardView cardView = (CardView)LayoutInflater.from(parent.getContext())
                .inflate(R.layout.view_card_serviceitem, parent, false);

        tvItemName = cardView.findViewById(R.id.tv_item_name);
        tvItemCost = cardView.findViewById(R.id.tv_value_cost);
        checkBox = cardView.findViewById(R.id.chkbox);


        return new ServiceItemHolder(cardView);
    }

    @Override
    public void onBindViewHolder(@NonNull ServiceItemHolder holder, int position) {

        itemPosition = position;
        holder.bindItemToHolder(serviceList.get(position));

        // itemView is given as a field in ViewHolder.
        // Attach an event handler when clicking an item of RecyclerView
        holder.itemView.findViewById(R.id.tv_value_cost).setOnClickListener(v ->
            itemListener.inputItemCost(tvItemName.getText().toString(), tvItemCost, position));
    }

    // Invoked by RecyclerView.Adapter<VH>.notifyItemChanged() with payloads
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
                }
            }
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

    }

    @Override
    public int getItemCount() {
        return serviceList.size();
    }

}
