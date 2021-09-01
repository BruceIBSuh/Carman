package com.silverback.carman.adapters;

import android.animation.ValueAnimator;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.StyleSpan;
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

import com.silverback.carman.BaseActivity;
import com.silverback.carman.R;
import com.silverback.carman.database.ServiceManagerDao;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.List;

public class ExpServiceItemAdapter extends RecyclerView.Adapter<ExpServiceItemAdapter.ServiceItemViewHolder> {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(ExpServiceItemAdapter.class);

    // Objects
    private JSONArray jsonArray;
    private OnParentFragmentListener mListener;
    private DecimalFormat df;

    private SparseArray<ServiceManagerDao.LatestServiceData> sparseArray;
    public boolean[] arrCheckedState;
    public int[] arrItemCost;
    public String[] arrItemMemo;

    // Fields
    private String format, unit, month;
    private int curMileage;
    private int safeColor, warningColor, progressColor;
    private int svcCheckPeriod;
    private int itemPos;

    // Listener to communicate b/w the parent fragment and the RecyclerView.Adapter therein
    // to get the values from the service item recyclerview.
    public interface OnParentFragmentListener {
        void inputItemCost(String title, TextView targetView, int position);
        void inputItemMemo(String title, TextView targetView, int position);
        void subtractCost(int value);
        int getCurrentMileage();
    }

    // Constructor
    public ExpServiceItemAdapter(JSONArray jsonArray, int period, OnParentFragmentListener listener) {
        super();
        this.jsonArray = jsonArray;
        mListener = listener;
        this.svcCheckPeriod = period;

        df = BaseActivity.getDecimalFormatInstance();
        arrCheckedState = new boolean[jsonArray.length()];
        arrItemCost = new int[jsonArray.length()];
        arrItemMemo = new String[jsonArray.length()];

        sparseArray = new SparseArray<>();
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
        safeColor = cardView.getResources().getColor(R.color.pbSafe);
        warningColor = cardView.getResources().getColor(R.color.pbWarning);

        return new ServiceItemViewHolder(cardView);
    }

    @Override
    public void onBindViewHolder(@NonNull ServiceItemViewHolder holder, int position) {
        JSONObject jsonObject = jsonArray.optJSONObject(position);
        final int maxMileage = jsonObject.optInt("mileage");
        final int maxMonth = jsonObject.optInt("month");
        int lastMileage = 0;
        long lastTime = 0;
        int maxValue = 0;
        int lapse = 0;

        log.i("Position: %s", position);

        holder.tvItemName.setText(jsonObject.optString("name"));
        holder.cbServiceItem.setChecked(arrCheckedState[position]);
        // Retain the values of service cost and memo when rebound.
        if (arrCheckedState[position]) {
            holder.tvItemCost.setText(df.format(arrItemCost[position]));
            holder.tvItemMemo.setText(arrItemMemo[position]);
        }

        if(sparseArray.get(position) != null) {
            lastMileage = sparseArray.get(position).mileage;
            lastTime = sparseArray.get(position).dateTime;
            log.i("lastMonth: %s", lastTime);
            String date = BaseActivity.formatMilliseconds(format, lastTime);
            String mileage = df.format(lastMileage);
            holder.tvLastService.setText(String.format("%s %s%s", date, mileage, unit));

        } else {
            holder.tvLastService.setText("");
        }

        SpannableStringBuilder ssb = new SpannableStringBuilder();
        String mileageUnit = df.format(maxMileage) + unit;
        String monthUnit = jsonObject.optInt("month") + month;
        ssb.append(mileageUnit).append("/").append(monthUnit);
        if (svcCheckPeriod == 0) {
            ssb.setSpan(new StyleSpan(Typeface.BOLD), 0, mileageUnit.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            lapse = Math.min(curMileage - lastMileage, maxMileage);
            maxValue = maxMileage;
        } else if (svcCheckPeriod == 1) {
            ssb.setSpan(new StyleSpan(Typeface.BOLD), mileageUnit.length() + 1, ssb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            maxValue = maxMonth * 30 * 24 * 60;
            lapse = (int)((System.currentTimeMillis() - lastTime) / (1000 * 60));
        }

        holder.tvMaxPeriod.setText(ssb);

        // Set the progressbar color and animate it
        setServiceProgressBar(holder, lapse, maxValue);
    }

    /*
     * Partial Bind: onBindViewHolder(holder, pos, payloads) vs Full bind: onBindViewHolder(holder, pos)
     * The payloads parameter is a merge list from notifyItemChanged(int, Object) or
     * notifyItemRangeChanged(int, int, Object).
     * If the payloads list is not empty, the ViewHolder is currently bound to old data and
     * Adapter may run an efficient partial update using the payload info. If the payload is empty,
     * Adapter must run a full bind. Adapter should not assume that the payload passed in notify methods
     * will be received by onBindViewHolder()
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
                    String mileage = df.format(data.mileage);

                    JSONObject jsonObject = jsonArray.optJSONObject(pos);
                    final int maxMileage = jsonObject.optInt("mileage");
                    final int lastMileage = data.mileage;
                    int period = Math.min(curMileage - lastMileage, maxMileage);

                    // Partial Binding of the last service and the ProgressBar
                    holder.tvLastService.setText(String.format("%s %s%s", date, mileage, unit));

                    // Set the progress color according to the period / maxMileage ratio.
                    setServiceProgressBar(holder, period, maxMileage);
                }
            }

        }

    }

    @Override
    public int getItemCount() {
        return jsonArray.length();
    }


    public void setServiceData(int position, ServiceManagerDao.LatestServiceData data) {
        sparseArray.put(position, data);
        notifyDataSetChanged();
    }

    // RecyclerView.ViewHolder
    static class ServiceItemViewHolder extends RecyclerView.ViewHolder implements
            View.OnClickListener,
            CompoundButton.OnCheckedChangeListener {

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
            tvItemName = view.findViewById(R.id.tv_name);
            tvLastService = view.findViewById(R.id.tv_last_service);
            tvItemCost = view.findViewById(R.id.tv_value_cost);
            tvItemMemo = view.findViewById(R.id.tv_item_info);
            tvMaxPeriod = view.findViewById(R.id.tv_max_period);
            cbServiceItem = view.findViewById(R.id.chkbox);
            pb = view.findViewById(R.id.progbar_service_period);

            tvItemCost.setOnClickListener(this);
            tvItemMemo.setOnClickListener(this);
            //cbServiceItem.setOnCheckedChangeListener(this);
            //binding.chkbox.setOnCheckedChangeListener(this);

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
                        //mListener.subtractCost(arrItemCost[getAdapterPosition()]);
                    }

                    //mListener.inputItemCost(title, tvItemCost, getAdapterPosition());
                    break;

                case R.id.tv_item_info:
                    //mListener.inputItemMemo(title, tvItemMemo, getAdapterPosition());
                    break;
            }
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

            final int pos = getAdapterPosition(); // get the position of a checked item.
            //arrCheckedState[pos] = isChecked; // update checked items in the entire list.

            if(isChecked) {
                layout.setVisibility(View.VISIBLE);
                animSlideUpAndDown(layout, 0, 75);

            } else {
                animSlideUpAndDown(layout, 75, 0);
                // Substract the item cost input at the moment out of the total cost.
                //if(arrItemCost[pos] != 0) mListener.subtractCost(arrItemCost[pos]);
                //arrItemCost[pos] = 0;
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

            ViewGroup.LayoutParams params = layout.getLayoutParams();
            ValueAnimator animSlide = ValueAnimator.ofInt(convStartValue, convEndValue);
            animSlide.addUpdateListener(valueAnimator -> {
                //target.getLayoutParams().height = (Integer)valueAnimator.getAnimatedValue();
               // target.requestLayout();
                params.height = (Integer)valueAnimator.getAnimatedValue();
                layout.setLayoutParams(params);
            });

            animSlide.setDuration(500);
            animSlide.start();
        }

    }

    private void setServiceProgressBar(ServiceItemViewHolder holder, int lapse, int maxValue) {
        int progressColor = (lapse >= (maxValue * 0.8))? warningColor : safeColor;
        Drawable progressDrawable = ((LayerDrawable)holder.pb.getProgressDrawable()).getDrawable(1);
        progressDrawable.setColorFilter(progressColor, PorterDuff.Mode.SRC_IN);

        // Animate the progress
        ProgressBarAnimation pbAnim = new ProgressBarAnimation(holder.pb, 0, lapse);
        pbAnim.setDuration(500);
        holder.pb.setMax(maxValue);
        holder.pb.startAnimation(pbAnim);
    }

    // ProgressBar Animation class from StackOverflow
    static class ProgressBarAnimation extends Animation {
        private ProgressBar progressBar;
        private float from;
        private float to;

        //ProgressBarAnimation(ProgressBar progressBar, float from, float to) {
        ProgressBarAnimation(ProgressBar progressBar, float from, float to) {
            super();
            this.from = from;
            this.to = to;
            this.progressBar = progressBar;
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            super.applyTransformation(interpolatedTime, t);
            float value = from + (to - from) * interpolatedTime;
            progressBar.setProgress((int)value);
        }

    }
}
