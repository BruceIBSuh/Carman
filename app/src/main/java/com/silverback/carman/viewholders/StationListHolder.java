package com.silverback.carman.viewholders;

import android.content.Context;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.silverback.carman.BaseActivity;
import com.silverback.carman.R;
import com.silverback.carman.databinding.CardviewGasStationsBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.viewmodels.Opinet;

import java.text.DecimalFormat;

public class StationListHolder extends RecyclerView.ViewHolder {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(StationListHolder.class);

    // Objects
    private final CardviewGasStationsBinding binding;
    private final Context context;
    private static final DecimalFormat df;
    //private ImageView imgLogo;
    //private TextView tvName, tvPrice, tvDistance, tvWashLabel;
    //private String price, distance, carwash;
    //private String stnId;
    public TextView tvWashValue;

    static {
        df = BaseActivity.getDecimalFormatInstance();
    }

    // Constructor
    public StationListHolder(Context context, CardviewGasStationsBinding binding) {
        super(binding.getRoot());
        this.binding = binding;
        this.context = context;
    }

    public void bindToStationList(Opinet.GasStnParcelable data) {
        String stnId = data.getStnId(); // Pass Station ID when clicking a cardview item.
        int resLogo = getGasStationImage(data.getStnCode());

        binding.imgLogo.setImageResource(resLogo);
        binding.cardviewGasStnName.setText(data.getStnName());
        binding.tvValuePrice.setText(String.format(
                "%s%2s", df.format(data.getStnPrice()), context.getString(R.string.unit_won)));
        binding.tvValueDistance.setText(String.format(
                "%s%4s", df.format(data.getStnDistance()), context.getString(R.string.unit_meter)));

        String strCarwash = (data.getIsWash())? context.getString(R.string.general_carwash_yes):
                context.getString(R.string.general_carwash_no);
        binding.tvValueCarwash.setText(strCarwash);
    }

    public TextView getCarWashView() {
        return binding.tvValueCarwash;
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
