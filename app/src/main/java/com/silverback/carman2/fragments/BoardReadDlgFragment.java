package com.silverback.carman2.fragments;


import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.MetadataChanges;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;
import com.silverback.carman2.BoardActivity;
import com.silverback.carman2.R;
import com.silverback.carman2.adapters.BoardCommentAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.viewmodels.FragmentSharedModel;
import com.silverback.carman2.viewmodels.ImageViewModel;
import com.silverback.carman2.utils.ApplyImageResourceUtil;
import com.silverback.carman2.utils.Constants;
import com.silverback.carman2.utils.QueryAndPagingHelper;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
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
 * This dialogfragment reads a post content when tapping  an item recycled in BoardPagerFragment.
 */
public class BoardReadDlgFragment extends DialogFragment implements
        View.OnClickListener,
        QueryAndPagingHelper.OnPaginationListener {

    private static final LoggingHelper log = LoggingHelperFactory.create(BoardReadDlgFragment.class);

    // Constants
    private static final int STATE_COLLAPSED = 0;
    private static final int STATE_EXPANDED = 1;
    private static final int STATE_IDLE = 2;

    // Objects
    private Context context;
    private OnEditModeListener mListener;
    private FirebaseFirestore firestore;
    private DocumentReference postRef;
    private Source source;
    private ApplyImageResourceUtil imgUtil;
    private ImageViewModel imgViewModel;
    private FragmentSharedModel sharedModel;
    private BoardCommentAdapter commentAdapter;
    private String postTitle, postContent, userName, userPic;
    private List<String> imgUriList;
    private SharedPreferences mSettings;
    private List<DocumentSnapshot> snapshotList;
    private List<CharSequence> autoclub;


    // UIs
    private View localView;
    private ConstraintLayout constPostingLayout, constCommentLayout;
    private Toolbar toolbar;
    private View underline;
    private RecyclerView recyclerComment;
    private EditText etComment;
    private TextView tvCompathyCnt, tvCommentCnt;


    // Fields
    private SpannableStringBuilder autoTitle;
    private String tabTitle;
    private String autoClub;
    private String userId, documentId;
    private int tabPage;
    //private int position;
    private int appbarOffset;
    private int cntComment, cntCompathy;
    private boolean isCommentVisible;
    private boolean hasCompathy;

    // Interface to notify BoardActivity of pressing the edit menu in the toolbar which is visible
    // only when a user reads his/her own post, comparing the ids of the user and board_general
    // collection.
    public interface OnEditModeListener {
        void onEditClicked(Bundle bundle);
    }

    // Set the interface listener to BoardActivity at the lifecycle of onAttachFragment.
    public void setEditModeListener(OnEditModeListener listener) {
        mListener = listener;
    }

    // Constructor default.
    public BoardReadDlgFragment() {
        // Required empty public constructor
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.context = getContext();

        firestore = FirebaseFirestore.getInstance();
        snapshotList = new ArrayList<>();
        imgUtil = new ApplyImageResourceUtil(getContext());
        imgViewModel = new ViewModelProvider(requireActivity()).get(ImageViewModel.class);
        sharedModel = new ViewModelProvider(requireActivity()).get(FragmentSharedModel.class);

        if(getArguments() != null) {
            tabPage = getArguments().getInt("tabPage");//for displaying the title of viewpager page.
            //position = getArguments().getInt("position");
            postTitle = getArguments().getString("postTitle");
            postContent = getArguments().getString("postContent");
            userName = getArguments().getString("userName");
            userPic = getArguments().getString("userPic");
            autoClub = getArguments().getString("autoClub");
            imgUriList = getArguments().getStringArrayList("uriImgList");
            userId = getArguments().getString("userId");
            cntComment = getArguments().getInt("cntComment");
            cntCompathy = getArguments().getInt("cntCompahty");
            documentId = getArguments().getString("documentId");
            autoclub = getArguments().getCharSequenceArrayList("autoclub");
            log.i("DocumentID: %s, %s, %s", tabPage, documentId, autoclub);
        }

        // Get the auto data arguemnt from BoardPagerFragment, which is of JSON string tyepe and
        // it requires to create JSONArray that may be converted to StringBuilder.
        /*
        if(!TextUtils.isEmpty(autoClub)) {
            try {
                JSONArray jsonArray = new JSONArray(autoClub);
                StringBuilder sb = new StringBuilder();
                for(int i = 0; i < jsonArray.length(); i++)
                    sb.append(jsonArray.optString(i)).append(" ");
                autoClub = sb.toString();
                log.i("autoClub in readposing: %s", autoClub);

            } catch(JSONException e) {
                e.printStackTrace();
            }

        } else autoClub = null;
         */


        // Get the current document reference which should be shared in the fragment.
        // Initially attach SnapshotListener to have the comment collection updated, then remove
        // the listener to prevent connecting to the server. Instead, update the collection using
        // Source.Cache.
        postRef = firestore.collection("board_general").document(documentId);
        postRef.get().addOnSuccessListener(aVoid -> {
            ListenerRegistration commentListener = postRef.collection("comments")
                    .addSnapshotListener((querySnapshot, e) -> {
                        if(e != null) return;
                        source = (querySnapshot != null && querySnapshot.getMetadata().hasPendingWrites())
                                ? Source.CACHE : Source.SERVER;
                        log.i("BoardRadDlgFragment Source: %s", source);
                    });

            commentListener.remove();
        });

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



    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        localView = inflater.inflate(R.layout.dialog_board_read, container, false);

        AppBarLayout appbarLayout = localView.findViewById(R.id.appbar_board_read);
        toolbar = localView.findViewById(R.id.toolbar_board_read);
        constPostingLayout = localView.findViewById(R.id.constraint_posting);
        constCommentLayout = localView.findViewById(R.id.constraint_comment);
        TextView tvTitle = localView.findViewById(R.id.tv_post_title);
        TextView tvUserName = localView.findViewById(R.id.tv_username);
        TextView tvAutoInfo = localView.findViewById(R.id.tv_autoinfo);
        TextView tvDate = localView.findViewById(R.id.tv_posting_date);
        ImageView imgUserPic = localView.findViewById(R.id.img_userpic);
        etComment = localView.findViewById(R.id.et_comment);
        ImageButton btnSendComment = localView.findViewById(R.id.imgbtn_send_comment);
        ImageButton btnComment = localView.findViewById(R.id.imgbtn_comment);
        ImageButton btnCompathy = localView.findViewById(R.id.imgbtn_compathy);
        tvCommentCnt = localView.findViewById(R.id.tv_cnt_comment);
        tvCompathyCnt = localView.findViewById(R.id.tv_cnt_compathy);
        underline = localView.findViewById(R.id.view_underline_header);
        recyclerComment = localView.findViewById(R.id.recycler_comments);

        // Set the stand-alone toolabr which works in the same way that the action bar does in most
        // cases, but you do not set the toolbar to act as the action bar. In standalone mode, you
        // need to manually populate the toolbar with content and actions as follows. Also, the
        // navigation icon(back arrow) should be handled in setToolbarTitleIcon().
        toolbar.setNavigationOnClickListener(view -> {
            if(imgViewModel != null) imgViewModel = null;
            dismiss();
        });
        tabTitle = getResources().getStringArray(R.array.board_tab_title)[tabPage];
        autoTitle = ((BoardActivity) getActivity()).getAutoClubTitle();

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
        tvAutoInfo.setText(autoClub);
        tvDate.setText(getArguments().getString("timestamp"));
        tvCommentCnt.setText(String.valueOf(cntComment));
        tvCompathyCnt.setText(String.valueOf(cntCompathy));

        // Retreive the auto data from the server and set it to the view
        //setAutoDataString(tvAutoInfo);

        // RecyclerView for showing comments
        recyclerComment.setLayoutManager(new LinearLayoutManager(context));
        commentAdapter = new BoardCommentAdapter(snapshotList);
        recyclerComment.setAdapter(commentAdapter);

        // Pagination using QueryAndPagingHelper which requires refactor.
        QueryAndPagingHelper pagingUtil = new QueryAndPagingHelper();
        pagingUtil.setOnPaginationListener(this);
        recyclerComment.addOnScrollListener(pagingUtil);


        // Event handler for clicking buttons
        //btnDismiss.setOnClickListener(view -> dismiss());
        // On clicking the comment button, show the comment input form.
        btnComment.setOnClickListener(this);
        // Button to set compathy which increase the compathy number if the user has never picked it up.
        btnCompathy.setOnClickListener(view -> setCompathyCount());
        // Upload the comment to Firestore, which needs to refactor for filtering text.
        btnSendComment.setOnClickListener(this);


        // BoardPagerFragment has already updatedd the posting items when created, the comment list
        // shouldn't be updated from the server.
        pagingUtil.setCommentQuery(source, "timestamp", documentId);

        // Realtime update of the comment count and compathy count using SnapshotListener.
        // MetadataChanges.hasPendingWrite metadata.hasPendingWrites property that indicates
        // whether the document has local changes that haven't been written to the backend yet.
        // This property may determine the source of events
        postRef.addSnapshotListener(MetadataChanges.INCLUDE, (snapshot, e) -> {
            log.i("comment snapshot listener: %s", snapshot.getMetadata().hasPendingWrites());
            if(e != null) return;
            if(snapshot != null && snapshot.exists()) {
                long countComment = snapshot.getLong("cnt_comment");
                long countCompathy = snapshot.getLong("cnt_compathy");
                tvCommentCnt.setText(String.valueOf(countComment));
                tvCompathyCnt.setText(String.valueOf(countCompathy));
            }
        });

        // Toolbar menu: if the post is written by the user, show the menu for editting the post.
        // Consider that a new dialogfragment should be created or reuse BoardWriteFragment with
        // putting the data in the fragment.
        inflateEditMenuInToolbar();

        // Rearrange the text by paragraphs
        readContentView(postContent);

        // Set the user image to the view on the header, the uri of which is provided as an arguemnt
        // from BoardPasoingAdapter. Otherwise, the default image is provided.
        String userImage = (TextUtils.isEmpty(userPic))? Constants.imgPath + "ic_user_blank_gray":userPic;
        int size = Constants.ICON_SIZE_TOOLBAR_USERPIC;
        imgUtil.applyGlideToImageView(Uri.parse(userImage), imgUserPic, size, size, true);

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

        // SET THE USER IMAGE ICON
        // ImageViewModel receives a drawable as LiveData from ApplyImageResourceUtil.applyGlideToDrawable()
        // in which Glide creates the custom target that translates an image fitting to a given
        // size and returns a drawable.
        imgViewModel.getGlideDrawableTarget().observe(getViewLifecycleOwner(), drawable -> {
            toolbar.setLogo(drawable);
            toolbar.setContentInsetStartWithNavigation(0);
        });

        // The user reads one's own posting and pick the delete button in the toolbar to pop up
        // the alert dialog. Picking the confirm button, FragmentSharedModel.getPostRemoved()
        // notifies BoardPagerFragment that the user has deleted the post w/ the item position.
        // To prevent the model from autmatically invoking the method, set the value to false;
        sharedModel.getAlertPostResult().setValue(false);
        sharedModel.getAlertPostResult().observe(requireActivity(), result -> {
            // The post will be deleted from Firestore.
            log.i("Alert confirms to delete the post");
            if(result) {
                postRef.delete().addOnSuccessListener(aVoid -> {
                    //Snackbar.make(getView(), R.string.board_msg_delete, Snackbar.LENGTH_SHORT).show();
                    sharedModel.getRemovedPosting().setValue(documentId);
                    dismiss();
                })
                // Method reference in Lambda which uses class name and method name w/o parenthesis
                .addOnFailureListener(Throwable::printStackTrace);
            }
        });

    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onClick(View v) {
        switch(v.getId()) {

            case R.id.imgbtn_comment:
                if(isCommentVisible) constCommentLayout.setVisibility(View.INVISIBLE);
                else constCommentLayout.setVisibility(View.VISIBLE);
                isCommentVisible = !isCommentVisible;
                break;

            case R.id.imgbtn_send_comment:
                if(TextUtils.isEmpty(etComment.getText())) {
                    Snackbar.make(localView, getString(R.string.board_msg_no_comment), Snackbar.LENGTH_SHORT).show();
                    return;
                }

                // On finishing upload, close the soft input and the comment view.
                if(uploadComment()) {
                    // Close the soft input mehtod when clicking the upload button
                    ((InputMethodManager)(getActivity().getSystemService(INPUT_METHOD_SERVICE)))
                            .hideSoftInputFromWindow(localView.getWindowToken(), 0);

                    // Make the comment view invisible
                    constCommentLayout.setVisibility(View.INVISIBLE);
                    isCommentVisible = !isCommentVisible;
                }

                break;
        }

    }

    // The following 3 callbacks are invoked by QueryAndPagingHelper to query a collection reference
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
    @SuppressWarnings("ConstantConditions")
    private boolean uploadComment() {

        Map<String, Object> comment = new HashMap<>();
        comment.put("comment", etComment.getText().toString());
        // Required to determine the standard to set time b/w server and local.
        //comment.put("timestamp", FieldValue.serverTimestamp());
        Calendar calendar = Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault());
        Date date = calendar.getTime();
        comment.put("timestamp", new Timestamp(date));
        log.i("date: %s", date);
        // Fetch the comment user id saved in the storage
        try(FileInputStream fis = getActivity().openFileInput("userId");
            BufferedReader br = new BufferedReader(new InputStreamReader(fis))){
            String commentId =  br.readLine();
            comment.put("user", commentId);
        } catch(IOException e) {
            e.printStackTrace();
        }

        // First, get the document with a given id, then add data
        //DocumentReference documentRef = firestore.collection("board_general").document(documentId);
        postRef.get().addOnSuccessListener(document -> {
            if(document.exists()) {
                final CollectionReference colRef = document.getReference().collection("comments");
                colRef.add(comment).addOnSuccessListener(commentDoc -> {
                    // increase the cnt_cooment in the parent document.
                    postRef.update("cnt_comment", FieldValue.increment(1));
                    // Update the recycler adapter to enlist the pending comment. Don't have to use
                    // SnapshotListener, even thoug it may not update the RecyclerView of other users
                    // simultaneously

                    commentDoc.get(Source.CACHE).addOnSuccessListener(commentSnapshot -> {
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
    private void readContentView(String content) {
        // When an image is attached as the post writes, the line separator is supposed to put in at
        // before and after the image. That's why the regex contains the line separator in order to
        // get the right end position.
        final String REGEX_MARKUP = "\\[image_\\d]\\n";
        final Matcher m = Pattern.compile(REGEX_MARKUP).matcher(content);

        int index = 0; //
        int start = 0;
        int constraintId = constPostingLayout.getId();
        int topConstraint;
        int prevImageId = 0;

        // Create LayoutParams using LinearLayout(RelativeLayout).LayoutParams, not using Constraint
        // Layout.LayoutParams. WHY?
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);


        // If the content contains images, which means the markup(s) exists in the content, the content
        // is split into parts of texts and images and respectively connected to ConstraintSet.
        while(m.find()) {
            // Check if the content starts w/ text or image, which depends on the value of start and
            // add the
            String paragraph = content.substring(start, m.start());
            TextView tv = new TextView(context);
            tv.setId(View.generateViewId());
            tv.setText(paragraph);
            constPostingLayout.addView(tv, params);
            topConstraint = (start == 0) ? underline.getId() : prevImageId;

            ConstraintSet tvSet = new ConstraintSet();
            tvSet.clone(constPostingLayout);
            tvSet.connect(tv.getId(), ConstraintSet.START, constraintId, ConstraintSet.START, 16);
            tvSet.connect(tv.getId(), ConstraintSet.END, constraintId, ConstraintSet.END, 16);
            tvSet.connect(tv.getId(), ConstraintSet.TOP, topConstraint, ConstraintSet.BOTTOM, 32);
            tvSet.applyTo(constPostingLayout);

            // Even if no content exists, ConstrainSet.TOP should be tv.getId() b/c a line is inserted
            // when attaching an image.
            ImageView imgView = new ImageView(context);
            imgView.setId(View.generateViewId());
            prevImageId = imgView.getId();
            constPostingLayout.addView(imgView, params);

            ConstraintSet imgSet = new ConstraintSet();
            imgSet.clone(constPostingLayout);
            imgSet.connect(imgView.getId(), ConstraintSet.START, constraintId, ConstraintSet.START, 0);
            imgSet.connect(imgView.getId(), ConstraintSet.END, constraintId, ConstraintSet.END, 0);
            imgSet.connect(imgView.getId(), ConstraintSet.TOP, tv.getId(), ConstraintSet.BOTTOM, 16);
            imgSet.applyTo(constPostingLayout);

            // Consider to apply Glide thumbnail() method.
            Glide.with(context).asBitmap().load(imgUriList.get(index))
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).fitCenter().into(imgView);

            start = m.end();
            index++;
        }


        // Coordinate the position b/w the last part, no matter what is image or text in the content,
        // and the following recycler view by the patterns.

        // No image attached
        if(start == 0) {
            TextView noImageText = new TextView(context);
            noImageText.setId(View.generateViewId());
            noImageText.setText(content);
            constPostingLayout.addView(noImageText, params);

            ConstraintSet tvSet = new ConstraintSet();
            tvSet.clone(constPostingLayout);
            tvSet.connect(noImageText.getId(), ConstraintSet.START, constraintId, ConstraintSet.START, 16);
            tvSet.connect(noImageText.getId(), ConstraintSet.END, constraintId, ConstraintSet.END, 16);
            tvSet.connect(noImageText.getId(), ConstraintSet.TOP, underline.getId(), ConstraintSet.BOTTOM, 0);
            tvSet.connect(recyclerComment.getId(), ConstraintSet.TOP, noImageText.getId(), ConstraintSet.TOP, 64);

            tvSet.applyTo(constPostingLayout);

        // Text exists after the last image. The last textview is constrained to the previous imageview
        // and the recyclerview constrained to the textview.
        } else if(start < content.length()) {
            String lastParagraph = content.substring(start);
            log.i("Last Paragraph: %s", lastParagraph);
            TextView lastView = new TextView(context);
            lastView.setId(View.generateViewId());
            lastView.setText(lastParagraph);
            constPostingLayout.addView(lastView, params);

            ConstraintSet tvSet = new ConstraintSet();
            tvSet.clone(constPostingLayout);
            tvSet.connect(lastView.getId(), ConstraintSet.START, constraintId, ConstraintSet.START, 16);
            tvSet.connect(lastView.getId(), ConstraintSet.END, constraintId, ConstraintSet.END, 16);
            tvSet.connect(lastView.getId(), ConstraintSet.TOP, prevImageId, ConstraintSet.BOTTOM, 0);
            tvSet.connect(recyclerComment.getId(), ConstraintSet.TOP, lastView.getId(), ConstraintSet.BOTTOM, 64);
            tvSet.applyTo(constPostingLayout);

        // In case no text exists after the last image, the recyclerView is constrained to the last
        // ImageView.
        } else if(start == content.length()) {
            ConstraintSet recyclerSet = new ConstraintSet();
            recyclerSet.clone(constPostingLayout);
            recyclerSet.connect(recyclerComment.getId(), ConstraintSet.TOP, prevImageId, ConstraintSet.BOTTOM, 64);
            recyclerSet.applyTo(constPostingLayout);
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
                userPic = (TextUtils.isEmpty(userPic))?Constants.imgPath + "ic_user_blank_gray":userPic;
                toolbar.setNavigationIcon(null);
                toolbar.setTitle(spannable);
                toolbar.setSubtitle(userName);
                imgUtil.applyGlideToDrawable(userPic, Constants.ICON_SIZE_TOOLBAR_USERPIC, imgViewModel);

                break;

            case STATE_EXPANDED:
                toolbar.setNavigationIcon(R.drawable.ic_action_navigation);
                if(tabPage == Constants.BOARD_AUTOCLUB) toolbar.setTitle(autoTitle);
                else toolbar.setTitle(tabTitle);

                toolbar.setSubtitle("");
                toolbar.setLogo(null);
                break;

            case STATE_IDLE:
                //log.i("STATE_IDLE");
                break;

        }
    }

    // Check if the user has already picked a post as favorite doing queries the compathy collection,
    // documents of which contains user ids
    @SuppressWarnings("ConstantConditions")
    private void setCompathyCount() {
        // Prevent repeated connection to Firestore every time when users click the button.
        if(hasCompathy) {
            log.i("First click");
            Snackbar.make(getView(), getString(R.string.board_msg_compathy), Snackbar.LENGTH_SHORT).show();
            return;
        }

        //final DocumentReference docRef = firestore.collection("board_general").document(documentId);
        final DocumentReference compathyRef = postRef.collection("compathy").document(userId);

        compathyRef.get().addOnCompleteListener(task -> {
            if(task.isSuccessful()) {
                DocumentSnapshot snapshot = task.getResult();
                if(snapshot != null && snapshot.exists()) {
                    hasCompathy = true;
                    //docRef.update("cnt_compathy", FieldValue.increment(-1));
                    //compathyRef.delete();
                    Snackbar.make(getView(), getString(R.string.board_msg_compathy), Snackbar.LENGTH_SHORT).show();

                } else {
                    postRef.update("cnt_compathy", FieldValue.increment(1));
                    Map<String, Object> data = new HashMap<>();
                    data.put("timestamp", FieldValue.serverTimestamp());
                    compathyRef.set(data);
                }
            }

        }).addOnSuccessListener(aVoid -> log.i("isCompathy exists"))
                .addOnFailureListener(e -> log.e("isCompathy does not exist: %s", e.getMessage()));

    }

    // If a user is the owner of the post, show the menus in the toolbar which edit or delete the post.
    @SuppressWarnings("ConstantConditions")
    private void inflateEditMenuInToolbar() {
        // The userId here means the id of user who writes the posting item whereas the viewId means
        // the id of who reads the item. If both ids are equal, the edit buttons(revise and delete)
        // are visible, which means the writer(userId) can edit one's own post.
        try (FileInputStream fis = getActivity().openFileInput("userId");
             BufferedReader br = new BufferedReader(new InputStreamReader(fis))) {

            String viewerId = br.readLine();
            if(userId.equals(viewerId)) {
                toolbar.inflateMenu(R.menu.menu_board_read);
                toolbar.setOnMenuItemClickListener(item -> {
                    switch(item.getItemId()) {
                        case R.id.action_board_edit:
                            //sharedModel.getImageChooser().setValue(-1);
                            // Create the dialog fragment with arguments which have been passed from
                            // BoardPagerFragment when an item was picked.
                            /*
                            FrameLayout frame = ((BoardActivity)getActivity()).getBoardFrameLayout();
                            BoardWriteFragment writePostFragment = new BoardWriteFragment();
                            Bundle bundle = new Bundle();
                            bundle.putBoolean("isEditMode", true);
                            //bundle.putAll(getArguments());
                            writePostFragment.setArguments(bundle);
                            if(frame.getChildCount() > 0) frame.removeAllViews();

                            getActivity().getSupportFragmentManager().beginTransaction()
                                    .addToBackStack(null)
                                    .replace(frame.getId(), writePostFragment)
                                    .commit();

                             */

                            mListener.onEditClicked(getArguments());
                            dismiss();

                            return true;

                        case R.id.action_board_delete:
                            String title = getString(R.string.board_alert_delete);
                            String msg = getString(R.string.board_alert_msg);
                            AlertDialogFragment.newInstance(title, msg, Constants.BOARD)
                                    .show(getActivity().getSupportFragmentManager(), null);
                            return true;

                        default: return false;

                    }

                });
            }

        } catch(IOException e) {
            e.printStackTrace();
        }
    }


    private void setAutoDataString(TextView autoInfo) {
        // Get the auto data, which is saved as the type of json string in SharedPreferences, for
        // displaying it in the post header.
        firestore.collection("users").document(userId).get().addOnCompleteListener(task -> {
            if(task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if(document != null && document.exists()) {
                    String jsonAutoInfo = document.getString("auto_data");
                    log.i("auto data: %s", jsonAutoInfo);
                    try {
                        JSONArray jsonArray = new JSONArray(jsonAutoInfo);
                        StringBuilder sb = new StringBuilder();
                        for(int i = 0; i < jsonArray.length(); i++) {
                            sb.append(jsonArray.optString(i)).append(" ");
                        }

                        autoClub = sb.toString();
                        autoInfo.setText(sb.toString());

                    } catch(JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }
}
