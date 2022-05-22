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
import static com.silverback.carman.BoardActivity.PAGING_COMMENT;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.silverback.carman.BoardActivity;
import com.silverback.carman.R;
import com.silverback.carman.SettingActivity;
import com.silverback.carman.adapters.BoardCommentAdapter;
import com.silverback.carman.adapters.BoardReadFeedAdapter;
import com.silverback.carman.databinding.BoardFragmentReadBinding;
import com.silverback.carman.databinding.BoardReadHeaderBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.ApplyImageResourceUtil;
import com.silverback.carman.utils.Constants;
import com.silverback.carman.utils.CustomPostingObject;
import com.silverback.carman.utils.QueryPostPaginationUtil;
import com.silverback.carman.utils.RecyclerDividerUtil;
import com.silverback.carman.viewmodels.FragmentSharedModel;
import com.silverback.carman.viewmodels.ImageViewModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BoardReadFragment extends DialogFragment implements
        View.OnClickListener,
        Toolbar.OnMenuItemClickListener,
        //CompoundButton.OnCheckedChangeListener,
        BoardReadFeedAdapter.ReadFeedAdapterListener,
        BoardCommentAdapter.CommentAdapterListener,
        QueryPostPaginationUtil.OnQueryPaginationCallback {

    private static final LoggingHelper log = LoggingHelperFactory.create(BoardReadFragment.class);

    public static final int POST_CONTENT = 0;
    public static final int COMMENT_HEADER = 1;
    public static final int COMMENT_LIST = 2;
    public static final int EMPTY_VIEW = 3;

    // Constants
    private static final int STATE_COLLAPSED = 0;
    private static final int STATE_EXPANDED = 1;
    private static final int STATE_IDLE = 2;

    // Objects
    private Context context;
    private FirebaseFirestore mDB;
    private CustomPostingObject obj;
    private ListenerRegistration regListener;
    private QueryPostPaginationUtil queryPaginationUtil;
    private SharedPreferences mSettings;

    private BoardReadFeedAdapter boardReadFeedAdapter;

    private DocumentReference postRef;
    private ApplyImageResourceUtil imgUtil;
    private ImageViewModel imgViewModel;
    private FragmentSharedModel sharedModel;
    private BoardCommentAdapter commentAdapter;
    private String documentId;
    private String viewerId;
    private ArrayList<String> uriStringList, autofilter;
    private List<DocumentSnapshot> commentShotList;
    private InputMethodManager imm;

    //private FragmentBoardReadBinding binding;
    private BoardFragmentReadBinding binding;
    private BoardReadHeaderBinding headerBinding;

    private SpannableStringBuilder autoTitle;
    private String tabTitle;
    private String userPic;
    private int tabPage, position;
    private int replyCheckedPos;
    private int appbarOffset;
    private int cntComment, cntCompathy;
    private boolean isCommentVisible;
    private boolean hasCompathy;

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
            obj = getArguments().getParcelable("postingObj");
            assert obj != null;
            tabPage = getArguments().getInt("tabPage");
            position = getArguments().getInt("position");
            viewerId = getArguments().getString("viewerId");
            documentId = getArguments().getString("documentId");
            cntComment = obj.getCntComment();
            cntCompathy = obj.getCntCompahty();

            if(obj.getPostImages() != null) uriStringList = new ArrayList<>(obj.getPostImages());
            if(obj.getAutofilter() != null) autofilter = new ArrayList<>(obj.getAutofilter());
        }

        this.context = requireContext();
        mDB = FirebaseFirestore.getInstance();
        mSettings = PreferenceManager.getDefaultSharedPreferences(context);
        imgUtil = new ApplyImageResourceUtil(context);
        imm = (InputMethodManager)context.getSystemService(INPUT_METHOD_SERVICE);

        queryPaginationUtil = new QueryPostPaginationUtil(mDB, this);
        commentShotList = new ArrayList<>();
        commentAdapter = new BoardCommentAdapter(getContext(), commentShotList, viewerId, this);

        boardReadFeedAdapter = new BoardReadFeedAdapter(obj, commentAdapter, this);

        postRef = mDB.collection("user_post").document(documentId);
        queryPaginationUtil.setCommentQuery(postRef, "timestamp");

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        //binding = FragmentBoardReadBinding.inflate(inflater);
        binding = BoardFragmentReadBinding.inflate(inflater, container, false);
        headerBinding = BoardReadHeaderBinding.inflate(inflater, container, false);
        // Set the stand-alone toolabr which works in the same way that the action bar does in most
        // cases, but you do not set the toolbar to act as the action bar. In standalone mode, you
        // need to manually populate the toolbar with content and actions as follows. Also, the
        // navigation icon(back arrow) should be handled in setToolbarTitleIcon().
        binding.toolbarBoardRead.setNavigationOnClickListener(view -> dismiss());
        tabTitle = getResources().getStringArray(R.array.board_tab_title)[tabPage];
        autoTitle = ((BoardActivity)requireActivity()).getAutoClubTitle();

        setHasOptionsMenu(true);
        // If the user is the owner of a post, display the edit menu in the toolbar, which should
        // use MenuInflater and create menu dynimically. It seems onCreateOptionsMenu does not work
        // in DialogFragment
        if(obj.getUserId() != null && obj.getUserId().equals(viewerId)) {
            //createEditOptionsMenu();
            binding.toolbarBoardRead.inflateMenu(R.menu.options_board_read);
            binding.toolbarBoardRead.setOnMenuItemClickListener(this);
        }

        // Attach the user image in the header, if any, using Glide. Otherwise, the blank image
        // is set.
        ((BoardActivity)requireActivity()).setUserProfile(obj.getUserId(), binding.tvUsername, binding.imgUserpic);
        binding.tvPostTitle.setText(obj.getPostTitle());
        binding.tvPostingDate.setText(requireArguments().getString("timestamp"));
        binding.tvCntComment.setText(String.valueOf(cntComment));
        binding.tvCntCompathy.setText(String.valueOf(cntCompathy));

        LinearLayoutManager layout = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        RecyclerDividerUtil divider = new RecyclerDividerUtil(Constants.DIVIDER_HEIGHT_POSTINGBOARD,
                0, ContextCompat.getColor(requireContext(), R.color.recyclerDivider));
        binding.recyclerRead.setHasFixedSize(false);
        binding.recyclerRead.setLayoutManager(layout);
        //binding.recyclerRead.addItemDecoration(divider);
        binding.recyclerRead.setItemAnimator(new DefaultItemAnimator());
        binding.recyclerRead.setAdapter(boardReadFeedAdapter);

        // Retreive the auto data from the server and set it to the view
        // UPADTE THE FIRESTORE FIELD NAMES REQUIRED !!
        //showUserAutoClub(binding.tvAutoinfo);

        // Event handler for buttons
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
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        if(menuItem.getItemId() == R.id.action_board_edit) {
            BoardEditFragment editFragment = new BoardEditFragment();
            Bundle editBundle = new Bundle();
            editBundle.putString("documentId", documentId);
            editBundle.putString("postTitle", obj.getPostTitle());
            editBundle.putString("postContent", obj.getPostContent());
            editBundle.putInt("position", position);
            if (uriStringList != null && uriStringList.size() > 0) {
                editBundle.putStringArrayList("uriImgList", uriStringList);
            }

            if(autofilter != null && autofilter.size() > 0){
                editBundle.putStringArrayList("autofilter", autofilter);
            }

            editFragment.setArguments(editBundle);
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .addToBackStack(null)
                    .replace(android.R.id.content, editFragment)
                    .commit();
            dismiss();
            return true;

        } else if(menuItem.getItemId() == R.id.action_board_delete) {
            String title = getString(R.string.board_alert_delete);
            String msg = getString(R.string.board_alert_msg);

            DialogFragment fragment = CustomDialogFragment.newInstance(title, msg, Constants.BOARD);
            FragmentManager fragmentManager = getChildFragmentManager();
            fragmentManager.setFragmentResultListener("confirmToRemove", fragment, (req, res) -> {
                if(req.matches("confirmToRemove") && (res.getBoolean("confirmed"))) {
                    postRef.get().addOnSuccessListener(post -> {
                        sharedModel.getRemovedPosting().setValue(post);
                        postRef.delete().addOnSuccessListener(aVoid -> dismiss());
                    });
                }
            });

            fragment.show(fragmentManager, "alert");
            return true;

        } else return false;
    }

    // Implement BoardReadFeedAdapter.ReadFeedAdapterListener to handle the visibiillity of the button
    // which loads more comments to load if the number of comments are more than the comment pagination.
    @Override
    public void showCommentLoadButton(int isVisible) {
        log.i("showCommentLoadButton");
        binding.imgbtnLoadComment.setVisibility(isVisible);
    }

    @Override
    public void onCommentSwitchChanged(boolean isChecked) {
        log.i("onCommentSwitchChanged: %s", isChecked);

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
                    commentAdapter.notifyItemChanged(replyCheckedPos, true);
                }

                isCommentVisible = !isCommentVisible;
            }

        } else if(v.getId() == R.id.imgbtn_send_comment) {
            if(TextUtils.isEmpty(binding.etComment.getText())) {
                Snackbar.make(binding.getRoot(), getString(R.string.board_msg_no_comment), Snackbar.LENGTH_SHORT).show();
            } else uploadComment();

        } else if(v.getId() == R.id.imgbtn_load_comment) {
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
        commentAdapter.submitCommentList(commentShotList);
    }

    @Override
    public void getNextQueryResult(QuerySnapshot nextShots) {
        for(DocumentSnapshot comment : nextShots) commentShotList.add(comment);
        commentAdapter.submitCommentList(commentShotList);
    }

    @Override
    public void getLastQueryResult(QuerySnapshot lastShots) {
        for(DocumentSnapshot comment : lastShots) commentShotList.add(comment);
        commentAdapter.submitCommentList(commentShotList);
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
            boardReadFeedAdapter.notifyItemChanged(COMMENT_HEADER, cntComment);
        }).addOnFailureListener(Throwable::printStackTrace);
    }

    @Override
    public void OnUploadReplyDone(int pos, boolean isDone) {
        //log.i("upload reply: %s, %s", pos, isDone);
        if(imm.isActive()) imm.hideSoftInputFromWindow(binding.getRoot().getWindowToken(), 0);

        String msg = (isDone)?"uploading reply done" : "uploading reply failed";
        Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_SHORT).show();
        //commentAdapter.notifyItemChanged(position, false);
        //queryPaginationUtil.setCommentReplyQuery();
    }

    @Override
    public void notifyNoData() {
        Snackbar.make(binding.getRoot(), "No more replies", Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void OnReplySwitchChecked(int checkedPos, int bindingPos) {
        log.i("notified by the comment adapter: %s, %s", checkedPos, bindingPos);
        if(imm.isActive()) imm.hideSoftInputFromWindow(binding.getRoot().getWindowToken(), 0);

        // Hide the comment edittext if the reply switch turns on.
        if(binding.etComment.isFocused()) {
            binding.constraintComment.setVisibility(View.GONE);
            isCommentVisible = false;
        }
        // A new reply switch turns on and the previous reply switch should be off.
        if(checkedPos != bindingPos) commentAdapter.notifyItemChanged(checkedPos, true);
        this.replyCheckedPos = bindingPos;

        //binding.nestedScrollview.post(() -> binding.nestedScrollview.fullScroll(View.FOCUS_DOWN));
        //int scrollY = binding.nestedScrollview.getHeight();
        //binding.nestedScrollview.post(() -> binding.nestedScrollview.smoothScrollTo(0, scrollY, 1500));
    }

    @Override
    public void OnReplyContentFocused(View view) {
        log.i("content edit text focused");
        if(!checkUserName()) view.clearFocus();
        //binding.nestedScrollview.post(() -> binding.nestedScrollview.smoothScrollTo(0, view.getBottom()));
    }

    private void getActivityResultCallback(ActivityResult result) {
        log.i("activity result: %s", result.getData());
        if(result.getData() != null) log.i("user name:");
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
        SpannableString spannable = new SpannableString(obj.getPostTitle());
        int size = Math.abs(appbarOffset) / 6;
        spannable.setSpan(new AbsoluteSizeSpan(size), 0, spannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        binding.toolbarBoardRead.setTitle(spannable);
        switch(state) {
            case STATE_COLLAPSED:
                //String userPic = (TextUtils.isEmpty(obj.getUserPic()))? Constants.imgPath + "ic_user_blank_gray" : obj.getUserPic();
                binding.toolbarBoardRead.setNavigationIcon(null);
                binding.toolbarBoardRead.setTitle(spannable);
                binding.toolbarBoardRead.setSubtitle(obj.getUserName());
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

    // Method for uploading the comment to Firestore
    private void uploadComment() {
        Map<String, Object> comment = new HashMap<>();
        comment.put("cnt_reply", 0);
        comment.put("comment", binding.etComment.getText().toString());
        comment.put("timestamp", FieldValue.serverTimestamp());
        comment.put("user_id", viewerId);

        postRef.collection("comments").add(comment).addOnSuccessListener(commentRef -> {
            queryPaginationUtil.setCommentQuery(postRef, "timestamp");
            postRef.update("cnt_comment", FieldValue.increment(1)).addOnSuccessListener(Void -> {
                cntComment++;
                binding.tvCntComment.setText(String.valueOf(cntComment));
                boardReadFeedAdapter.notifyItemChanged(COMMENT_HEADER, cntComment);
            });
        }).addOnFailureListener(Throwable::printStackTrace);

        imm.hideSoftInputFromWindow(binding.getRoot().getWindowToken(), 0);
        binding.constraintComment.setVisibility(View.GONE);
        isCommentVisible = !isCommentVisible;

        /*
        DocumentReference userRef = mDB.collection("users").document(viewerId);
        mDB.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot doc = transaction.get(userRef);
            List<?> nameList = (List<?>)doc.get("user_names");
            assert nameList != null;
            List<String> tempList = new ArrayList<>();
            for(Object obj : nameList) tempList.add((String)obj);

            comment.put("user_name", tempList.get(tempList.size() - 1));
            comment.put("user_pic", doc.getString("user_pic"));

            postRef.collection("comments").add(comment).addOnSuccessListener(commentRef -> {
                queryPaginationUtil.setCommentQuery(postRef, "timestamp");
                postRef.update("cnt_comment", FieldValue.increment(1)).addOnSuccessListener(Void -> {
                    cntComment++;
                    binding.tvCntComment.setText(String.valueOf(cntComment));
                    boardReadFeedAdapter.notifyItemChanged(COMMENT_HEADER, cntComment);
                });
            }).addOnFailureListener(Throwable::printStackTrace);

            imm.hideSoftInputFromWindow(binding.getRoot().getWindowToken(), 0);
            binding.constraintComment.setVisibility(View.GONE);
            isCommentVisible = !isCommentVisible;
            return null;
        });

         */
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


    // Display the auto club if the user has set the automaker, automodel, enginetype, and autoyear.
    /*
    private void showUserAutoClub(final TextView autoInfo) {
        mDB.collection("users").document(postOwnerId).get().addOnCompleteListener(task -> {
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
