package com.silverback.carman.adapters;

import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupWindow;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.silverback.carman.R;
import com.silverback.carman.databinding.ItemviewBoardReplyBinding;
import com.silverback.carman.databinding.PopupCommentOverflowBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.ApplyImageResourceUtil;
import com.silverback.carman.utils.Constants;
import com.silverback.carman.utils.PopupDropdownUtil;

import java.util.ArrayList;
import java.util.List;

public class CommentReplyAdapter extends RecyclerView.Adapter<CommentReplyAdapter.ViewHolder> {
    final LoggingHelper log = LoggingHelperFactory.create(CommentReplyAdapter.class);
    //private CommentAdapterListener callback;
    private DocumentReference commentRef;
    private List<DocumentSnapshot> replyList;
    private ApplyImageResourceUtil imgutil;
    private PopupDropdownUtil popupDropdownUtil;
    private String viewerId;

    // static instance using the lazy holder class.
    private CommentReplyAdapter(){}
    private static class InnerClazz {
        private static final CommentReplyAdapter sInstance = new CommentReplyAdapter();
    }
    public static CommentReplyAdapter getInstance() {
        return InnerClazz.sInstance;
    }

    public void setReplyInitParams(
            PopupDropdownUtil dropdownUtil, ApplyImageResourceUtil imgutil, String viewerId) {
        this.popupDropdownUtil = dropdownUtil;
        this.imgutil = imgutil;
        this.viewerId = viewerId;
    }

    public void setReplyAdapterListener(BoardCommentAdapter.CommentAdapterListener callback) {
        //this.callback = callback;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ItemviewBoardReplyBinding replyBinding;
        public ViewHolder(View replyView) {
            super(replyView);
            replyBinding = ItemviewBoardReplyBinding.bind(replyView);
        }

        ImageView getReplyUserImage() { return replyBinding.imgReplyUser; }
        ImageView getOverflowView() { return replyBinding.imgReplyOverflow; }

        void setReplyProfile(DocumentSnapshot doc) {
            replyBinding.tvUserName.setText(doc.getString("user_name"));
            replyBinding.tvReplyTimestamp.setText(String.valueOf(doc.getDate("timestamp")));
            replyBinding.tvReplyContent.setText(doc.getString("reply_content"));
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View replyView = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.itemview_board_reply, parent, false);
        return new ViewHolder(replyView);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentReplyAdapter.ViewHolder holder, int position) {
        final DocumentSnapshot doc = replyList.get(position);
        holder.setReplyProfile(doc);
        setReplyUserPic(holder, doc);
        holder.getOverflowView().setOnClickListener(view ->
                showReplyPopupWindow(view, holder, doc, position));
    }

    @Override
    public int getItemCount() {
        return replyList.size();
    }

    public void setCommentReplyList(DocumentReference commentRef) {
        replyList = new ArrayList<>();
        this.commentRef = commentRef;
        commentRef.collection("replies").get().addOnSuccessListener(replyShot -> {
            for(DocumentSnapshot doc : replyShot) replyList.add(doc);
        });
    }

    private void setReplyUserPic(CommentReplyAdapter.ViewHolder holder, DocumentSnapshot doc) {
        final String imgurl = (!TextUtils.isEmpty(doc.getString("user_pic")))?
                doc.getString("user_pic") : Constants.imgPath + "ic_user_blank_gray";
        int x = holder.getReplyUserImage().getWidth();
        int y = holder.getReplyUserImage().getHeight();
        imgutil.applyGlideToImageView(Uri.parse(imgurl), holder.getReplyUserImage(), x, y, true);
    }

    private void showReplyPopupWindow(
            View view, CommentReplyAdapter.ViewHolder holder, DocumentSnapshot doc, int pos){

        LayoutInflater inflater = LayoutInflater.from(view.getContext());
        View contentView = inflater.inflate(
                R.layout.popup_comment_overflow, holder.replyBinding.getRoot(), false);
        popupDropdownUtil.setInitParams(contentView, holder.getOverflowView(), doc);
        PopupWindow dropdown = popupDropdownUtil.createPopupWindow();

        PopupCommentOverflowBinding popupBinding = PopupCommentOverflowBinding.bind(contentView);
        if(viewerId.equals(doc.getString("user_id"))) {
            popupBinding.tvPopup1.setVisibility(View.VISIBLE);
            popupBinding.tvPopup1.setOnClickListener(v -> {
                //callback.deleteCommentReply(this, commentRef.getId(), doc.getId(), pos);
                dropdown.dismiss();
            });
        }

        popupBinding.tvPopup2.setOnClickListener(v -> {
            log.i("menu2");
            dropdown.dismiss();
        });
        popupBinding.tvPopup3.setOnClickListener(v -> {
            log.i("menu3");
            dropdown.dismiss();
        });
    }


}
