package com.silverback.carman.adapters;

import static android.media.CamcorderProfile.get;

import static org.apache.poi.sl.usermodel.PresetColor.Info;

import android.content.Context;
import android.os.Build;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.AdapterListUpdateCallback;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.silverback.carman.BaseActivity;
import com.silverback.carman.R;
import com.silverback.carman.databinding.MainRecyclerGasBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.threads.StationGasRunnable;
import com.silverback.carman.threads.StationInfoRunnable;

import java.text.DecimalFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class StationGasAdapter extends RecyclerView.Adapter<StationGasAdapter.ViewHolder> {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(StationGasAdapter.class);

    // Objects
    private Context context;
    private List<StationGasRunnable.Item> stationList;
    private final OnItemClickCallback mListener;
    private final DecimalFormat df;

    // Interface
    public interface OnItemClickCallback {
        void onItemClicked(int pos);
    }

    // Constructor
    public StationGasAdapter(OnItemClickCallback listener) {
        super();
        mListener = listener;
        df = BaseActivity.getDecimalFormatInstance();
    }

    public void setStationList(List<StationGasRunnable.Item> stationList) {
        this.stationList = stationList;
    }

    protected static class ViewHolder extends RecyclerView.ViewHolder {
        private final MainRecyclerGasBinding binding;
        public ViewHolder(View itemView) {
            super(itemView);
            binding = MainRecyclerGasBinding.bind(itemView);
        }

        ImageView getLogoImageView() { return binding.imgLogo; }
        TextView getNameView() { return binding.tvStnName; }
        TextView getPriceView() { return binding.tvValuePrice; }
        TextView getDistanceView() { return binding.tvValueDistance; }
        ImageView getCarWashView() { return binding.imgviewCarwash; }
        ImageView getCvSView() { return binding.imgviewCvs; }
        ImageView getSvcView() { return binding.imgviewSvc; }

    }

    @NonNull
    @Override
    public StationGasAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.main_recycler_gas, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StationGasAdapter.ViewHolder holder, int position) {
        final StationGasRunnable.Item data = stationList.get(position);
        String stnId = data.getStnId(); // Pass Station ID when clicking a cardview item.
        int resLogo = getGasStationImage(data.getStnCompany());
        String carwash = (data.getIsCarWash())?context.getString(R.string.general_carwash_yes):context.getString(R.string.general_carwash_no);

        holder.getLogoImageView().setImageDrawable(ContextCompat.getDrawable(context, resLogo));
        holder.getNameView().setText(data.getStnName());
        holder.getPriceView().setText(String.format("%s%2s", df.format(data.getGasPrice()), context.getString(R.string.unit_won)));
        holder.getDistanceView().setText(String.format("%s%2s", df.format(data.getStnDistance()), context.getString(R.string.unit_meter)));
        
        // Set the visibility of the facility icons.
        int washVisible = (data.getIsCarWash())? View.VISIBLE : View.GONE;
        int cvsVisible = (data.getIsCVS())? View.VISIBLE : View.GONE;
        int svcVisible = (data.getIsService())? View.VISIBLE : View.GONE;

        holder.getCarWashView().setVisibility(washVisible);
        holder.getCvSView().setVisibility(cvsVisible);
        holder.getSvcView().setVisibility(svcVisible);

        holder.itemView.setOnClickListener(view -> {
            if(mListener != null) mListener.onItemClicked(position);
        });
    }

    @Override
    public void onBindViewHolder(@NonNull StationGasAdapter.ViewHolder holder, int position,
                                 @NonNull List<Object> payloads) {
        if(payloads.isEmpty()) super.onBindViewHolder(holder, position, payloads);
        else {
            for(Object payload : payloads) {
                if(payload instanceof SparseArray){
                    Object obj = ((SparseArray<?>)payload).valueAt(position);
                    StationInfoRunnable.Info info = (StationInfoRunnable.Info)obj;
                    if(info.carWashYN.matches("Y")) holder.getCarWashView().setVisibility(View.VISIBLE);
                    if(info.maintYN.matches("Y")) holder.getSvcView().setVisibility(View.VISIBLE);
                    if(info.cvsYN.matches("Y")) holder.getCvSView().setVisibility(View.VISIBLE);
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        return stationList.size();
    }

    public List<StationGasRunnable.Item> sortStationList(boolean isPriceOrder) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (isPriceOrder) {
                Collections.sort(stationList, Comparator.comparingInt(t -> (int) t.getGasPrice()));
            } else {
                Collections.sort(stationList, Comparator.comparingInt(t -> (int) t.getStnDistance()));
            }

        } else {
            if (isPriceOrder) {
                Collections.sort(stationList, (t1, t2) ->
                        Integer.compare((int) t1.getGasPrice(), (int) t2.getGasPrice()));
            } else {
                Collections.sort(stationList, (t1, t2) ->
                        Integer.compare((int) t1.getStnDistance(), (int) t2.getStnDistance()));
            }
        }

        return stationList;
    }

    private static int getGasStationImage(String name) {
        int resId = -1;
        switch(name) {
            case "SKE": resId = R.drawable.logo_sk; break;
            case "GSC": resId = R.drawable.logo_gs; break;
            case "HDO": resId = R.drawable.logo_hyundai; break;
            case "SOL": resId = R.drawable.logo_soil; break;
            case "RTO": resId = R.drawable.logo_pb; break;
            case "RTX": resId = R.drawable.logo_express; break;
            case "NHO": resId = R.drawable.logo_nonghyup; break;
            case "E1G": resId = R.drawable.logo_e1g; break;
            case "SKG": resId = R.drawable.logo_skg; break;
            case "ETC": resId = R.drawable.logo_anonym; break;
            default: break;
        }

        return resId;
    }

}
