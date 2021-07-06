package com.silverback.carman.adapters;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.silverback.carman.R;
import com.silverback.carman.databinding.CardviewGasStationsBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.viewmodels.Opinet;
import com.silverback.carman.utils.Constants;
import com.silverback.carman.viewholders.StationListHolder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class StationListAdapter extends RecyclerView.Adapter<StationListHolder> {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(StationListAdapter.class);

    // Objects
    private CardviewGasStationsBinding binding;
    private Context context;
    private List<Opinet.GasStnParcelable> stationList;
    private final OnRecyclerItemClickListener mListener;

    // Interface
    public interface OnRecyclerItemClickListener {
        void onItemClicked(int pos);
    }

    // Constructor
    public StationListAdapter(List<Opinet.GasStnParcelable> list, OnRecyclerItemClickListener listener) {
        super();
        stationList = list;
        mListener = listener;
    }


    @NonNull
    @Override
    public StationListHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        binding = CardviewGasStationsBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new StationListHolder(context, binding);
    }

    @Override
    public void onBindViewHolder(@NonNull StationListHolder holder, int position) {
        final Opinet.GasStnParcelable data = stationList.get(position);
        holder.bindToStationList(data);

        holder.itemView.setOnClickListener(view -> {
            if(mListener != null) mListener.onItemClicked(position);
        });

    }


    @Override
    public void onBindViewHolder(
            @NonNull StationListHolder holder, int position, @NonNull List<Object> payloads) {
        if(payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads);
        } else {
            log.i("Carwash: %s", payloads.size());
            // On receiving car wash values, set the progressbar to be View.GONE and set the message
            // to the textview.
            for(Object obj : payloads) {
                String msg = ((boolean)obj) ?
                        context.getString(R.string.general_carwash_yes):
                        context.getString(R.string.general_carwash_no);
                // Update the car wash value.
                holder.getCarWashView().setText(msg);
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
    public List<Opinet.GasStnParcelable> sortStationList(boolean bStationOrder) {
        File file = new File(context.getCacheDir(), Constants.FILE_CACHED_NEAR_STATIONS);
        Uri uri = Uri.fromFile(file);

        try(InputStream is = context.getContentResolver().openInputStream(uri);
            ObjectInputStream ois = new ObjectInputStream(is)) {
            stationList = (List<Opinet.GasStnParcelable>)ois.readObject();

            if(bStationOrder) Collections.sort(stationList, new PriceAscCompare()); // Price Ascending order
            else Collections.sort(stationList, new DistanceDescCompare()); // Distance Ascending order

            notifyDataSetChanged();

            return stationList;

        } catch (FileNotFoundException e) {
            log.e("FileNotFoundException: %s", e.getMessage());
        } catch (IOException e) {
            log.e("IOException: %s", e.getMessage());
        } catch (ClassNotFoundException e) {
            log.e("ClassNotFoundException: %s", e.getMessage());
        } catch(ClassCastException e) {
            log.e("ClassCastException: %s", e.getMessage());
        }

        return null;
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

}
