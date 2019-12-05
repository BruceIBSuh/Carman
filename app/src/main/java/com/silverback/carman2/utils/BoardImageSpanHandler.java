package com.silverback.carman2.utils;

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
    private int cursor;

    // Initialize the static members
    static {
        SPAN_FLAG = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;
        imageTag = 1;
        markup = "";
    }

    // Constructor
    public BoardImageSpanHandler(Editable editable) {
        this.editable = editable;
        editable.setSpan(this, 0, editable.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
    }

    // Callbacks invoked by SpanWatcher
    @Override
    public void onSpanAdded(Spannable text, Object what, int start, int end) {}
    @Override
    public void onSpanRemoved(Spannable text, Object what, int start, int end) {}
    @Override
    public void onSpanChanged(Spannable text, Object what, int ostart, int oend, int nstart, int nend) {

        log.i("what: %s", what);
        // Set the selection anchor to Spannable.
        if (what == Selection.SELECTION_START) {
            log.i("SELECTION_START: %s, %s, %s, %s, %s", text, ostart, oend, nstart, nend);
            cursor = (ostart == nstart)?ostart : Math.max(ostart, nstart);

        // Cursor moves at the counter-direction
        } else if (what == Selection.SELECTION_END) {
            log.i("SELECTION_END %s, %s, %s, %s, %s", text, ostart, oend, nstart, nend);
            cursor = (ostart == nstart)?ostart : Math.max(ostart, nstart);
        }
    }

    // InputFilter
    public void setImageSpanInputFilter(){
        editable.setFilters(new InputFilter[]{ (source, start, end, dest, dstart, dend) -> {
            log.i("Filters: %s ,%s, %s, %s, %s, %s", source, start, end, dest, dstart, dend);

            // Disable ImageSpan to be deleted by soft input
            for(ImageSpan span : arrImgSpan) {
                log.i("span position: %s, %s", dest.getSpanStart(span), dest.getSpanEnd(span));
                if(cursor == dest.getSpanEnd(span))
                    Selection.setSelection((Spannable)dest, dest.getSpanStart(span) - 1);
                else if(cursor == dest.getSpanStart(span))
                    Selection.setSelection((Spannable)dest, dest.getSpanEnd(span) + 1);
            }

            /*
            if(source instanceof Spanned) {
                if(Math.abs(start - end) == 0 && cursor > 1)
                    Selection.setSelection(editable, dstart - markup.length() - 1);
            }
            */
            //Selection.setSelection(ssb, newStart + source.length());
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
        //log.i("arrImageSpan: %s", arrImgSpan.length);

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
