package com.silverback.carman2.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Opinet;
import com.silverback.carman2.threads.StationListTask;
import com.silverback.carman2.viewholders.StationsViewHolder;

import java.util.List;

public class StationListAdapter extends RecyclerView.Adapter<StationsViewHolder> {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(StationListAdapter.class);

    // Objects
    private Context context;
    private List<Opinet.GasStnParcelable> stationList;
    private StationListTask stationInfoTask;

    // Constructor
    public StationListAdapter(List<Opinet.GasStnParcelable> data) {
        stationList = data;
    }


    @NonNull
    @Override
    public StationsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        CardView cardView = (CardView)LayoutInflater.from(context)
                .inflate(R.layout.cardview_stations, parent, false);

        return new StationsViewHolder(cardView);

    }

    @Override
    public void onBindViewHolder(@NonNull StationsViewHolder holder, int position) {
        //TextView tvStationName = holder.cardView.findViewById(R.id.tv_station_name);
        //tvStationName.setText(stationList.get(position).getStnName());
        String stationCode = stationList.get(position).getStnId();
        //stationInfoTask = ThreadManager.startStationInfoTask(context, stationCode);
        holder.bindToStation(stationList.get(position));
    }

    @Override
    public int getItemCount() {
        return stationList.size();
    }

}
