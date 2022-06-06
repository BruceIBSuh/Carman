package com.silverback.carman.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.silverback.carman.R;
import com.silverback.carman.databinding.MainRecyclerviewEvBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.threads.EvStationListRunnable;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class EvStationListAdapter extends RecyclerView.Adapter<EvStationListAdapter.ViewHolder> {
    private static final LoggingHelper log = LoggingHelperFactory.create(EvStationListAdapter.class);

    private final List<EvStationListRunnable.EvStationInfo> evList;
    private final DecimalFormat df;
    private Context context;

    public EvStationListAdapter(List<EvStationListRunnable.EvStationInfo> evList) {
        this.evList = evList;
        df = (DecimalFormat) NumberFormat.getInstance(Locale.getDefault());
        df.applyPattern("#,###");
        df.setDecimalSeparatorAlwaysShown(false);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final MainRecyclerviewEvBinding binding;
        public ViewHolder(View itemView) {
            super(itemView);
            binding = MainRecyclerviewEvBinding.bind(itemView);
        }

        TextView getEvStationName() {
            return binding.tvEvName;
        }
        TextView getDistanceView() { return binding.tvDistance;}
        TextView getChargerIdView() { return binding.tvChgrId; }
        TextView getAccessView() { return binding.tvChgrLimited; }
        TextView getLimitDetailView() { return binding.tvLimitDetail; }
        TextView getChargerTypeView() { return binding.tvChgrType;}

    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.main_recyclerview_ev, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EvStationListRunnable.EvStationInfo info = evList.get(position);
        String limitDetail = (info.getLimitDetail() == null)?
                context.getString(R.string.main_ev_no_limit) : info.getLimitDetail();

        holder.getEvStationName().setText(info.getEvName());
        holder.getChargerIdView().setText(info.getChargerId());
        holder.getAccessView().setText(info.getIsPublic());
        holder.getLimitDetailView().setText(limitDetail);

        log.i("Distance: %s", info.getDistance());
        holder.getDistanceView().setText(df.format(info.getDistance()));
        holder.getChargerTypeView().setText(info.getChargerType());
    }

    @Override
    public int getItemCount() {
        return evList.size();
    }



}
