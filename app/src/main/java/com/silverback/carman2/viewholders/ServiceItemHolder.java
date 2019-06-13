package com.silverback.carman2.viewholders;

import android.animation.ObjectAnimator;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

public class ServiceItemHolder extends RecyclerView.ViewHolder implements CompoundButton.OnCheckedChangeListener{

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(ServiceItemHolder.class);

    // Objects
    private TextView tvItemName;
    private ConstraintLayout layout;
    private Animation slideUp, slideDown;

    // Temp
    private ObjectAnimator animation;


    // Constructor
    public ServiceItemHolder(CardView view) {
        super(view);

        tvItemName = view.findViewById(R.id.tv_name);
        layout = view.findViewById(R.id.constraint_stmts);
        CheckBox chkbox = view.findViewById(R.id.chkbox);



        chkbox.setOnCheckedChangeListener(this);

        slideUp = AnimationUtils.loadAnimation(view.getContext(), R.anim.slide_up);
        slideDown = AnimationUtils.loadAnimation(view.getContext(), R.anim.slide_down);



    }

    public void bindItemToHolder(String item) {
        tvItemName.setText(item);
    }


    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

        if(isChecked) {
            layout.setVisibility(View.VISIBLE);
            //layout.setAnimation(slideUp);
            // Temp

        } else {
            layout.setVisibility(View.GONE);
            layout.setAnimation(slideUp);
        }

    }
}
