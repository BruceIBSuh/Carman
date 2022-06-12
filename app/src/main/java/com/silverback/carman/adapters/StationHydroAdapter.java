package com.silverback.carman.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.silverback.carman.R;
import com.silverback.carman.databinding.MainRecyclerHydroBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.threads.StationHydroRunnable;

import java.text.DecimalFormat;
import java.util.List;

public class StationHydroAdapter extends RecyclerView.Adapter<StationHydroAdapter.ViewHolder> {

    private static final LoggingHelper log = LoggingHelperFactory.create(StationHydroAdapter.class);

    private Context context;
    private final List<StationHydroRunnable.HydroStationObj> hydroList;
    private final DecimalFormat df;
    private String distUnit;

    public StationHydroAdapter(List<StationHydroRunnable.HydroStationObj>  hydroList) {
        this.hydroList = hydroList;
        this.df = (DecimalFormat) DecimalFormat.getInstance();
    }

    protected static class ViewHolder extends RecyclerView.ViewHolder {
        private final MainRecyclerHydroBinding binding;
        public ViewHolder(View itemView) {
            super(itemView);
            binding = MainRecyclerHydroBinding.bind(itemView);
        }

        TextView getHydroNameView() { return binding.tvHydroName; }
        TextView getHydroAddrsView() { return binding.tvHydroAddrs; }
        TextView getHydroBizhourView() { return binding.tvHydroBizhour; }
        TextView getHydroPriceView() { return binding.tvHydroPrice; }
        TextView getHydroChgrNumView() { return binding.tvHydroChgrnum; }
        TextView getHydroPhone() { return binding.tvHydroDistance; }

    }

    @NonNull
    @Override
    public StationHydroAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        distUnit = context.getString(R.string.unit_km);
        View view = LayoutInflater.from(context).inflate(R.layout.main_recycler_hydro, parent, false);
        return new StationHydroAdapter.ViewHolder(view);

    }

    @Override
    public void onBindViewHolder(@NonNull StationHydroAdapter.ViewHolder holder, int position) {
        StationHydroRunnable.HydroStationObj obj = hydroList.get(position);
        String addrs = obj.getAddrs();
        if(TextUtils.isEmpty(addrs)) addrs = context.getString(R.string.main_hydro_close);

        holder.getHydroNameView().setText(obj.getName());
        holder.getHydroAddrsView().setText(addrs);
        holder.getHydroPriceView().setText(obj.getPrice());
        holder.getHydroChgrNumView().setText(String.valueOf(obj.getCharger()));
        holder.getHydroBizhourView().setText(obj.getBizhour());
        holder.getHydroPhone().setText(df.format(obj.getDistance()/1000.0).concat(distUnit));
    }

    @Override
    public int getItemCount() {
        return hydroList.size();
    }
}
