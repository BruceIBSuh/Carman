package com.silverback.carman.graph;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

public class ExpenseConfigView extends View {

    private final Context context;

    public ExpenseConfigView(Context context) {
        super(context);
        this.context = context;
    }

    public ExpenseConfigView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        getAttributes(context, attrs);
        init();
    }

    private void getAttributes(Context context, AttributeSet attrs) {}

    private void init(){}
}
