package com.silverback.carman.adapters;

import android.content.Context;
import android.net.Uri;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ServerTimestamp;
import com.google.firebase.firestore.Transaction;
import com.silverback.carman.R;
import com.silverback.carman.databinding.ItemviewBoardCommentBinding;
import com.silverback.carman.databinding.ItemviewBoardReplyBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.ApplyImageResourceUtil;
import com.silverback.carman.utils.Constants;
import com.silverback.carman.utils.RecyclerDividerUtil;

import org.w3c.dom.Comment;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class BoardCommentAdapter extends RecyclerView.Adapter<BoardCommentAdapter.ViewHolder> {

    private static final LoggingHelper log = LoggingHelperFactory.create(BoardCommentAdapter.class);

    private final DeleteCommentListener commentListener;
    private final FirebaseFirestore firestore;

    private final List<DocumentSnapshot> commentList;
    private final ApplyImageResourceUtil imageUtil;
    private final Context context;
    private final Context styleWrapper;
    private CommentReplyAdapter replyAdapter;

    private String viewerId;
    private List<DocumentSnapshot> replyList;

    public interface DeleteCommentListener {
        void deleteComment(String commentId, int position);
        void addCommentReply(DocumentSnapshot comment, CharSequence content);
    }

    // Constructor
    public BoardCommentAdapter(
            Context context, List<DocumentSnapshot> snapshotList, DeleteCommentListener listener) {
        this.context = context;
        this.commentList = snapshotList;
        this.commentListener = listener;

        styleWrapper = new ContextThemeWrapper(context, R.style.CarmanPopupMenu);
        firestore = FirebaseFirestore.getInstance();
        imageUtil = new ApplyImageResourceUtil(context);



        try (FileInputStream fis = context.openFileInput("userId");
             BufferedReader br = new BufferedReader(new InputStreamReader(fis))) {
            viewerId = br.readLine();
        } catch(IOException e) {e.printStackTrace();};

    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ItemviewBoardCommentBinding commentBinding;
        SimpleDateFormat sdf = new SimpleDateFormat("MM.dd HH:mm", Locale.getDefault());

        public ViewHolder(View itemView) {
            super(itemView);
            commentBinding = ItemviewBoardCommentBinding.bind(itemView);
        }

        ImageView getUserImageView() {
            return commentBinding.imgCommentUser;
        }
        ImageView getOverflowView() { return commentBinding.imgOverflow; }
        ImageView getSendReplyView() { return commentBinding.imgbtnSendReply; }

        void setCommentProfile(DocumentSnapshot doc) {
            commentBinding.tvCommentUser.setText(doc.getString("user_name"));
            commentBinding.tvCommentContent.setText(doc.getString("comment"));
            final Date date = doc.getDate("timestamp");
            if(date != null) commentBinding.tvCommentTimestamp.setText(sdf.format(date));
            long count = Objects.requireNonNull(doc.getLong("cnt_reply"));
            commentBinding.headerReplyCnt.setText(String.valueOf(count));
        }

        void dispReplyToComment(){
            commentBinding.switchReply.setOnCheckedChangeListener((compoundButton, b) -> {
                if(b) commentBinding.recyclerReply.setVisibility(View.VISIBLE);
                else commentBinding.recyclerReply.setVisibility(View.GONE);
            });
        }

        CharSequence getReplyContent() {
            CharSequence content = commentBinding.etCommentReply.getText();
            if(TextUtils.isEmpty(content)) {
                Snackbar.make(commentBinding.getRoot(), "no content", Snackbar.LENGTH_SHORT).show();
                return null;
            } else return content;
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(context).inflate(R.layout.itemview_board_comment, parent, false);

        LinearLayoutManager layout = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false);
        RecyclerDividerUtil divider = new RecyclerDividerUtil(Constants.DIVIDER_HEIGHT_POSTINGBOARD,
                0, ContextCompat.getColor(context, R.color.recyclerDivider));

        // RecyclerView for showing comments
        /*
        replyAdapter = CommentReplyAdapter.getInstance();

        RecyclerView recyclerReplyView = itemView.findViewById(R.id.recyclerView_reply);
        recyclerReplyView.setHasFixedSize(false); //due to banner plugin
        recyclerReplyView.setLayoutManager(layout);
        recyclerReplyView.addItemDecoration(divider);
        recyclerReplyView.setItemAnimator(new DefaultItemAnimator());
        recyclerReplyView.setAdapter(replyAdapter);
        */
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DocumentSnapshot comment = commentList.get(position);
        //replyAdapter.setReplyList(comment.getReference());

        holder.setCommentProfile(comment);
        final String imgurl = (!TextUtils.isEmpty(comment.getString("user_pic")))?
                comment.getString("user_pic") : Constants.imgPath + "ic_user_blank_gray";
        setUserImage(holder, imgurl);

        holder.dispReplyToComment();
        holder.getOverflowView().setOnClickListener(view -> setPopupMenu(holder, comment, position));
        holder.getSendReplyView().setOnClickListener(view -> uploadReply(holder, comment, position));


    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull List<Object> payloads) {
        if(payloads.isEmpty()) super.onBindViewHolder(holder, position, payloads);
        else {
            DocumentSnapshot snapshot = (DocumentSnapshot)payloads.get(0);
            log.i("Partial Binding: %s", snapshot.getString("user"));
        }

    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }


    private void setUserImage(ViewHolder holder, String imgPath) {
        int x = holder.getUserImageView().getWidth();
        int y = holder.getUserImageView().getHeight();
        imageUtil.applyGlideToImageView(Uri.parse(imgPath), holder.getUserImageView(), x, y, true);
    }

    private void uploadReply(ViewHolder holder, DocumentSnapshot comment, int pos) {
        final CharSequence content = holder.getReplyContent();
        if(TextUtils.isEmpty(content)) return;
        commentListener.addCommentReply(comment, content);


        /*
        Map<String, Object> object = new HashMap<>();
        object.put("user_id", viewerId);
        object.put("timestamp", FieldValue.serverTimestamp());
        object.put("reply_content", content);
        log.i("reply profile: %s, %s, %s, %s", viewerId, FieldValue.serverTimestamp(), content, comment.getId());

        final DocumentReference docref = firestore.collection("users").document(viewerId);
        firestore.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot doc = transaction.get(docref);
            object.put("user_name", doc.getString("user_name"));
            object.put("user_pic", doc.getString("user_pic"));

            comment.getReference().collection("replies").add(object).addOnSuccessListener(aVoid -> {
                comment.getReference().update("cnt_reply", FieldValue.increment(1));
            }).addOnFailureListener(Throwable::printStackTrace);

            return null;
        });

         */
    }

    private void setPopupMenu(ViewHolder holder, DocumentSnapshot comment, int position) {
        final String commentId = comment.getId();
        final String ownerId = comment.getString("user_id");

        PopupMenu popupMenu = new PopupMenu(styleWrapper, holder.getOverflowView());
        popupMenu.getMenuInflater().inflate(R.menu.popup_board_comment, popupMenu.getMenu());

        boolean visible = ownerId != null && ownerId.equals(viewerId);
        popupMenu.getMenu().findItem(R.id.board_comment_delete).setVisible(visible);

        popupMenu.setOnMenuItemClickListener(menuItem -> {
            if(menuItem.getItemId() == R.id.board_comment_delete) {
                commentListener.deleteComment(commentId, position);

            } else if(menuItem.getItemId() == R.id.board_comment_report) {
                log.i("popup report");

            } else if(menuItem.getItemId() == R.id.board_comment_share) {
                log.i("popup share");
            }

            return false;
        });

        popupMenu.show();
    }

    private static class CommentReplyAdapter extends RecyclerView.Adapter<CommentReplyAdapter.ViewHolder> {
        private List<DocumentSnapshot> replyList;

        private static class InnerClazz {
            private static final CommentReplyAdapter sInstance = new CommentReplyAdapter();
        }

        public static CommentReplyAdapter getInstance() {
            return InnerClazz.sInstance;
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            ItemviewBoardReplyBinding replyBinding;
            public ViewHolder(ItemviewBoardReplyBinding replyBinding) {
                super(replyBinding.getRoot());
                this.replyBinding = replyBinding;
            }

            void setReplyProfile(DocumentSnapshot reply) {
                replyBinding.tvReplyContent.setText(reply.getString("reply_content"));
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            ItemviewBoardReplyBinding replyBinding = ItemviewBoardReplyBinding.inflate(inflater);
            return new CommentReplyAdapter.ViewHolder(replyBinding);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            final DocumentSnapshot doc = replyList.get(position);
            holder.setReplyProfile(doc);
        }

        @Override
        public int getItemCount() {
            return replyList.size();
        }

        public void setReplyList(DocumentReference commentRef) {
            replyList = new ArrayList<>();
            commentRef.collection("replies").get().addOnSuccessListener(replyShot -> {
                for(DocumentSnapshot doc : replyShot) replyList.add(doc);
            });
        }

    }

}
