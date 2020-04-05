package com.silverback.carman2.utils;

import android.text.DynamicLayout;
import android.text.Editable;
import android.text.Selection;
import android.text.SpanWatcher;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.text.Selection.SELECTION_START;

/**
 * When SpanWatcher is attached to a Spannable, its methods will be called to notify it that
 * other markup objects have been added, changed, or removed.
 */
public class BoardImageSpanHandler implements SpanWatcher {

    private static final LoggingHelper log = LoggingHelperFactory.create(BoardImageSpanHandler.class);

    // Objects
    private OnImageSpanListener mListener;
    private Editable editable;
    private List<ImageSpan> spanList;

    // Interface to notify BoardWriteFragment that an image span is removed so that the fragment
    // should remove the span out the list and the adapter as well.
    public interface OnImageSpanListener {
        void notifyAddImageSpan(ImageSpan imgSpan, int position);
        void notifyRemovedImageSpan(int position);
    }

    // Constructor
    public BoardImageSpanHandler(Editable editable, OnImageSpanListener listener) {
        this.editable = editable;
        mListener = listener;
        spanList = new ArrayList<>();
        //editable.setSpan(this, 0, 0, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
    }

    // This method is called to notify that the specified object has been attached to the
    // specified range of the text.
    @Override
    public void onSpanAdded(Spannable text, Object what, int start, int end) {
        if(what instanceof ImageSpan) {
            log.i("onSpanAdded");
            // In case a new span is inserted in the middle of existing spans, tags have to be
            // reset.
            if(spanList.size() > 0) resetImageSpanTag(text);

            String tag = text.toString().substring(start, end);
            Matcher num = Pattern.compile("\\d+").matcher(tag);
            while(num.find()) {
                int position = Integer.valueOf(num.group(0));
                spanList.add(position, (ImageSpan)what);
                mListener.notifyAddImageSpan((ImageSpan)what, position);
                log.i("span added: %s, %s", position, what);
            }
        }
    }

    // This method is called to notify you that the specified object has been detached from the
    // specified range of the text.
    @Override
    public void onSpanRemoved(Spannable text, Object what, int start, int end) {

        if(what instanceof ImageSpan) {
            log.i("onSpanRemoved: %s, %s, %s, %s", text, what, start, end);
            String tag = text.toString().substring(start, end);
            Matcher m = Pattern.compile("\\d").matcher(tag);
            while(m.find()) {
                int position = Integer.valueOf(m.group()); // group(0) means all. group(1) first.
                log.i("Position: %s", position);
                spanList.remove(what);
                mListener.notifyRemovedImageSpan(position);
            }

            if(spanList.size() > 0) resetImageSpanTag(text);
        }
    }

    // In the state of cursor which indicates SELECTION_START is equal to SELECTION_END, a new
    // position increases or decreses compared with an old position as the cursor moves forward or
    // backward. When the cursor adds or removes a character, all the position values increases
    // or decreases equally.
    // On the other hand, in a range state which indicates that SELECTION_START values is not the
    // same as SELECTION_END values, the position values of SELECTION_START increases or decreases
    // as the text handle at the start moves forward or backward. The position values of SELECTION_END
    // works in the same way.
    @Override
    public void onSpanChanged(Spannable text, Object what, int ostart, int oend, int nstart, int nend) {
        // As long as the touch down and touch up at the same position, all position values are the
        // same no matter what is SELECTION_START OR SELECTION_END. When it makes a range,
        // however, the SELECTION_START and the SELECTION_END values become different.
        log.i("onSpanChanged: %s, %s, %s, %s, %s, %s", text, what, ostart, oend, nstart, nend);
        if (what == SELECTION_START) {
            log.i("SELECTION_START");
            // Cursor adds or removes a character
            if(ostart == nstart) {
                log.i("adding or removing: %s, %s, %s, %s", ostart, oend, nstart, nend);
                for(ImageSpan span : spanList) {
                    if(nstart == text.getSpanEnd(span)) {
                        Selection.setSelection(text, text.getSpanStart(span) - 1);
                    }
                }
            // Cursor first inserts or moves back and forth or sets a range with the text handle created.
            } else {
                //text.setSpan(this, 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                log.i("removed text: %s", editable);
                log.i("cursor moving or ranging: %s, %s, %s, %s", ostart, oend, nstart, nend);
            }
        }
    }

    // Edit mode.
    public void setImageSpanList(List<ImageSpan> spans) {
        spanList = spans;
        log.i("spanlist size: %s", spanList.size());
    }

    // Write mode
    public void setImageSpan(ImageSpan span) {
        log.i("setImageSpan: %s", span);
        int start = Selection.getSelectionStart(editable);
        int end = Selection.getSelectionEnd(editable);

        //this.imageSpan = span;
        int imgTag = 0;
        String markup = "[image_" + imgTag + "]\n";
        editable.replace(Math.min(start, end), Math.max(start, end), markup);
        editable.setSpan(this, Math.min(start, end), Math.min(start, end) + markup.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        editable.setSpan(span, Math.min(start, end), Math.min(start, end) + markup.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    public void removeImageSpan(int position) {
        log.i("span position: %s", position);
        int start = editable.getSpanStart(spanList.get(position));
        int end = editable.getSpanEnd(spanList.get(position));
        editable.setSpan(this, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        editable.removeSpan(spanList.remove(position));
        editable.replace(start, end, "");
    }

    // Reset the image tag each time a new imagespan is added particif(text.getSpans(0, text.length(), ImageSpan.class).length > 0)
    //                resetImageSpanTag(text);ularly in case of inserting.
    private void resetImageSpanTag(Spannable text) {
        // Reset the markup tag
        Matcher m = Pattern.compile("\\[image_\\d]\\n").matcher(text);
        int tag = 0;
        while(m.find()) {
            editable.replace(m.start(), m.end(), "[image_" + tag + "]\n");
            tag++;
        }
    }

}
