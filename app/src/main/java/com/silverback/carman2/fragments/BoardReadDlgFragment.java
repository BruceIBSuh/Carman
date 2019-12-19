package com.silverback.carman2.fragments;


import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.LeadingMarginSpan;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.firestore.FirebaseFirestore;
import com.silverback.carman2.BoardActivity;
import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.ImageViewModel;
import com.silverback.carman2.threads.AttachedBitmapTask;
import com.silverback.carman2.threads.ThreadManager;
import com.silverback.carman2.utils.Constants;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A simple {@link Fragment} subclass.
 */
public class BoardReadDlgFragment extends DialogFragment {

    private static final LoggingHelper log = LoggingHelperFactory.create(BoardReadDlgFragment.class);

    // Constants
    private final int SPANNED_FLAG = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;

    // Objects
    private Context context;
    private SpannableStringBuilder spannable;
    private ImageViewModel imageModel;
    private String postTitle, postContent, userName, userPic;
    private List<String> imgUriList;
    //private LruCache<String, Bitmap> memCache;
    private List<Integer> viewIdList;
    private AttachedBitmapTask bitmapTask;
    private List<Bitmap> bmpList;
    private SharedPreferences mSettings;

    // UIs
    private ConstraintLayout constraintLayout;
    //private ConstraintSet set;
    //private ImageView imgView;
    //private ConstraintLayout.LayoutParams layoutParams;
    private TextView tvAutoInfo;
    private TextView tvContent;

    private ImageView attachedImage;

    // Fields
    private StringBuilder autoData;
    private String userId;
    private int cntImages;

    public BoardReadDlgFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.context = getContext();
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        imageModel = ViewModelProviders.of(this).get(ImageViewModel.class);
        //sdf = new SimpleDateFormat("MM.dd HH:mm", Locale.getDefault());

        if(getArguments() != null) {
            postTitle = getArguments().getString("postTitle");
            postContent = getArguments().getString("postContent");
            userName = getArguments().getString("userName");
            userPic = getArguments().getString("userPic");
            imgUriList = getArguments().getStringArrayList("imageUriList");
            userId = getArguments().getString("userId");
        }

        // Separate the text by line feeder("\n") to set the leading margin span to it, then return
        // a margin-formatted spannable string, which, in turn, set the image spans to display
        // attached images as images are notified to retrieve by the task.
        spannable = translateParagraphSpan(postContent);

        // Initiate the task to fetch images attached to the post
        if(imgUriList != null && imgUriList.size() > 0) {
            bitmapTask = ThreadManager.startAttachedBitmapTask(context, imgUriList, imageModel);
        }


        // Get the auto data, which is saved as the type of json string in SharedPreferences, for
        // displaying it in the post header.
        if(getActivity() != null) mSettings = ((BoardActivity)getActivity()).getSettings();
        String json = mSettings.getString(Constants.VEHICLE, null);

        try {
            JSONArray jsonArray = new JSONArray(json);
            autoData = new StringBuilder();

            // Refactor required
            autoData.append(jsonArray.get(0)).append(" ")
                    .append(jsonArray.get(2)).append(" ")
                    .append(jsonArray.get(3));

        } catch(JSONException e) {
            log.e("JSONException: %s", e.getMessage());
        }

    }


    @SuppressWarnings("ConstantConditions")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View localView = inflater.inflate(R.layout.dialog_board_read, container, false);
        constraintLayout = localView.findViewById(R.id.constraint_posting);
        ImageButton btn = localView.findViewById(R.id.imgbtn_dismiss);
        TextView tvTitle = localView.findViewById(R.id.tv_post_title);
        TextView tvUserName = localView.findViewById(R.id.tv_username);
        tvAutoInfo = localView.findViewById(R.id.tv_autoinfo);
        TextView tvDate = localView.findViewById(R.id.tv_posting_date);
        tvContent = localView.findViewById(R.id.tv_posting_body);
        ImageView imgUserPic = localView.findViewById(R.id.img_userpic);
        btn.setOnClickListener(view -> dismiss());

        tvTitle.setText(postTitle);
        tvUserName.setText(userName);
        tvAutoInfo.setText(autoData.toString());
        tvContent.setText(postContent);
        tvDate.setText(getArguments().getString("timestamp"));


        // Set the user image
        Uri uriUserPic = Uri.parse(userPic);
        Glide.with(getContext())
                .asBitmap()
                .load(uriUserPic)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .circleCrop()
                .into(imgUserPic);

        return localView;
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);

        // Set the image span to the post content as the image span instances are retrieved from
        // AttachedBitmapTask.
        imageModel.getImageSpanArray().observe(getViewLifecycleOwner(), spanArray -> {
            //log.i("ImageSpan: %s", spanArray.keyAt(0));
            SpannableStringBuilder imgSpannable = createImageSpanString(spanArray);
            tvContent.setText(imgSpannable);

        });
    }

    // Divide the text by line separator("\n"), excluding image lines("[image]"), then set the span
    // for making the leading margin to every single line.
    private SpannableStringBuilder translateParagraphSpan(String text) {

        SpannableStringBuilder spannable = new SpannableStringBuilder(text);
        final String REGEX_SEPARATOR = "\n";
        final String REGEX_MARKUP = "\\[image_\\d]";
        final Matcher m = Pattern.compile(REGEX_SEPARATOR).matcher(spannable);

        int start = 0;
        while(m.find()) {
            CharSequence paragraph = spannable.subSequence(start, m.start());
            // Include the lines that does not contains the image markup for displaying attached images.
            if(!Pattern.compile(REGEX_MARKUP).matcher(paragraph).matches()) {
                log.i("Paragraph: %s, %s, %s", paragraph, start, m.start());
                spannable.setSpan(new LeadingMarginSpan.Standard(25), start, m.start(), SPANNED_FLAG);
            }

            start = m.end();
        }

        // Handle the last charSequence after the last line separator in the text because the while
        // looping makes paragraph the second last charSequence which ends at the last line separator.
        if(start < spannable.length()) {
            spannable.setSpan(new LeadingMarginSpan.Standard(25), start, spannable.length(), SPANNED_FLAG);
        }

        log.i("start: %s, %s", start, spannable.length());
        if(start == 0) {
            spannable.insert(start, "  ");
            spannable.setSpan(new ForegroundColorSpan(Color.RED), start, spannable.length(), SPANNED_FLAG);
        }
        log.i("Spannable: %s", spannable);
        return spannable;
    }

    // On fetching images by AttachedBitmapTask and being notified by ImageViewModel, set ImageSpans
    // to the margin-formatted text.
    @SuppressWarnings("ConstantConditiosn")
    private SpannableStringBuilder createImageSpanString(SparseArray<ImageSpan> spanArray) {

        //SpannableStringBuilder ssb = new SpannableStringBuilder(spannable);
        String regexMarkup = "\\[image_\\d]";
        final Matcher markup = Pattern.compile(regexMarkup).matcher(spannable);

        int key = 0;
        while(markup.find()) {
            if(spanArray.get(key) != null) {
                spannable.setSpan(spanArray.get(key), markup.start(), markup.end(), SPANNED_FLAG);

            } else {
                log.i("Failed to set Span");
            }

            key++;
        }

        // Feed the line separator rigth before and after the image span
        ImageSpan[] arrSpans = spannable.getSpans(0, spannable.length(), ImageSpan.class);
        for(ImageSpan span : arrSpans) {
            spannable.insert(spannable.getSpanStart(span) - 1, "\n");
            spannable.insert(spannable.getSpanEnd(span) + 1, "\n");
        }

        return spannable;
    }

}
