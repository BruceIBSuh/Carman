package com.silverback.carman.utils;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;

public class RecyclerDividerUtil extends RecyclerView.ItemDecoration{

    public static LoggingHelper log = LoggingHelperFactory.create(RecyclerDividerUtil.class);

    private final float height;
    private final float padding;
    private final Paint paint;

    public RecyclerDividerUtil(float height, float padding, int color) {
        this.height= height;
        this.padding= padding;
        paint = new Paint();
        paint.setColor(color);
    }

    @Override
    public void getItemOffsets(
            @NonNull Rect outrect, @NonNull View view,
            @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {

    }

    @Override
    public void onDraw(
            @NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {

    }

    @Override
    public void onDrawOver(
            @NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {

        float left = parent.getPaddingLeft() + padding;
        float right = parent.getWidth() - parent.getPaddingEnd() - padding;

        for(int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)child.getLayoutParams();
            float top = child.getBottom();// + params.bottomMargin;
            float bottom = top + height;
            c.drawRect(left, top, right, bottom, paint);

        }
    }
}
