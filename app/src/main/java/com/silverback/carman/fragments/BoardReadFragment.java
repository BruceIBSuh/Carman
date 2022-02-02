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

import static android.content.Context.INPUT_METHOD_SERVICE;
import static com.silverback.carman.BoardActivity.AUTOCLUB;
import static com.silverback.carman.BoardActivity.PAGINATION;

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
import android.util.SparseLongArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
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
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.MetadataChanges;
import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.silverback.carman.BaseActivity;
import com.silverback.carman.BoardActivity;
import com.silverback.carman.R;
import com.silverback.carman.SettingActivity;
import com.silverback.carman.adapters.BoardCommentAdapter;
import com.silverback.carman.databinding.FragmentBoardReadBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.ApplyImageResourceUtil;
import com.silverback.carman.utils.Constants;
import com.silverback.carman.utils.QueryPostPaginationUtil;
import com.silverback.carman.viewmodels.FragmentSharedModel;
import com.silverback.carman.viewmodels.ImageViewModel;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This dialogfragment reads a post content in the full size when tapping  an item recycled in
 * BoardPagerFragment.
 */
public class BoardReadFragment extends DialogFragment implements
        View.OnClickListener,
        QueryPostPaginationUtil.OnQueryPaginationCallback {
        //QueryCommentPagingUtil.OnQueryPaginationCallback {

    private static final LoggingHelper log = LoggingHelperFactory.create(BoardReadFragment.class);

    // Constants
    private static final int STATE_COLLAPSED = 0;
    private static final int STATE_EXPANDED = 1;
    private static final int STATE_IDLE = 2;

    // Objects
    private Context context;
    private DialogInterface.OnDismissListener mDismissListener;
    //private PostingBoardRepository postRepo;
    //private PostingBoardViewModel postingModel;
    //private PostingClubRepository pagingUtil;
    //private OnDialogDismissListener dialogDismissListener;
    private ListenerRegistration regListener;
    //private QueryCommentPagingUtil queryCommentPagingUtil;
    private QueryPostPaginationUtil queryPaginationUtil;
    private SharedPreferences mSettings;
    //private OnEditModeListener mListener;
    private FirebaseFirestore firestore;
    private FirebaseStorage firebaseStorage;
    private DocumentReference postRef;
    private ApplyImageResourceUtil imgUtil;
    private ImageViewModel imgViewModel;
    private FragmentSharedModel sharedModel;
    private BoardCommentAdapter commentAdapter;
    private String postTitle, postContent, userName, userPic;
    private List<String> uriStringList;
    private List<DocumentSnapshot> commentShotList;
    //private ListenerRegistration commentListener;
    //private List<CharSequence> autoclub;

    // UIs
    private FragmentBoardReadBinding binding;
    // Fields
    private SpannableStringBuilder autoTitle;
    private String tabTitle;
    private String userId, documentId;
    private int tabPage;
    private int position; // item poistion in the recyclerview.
    private int appbarOffset;
    private int cntComment, cntCompathy;
    private boolean isCommentVisible;
    private boolean hasCompathy;
    private boolean isLoading;

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
        this.context = getContext();

        firestore = FirebaseFirestore.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();
        mSettings = ((BaseActivity)requireActivity()).getSharedPreferernces();

        //queryCommentPagingUtil = new QueryCommentPagingUtil(firestore, this);
        queryPaginationUtil = new QueryPostPaginationUtil(firestore, this);
        commentShotList = new ArrayList<>();
        commentAdapter = new BoardCommentAdapter(commentShotList);

        imgUtil = new ApplyImageResourceUtil(getContext());
        imgViewModel = new ViewModelProvider(this).get(ImageViewModel.class);
        sharedModel = new ViewModelProvider(requireActivity()).get(FragmentSharedModel.class);

        if(getArguments() != null) {
            tabPage = getArguments().getInt("tabPage");
            position = getArguments().getInt("position");
            postTitle = getArguments().getString("postTitle");
            postContent = getArguments().getString("postContent");
            userName = getArguments().getString("userName");
            userPic = getArguments().getString("userPic");
            uriStringList = getArguments().getStringArrayList("urlImgList");
            userId = getArguments().getString("userId");
            cntComment = getArguments().getInt("cntComment");
            cntCompathy = getArguments().getInt("cntCompahty");
            documentId = getArguments().getString("documentId");
        }

        // Get the current document reference which should be shared in the fragment.
        // Initially, attach SnapshotListener to have the comment collection updated, then remove
        // the listener to prevent connecting to the server. Instead, update the collection using
        // Source.Cache.
        postRef = firestore.collection("user_post").document(documentId);
        /*
        postRef.get().addOnSuccessListener(aVoid -> commentListener = postRef.collection("comments")
                .addSnapshotListener(MetadataChanges.INCLUDE, (querySnapshot, e) -> {
                    if(e != null) return;
                    source = (querySnapshot != null && querySnapshot.getMetadata().hasPendingWrites())
                            ? Source.CACHE : Source.SERVER;
                })
        );
         */



        /*
        // Instantiate PagingQueryHelper to paginate comments in a post.
        //pagingUtil = new PostingClubRepository(firestore);
        //pagingUtil.setOnPaginationListener(this);
        postRepo = new PostingBoardRepository();
        postingModel = new ViewModelProvider(this, new PostingBoardModelFactory(postRepo))
                .get(PostingBoardViewModel.class);

         */
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
        createEditOptionsMenu();

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
        // it feasible to listen to scrolling, use the parent scollview listener.
        binding.vgNestedscrollview.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener)
                (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {

            if((scrollY >= (binding.recyclerComments.getMeasuredHeight() - v.getMeasuredHeight()) && scrollY > oldScrollY)) {
                if(!isLoading) {
                    isLoading = true;
                    queryPaginationUtil.setNextQuery();
                }
            }
        });

        binding.tvPostTitle.setText(postTitle);
        binding.tvUsername.setText(userName);
        binding.tvPostingDate.setText(requireArguments().getString("timestamp"));
        binding.tvCntComment.setText(String.valueOf(cntComment));
        binding.tvCntCompathy.setText(String.valueOf(cntCompathy));

        // Retreive the auto data from the server and set it to the view

        // UPADTE THE FIRESTORE FIELD NAMES REQUIRED !!
        //showUserAutoClub(binding.tvAutoinfo);


        // RecyclerView for showing comments
        binding.recyclerComments.setLayoutManager(new LinearLayoutManager(context));
        binding.recyclerComments.setAdapter(commentAdapter);
        //setRecyclerViewScrollListener();
        //binding.recyclerComments.addOnScrollListener(pagingUtil);

        // Event handler for buttons
        binding.imgbtnComment.setOnClickListener(this);
        binding.imgbtnCompathy.setOnClickListener(view -> setCompathyCount());
        binding.imgbtnSendComment.setOnClickListener(this);



        // Attach the user image in the header, if any, using Glide. Otherwise, the blank image
        // is set.
        String userImage = (TextUtils.isEmpty(userPic))?Constants.imgPath + "ic_user_blank_gray":userPic;
        int size = Constants.ICON_SIZE_TOOLBAR_USERPIC;
        imgUtil.applyGlideToImageView(Uri.parse(userImage), binding.imgUserpic, size, size, true);

        // Realtime update of the comment count and compathy count using SnapshotListener.
        // MetadataChanges.hasPendingWrite metadata.hasPendingWrites property that indicates
        // whether the document has local changes that haven't been written to the backend yet.
        // This property may determine the source of events
        regListener = postRef.addSnapshotListener(MetadataChanges.INCLUDE, (snapshot, e) -> {
            if(e != null) {
                e.printStackTrace();
                return;
            }

            if(snapshot != null && snapshot.exists()) {
                BoardGeneralObject board = snapshot.toObject(BoardGeneralObject.class);
                //long cntComment = snapshot.getLong("cnt_comment");
                //long cntCompathy = snapshot.getLong("cnt_compathy");
                long cntComment = Objects.requireNonNull(board).getCommentCount();
                long cntCompathy = Objects.requireNonNull(board).getCompathyCount();
                binding.tvCntComment.setText(String.valueOf(cntComment));
                binding.tvCntCompathy.setText(String.valueOf(cntCompathy));
            }
        });
        // Rearrange the text by paragraphs
        readContentView(postContent);
        // Query comments
        //pagingUtil.setCommentQuery(tabPage, "timestamp", postRef);
        //queryCommentSnapshot(postRef);
        //queryCommentPagingUtil.setCommentQuery(postRef);
        isLoading = true;
        queryPaginationUtil.setCommentQuery(postRef);
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
        sharedModel.getAlertPostResult().observe(getViewLifecycleOwner(), result -> {
            // Confirmed in the didalog.
            if(result) {
                postRef.delete().addOnSuccessListener(aVoid -> {
                    if(uriStringList != null && uriStringList.size() > 0){
                        for (String url : uriStringList)
                            firebaseStorage.getReferenceFromUrl(url).delete();
                    }
                    log.i("another viewmodel invoked: %s", position);
                    //sharedModel.getRemovedPosting().setValue(postRef.getId());
                    // notifyItemRemoved required!!
                    //sharedModel.getRemovedPosting().setValue(position);
                    //((BoardActivity)requireActivity()).addViewPager();
                    dismiss();
                }).addOnFailureListener(Throwable::printStackTrace);

            } else dismiss();
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        regListener.remove();
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        log.i("onDismiss");
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.imgbtn_comment) {
            // Check whether a user name is set. Otherwise, show an messagie in the snackbar to
            // move to SettingPrefActivity to make a user name.
            String userName = mSettings.getString(Constants.USER_NAME, null);
            if(TextUtils.isEmpty(userName)) {
                Snackbar snackbar = Snackbar.make(
                        binding.getRoot(), getString(R.string.board_msg_username), Snackbar.LENGTH_LONG);
                snackbar.setAction(R.string.board_msg_action_setting, view -> {
                    Intent intent = new Intent(getActivity(), SettingActivity.class);
                    intent.putExtra("requestCode", Constants.REQUEST_BOARD_SETTING_USERNAME);
                    //startActivityForResult(intent, Constants.REQUEST_BOARD_SETTING_USERNAME);
                    activityResultLauncher.launch(intent);
                }).show();


            } else {
                int visibility = (isCommentVisible) ? View.GONE : View.VISIBLE;
                binding.constraintComment.setVisibility(visibility);
                //constCommentLayout.setVisibility(View.VISIBLE);
                binding.etComment.getText().clear();
                binding.etComment.requestFocus();
                isCommentVisible = !isCommentVisible;
            }

        } else if(v.getId() == R.id.imgbtn_send_comment) {
            if(TextUtils.isEmpty(binding.etComment.getText())) {
                Snackbar.make(binding.getRoot(), getString(R.string.board_msg_no_comment), Snackbar.LENGTH_SHORT).show();
                return;
            }

            uploadComment();
        }
    }

    // Implement QueryPostPaginationUtil.OnQueryPaginationCallback overriding the follwoing methods
    // to show comments on the post by the pagination.
    @Override
    public void getFirstQueryResult(QuerySnapshot postShots) {
        if(commentShotList.size() > 0) commentShotList.clear();
        for(DocumentSnapshot comment : postShots) {
            commentShotList.add(comment);
            commentAdapter.notifyDataSetChanged();
        }
        // In case the first query retrieves shots less than the pagination number, no more loading
        // is made.
        isLoading = postShots.size() < PAGINATION;
    }

    @Override
    public void getNextQueryResult(QuerySnapshot nextShots) {
        for(DocumentSnapshot comment : nextShots) {
            commentShotList.add(comment);
            commentAdapter.notifyDataSetChanged();
        }

        isLoading = nextShots.size() < PAGINATION;
    }

    @Override
    public void getLastQueryResult(QuerySnapshot lastShots) {
        for(DocumentSnapshot comment : lastShots) commentShotList.add(comment);
        commentAdapter.notifyDataSetChanged();
        isLoading = true;
    }

    @Override
    public void getQueryErrorResult(Exception e) {
        Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
        isLoading = true;
    }

    // Subclass of RecyclerView.ScrollViewListner.
    /*
    private void setRecyclerViewScrollListener() {
        RecyclerView.OnScrollListener scrollListener = new RecyclerView.OnScrollListener(){
            boolean isScrolling;
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                //if (newState == RecyclerView.SCROLL_STATE_IDLE) fabWrite.show();
                if(newState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                    log.i("newState: %s", newState);
                    isScrolling = true;
                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                //if (dy > 0 || dy < 0 && fabWrite.isShown()) fabWrite.hide();
                log.i("onScrolled");
                LinearLayoutManager layoutManager = ((LinearLayoutManager) recyclerView.getLayoutManager());
                if (layoutManager != null) {
                    int firstVisibleProductPosition = layoutManager.findFirstVisibleItemPosition();
                    int visiblePostCount = layoutManager.getChildCount();
                    int totalPostCount = layoutManager.getItemCount();
                    log.i("layout: %s, %s, %s, %s", isScrolling, firstVisibleProductPosition, visiblePostCount, totalPostCount);
                    if (!isLoading && isScrolling && (firstVisibleProductPosition + visiblePostCount == totalPostCount)) {
                        isScrolling = false;
                        isLoading = true;
                        log.i("next query by scrolling");
                        //pbPaging.setVisibility(View.VISIBLE);
                        queryCommentPagingUtil.setNextQuery();

                        //if(currentPage != AUTOCLUB) queryPostSnapshot(currentPage);
                        //else if(!isLastPage) clubRepo.setNextQuery();
                    }
                }
            }

        };

        recyclerComment.addOnScrollListener(scrollListener);
    }

     */

    // The following callbacks are invoked by PagingQueryHelper to query comments up to the limit
    // and on showing the last one, another query get started.
    /*
    @Override
    public void setFirstQuery(int page, QuerySnapshot snapshot) {
        snapshotList.clear();
        for(DocumentSnapshot document : snapshot) snapshotList.add(document);
        commentAdapter.notifyDataSetChanged();
    }
    @Override
    public void setNextQueryStart(boolean b) {
        // Set the visibility of Progressbar to visible.
    }
    @Override
    public void setNextQueryComplete(int page, QuerySnapshot querySnapshot) {
        if(querySnapshot.size() == 0) return;
        for(DocumentSnapshot document : querySnapshot) snapshotList.add(document);
        //commentAdapter.notifyDataSetChanged();
        commentAdapter.notifyItemInserted(0);
    }

     */
    private void getActivityResultCallback(ActivityResult result) {

    }

    // Method for uploading the comment to Firestore.
    //@SuppressWarnings("ConstantConditions")
    private void uploadComment() {
        Map<String, Object> comment = new HashMap<>();
        comment.put("comment", binding.etComment.getText().toString());
        comment.put("timestamp", FieldValue.serverTimestamp());
        // Fetch the comment user id saved in the storage
        try(FileInputStream fis = requireActivity().openFileInput("userId");
            BufferedReader br = new BufferedReader(new InputStreamReader(fis))){
            String commentId =  br.readLine();
            comment.put("userId", commentId);
        } catch(IOException e) {
            e.printStackTrace();
        }

        // Get the document first, then the comment sub collection is retrieved. If successful, update
        // the comment count in the document and reset the fields.
        postRef.get().addOnSuccessListener(document -> {
            if(document.exists()) {
                final CollectionReference colRef = document.getReference().collection("comments");
                colRef.add(comment).addOnSuccessListener(commentDoc -> {
                    postRef.update("cnt_comment", FieldValue.increment(1));
                    queryPaginationUtil.setCommentQuery(postRef);

                    // Create the viewmodel livedata as SparseArray<Long>
                    SparseLongArray sparseArray = new SparseLongArray();
                    Long cntComment = (Long)document.get("cnt_comment");
                    if(cntComment != null) sparseArray.put(position, cntComment + 1);
                    sharedModel.getNewComment().setValue(sparseArray);

                }).addOnFailureListener(e -> {
                    e.printStackTrace();
                    Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
                });

                // Hide the soft input method.
                ((InputMethodManager)(requireActivity().getSystemService(INPUT_METHOD_SERVICE)))
                        .hideSoftInputFromWindow(binding.getRoot().getWindowToken(), 0);

                // Make the comment view invisible and reset the flag.
                binding.constraintComment.setVisibility(View.GONE);
                isCommentVisible = !isCommentVisible;
            }
        });
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
        //int parent = binding.constraintPosting;
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
            tvSet.connect(tv.getId(), ConstraintSet.TOP, target, ConstraintSet.BOTTOM, 32);
            tvSet.applyTo(parent);

            // Even if no content exists, ConstrainSet.TOP should be tv.getId() b/c a line is inserted
            // when attaching an image.
            ImageView imgView = new ImageView(context);
            imgView.setId(View.generateViewId());
            prevImageId = imgView.getId();
            parent.addView(imgView, params);

            ConstraintSet imgSet = new ConstraintSet();
            imgSet.clone(parent);
            imgSet.connect(imgView.getId(), ConstraintSet.START, parent.getId(), ConstraintSet.START, 0);
            imgSet.connect(imgView.getId(), ConstraintSet.END, parent.getId(), ConstraintSet.END, 0);
            imgSet.connect(imgView.getId(), ConstraintSet.TOP, tv.getId(), ConstraintSet.BOTTOM, 32);
            imgSet.applyTo(parent);

            // Consider to apply Glide thumbnail() method.
            Glide.with(context).asBitmap().load(uriStringList.get(index))
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
            parent.addView(noImageText, params);

            ConstraintSet tvSet = new ConstraintSet();
            tvSet.clone(parent);
            tvSet.connect(noImageText.getId(), ConstraintSet.START, parent.getId(), ConstraintSet.START, 16);
            tvSet.connect(noImageText.getId(), ConstraintSet.END, parent.getId(), ConstraintSet.END, 16);
            tvSet.connect(noImageText.getId(), ConstraintSet.TOP, binding.guideline.getId(), ConstraintSet.BOTTOM, 32);
            tvSet.connect(binding.recyclerComments.getId(), ConstraintSet.TOP, noImageText.getId(), ConstraintSet.BOTTOM, 64);

            tvSet.applyTo(parent);

        // Text exists after the last image. The last textview is constrained to the previous imageview
        // and the recyclerview constrained to the textview.
        } else if(start < content.length()) {
            String lastParagraph = content.substring(start);
            TextView lastView = new TextView(context);
            lastView.setId(View.generateViewId());
            lastView.setText(lastParagraph);
            parent.addView(lastView, params);

            ConstraintSet tvSet = new ConstraintSet();
            tvSet.clone(parent);
            tvSet.connect(lastView.getId(), ConstraintSet.START, parent.getId(), ConstraintSet.START, 16);
            tvSet.connect(lastView.getId(), ConstraintSet.END, parent.getId(), ConstraintSet.END, 16);
            tvSet.connect(lastView.getId(), ConstraintSet.TOP, prevImageId, ConstraintSet.BOTTOM, 0);
            tvSet.connect(binding.recyclerComments.getId(), ConstraintSet.TOP, lastView.getId(), ConstraintSet.BOTTOM, 64);
            tvSet.applyTo(parent);

        // No text exists after the last image; the recyclerView is constrained to the last ImageView
        } else if(start == content.length()) {
            ConstraintSet recyclerSet = new ConstraintSet();
            recyclerSet.clone(parent);
            recyclerSet.connect(binding.recyclerComments.getId(), ConstraintSet.TOP, prevImageId, ConstraintSet.BOTTOM, 64);
            recyclerSet.applyTo(parent);
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
                userPic = (TextUtils.isEmpty(userPic)) ? Constants.imgPath + "ic_user_blank_gray" : userPic;
                binding.toolbarBoardRead.setNavigationIcon(null);
                binding.toolbarBoardRead.setTitle(spannable);
                binding.toolbarBoardRead.setSubtitle(userName);
                imgUtil.applyGlideToDrawable(userPic, Constants.ICON_SIZE_TOOLBAR_USERPIC, imgViewModel);
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

    // Check if the user has already picked a post as favorite doing queries the compathy collection,
    // documents of which contains user ids
    //@SuppressWarnings("ConstantConditions")
    private void setCompathyCount() {
        // Prevent repeated connection to Firestore every time when users click the button.
        if(hasCompathy) {
            Snackbar.make(binding.getRoot(), getString(R.string.board_msg_compathy), Snackbar.LENGTH_SHORT).show();
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
                    Snackbar.make(binding.getRoot(), getString(R.string.board_msg_compathy), Snackbar.LENGTH_SHORT).show();

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

    // As long as a post belongs to the user, show the menu in the toolbar which enables the user
    // to edits or delete the post.
    //
    // The userId means the id of the post item owner whereas the viewId means that of who reads
    // item.  The edit buttons turn visible only when both ids are equal, which means the reader
    // is the post owner.
    private void createEditOptionsMenu() {
        try (FileInputStream fis = requireActivity().openFileInput("userId");
             BufferedReader br = new BufferedReader(new InputStreamReader(fis))) {

            String viewerId = br.readLine();
            if(userId != null && userId.equals(viewerId)) {
                binding.toolbarBoardRead.inflateMenu(R.menu.options_board_read);
                binding.toolbarBoardRead.setOnMenuItemClickListener(item -> {
                    if(item.getItemId() == R.id.action_board_edit) {
                        //mListener.onEditClicked(getArguments());
                        ((BoardActivity)requireActivity()).addEditFragment(getArguments());
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
    }

    // Display the auto club if the user has set the automaker, automodel, enginetype, and autoyear.
    private void showUserAutoClub(final TextView autoInfo) {
        firestore.collection("users").document(userId).get().addOnCompleteListener(task -> {
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
    private static class BoardGeneralObject {
        @PropertyName("post_title")
        private String postTitle;
        @PropertyName("post_content")
        private String postContent;
        @PropertyName("post_general")
        private boolean isGeneralPost;
        @PropertyName("timestamp")
        private long timestamp;
        @PropertyName("user_id")
        private String userId;
        @PropertyName("user_name")
        private String userName;
        @PropertyName("cnt_view")
        private long cntView;
        @PropertyName("cnt_comment")
        private long cntComment;
        @PropertyName("cnt_ccompathy")
        private long cntCompathy;

        public BoardGeneralObject() {
            // Must have a public no-argument constructor
        }

        public BoardGeneralObject(long view, long comment, long compathy) {
            this.cntView = view;
            this.cntComment = comment;
            this.cntCompathy = compathy;
        }

        @PropertyName("cnt_view")
        public long getViewCount() {
            return cntView;
        }
        @PropertyName("cnt_comment")
        public long getCommentCount() {
            return cntComment;
        }
        @PropertyName("cnt_ccompathy")
        public long getCompathyCount() {
            return cntCompathy;
        }
    }


}
