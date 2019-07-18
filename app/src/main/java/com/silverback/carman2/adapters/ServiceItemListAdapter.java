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
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
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
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.List;

public class ServiceItemListAdapter extends RecyclerView.Adapter<ServiceItemListAdapter.ServiceItemViewHolder> {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(ServiceItemListAdapter.class);

    // Objects
    private JSONArray jsonArray;
    private OnParentFragmentListener mListener;
    private ProgressBarAnimation pbAnim;
    private DecimalFormat df;

    private SparseArray<ServiceManagerDao.ServicedItemData> servicedItems;
    public boolean[] arrCheckedState;
    public int[] arrItemCost;
    public String[] arrItemMemo;
    //public String[] arrItems;


    // Fields
    private int period;
    private int currentMileage;

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
        format = cardView.getContext().getResources().getString(R.string.date_format_2);

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

        JSONObject jsonObject = jsonArray.optJSONObject(position);
        holder.tvItemName.setText(jsonObject.optString("name"));
        holder.cbServiceItem.setChecked(arrCheckedState[position]);

        log.i("Last Service: %s", servicedItems.size());

        if(servicedItems.get(position) != null) {
            String date = BaseActivity.formatMilliseconds(format, servicedItems.get(position).dateTime);
            String mileage = df.format(servicedItems.get(position).mileage);
            holder.tvLastService.setText(String.format("%s, %s%s", date, mileage, "km"));
        }

        if (arrCheckedState[position]) {
            holder.tvItemCost.setText(df.format(arrItemCost[position]));
            holder.tvItemMemo.setText(arrItemMemo[position]);
        }

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
        ProgressBar progBar;


        ServiceItemViewHolder(CardView view){
            super(view);

            layout = view.findViewById(R.id.constraint_stmts);
            tvItemName = view.findViewById(R.id.tv_setting_item);
            tvLastService = view.findViewById(R.id.tv_last_service);
            tvItemCost = view.findViewById(R.id.tv_value_cost);
            tvItemMemo = view.findViewById(R.id.tv_item_info);
            cbServiceItem = view.findViewById(R.id.chkbox);
            progBar = view.findViewById(R.id.progressBar);

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
                    if(cbServiceItem.isChecked()) arrCheckedState[getAdapterPosition()] = true;
                    else {
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

    // ProgressBar Animation class from StackOverflow
    class ProgressBarAnimation extends Animation {

        private ProgressBar progressBar;
        private float from;
        private float to;

        ProgressBarAnimation(ProgressBar progressBar, float from, final float to) {
            super();
            this.progressBar = progressBar;
            this.from = from;
            this.to = to;
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            super.applyTransformation(interpolatedTime, t);
            float value = from + (to - from) * interpolatedTime;
            progressBar.setProgress((int)value);
        }
    }

}
