package com.silverback.carman2.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

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

public class ServiceListAdapter extends RecyclerView.Adapter<ServiceItemHolder> {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(ServiceListAdapter.class);

    // Objects
    private CardView cardView;
    private ArrayList<String> serviceList;

    // Constructor
    public ServiceListAdapter(String jsonItems) {
        super();

        serviceList = new ArrayList<>();
        // Covert Json string transferred from ServiceFragment to JSONArray which is bound to
        // the viewholder.
        try {
            JSONArray jsonArray = new JSONArray(jsonItems);
            for(int i = 0; i < jsonArray.length(); i++) {
                serviceList.add(jsonArray.getString(i));
            }

            serviceList.add("test");
            serviceList.add("test2");
        } catch(JSONException e) {
            log.e("JSONException: %s", e.getMessage());
        }

    }

    @NonNull
    @Override
    public ServiceItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        cardView = (CardView)LayoutInflater.from(parent.getContext())
                .inflate(R.layout.view_card_serviceitem, parent, false);

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
}
