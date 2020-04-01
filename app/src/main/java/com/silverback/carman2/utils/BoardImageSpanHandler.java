package com.silverback.carman2.utils;

import android.text.Editable;
import android.text.Selection;
import android.text.SpanWatcher;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ImageSpan;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * When SpanWatcher is attached to a Spannable, its methods will be called to notify it that
 * other markup objects have been added, changed, or removed.
 */
public class BoardImageSpanHandler implements SpanWatcher {

    private static final LoggingHelper log = LoggingHelperFactory.create(BoardImageSpanHandler.class);

    // Constants
    private static String markup;

    // Objects
    private OnImageSpanListener mListener;
    private SpannableStringBuilder ssb;
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
        //ssb = new SpannableStringBuilder(editable);
        editable.setSpan(this, 0, 0, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
    }

    // This method is called to notify you that the specified object has been attached to the
    // specified range of the text.
    @Override
    public void onSpanAdded(Spannable text, Object what, int start, int end) {
        if(what instanceof ImageSpan) {
            log.i("onSpanAdded: %s, %s, %s, %s", text, what, start, end);
            // Image tag that indicates the placeholder of an attached image should be reset
            resetImageSpanTag(text);

            String tag = text.toString().substring(start, end);
            Matcher num = Pattern.compile("\\d+").matcher(tag);
            while(num.find()) {
                int position = Integer.valueOf(num.group(0));
                spanList.add(position, (ImageSpan)what);
                mListener.notifyAddImageSpan((ImageSpan)what, position);
            }
        }
    }

    // This method is called to notify you that the specified object has been detached from the
    // specified range of the text.
    @Override
    public void onSpanRemoved(Spannable text, Object what, int start, int end) {
        if(what instanceof ImageSpan) {
            log.i("onSpanRemoved: %s, %s, %s, %s", text, what, start, end);
            resetImageSpanTag(text);
            log.i("onSpanRemoved: %s, %s, %s, %s", text, what, start, end);
            String tag = text.toString().substring(start, end);
            Matcher m = Pattern.compile("\\d").matcher(tag);
            while(m.find()) {
                int position = Integer.valueOf(m.group(0));
                spanList.remove(what);
                mListener.notifyRemovedImageSpan(position);
                log.i("removed position: %s, %s", what, position);
            }
        }
    }

    // This method is called to notify that the specified object has been relocated from
    // the range ostart…oend to the new range nstart…nend of the text.
    // Cursor action:
    // (ostart == oend) < (nstart == nend) : moving forward
    // (ostart == oned) > (nstart == nend) : moving backward
    // (ostart == oend) == (nstart == nend) : adding or removing character
    // Either ading or removing a character makes all postions equal.
    @Override
    public void onSpanChanged(Spannable text, Object what, int ostart, int oend, int nstart, int nend) {
        // As long as the touch down and touch up at the same position, all position values are the
        // same no matter what is SELECTION_START OR SELECTION_END. When it makes a range,
        // however, the SELECTION_START and the SELECTION_END values become different.
        if (what == Selection.SELECTION_START) {
            // cursor position when adding or removing a charactor
            if((ostart == nstart)) {
                log.i("adding or removing: %s, %s, %s, %s", ostart, oend, nstart, nend);
                for(ImageSpan span : spanList) {
                    if(nstart == text.getSpanEnd(span)) {
                        log.i("Spanned at the end!!!!");
                        Selection.setSelection(text, Math.max(0, text.getSpanStart(span) - 1));
                    }
                }
            // Preven ImageSpan from deleting when it sets range and cut or del the range by blocking
            // the cursor from moving left.
            // cursor position when moving or ranging
            } else {
                for(ImageSpan span : spanList) {
                    if(nstart == text.getSpanStart(span)) {
                        Selection.setSelection(text, Math.max(0, text.getSpanStart(span) - 1));
                        break;
                    }
                }
            }

        } else if (what == Selection.SELECTION_END) {
            //log.i("SELECTION_END %s, %s, %s, %s", ostart, oend, nstart, nend);
        }

    }


    public void setImageSpan(List<ImageSpan> spans) {
        spanList = spans;
        log.i("spanlist size: %s", spanList.size());
    }


    // Reset the image tag each time a new imagespan is added particularly in case of inserting.
    private void resetImageSpanTag(Spannable text) {
        // Reset the markup tag
        Matcher m = Pattern.compile("\\[image_\\d]\\n").matcher(text);
        int tag = 0;
        while(m.find()) {
            markup = "[image_" + tag + "]\n";
            editable.replace(m.start(), m.end(), markup);
            tag++;
        }
    }

}
