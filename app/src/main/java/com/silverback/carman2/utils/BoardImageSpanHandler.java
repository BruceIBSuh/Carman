package com.silverback.carman2.utils;

import android.text.Editable;
import android.text.Selection;
import android.text.SpanWatcher;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ImageSpan;
import android.view.inputmethod.InputConnection;
import android.widget.EditText;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 *
 ****** Tagging in the markup *****
 * The markup tag should be synced with the adapter position. For this reason, when an new image inserts
 * in the middle, the tag is named by the cursor position using getSpanTag() which is notified to the
 * adapter for inserting the markup in the text, then make all the existing tags reset sequentially.
 * When removing an span, fetch the tag from the markup,
 *
 ***** Surrounded Text ******
 * When deleting a surrounded text with the text handles, TextWatcher.onAfterTextChanged() manages
 * to detect which spans to have deleted usinsg getSpanStart() which should be -1.
 ****** Line breaker issue *****
 * When inserting an imagespan, a line breaker inserts at the end to start a new paragraph. In case
 * an imagespan inserts in the middle, however, a single line is left empty due to the line breaker
 * and the cursor is located at the
 */
public class BoardImageSpanHandler implements SpanWatcher {

    private static final LoggingHelper log = LoggingHelperFactory.create(BoardImageSpanHandler.class);

    private final String markup = "\\[image_\\d]\\n";

    // Objects
    private Matcher m;
    private EditText editText;
    private InputConnection ic;
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
            boolean isSurroundedText = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after){
                //log.i("beforeTextChanged: %s, %s, %s", start, count, after);
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //log.i("onTextChanged: %s, %s, %s, %s", s, start, before, count);
                // Deleting an character using backspace
                if(count == 0 && before == 1) {
                    for(ImageSpan span : spanList) {
                        if(start == editable.getSpanEnd(span)) {
                            // When the cursor meets getSpanEnd(), hold up the position at the moment.
                            Selection.setSelection(editable, editable.getSpanEnd(span));
                            // Have to use View.post() because the system waits for letting some layouts
                            // done until an appropriate time. Thus, if Selection.setSelection() is made
                            // before layouts are finished, the system may undo setSelection().
                            editText.post(() -> Selection.setSelection(editable, editable.getSpanStart(span) - 1));
                            break;

                        }
                    }
                // Deleting characters surrounded by the text handles all at once by using backspace.
                } else if(count == 0 && before > 1) isSurroundedText = true;
            }


            @Override
            public void afterTextChanged(Editable s){
                //log.i("span nums: %s, %s", isSurroundedText, spanList.size());
                if(!isSurroundedText) return;

                // To prevent ConcurrentModificationException which occurs when an element is
                // removed during looping, substract the list size and index to exclude the
                // elimination.
                int size = spanList.size();
                for(int i = 0; i < size; i++) {
                    if(s.getSpanStart(spanList.get(i)) == -1) {
                        log.i("index: %s, %s, %s", i, spanList.get(i), s.getSpanStart(spanList.get(i)));
                        mListener.notifyRemovedImageSpan(i);
                        spanList.remove(spanList.get(i));
                        size--;
                        i--;
                    }
                }
            }

        });
    }

    // This method is called to notify that the specified object has been attached to the
    // specified range of the text.
    @Override
    public void onSpanAdded(Spannable text, Object what, int start, int end) {
        if(what instanceof ImageSpan) {
            // In case a new span is inserted in the middle of existing spans, tags have to be
            // reset.
            //resetImageSpanTag(text);
            String tag = text.toString().substring(start, end);
            m = Pattern.compile("\\d").matcher(tag);
            while(m.find()) {
                int position = Integer.valueOf(m.group());
                spanList.add(position, (ImageSpan)what);
                mListener.notifyAddImageSpan((ImageSpan)what, position);
            }
            resetImageSpanTag(text);
        }
    }

    // This method is called to notify that the specified object has been detached from the specified
    // range of the text.
    @Override
    public void onSpanRemoved(Spannable text, Object what, int start, int end) {
        if(what instanceof ImageSpan) {
            //log.i("Text span removed: %s", text);
            resetImageSpanTag(text);
            String tag = text.toString().substring(start, end);
            m = Pattern.compile("\\d").matcher(tag);
            while(m.find()) {
                // Required to subtract 1 from the position in Matcher because it starts with 1
                // in the finding order.
                int position = Integer.valueOf(m.group()); // group(0) means all. group(1) first.
                //log.i("removed span position: %s", position);
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
        // however, the SELECTION_START and the SELECTION_END values become differe
        //log.i("onSpanChanged: %s", what);
        if(what == Selection.SELECTION_END) {
            for(ImageSpan span : spanList) {
                //log.i("cursor insert: %s, %s", Math.min(ostart, nstart), text.getSpanStart(span));
                if(text.getSpanStart(span) == nstart) {
                    editText.post(() -> Selection.setSelection(text, text.getSpanStart(span) - 1));
                    break;
                }

            }
        }
    }

    // Edit mode to call in the existing imagespan list and set spans to the editable and imagespans
    // in the text.
    public void setImageSpanList(List<ImageSpan> spans) {
        spanList = spans;
        log.i("existing spans: %s", spans.size());
        for(ImageSpan span : spans) log.i("Image span: %s", span);
        m = Pattern.compile("\\[image_\\d]\\n").matcher(editable);
        int index = 0;
        while(m.find()) {
            editable.setSpan(spanList.get(index), m.start(), m.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            editable.setSpan(this, m.start(), m.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            index++;
        }
    }

    // When adding a new imagespan, get the current cursor position and the position that the span
    // inserts. The span is required not only to set the span to Editable for notifying the position
    // to SpanWatcher but also to set the imagespan at the same position.
    public void setImageSpan(ImageSpan span) {
        log.i("setImageSpan: %s", span);
        int curpos = editText.getSelectionStart();
        int imgTag = (spanList.size() == 0)? 0 : getSpanTag(curpos);
        String markup = "[image_" + imgTag + "]\n";
        editable.insert(curpos, markup);
        // Attention to put the following spans in order. Otherwise, it fails to notify that a new
        // ImageSpan is added.
        editable.setSpan(this, curpos, curpos + markup.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        editable.setSpan(span, curpos, curpos + markup.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // If a span inserts in the middle, due to the line breaker at the end, an empty line will
        // be interleaved before the following paragraph, leaving the cursor at the first of the
        // empty line. The issue is that when the cursor moves back, spans to notify the imagespan
        // position will be removed, which results in deleting an image unexpectedly. To prevent
        // it, the empty line should be removed when inserting an image span by replacing the line
        // with the empty spance.
        if(curpos + markup.length() < editable.length()) {
            // Remove the line breaker in the empty line which immediately follows another line
            // break at the end of the markup.
            editable.replace(curpos + markup.length(), curpos + markup.length() + 1, "");
            log.i("Imagespan inserted: %s", editable);
        }
    }

    // When removing an imagespan, receiving the position from the adapter, get the target span to
    // remove and delete the markup. In SpanWatcher.OnSpanRemoved(), reset the tags excluding the
    // removed span first to make the position synced with the adapter to remove the same image
    // from the adapter.
    public void removeImageSpan(int position) {
        log.i("adapter position: %s", position);
        int start = editable.getSpanStart(spanList.get(position));
        int end = editable.getSpanEnd(spanList.get(position));

        try {
            //editable.setSpan(this, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            //editable.removeSpan(spanList.get(position)); //handles in onSpanRemoved()
            editable.replace(start, end, "");
        } catch(IndexOutOfBoundsException e) {
            resetImageSpanTag(editable);
            log.i("text: %s", editable);
            log.e("IndexOutOfBoundException: %s", e.getMessage());
            editable.removeSpan(spanList.get(position));
            mListener.notifyRemovedImageSpan(position);
        }
    }

    // Set an sequential tag when adding a new imagespan.
    private int getSpanTag(int cursor) {
        int index = 0;
        //if(spanList == null || spanList.size() == 0) return 0;
        for(ImageSpan span : spanList) {
            if(cursor > editable.getSpanStart(span)) index++;
            else break;
        }

        return index;
    }

    // Reset the image tag each time a new imagespan is added particif(text.getSpans(0, text.length(), ImageSpan.class).length > 0)
    // resetImageSpanTag(text);ularly in case of inserting.
    private void resetImageSpanTag(Spannable text) {
        // Reset the markup tag
        m = Pattern.compile("\\[image_\\d]\\n").matcher(text);
        int tag = 0;
        while(m.find()) {
            editable.replace(m.start(), m.end(), "[image_" + tag + "]\n");
            tag++;
        }
    }


}
