package com.silverback.carman.adapters;

import android.content.Context;
import android.os.Build;
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

import com.google.common.collect.Lists;
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
    //private List<Opinet.GasStnParcelable> stationList;
    private List<StationGasRunnable.Item> gasStationList;
    private final AsyncListDiffer<StationGasRunnable.Item> mDiffer;
    private AdapterListUpdateCallback updateCallback;
    private final OnItemClickCallback mListener;
    private final DecimalFormat df;

    // Interface
    public interface OnItemClickCallback {
        void onItemClicked(int pos);
    }

    // Constructor
    //public StationGasAdapter(List<Opinet.GasStnParcelable> list, OnRecyclerItemClickListener listener) {
    public StationGasAdapter(OnItemClickCallback listener) {
        super();
        mListener = listener;
        mDiffer = new AsyncListDiffer<>(this, DIFF_CALLBACK_GAS);
        df = BaseActivity.getDecimalFormatInstance();
        updateCallback = new AdapterListUpdateCallback(this);
    }

    public void submitGasList(List<StationGasRunnable.Item> gasStationList) {
        this.gasStationList = gasStationList;
        //mDiffer.submitList(Lists.newArrayList(gasStationList)/*, postingAdapterCallback::onSubmitPostingListDone*/);
        mDiffer.submitList(Lists.newArrayList(gasStationList), () -> updateCallback.onChanged(0, 2, ""));
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
        //final StationGasRunnable.Item data = stationList.get(position);
        StationGasRunnable.Item data = mDiffer.getCurrentList().get(position);
        String stnId = data.getStnId(); // Pass Station ID when clicking a cardview item.
        int resLogo = getGasStationImage(data.getStnCompany());
        String carwash = (data.getIsCarWash())?context.getString(R.string.general_carwash_yes):context.getString(R.string.general_carwash_no);
        log.i("car wash: %s", carwash);

        holder.getLogoImageView().setImageDrawable(ContextCompat.getDrawable(context, resLogo));
        holder.getNameView().setText(data.getStnName());
        holder.getPriceView().setText(String.format("%s%s", df.format(data.getGasPrice()), context.getString(R.string.unit_won)));
        holder.getDistanceView().setText(String.format("%s%s", df.format(data.getStnDistance()), context.getString(R.string.unit_meter)));
        
        // Set the visibility of the facility icons.
        if(data.getIsCarWash()) holder.getCarWashView().setVisibility(View.VISIBLE);
        if(data.getIsCVS()) holder.getCvSView().setVisibility(View.VISIBLE);
        if(data.getIsService()) holder.getSvcView().setVisibility(View.VISIBLE);

        holder.itemView.setOnClickListener(view -> {
            if(mListener != null) mListener.onItemClicked(position);
        });
    }




    @Override
    public void onBindViewHolder(@NonNull StationGasAdapter.ViewHolder holder, int position,
                                 @NonNull List<Object> payloads) {

        if(payloads.isEmpty()) super.onBindViewHolder(holder, position, payloads);
        else {
            // On receiving car wash values, set the progressbar to be View.GONE and set the message
            // to the textview.
            for(Object obj : payloads) {
                if(obj instanceof StationInfoRunnable.Info) {
                    log.i("partial binding");
                }
                //String msg = ((boolean)obj)? context.getString(R.string.general_carwash_yes): context.getString(R.string.general_carwash_no);
                // Update the car wash value.
                //holder.getCarWashView().setText(msg);
            }
        }
    }

    @Override
    public int getItemCount() {
        //return stationList.size();
        return mDiffer.getCurrentList().size();
    }
    /*
     * Sorts the already saved station list  from the Opinet by price and distance
     * @param uri :  file saved in the cache location
     * @param sort : true - price order, false - distance order
     */

    //@SuppressWarnings("unchecked")
    //public List<StationGasRunnable.Item> sortStationList(boolean bStationOrder) {
    public List<StationGasRunnable.Item> sortStationList(boolean isPriceOrder) {
        log.i("Listing order: distance or price");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            if(isPriceOrder) {
                Collections.sort(gasStationList, Comparator.comparingInt(t -> (int) t.getGasPrice()));
            } else {
                Collections.sort(gasStationList, Comparator.comparingInt(t -> (int) t.getStnDistance()));
            }

        else {
            if(isPriceOrder) {
                Collections.sort(gasStationList, (t1, t2) ->
                        Integer.compare((int) t1.getGasPrice(), (int) t2.getGasPrice()));
            } else {
                Collections.sort(gasStationList, (t1, t2) ->
                        Integer.compare((int)t2.getStnDistance(), (int)t1.getStnDistance()));
            }
        }

        return gasStationList;
        /*
        File file = new File(context.getCacheDir(), Constants.FILE_CACHED_NEAR_STATIONS);
        Uri uri = Uri.fromFile(file);
        stationList.clear();

        try(InputStream is = context.getContentResolver().openInputStream(uri);
            ObjectInputStream ois = new ObjectInputStream(is)) {
            List<?> listObj = (List<?>)ois.readObject();

            for(Object obj : listObj) stationList.add((StationGasRunnable.Item)obj);
            //stationList = (List<Opinet.GasStnParcelable>)ois.readObject();

            if(bStationOrder) Collections.sort(stationList, new PriceAscCompare()); // Price Ascending order
            else Collections.sort(stationList, new DistanceDescCompare()); // Distance Ascending order

            //notifyDataSetChanged();
            notifyItemRangeChanged(0, stationList.size());

            return stationList;

        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            log.e("Error occurred while sorting: %s", e.getMessage());
        }

        return null;
         */


    }

    private static final DiffUtil.ItemCallback<StationGasRunnable.Item> DIFF_CALLBACK_GAS =
            new DiffUtil.ItemCallback<StationGasRunnable.Item>() {
                @Override
                public boolean areItemsTheSame(@NonNull StationGasRunnable.Item oldItem,
                                               @NonNull StationGasRunnable.Item newItem) {
                    return Objects.equals(oldItem.getStnId(), newItem.getStnId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull StationGasRunnable.Item oldItem,
                                                  @NonNull StationGasRunnable.Item newItem) {

                    return Objects.equals(oldItem.getStnName(), newItem.getStnName());

                }

                public Object getChangePayload(@NonNull StationGasRunnable.Item oldItem,
                                               @NonNull StationGasRunnable.Item newItem) {
                    return super.getChangePayload(oldItem, newItem);
                }
            };

    // Class for sorting the list by ascending price or descending distance, implementing Comparator<T>
    private static class PriceAscCompare implements Comparator<StationGasRunnable.Item> {
        @SuppressWarnings("all")
        @Override
        public int compare(StationGasRunnable.Item t1, StationGasRunnable.Item t2) {
            //Log.d(TAG, "getStnPrice: " + t1.getStnPrice() + ", " + t2.getStnPrice());
            //return Integer.compare((int)t1.getStnPrice(), (int)t2.getStnPrice()) //API 19 or higher
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                return Integer.compare((int)t1.getGasPrice(), (int)t2.getGasPrice());
            } else {
                return (int) t1.getGasPrice() < (int) t2.getGasPrice() ? -1 :
                        (int) t1.getGasPrice() > (int) t2.getGasPrice() ? 1 : 0;
            }

        }
    }

    private static class DistanceDescCompare implements Comparator<StationGasRunnable.Item> {
        @SuppressWarnings("all")
        @Override
        public int compare(StationGasRunnable.Item t1, StationGasRunnable.Item t2) {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                return Integer.compare((int)t1.getStnDistance(), (int)t2.getStnDistance());
            } else {
                return (int) t1.getStnDistance() < (int) t2.getStnDistance() ? -1 :
                        (int) t1.getStnDistance() > (int) t2.getStnDistance() ? 1 : 0;
            }
        }
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
