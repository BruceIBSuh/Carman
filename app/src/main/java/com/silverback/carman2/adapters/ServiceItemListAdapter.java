package com.silverback.carman2.adapters;

import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.RecyclerView;

import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.ServiceCheckListModel;

import java.util.List;

public class ServiceItemListAdapter extends RecyclerView.Adapter<ServiceItemListAdapter.ServiceItemViewHolder> {
    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(ServiceItemListAdapter.class);

    // Objects
    private Fragment serviceFragment;
    private ServiceCheckListModel checkListModel;
    private Observer<SparseBooleanArray> chkboxObserver;
    private OnParentFragmentListener mListener;

    private boolean[] arrCheckedState;
    private String[] arrItemCost;
    private String[] arrItemMemo;
    private String[] arrItems;

    public interface OnParentFragmentListener {
        void inputItemCost(String title, TextView targetView, int position);
        void inputItemMemo(String title, TextView targetView, int position);
        void subtractCost(String value);
    }

    // Constructor
    public ServiceItemListAdapter(Fragment fm, ServiceCheckListModel model, String[] items) {

        super();

        serviceFragment = fm;
        checkListModel = model;
        mListener = (OnParentFragmentListener)fm;
        this.arrItems = items;

        arrCheckedState = new boolean[arrItems.length];
        arrItemCost = new String[arrItems.length];
        arrItemMemo = new String[arrItems.length];

    }

    @NonNull
    @Override
    public ServiceItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        CardView cardView = (CardView)LayoutInflater.from(parent.getContext())
                .inflate(R.layout.view_card_serviceitem, parent, false);

        return new ServiceItemViewHolder(cardView);
    }


    // Invoked by notifyItemChanged of RecyclerView.Adapter with payloads as param.
    @Override
    public void onBindViewHolder(@NonNull ServiceItemViewHolder holder, int pos, @NonNull List<Object> payloads){
        if(payloads.isEmpty()) {
            super.onBindViewHolder(holder, pos, payloads);
        } else {
            if(payloads.get(0) instanceof String) {
                String value = (String)payloads.get(0);
                holder.tvItemCost.setText(value);
                arrItemCost[pos] = value;
            }

        }
    }

    @Override
    public void onBindViewHolder(@NonNull ServiceItemViewHolder holder, int position) {
        holder.bindItemToHolder(position);
    }

    @Override
    public int getItemCount() {
        return arrItems.length;
    }



    /**
     * ViewModel
     */
    class ServiceItemViewHolder extends RecyclerView.ViewHolder implements
            CompoundButton.OnCheckedChangeListener, View.OnClickListener {

        // UIs
        ConstraintLayout layout;
        TextView tvItemName;
        TextView tvItemCost;
        TextView tvItemMemo;
        CheckBox cbServiceItem;

        ServiceItemViewHolder(CardView view){
            super(view);

            layout = view.findViewById(R.id.constraint_stmts);
            tvItemName = view.findViewById(R.id.tv_item_name);
            tvItemCost = view.findViewById(R.id.tv_value_cost);
            tvItemMemo = view.findViewById(R.id.tv_item_info);
            cbServiceItem = view.findViewById(R.id.chkbox);

            tvItemCost.setOnClickListener(this);
            tvItemMemo.setOnClickListener(this);
            cbServiceItem.setOnCheckedChangeListener(this);

        }

        @Override
        public void onClick(View v) {
            final String title = tvItemName.getText().toString();
            switch(v.getId()) {
                case R.id.tv_value_cost:
                    mListener.inputItemCost(title, tvItemCost, getAdapterPosition());
                    break;

                case R.id.tv_item_info:
                    mListener.inputItemMemo(title, tvItemMemo, getAdapterPosition());
                    break;

            }
        }


        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

            // Check if checked items are beyond the number set in SharedPreferences.
            // Required to be completed.
            int count = 0;
            for(boolean b : arrCheckedState) count += (b ? 1 : 0);
            log.i("checked count: %s, %s", count, buttonView);

            if(isChecked) {
                layout.setVisibility(View.VISIBLE);
                animSlideUpAndDown(layout, 0, 120);
            } else {
                // Subtract the cost from the total cost when canceling using the listener.
                mListener.subtractCost(tvItemCost.getText().toString());
                tvItemCost.setText("0");
                animSlideUpAndDown(layout, 120, 0);

            }

            // Set a checked state value in boolean Array to retain the value while recycling.
            arrCheckedState[getAdapterPosition()] = isChecked;
        }

        // Combine data in the adapter with views in the viewholder.
        void bindItemToHolder(int position) {
            tvItemName.setText(arrItems[position]);
            cbServiceItem.setChecked(arrCheckedState[position]);
            tvItemCost.setText(arrItemCost[position]);
            tvItemMemo.setText(arrItemMemo[position]);
        }

        void animSlideUpAndDown(View target, int startValue, int endValue) {
            // Convert dp to int
            int convEndValue = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, endValue, target.getResources().getDisplayMetrics());

            int convStartValue = (int)TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, startValue, target.getResources().getDisplayMetrics());

            ValueAnimator animSlide = ValueAnimator.ofInt(convStartValue, convEndValue).setDuration(500);
            animSlide.addUpdateListener(valueAnimator -> {
                target.getLayoutParams().height = (Integer)valueAnimator.getAnimatedValue();
                target.requestLayout();

            });

            AnimatorSet animSet = new AnimatorSet();
            animSet.play(animSlide);
            animSet.start();
        }


    }

}
