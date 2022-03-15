package com.silverback.carman.adapters;

import static com.silverback.carman.BoardActivity.PAGING_REPLY;

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
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
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

public class BoardReplyAdapter extends RecyclerView.Adapter<BoardReplyAdapter.ViewHolder> {
    final LoggingHelper log = LoggingHelperFactory.create(BoardReplyAdapter.class);

    private BoardCommentAdapter.CommentAdapterListener commentListener;
    private Query query;
    private DocumentReference commentRef;
    private QuerySnapshot querySnapshot;
    private List<DocumentSnapshot> replyList;
    private ApplyImageResourceUtil imgutil;
    private PopupDropdownUtil popupDropdownUtil;
    private String viewerId;

    // static instance using the lazy holder class.
    private BoardReplyAdapter(){}

    private static class InnerReplyAdapterClazz {
        private static final BoardReplyAdapter sInstance = new BoardReplyAdapter();
    }
    public static BoardReplyAdapter getInstance() {
        return InnerReplyAdapterClazz.sInstance;
    }

    public void setReplyInitParams(
            PopupDropdownUtil dropdownUtil, ApplyImageResourceUtil imgutil, String viewerId) {
        this.popupDropdownUtil = dropdownUtil;
        this.imgutil = imgutil;
        this.viewerId = viewerId;

        replyList = new ArrayList<>();
    }

    public void setReplyAdapterListener(BoardCommentAdapter.CommentAdapterListener commentListener) {
        this.commentListener = commentListener;
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
    public void onBindViewHolder(@NonNull BoardReplyAdapter.ViewHolder holder, int position) {
        final DocumentSnapshot doc = replyList.get(position);
        holder.setReplyProfile(doc);
        setReplyUserPic(holder, doc);
        holder.getOverflowView().setOnClickListener(v -> showReplyPopupWindow(v, holder, doc, position));
    }

    @Override
    public void onBindViewHolder(
            @NonNull BoardReplyAdapter.ViewHolder holder, int pos, @NonNull List<Object> payloads) {
        if(payloads.isEmpty()) super.onBindViewHolder(holder, pos, payloads);
        else {
            log.i("reply adapter payloads: %s", payloads.get(0));
        }
    }

    @Override
    public int getItemCount() {
        return replyList.size();
    }

    public void queryCommentReply(DocumentReference commentRef) {
        replyList.clear();
        this.commentRef = commentRef;
        querySnapshot = null;

        query = commentRef.collection("replies").orderBy("timestamp", Query.Direction.DESCENDING);
        query.limit(PAGING_REPLY).addSnapshotListener((querySnapshot, e) -> {
            if(e != null) return;
            this.querySnapshot = querySnapshot;
            if((querySnapshot != null)) for(DocumentSnapshot doc : querySnapshot) replyList.add(doc);
            notifyItemRangeChanged(0, replyList.size(), true);
            commentListener.notifyLoadingReplyDone();
        });

        /*
        query.limit(PAGING_REPLY).get().addOnSuccessListener(replyshots -> {
            this.querySnapshot = replyshots;
            for(DocumentSnapshot doc : replyshots) replyList.add(doc);
        });

         */
    }

    public void queryNextReply() {
        DocumentSnapshot lastVisible = querySnapshot.getDocuments().get(querySnapshot.size() - 1);
        query.startAfter(lastVisible).limit(PAGING_REPLY).get().addOnSuccessListener(replies -> {
            this.querySnapshot = replies;
            final int start = replyList.size();

            for(DocumentSnapshot comment : replies) replyList.add(comment);
            notifyItemRangeChanged(start, replyList.size(), true);

            commentListener.notifyLoadingReplyDone();

        }).addOnFailureListener(Throwable::printStackTrace);
    }



    private void setReplyUserPic(BoardReplyAdapter.ViewHolder holder, DocumentSnapshot doc) {
        final String imgurl = (!TextUtils.isEmpty(doc.getString("user_pic")))?
                doc.getString("user_pic") : Constants.imgPath + "ic_user_blank_gray";
        int x = holder.getReplyUserImage().getWidth();
        int y = holder.getReplyUserImage().getHeight();
        imgutil.applyGlideToImageView(Uri.parse(imgurl), holder.getReplyUserImage(), x, y, true);
    }


    public void showReplyPopupWindow(
            View view, BoardReplyAdapter.ViewHolder holder, DocumentSnapshot doc, int pos){

        LayoutInflater inflater = LayoutInflater.from(view.getContext());
        View contentView = inflater.inflate(
                R.layout.popup_comment_overflow, holder.replyBinding.getRoot(), false);
        popupDropdownUtil.setInitParams(contentView, holder.getOverflowView(), doc);
        PopupWindow dropdown = popupDropdownUtil.createPopupWindow();

        PopupCommentOverflowBinding popupBinding = PopupCommentOverflowBinding.bind(contentView);
        if(viewerId.equals(doc.getString("user_id"))) {
            popupBinding.tvPopup1.setVisibility(View.VISIBLE);
            popupBinding.tvPopup1.setOnClickListener(v -> {
                commentListener.deleteCommentReply(this, commentRef.getId(), doc.getId(), pos);
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
