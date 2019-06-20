package com.silverback.carman2.viewholders;

import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
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

    // UIs
    private TextView tvItemName;
    private ConstraintLayout layout;
    private TextView tvCost;
    private CheckBox cbServiceItem;



    // Constructor
    public ServiceItemHolder(CardView view) {
        super(view);

        tvItemName = view.findViewById(R.id.tv_item_name);
        layout = view.findViewById(R.id.constraint_stmts);
        cbServiceItem = view.findViewById(R.id.chkbox);
        tvCost = view.findViewById(R.id.tv_value_cost);

        // Initialize OnCheckedChangeListener with null at first, then attach its listener
        // to retain the value as RecyclerView scrolls.
        cbServiceItem.setOnCheckedChangeListener((buttnView, isChecked) -> {
            cbServiceItem.setChecked(isChecked);
            if(isChecked) {
                layout.setVisibility(View.VISIBLE);
                animSlideUpAndDown(layout, 0, 120);

            } else {
                tvCost.setText(view.getResources().getString(R.string.value_zero));
                animSlideUpAndDown(layout, 120, 0);
            }
        });

        tvCost.setOnClickListener(v -> {

        });

    }

    public void bindItemToHolder(String item) {
        tvItemName.setText(item);
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
