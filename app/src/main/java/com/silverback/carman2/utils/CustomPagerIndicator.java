package com.silverback.carman2.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class CustomPagerIndicator extends LinearLayout {

    private Context mContext;
    private int itemMargin = 10;
    private int animDuration = 250;

    private int mDefaultCircle;
    private int mSelectCircle;

    private ImageView[] imageDot;

    // Property setters of anim, margin.
    /*
    public void setAnimDuration(int animDuration) {
        this.animDuration = animDuration;
    }
    */

    /*
    public void setItemMargin(int itemMargin) {
        this.itemMargin = itemMargin;
    }
    */

    public CustomPagerIndicator(Context context) {
        super(context);
        mContext = context;
    }


    public CustomPagerIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;

        //When using xml for creating layout,
        /*
        LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.pager_indicator, this);
        */
    }

    public void createPanel(int count, int defaultCircle, int selectCircle) {

        mDefaultCircle = defaultCircle;
        mSelectCircle = selectCircle;
        imageDot = new ImageView[count];

        for (int i = 0; i < count; i++) {

            imageDot[i] = new ImageView(mContext);

            LayoutParams params = new LayoutParams(
                    LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            params.topMargin = itemMargin;
            params.bottomMargin = itemMargin;
            params.leftMargin = itemMargin;
            params.rightMargin = itemMargin;
            params.gravity = Gravity.CENTER;

            imageDot[i].setLayoutParams(params);
            imageDot[i].setImageResource(defaultCircle);
            imageDot[i].setTag(imageDot[i].getId(), false);
            this.addView(imageDot[i]);
        }

        selectDot(0);

    }

    public void selectDot(int position) {

        for(int i = 0; i < imageDot.length; i++) {
            if(i == position) {
                imageDot[i].setImageResource(mSelectCircle);
                selectScaleAnim(imageDot[i], 1f, 1.5f);
            } else {
                if((boolean)imageDot[i].getTag(imageDot[i].getId())) {
                    imageDot[i].setImageResource(mDefaultCircle);
                    defaultScaleAnim(imageDot[i], 1.5f, 1f);
                }
            }
        }
    }

    // Animation
    private void selectScaleAnim(View view, float startScale, float endScale) {
        Animation anim = new ScaleAnimation(
                startScale, endScale,
                startScale, endScale,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        anim.setFillAfter(true);
        anim.setDuration(animDuration);
        view.startAnimation(anim);
        view.setTag(view.getId(), true);
    }

    private void defaultScaleAnim(View view, float startScale, float endScale) {
        Animation anim = new ScaleAnimation(
                startScale, endScale,
                startScale, endScale,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        anim.setFillAfter(true);
        anim.setDuration(animDuration);
        view.startAnimation(anim);
        view.setTag(view.getId(), false);
    }
}
