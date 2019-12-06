package com.silverback.carman2.utils;

import android.app.Notification;
import android.content.IntentFilter;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Selection;
import android.text.SpanWatcher;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ImageSpan;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

public class BoardImageSpanHandler implements SpanWatcher {

    private static final LoggingHelper log = LoggingHelperFactory.create(BoardImageSpanHandler.class);

    private static final int SPAN_FLAG;
    private static int imageTag;
    private static String markup;

    // Objects
    private Editable editable;
    private ImageSpan[] arrImgSpan;
    private int cursorPos;
    private boolean cursorDir;

    // Initialize the static members
    static {
        SPAN_FLAG = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;
        imageTag = 1;
        markup = "";
    }

    // Constructor
    public BoardImageSpanHandler(Editable editable) {
        this.editable = editable;
        editable.setSpan(this, 0, 0, Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
    }

    // Callbacks invoked by SpanWatcher
    @Override
    public void onSpanAdded(Spannable text, Object what, int start, int end) {
        //log.i("onSpanAdded");
    }
    @Override
    public void onSpanRemoved(Spannable text, Object what, int start, int end) {
        //log.i("onSpanRemoved");
    }
    @Override
    public void onSpanChanged(Spannable text, Object what, int ostart, int oend, int nstart, int nend) {

        cursorDir = ostart <= nstart;
        log.i("what: %s", what);

        // Set the selection anchor to SpannableS
        if (what == Selection.SELECTION_START) {
            //log.i("SELECTION_START: %s, %s, %s, %s, %s", text, ostart, oend, nstart, nend);
            // forward direction set to true. In case of, ostart == nstart(or oend == nend), it
            // indicates a character is inserted or deleted.
            //log.i("Cursor direction: %s, %s", cursorDir, nend);
        // Cursor moves at the counter-direction
        } else if (what == Selection.SELECTION_END) {
            //log.i("SELECTION_END %s, %s, %s, %s, %s", text, ostart, oend, nstart, nend);

        }




    }

    // InputFilter
    public void setImageSpanInputFilter(){

        editable.setFilters(new InputFilter[]{ (source, start, end, dest, dstart, dend) -> {
            log.i("Filters: %s ,%s, %s, %s, %s, %s", source, start, end, dest, dstart, dend);

            for(ImageSpan span : arrImgSpan){
                log.i("span position: %s, %s, %s, %s", dstart, dend, dest.getSpanStart(span), dest.getSpanEnd(span));
                if(dstart < dest.getSpanEnd(span)) {
                    log.i("Skipping: %s, %s", dstart, dest.getSpanEnd(span));
                    Selection.setSelection((Spannable)dest, dstart - markup.length());
                }
            }

            if(source instanceof Spanned) {
                log.i("source instanceof Spanned");
            }



            return null;
        }});
    }


    public void setImageSpanToPosting(ImageSpan span) {
        int start = Selection.getSelectionStart(editable);
        int end = Selection.getSelectionEnd(editable);
        markup = "[image_" + imageTag + "]\n";
        log.i("Selection: %s, %s", start, end);
        editable.replace(Math.min(start, end), Math.max(start, end), markup);
        editable.setSpan(span, Math.min(start, end), Math.min(start, end) + markup.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        arrImgSpan = editable.getSpans(0, editable.length(), ImageSpan.class);

        Selection.setSelection(editable, editable.length());
        imageTag += 1;

        log.i("span markup: %s", markup);
    }


    // When an image is removed from the grid, the span containing the image and the markup string
    // should be removed at the same time.
    public void removeImageSpan(int position) {
        int start = editable.getSpanStart(arrImgSpan[position]);
        editable.removeSpan(arrImgSpan[position]);//remove the image span
        editable.replace(start, start + markup.length(), "");//delete the markkup
        imageTag -= 1;
    }

    public ImageSpan[] getImageSpan() {
        return editable.getSpans(0, editable.length(), ImageSpan.class);
    }
}
