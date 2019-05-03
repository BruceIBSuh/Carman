package com.silverback.carman2.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

public class InputBtnPadView extends ConstraintLayout implements View.OnClickListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(InputBtnPadView.class);

    // UIs
    private Button btn1, btn2, btn3, btn4;

    // Constructor
    public InputBtnPadView(Context context) {
        super(context);
        getAttribute(context, null);
    }

    public InputBtnPadView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getAttribute(context, attrs);
    }

    public InputBtnPadView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        getAttribute(context, attrs);
    }

    private void getAttribute(Context context, AttributeSet attrs) {

        LayoutInflater.from(context).inflate(R.layout.view_button_pad, this, true);
        btn1 = findViewById(R.id.padButton1);
        btn2 = findViewById(R.id.padButton2);
        btn3 = findViewById(R.id.padButton3);
        btn4 = findViewById(R.id.padButton4);

        btn1.setOnClickListener(this);
        btn2.setOnClickListener(this);
        btn3.setOnClickListener(this);
        btn4.setOnClickListener(this);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.InputBtnPadView);
        try {
            String btnText1 = (String)typedArray.getText(R.styleable.InputBtnPadView_btn1);
            String btnText2 = (String)typedArray.getText(R.styleable.InputBtnPadView_btn2);
            String btnText3 = (String)typedArray.getText(R.styleable.InputBtnPadView_btn3);
            String btnText4 = (String)typedArray.getText(R.styleable.InputBtnPadView_btn4);

            btn1.setText(btnText1);
            btn2.setText(btnText2);
            btn3.setText(btnText3);
            btn4.setText(btnText4);

        } finally {
            typedArray.recycle();
        }

    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.padButton1:
                log.i("btn1: %s", btn1.getText().toString());

                break;
            case R.id.padButton2:
                log.i("btn2");
                break;
            case R.id.padButton3:
                log.i("btn3");
                break;
            case R.id.padButton4:
                log.i("btn4");
                break;
        }
    }
}
