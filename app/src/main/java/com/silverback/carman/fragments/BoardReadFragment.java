/*
 * Copyright (C) 2012 The Carman Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.silverback.carman.fragments;

import static android.app.Activity.RESULT_OK;
import static android.content.Context.INPUT_METHOD_SERVICE;
import static com.silverback.carman.BoardActivity.AUTOCLUB;
import static com.silverback.carman.BoardActivity.PAGING_COMMENT;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentResultListener;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Transaction;
import com.google.firebase.storage.FirebaseStorage;
import com.silverback.carman.BoardActivity;
import com.silverback.carman.R;
import com.silverback.carman.SettingActivity;
import com.silverback.carman.adapters.BoardCommentAdapter;
import com.silverback.carman.adapters.BoardReplyAdapter;
import com.silverback.carman.databinding.FragmentBoardReadBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.ApplyImageResourceUtil;
import com.silverback.carman.utils.Constants;
import com.silverback.carman.utils.QueryPostPaginationUtil;
import com.silverback.carman.utils.RecyclerDividerUtil;
import com.silverback.carman.viewmodels.FragmentSharedModel;
import com.silverback.carman.viewmodels.ImageViewModel;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This dialogfragment reads a post content in the full size when clicking  an item recycled in
 * BoardPagerFragment.
 */
public class BoardReadFragment extends DialogFragment implements
        View.OnClickListener, CompoundButton.OnCheckedChangeListener,
        BoardCommentAdapter.CommentAdapterListener,
        QueryPostPaginationUtil.OnQueryPaginationCallback {
        //QueryCommentPagingUtil.OnQueryPaginationCallback {

    private static final LoggingHelper log = LoggingHelperFactory.create(BoardReadFragment.class);

    // Constants
    private static final int STATE_COLLAPSED = 0;
    private static final int STATE_EXPANDED = 1;
    private static final int STATE_IDLE = 2;

    // Objects
    private Context context;
    //private PostingBoardRepository postRepo;
    //private PostingBoardViewModel postingModel;
    //private PostingClubRepository pagingUtil;
    //private OnDialogDismissListener dialogDismissListener;
    private ListenerRegistration regListener;
    //private QueryCommentPagingUtil queryCommentPagingUtil;
    private QueryPostPaginationUtil queryPaginationUtil;
    private SharedPreferences mSettings;
    //private OnEditModeListener mListener;
    private FirebaseFirestore mDB;
    private FirebaseStorage storage;
    private DocumentReference postRef;
    private ApplyImageResourceUtil imgUtil;
    private ImageViewModel imgViewModel;
    private FragmentSharedModel sharedModel;
    private BoardCommentAdapter commentAdapter;
    private String documentId, postTitle, postContent, postOwnerId, postOwnerName, postOwnerPic;
    private String viewerId;
    private ArrayList<String> uriStringList;
    private List<DocumentSnapshot> commentShotList;
    private InputMethodManager imm;
    //private ListenerRegistration commentListener;
    //private List<CharSequence> autoclub;

    // UIs
    private FragmentBoardReadBinding binding;
    // Fields
    private SpannableStringBuilder autoTitle;
    private String tabTitle;
    private int tabPage;
    private int position; // item poistion in the recyclerview.
    private int checkedPos;
    private int appbarOffset;
    private int cntComment, cntCompathy;
    private boolean isCommentVisible;
    private boolean hasCompathy;
    //private boolean isLoading;

    // Interface for notifying BoardActivity of pressing the edit menu in the toolbar which is visible
    // only when a user reads his/her own post
    /*
    public interface OnEditModeListener {
        void onEditClicked(Bundle bundle);
    }
    // Interface for listening to BoardActivity at the lifecycle of onAttachFragment.
    public void setEditModeListener(OnEditModeListener listener) {
        mListener = listener;
    }
     */
    // Constructor default.
    public BoardReadFragment() {
        // Required empty public constructor
    }

    final ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), this::getActivityResultCallback);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getArguments() != null) {
            tabPage = getArguments().getInt("tabPage");
            position = getArguments().getInt("position");
            documentId = getArguments().getString("documentId");
            postTitle = getArguments().getString("postTitle");
            postContent = getArguments().getString("postContent");
            postOwnerName = getArguments().getString("userName");
            postOwnerPic = getArguments().getString("userPic");
            postOwnerId = getArguments().getString("userId");
            uriStringList = getArguments().getStringArrayList("urlImgList");
            cntComment = (int)getArguments().getLong("cntComment");
            cntCompathy = (int)getArguments().getLong("cntCompathy");
        }

        // Get the viewer id for checking whether the post owner is the viewer
        try(FileInputStream fis = requireActivity().openFileInput("userId");
            BufferedReader br = new BufferedReader(new InputStreamReader(fis))){
            viewerId = br.readLine();
        } catch(IOException e) {e.printStackTrace();}

        this.context = requireContext();
        mDB = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        mSettings = PreferenceManager.getDefaultSharedPreferences(context);
        imgUtil = new ApplyImageResourceUtil(context);
        imm = (InputMethodManager)context.getSystemService(INPUT_METHOD_SERVICE);

        //queryCommentPagingUtil = new QueryCommentPagingUtil(firestore, this);
        queryPaginationUtil = new QueryPostPaginationUtil(mDB, this);
        commentShotList = new ArrayList<>();
        commentAdapter = new BoardCommentAdapter(getContext(), commentShotList, viewerId, this);

        // Get the current document reference which should be shared in the fragment.
        // Initially, attach SnapshotListener to have the comment collection updated, then remove
        // the listener to prevent connecting to the server. Instead`, update the collection using
        // Source.Cache.
        postRef = mDB.collection("user_post").document(documentId);
        queryPaginationUtil.setCommentQuery(postRef, "timestamp");
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentBoardReadBinding.inflate(inflater);
        // Set the stand-alone toolabr which works in the same way that the action bar does in most
        // cases, but you do not set the toolbar to act as the action bar. In standalone mode, you
        // need to manually populate the toolbar with content and actions as follows. Also, the
        // navigation icon(back arrow) should be handled in setToolbarTitleIcon().
        binding.toolbarBoardRead.setNavigationOnClickListener(view -> dismiss());
        tabTitle = getResources().getStringArray(R.array.board_tab_title)[tabPage];
        autoTitle = ((BoardActivity)requireActivity()).getAutoClubTitle();

        //setHasOptionsMenu(true);
        // If the user is the owner of a post, display the edit menu in the toolbar, which should
        // use MenuInflater and create menu dynimically. It seems onCreateOptionsMenu does not work
        // in DialogFragment
        if(postOwnerId != null && postOwnerId.equals(viewerId)) {
            createEditOptionsMenu();
        }


        binding.tvPostTitle.setText(postTitle);
        binding.tvUsername.setText(postOwnerName);
        binding.tvPostingDate.setText(requireArguments().getString("timestamp"));
        binding.tvCntComment.setText(String.valueOf(cntComment));
        binding.tvCntCompathy.setText(String.valueOf(cntCompathy));

        // Retreive the auto data from the server and set it to the view
        // UPADTE THE FIRESTORE FIELD NAMES REQUIRED !!
        //showUserAutoClub(binding.tvAutoinfo);

        // RecyclerView for showing comments
        LinearLayoutManager layout = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        RecyclerDividerUtil divider = new RecyclerDividerUtil(Constants.DIVIDER_HEIGHT_POSTINGBOARD,
                0, ContextCompat.getColor(requireContext(), R.color.recyclerDivider));
        binding.recyclerComments.setHasFixedSize(false); //due to banner plugin
        binding.recyclerComments.setLayoutManager(layout);
        binding.recyclerComments.addItemDecoration(divider);
        binding.recyclerComments.setItemAnimator(new DefaultItemAnimator());
        binding.recyclerComments.setAdapter(commentAdapter);
        //setRecyclerViewScrollListener();
        //binding.recyclerComments.addOnScrollListener(pagingUtil);

        // Event handler for buttons
        binding.switchComment.setOnCheckedChangeListener(this);
        binding.imgbtnComment.setOnClickListener(this);
        binding.imgbtnLoadComment.setOnClickListener(this);
        binding.imgbtnCompathy.setOnClickListener(view -> setCompathyCount());
        binding.imgbtnSendComment.setOnClickListener(this);
        // Implements the abstract method of AppBarStateChangeListener to be notified of the state
        // of appbarlayout as it is scrolling, which changes the toolbar title and icon by the
        // scroling state.
        binding.appbarBoardRead.addOnOffsetChangedListener(new AppBarStateChangeListener() {
            @Override
            public void onStateChanged(AppBarLayout appBarLayout, int state) {
                setToolbarTitleIcon(state);
            }
        });

        // RecyclerView.OnScrollListener() does not work if it is inside (Nested)ScrollView. To make
        // it listen to scrolling, use the parent scollview listener.
        /*
        binding.nestedScrollview.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener)
                (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                    if((scrollY >= (binding.recyclerComments.getMeasuredHeight() - v.getMeasuredHeight())
                            && scrollY > oldScrollY)) {
                        if(!isLoading) {
                            isLoading = true;
                            queryPaginationUtil.setNextPostQuery();
                        }
                    }
                });
        */
        // Attach the user image in the header, if any, using Glide. Otherwise, the blank image
        // is set.
        String userImage = (TextUtils.isEmpty(postOwnerPic))?Constants.imgPath + "ic_user_blank_gray": postOwnerPic;
        int size = Constants.ICON_SIZE_TOOLBAR_USERPIC;
        imgUtil.applyGlideToImageView(Uri.parse(userImage), binding.imgUserpic, size, size, true);

        // Rearrange the text by paragraphs
        readContentView(postContent);

        return binding.getRoot();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sharedModel = new ViewModelProvider(requireActivity()).get(FragmentSharedModel.class);
        imgViewModel = new ViewModelProvider(requireActivity()).get(ImageViewModel.class);

        // SET THE USER IMAGE ICON
        // ImageViewModel receives a drawable as LiveData from ApplyImageResourceUtil.applyGlideToDrawable()
        // in which Glide creates the custom target that translates an image fitting to a given
        // size and returns a drawable.
        imgViewModel.getGlideDrawableTarget().observe(this, drawable -> {
            binding.toolbarBoardRead.setLogo(drawable);
            binding.toolbarBoardRead.setContentInsetStartWithNavigation(0);
        });

        // If a post is the user's own one, the delete button appears on the toolbar. When tapping the
        // button and picking the confirm button, FragmentSharedModel.getPostRemoved() notifies
        // BoardPagerFragment that the user has deleted the post w/ the item position. To prevent
        // the model from automatically invoking the method, initially set the value to false;

        // NOT WORKING. INSTEAD USE FragmentResultListener
        sharedModel.getAlertPostResult().observe(getViewLifecycleOwner(), confirmed -> {
            if(confirmed) {
                dismiss();
                /*
                postRef.delete().addOnSuccessListener(aVoid -> {
                    if(uriStringList != null && uriStringList.size() > 0) {
                        for (String url : uriStringList) storage.getReferenceFromUrl(url).delete();
                    }
                    sharedModel.getRemovedPosting().setValue(position);
                    dismiss();
                }).addOnFailureListener(Throwable::printStackTrace);

                 */
            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();
        /*
        // Realtime update of the comment count and compathy count using SnapshotListener.
        // MetadataChanges.hasPendingWrite metadata.hasPendingWrites property that indicates
        // whether the document has local changes that haven't been written to the backend yet.
        // This property may determine the source of events
        regListener = postRef.addSnapshotListener(MetadataChanges.INCLUDE, (snapshot, e) -> {
            if(e != null) return;
            if(snapshot != null && snapshot.exists()) {
                long cntComment = Objects.requireNonNull(snapshot.getLong("cnt_comment"));
                long cntCompathy = Objects.requireNonNull(snapshot.getLong("cnt_compathy"));
                //binding.tvCntComment.setText(String.valueOf(cntComment));
                //binding.tvCntCompathy.setText(String.valueOf(cntCompathy));
                //binding.headerCommentCnt.setText(String.valueOf(cntComment));
            }
        });

         */
    }

    @Override
    public void onPause() {
        log.i("onPause");
        super.onPause();
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        log.i("onDismiss");
        super.onDismiss(dialog);
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
        if(isChecked) {
            //queryPaginationUtil.setCommentQuery(postRef);
            log.i("comment list:%s", commentShotList.size());
            binding.recyclerComments.setVisibility(View.VISIBLE);
            int visible = (cntComment > PAGING_COMMENT) ? View.VISIBLE : View.GONE;
            binding.imgbtnLoadComment.setVisibility(visible);
            //if(cntComment > PAGING_COMMENT) binding.imgbtnLoadComment.setVisibility(View.VISIBLE);
            //else binding.imgbtnLoadComment.setVisibility(View.GONE);

        } else {
            //commentAdapter.notifyItemRangeRemoved(0, commentShotList.size());
            //commentShotList.clear();
            binding.recyclerComments.setVisibility(View.GONE);
            //binding.nestedScrollview.post(() -> binding.nestedScrollview.fullScroll(View.FOCUS_UP));
        }
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.imgbtn_comment) {
            // Check whether a user name is set. Otherwise, show an messagie in the snackbar to
            // move to SettingPrefActivity to make a user name.
            if(checkUserName()) {
                int visibility = isCommentVisible ? View.GONE : View.VISIBLE;
                //int direction = isCommentVisible ? View.FOCUS_UP : View.FOCUS_DOWN;
                binding.constraintComment.setVisibility(visibility);
                binding.etComment.getText().clear();

                if(!isCommentVisible) {
                    binding.etComment.requestFocus();
                    commentAdapter.notifyItemChanged(checkedPos, true);
                }
                isCommentVisible = !isCommentVisible;
            }

        } else if(v.getId() == R.id.imgbtn_send_comment) {
            if(TextUtils.isEmpty(binding.etComment.getText())) {
                Snackbar.make(binding.getRoot(), getString(R.string.board_msg_no_comment), Snackbar.LENGTH_SHORT).show();
            } else uploadComment();

        } else if(v.getId() == R.id.imgbtn_load_comment) {
            log.i("add more comments");
            if(cntComment > commentAdapter.getItemCount()) queryPaginationUtil.setNextCommentQuery();
            else notifyNoData();
        }
    }

    // Implement QueryPostPaginationUtil.OnQueryPaginationCallback overriding the follwoing methods
    // to show comments on the post by the pagination.
    @Override
    public void getFirstQueryResult(QuerySnapshot commentShots) {
        commentShotList.clear();
        for(DocumentSnapshot comment : commentShots) commentShotList.add(comment);
        commentAdapter.notifyItemRangeChanged(0, commentShots.size());
        //binding.nestedScrollview.post(() -> binding.nestedScrollview.fullScroll(View.FOCUS_DOWN));
        int scrollY = binding.nestedScrollview.getHeight();
        binding.nestedScrollview.post(() -> binding.nestedScrollview.smoothScrollTo(0, scrollY, 1000));
    }

    @Override
    public void getNextQueryResult(QuerySnapshot nextShots) {
        final int start = commentShotList.size();
        for(DocumentSnapshot comment : nextShots) commentShotList.add(comment);
        commentAdapter.notifyItemRangeChanged(start, nextShots.size());
        binding.nestedScrollview.post(() -> binding.nestedScrollview.fullScroll(View.FOCUS_DOWN));
    }

    @Override
    public void getLastQueryResult(QuerySnapshot lastShots) {
        final int start = commentShotList.size();
        for(DocumentSnapshot comment : lastShots) commentShotList.add(comment);
        commentAdapter.notifyItemRangeChanged(start, lastShots.size());
        binding.nestedScrollview.post(() -> binding.nestedScrollview.fullScroll(View.FOCUS_DOWN));
    }

    @Override
    public void getQueryErrorResult(Exception e) {
        Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
    }

    // The BoardCommentAdapter.CommentAdapterListener interface impelemts the following methods
    // deleteComment(): may delete a comment as long as the reader is the owner of a comment.
    // deleteCommentReply(): may delete a reply as long as the reader is the owner of a reply.
    // addCommentReply(): may add a comment
    // notifyReplyChecked(): notified of whether the switch button turns on or off.
    @Override
    public void deleteComment(DocumentSnapshot doc) {
        postRef.update("cnt_comment", FieldValue.increment(-1)).addOnSuccessListener(bVoid -> {
            cntComment --;
            if(cntComment <= PAGING_COMMENT) binding.imgbtnLoadComment.setVisibility(View.GONE);
            binding.tvCntComment.setText(String.valueOf(cntComment));
            binding.headerCommentCnt.setText(String.valueOf(cntComment));
        }).addOnFailureListener(Throwable::printStackTrace);
    }
    @Override
    public void deleteCommentReply(BoardReplyAdapter replyAdapter, DocumentReference commentRef) {
        log.i("post deletion handling");
        //final DocumentReference commentRef = postRef.collection("comments").document(commentId);
        /*
        commentRef.collection("replies").document(replyId).delete().addOnCompleteListener(task -> {
            if(task.isSuccessful()) {
                commentRef.update("cnt_reply", FieldValue.increment(-1));
                replyAdapter.notifyItemRemoved(pos);
            }
        });

         */
    }

    @Override
    public void notifyUploadReplyDone(int pos, boolean isDone) {
        //log.i("upload reply: %s, %s", pos, isDone);
        if(imm.isActive()) imm.hideSoftInputFromWindow(binding.getRoot().getWindowToken(), 0);

        String msg = (isDone)?"uploading reply done" : "uploading reply failed";
        Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_SHORT).show();
        //commentAdapter.notifyItemChanged(position, false);
    }

    @Override
    public void notifyNoData() {
        Snackbar.make(binding.getRoot(), "No more replies", Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void notifySwitchChecked(int checkedPos, int bindingPos) {
        if(imm.isActive()) imm.hideSoftInputFromWindow(binding.getRoot().getWindowToken(), 0);
        // Hide the comment edittext if the reply switch turns on.
        if(binding.etComment.isFocused()) {
            binding.constraintComment.setVisibility(View.GONE);
            isCommentVisible = false;
        }
        // A new reply switch turns on and another switch should be off.
        this.checkedPos = bindingPos;
        if(checkedPos != bindingPos) commentAdapter.notifyItemChanged(checkedPos, true);

        //binding.nestedScrollview.post(() -> binding.nestedScrollview.fullScroll(View.FOCUS_DOWN));
        int scrollY = binding.nestedScrollview.getHeight();
        binding.nestedScrollview.post(() -> binding.nestedScrollview.smoothScrollTo(0, scrollY, 1500));
    }

    @Override
    public void notifyEditTextFocused(View view) {
        log.i("content edit text focused");
        if(!checkUserName()) view.clearFocus();
        binding.nestedScrollview.post(() -> binding.nestedScrollview.smoothScrollTo(0, view.getBottom()));
    }

    private void getActivityResultCallback(ActivityResult result) {
        log.i("activity result: %s", result.getData());
        if(result.getData() != null) log.i("user name:");
    }

    // Make up the text-based content and any image attached in ConstraintLayout which is dynamically
    // created by ConstraintSet. Images should be managed by Glide.
    // The regular expression makes text and images split with the markup which was inserted when images
    // were created. While looping the content, split parts of text and image are conntected by
    // ConstraintSets which are applied to the parent ConstraintLayout.
    // The recyclerview which displays comments at the bottom should be coordinated according to
    // whether the content has images or not.
    private void readContentView(String content) {
        // When an image is attached as the post writes, the line separator is supposed to put in at
        // before and after the image. That's why the regex contains the line separator in order to
        // get the right end position.
        final String REGEX_MARKUP = "\\[image_\\d]";
        final Matcher m = Pattern.compile(REGEX_MARKUP).matcher(content);
        final ConstraintLayout parent = binding.constraintPosting;

        int index = 0;
        int start = 0;
        int target;
        int prevImageId = 0;

        // Create LayoutParams using LinearLayout(RelativeLayout).LayoutParams, not using Constraint
        // Layout.LayoutParams. WHY?
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        // If the content contains images, which means any markup(s) exists in the content, the content
        // is split into parts of texts and images and respectively connected to ConstraintSet.
        while(m.find()) {
            // Check whether the content starts w/ text or image, which depends on the value of start.
            String paragraph = content.substring(start, m.start());
            TextView tv = new TextView(context);
            tv.setId(View.generateViewId());
            tv.setText(paragraph);
            parent.addView(tv, params);
            target = (start == 0) ? binding.guideline.getId() : prevImageId;

            ConstraintSet tvSet = new ConstraintSet();
            tvSet.clone(parent);
            tvSet.connect(tv.getId(), ConstraintSet.START, parent.getId(), ConstraintSet.START, 16);
            tvSet.connect(tv.getId(), ConstraintSet.END, parent.getId(), ConstraintSet.END, 16);
            tvSet.connect(tv.getId(), ConstraintSet.TOP, target, ConstraintSet.BOTTOM, 16);
            tvSet.applyTo(parent);

            ImageView imgView = new ImageView(context);
            imgView.setId(View.generateViewId());
            prevImageId = imgView.getId();
            parent.addView(imgView, params);

            ConstraintSet imgSet = new ConstraintSet();
            imgSet.clone(parent);
            imgSet.connect(imgView.getId(), ConstraintSet.START, parent.getId(), ConstraintSet.START, 16);
            imgSet.connect(imgView.getId(), ConstraintSet.END, parent.getId(), ConstraintSet.END, 16);
            imgSet.connect(imgView.getId(), ConstraintSet.TOP, tv.getId(), ConstraintSet.BOTTOM, 0);
            imgSet.applyTo(parent);

            // Consider to apply Glide thumbnail() method.
            Glide.with(context).asBitmap().load(uriStringList.get(index))
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).fitCenter().into(imgView);

            start = m.end();
            index++;
        }

        // Coordinate the position b/w the last part, no matter what is imageview or textview in the content,
        // and the following recyclerview which shows any comment
        // Simple text w/o any image
        if(start == 0) {
            log.i("simple text");
            TextView simpleText = new TextView(context);
            simpleText.setId(View.generateViewId());
            simpleText.setText(content);
            parent.addView(simpleText, params);

            ConstraintSet tvSet = new ConstraintSet();
            tvSet.clone(parent);
            tvSet.connect(simpleText.getId(), ConstraintSet.START, parent.getId(), ConstraintSet.START, 16);
            tvSet.connect(simpleText.getId(), ConstraintSet.END, parent.getId(), ConstraintSet.END, 16);
            tvSet.connect(simpleText.getId(), ConstraintSet.TOP, binding.guideline.getId(), ConstraintSet.BOTTOM, 16);
            tvSet.connect(binding.headerComment.getId(), ConstraintSet.TOP, simpleText.getId(), ConstraintSet.BOTTOM, 64);
            //tvSet.connect(binding.recyclerComments.getId(), ConstraintSet.TOP, simpleText.getId(), ConstraintSet.BOTTOM, 16);
            //tvSet.connect(binding.headerComment.getId(), ConstraintSet.TOP, simpleText.getId(), ConstraintSet.BOTTOM, 0);
            tvSet.applyTo(parent);

        // Text after an image
        } else if(start < content.length()) {
            String lastParagraph = content.substring(start);
            log.i("text after an image: %s", lastParagraph.length());
            TextView lastView = new TextView(context);
            lastView.setId(View.generateViewId());
            lastView.setText(lastParagraph);
            parent.addView(lastView, params);

            ConstraintSet tvSet = new ConstraintSet();
            tvSet.clone(parent);
            tvSet.connect(lastView.getId(), ConstraintSet.START, parent.getId(), ConstraintSet.START, 16);
            tvSet.connect(lastView.getId(), ConstraintSet.END, parent.getId(), ConstraintSet.END, 16);
            tvSet.connect(lastView.getId(), ConstraintSet.TOP, prevImageId, ConstraintSet.BOTTOM, 0);
            tvSet.connect(binding.headerComment.getId(), ConstraintSet.TOP, lastView.getId(), ConstraintSet.BOTTOM, 64);
            tvSet.applyTo(parent);

        // No text after the last image
        } else if(start == content.length()) {
            log.i("image positioned at the last");
            ConstraintSet imageSet = new ConstraintSet();
            imageSet.clone(parent);
            imageSet.connect(binding.headerComment.getId(), ConstraintSet.TOP, prevImageId, ConstraintSet.BOTTOM, 0);
            imageSet.applyTo(parent);
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
                if (mCurrentState != STATE_EXPANDED) onStateChanged(appBarLayout, STATE_EXPANDED);
                mCurrentState = STATE_EXPANDED;

            } else if (Math.abs(verticalOffset) >= appBarLayout.getTotalScrollRange()) {
                if (mCurrentState != STATE_COLLAPSED) onStateChanged(appBarLayout, STATE_COLLAPSED);
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

    // Set the toolbar Icon and title as the appbarlayout is scrolling, which is notified by
    // AppBarStateChangeListener.
    private void setToolbarTitleIcon(int state) {
        SpannableString spannable = new SpannableString(postTitle);
        int size = Math.abs(appbarOffset) / 6;
        spannable.setSpan(new AbsoluteSizeSpan(size), 0, spannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        binding.toolbarBoardRead.setTitle(spannable);
        switch(state) {
            case STATE_COLLAPSED:
                postOwnerPic = (TextUtils.isEmpty(postOwnerPic)) ? Constants.imgPath + "ic_user_blank_gray" : postOwnerPic;
                binding.toolbarBoardRead.setNavigationIcon(null);
                binding.toolbarBoardRead.setTitle(spannable);
                binding.toolbarBoardRead.setSubtitle(postOwnerName);
                imgUtil.applyGlideToDrawable(postOwnerPic, Constants.ICON_SIZE_TOOLBAR_USERPIC, imgViewModel);
                binding.toolbarBoardRead.setOnClickListener(view -> dismiss());
                break;

            case STATE_EXPANDED:
                binding.toolbarBoardRead.setNavigationIcon(R.drawable.ic_action_navigation);
                if(tabPage == AUTOCLUB) binding.toolbarBoardRead.setTitle(autoTitle);
                else binding.toolbarBoardRead.setTitle(tabTitle);
                binding.toolbarBoardRead.setSubtitle("");
                binding.toolbarBoardRead.setLogo(null);
                break;

            case STATE_IDLE: break;

        }
    }

    // Method for uploading the comment to Firestore
    private void uploadComment() {
        Map<String, Object> comment = new HashMap<>();
        comment.put("cnt_reply", 0);
        comment.put("comment", binding.etComment.getText().toString());
        comment.put("timestamp", FieldValue.serverTimestamp());
        // Fetch the comment user id saved in the storage
        try(FileInputStream fis = requireActivity().openFileInput("userId");
            BufferedReader br = new BufferedReader(new InputStreamReader(fis))){
            String commentId =  br.readLine();
            comment.put("user_id", commentId);
            final DocumentReference docref = mDB.collection("users").document(commentId);
            mDB.runTransaction((Transaction.Function<Void>) transaction -> {
                DocumentSnapshot doc = transaction.get(docref);
                comment.put("user_name", doc.getString("user_name"));
                comment.put("user_pic", doc.getString("user_pic"));

                postRef.collection("comments").add(comment).addOnSuccessListener(commentRef -> {
                    //queryPaginationUtil.setCommentQuery(postRef, "timestamp");
                    commentRef.get().addOnSuccessListener(snapshot -> {
                        commentShotList.add(0, snapshot);
                        commentAdapter.notifyItemInserted(0);
                    });

                    postRef.update("cnt_comment", FieldValue.increment(1)).addOnSuccessListener(aVoid->{
                        cntComment++;
                        binding.tvCntComment.setText(String.valueOf(cntComment));
                        binding.headerCommentCnt.setText(String.valueOf(cntComment));
                    });

                    binding.nestedScrollview.fullScroll(View.FOCUS_UP);

                }).addOnFailureListener(Throwable::printStackTrace);

                imm.hideSoftInputFromWindow(binding.getRoot().getWindowToken(), 0);
                // Make the comment view invisible and reset the flag.
                binding.constraintComment.setVisibility(View.GONE);
                isCommentVisible = !isCommentVisible;
                return null;
            });

        } catch(IOException | NullPointerException e) {e.printStackTrace();}

    }

    // Check if the user has already picked a post as favorite doing queries the compathy collection,
    // documents of which contains user ids
    private void setCompathyCount() {
        // Prevent repeated connection to Firestore every time when users click the button.
        final String msg = getString(R.string.board_msg_compathy);
        if(hasCompathy) {
            Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_SHORT).show();
            return;
        }

        final DocumentReference compathyRef = postRef.collection("compathy").document(viewerId);
        compathyRef.get().addOnCompleteListener(task -> {
            if(task.isSuccessful()) {
                DocumentSnapshot snapshot = task.getResult();
                if(snapshot != null && snapshot.exists()) {
                    hasCompathy = true;
                    Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_SHORT).show();
                } else {
                    postRef.update("cnt_compathy", FieldValue.increment(1)).addOnSuccessListener(aVoid -> {
                        Map<String, Object> data = new HashMap<>();
                        data.put("timestamp", FieldValue.serverTimestamp());
                        compathyRef.set(data);
                        cntCompathy++;
                        binding.tvCntCompathy.setText(String.valueOf(cntCompathy));

                    });
                }
            }
        });
    }

    // As long as a post belongs to the user, show the menu in the toolbar which enables the user
    // to edits or delete the post.

    // The userId means the id of the post item owner whereas the viewId means that of who reads
    // item.  The edit buttons turn visible only when both ids are equal, which means the reader
    // is the post owner.
    private void createEditOptionsMenu() {
        binding.toolbarBoardRead.inflateMenu(R.menu.options_board_read);
        binding.toolbarBoardRead.setOnMenuItemClickListener(item -> {
            if(item.getItemId() == R.id.action_board_edit) {
                BoardEditFragment editFragment = new BoardEditFragment();
                Bundle editBundle = new Bundle();
                editBundle.putString("documentId", documentId);
                editBundle.putString("postTitle", postTitle);
                editBundle.putString("postContent", postContent);
                editBundle.putInt("position", position);
                if (uriStringList != null && uriStringList.size() > 0) {
                    log.i("uriStringList: %s", uriStringList.size());
                    editBundle.putStringArrayList("uriImgList", uriStringList);
                }

                editFragment.setArguments(editBundle);
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .addToBackStack(null)
                        .add(android.R.id.content, editFragment)
                        .commit();
                dismiss();
                //return true;

            } else if(item.getItemId() == R.id.action_board_delete) {
                /*
                postRef.delete().addOnSuccessListener(aVoid -> {
                    sharedModel.getRemovedPosting().setValue(position);
                    dismiss();
                }).addOnFailureListener(Throwable::printStackTrace);
                */
                String title = getString(R.string.board_alert_delete);
                String msg = getString(R.string.board_alert_msg);

                DialogFragment fragment = AlertDialogFragment.newInstance(title, msg, Constants.BOARD);
                FragmentManager fragmentManager = getChildFragmentManager();

                // Should do research on FragmentResultListener;
                fragmentManager.setFragmentResultListener("confirmed", this, (req, result) -> {
                    postRef.delete().addOnSuccessListener(aVoid -> {
                        sharedModel.getRemovedPosting().setValue(true);
                        dismiss();
                    }).addOnFailureListener(Throwable::printStackTrace);
                });

                fragment.show(fragmentManager, "alert");
                //return true;
            }
            return false;
        });

        /*
        try (FileInputStream fis = requireActivity().openFileInput("userId");
             BufferedReader br = new BufferedReader(new InputStreamReader(fis))) {

            String viewerId = br.readLine();
            if(postOwnerId != null && postOwnerId.equals(viewerId)) {
                binding.toolbarBoardRead.inflateMenu(R.menu.options_board_read);
                binding.toolbarBoardRead.setOnMenuItemClickListener(item -> {
                    if(item.getItemId() == R.id.action_board_edit) {
                        //mListener.onEditClicked(getArguments());
                        //((BoardActivity)requireActivity()).addEditFragment(getArguments());
                        BoardEditFragment editFragment = new BoardEditFragment();
                        Bundle editBundle = new Bundle();
                        editBundle.putString("documentId", documentId);
                        editBundle.putString("postTitle", postTitle);
                        editBundle.putString("postContent", postContent);
                        editBundle.putInt("position", position);
                        if(uriStringList != null && uriStringList.size() > 0){
                            log.i("uriStringList: %s", uriStringList.size());
                            editBundle.putStringArrayList("uriImgList", uriStringList);
                        }
                        editFragment.setArguments(editBundle);
                        requireActivity().getSupportFragmentManager().beginTransaction()
                                .addToBackStack(null)
                                .add(android.R.id.content, editFragment)
                                .commit();

                        dismiss();
                        return true;

                    } else if(item.getItemId() == R.id.action_board_delete) {
                        String title = getString(R.string.board_alert_delete);
                        String msg = getString(R.string.board_alert_msg);
                        AlertDialogFragment.newInstance(title, msg, Constants.BOARD)
                                .show(requireActivity().getSupportFragmentManager(), null);

                        return true;
                    }
                    return false;
                });
            }
        } catch(IOException e) {
            e.printStackTrace();
        }

         */
    }

    // Display the auto club if the user has set the automaker, automodel, enginetype, and autoyear.
    /*
    private void showUserAutoClub(final TextView autoInfo) {
        firestore.collection("users").document(postOwnerId).get().addOnCompleteListener(task -> {
            if(task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if(document != null && document.exists()) {
                    String jsonAutoInfo = document.getString("user_club");
                    try {
                        JSONArray json = new JSONArray(jsonAutoInfo);
                        if(json.length() == 0) return;

                        StringBuilder sb = new StringBuilder();
                        for(int i = 0; i < json.length(); i++) {
                            if(json.optString(i) != null && !json.optString(i).equalsIgnoreCase("null")) {
                                sb.append(json.optString(i)).append(" ");
                            }
                        }
                        autoInfo.setText(sb.toString());
                    } catch(JSONException | NullPointerException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

     */

    /*
    private void queryCommentSnapshot(DocumentReference docref) {
        postRepo.setCommentQuery(docref);
        PostingBoardLiveData postLiveData = postingModel.getPostingBoardLiveData();
        if(postLiveData != null) {
            postLiveData.observe(getViewLifecycleOwner(), operation -> {
                int type = operation.getType();
                DocumentSnapshot postshot = operation.getDocumentSnapshot();
                switch(type) {
                    case 0: // ADDED
                        commentShotList.add(postshot);
                        break;

                    case 1: // MODIFIED
                        log.i("MODIFIED");
                        for(int i = 0; i < commentShotList.size(); i++) {
                            DocumentSnapshot snapshot = commentShotList.get(i);
                            if(snapshot.getId().equals(postshot.getId())) {
                                commentShotList.remove(snapshot);
                                commentShotList.add(i, postshot);
                            }
                        }
                        break;

                    case 2: // REMOVED
                        for(int i = 0; i < commentShotList.size(); i++) {
                            DocumentSnapshot snapshot = commentShotList.get(i);
                            if(snapshot.getId().equals(postshot.getId())) commentShotList.remove(snapshot);
                        }
                        break;
                }

                commentAdapter.notifyDataSetChanged();

            });
        }
    }
     */

    private boolean checkUserName() {
        String userName = mSettings.getString(Constants.USER_NAME, null);
        if (TextUtils.isEmpty(userName)) {
            Snackbar snackbar = Snackbar.make(binding.getRoot(),
                    getString(R.string.board_msg_username), Snackbar.LENGTH_LONG);

            snackbar.setAction(R.string.board_msg_action_setting, view -> {
                Intent intent = new Intent(getActivity(), SettingActivity.class);
                intent.putExtra("postingboard", Constants.REQUEST_BOARD_SETTING_USERNAME);
                activityResultLauncher.launch(intent);
            }).show();
            return false;
        } else return true;
    }
}
