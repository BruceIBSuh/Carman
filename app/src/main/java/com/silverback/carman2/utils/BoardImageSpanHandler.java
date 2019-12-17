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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BoardImageSpanHandler implements SpanWatcher {

    private static final LoggingHelper log = LoggingHelperFactory.create(BoardImageSpanHandler.class);

    private static final int SPAN_FLAG;
    private static int imageTag;
    //private static String markup;
    private static Spannable spannable;

    // Objects
    private Editable editable;
    //private ImageSpan[] arrImgSpan;
    private List<ImageSpan> imgSpanList;
    private OnAttachedImageListener mListenr;

    private Pattern pattern;
    private String markup;

    // Initialize the static members
    static {
        SPAN_FLAG = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;
        imageTag = 1;
        //markup = "";
    }

    public interface OnAttachedImageListener {
        List<ImageSpan> getImageSpanList();
    }

    // Constructor
    public BoardImageSpanHandler(Editable editable) {
        this.editable = editable;
        editable.setSpan(this, 0, 0, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        markup = "[image_" + imageTag + "]\n";
        imgSpanList = new ArrayList<>();
        pattern = Pattern.compile(markup);
    }

    // Callbacks invoked by SpanWatcher
    @Override
    public void onSpanAdded(Spannable text, Object what, int start, int end) {
        log.i("onSpanAdded");
    }
    @Override
    public void onSpanRemoved(Spannable text, Object what, int start, int end) {
        log.i("onSpanRemoved");
    }

    @Override
    public void onSpanChanged(Spannable text, Object what, int ostart, int oend, int nstart, int nend) {
        log.i("onSpanChanged: %s, %s", text, what);
        //if(arrImgSpan == null || arrImgSpan.length == 0) return;
        if(imgSpanList == null || imgSpanList.size() == 0) return;
        // As long as the touch down and touch up at the same position, all position values are
        // the same no matter what is SELECTION_START OR SELECTION_END.
        // When it makes a range, however, the SELECTION_START and the SELECTION_END values become different.
        if (what == Selection.SELECTION_START) {
            // Indicate that the cursor is moving, not making a range.
            log.i("SELECTION_START: %s, %s, %s, %s", ostart, oend, nstart, nend);

            // The same values indicate that the cursur is moving by the key input or no change is
            // is made when the selection is made set range.
            if((ostart == nstart) && (oend == nend)) {
                //for(ImageSpan span : arrImgSpan) {
                for(ImageSpan span : imgSpanList) {
                    if(nstart == text.getSpanEnd(span)) {
                        log.i("Spanned at the end!!!!");
                        Selection.setSelection(text, Math.max(0, text.getSpanStart(span) - 1));
                        break;
                    }
                }
            }

        } else if (what == Selection.SELECTION_END) {
            log.i("SELECTION_END %s, %s, %s, %s", ostart, oend, nstart, nend);

        }

        /*
        final Matcher m = pattern.matcher(text.subSequence(Math.min(ostart, nstart), Math.max(oend, nend)));
        while(m.find()) {
            //String tag = editable.subSequence(m.start(), m.end()).toString();
            //int number = Integer.valueOf(tag.replaceAll("\\D", ""));
            log.i("ImageSpan removed");
        }
        */

    }

    private void updateImageSpanList(Spannable editable) {
        // Find the tag from the posting String.

    }


    // InputFilte
    public void setImageSpanInputFilter(){
        editable.setFilters(new InputFilter[]{ (source, start, end, dest, dstart, dend) -> {
            log.i("InputFilter: %s, %s, %s, %s, %s", start, end, dest, dstart,dend);
            //for(ImageSpan span : arrImgSpan){
            for(ImageSpan span : imgSpanList) {
                log.i("Within ImageSpan: %s, %s", dest.getSpanStart(span), dest.getSpanEnd(span));
                //if(dstart == 0) break;
                if(Math.min(dstart, dend) < dest.getSpanEnd(span)) {
                    log.i("ImageSpan skipping");
                    Selection.setSelection(editable, dest.getSpanStart(span) - 1);
                }
            }
            return null;
        }});

    }

    public void setImageSpanToPosting(ImageSpan span) {

        int start = Selection.getSelectionStart(editable);
        int end = Selection.getSelectionEnd(editable);
        markup = "[image_" + imageTag + "]\n";

        editable.replace(Math.min(start, end), Math.max(start, end), markup);
        editable.setSpan(span, Math.min(start, end), Math.min(start, end) + markup.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        //arrImgSpan = editable.getSpans(0, editable.length(), ImageSpan.class);
        imgSpanList.add(span);

        Selection.setSelection(editable, editable.length());
        imageTag += 1;

    }


    // When an image is removed from the GridView, the span containing the image and the markup string
    // should be removed at the same time.
    public void removeImageSpan(int position) {
        //int start = editable.getSpanStart(arrImgSpan[position]);
        int start = editable.getSpanStart(imgSpanList.get(position));
        //editable.removeSpan(arrImgSpan[position]);//remove the image span
        editable.removeSpan(imgSpanList.get(position));
        editable.replace(start, start + markup.length(), "");//delete the markkup
        imageTag -= 1;
    }

    // Referenced by removeGridImage() defined in BoardWriteDlgFragmen, which is invoked when an
    // image in the GridView is selected to remove out of the list.
    public ImageSpan[] getImageSpan() {
        return editable.getSpans(0, editable.length(), ImageSpan.class);
    }
}
