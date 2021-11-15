package com.silverback.carman.graph;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;

public class ExpenseConfigView extends View {

    private final LoggingHelper log = LoggingHelperFactory.create(ExpenseConfigView.class);
    private final Context context;
    private Paint paint;
    private int mViewWidth;
    private int mViewHeight;

    public ExpenseConfigView(Context context) {
        super(context);
        this.context = context;
    }

    public ExpenseConfigView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        getAttributes(context, attrs);

    }

    private void getAttributes(Context context, AttributeSet attrs) {
        init();
    }

    private void init(){
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.parseColor("#FF9900"));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawCircle((float)mViewWidth/2, (float)mViewHeight/2, 130, paint);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int minWidth = getPaddingLeft() + getPaddingRight() + getSuggestedMinimumWidth();
        mViewWidth = resolveSizeAndState(minWidth, widthMeasureSpec, 1);

        int minHeight = getPaddingTop() + getPaddingBottom() + getSuggestedMinimumHeight();
        mViewHeight = resolveSizeAndState(minHeight, heightMeasureSpec, 1);
        setMeasuredDimension(mViewWidth, mViewHeight);
    }
}
