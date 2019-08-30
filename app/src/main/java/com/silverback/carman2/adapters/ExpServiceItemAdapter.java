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

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.silverback.carman2.BaseActivity;
import com.silverback.carman2.R;
import com.silverback.carman2.database.ServiceManagerDao;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.PagerAdapterViewModel;
import com.silverback.carman2.threads.ProgressBarAnimTask;
import com.silverback.carman2.threads.ThreadManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.List;

public class ExpServiceItemAdapter extends RecyclerView.Adapter<ExpServiceItemAdapter.ServiceItemViewHolder> {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(ExpServiceItemAdapter.class);

    // Objects
    private ProgressBarAnimTask mTask;
    private PagerAdapterViewModel model;
    private JSONArray jsonArray;
    private OnParentFragmentListener mListener;
    private DecimalFormat df;

    private SparseArray<ServiceManagerDao.LatestServiceData> serviceData;
    public boolean[] arrCheckedState;
    public int[] arrItemCost;
    public String[] arrItemMemo;

    // UIs
    private TextView tvItemCost, tvItemMemo;
    private CheckBox cbServiceItem;
    private ProgressBar progressBar;

    // Fields
    private String format, unit, month;
    private int curMileage;
    private int lastMileage;

    // Listener to communicate b/w the parent fragment and the RecyclerView.Adapter therein
    // to get the values from the service item recyclerview.
    public interface OnParentFragmentListener {
        void inputItemCost(String title, TextView targetView, int position);
        void inputItemMemo(String title, TextView targetView, int position);
        void subtractCost(int value);
        int getCurrentMileage();
    }

    // Constructor
    //public ExpServiceItemAdapter(JSONArray jsonArray, OnParentFragmentListener listener) {
    public ExpServiceItemAdapter(PagerAdapterViewModel model, JSONArray jsonArray, OnParentFragmentListener listener) {

        super();

        this.model = model;
        this.jsonArray = jsonArray;
        df = BaseActivity.getDecimalFormatInstance();
        arrCheckedState = new boolean[jsonArray.length()];
        arrItemCost = new int[jsonArray.length()];
        arrItemMemo = new String[jsonArray.length()];
        serviceData = new SparseArray<>();
        mListener = listener;

    }


    @NonNull
    @Override
    public ServiceItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        CardView cardView = (CardView)LayoutInflater.from(parent.getContext())
                .inflate(R.layout.cardview_service_item, parent, false);

        format = cardView.getResources().getString(R.string.date_format_2);
        unit = cardView.getResources().getString(R.string.unit_km);
        month = cardView.getResources().getString(R.string.unit_month);
        curMileage = mListener.getCurrentMileage();

        return new ServiceItemViewHolder(cardView);
    }

    @Override
    public void onBindViewHolder(@NonNull ServiceItemViewHolder holder, int position) {

        JSONObject jsonObject = jsonArray.optJSONObject(position);
        final int maxMileage = jsonObject.optInt("mileage");
        int lastMileage = 0;

        holder.tvItemName.setText(jsonObject.optString("name"));
        holder.cbServiceItem.setChecked(arrCheckedState[position]);
        holder.tvMaxPeriod.setText(String.format("%s%s/%s%s",
                df.format(maxMileage), unit, jsonObject.optInt("month"), month));


        if(serviceData.get(position) != null) {
            log.i("service data: %s, %s", position, serviceData.get(position).itemName);
            String date = BaseActivity.formatMilliseconds(format, serviceData.get(position).dateTime);
            String mileage = df.format(serviceData.get(position).mileage);
            holder.tvLastService.setText(String.format("%s %s%s", date, mileage, unit));
        } else {
            log.i("no serviced: %s, %s", position, jsonObject.optString("name"));
        }

        // Animate the ProgressBar
        synchronized (this) {
            log.i("ProgressBar animation: %s", position);
            //int period = (curMileage - lastMileage <= maxMileage) ? curMileage - lastMileage : maxMileage;
            //holder.pb.setMax(maxMileage);
            final ProgressBarAnimation pbAnim = new ProgressBarAnimation(holder.pb, lastMileage, maxMileage);
            pbAnim.setDuration(1000);
            holder.pb.startAnimation(pbAnim);
        }

        // Retain the values of service cost and memo when rebound.
        if (arrCheckedState[position]) {
            holder.tvItemCost.setText(df.format(arrItemCost[position]));
            holder.tvItemMemo.setText(arrItemMemo[position]);
        }
    }

    /*
     * Partial Bind: onBindViewHolder(holder, pos, payloads) vs Full bind: onBindViewHolder(holder, pos)
     * The payloads parameter is a merge list from notifyItemChanged(int, Object) or
     * notifyItemRangeChanged(int, int, Object).f the payloads list is not empty, the ViewHolder is currently bound to old data and
     * Adapter may run an efficient partial update using the payload info. If the payload is empty, Adapter must run a full bind.
     * Adapter should not assume that the payload passed in notify methods will be received by onBindViewHolder()
     */
    @Override
    public void onBindViewHolder(@NonNull ServiceItemViewHolder holder, int pos, @NonNull List<Object> payloads){

        if(payloads.isEmpty()) {
            super.onBindViewHolder(holder, pos, payloads);

        } else {

            for(Object payload: payloads) {
                if(payload instanceof SparseIntArray) {
                    SparseIntArray data = (SparseIntArray)payload;
                    holder.tvItemCost.setText(df.format(data.valueAt(0)));
                    arrItemCost[pos] = data.valueAt(0);

                }else if(payload instanceof SparseArray) {
                    SparseArray data = (SparseArray)payload;
                    holder.tvItemMemo.setText(data.valueAt(0).toString());
                    arrItemMemo[pos] = data.valueAt(0).toString();

                }else if(payload instanceof ServiceManagerDao.LatestServiceData) {
                    ServiceManagerDao.LatestServiceData data = (ServiceManagerDao.LatestServiceData)payload;
                    String date = BaseActivity.formatMilliseconds(format, data.dateTime);
                    String mileage = df.format(lastMileage = data.mileage);

                    // Partial Binding
                    holder.tvLastService.setText(String.format("%s %s%s", date, mileage, unit));

                    JSONObject jsonObject = jsonArray.optJSONObject(pos);
                    final int maxMileage = jsonObject.optInt("mileage");
                    final int lastMileage = data.mileage;
                    //int period = (curMileage - lastMileage <= maxMileage)? curMileage - lastMileage : maxMileage;
                    //holder.pb.setMax(maxMileage);

                    final ProgressBarAnimation pbAnim = new ProgressBarAnimation(holder.pb, lastMileage, maxMileage);
                    pbAnim.setDuration(1000);
                    holder.pb.startAnimation(pbAnim);



                }
            }

        }

    }

    @Override
    public int getItemCount() {
        return jsonArray.length();
    }


    public void setServiceData(int position, ServiceManagerDao.LatestServiceData data) {
        log.i("service data in the adapter: %s, %s", position, data.itemName);
        serviceData.put(position, data);
    }

    // RecyclerView.ViewHolder
    class ServiceItemViewHolder extends RecyclerView.ViewHolder implements
            View.OnClickListener, CompoundButton.OnCheckedChangeListener {

        // UIs
        ConstraintLayout layout;
        TextView tvItemName;
        TextView tvLastService;
        TextView tvItemCost;
        TextView tvItemMemo;
        TextView tvMaxPeriod;
        CheckBox cbServiceItem;
        ProgressBar pb;

        ServiceItemViewHolder(CardView view){
            super(view);

            layout = view.findViewById(R.id.constraint_stmts);
            tvItemName = view.findViewById(R.id.tv_service_item);
            tvLastService = view.findViewById(R.id.tv_last_service);
            tvItemCost = view.findViewById(R.id.tv_value_cost);
            tvItemMemo = view.findViewById(R.id.tv_item_info);
            tvMaxPeriod = view.findViewById(R.id.tv_max_period);
            cbServiceItem = view.findViewById(R.id.chkbox);
            pb = view.findViewById(R.id.progbar_service_period);

            tvItemCost.setOnClickListener(this);
            tvItemMemo.setOnClickListener(this);
            //cbServiceItem.setOnClickListener(this);
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
            }
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

            final int pos = getAdapterPosition(); // get the position of a checked item.
            arrCheckedState[pos] = isChecked; // update checked items in the entire list.

            if(isChecked) {
                layout.setVisibility(View.VISIBLE);
                animSlideUpAndDown(layout, 0, 75);
                /*
                int curMileage = mListener.getCurrentMileage();
                int lastMileage = (sparseSvcDataArray.get(pos) != null)? sparseSvcDataArray.get(pos).mileage : 0;
                //maxMileage = jsonArray.optJSONObject(pos).optInt("mileage");
                period = (curMileage - lastMileage <= maxMileage)? curMileage - lastMileage : maxMileage;
                pb.setMax(maxMileage);
                log.i("onBindViewHolder: %s, %s, %s, %s", curMileage, lastMileage, maxMileage, period);

                final ProgressBarAnimation pbAnim = new ProgressBarAnimation(pb, 0, period);
                pbAnim.setDuration(1000);
                pb.startAnimation(pbAnim);
                */


            } else {
                animSlideUpAndDown(layout, 75, 0);
                // Substract the item cost input at the moment out of the total cost.
                if(arrItemCost[pos] != 0) mListener.subtractCost(arrItemCost[pos]);
                arrItemCost[pos] = 0;
                tvItemCost.setText("0");
                tvItemMemo.setText("");
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
        private float to;

        //ProgressBarAnimation(ProgressBar progressBar, float from, final float to) {
        ProgressBarAnimation(ProgressBar progressBar, float lastMileage, float maxMileage) {
            super();

            this.progressBar = progressBar;
            this.to = (curMileage - lastMileage <= maxMileage)? curMileage - lastMileage : maxMileage;
            this.progressBar.setMax((int)maxMileage);
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            super.applyTransformation(interpolatedTime, t);
            final float from = 0;
            float value = from + (to - from) * interpolatedTime;
            progressBar.setProgress((int)value);
        }
    }

}
