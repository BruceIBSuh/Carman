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
import androidx.lifecycle.LifecycleOwner;

import com.google.firebase.firestore.FirebaseFirestore;
import com.silverback.carman.R;
import com.silverback.carman.database.CarmanDatabase;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.DisplayResolutionUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class RecentExpenseView extends View {

    public static LoggingHelper log = LoggingHelperFactory.create(RecentExpenseView.class);

    private static final float ACCELERATOR = 0.02f;

    private final Context context;

    private CarmanDatabase mDB;
    private Calendar calendar;
    private SimpleDateFormat sdf;

    private Paint fgPaint;
    private Paint textPaint;
    private Paint dividerPaint;

    private ArrayList<Float> targetPercentList;
    private ArrayList<Float> percentList;



    private int mViewWidth;
    private int mViewHeight;
    private int barWidth;
    private int topMargin;

    private int maxExpense;

    private Paint[] arrPaint = new Paint[3];
    private final int[] arrBarColor = new int[3];
    private final int[] arrExpense = new int[3];
    private final String[] arrMonthName = new String[3];

    private final Runnable animator = new Runnable() {
        @Override
        public void run() {
            boolean needNewFrame = false;
            for(int i = 0; i < targetPercentList.size(); i++) {
                if (percentList.get(i) < targetPercentList.get(i)) {
                    percentList.set(i, percentList.get(i) + ACCELERATOR);
                    needNewFrame = true;

                } else if (percentList.get(i) > targetPercentList.get(i)){
                    percentList.set(i, percentList.get(i) - ACCELERATOR);
                    needNewFrame = true;
                }

                if(Math.abs(targetPercentList.get(i) - percentList.get(i)) < ACCELERATOR){
                    percentList.set(i, targetPercentList.get(i));
                }

            }

            if(needNewFrame) postDelayed(this, 3);
            invalidate();
        }
    };


    // Constructor
    public RecentExpenseView(Context context) {
        super(context);
        this.context = context;
        init();
    }

    public RecentExpenseView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        getAttributes(context, attrs);
        init();
    }

    private void getAttributes(Context context, AttributeSet attrs) {
        TypedArray typedArray =
                context.getTheme().obtainStyledAttributes(attrs, R.styleable.RecentExpenseView, 0, 0);
        try {
            arrBarColor[0] = typedArray.getColor(R.styleable.RecentExpenseView_graphBarColor1, 0);
            arrBarColor[1] = typedArray.getColor(R.styleable.RecentExpenseView_graphBarColor2, 0);
            arrBarColor[2] = typedArray.getColor(R.styleable.RecentExpenseView_graphBarColor3, 0);
        } finally {
            typedArray.recycle();
        }

    }

    private void init() {
        mDB = CarmanDatabase.getDatabaseInstance(context);
        calendar = Calendar.getInstance();
        sdf = new SimpleDateFormat("MMM", Locale.ENGLISH);

        percentList = new ArrayList<>();
        Rect rect = new Rect();
        arrPaint = new Paint[3];
        for(int i = 0; i < 3; i++) {
            arrPaint[i] = new Paint(Paint.ANTI_ALIAS_FLAG);
            arrPaint[i].setColor(arrBarColor[i]);
        }

        // Set the graph bar width
        barWidth = DisplayResolutionUtils.dip2px(context, 10);
        topMargin = DisplayResolutionUtils.dip2px(context, 15);
        int textSize = DisplayResolutionUtils.dip2px(context, 11);

        // Set the background color of the graph
        Paint bgPaint = new Paint();
        bgPaint.setAntiAlias(true);
        bgPaint.setColor(Color.BLUE);

        // Foreground color of the graph
        fgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fgPaint.setColor(ContextCompat.getColor(context, android.R.color.white));

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(ContextCompat.getColor(context, android.R.color.white));
        textPaint.setTextSize(textSize);

        dividerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dividerPaint.setColor(ContextCompat.getColor(context, android.R.color.white));
    }

    //public void setExpenseData(ArrayList<Integer> expList) {
    public void setExpenseData(int expense, LifecycleOwner lifecycleOwner) {
        arrExpense[2] = expense;
        maxExpense = arrExpense[2];
        arrMonthName[2] = sdf.format(calendar.getTime());

        for(int i = 1; i >= 0; i--) {
            calendar.add(Calendar.MONTH, -1);
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            final long start = calendar.getTimeInMillis();
            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
            final long end = calendar.getTimeInMillis();

            arrMonthName[i] = sdf.format(calendar.getTime());
            queryMonthlyExpense(i, start, end, lifecycleOwner);
        }

    }

   private void queryMonthlyExpense(final int index, long start, long end, LifecycleOwner lifecycleOwner) {
       synchronized (this) {
           mDB.expenseBaseModel().loadTotalExpenseByMonth(start, end).observe(lifecycleOwner, expenses -> {
               int totalExpense = 0;
               for(Integer value : expenses) totalExpense += value;
               arrExpense[index] = totalExpense;
               if(maxExpense < totalExpense) maxExpense = totalExpense;
               if(index == 0) setGraphBarHeight(arrExpense, maxExpense);
           });
       }

   }

    private void setGraphBarHeight(int[] expenses, int max) {
        targetPercentList = new ArrayList<>();
        if(max == 0) max = 1;//prevents the possibility to be divided by zero

        for(Integer value : expenses) {
            targetPercentList.add(1f - (float)value / (float)max);
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

        removeCallbacks(animator);
        post(animator);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        this.setBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_blue_dark));

        float barOffset = (float)mViewWidth / 3;
        // Draw the column divider
        canvas.drawRect(barOffset - 1, 0, barOffset + 1, mViewHeight, dividerPaint);
        canvas.drawRect(barOffset * 2 - 1, 0, barOffset * 2 + 1, mViewHeight, dividerPaint);
        // Draw each graph bar for last 3 months
        if(percentList != null && percentList.size() > 0) {
            for(int i = 0; i < 3; i++) {
                final float offset = (barOffset * i) + barOffset / 2;
                final int top = topMargin + (int)((mViewHeight - topMargin) * percentList.get(i));
                canvas.drawRect(offset - barWidth, top, offset + barWidth, mViewHeight, arrPaint[i]);
                canvas.drawText(arrMonthName[i], offset - 25, 30, textPaint);
            }
        }

    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int minWidth = getPaddingLeft() + getPaddingRight() + getSuggestedMinimumWidth();
        mViewWidth = resolveSizeAndState(minWidth, widthMeasureSpec, 1);

        int minHeight = getPaddingTop() + getPaddingBottom() + getSuggestedMinimumHeight();
        mViewHeight = resolveSizeAndState(minHeight, heightMeasureSpec, 1);

        setMeasuredDimension(mViewWidth, mViewHeight);
    }


}
