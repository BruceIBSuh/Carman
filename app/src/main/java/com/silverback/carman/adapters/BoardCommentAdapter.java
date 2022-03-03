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
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.silverback.carman.R;
import com.silverback.carman.databinding.ItemviewBoardCommentBinding;
import com.silverback.carman.databinding.ItemviewBoardReplyBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.ApplyImageResourceUtil;
import com.silverback.carman.utils.Constants;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BoardCommentAdapter extends RecyclerView.Adapter<BoardCommentAdapter.ViewHolder> {

    private static final LoggingHelper log = LoggingHelperFactory.create(BoardCommentAdapter.class);

    private final FirebaseFirestore firestore;
    private PopupMenu popupMenu;
    private final List<DocumentSnapshot> commentList;
    private final SimpleDateFormat sdf;
    private final ApplyImageResourceUtil imageUtil;
    private final Context context;
    private final Context styleWrapper;

    // Constructor
    public BoardCommentAdapter(Context context, List<DocumentSnapshot> snapshotList) {
        this.context = context;
        this.commentList = snapshotList;

        styleWrapper = new ContextThemeWrapper(context, R.style.CarmanPopupMenu);
        firestore = FirebaseFirestore.getInstance();
        sdf = new SimpleDateFormat("MM.dd HH:mm", Locale.getDefault());
        imageUtil = new ApplyImageResourceUtil(context);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ItemviewBoardCommentBinding commentBinding;
        public ViewHolder(View itemView) {
            super(itemView);
            commentBinding = ItemviewBoardCommentBinding.bind(itemView);
        }


        TextView getCommentTimestampView() {
            return commentBinding.tvCommentTimestamp;
        }
        ImageView getUserImageView() {
            return commentBinding.imgCommentUser;
        }
        ImageView getOverflowView() { return commentBinding.imgOverflow; }

        void setCommentProfile(DocumentSnapshot doc) {
            commentBinding.tvCommentUser.setText(doc.getString("user_name"));
            commentBinding.tvCommentContent.setText(doc.getString("comment"));
        }

        void dispReplyToComment(){
            commentBinding.switchReply.setOnCheckedChangeListener((compoundButton, b) -> {
                if(b) commentBinding.recyclerReply.setVisibility(View.VISIBLE);
                else commentBinding.recyclerReply.setVisibility(View.GONE);
            });
        }



    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(context).inflate(R.layout.itemview_board_comment, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DocumentSnapshot comment = commentList.get(position);
        // user name and comment content
        holder.setCommentProfile(comment);
        // timestamp
        final Date date = comment.getDate("timestamp");
        if(date != null) holder.getCommentTimestampView().setText(sdf.format(date));
        // user image
        final String imgurl = (!TextUtils.isEmpty(comment.getString("user_pic")))?
                comment.getString("user_pic") : Constants.imgPath + "ic_user_blank_gray";
        setUserImage(holder, imgurl);
        // popup menu event handler when clicking the overflow image button
        setPopupMenu(holder);

        /*
        final String userId = comment.getString("userId");
        if(userId != null && !TextUtils.isEmpty(userId)) {
            firestore.collection("users").document(userId).get().addOnCompleteListener(task -> {
                if(task.isSuccessful()) {
                    DocumentSnapshot doc = task.getResult();
                    log.i("user name: %s", doc.getString("user_name"));
                    //holder.getCommentUserView().setText(doc.getString("user_name"));
                    if(!TextUtils.isEmpty(doc.getString("user_pic")))
                        setUserImage(holder, doc.getString("user_pic"));
                    else setUserImage(holder, Constants.imgPath + "ic_user_blank_gray");
                }
            });
        }

         */

        holder.dispReplyToComment();
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

    private void setPopupMenu(ViewHolder holder) {
        holder.getOverflowView().setOnClickListener(view -> {
            popupMenu = new PopupMenu(styleWrapper, view);
            popupMenu.getMenuInflater().inflate(R.menu.popup_board_comment, popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(menuItem -> {
                if(menuItem.getItemId() == R.id.board_comment_delete) {
                    log.i("popup delete");
                } else if(menuItem.getItemId() == R.id.board_comment_report) {
                    log.i("popup report");
                } else if(menuItem.getItemId() == R.id.board_comment_share) {
                    log.i("popup share");
                }

                return false;
            });

            popupMenu.show();

        });
    }

    private static class ReplyAdapter extends RecyclerView.Adapter<ReplyAdapter.ViewHolder> {
        private ItemviewBoardReplyBinding replyBinding;
        private List<DocumentSnapshot> replyList;

        public ReplyAdapter(Context context, List<DocumentSnapshot> replyList) {
            this.replyList = replyList;
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            public ViewHolder(ItemviewBoardReplyBinding replyBinding) {
                super(replyBinding.getRoot());
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            ItemviewBoardReplyBinding replyBinding = ItemviewBoardReplyBinding.inflate(inflater);
            return new ReplyAdapter.ViewHolder(replyBinding);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        }

        @Override
        public int getItemCount() {
            return replyList.size();
        }
    }

}
