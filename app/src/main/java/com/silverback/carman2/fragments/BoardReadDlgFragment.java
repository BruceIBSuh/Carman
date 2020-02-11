package com.silverback.carman2.fragments;


import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.MetadataChanges;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;
import com.google.firebase.firestore.Transaction;
import com.silverback.carman2.BoardActivity;
import com.silverback.carman2.R;
import com.silverback.carman2.adapters.BoardCommentAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.threads.AttachedBitmapTask;
import com.silverback.carman2.utils.Constants;
import com.silverback.carman2.utils.PaginationHelper;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.content.Context.INPUT_METHOD_SERVICE;

/**
 * A simple {@link Fragment} subclass.
 */
public class BoardReadDlgFragment extends DialogFragment implements PaginationHelper.OnPaginationListener {

    private static final LoggingHelper log = LoggingHelperFactory.create(BoardReadDlgFragment.class);

    // Constants
    private static final int STATE_COLLAPSED = 0;
    private static final int STATE_EXPANDED = 1;
    private static final int STATE_IDLE = 2;

    // Objects
    private FirebaseFirestore firestore;
    private Context context;
    private DocumentSnapshot document;
    private BoardCommentAdapter commentAdapter;
    //private SpannableStringBuilder spannable;
    //private ImageViewModel imageModel;
    private String postTitle, postContent, userName, userPic;
    private List<String> imgUriList;
    private List<Integer> viewIdList;
    private AttachedBitmapTask bitmapTask;
    private List<Bitmap> bmpList;
    private SharedPreferences mSettings;
    private List<DocumentSnapshot> snapshotList;
    private Uri uriUserImage;

    // UIs
    private AppBarLayout appbarLayout;
    private ConstraintLayout constraintHeader, constraintPosting;
    private Toolbar toolbar;
    private View underline;
    private RecyclerView recyclerComment;
    private EditText etComment;
    private TextView tvCompathyCnt, tvCommentCnt;

    private ImageView attachedImage;

    // Fields
    private String tabTitle;
    private String autoData;
    private String userId, documentId;
    private int cntImages;
    private int tabPage;
    private int appbarOffset;
    private int cntComment, cntCompathy;
    private int mCurrentState;
    private boolean isCommentVisible;

    public BoardReadDlgFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.context = getContext();

        firestore = FirebaseFirestore.getInstance();
        snapshotList = new ArrayList<>();
        //sdf = new SimpleDateFormat("MM.dd HH:mm", Locale.getDefault());

        if(getArguments() != null) {
            tabPage = getArguments().getInt("tabPage");//for displaying the title of viewpager page.
            postTitle = getArguments().getString("postTitle");
            postContent = getArguments().getString("postContent");
            userName = getArguments().getString("userName");
            userPic = getArguments().getString("userPic");
            imgUriList = getArguments().getStringArrayList("uriImgList");
            userId = getArguments().getString("userId");
            cntComment = getArguments().getInt("cntComment");
            cntCompathy = getArguments().getInt("cntCompahty");
            documentId = getArguments().getString("documentId");
            log.i("DocumentID: %s, %s", tabPage, documentId);
        }

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

        if(TextUtils.isEmpty(json)) autoData = null;
        else {
            try {
                JSONArray jsonArray = new JSONArray(json);
                autoData = jsonArray.optString(0) + ", "
                        + jsonArray.optString(1) + ", "
                        + jsonArray.optString(2) + ", "
                        + jsonArray.optString(3);

            } catch (JSONException e) {
                log.e("JSONException: %s", e.getMessage());
            }
        }
    }


    @SuppressWarnings("ConstantConditions")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View localView = inflater.inflate(R.layout.dialog_board_read, container, false);

        appbarLayout = localView.findViewById(R.id.appbar_board_read);
        toolbar = localView.findViewById(R.id.toolbar_board_read);
        constraintHeader = localView.findViewById(R.id.constraint_header);
        constraintPosting = localView.findViewById(R.id.constraint_posting);
        ConstraintLayout commentLayout = localView.findViewById(R.id.constraint_comment);
        TextView tvTitle = localView.findViewById(R.id.tv_post_title);
        TextView tvUserName = localView.findViewById(R.id.tv_username);
        TextView tvAutoInfo = localView.findViewById(R.id.tv_autoinfo);
        TextView tvDate = localView.findViewById(R.id.tv_posting_date);
        ImageView imgUserPic = localView.findViewById(R.id.img_userpic);
        etComment = localView.findViewById(R.id.et_comment);
        ImageButton btnSendComment = localView.findViewById(R.id.imgbtn_comment);
        Button btnComment = localView.findViewById(R.id.btn_comment);
        Button btnCompathy = localView.findViewById(R.id.btn_compathy);
        tvCommentCnt = localView.findViewById(R.id.tv_cnt_comment);
        tvCompathyCnt = localView.findViewById(R.id.tv_cnt_compathy);
        underline = localView.findViewById(R.id.view_underline_header);
        recyclerComment = localView.findViewById(R.id.recycler_comments);

        // Set the stand-alone toolabr which works in the same way that the action bar does in most
        // cases, but you do not set the toolbar to act as the action bar. In standalone mode, you
        // need to manually populate the toolbar with content and actions as folowing.
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener(){
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch(item.getItemId()) {
                    case R.id.menu_board_read_close:
                        dismiss();//close the dialogfragment
                        break;
                    case R.id.menu_board_read_spam:
                        break;
                }
                return true;
            }
        });
        toolbar.inflateMenu(R.menu.menu_board_read);
        tabTitle = getResources().getStringArray(R.array.board_tab_title)[tabPage];
        toolbar.setTitle(tabTitle);

        // Implements the abstract method of AppBarStateChangeListener to be notified of the state
        // of appbarlayout as it is scrolling, which changes the toolbar title and icon by the
        // scroling state.
        appbarLayout.addOnOffsetChangedListener(new AppBarStateChangeListener() {
            @Override
            public void onStateChanged(AppBarLayout appBarLayout, int state) {
                setToolbarTitleIcon(state);
            }
        });

        tvTitle.setText(postTitle);
        tvUserName.setText(userName);
        tvAutoInfo.setText(autoData);
        tvDate.setText(getArguments().getString("timestamp"));
        tvCommentCnt.setText(String.valueOf(cntComment));
        tvCompathyCnt.setText(String.valueOf(cntCompathy));

        // RecyclerView for showing comments
        recyclerComment.setLayoutManager(new LinearLayoutManager(context));
        commentAdapter = new BoardCommentAdapter(snapshotList);
        recyclerComment.setAdapter(commentAdapter);

        // Pagination using PaginationHelper which requires refactor.
        PaginationHelper pagingUtil = new PaginationHelper();
        pagingUtil.setOnPaginationListener(this);
        recyclerComment.addOnScrollListener(pagingUtil);
        pagingUtil.setCommentQuery("timestamp", documentId, Constants.PAGINATION);

        // Event handler for clicking buttons
        //btnDismiss.setOnClickListener(view -> dismiss());
        btnComment.setOnClickListener(view -> {
            if(isCommentVisible) commentLayout.setVisibility(View.INVISIBLE);
            else commentLayout.setVisibility(View.VISIBLE);
            isCommentVisible = !isCommentVisible;
        });

        btnCompathy.setOnClickListener(view -> {
            firestore.collection("board_general").document(documentId)
                    .update("cnt_compathy", FieldValue.increment(1));
        });

        // Upload the comment to Firestore, which needs to refactor for filtering text.
        btnSendComment.setOnClickListener(view -> {
            if(TextUtils.isEmpty(etComment.getText())) {
                Snackbar.make(localView, "Emapty comment", Snackbar.LENGTH_SHORT).show();
                return;
            }

            // On finishing upload, close the soft input and the comment view.
            if(uploadComment()) {
                // Close the soft input mehtod when clicking the upload button
                ((InputMethodManager)(getActivity().getSystemService(INPUT_METHOD_SERVICE)))
                        .hideSoftInputFromWindow(localView.getWindowToken(), 0);

                // Make the comment view invisible
                commentLayout.setVisibility(View.INVISIBLE);
                isCommentVisible = !isCommentVisible;
            }
        });

        // Realtime update of the comment count and compathy count using SnapshotListener.
        final DocumentReference docRef = firestore.collection("board_general").document(documentId);
        docRef.addSnapshotListener(MetadataChanges.INCLUDE, (snapshot, e) -> {
            if(e != null) return;
            if(snapshot != null && snapshot.exists()) {
                long countComment = snapshot.getLong("cnt_comment");
                long countCompathy = snapshot.getLong("cnt_compathy");
                tvCommentCnt.setText(String.valueOf(countComment));
                tvCompathyCnt.setText(String.valueOf(countCompathy));
            }
        });


        // Rearrange the text by paragraphs
        createContentView(postContent);

        // Set the user image
        uriUserImage = Uri.parse(userPic);
        Glide.with(context)
                .asBitmap()
                .load(uriUserImage)
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



    // Implement the callback of AppbarLayout.OnOffsetChangedListene
    /*
    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
        log.i("onOffsetChanged: %s", verticalOffset);
        //if(Math.abs(verticalOffset) == constraintHeader.getHeight()) {

        // AppBarLayout collapsed
        if(verticalOffset == 0) {
            toolbar.setTitle(tabTitle);
            toolbar.setSubtitle("");
            toolbar.setLogo(null);

        // AppBarLayout expanded
        } else if(Math.abs(verticalOffset) >= appBarLayout.getTotalScrollRange()) {
            toolbar.setTitle(postTitle);


        } else {

        }
    }
    */



    // The following 3 callbacks are invoked by PaginationHelper to query a collection reference
    // up to the limit and on showing the last one, another query get started.
    @Override
    public void setFirstQuery(QuerySnapshot snapshot) {
        for(DocumentSnapshot document : snapshot) snapshotList.add(document);
        commentAdapter.notifyDataSetChanged();
    }
    @Override
    public void setNextQueryStart(boolean b) {
        // Set the visibility of Progressbar to visible.
    }
    @Override
    public void setNextQueryComplete(QuerySnapshot querySnapshot) {
        for(DocumentSnapshot document : querySnapshot) snapshotList.add(document);
        //pagingProgressBar.setVisibility(View.INVISIBLE);
        commentAdapter.notifyDataSetChanged();
    }

    // Method for uploading the comment to Firestore.
    private boolean uploadComment() {

        Calendar calendar = Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault());
        Date date = calendar.getTime();
        log.i("date: %s", date);

        Map<String, Object> comment = new HashMap<>();
        comment.put("comment", etComment.getText().toString());
        comment.put("user", userId);
        //comment.put("timestamp", FieldValue.serverTimestamp());
        comment.put("timestamp", new Timestamp(date));


        // First, get the document with a given id, then add data
        final DocumentReference documentRef = firestore.collection("board_general").document(documentId);
        documentRef.get().addOnSuccessListener(document -> {
            if(document.exists()) {
                final CollectionReference colRef = document.getReference().collection("comments");
                colRef.add(comment).addOnSuccessListener(commentDoc -> {

                    // increase the cnt_cooment in the parent document.
                    documentRef.update("cnt_comment", FieldValue.increment(1));

                    // Update the recycler adapter to enlist the pending comment. Don't have to use
                    // SnapshotListener, even thoug it may not update the RecyclerView of other users
                    // simultaneously.
                    Source source = Source.CACHE;
                    commentDoc.get(source).addOnSuccessListener(commentSnapshot -> {
                        snapshotList.add(0, commentSnapshot);
                        commentAdapter.notifyItemInserted(0);
                    });

                }).addOnFailureListener(e -> log.e("Add comments failed"));
            }
        });



        return true;
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

    // Display the view of contents and images in ConstraintLayout which is dynamically created
    // using ConstraintSet. Images are managed by Glide.
    // The regular expression makes text and images split with the markup which was made when images
    // were inserted. While looping the content, split parts of text and image are conntected to
    // ConstraintSets which are applied to the parent ConstraintLayout.
    // The recyclerview which displays comments at the bottom should be coordinated according to
    // whether the content has images or not.
    private void createContentView(String text) {
        // When an image is attached as the post writes, the line separator is supposed to put in at
        // before and after the image. That's why the regex contains the line separator in order to
        // get the right end position.
        final String REGEX_MARKUP = "\\[image_\\d]\\n";
        final Matcher m = Pattern.compile(REGEX_MARKUP).matcher(text);

        int index = 0; //
        int start = 0;
        int constraintId = constraintPosting.getId();
        int topConstraint;
        int prevImageId = 0;

        // Create LayoutParams using LinearLayout(RelativeLayout).LayoutParams, not using Constraint
        // Layout.LayoutParams.
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        // If the content contains images, which means the markup(s) exists in the content, the content
        // is split into part of text or image and is respectively connected to ConstraintSet.
        while(m.find()) {
            String paragraph = text.substring(start, m.start());
            TextView tv = new TextView(context);
            tv.setId(View.generateViewId());
            tv.setText(paragraph);
            constraintPosting.addView(tv, params);
            topConstraint = (start == 0) ? underline.getId() : prevImageId;

            ConstraintSet tvSet = new ConstraintSet();
            tvSet.clone(constraintPosting);
            tvSet.connect(tv.getId(), ConstraintSet.START, constraintId, ConstraintSet.START, 16);
            tvSet.connect(tv.getId(), ConstraintSet.END, constraintId, ConstraintSet.END, 16);
            tvSet.connect(tv.getId(), ConstraintSet.TOP, topConstraint, ConstraintSet.BOTTOM, 32);
            tvSet.applyTo(constraintPosting);

            ImageView imgView = new ImageView(context);
            imgView.setBackgroundColor(Color.RED);
            imgView.setId(View.generateViewId());
            prevImageId = imgView.getId();
            constraintPosting.addView(imgView, params);

            ConstraintSet imgSet = new ConstraintSet();
            imgSet.clone(constraintPosting);
            imgSet.connect(imgView.getId(), ConstraintSet.START, constraintId, ConstraintSet.START, 0);
            imgSet.connect(imgView.getId(), ConstraintSet.END, constraintId, ConstraintSet.END, 0);
            imgSet.connect(imgView.getId(), ConstraintSet.TOP, tv.getId(), ConstraintSet.BOTTOM, 16);
            imgSet.applyTo(constraintPosting);

            // Image rotation issue occurred!
            Glide.with(context)
                    .asBitmap()
                    .load(imgUriList.get(index))
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .fitCenter()
                    .into(imgView);

            start = m.end();
            index++;

        }


        // Corrdinate the position b/w the last part, no matter what is image or text in the content,
        // and the following recycler view by the patterns.

        log.i("start position: %s", start);
        // No imaage attached.
        if(start == 0) {
            TextView noImageText = new TextView(context);
            noImageText.setId(View.generateViewId());
            noImageText.setText(text);
            constraintPosting.addView(noImageText, params);

            ConstraintSet tvSet = new ConstraintSet();
            tvSet.clone(constraintPosting);
            tvSet.connect(noImageText.getId(), ConstraintSet.START, constraintId, ConstraintSet.START, 16);
            tvSet.connect(noImageText.getId(), ConstraintSet.END, constraintId, ConstraintSet.END, 16);
            tvSet.connect(noImageText.getId(), ConstraintSet.TOP, underline.getId(), ConstraintSet.BOTTOM, 0);
            //tvSet.connect(noImageText.getId(), ConstraintSet.TOP, constraintId, ConstraintSet.BOTTOM, 0);
            //tvSet.connect(recyclerComment.getId(), ConstraintSet.TOP, noImageText.getId(), ConstraintSet.BOTTOM, 32);

            tvSet.applyTo(constraintPosting);

        // Text exists after the last image. The last TextView is constrained to the previous ImageView
        // and the RecyclerView constrained to the TextView.
        } else if(start < text.length()) {
            String lastParagraph = text.substring(start);
            log.i("Last Paragraph: %s", lastParagraph);
            TextView lastView = new TextView(context);
            lastView.setId(View.generateViewId());
            lastView.setText(lastParagraph);
            constraintPosting.addView(lastView, params);

            ConstraintSet tvSet = new ConstraintSet();
            tvSet.clone(constraintPosting);
            tvSet.connect(lastView.getId(), ConstraintSet.START, constraintId, ConstraintSet.START, 16);
            tvSet.connect(lastView.getId(), ConstraintSet.END, constraintId, ConstraintSet.END, 16);
            tvSet.connect(lastView.getId(), ConstraintSet.TOP, prevImageId, ConstraintSet.BOTTOM, 0);
            tvSet.connect(recyclerComment.getId(), ConstraintSet.TOP, lastView.getId(), ConstraintSet.BOTTOM, 32);
            tvSet.applyTo(constraintPosting);

        // In case no text exists after the last image, the RecyclerView is constrained to the last
        // ImageView.
        } else if(start == text.length()) {
            ConstraintSet recyclerSet = new ConstraintSet();
            recyclerSet.clone(constraintPosting);
            recyclerSet.connect(recyclerComment.getId(), ConstraintSet.TOP, prevImageId, ConstraintSet.BOTTOM, 32);
            recyclerSet.applyTo(constraintPosting);
        }

    }


    // This abstract class notifies the state of the appbarlayout by implementing the listener.
    // The reason that the listener should be implemented first is that the listener notifies every
    // scrolling changes which keep the view being invalidated. The abstract class may, in turn,
    // receive changes and only notifies the specified state to the view.
    abstract class AppBarStateChangeListener implements AppBarLayout.OnOffsetChangedListener {

        int mCurrentState = STATE_IDLE;

        @Override
        public final void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {

            if (verticalOffset == 0) {
                if (mCurrentState != STATE_EXPANDED) {
                    onStateChanged(appBarLayout, STATE_EXPANDED);
                }
                mCurrentState = STATE_EXPANDED;
            } else if (Math.abs(verticalOffset) >= appBarLayout.getTotalScrollRange()) {
                if (mCurrentState != STATE_COLLAPSED) {
                    onStateChanged(appBarLayout, STATE_COLLAPSED);
                }
                mCurrentState = STATE_COLLAPSED;
            } else {
                log.i("vertical offset: %s", verticalOffset);
                //if (mCurrentState != STATE_IDLE) {
                if(appbarOffset != verticalOffset) {
                    appbarOffset = verticalOffset;
                    onStateChanged(appBarLayout, STATE_IDLE);
                }
                mCurrentState = STATE_IDLE;
            }
        }


        abstract void onStateChanged(AppBarLayout appBarLayout, int state);
    }

    // Set the toolbar Icon and title as the appbarlayout is scrolling, which is notified by the
    // the abstract class of AppBarStateChangeListener.
    private void setToolbarTitleIcon(int state) {

        SpannableString spannable = new SpannableString(postTitle);
        int size = Math.abs(appbarOffset) / 6;
        spannable.setSpan(new AbsoluteSizeSpan(size), 0, spannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        toolbar.setTitle(spannable);

        switch(state) {
            case STATE_COLLAPSED:
                toolbar.setTitle(spannable);
                toolbar.setSubtitle(userName);

                Glide.with(context).load(uriUserImage).override(toolbar.getHeight() - 15).circleCrop()
                        .into(new CustomTarget<Drawable>(){
                            @Override
                            public void onResourceReady(
                                    @NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                                toolbar.setLogo(resource);
                                toolbar.setContentInsetStartWithNavigation(0);
                            }

                            @Override
                            public void onLoadCleared(@Nullable Drawable placeholder) {}
                        });
                break;

            case STATE_EXPANDED:
                toolbar.setTitle(tabTitle);
                toolbar.setSubtitle("");
                toolbar.setLogo(null);
                break;

            case STATE_IDLE:
                log.i("STATE_IDLE");
                break;

        }
    }


    // Divide the text by line separator("\n"), excluding image lines("[image]"), then set the span
    // for making the leading margin to every single line. Alternative solution but not applied here.
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
