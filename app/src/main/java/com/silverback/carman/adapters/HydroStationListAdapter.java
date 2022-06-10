package com.silverback.carman.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.silverback.carman.R;
import com.silverback.carman.databinding.MainRecyclerHydroBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.threads.HydroStationListRunnable;
import com.silverback.carman.utils.ExcelToJsonUtil;

import java.util.List;

public class HydroStationListAdapter extends RecyclerView.Adapter<HydroStationListAdapter.ViewHolder> {

    private static final LoggingHelper log = LoggingHelperFactory.create(HydroStationListAdapter.class);

    private Context context;
    private final List<HydroStationListRunnable.HydroStationObj> hydroList;

    public HydroStationListAdapter(List<HydroStationListRunnable.HydroStationObj>  hydroList) {
        this.hydroList = hydroList;
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
        TextView getHydroPhone() { return binding.tvHydroPhone; }

    }

    @NonNull
    @Override
    public HydroStationListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.main_recycler_hydro, parent, false);
        return new HydroStationListAdapter.ViewHolder(view);

    }

    @Override
    public void onBindViewHolder(@NonNull HydroStationListAdapter.ViewHolder holder, int position) {
        HydroStationListRunnable.HydroStationObj obj = hydroList.get(position);
        String addrs = obj.getAddrs();
        if(TextUtils.isEmpty(addrs)) addrs = context.getString(R.string.main_hydro_close);

        holder.getHydroNameView().setText(obj.getName());
        holder.getHydroAddrsView().setText(addrs);
        holder.getHydroPriceView().setText(obj.getPrice());
        holder.getHydroChgrNumView().setText(String.valueOf(obj.getCharger()));
        holder.getHydroBizhourView().setText(obj.getBizhour());
        holder.getHydroPhone().setText(String.valueOf(obj.getDistance()));

        log.i("Distance: %s", obj.getDistance());
    }

    @Override
    public int getItemCount() {
        return hydroList.size();
    }
}
