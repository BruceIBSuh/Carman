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
    private SpannableStringBuilder ssb;
    private Editable editable;
    private ImageSpan[] arrImgSpan;
    private SpanWatcher[] arrSpanWatcher;
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
        ssb = new SpannableStringBuilder(editable);
        editable.setSpan(this, 0, 0, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
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

        if(arrImgSpan == null || arrImgSpan.length == 0) return;

        // As long as the touch down and touch up at the same position, all position values are
        // the same no matter what value is SELECTION_START OR SELECTION_END.
        // When it makes a range, however, the SELECTION_START and the SELECTION_END values become different.
        if (what == Selection.SELECTION_START) {
            // Indicate that the cursor is moving, not making a range.
            log.i("SELECTION_START: %s, %s, %s, %s", ostart, oend, nstart, nend);
            // Prevent ImageSpan from deleting by picking del key.
            if((ostart == nstart) && (oend == nend)) {
                for(ImageSpan span : arrImgSpan) {
                    if(nstart == text.getSpanEnd(span)) {
                        log.i("Spanned at the end!!!!");
                        Selection.setSelection(text, Math.max(0, text.getSpanStart(span) - 1));
                    }
                }
            // Preven ImageSpan from deleting when it sets range and cut or del the range by blocking
            // the cursor from moving left.
            } else if((ostart > nstart) && (oend > nend)) {
                for(ImageSpan span : arrImgSpan) {
                    if(nstart == text.getSpanEnd(span)) {
                        Selection.setSelection(text, Math.max(0, text.getSpanEnd(span) + 1));
                    }
                }
            }
        } else if (what == Selection.SELECTION_END) {
            log.i("SELECTION_END %s, %s, %s, %s", ostart, oend, nstart, nend);
        }

    }

    // InputFilter
    /*
    public void setImageSpanInputFilter(){

        editable.setFilters(new InputFilter[]{ (source, start, end, dest, dstart, dend) -> {
            log.i("Filters: %s ,%s, %s, %s, %s, %s", source, start, end, dest, dstart, dend);
            for(ImageSpan span : arrImgSpan){
                log.i("Within ImageSpan: %s, %s", dest.getSpanStart(span), dest.getSpanEnd(span));
                if(dend == dest.getSpanStart(span)) {
                    log.i("ImageSpan skipping");
                    Selection.setSelection((Spannable)dest, 1);
                }
            }

            if(source instanceof Spanned) {
            }

            if(source instanceof ImageSpan) {
                log.i("ImageSpan");
            }

            return null;
        }});
    }

     */


    public void setImageSpanToPosting(ImageSpan span) {

        int start = Selection.getSelectionStart(editable);
        int end = Selection.getSelectionEnd(editable);
        markup = "[image_" + imageTag + "]\n";

        editable.replace(Math.min(start, end), Math.max(start, end), markup);
        editable.setSpan(span, Math.min(start, end), Math.min(start, end) + markup.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        arrImgSpan = editable.getSpans(0, editable.length(), ImageSpan.class);

        Selection.setSelection(editable, editable.length());
        imageTag += 1;

    }


    // When an image is removed from the GridView, the span containing the image and the markup string
    // should be removed at the same time.
    public void removeImageSpan(int position) {
        int start = editable.getSpanStart(arrImgSpan[position]);
        editable.removeSpan(arrImgSpan[position]);//remove the image span
        editable.replace(start, start + markup.length(), "");//delete the markkup
        imageTag -= 1;
    }

    // Referenced by removeGridImage() defined in BoardWriteDlgFragmen, which is invoked when an
    // image in the GridView is selected to remove out of the list.
    public ImageSpan[] getImageSpan() {
        return editable.getSpans(0, editable.length(), ImageSpan.class);
    }
}
