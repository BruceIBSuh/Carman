package com.silverback.carman2.viewholders;

import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

public class ServiceItemHolder extends RecyclerView.ViewHolder {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(ServiceItemHolder.class);

    // Objects
    private Context context;

    // UIs
    private TextView tvItemName, tvItemCost;
    private ConstraintLayout layout;
    private CheckBox cbServiceItem;


    // Constructor
    public ServiceItemHolder(CardView view) {
        super(view);
        this.context = view.getContext();

        layout = view.findViewById(R.id.constraint_stmts);
        tvItemName = view.findViewById(R.id.tv_item_name);
        tvItemCost = view.findViewById(R.id.tv_value_cost);
        cbServiceItem = view.findViewById(R.id.chkbox);

        // Initialize OnCheckedChangeListener with null at first, then attach its listener
        // to retain the value as RecyclerView scrolls.
        /*
        cbServiceItem.setOnCheckedChangeListener((buttnView, isChecked) -> {
            cbServiceItem.setChecked(isChecked);
            if(isChecked) {
                layout.setVisibility(View.VISIBLE);
                animSlideUpAndDown(layout, 0, 120);
                chkboxStateArray.put(position, true);

            } else {
                tvCost.setText(view.getResources().getString(R.string.value_zero));
                animSlideUpAndDown(layout, 120, 0);
                chkboxStateArray.put(position, false);
            }
        });
        */
    }

    public void bindItemToHolder(String item, boolean isChecked) {
        tvItemName.setText(item);
        cbServiceItem.setChecked(isChecked);

    }

    public void doCheckBoxAction(boolean isChecked) {
        cbServiceItem.setChecked(isChecked);
        if(isChecked) {
            layout.setVisibility(View.VISIBLE);
            animSlideUpAndDown(layout, 0, 120);

        } else {
            tvItemCost.setText(context.getResources().getString(R.string.value_zero));
            animSlideUpAndDown(layout, 120, 0);
        }
    }


    private void animSlideUpAndDown(View target, int startValue, int endValue) {
        // Convert dp to int
        int convEndValue = (int)TypedValue.applyDimension(
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
