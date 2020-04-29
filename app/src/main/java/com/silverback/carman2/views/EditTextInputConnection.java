package com.silverback.carman2.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;

import androidx.appcompat.widget.AppCompatEditText;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

public class EditTextInputConnection extends AppCompatEditText {

    private static final LoggingHelper log = LoggingHelperFactory.create(EditTextInputConnection.class);

    public EditTextInputConnection(Context context) {
        super(context);
    }

    public EditTextInputConnection(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EditTextInputConnection(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        return new CustomInputConnection(super.onCreateInputConnection(outAttrs), true);
    }

    private class CustomInputConnection extends InputConnectionWrapper {

        CustomInputConnection(InputConnection target, boolean mutable) {
            super(target, mutable);
        }

        @Override
        public boolean sendKeyEvent(KeyEvent event) {
            log.i("sendkeyevent: %s", event);
            return super.sendKeyEvent(event);
        }

        @Override
        public boolean deleteSurroundingText(int beforeLength, int afterLength) {
            log.i("deletesurroundingtext: %s, %s", beforeLength, afterLength);
            return super.deleteSurroundingText(beforeLength, afterLength);
        }

        @Override
        public CharSequence getSelectedText(int flag) {
            log.i("getSelectedText: %s", flag);
            return super.getSelectedText(flag);
        }
    }


}
