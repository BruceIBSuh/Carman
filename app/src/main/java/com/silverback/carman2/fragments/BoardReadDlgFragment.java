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
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.silverback.carman2.BoardActivity;
import com.silverback.carman2.R;
import com.silverback.carman2.adapters.BoardCommentAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.ImageViewModel;
import com.silverback.carman2.threads.AttachedBitmapTask;
import com.silverback.carman2.utils.Constants;
import com.silverback.carman2.utils.PaginationHelper;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A simple {@link Fragment} subclass.
 */
public class BoardReadDlgFragment extends DialogFragment implements PaginationHelper.OnPaginationListener{

    private static final LoggingHelper log = LoggingHelperFactory.create(BoardReadDlgFragment.class);

    // Constants
    private final int SPANNED_FLAG = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;
    private final int LEADING = 20;
    private final int QUERY_LIMIT = 25;


    // Objects
    private FirebaseFirestore firestore;
    private Context context;
    private BoardCommentAdapter commentAdapter;
    private List<DocumentSnapshot> snapshotList;
    private PaginationHelper paginationHelper;
    //private SpannableStringBuilder spannable;
    //private ImageViewModel imageModel;
    private String postTitle, postContent, userName, userPic;
    private List<String> imgUriList;
    //private List<Integer> viewIdList;
    //private AttachedBitmapTask bitmapTask;
    //private List<Bitmap> bmpList;
    private SharedPreferences mSettings;

    // UIs
    private ConstraintLayout constraintLayout;
    private View underline;
    private RecyclerView recyclerComment;

    //private ImageView attachedImage;

    // Fields
    private StringBuilder autoData;
    private String userId, documentId;
    //private int cntImages;
    private boolean isCommentVisible;

    public BoardReadDlgFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.context = getContext();

        firestore = FirebaseFirestore.getInstance();
        //imageModel = ViewModelProviders.of(this).get(ImageViewModel.class);
        //sdf = new SimpleDateFormat("MM.dd HH:mm", Locale.getDefault());
        snapshotList = new ArrayList<>();

        if(getArguments() != null) {
            postTitle = getArguments().getString("postTitle");
            postContent = getArguments().getString("postContent");
            userName = getArguments().getString("userName");
            userPic = getArguments().getString("userPic");
            imgUriList = getArguments().getStringArrayList("imageUriList");
            userId = getArguments().getString("userId");
            documentId = getArguments().getString("documentId");
        }

        // Instantiate PaginationHelper and attach the listener for paing comments.
        paginationHelper = new PaginationHelper();
        paginationHelper.setOnPaginationListener(this);
        paginationHelper.setCommentQuery(documentId, "timestamp", QUERY_LIMIT);

        // Separate the text by line feeder("\n") to set the leading margin span to it, then return
        // a margin-formatted spannable string, which, in turn, set the image spans to display
        // attached images as images are notified to retrieve by the task.
        //spannable = translateParagraphSpan(postContent);

        // Initiate the task to fetch images attached to the post
        /*
        if(imgUriList != null && imgUriList.size() > 0) {
            bitmapTask = ThreadManager.startAttachedBitmapTask(context, imgUriList, imageModel);
        }
        */


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
        ConstraintLayout commentLayout = localView.findViewById(R.id.constraint_comment);
        TextView tvTitle = localView.findViewById(R.id.tv_post_title);
        TextView tvUserName = localView.findViewById(R.id.tv_comment_username);
        TextView tvAutoInfo = localView.findViewById(R.id.tv_autoinfo);
        TextView tvDate = localView.findViewById(R.id.tv_posting_date);
        ImageView imgUserPic = localView.findViewById(R.id.img_userpic);
        EditText etComment = localView.findViewById(R.id.et_comment);
        ImageButton btnDismiss = localView.findViewById(R.id.imgbtn_dismiss);
        ImageButton btnSendComment = localView.findViewById(R.id.imgbtn_comment);
        Button btnComment = localView.findViewById(R.id.btn_comment);
        underline = localView.findViewById(R.id.view_underline_header);
        recyclerComment = localView.findViewById(R.id.recycler_board_comments);

        tvTitle.setText(postTitle);
        tvUserName.setText(userName);
        tvAutoInfo.setText(autoData.toString());
        tvDate.setText(getArguments().getString("timestamp"));

        recyclerComment.setLayoutManager(new LinearLayoutManager(context));
        recyclerComment.addOnScrollListener(paginationHelper);
        commentAdapter = new BoardCommentAdapter(snapshotList);
        recyclerComment.setAdapter(commentAdapter);


        btnDismiss.setOnClickListener(view -> dismiss());

        //
        btnComment.setOnClickListener(view -> {
            if(isCommentVisible) commentLayout.setVisibility(View.INVISIBLE);
            else commentLayout.setVisibility(View.VISIBLE);
            isCommentVisible = !isCommentVisible;
        });

        // Upload the comment to Firestore, which needs to refactor for filtering text.
        btnSendComment.setOnClickListener(view -> {
            if(TextUtils.isEmpty(etComment.getText())) {
                Snackbar.make(localView, "Emapty comment", Snackbar.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> comment = new HashMap<>();
            comment.put("comment", etComment.getText().toString());
            comment.put("user", userId);
            comment.put("timestamp", FieldValue.serverTimestamp());

            firestore.collection("board_general").document(documentId).get()
                    .addOnSuccessListener(snapshot -> {
                        if(snapshot.exists()) {
                            snapshot.getReference().collection("comments").add(comment)
                                    .addOnSuccessListener(doc -> log.i("DocumentID: %s", doc.getId()))
                                    .addOnFailureListener(e -> log.e("Add comments failed"));
                        }
                    });
        });


        createParagraphView(postContent);


        // Set the user image
        Uri uriUserPic = Uri.parse(userPic);
        Glide.with(context)
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

    // The following 3 callbacks are invoked by PaginationHelper.OnPaginationListener which
    // notifies the adapter of the first and the next query result.
    @Override
    public void setFirstQuery(QuerySnapshot snapshot) {
        for(DocumentSnapshot document : snapshot) snapshotList.add(document);
        commentAdapter.notifyDataSetChanged();
    }
    @Override
    public void setNextQueryStart(boolean b) {
        //pagingProgressBar.setVisibility(View.VISIBLE);
    }
    @Override
    public void setNextQueryComplete(QuerySnapshot querySnapshot) {
        for(DocumentSnapshot document : querySnapshot) snapshotList.add(document);
        //pagingProgressBar.setVisibility(View.INVISIBLE);
        commentAdapter.notifyDataSetChanged();
    }

    /*
    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);

        // Set the image span to the post content as the image span instances are retrieved from
        // AttachedBitmapTask.
        imageModel.getImageSpanArray().observe(getViewLifecycleOwner(), spanArray -> {
            //log.i("ImageSpan: %s", spanArray.keyAt(0));
            //SpannableStringBuilder imgSpannable = createImageSpanString(spanArray);
            //tvContent.setText(imgSpannable);

        });
    }

    @Override
    public void onPause() {
        super.onPause();
        if(bitmapTask != null) bitmapTask = null;
    }
     */


    private void createParagraphView(String text) {

        // When an image is attached as the post writes, the line separator is supposed to put in at
        // before and after the image. That's why the regex contains the line separator in order to
        // get the right end position.
        final String REGEX_MARKUP = "\\[image_\\d]\\n";
        final Matcher m = Pattern.compile(REGEX_MARKUP).matcher(text);

        final int MARGIN_TOP = 64;
        int index = 0;
        int start = 0;
        int constraintId = constraintLayout.getId();
        int topConstraint = 0;
        int prevImageId = 0;

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);

        while(m.find()) {
            String paragraph = text.substring(start, m.start());
            TextView tv = new TextView(context);
            tv.setId(View.generateViewId());
            tv.setText(paragraph);
            constraintLayout.addView(tv, params);
            topConstraint = (start == 0) ? underline.getId() : prevImageId;

            ConstraintSet tvSet = new ConstraintSet();
            tvSet.clone(constraintLayout);
            tvSet.connect(tv.getId(), ConstraintSet.START, constraintId, ConstraintSet.START, 16);
            tvSet.connect(tv.getId(), ConstraintSet.END, constraintId, ConstraintSet.END, 16);
            tvSet.connect(tv.getId(), ConstraintSet.TOP, topConstraint, ConstraintSet.BOTTOM, 16);
            tvSet.applyTo(constraintLayout);

            ImageView imgView = new ImageView(context);
            imgView.setBackgroundColor(Color.RED);
            imgView.setId(View.generateViewId());
            prevImageId = imgView.getId();
            constraintLayout.addView(imgView, params);

            ConstraintSet imgSet = new ConstraintSet();
            imgSet.clone(constraintLayout);
            imgSet.connect(imgView.getId(), ConstraintSet.START, constraintId, ConstraintSet.START, 0);
            imgSet.connect(imgView.getId(), ConstraintSet.END, constraintId, ConstraintSet.END, 0);
            imgSet.connect(imgView.getId(), ConstraintSet.TOP, tv.getId(), ConstraintSet.BOTTOM, 16);
            imgSet.applyTo(constraintLayout);

            Glide.with(context)
                    .asBitmap()
                    .load(imgUriList.get(index))
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .fitCenter()
                    .into(imgView);

            start = m.end();
            index++;

        }


        // No image attached.
        if(start == 0) {
            TextView noImageText = new TextView(context);
            noImageText.setId(View.generateViewId());
            noImageText.setText(text);
            constraintLayout.addView(noImageText, params);

            ConstraintSet tvSet = new ConstraintSet();
            tvSet.clone(constraintLayout);
            tvSet.connect(noImageText.getId(), ConstraintSet.START, constraintId, ConstraintSet.START, 16);
            tvSet.connect(noImageText.getId(), ConstraintSet.END, constraintId, ConstraintSet.END, 16);
            tvSet.connect(noImageText.getId(), ConstraintSet.TOP, underline.getId(), ConstraintSet.BOTTOM, 0);
            tvSet.connect(recyclerComment.getId(), ConstraintSet.TOP, noImageText.getId(), ConstraintSet.BOTTOM, MARGIN_TOP);

            tvSet.applyTo(constraintLayout);

        // Text exists after the last image. The last TextView is constrained to the previous ImageView
        // and the RecyclerView constrained to the TextView.
        } else if(start < text.length()) {
            String lastParagraph = text.substring(start);
            log.i("Last Paragraph: %s", lastParagraph);
            TextView lastView = new TextView(context);
            lastView.setId(View.generateViewId());
            lastView.setText(lastParagraph);
            constraintLayout.addView(lastView, params);

            ConstraintSet tvSet = new ConstraintSet();
            tvSet.clone(constraintLayout);
            tvSet.connect(lastView.getId(), ConstraintSet.START, constraintId, ConstraintSet.START, 16);
            tvSet.connect(lastView.getId(), ConstraintSet.END, constraintId, ConstraintSet.END, 16);
            tvSet.connect(lastView.getId(), ConstraintSet.TOP, prevImageId, ConstraintSet.BOTTOM, 0);
            tvSet.connect(recyclerComment.getId(), ConstraintSet.TOP, lastView.getId(), ConstraintSet.BOTTOM, MARGIN_TOP);
            tvSet.applyTo(constraintLayout);

        // In case no text exists after the last image, the RecyclerView is constrained to the last
        // ImageView.
        } else if(start == text.length()) {
            ConstraintSet recyclerSet = new ConstraintSet();
            recyclerSet.clone(constraintLayout);
            recyclerSet.connect(recyclerComment.getId(), ConstraintSet.TOP, prevImageId, ConstraintSet.BOTTOM, MARGIN_TOP);
            recyclerSet.applyTo(constraintLayout);
        }

    }




    // Divide the text by line separator("\n"), excluding image lines("[image]"), then set the span
    // for making the leading margin to every single line.
    /*
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
                spannable.setSpan(new LeadingMarginSpan.Standard(LEADING), start, m.start(), SPANNED_FLAG);
            }

            start = m.end();
        }

        log.i("start: %s, %s", start, spannable.length());
        if(start == 0) {
            spannable.setSpan(new LeadingMarginSpan.Standard(LEADING), start, spannable.length(),
                    Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        }
        // Handle the last charSequence after the last line separator in the text because the while
        // looping makes paragraph the second last charSequence which ends at the last line separator.
        else if(start < spannable.length()) {
            spannable.setSpan(new LeadingMarginSpan.Standard(LEADING), start, spannable.length(), SPANNED_FLAG);
        }

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

    class CustomLeadingMarginSpan extends LeadingMarginSpan.Standard {

        public CustomLeadingMarginSpan(int every) {
            super(every);
        }

        @Override
        public void drawLeadingMargin (Canvas c, Paint p,
                                       int x, int dir, int top, int baseline, int bottom,
                                       CharSequence text, int start, int end,
                                       boolean first,
                                       Layout layout) {
            log.i("Layout: %s", layout);
        }
    }
    */

}
