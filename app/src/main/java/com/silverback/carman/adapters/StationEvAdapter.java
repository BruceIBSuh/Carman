package com.silverback.carman.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.silverback.carman.R;
import com.silverback.carman.databinding.MainRecyclerviewEvBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.rest.EvRetrofitTikXml;
import com.silverback.carman.threads.StationEvRunnable;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class StationEvAdapter extends RecyclerView.Adapter<StationEvAdapter.ViewHolder> {
    private static final LoggingHelper log = LoggingHelperFactory.create(StationEvAdapter.class);

    private final List<StationEvRunnable.Item> evList;
    private final DecimalFormat df;
    private Context context;

    public StationEvAdapter(List<StationEvRunnable.Item> evList) {
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

        ImageView getChgrStatusView() { return binding.ivChgrStatus; }
        TextView getEvStationName() {
            return binding.tvEvName;
        }
        TextView getDistanceView() { return binding.tvDistance;}
        TextView getChargerIdView() { return binding.tvChgrId; }
        TextView getBizNameView() { return binding.tvBizName; }
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
        if(evList.size() == 0) return;

        StationEvRunnable.Item info = evList.get(position);
        String limitDetail = (TextUtils.isEmpty(info.getLimitDetail()))?
                context.getString(R.string.main_ev_no_limit) : info.getLimitDetail();
        String charId = "_" + Integer.parseInt(info.getChgerId());

        holder.getChgrStatusView().setImageDrawable(getStatusImage(info.getStat()));
        holder.getEvStationName().setText(info.getStdNm().replaceAll("\\d*\\([\\w\\s]*\\)", ""));
        //holder.getChargerIdView().setText(charId);
        //holder.getChargerStatus().setText(String.valueOf(info.getStat()));
        //holder.getBizNameView().setText(info.getBusiNm());
        holder.getBizNameView().setText(String.valueOf(info.getCntCharger()));
        holder.getLimitDetailView().setText(limitDetail);

        holder.getDistanceView().setText(df.format(info.getDistance()));
        holder.getChargerTypeView().setText(info.getChgerType());
    }

    @Override
    public int getItemCount() {
        return evList.size();
    }

    private Drawable getStatusImage(int status) {

        switch(status) {
            case 1: return ContextCompat.getDrawable(context, android.R.drawable.presence_away);
            case 2: return ContextCompat.getDrawable(context, android.R.drawable.presence_online);
            case 3: return ContextCompat.getDrawable(context, android.R.drawable.presence_busy);
            case 4: case 9: return ContextCompat.getDrawable(context, R.drawable.bg_circle_gray);
            default: return null;
        }
    }

}
