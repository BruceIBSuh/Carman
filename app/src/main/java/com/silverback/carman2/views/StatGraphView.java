package com.silverback.carman2.views;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.silverback.carman2.R;

public class StatGraphView extends ConstraintLayout {


    public StatGraphView(Context context) {
        super(context);

        LayoutInflater.from(context).inflate(R.layout.view_stat_graph, this, true);

    }


}
