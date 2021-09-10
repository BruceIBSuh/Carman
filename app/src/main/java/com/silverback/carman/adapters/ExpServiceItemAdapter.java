package com.silverback.carman.adapters;

import android.animation.ValueAnimator;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.silverback.carman.BaseActivity;
import com.silverback.carman.R;
import com.silverback.carman.database.ServiceManagerDao;
import com.silverback.carman.databinding.CardviewServiceItemBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.Constants;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.List;

public class ExpServiceItemAdapter extends RecyclerView.Adapter<ExpServiceItemAdapter.ServiceItemViewHolder> {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(ExpServiceItemAdapter.class);

    private final JSONArray jsonArray;
    private final OnParentFragmentListener callback;
    private final DecimalFormat df;

    private final SparseArray<ServiceManagerDao.LatestServiceData> sparseArray;
    public boolean[] arrCheckedState;
    public int[] arrItemCost;
    public String[] arrItemMemo;

    // Fields
    private String format, unit, month;
    private int curMileage;
    private int safeColor, warningColor, progressColor;
    private final int svcCheckPeriod;
    private int itemPos;

    // Listener to communicate b/w the parent fragment and the RecyclerView.Adapter therein
    // to get the values from the service item recyclerview.
    public interface OnParentFragmentListener {
        void setItemPosition(int position);
        //void inputItemCost(String title, TextView targetView, int position);
        //void inputItemMemo(String title, TextView targetView, int position);
        void subtractCost(int value);
        int getCurrentMileage();
    }

    // Constructor
    public ExpServiceItemAdapter(JSONArray jsonArray, int period, OnParentFragmentListener callback) {
        super();

        this.callback = callback;
        this.jsonArray = jsonArray;
        this.svcCheckPeriod = period;

        df = BaseActivity.getDecimalFormatInstance();
        arrCheckedState = new boolean[jsonArray.length()];
        arrItemCost = new int[jsonArray.length()];
        arrItemMemo = new String[jsonArray.length()];
        sparseArray = new SparseArray<>();

        curMileage = callback.getCurrentMileage();

    }

    @NonNull
    @Override
    public ServiceItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        CardviewServiceItemBinding binding = CardviewServiceItemBinding.inflate(inflater, parent, false);

        // Set initial values.
        format = parent.getResources().getString(R.string.date_format_2);
        unit = parent.getResources().getString(R.string.unit_km);
        month = parent.getResources().getString(R.string.unit_month);
        safeColor = ContextCompat.getColor(parent.getContext(), R.color.pbSafe);
        warningColor = ContextCompat.getColor(parent.getContext(), R.color.pbWarning);

        return new ServiceItemViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ServiceItemViewHolder holder, int position) {

        JSONObject jsonObject = jsonArray.optJSONObject(position);

        final int defaultMileage = jsonObject.optInt("mileage");
        final int defaultMonth = jsonObject.optInt("month");

        int maxValue = 0;
        int lapse = 0;


        holder.binding.tvItemName.setText(jsonObject.optString("name"));
        holder.binding.chkbox.setChecked(arrCheckedState[position]);

        holder.binding.chkbox.setOnCheckedChangeListener((buttion, isChecked) -> {
            arrCheckedState[position] = isChecked;
            if(isChecked) {
                callback.setItemPosition(position);
                holder.animSlideUpAndDown(holder.binding.layoutDataInput, 0, 80);

            } else {
                callback.subtractCost(arrItemCost[position]);
                holder.animSlideUpAndDown(holder.binding.layoutDataInput, 80, 0);
                holder.binding.tvItemCost.setText("0");
                holder.binding.tvItemMemo.setText("");
            }
        });


        // Retain the values of service cost and memo when rebound.
        if (arrCheckedState[position]) {
            holder.binding.tvItemCost.setText(df.format(arrItemCost[position]));
            holder.binding.tvItemMemo.setText(arrItemMemo[position]);
        }

        /*
        if(sparseArray.get(position) != null) {
            log.i("sparsearray: %s", position);
            lastMileage = sparseArray.get(position).mileage;
            lastTime = sparseArray.get(position).dateTime;
            String date = BaseActivity.formatMilliseconds(format, lastTime);
            String mileage = df.format(lastMileage);
            holder.binding.tvLastService.setText(String.format("%s %s%s", date, mileage, unit));

        } else holder.binding.tvLastService.setText("");
        */

        SpannableStringBuilder ssb = new SpannableStringBuilder();
        String mileageUnit = df.format(defaultMileage) + unit;
        String monthUnit = jsonObject.optInt("month") + month;
        ssb.append(mileageUnit).append("/").append(monthUnit);

        if (svcCheckPeriod == 0) {
            ssb.setSpan(new StyleSpan(Typeface.BOLD),
                    0, mileageUnit.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            //lapse = Math.min(curMileage - lastMileage, defaultMileage);
            lapse = Math.min(curMileage, defaultMileage);
            maxValue = defaultMileage;
        } else if (svcCheckPeriod == 1) {
            ssb.setSpan(new StyleSpan(Typeface.BOLD),
                    mileageUnit.length() + 1, ssb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            maxValue = defaultMonth * 30 * 24 * 60;
            lapse = (int)(Math.min(System.currentTimeMillis(), maxValue) / (1000 * 60));
        }

        holder.binding.tvDefaultPeriod.setText(ssb);

        // Set the progressbar color and animate it
        log.i("holder: %s, %s", position, holder.binding.tvItemName.getText());
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
        log.i("payloads: %s", payloads);
        if(payloads.isEmpty()) super.onBindViewHolder(holder, pos, payloads);
        else {
            for(Object payload: payloads) {
                if(payload instanceof SparseIntArray) {
                    log.i("SparseIntArray");
                    SparseIntArray data = (SparseIntArray)payload;
                    holder.binding.tvItemCost.setText(df.format(data.valueAt(0)));
                    arrItemCost[pos] = data.valueAt(0);

                } else if(payload instanceof SparseArray<?>) {
                    log.i("SparseArray<?>");
                    SparseArray<?> data = (SparseArray<?>)payload;
                    holder.binding.tvItemMemo.setText(data.valueAt(0).toString());
                    arrItemMemo[pos] = data.valueAt(0).toString();

                } else if(payload instanceof ServiceManagerDao.LatestServiceData) {
                    log.i("queried data : %s, %s", pos, ((ServiceManagerDao.LatestServiceData) payload).jsonItemName);
                    ServiceManagerDao.LatestServiceData data = (ServiceManagerDao.LatestServiceData)payload;
                    String date = BaseActivity.formatMilliseconds(format, data.dateTime);
                    String mileage = df.format(data.mileage);

                    JSONObject jsonObject = jsonArray.optJSONObject(pos);
                    final int maxMileage = jsonObject.optInt("mileage");
                    final int lastMileage = data.mileage;
                    int period = Math.min(curMileage - lastMileage, maxMileage);

                    // Partial Binding of the last service and the ProgressBar
                    holder.binding.tvLastService.setText(String.format("%s %s%s", date, mileage, unit));

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

    // Invoked in RecyclerServiceItemView
    /*
    public void setServiceData(int position, ServiceManagerDao.LatestServiceData data) {
        sparseArray.put(position, data);
        //notifyDataSetChanged();
        notifyItemChanged(position, data);
    }

     */

    static class ServiceItemViewHolder extends RecyclerView.ViewHolder {

        CardviewServiceItemBinding binding;
        ServiceItemViewHolder(CardviewServiceItemBinding binding){
            super(binding.getRoot());
            this.binding = binding;
            // Take the divider consideration into calculating the height.
            ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(itemView.getLayoutParams());
            params.setMargins(0, 0, 0, Constants.DIVIDER_HEIGHT_EXPENSE);
            itemView.setLayoutParams(params);
        }

        void animSlideUpAndDown(View target, int startValue, int endValue) {
            // Convert dp to int
            int convEndValue = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, endValue, target.getResources().getDisplayMetrics());
            int convStartValue = (int)TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, startValue, target.getResources().getDisplayMetrics());

            ViewGroup.LayoutParams params = binding.layoutDataInput.getLayoutParams();
            ValueAnimator animSlide = ValueAnimator.ofInt(convStartValue, convEndValue);
            animSlide.addUpdateListener(valueAnimator -> {
                //target.getLayoutParams().height = (Integer)valueAnimator.getAnimatedValue();
               // target.requestLayout();
                params.height = (Integer)valueAnimator.getAnimatedValue();
                binding.layoutDataInput.setLayoutParams(params);
            });

            animSlide.setDuration(500);
            animSlide.start();
        }

    }

    private synchronized void setServiceProgressBar(ServiceItemViewHolder holder, int lapse, int maxValue) {
        log.i("progressbar anim: %s", lapse);
        int progressColor = (lapse >= (maxValue * 0.8))? warningColor : safeColor;
        final ProgressBar pb = holder.binding.progbarServicePeriod;

        Drawable progressDrawable = ((LayerDrawable)pb.getProgressDrawable()).getDrawable(1);
        progressDrawable.setColorFilter(progressColor, PorterDuff.Mode.SRC_IN);

        // Animate the progress
        ProgressBarAnimation pbAnim = new ProgressBarAnimation(pb, 0, lapse);
        pbAnim.setDuration(1000);
        pb.setMax(maxValue);
        pb.startAnimation(pbAnim);

    }

    // ProgressBar Animation class referenced from StackOverflow
    static class ProgressBarAnimation extends Animation {
        private final ProgressBar progressBar;
        private final float from;
        private final float to;

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
