package com.silverback.carman2.utils;

import android.text.DynamicLayout;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Selection;
import android.text.SpanWatcher;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ImageSpan;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.text.Selection.SELECTION_END;
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

    // Fields
    private String markup;

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
        editable.setSpan(this, 0, 0, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        //setImageSpanFilters(editable);

    }

    // This method is called to notify that the specified object has been attached to the
    // specified range of the text.
    @Override
    public void onSpanAdded(Spannable text, Object what, int start, int end) {
        if(what instanceof ImageSpan) {
            // In case a new span is inserted in the middle of existing spans, tags have to be
            // reset.
            if(spanList.size() > 0) resetImageSpanTag(text);
            String tag = text.toString().substring(start, end);
            Matcher num = Pattern.compile("\\d").matcher(tag);
            while(num.find()) {
                int position = Integer.valueOf(num.group());
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
            String tag = text.toString().substring(start, end);
            Matcher m = Pattern.compile("\\d").matcher(tag);
            while(m.find()) {
                int position = Integer.valueOf(m.group()); // group(0) means all. group(1) first.
                log.i("Position: %s", position);
                spanList.remove(what);
                mListener.notifyRemovedImageSpan(position);
            }

            text.setSpan(this, start, end, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
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
        if(what == SELECTION_START) {
            log.i("SELECTION_START: %s, %s, %s, %s", ostart, oend, nstart, nend);
            // Cursor adds or removes a character
            if (ostart == nstart) {
                for (ImageSpan span : spanList) {
                    if (nstart == text.getSpanEnd(span)) {
                        log.i("Spanned: %s", text.getSpanStart(span));
                        Selection.setSelection(text, text.getSpanStart(span) - 1);
                        text.setSpan(this, text.getSpanStart(span) - 1, text.getSpanStart(span),
                                Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                        break;
                    }
                }
            // Cursor inserts in the middle of the text
            } else {
                log.i("cursor pos: %s, %s", Selection.getSelectionStart(text), Selection.getSelectionEnd(text));
                int cursorPos = Math.min(Selection.getSelectionStart(text), Selection.getSelectionEnd(text));
                for(ImageSpan span : spanList) {
                    log.i("first place: %s, %s",Selection.getSelectionStart(text), text.getSpanStart(span));
                    if(cursorPos == text.getSpanStart(span)) {
                        Selection.setSelection(text, text.getSpanStart(span) - 1);
                        break;
                    }
                }

                text.setSpan(this, cursorPos, text.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            }

        } else if(what == SELECTION_END) {
            log.i("SELECTION_END: %s, %s, %s, %s", ostart, oend, nstart, nend);
            // ConcurrentModificationException occurs as long as an item is removed while looping.
            // For preventing the exception, size and index values should be decreased each time
            // any item is removed.

            int size = spanList.size();
            for(int i = 0; i < size; i++) {
                if(text.getSpanStart(spanList.get(i)) == -1 || text.getSpanEnd(spanList.get(i)) == -1) {
                    text.removeSpan(spanList.get(i));
                    spanList.remove(spanList.get(i));
                    mListener.notifyRemovedImageSpan(i);
                    size--;
                    i--;
                }
            }

            /*
            ListIterator iter = spanList.listIterator();
            while(iter.hasNext()) {
                int index = iter.nextIndex();
                if(text.getSpanStart(iter.next()) == -1 || text.getSpanEnd(iter.next()) == -1) {
                    iter.remove();
                    mListener.notifyRemovedImageSpan(index);
                }
            }

             */
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

        int imgTag = spanList.size();
        markup = "[image_" + imgTag + "]\n";

        editable.replace(Math.min(start, end), Math.max(start, end), markup);
        // Attention to put the following spans in order. Otherwise, it fails to notify that a new
        // ImageSpan is added.
        editable.setSpan(this, Math.min(start, end), Math.min(start, end) + markup.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        editable.setSpan(span, Math.min(start, end), Math.min(start, end) + markup.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);



        //setImageSpanFilters(editable);
    }

    // Invoked by removeImage(), callback invoked by BoardImageAdapter.OnBoardAttachImageListener
    // when removing an image in the recyclerview.
    public void removeImageSpan(int position) {
        int start = editable.getSpanStart(spanList.get(position));
        int end = editable.getSpanEnd(spanList.get(position));
        log.i("span position: %s, %s, %s", position, start, end);
        // Required to setSpan() to notify that the ImageSpan is removed.
        try {
            editable.setSpan(this, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            editable.removeSpan(spanList.remove(position));
            editable.replace(start, end, "");
        } catch(IndexOutOfBoundsException e) {
            log.e("IndexOutOfBoundException: %s", e.getMessage());

        }
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

    private void setImageSpanFilters(Editable editable) {

        editable.setFilters(new InputFilter[]{(source, start, end, dest, dstart, dend) -> {
            log.i("Filter: %s, %s, %s, %s, %s, %s", source, start, end, dest, dstart, dend);
            for(ImageSpan span : spanList) {
                if(dstart == editable.getSpanEnd(span)) {
                    Selection.setSelection(editable, dstart);
                    break;
                }
            }


            return null;
        }});
    }


}
