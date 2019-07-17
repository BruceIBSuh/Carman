package com.silverback.carman2.adapters;

import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.text.TextUtils;
import android.util.SparseArray;
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
import androidx.recyclerview.widget.RecyclerView;

import com.silverback.carman2.BaseActivity;
import com.silverback.carman2.R;
import com.silverback.carman2.database.ServiceManagerDao;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import org.json.JSONArray;

import java.text.DecimalFormat;
import java.util.List;

public class ServiceItemListAdapter extends RecyclerView.Adapter<ServiceItemListAdapter.ServiceItemViewHolder> {
    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(ServiceItemListAdapter.class);



    // Objects
    private JSONArray jsonArray;
    private OnParentFragmentListener mListener;
    private DecimalFormat df;

    private SparseArray<ServiceManagerDao.ServicedItemData> servicedItems;
    public boolean[] arrCheckedState;
    public int[] arrItemCost;
    public String[] arrItemMemo;
    public String[] arrItems;


    // Fields
    private String format;

    // Listener to communicate b/w the parent Fragment and this RecyclerView.Adapter
    // to invoke
    public interface OnParentFragmentListener {
        void inputItemCost(String title, TextView targetView, int position);
        void inputItemMemo(String title, TextView targetView, int position);
        void subtractCost(int value);
    }

    // Constructor
    public ServiceItemListAdapter(
            JSONArray jsonArray,
            SparseArray<ServiceManagerDao.ServicedItemData> servicedItems,
            OnParentFragmentListener listener) {

        super();

        this.jsonArray = jsonArray;
        this.servicedItems = servicedItems;
        mListener = listener;

        df = BaseActivity.getDecimalFormatInstance();

        arrCheckedState = new boolean[jsonArray.length()];
        arrItemCost = new int[jsonArray.length()];
        arrItemMemo = new String[jsonArray.length()];
    }

    @NonNull
    @Override
    public ServiceItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        CardView cardView = (CardView)LayoutInflater.from(parent.getContext())
                .inflate(R.layout.view_card_serviceitem, parent, false);

        format = cardView.getContext().getResources().getString(R.string.date_format_1);

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
        return jsonArray.length();
    }

    /*
     * ViewModel
     */
    class ServiceItemViewHolder extends RecyclerView.ViewHolder implements
            View.OnClickListener, CompoundButton.OnCheckedChangeListener {

        // UIs
        ConstraintLayout layout;
        TextView tvItemName;
        TextView tvLastService;
        TextView tvItemCost;
        TextView tvItemMemo;
        CheckBox cbServiceItem;

        ServiceItemViewHolder(CardView view){
            super(view);

            layout = view.findViewById(R.id.constraint_stmts);
            tvItemName = view.findViewById(R.id.tv_setting_item);
            tvLastService = view.findViewById(R.id.tv_last_service);
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
                        //tvItemCost.setText("0");
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
            //tvItemName.setText(arrItems[pos]);
            tvItemName.setText(jsonArray.optJSONObject(pos).optString("name"));
            cbServiceItem.setChecked(arrCheckedState[pos]);

            if(servicedItems.get(pos) != null) {
                String date = BaseActivity.formatMilliseconds(format, servicedItems.get(pos).dateTime);
                String mileage = df.format(servicedItems.get(pos).mileage);
                tvLastService.setText(String.format("%s, %s%s", date, mileage, "km"));
            }

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
