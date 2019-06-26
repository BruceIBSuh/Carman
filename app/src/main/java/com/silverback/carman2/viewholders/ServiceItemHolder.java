package com.silverback.carman2.viewholders;

import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.silverback.carman2.R;
import com.silverback.carman2.adapters.ServiceItemListAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.ServiceCheckListModel;

public class ServiceItemHolder extends RecyclerView.ViewHolder implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(ServiceItemHolder.class);

    // Objects
    private ServiceCheckListModel checkListModel;
    private SparseBooleanArray arrChkbox;
    private ServiceItemListAdapter.OnNumPadListener mListener;
    private SparseBooleanArray arrCheckBox;
    private Context context;

    // UIs
    private TextView tvItemName, tvItemCost;
    private ConstraintLayout layout;
    private CheckBox cbServiceItem;


    // Constructor
    public ServiceItemHolder(CardView view, ServiceCheckListModel model, ServiceItemListAdapter.OnNumPadListener listener) {
        super(view);

        log.i("ServiceItemHolder constructor");

        this.context = view.getContext();
        checkListModel = model;
        mListener = listener;
        arrCheckBox = new SparseBooleanArray();

        layout = view.findViewById(R.id.constraint_stmts);
        tvItemName = view.findViewById(R.id.tv_item_name);
        tvItemCost = view.findViewById(R.id.tv_value_cost);
        cbServiceItem = view.findViewById(R.id.chkbox);

        tvItemCost.setOnClickListener(this);
        cbServiceItem.setOnCheckedChangeListener(this);


    }

    @Override
    public void onClick(View v) {
        mListener.inputItemCost(tvItemName.getText().toString(), tvItemCost, getAdapterPosition());
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if(isChecked) {
            arrCheckBox.put(getAdapterPosition(), true);
            layout.setVisibility(View.VISIBLE);
            animSlideUpAndDown(layout, 0, 120);
        } else {
            cbServiceItem.setChecked(false);
            arrCheckBox.put(getAdapterPosition(), false);
            tvItemCost.setText(context.getResources().getString(R.string.value_zero));
            animSlideUpAndDown(layout, 120, 0);
        }

        checkListModel.getChkboxState().setValue(arrCheckBox);

    }

    public void bindItemToHolder(String item, boolean isChecked) {
        log.i("Item Checked: %s, %s", item, isChecked);
        tvItemName.setText(item);
        cbServiceItem.setChecked(isChecked);

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
