package com.silverback.carman2.utils;

import android.text.Editable;
import android.text.Selection;
import android.text.SpanWatcher;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ImageSpan;
import android.widget.EditText;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.text.Selection.SELECTION_END;
import static android.text.Selection.SELECTION_START;

/**
 *
 * When SpanWatcher is attached to a Spannable, its methods will be called to notify that other
 * markup objects have been added, changed, or removed.
 *
 * Special care should be paid to the span flags which indicates the ImageSpan postions.
 * From the start, the text is spanned with SpanWatcher to tell where Selection.getSelectionStart()
 * is. SpanWatcher stops the span at the position where an imagespan is set such that another span
 * has to be set at the end of the imagespan with Spanned.SPAN_EXCLUSIVE_INCLUSIVE in order to
 * recognize the current cursor position.
 *
 * When removing an imagespan, the text span is to be set as well with Spanned.SPAN_INCLUSIVE_EXCLUSIVE
 * On the other hand, inserting the cursor in the millde of text, the text span has to be reset from
 * that point with Spanned.SPAN_INCLUSIVE_INCLUSIVE. The reason that the span end should be INCLUSIVE
 * is that the cursor not only moves backward deleting a character but also moves forward adding one.
 */
public class BoardImageSpanHandler implements SpanWatcher {

    private static final LoggingHelper log = LoggingHelperFactory.create(BoardImageSpanHandler.class);

    // Objects
    private EditText editText;
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
    public BoardImageSpanHandler(EditText editText, OnImageSpanListener listener) {

        this.editable = editText.getText();
        this.editText = editText;
        mListener = listener;
        spanList = new ArrayList<>();
        //editable.setSpan(this, 0, 0, Spanned.SPAN_INCLUSIVE_INCLUSIVE);

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after){}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(count == 0 && before == 1) {
                    for(ImageSpan span : spanList) {
                        if(start == editable.getSpanEnd(span)) {
                            // Have to use View.post() because the system waits for letting some layouts
                            // done until an appropriate time. Thus, if Selection.setSelection() is made
                            // before layouts are finished, the system may undo setSelection().
                            editText.post(() -> Selection.setSelection(editable, editable.getSpanStart(span) - 1));
                            break;
                        }
                    }
                }
            }
            @Override
            public void afterTextChanged(Editable s){}
        });
    }

    // This method is called to notify that the specified object has been attached to the
    // specified range of the text.
    @Override
    public void onSpanAdded(Spannable text, Object what, int start, int end) {
        if(what instanceof ImageSpan) {
            // In case a new span is inserted in the middle of existing spans, tags have to be
            // reset.
            log.d("onSpanAdded: %s", what);
            if(spanList.size() >= 2) resetImageSpanTag(text);
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
        log.d("onSpanRemoved: %s", what);
        if(what instanceof ImageSpan) {
            //log.i("onSpanRemoved: %s, %s, %s, %s", text, what, start, end);
            resetImageSpanTag(text);
            String tag = text.toString().substring(start, end);
            Matcher m = Pattern.compile("\\d").matcher(tag);
            while(m.find()) {
                // Required to subtract 1 from the position in Matcher because it starts with 1
                // in the finding order.
                int position = Integer.valueOf(m.group()); // group(0) means all. group(1) first.
                log.i("removed span position: %s", position);
                spanList.remove(what);
                mListener.notifyRemovedImageSpan(position);
            }

            //text.setSpan(this, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
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
            if(ostart != nstart) {
                // Prevent the cursor from inserting at the start of imagespans. Seems not to be working
                int cursorPos = Selection.getSelectionStart(text);
                for (ImageSpan span : spanList) {
                    if (cursorPos == text.getSpanStart(span)) {
                        log.i("cursor shouldn't be inserted here");
                        editText.post(() -> Selection.setSelection(editable, editable.getSpanStart(span) - 1));
                        break;
                    }
                }
            } //else log.i("cursor state");
        } else if(what == SELECTION_END) {
            //log.i("SELECTION_END: %s, %s, %s, %s", ostart, oend, nstart, nend);
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
        }

    }

    // Edit mode.
    public void setImageSpanList(List<ImageSpan> spans) {
        spanList = spans;
        for(ImageSpan span : spans) log.i("Image span: %s", span);

        Matcher m = Pattern.compile("\\[image_\\d]\\n").matcher(editable);
        int index = 0;
        while(m.find()) {
            //editable.setSpan(this, m.start(), m.end(), Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
            editable.setSpan(spanList.get(index), m.start(), m.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            index++;
        }
    }

    // Write mode
    public void setImageSpan(ImageSpan span) {
        log.i("setImageSpan: %s", span);
        int start = Selection.getSelectionStart(editable);
        int end = Selection.getSelectionEnd(editable);

        int imgTag = spanList.size();
        String markup = "[image_" + imgTag + "]\n";

        editable.replace(Math.min(start, end), Math.max(start, end), markup);
        // Attention to put the following spans in order. Otherwise, it fails to notify that a new
        // ImageSpan is added.
        editable.setSpan(this, Math.min(start, end), Math.min(start, end) + markup.length(),
                Spanned.SPAN_EXCLUSIVE_INCLUSIVE);

        editable.setSpan(span, Math.min(start, end), Math.min(start, end) + markup.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
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
    // resetImageSpanTag(text);ularly in case of inserting.
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
