package com.silverback.carman2.adapters;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.silverback.carman2.BaseActivity;
import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Constants;
import com.silverback.carman2.models.Opinet;
import com.silverback.carman2.viewholders.StationListHolder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

public class StationListAdapter extends RecyclerView.Adapter<StationListHolder> {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(StationListAdapter.class);

    // Objects
    private Context context;
    private DecimalFormat df;
    private List<Opinet.GasStnParcelable> stationList;
    private OnRecyclerItemClickListener mListener;


    // Interface
    public interface OnRecyclerItemClickListener {
        void onItemClicked(int pos);
    }

    // Constructor
    public StationListAdapter(List<Opinet.GasStnParcelable> list, OnRecyclerItemClickListener listener) {
        super();
        df = BaseActivity.getDecimalFormatInstance();
        stationList = list;
        mListener = listener;
    }


    @NonNull
    @Override
    public StationListHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        CardView cardView = (CardView)LayoutInflater.from(parent.getContext())
                .inflate(R.layout.cardview_stationlist, parent, false);

        return new StationListHolder(cardView);

    }

    @Override
    public void onBindViewHolder(@NonNull StationListHolder holder, int position) {

        final Opinet.GasStnParcelable data = stationList.get(position);
        holder.bindToStationList(data);

        /*
        holder.stnId = data.getStnId(); // Pass Station ID when clicking a cardview item.
        holder.stnName = data.getStnName();
        int resLogo = getGasStationImage(data.getStnCode());
        holder.imgLogo.setImageResource(resLogo);

        // TEST CODING FOR CHECKING IF A STATION HAS BEEN VISITED!!
        holder.tvName.setText(data.getStnName());
        holder.tvPrice.setText(String.format("%s%2s", df.format(data.getStnPrice()), context.getString(R.string.unit_won)));
        holder.tvDistance.setText(String.format("%s%4s", df.format(data.getStnDistance()), context.getString(R.string.unit_meter)));
        holder.tvWashValue.setText(String.valueOf(data.getIsWash()));
        */
        holder.itemView.setOnClickListener(view -> {
            log.i("cardview position: %s, %s", position, mListener);
            if(mListener != null) mListener.onItemClicked(position);
        });

    }


    @Override
    public void onBindViewHolder(@NonNull StationListHolder holder, int position, @NonNull List<Object> payloads) {
        if(payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads);

        }else{
            log.i("OnItemChagned: %s", payloads.get(0));
            //holder.tvWashValue.setText(payloads.get(0).toString());

            for(Object obj : payloads) {
                if(obj instanceof Boolean) {
                    log.i("isCarwash in StationListAdapter: %s, %s", position, obj);
                    String isWash = obj.toString();
                    holder.tvWashValue.setText(isWash);
                } else if(obj == null) {
                    holder.tvWashValue.setText("N/A");
                }
            }

        }
    }

    @Override
    public int getItemCount() {
        return stationList.size();
    }
    /*
     * Sorts the already saved station list  from the Opinet by price and distance
     * @param uri :  file saved in the cache location
     * @param sort : true - price order, false - distance order
     */
    @SuppressWarnings("unchecked")
    public void sortStationList(boolean sort) {

        File fStationList = new File(context.getCacheDir(), Constants.FILE_CACHED_NEAR_STATIONS);
        Uri uri = Uri.fromFile(fStationList);

        try(InputStream is = context.getContentResolver().openInputStream(uri);
            ObjectInputStream ois = new ObjectInputStream(is)) {

            stationList = (List<Opinet.GasStnParcelable>)ois.readObject();

            //if(sort) Collections.sort(stationList, new PriceAscCompare()); // Price Ascending order
            //else Collections.sort(stationList, new DistanceDescCompare()); // Distance Ascending order
            notifyDataSetChanged();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }


        //stationList = (List<Opinet.GasStnParcelable>)ois.readObject();

        if(stationList.size() <= 0) return;

        if(sort) Collections.sort(stationList, new PriceAscCompare()); // Price Ascending order
        else Collections.sort(stationList, new DistanceDescCompare()); // Distance Ascending order
    }



    // Class for sorting the list by ascending price or descending distance, implementing Comparator<T>
    private class PriceAscCompare implements Comparator<Opinet.GasStnParcelable> {
        @SuppressWarnings("all")
        @Override
        public int compare(Opinet.GasStnParcelable t1, Opinet.GasStnParcelable t2) {
            //Log.d(TAG, "getStnPrice: " + t1.getStnPrice() + ", " + t2.getStnPrice());
            //return Integer.compare((int)t1.getStnPrice(), (int)t2.getStnPrice()) //API 19 or higher

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                return Integer.compare((int)t1.getStnPrice(), (int)t2.getStnPrice());
            } else {
                return (int) t1.getStnPrice() < (int) t2.getStnPrice() ? -1 :
                        (int) t1.getStnPrice() > (int) t2.getStnPrice() ? 1 : 0;
            }

        }
    }

    private class DistanceDescCompare implements Comparator<Opinet.GasStnParcelable> {
        @SuppressWarnings("all")
        @Override
        public int compare(Opinet.GasStnParcelable t1, Opinet.GasStnParcelable t2) {
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
