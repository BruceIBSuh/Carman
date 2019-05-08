package com.silverback.carman2.utils;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import com.silverback.carman2.BaseActivity;

import java.text.DecimalFormat;

public class NumberTextWatcher implements TextWatcher {

    // Constants
    //private static final String TAG = "NumberTextWatcher";


    // Objects
    private static DecimalFormat df;
    private EditText editText;

    static {
        df = BaseActivity.getDecimalFormatInstance();
    }

    // Constructor
    public NumberTextWatcher(EditText editText) {
        this.editText = editText;
    }


    @Override
    public void afterTextChanged(Editable editable) { // Editable edit = EditText.getText();

        editText.removeTextChangedListener(this);

        try {

            String text = editable.toString();

            int firstLength, lastLength;
            firstLength = text.length();

            if(text.contains(",")) text = text.replaceAll(",", "");

            Number n = df.parse(text);
            int cursor = editText.getSelectionStart();

            editText.setText(df.format(n));

            // Move the cursor at the end of number awaiting number input.

            lastLength = editText.getText().length();
            int selection = (cursor + (lastLength - firstLength));
            //Log.d(TAG, "position: " + cursor + ", " + firstLength + ", " + lastLength + ", " + selection);

            if (selection > 0 && selection <= editText.getText().length()) {
                editText.setSelection(selection);
            } else {
                // place cursor at the end when the separator exists.
                editText.setSelection(editText.getText().length());
            }

        } catch (NumberFormatException e) {
            //Log.e(TAG, "NumberFormatException: " + e.getMessage());
        } catch (Exception e) {
            //Log.d(TAG, "Exception: " + e.getMessage());
        }

        editText.addTextChangedListener(this);
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int i, int i1, int i2) {
        /*
        boolean isFractional = s.toString().contains(
                String.valueOf(df.getDecimalFormatSymbols().getDecimalSeparator()));
        */
    }
}