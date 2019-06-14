package com.silverback.carman2.viewholders;

import android.animation.ObjectAnimator;
import android.util.DisplayMetrics;
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

    // Temp
    private ObjectAnimator animation;


    // Constructor
    public ServiceItemHolder(CardView view) {
        super(view);

        tvItemName = view.findViewById(R.id.tv_name);
        layout = view.findViewById(R.id.constraint_stmts);
        CheckBox chkbox = view.findViewById(R.id.chkbox);

        // Temp Coding to get the size of the layout.
        /*
        DisplayMetrics dispMetrics = new DisplayMetrics();
        layout.getDisplay().getMetrics(dispMetrics);
        log.i("ConstraintLayout size: %s, %s", dispMetrics.widthPixels, dispMetrics.heightPixels);
        */

        chkbox.setOnCheckedChangeListener(this);
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
            /*
            ObjectAnimator animDown = ObjectAnimator.ofFloat(layout, "translationY", 50f);
            animDown.setDuration(2000);
            animDown.start();
            */

        } else {
            //layout.setVisibility(View.GONE);
            //layout.setAnimation(slideUp);
            /*
            ObjectAnimator animUp = ObjectAnimator.ofFloat(layout, "translationY", -250f);
            animUp.setDuration(1000);
            animUp.start();
            */
            layout.setVisibility(View.GONE);
        }

    }
}
