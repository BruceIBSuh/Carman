package com.silverback.carman2.utils;

import android.content.IntentFilter;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Selection;
import android.text.SpanWatcher;
import android.text.Spannable;
import android.text.Spanned;
import android.text.style.ImageSpan;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

public class BoardImageSpanHandler implements SpanWatcher {

    private static final LoggingHelper log = LoggingHelperFactory.create(BoardImageSpanHandler.class);

    private static final int SPAN_FLAG;
    private static int imageTag;

    // Objects
    private Editable editable;

    // Fields
    private String markup;

    static {
        SPAN_FLAG = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;
        imageTag = 1;
    }

    // Constructor
    public BoardImageSpanHandler(Editable editable) {
        this.editable = editable;
        editable.setSpan(this, 0, 0, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
    }

    // Callbacks invoked by SpanWatcher
    @Override
    public void onSpanAdded(Spannable text, Object what, int start, int end) {}
    @Override
    public void onSpanRemoved(Spannable text, Object what, int start, int end) {}
    @Override
    public void onSpanChanged(Spannable text, Object what, int ostart, int oend, int nstart, int nend) {
        log.i("what: %s", what);
        if (what == Selection.SELECTION_START) {
            log.i("SpanWatcher_start: %s, %s", nstart, markup.length());
            if (nstart < markup.length()) {
                final int end = Math.max(markup.length(), Selection.getSelectionEnd(text));
                Selection.setSelection(text, markup.length(), end);
                log.i("SpanWatcher_start: %s, %s, %s", text, markup.length(), end);
            }

        } else if (what == Selection.SELECTION_END) {
            log.i("SpanWatcher_start: %s, %s", nstart, markup.length());
            final int start = Math.max(markup.length(), Selection.getSelectionEnd(text));
            final int end = Math.max(start, nstart);
            if (end != nstart) {
                Selection.setSelection(text, start, end);
                log.i("SpanWatcher_end: %s, %s, %s", text, markup.length(), end);
            }
        }
    }

    public void setImageSpanToPosting(ImageSpan span) {
        int selectionStart = Selection.getSelectionStart(editable);
        int selectionEnd = Selection.getSelectionEnd(editable);
        markup = "[image_" + imageTag + "]\n";
        log.i("Selection: %s, %s", selectionStart, selectionEnd);

        editable.replace(selectionStart, selectionEnd, markup);
        editable.setSpan(span, selectionStart, selectionStart + markup.length(), SPAN_FLAG);
        Selection.setSelection(editable, editable.length());
        imageTag += 1;

        log.i("span markup: %s", markup);
    }

    public void setImageSpanInputFilter(){
        editable.setFilters(new InputFilter[]{
                (source, start, end, dest, dstart, dend) -> {
                    log.i("Filters: %s ,%s, %s, %s, %s, %s", source, start, end, dest, dstart, dend);
                    return null;
                }
        });
    }

    public ImageSpan[] getImageSpan() {
        return editable.getSpans(0, editable.length(), ImageSpan.class);
    }
}
