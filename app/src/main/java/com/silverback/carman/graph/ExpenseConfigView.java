package com.silverback.carman.graph;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

import com.silverback.carman.R;
import com.silverback.carman.databinding.MainContentPagerConfigBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;

public class ExpenseConfigView extends View {

    private final LoggingHelper log = LoggingHelperFactory.create(ExpenseConfigView.class);

    private final Context context;
    private MainContentPagerConfigBinding binding;
    private final int numSector = 4;

    private RectF rectF;
    private Paint bgPaint;
    private Paint gasPaint, svcPaint, washPaint, miscPaint;
    private final int[] arrArcColor = new int[numSector];
    private final int[] arrIndexBg = new int[numSector];

    private int gasExp, svcExp, washExp, miscExp;
    private int mViewWidth;
    private int mViewHeight;

    private float gasAngle, svcAngle, washAngle, miscAngle;

    //private List<Integer> expList;
    private final Runnable animator = new Runnable() {
        @Override
        public void run() {
            boolean needNewFrame = false;
            if(needNewFrame) postDelayed(this, 3);
            invalidate();
        }
    };

    public ExpenseConfigView(Context context) {
        super(context);
        this.context = context;
        init();
    }

    public ExpenseConfigView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        getAttributes(context, attrs);
        init();
    }

    private void getAttributes(Context context, AttributeSet attrs) {
        //binding = MainContentPagerConfigBinding.inflate(LayoutInflater.from(context));
        TypedArray ta = context.getTheme().obtainStyledAttributes(attrs, R.styleable.ExpenseConfigView, 0, 0);
        try {
            arrArcColor[0] = ta.getColor(R.styleable.ExpenseConfigView_gasColor, 0);
            arrArcColor[1] = ta.getColor(R.styleable.ExpenseConfigView_svcColor, 0);
            arrArcColor[2] = ta.getColor(R.styleable.ExpenseConfigView_washColor, 0);
            arrArcColor[3] = ta.getColor(R.styleable.ExpenseConfigView_miscColor, 0);
        } finally {
            ta.recycle();
        }
    }

    private void init(){
        rectF = new RectF();
        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(Color.parseColor("#E0E0E0"));

        gasPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        svcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        washPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        miscPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        gasPaint.setColor(arrArcColor[0]);
        svcPaint.setColor(arrArcColor[1]);
        washPaint.setColor(arrArcColor[2]);
        miscPaint.setColor(arrArcColor[3]);

//        binding.indexGas.setBackgroundColor(arrArcColor[0]);
//        binding.indexSvc.setBackgroundColor(arrArcColor[1]);
//        binding.indexWash.setBackgroundColor(arrArcColor[2]);
//        binding.indexMisc.setBackgroundColor(arrArcColor[3]);

    }

    public void setExpenseConfigData(int... data) {
        int totalSum = 0;
        for(Integer expense : data) totalSum += expense;
        gasExp = data[0];
        svcExp = data[1];
        washExp = data[2];
        miscExp = data[3];
        //float gasAngle = Math.round(((gasExp / totalSum) * 100) / 100.0) * 360;
        gasAngle = Math.round(((float)gasExp / totalSum) * 360);
        svcAngle = Math.round(((float)svcExp / totalSum) * 360);
        washAngle = Math.round(((float)washExp / totalSum) * 360);
        miscAngle = Math.round(((float)miscExp / totalSum) * 360);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        final float radius = (float)mViewHeight/2;
        final float left = (float)mViewWidth / 2 - radius;
        rectF.set(left, 0, left + radius * 2, mViewHeight);
        canvas.drawCircle((float)mViewWidth/2, (float)mViewHeight/2, radius, bgPaint);

        log.i("ancle:%s, %s, %s, %s", gasAngle, svcAngle, washAngle, miscAngle);
        canvas.drawArc(rectF, 0, gasAngle, true, gasPaint);
        canvas.drawArc(rectF, gasAngle, svcAngle, true, svcPaint);
        canvas.drawArc(rectF, gasAngle + svcAngle, washAngle, true, washPaint);
        canvas.drawArc(rectF, gasAngle + svcAngle + washAngle, miscAngle, true, miscPaint);

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
