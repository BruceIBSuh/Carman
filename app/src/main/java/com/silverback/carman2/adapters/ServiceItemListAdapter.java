package com.silverback.carman2.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.silverback.carman2.R;
import com.silverback.carman2.fragments.InputPadFragment;
import com.silverback.carman2.fragments.ServiceManagerFragment;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.viewholders.ServiceItemHolder;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

public class ServiceItemListAdapter extends RecyclerView.Adapter<ServiceItemHolder> {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(ServiceItemListAdapter.class);

    // Objects
    private ServiceItemListener itemListener;
    private InputPadFragment numPad;
    private CardView cardView;
    private ArrayList<String> serviceList;

    //UIs
    private TextView tvItemName, tvItemCost;

    public interface ServiceItemListener {
        void inputItemCost(String itemName, View view);
    }

    // Constructor
    public ServiceItemListAdapter(String jsonItems) {
        super();

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

        cardView = (CardView)LayoutInflater.from(parent.getContext())
                .inflate(R.layout.view_card_serviceitem, parent, false);

        tvItemName = cardView.findViewById(R.id.tv_item_name);
        tvItemCost = cardView.findViewById(R.id.tv_value_cost);


        return new ServiceItemHolder(cardView);
    }

    @Override
    public void onBindViewHolder(@NonNull ServiceItemHolder holder, int position) {

        holder.bindItemToHolder(serviceList.get(position));

    }

    @Override
    public int getItemCount() {
        log.i("Item Count: %s", serviceList.size());
        return serviceList.size();
    }

    public void setServiceItemListener(ServiceItemListener listener) {
        itemListener = listener;
    }
}
