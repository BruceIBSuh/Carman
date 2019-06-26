package com.silverback.carman2.adapters;

import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.RecyclerView;

import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.ServiceCheckListModel;
import com.silverback.carman2.viewholders.ServiceItemHolder;

import java.util.ArrayList;
import java.util.List;

public class ServiceItemListAdapter extends RecyclerView.Adapter<ServiceItemHolder> {
    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(ServiceItemListAdapter.class);

    // Objects
    private ServiceCheckListModel checkListModel;
    private Observer<SparseBooleanArray> chkboxObserver;
    private OnNumPadListener mListener;
    private SparseBooleanArray sparseArrayChkbox;
    private boolean[] arrCheckedState;
    private String[] arrItemCost;
    private String[] arrItemMemo;
    private ArrayList<String> serviceList;
    private String[] arrItems;

    // UIs
    private TextView tvItemName, tvItemCost;
    private EditText etItemMemo;
    private CheckBox chkbox;

    // Fields
    private boolean isCheckBoxChecked;


    public interface OnNumPadListener {
        void inputItemCost(String itemName, TextView view, int position);
    }

    // Constructor
    public ServiceItemListAdapter(String[] arrItems, ServiceCheckListModel model, OnNumPadListener listener) {

        super();

        checkListModel = model;
        mListener = listener;
        this.arrItems = arrItems;
        sparseArrayChkbox = new SparseBooleanArray();

        chkboxObserver = new Observer<SparseBooleanArray>(){
            @Override
            public void onChanged(SparseBooleanArray sparseBooleanArray) {
                sparseArrayChkbox.put(sparseBooleanArray.keyAt(0), sparseBooleanArray.valueAt(0));
            }
        };

        checkListModel.getChkboxState().observeForever(chkboxObserver);
    }

    @NonNull
    @Override
    public ServiceItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        log.i("onCreateViewHolder");
        CardView cardView = (CardView)LayoutInflater.from(parent.getContext())
                .inflate(R.layout.view_card_serviceitem, parent, false);

        return new ServiceItemHolder(cardView, checkListModel, mListener);
    }


    @Override
    public void onBindViewHolder(@NonNull ServiceItemHolder holder, int position) {
        log.i("onBindViewHolder: %s, %s", position, sparseArrayChkbox.get(position));
        holder.bindItemToHolder(arrItems[position], sparseArrayChkbox.get(position));
    }

    // Invoked by notifyItemChanged of RecyclerView.Adapter with payloads as param.
    @Override
    public void onBindViewHolder(@NonNull ServiceItemHolder holder, int pos, @NonNull List<Object> payloads){
        if(payloads.isEmpty()) {
            super.onBindViewHolder(holder, pos, payloads);

        } else {
            for(Object payload : payloads) {
                if(payload instanceof String) {
                    TextView tvCost = holder.itemView.findViewById(R.id.tv_value_cost);
                    tvCost.setBackgroundResource(android.R.color.white);
                    tvCost.setText((String)payload);
                }
            }
        }
    }


    @Override
    public int getItemCount() {
        return arrItems.length;
    }

}
