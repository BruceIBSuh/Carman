package com.silverback.carman.adapters;

import static com.silverback.carman.BoardActivity.PAGING_REPLY;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.common.collect.Lists;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;
import com.silverback.carman.BoardActivity;
import com.silverback.carman.R;
import com.silverback.carman.databinding.ItemviewBoardReplyBinding;
import com.silverback.carman.databinding.PopupCommentOverflowBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.ApplyImageResourceUtil;
import com.silverback.carman.utils.Constants;
import com.silverback.carman.utils.PopupDropdownUtil;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BoardReplyAdapter extends RecyclerView.Adapter<BoardReplyAdapter.ViewHolder> {
    final LoggingHelper log = LoggingHelperFactory.create(BoardReplyAdapter.class);

    //private BoardCommentAdapter.CommentAdapterListener commentListener;
    private WeakReference<Context> weakContextRef;
    private FirebaseFirestore mDB;
    private ReplyAdapterListener replyAdapterListener;
    private BoardCommentAdapter.ViewHolder commentViewHolder;
    private AsyncListDiffer<DocumentSnapshot> mDiffer;
    private Query query;
    private DocumentReference commentRef;
    private QuerySnapshot querySnapshot;
    private List<DocumentSnapshot> replyList;
    private SimpleDateFormat sdf;

    public interface ReplyAdapterListener {
        void OnDeleteReply(BoardCommentAdapter.ViewHolder holder);
    }

    //private PopupDropdownUtil popupDropdownUtil;
    private String viewerId;

    // static instance using the lazy holder class.
    /*
    public BoardReplyAdapter(Context context, String viewerId, ReplyAdapterListener listener){
        this.context = context;
        this.viewerId = viewerId;
        this.replyAdapterListener = listener;

        mDiffer = new AsyncListDiffer<>(this, DIFF_CALLBACK_REPLY);
        replyList = new ArrayList<>();
        sdf = new SimpleDateFormat("MM.dd HH:mm", Locale.getDefault());
    }

     */

    public void setCommentViewHolder(BoardCommentAdapter.ViewHolder holder) {
        this.commentViewHolder = holder;
    }

    private static class InnerReplyAdapterClazz {
        private static final BoardReplyAdapter sInstance = new BoardReplyAdapter();
    }

    public static BoardReplyAdapter getInstance() {
        return InnerReplyAdapterClazz.sInstance;
    }

    public void initReplyAdapter(String viewerId) {
        this.viewerId = viewerId;

        mDB = FirebaseFirestore.getInstance();
        mDiffer = new AsyncListDiffer<>(this, DIFF_CALLBACK_REPLY);
        replyList = new ArrayList<>();
        sdf = new SimpleDateFormat("MM.dd HH:mm", Locale.getDefault());
    }


    /*
    public void setReplyAdapterListener(BoardCommentAdapter.CommentAdapterListener commentListener) {
        this.commentListener = commentListener;
    }

     */

    public void submitReplyList(List<DocumentSnapshot> replyList){
        mDiffer.submitList(Lists.newArrayList(replyList));//, recyclerListener::onRecyclerUpdateDone);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ItemviewBoardReplyBinding replyBinding;

        public ViewHolder(View replyView) {
            super(replyView);
            this.replyBinding = ItemviewBoardReplyBinding.bind(replyView);
        }

        TextView getUserNameView() { return replyBinding.tvUserName; }
        ImageView getReplyUserImage() { return replyBinding.imgReplyUser; }
        ImageView getOverflowView() { return replyBinding.imgReplyOverflow; }


    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View replyView = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.itemview_board_reply, parent, false);
        weakContextRef = new WeakReference<>(parent.getContext());
        return new ViewHolder(replyView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DocumentSnapshot doc = mDiffer.getCurrentList().get(position);
        String replyId = doc.getString("user_id");
        assert replyId != null;

        //((BoardActivity)context).setUserProfile(replyId, holder.getUserNameView(), holder.getReplyUserImage());
        setReplyUserProfile(replyId, holder.getUserNameView(), holder.getReplyUserImage());
        holder.replyBinding.tvReplyContent.setText(doc.getString("reply_content"));
        final Date date = doc.getDate("timestamp");
        if(date != null) holder.replyBinding.tvReplyTimestamp.setText(sdf.format(date));

        holder.getOverflowView().setOnClickListener(v -> showReplyPopupWindow(v, holder, doc));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int pos, @NonNull List<Object> payloads) {
        if(payloads.isEmpty()) super.onBindViewHolder(holder, pos, payloads);
        else log.i("reply adapter payloads: %s", payloads.get(0));
    }

    @Override
    public int getItemCount() {
        return mDiffer.getCurrentList().size();
    }

    public void queryCommentReply(DocumentReference commentRef, String field) {
        this.commentRef = commentRef;
        this.querySnapshot = null;
        replyList.clear();

        query = commentRef.collection("replies").orderBy(field, Query.Direction.DESCENDING);
        query.limit(PAGING_REPLY).get().addOnSuccessListener(querySnapshot -> {
            if(querySnapshot.isEmpty()) return;
            this.querySnapshot = querySnapshot;

            log.i("queried replyshot: %s", querySnapshot.size());
            for(DocumentSnapshot doc : querySnapshot) replyList.add(doc);
            submitReplyList(replyList);
        });


    }

    public void queryNextReply() {
        DocumentSnapshot lastVisible = querySnapshot.getDocuments().get(querySnapshot.size() - 1);
        query.startAfter(lastVisible).limit(PAGING_REPLY).get().addOnSuccessListener(nextShots -> {
            this.querySnapshot = nextShots;
            for(DocumentSnapshot comment : nextShots) replyList.add(comment);
            submitReplyList(replyList);

        }).addOnFailureListener(Throwable::printStackTrace);
    }

    private void setReplyUserProfile(String userId, TextView textView, ImageView imageView) {
       mDB.collection("users").document(userId).get().addOnSuccessListener(user -> {
            if(user.get("user_names") != null) {
                List<?> names = (List<?>)user.get("user_names");
                assert names != null;
                textView.setText((String)names.get(names.size() - 1));
            }

            Uri userImage = null;
            if(user.getString("user_pic") != null) userImage = Uri.parse(user.getString("user_pic"));
            Glide.with(weakContextRef.get()).load(userImage)
                    .placeholder(R.drawable.ic_user_blank_white)
                    .fitCenter().circleCrop().into(imageView);
       });
    }


    private void showReplyPopupWindow(View view, ViewHolder holder, DocumentSnapshot doc) {
        LayoutInflater inflater = LayoutInflater.from(view.getContext());
        View contentView = inflater.inflate(R.layout.popup_comment_overflow, holder.replyBinding.getRoot(), false);

        PopupDropdownUtil popupDropdownUtil = PopupDropdownUtil.getInstance();
        popupDropdownUtil.setInitParams(contentView, holder.getOverflowView(), doc);
        PopupWindow dropdown = popupDropdownUtil.createPopupWindow();

        PopupCommentOverflowBinding popupBinding = PopupCommentOverflowBinding.bind(contentView);
        if(viewerId.equals(doc.getString("user_id"))) {
            popupBinding.tvPopup1.setVisibility(View.VISIBLE);
            popupBinding.tvPopup1.setOnClickListener(v -> {
                doc.getReference().delete().addOnSuccessListener(Void -> {
                    commentRef.update("cnt_reply",FieldValue.increment(-1));
                    replyList.remove(holder.getBindingAdapterPosition());
                    submitReplyList(replyList);
                    replyAdapterListener.OnDeleteReply(commentViewHolder);
                });
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

    private static final DiffUtil.ItemCallback<DocumentSnapshot> DIFF_CALLBACK_REPLY = new DiffUtil.ItemCallback<DocumentSnapshot>() {
        @Override
        public boolean areItemsTheSame(@NonNull DocumentSnapshot oldItem, @NonNull DocumentSnapshot newItem) {
            return oldItem.getId().equals(newItem.getId());
        }
        @Override
        public boolean areContentsTheSame(@NonNull DocumentSnapshot oldItem, @NonNull DocumentSnapshot newItem) {
            return oldItem.equals(newItem);
        }
        public Object getChangePayload(@NonNull DocumentSnapshot oldItem, @NonNull DocumentSnapshot newItem) {
            return super.getChangePayload(oldItem, newItem);
        }
    };


}
