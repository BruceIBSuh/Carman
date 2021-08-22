package com.silverback.carman.graph;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.silverback.carman.R;
import com.silverback.carman.utils.DisplayResolutionUtils;

import java.util.ArrayList;

public class RecentMontlyExpenses extends View {

    private Context context;

    private Rect rect;
    private Paint fgPaint;

    private ArrayList<Float> target;
    private ArrayList<Float> percentList;
    private int graphBarWidth;


    // Constructor
    public RecentMontlyExpenses(Context context) {
        super(context);
        this.context = context;
        init();
    }

    public RecentMontlyExpenses(Context context, AttributeSet attrs) {
        super(context);
        this.context = context;
        getAttributes(context, attrs);
        init();
    }

    private void getAttributes(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.RecentMontlyExpenses);
        try {
            //graphAxisColor = typedArray.getColor(R.styleable.StatGraphView_graphAxisColor, 0);
            //graphLabelColor = typedArray.getColor(R.styleable.StatGraphView_graphLabelColor, 0);
        } finally {
            // init() cannot get started until the color attrs are obtained.
            //if (graphAxisColor != 0 && graphLabelColor != 0) init();

            typedArray.recycle();
        }

    }

    private void init() {
        // Set the graph bar width
        graphBarWidth = DisplayResolutionUtils.dip2px(context, 30);

        // Set the background color of the graph
        Paint bgPaint = new Paint();
        bgPaint.setAntiAlias(true);
        bgPaint.setColor(Color.BLUE);

        // Foreground color of the graph
        fgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fgPaint.setColor(ContextCompat.getColor(context, R.color.graphBarColor));
    }

    public void setGraphData(int[] arrExpense) {
        int maxExpense = arrExpense[0];
        for(int expense : arrExpense) if(expense > maxExpense) maxExpense = expense;
        setDataList(arrExpense, maxExpense);
    }

    private void setDataList(int[] expenses, int max) {
        if(max == 0) max = 1;
        target = new ArrayList<>();
        for(Integer integer : expenses) target.add(1 - (float)integer / (float)max);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

    }
}
