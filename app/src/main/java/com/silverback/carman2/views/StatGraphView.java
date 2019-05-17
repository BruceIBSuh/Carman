package com.silverback.carman2.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.jjoe64.graphview.GraphView;
import com.silverback.carman2.BaseActivity;
import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.utils.DisplayResolutionUtils;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class StatGraphView extends View {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(StatGraphView.class);

    // Constants
    //private final int BACKGROUND_COLOR = Color.parseColor("#383838");
    private final String[] bottomTextList = new String[] {
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
    private int WIDEN_FIRST_INTERVAL; //adds more space to the first interval of the graph
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

            if (needNewFrame) {
                postDelayed(this, 5);
            }

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

        log.i("StatGraphView constructor");
    }


    // Get Attributes
    private void getAttributes(Context context, AttributeSet attrs) {

        TypedArray arrAttrs = context.obtainStyledAttributes(attrs, R.styleable.StatGraphView);
        try {
            graphAxisColor = arrAttrs.getColor(R.styleable.StatGraphView_graphAxisColor, 0);
            graphLabelColor = arrAttrs.getColor(R.styleable.StatGraphView_graphLabelColor, 0);
            //Log.d(TAG, "attrs: " + graphLabelColor + ", " + graphAxisColor);
        } finally {
            // init() cannot get started until the color attrs are obtained.
            if(graphAxisColor != 0 && graphLabelColor != 0) init();
            arrAttrs.recycle();
        }

    }

    private void init() {


        int smallTextSize = getResources().getDimensionPixelSize(R.dimen.smallText);

        GRAPH_SIDE_MARGIN = DisplayResolutionUtils.dip2px(context, 20);
        WIDEN_FIRST_INTERVAL = DisplayResolutionUtils.dip2px(context, 5);
        TEXT_TOP_MARGIN = DisplayResolutionUtils.dip2px(context, 10);

        // Set the margin, textSize and the width of graph bar
        topMargin = DisplayResolutionUtils.dip2px(context, 50);
        int textSize = DisplayResolutionUtils.sp2px(context, 13);
        barWidth = DisplayResolutionUtils.dip2px(context, 15);

        /*
        Paint bgPaint = new Paint();
        bgPaint.setAntiAlias(true);
        bgPaint.setColor(BACKGROUND_COLOR);
        */

        fgPaint = new Paint();
        fgPaint.setColor(ContextCompat.getColor(context, R.color.graphBarColor));

        // Paint object for x-y axis of the graph
        axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        axisPaint.setColor(ContextCompat.getColor(context, R.color.graphAxisColor));
        axisPaint.setStyle(Paint.Style.STROKE);
        axisPaint.setStrokeWidth(3);

        // Paint to draw the monthly total expense on top of each graph bar
        expenseNumPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        //if(graphLabelColor != 0) expenseNumPaint.setColor(graphLabelColor);
        expenseNumPaint.setColor(ContextCompat.getColor(context, R.color.graphLabelColor));
        expenseNumPaint.setTextSize(smallTextSize);
        expenseNumPaint.setTextAlign(Paint.Align.CENTER);

        // Paint to draw the graph labels
        textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setTextSize(textSize);
        //if(graphLabelColor != 0) textPaint.setColor(graphLabelColor);
        textPaint.setColor(ContextCompat.getColor(context, R.color.graphLabelColor));
        textPaint.setTextAlign(Paint.Align.CENTER);

        // Rectangle to draw the bar
        rect = new Rect();
        percentList = new ArrayList<>();

        // value to be added when calculating the bottom position value of graph bars and interval.
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
        for(String s : bottomTextList) {
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

        if(max == 0) max = 1; // prevents the possibility to be divided by zero

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

        // Draw x-axis
        canvas.drawLine(GRAPH_SIDE_MARGIN - WIDEN_FIRST_INTERVAL,   // startX
                getHeight() - bottomTextHeight - TEXT_TOP_MARGIN,   // startY
                mViewWidth - GRAPH_SIDE_MARGIN,                     // stopX
                getHeight() - bottomTextHeight - TEXT_TOP_MARGIN,   // stopY
                axisPaint);                                         // Paint
        // Draw y-axis
        canvas.drawLine(GRAPH_SIDE_MARGIN - WIDEN_FIRST_INTERVAL,
                getHeight() - bottomTextHeight - TEXT_TOP_MARGIN,
                GRAPH_SIDE_MARGIN - WIDEN_FIRST_INTERVAL,
                topMargin - 25, // topMargin currently set to 35
                axisPaint);


        // Draw the graph bar
        int i = 0;
        if(percentList != null && !percentList.isEmpty()) {

            for(Float f : percentList) {
                rect.set(GRAPH_SIDE_MARGIN + (interval * i) + interval / 2 - barWidth / 2, // Left
                        topMargin + (int)((getHeight() - topMargin - bottomTextHeight - TEXT_TOP_MARGIN - axisStroke) * percentList.get(i)), // Top
                        GRAPH_SIDE_MARGIN + (interval * i) + interval / 2 + barWidth / 2, // Right
                        getHeight() - bottomTextHeight - TEXT_TOP_MARGIN - axisStroke); // Bottom

                canvas.drawRect(rect, fgPaint);

                // Draw the number of monthly total expense on the top of the graph bar.
                if(monthlyExpense[i] > 0) {
                    canvas.drawText(df.format(monthlyExpense[i] / 1000),
                            GRAPH_SIDE_MARGIN + (interval * i) + interval / 2,
                            topMargin + (int)((getHeight() - topMargin - bottomTextHeight - TEXT_TOP_MARGIN) * percentList.get(i)) - 20,
                            expenseNumPaint);
                }

                i++;
            }
        }

        // Draw the label text rigth under the x-axis
        i = 0;
        for(String s : bottomTextList) {
            canvas.drawText(s, GRAPH_SIDE_MARGIN + (interval * i) + interval / 2,
                    getHeight() - bottomTextDescent,
                    textPaint);
            i++;
        }

        // Draw the label unit of x-axis and y-axis
        canvas.drawText(getResources().getString(R.string.graph_x_axis_month),
                mViewWidth - GRAPH_SIDE_MARGIN, getHeight() - bottomTextDescent, textPaint);

        canvas.drawText(getResources().getString(R.string.graph_y_axis_unit),
                GRAPH_SIDE_MARGIN - WIDEN_FIRST_INTERVAL, topMargin - 40, textPaint);
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        //Log.d(TAG, "MeasureSpec mode: " + MeasureSpec.getMode(widthMeasureSpec) + ", "+ MeasureSpec.getMode(heightMeasureSpec));

        mViewWidth = measureWidth(widthMeasureSpec);
        int mViewHeight = measureHeight(heightMeasureSpec);

        //Log.d(TAG, "MeasureSpec size: " + mViewWidth + ", " + mViewHeight);

        setMeasuredDimension(mViewWidth, mViewHeight);
    }
    //
    private int measureWidth(int measureSpec) {
        int preferred = 0;
        if(bottomTextList != null) {
            preferred = bottomTextList.length * interval + GRAPH_SIDE_MARGIN * 2;
            //Log.d(TAG, "Preferred Value: " + preferred);
        }

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
