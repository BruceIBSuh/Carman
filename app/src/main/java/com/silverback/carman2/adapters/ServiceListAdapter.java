package com.silverback.carman2.adapters;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.viewholders.ServiceItemHolder;

import org.json.JSONArray;

import java.util.ArrayList;

public class ServiceListAdapter extends RecyclerView.Adapter<ServiceItemHolder> {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(ServiceListAdapter.class);

    // Constructor
    public ServiceListAdapter(String jsonItems) {

    }

    @NonNull
    @Override
    public ServiceItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull ServiceItemHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return 0;
    }
}
