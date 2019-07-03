package com.silverback.carman2.adapters;

import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.text.TextUtils;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.RecyclerView;

import com.silverback.carman2.BaseActivity;
import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.ServiceCheckListModel;

import java.text.DecimalFormat;
import java.util.List;

public class ServiceItemListAdapter extends RecyclerView.Adapter<ServiceItemListAdapter.ServiceItemViewHolder> {
    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(ServiceItemListAdapter.class);

    // Objects
   // private ServiceCheckListModel checkListModel;
    private Observer<SparseBooleanArray> chkboxObserver;
    private OnParentFragmentListener mListener;
    private DecimalFormat df;

    public boolean[] arrCheckedState;
    public int[] arrItemCost;
    public String[] arrItemMemo;
    public String[] arrItems;

    // Listener to communicate b/w the parent Fragment and this RecyclerView.Adapter
    // to invoke
    public interface OnParentFragmentListener {
        void inputItemCost(String title, TextView targetView, int position);
        void inputItemMemo(String title, TextView targetView, int position);
        void subtractCost(int value);
    }

    // Constructor
    public ServiceItemListAdapter(
            ServiceCheckListModel model, String[] items, OnParentFragmentListener listener) {

        super();

        this.arrItems = items;
        //checkListModel = model;
        mListener = listener;

        df = BaseActivity.getDecimalFormatInstance();

        arrCheckedState = new boolean[arrItems.length];
        arrItemCost = new int[arrItems.length];
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
    @SuppressWarnings("unchecked")
    @Override
    public void onBindViewHolder(@NonNull ServiceItemViewHolder holder, int pos, @NonNull List<Object> payloads){
        if(payloads.isEmpty()) {
            super.onBindViewHolder(holder, pos, payloads);
        } else {
            if(payloads.get(0) instanceof SparseIntArray) {
                SparseIntArray data = (SparseIntArray)payloads.get(0);
                holder.tvItemCost.setText(df.format(data.valueAt(0)));
                arrItemCost[pos] = data.valueAt(0);
            } else if(payloads.get(0) instanceof SparseArray) {
                SparseArray<String> data = (SparseArray)payloads.get(0);
                holder.tvItemMemo.setText(data.valueAt(0));
                arrItemMemo[pos] = data.valueAt(0);
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
            View.OnClickListener, CompoundButton.OnCheckedChangeListener {

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
            cbServiceItem.setOnClickListener(this);
            cbServiceItem.setOnCheckedChangeListener(this);
        }

        @Override
        public void onClick(View v) {
            final String title = tvItemName.getText().toString();
            switch(v.getId()) {
                case R.id.tv_value_cost:
                    // Subtract the number at first, no matter the number is zero or not in order
                    // to subtract a ready-input number from the total cost.
                    if(!TextUtils.equals(tvItemCost.getText(), "0")) {
                        tvItemCost.setText("0");
                        mListener.subtractCost(arrItemCost[getAdapterPosition()]);
                    }

                    mListener.inputItemCost(title, tvItemCost, getAdapterPosition());

                    break;

                case R.id.tv_item_info:
                    mListener.inputItemMemo(title, tvItemMemo, getAdapterPosition());
                    break;

                case R.id.chkbox:
                    if(cbServiceItem.isChecked()) {
                        int count = 0;
                        for(boolean b : arrCheckedState) count += (b ? 1 : 0);

                        if(count < 5) arrCheckedState[getAdapterPosition()] = true;
                        else {
                            Toast.makeText(v.getContext(), "Up to 5 items", Toast.LENGTH_SHORT).show();
                            cbServiceItem.setChecked(false);
                        }
                    } else {
                        arrCheckedState[getAdapterPosition()] = false;
                        if(arrItemCost[getAdapterPosition()] != 0)
                            mListener.subtractCost(arrItemCost[getAdapterPosition()]);

                        arrItemCost[getAdapterPosition()] = 0;
                        tvItemCost.setText("0");
                        tvItemMemo.setText("");

                    }
                    break;
            }
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if(isChecked) {
                layout.setVisibility(View.VISIBLE);
                animSlideUpAndDown(layout, 0, 120);
            } else {
                animSlideUpAndDown(layout, 120, 0);
            }
        }


        // Combine data in the adapter with views in the viewholder.
        void bindItemToHolder(int pos) {
            tvItemName.setText(arrItems[pos]);
            cbServiceItem.setChecked(arrCheckedState[pos]);

            if (arrCheckedState[pos]) {
                tvItemCost.setText(df.format(arrItemCost[pos]));
                tvItemMemo.setText(arrItemMemo[pos]);
            }

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
