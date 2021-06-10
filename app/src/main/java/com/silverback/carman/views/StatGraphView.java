package com.silverback.carman.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.silverback.carman.BaseActivity;
import com.silverback.carman.R;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.DisplayResolutionUtils;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class StatGraphView extends View {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(StatGraphView.class);

    // Constants
    private final String[] monthLabel = new String[] {
            "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12" };

    private static DecimalFormat df = BaseActivity.getDecimalFormatInstance();

    private Context context;

    private ArrayList<Float> percentList;
    private ArrayList<Float> targetPercentList;
    private int[] monthlyExpense;

    private Paint textPaint;
    private Paint fgPaint;
    private Paint axisPaint, expenseNumPaint;
    private Rect rect;
    private int topMargin;
    private int barWidth;

    private int mViewWidth;
    private int interval; // interval width with labels
    private int axisStroke;

    private int bottomTextDescent;
    private int bottomTextHeight;

    private int GRAPH_SIDE_MARGIN; //set the margins of the graph axis and the parent view
    private int WIDEN_FIRST_INTERVAL; //add more space to the first interval of the graph
    private int TEXT_TOP_MARGIN;

    private int graphAxisColor, graphLabelColor;

    private Runnable animator = new Runnable() {
        @Override
        public void run() {
            boolean needNewFrame = false;
            for(int i = 0; i < targetPercentList.size(); i++) {
                if (percentList.get(i) < targetPercentList.get(i)) {
                    percentList.set(i, percentList.get(i) + 0.02f);
                    needNewFrame = true;

                } else if (percentList.get(i) > targetPercentList.get(i)){
                    percentList.set(i, percentList.get(i) - 0.02f);
                    needNewFrame = true;
                }

                if(Math.abs(targetPercentList.get(i) - percentList.get(i)) < 0.02f){
                    percentList.set(i, targetPercentList.get(i));
                }

            }

            if (needNewFrame) postDelayed(this, 5);
            invalidate();
        }
    };

    // Constructor
    public StatGraphView(Context context) {
        super(context);
        this.context = context;
        init();
    }
    public StatGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        getAttributes(context, attrs);
        init();
    }

    // Get Attributes
    private void getAttributes(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.StatGraphView);
        try {
            graphAxisColor = typedArray.getColor(R.styleable.StatGraphView_graphAxisColor, 0);
            graphLabelColor = typedArray.getColor(R.styleable.StatGraphView_graphLabelColor, 0);
        } finally {
            // init() cannot get started until the color attrs are obtained.
            if(graphAxisColor != 0 && graphLabelColor != 0) init();
            typedArray.recycle();
        }

    }

    private void init() {
        int textSize = getResources().getDimensionPixelSize(R.dimen.extraSmallText);
        GRAPH_SIDE_MARGIN = DisplayResolutionUtils.dip2px(context, 15);
        WIDEN_FIRST_INTERVAL = DisplayResolutionUtils.dip2px(context, 5);
        TEXT_TOP_MARGIN = DisplayResolutionUtils.dip2px(context, 5);

        // Set the top margin and the bar width of the graph
        topMargin = DisplayResolutionUtils.dip2px(context, 30);
        barWidth = DisplayResolutionUtils.dip2px(context, 15);

        // Background color of the graph
        /*
        Paint bgPaint = new Paint();
        bgPaint.setAntiAlias(true);
        bgPaint.setColor(BACKGROUND_COLOR);
        */

        // Foreground color of the graph
        fgPaint = new Paint();
        fgPaint.setColor(ContextCompat.getColor(context, R.color.graphBarColor));

        // Paint object for x-y axis of the graph
        axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        //axisPaint.setColor(graphAxisColor);
        axisPaint.setColor(ContextCompat.getColor(context, R.color.graphAxisColor));
        axisPaint.setStyle(Paint.Style.STROKE);
        axisPaint.setStrokeWidth(3);

        // Paint for drawing the monthly total expense on top of each graph bar
        expenseNumPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        //expenseNumPaint.setColor(graphLabelColor);
        expenseNumPaint.setColor(ContextCompat.getColor(context, R.color.graphLabelColor));
        expenseNumPaint.setTextSize(textSize);
        expenseNumPaint.setTextAlign(Paint.Align.CENTER);

        // Paint to draw the labels of month below the x_axis of the graph.
        textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setTextSize(textSize);
        //textPaint.setColor(graphLabelColor);
        textPaint.setColor(ContextCompat.getColor(context, R.color.graphLabelColor));
        textPaint.setTextAlign(Paint.Align.CENTER);

        // Rectangle to draw the bar
        rect = new Rect();
        percentList = new ArrayList<>();

        // value to be added when calculating the bottom position value of graph bars and the interval
        axisStroke = (int)axisPaint.getStrokeWidth();
    }


    // Set the graph data and the label text, which is invoked from onPostExecute() of BarGraphTask
    // in StatisticsActivity.
    public void setGraphData(int[] arrData){
        // Get the max valaue out of the monthly total expense in a year, then pass it to setDataList()
        // for setting the relative height of each bars based on the max value which is set to the hightest.
        // Official idiomatic formula.
        int maxExpense = arrData[0];
        for(int i = 1; i < arrData.length; i++) if(arrData[i] > maxExpense) maxExpense = arrData[i];
        setDataList(arrData, maxExpense);

        // Set the graph labels on the bottom of x-axis.
        setBottomTextList();
    }

    // Set the size of label text based on the max value of text widths and heights
    public void setBottomTextList() {
        Rect r = new Rect(); // Instance of rectangle containing Text
        //boolean autoSetWidth = true;

        // Set the label width to the max text width. Here, it's not currently necessary to loop
        // to find the max width because each label has the same width, but remain not refactored
        // just for update in the future.
        for(String s : monthLabel) {
            textPaint.getTextBounds(s, 0, s.length(), r);
            if(bottomTextHeight < r.height()) {
                bottomTextHeight = r.height();
            }
            if(barWidth < r.width()) {
                barWidth = r.width();
            }
            if(bottomTextDescent < (Math.abs(r.bottom))) {
                bottomTextDescent = Math.abs(r.bottom);
            }
        }
        setMinimumWidth(2);
        postInvalidate(); // call Draw() on non-UI thread
    }

    // Refactor required because both of percentList and targetPercentList have the same size and
    // doesn't need to loop to make both sizes checked.
    public void setDataList(int[] expense, int max) {
        targetPercentList = new ArrayList<>();
        monthlyExpense = expense;

        if(max == 0) max = 1; //prevents the possibility to be divided by zero

        for(Integer integer : expense) {
            targetPercentList.add(1 - (float)integer / (float)max);
        }

        // Make the ArrayList size equal to be sure percetList.size() == targetPerentList.size()
        if(percentList.isEmpty() || percentList.size() < targetPercentList.size()) {
            int temp = targetPercentList.size() - percentList.size();
            for(int i = 0; i < temp; i++) {
                percentList.add(1f);
            }
        } else if(percentList.size() > targetPercentList.size()) {
            int temp = percentList.size() - targetPercentList.size();
            for(int i = 0; i < temp; i++) {
                percentList.remove(percentList.size() - 1);
            }
        }

        setMinimumWidth(2);
        removeCallbacks(animator);
        post(animator);

    }

    @Override
    protected void onDraw(Canvas canvas) {
        // Interval width of X-axis calculated by ViewWidth minus graph margin of both sides, and
        // minus y-axis width, then divided by 12 months
        interval = (mViewWidth - GRAPH_SIDE_MARGIN * 2 - axisStroke) / 12;
        // Draw x_axis
        canvas.drawLine(GRAPH_SIDE_MARGIN - WIDEN_FIRST_INTERVAL, // startX
                getHeight() - bottomTextHeight - TEXT_TOP_MARGIN, // startY
                mViewWidth - GRAPH_SIDE_MARGIN,                   // endX
                getHeight() - bottomTextHeight - TEXT_TOP_MARGIN, // endY
                axisPaint);                                       // Paint
        // Draw y_axis
        canvas.drawLine(GRAPH_SIDE_MARGIN - WIDEN_FIRST_INTERVAL,
                getHeight() - bottomTextHeight - TEXT_TOP_MARGIN,
                GRAPH_SIDE_MARGIN - WIDEN_FIRST_INTERVAL,
                topMargin - 25, // topMargin currently set to 35
                axisPaint);


        // Draw the graph bar
        int index = 0;
        if(percentList != null && !percentList.isEmpty()) {

            for(Float f : percentList) {
                rect.set(GRAPH_SIDE_MARGIN + (interval * index) + interval / 2 - barWidth / 2, // Left
                        topMargin + (int)((getHeight() - topMargin - bottomTextHeight - TEXT_TOP_MARGIN - axisStroke) * percentList.get(index)), // Top
                        GRAPH_SIDE_MARGIN + (interval * index) + interval / 2 + barWidth / 2, // Right
                        getHeight() - bottomTextHeight - TEXT_TOP_MARGIN - axisStroke); // Bottom

                canvas.drawRect(rect, fgPaint);

                // Draw the number of monthly total expense on the top of the graph bar.
                if(monthlyExpense[index] > 0) {
                    canvas.drawText(df.format(monthlyExpense[index] / 1000),
                            GRAPH_SIDE_MARGIN + (interval * index) + interval / 2,
                            topMargin + (int)((getHeight() - topMargin - bottomTextHeight - TEXT_TOP_MARGIN) * percentList.get(index)) - 5,
                            expenseNumPaint);
                }

                index++;
            }
        }

        // Draw the label text rigth under the x-axis
        index = 0;
        for(String s : monthLabel) {
            canvas.drawText(s, GRAPH_SIDE_MARGIN + (interval * index) + interval / 2,
                    getHeight() - bottomTextDescent,
                    textPaint);
            index++;
        }

        // Draw the label unit of the x_axis and y_axis
        canvas.drawText(getResources().getString(R.string.graph_x_axis_month), mViewWidth - GRAPH_SIDE_MARGIN - 10, getHeight() - bottomTextDescent, textPaint);
        canvas.drawText(getResources().getString(R.string.graph_y_axis_unit), GRAPH_SIDE_MARGIN - WIDEN_FIRST_INTERVAL, topMargin - 45, textPaint);
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mViewWidth = measureWidth(widthMeasureSpec);
        // TEST CODE : add 100
        int mViewHeight = measureHeight(heightMeasureSpec);

        setMeasuredDimension(mViewWidth, mViewHeight);
    }


    private int measureWidth(int measureSpec) {
        int preferred = 0;
        //if(monthLabel != null) {
            preferred = monthLabel.length * interval + GRAPH_SIDE_MARGIN * 2;
            //Log.d(TAG, "Preferred Value: " + preferred);
        //}

        return getMeasurement(measureSpec, preferred);
    }

    private int measureHeight(int measureSpec){
        int preferred = 300;
        return getMeasurement(measureSpec, preferred);
    }

    private int getMeasurement(int measureSpec, int preferred){

        int specSize = MeasureSpec.getSize(measureSpec);
        int measurement;

        switch(MeasureSpec.getMode(measureSpec)){
            case MeasureSpec.EXACTLY:
                measurement = specSize;
                //Log.d(TAG, "Mode: EXACTLY");
                break;
            case MeasureSpec.AT_MOST:
                measurement = Math.min(preferred, specSize);
                //Log.d(TAG, "Mode: AT_MOST");
                break;
            case MeasureSpec.UNSPECIFIED:
                measurement = specSize;
                //Log.d(TAG, "Mode: UNSPECIFIED");
                break;
            default:
                measurement = preferred;
                break;
        }

        return measurement;
    }

}
