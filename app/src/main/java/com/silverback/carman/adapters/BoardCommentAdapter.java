package com.silverback.carman.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.ListPopupWindow;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.silverback.carman.R;
import com.silverback.carman.databinding.ItemviewBoardCommentBinding;
import com.silverback.carman.databinding.ItemviewBoardReplyBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.ApplyImageResourceUtil;
import com.silverback.carman.utils.Constants;
import com.silverback.carman.utils.RecyclerDividerUtil;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class BoardCommentAdapter extends RecyclerView.Adapter<BoardCommentAdapter.ViewHolder> {

    private static final LoggingHelper log = LoggingHelperFactory.create(BoardCommentAdapter.class);

    private final DeleteCommentListener commentListener;
    private final FirebaseFirestore firestore;


    private final List<DocumentSnapshot> commentList;
    private final ApplyImageResourceUtil imageUtil;
    private final Context context;
    private final Context styleWrapper;

    private final ArrayAdapter arrayCommentAdapter;
    private final CommentReplyAdapter replyAdapter;
    private final RecyclerDividerUtil divider;

    private String viewerId;
    private List<DocumentSnapshot> replyList;

    public interface DeleteCommentListener {
        void deleteComment(String commentId, int position);
        void addCommentReply(DocumentSnapshot commentshot, CharSequence content);
    }

    // Constructor
    public BoardCommentAdapter(
            Context context, List<DocumentSnapshot> commentList, DeleteCommentListener listener) {
        this.context = context;
        this.commentList = commentList;
        this.commentListener = listener;

        styleWrapper = new ContextThemeWrapper(context, R.style.CarmanPopupMenu);
        firestore = FirebaseFirestore.getInstance();
        imageUtil = new ApplyImageResourceUtil(context);

        try (FileInputStream fis = context.openFileInput("userId");
             BufferedReader br = new BufferedReader(new InputStreamReader(fis))) {
            viewerId = br.readLine();
        } catch(IOException e) {e.printStackTrace();};

        final String[] items = {"delete", "report", "share"};
        arrayCommentAdapter = new ArrayAdapter<>(context, R.layout.board_comment_dropdown, items);

        divider = new RecyclerDividerUtil(Constants.DIVIDER_HEIGHT_POSTINGBOARD,
                0, ContextCompat.getColor(context, R.color.recyclerDivider));
        replyAdapter = CommentReplyAdapter.getInstance();
        replyAdapter.setImageUtl(imageUtil);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ItemviewBoardCommentBinding commentBinding;
        SimpleDateFormat sdf = new SimpleDateFormat("MM.dd HH:mm", Locale.getDefault());

        public ViewHolder(View itemView) {
            super(itemView);
            commentBinding = ItemviewBoardCommentBinding.bind(itemView);
        }

        // Getter for views.
        ImageView getUserImageView() {
            return commentBinding.imgCommentUser;
        }
        ImageView getOverflowView() { return commentBinding.imgOverflow; }
        ImageView getSendReplyView() { return commentBinding.imgbtnSendReply; }
        EditText getContentEditText() { return commentBinding.etCommentReply; }
        RecyclerView getRecyclerReplyView() { return commentBinding.recyclerviewReply; }

        void setCommentProfile(DocumentSnapshot doc) {
            commentBinding.tvCommentUser.setText(doc.getString("user_name"));
            commentBinding.tvCommentContent.setText(doc.getString("comment"));
            final Date date = doc.getDate("timestamp");
            if(date != null) commentBinding.tvCommentTimestamp.setText(sdf.format(date));

            // Temporary try catch b/c the db isn't complete.
            long count = Objects.requireNonNull(doc.getLong("cnt_reply"));
            commentBinding.headerReplyCnt.setText(String.valueOf(count));
        }

        void setReplyVisibility(){
            commentBinding.switchReply.setOnCheckedChangeListener((compoundButton, b) -> {
                if(b) commentBinding.linearReply.setVisibility(View.VISIBLE);
                else commentBinding.linearReply.setVisibility(View.GONE);
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
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DocumentSnapshot commentshot = commentList.get(position);
        holder.setCommentProfile(commentshot);
        holder.setReplyVisibility();

        setCommentUserPic(holder, commentshot);
        holder.getOverflowView().setOnClickListener(view -> showListPopupWindow(holder));
        holder.getSendReplyView().setOnClickListener(view -> uploadReply(holder, commentshot));


        if(Objects.requireNonNull(commentshot.getLong("cnt_reply")) > 0) {
            replyAdapter.setCommentReplyList(commentshot.getReference());
            LinearLayoutManager layout = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false);
            holder.getRecyclerReplyView().setLayoutManager(layout);
            holder.getRecyclerReplyView().addItemDecoration(divider);
            holder.getRecyclerReplyView().setHasFixedSize(false);
            holder.getRecyclerReplyView().setItemAnimator(new DefaultItemAnimator());
            holder.getRecyclerReplyView().setAdapter(replyAdapter);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int pos, @NonNull List<Object> payloads){
        if(payloads.isEmpty()) super.onBindViewHolder(holder, pos, payloads);
        else log.i("Partial Binding:");
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    private void setCommentUserPic(ViewHolder holder, DocumentSnapshot commentshot) {
        final String imgurl = (!TextUtils.isEmpty(commentshot.getString("user_pic")))?
                commentshot.getString("user_pic") : Constants.imgPath + "ic_user_blank_gray";

        int x = holder.getUserImageView().getWidth();
        int y = holder.getUserImageView().getHeight();
        imageUtil.applyGlideToImageView(Uri.parse(imgurl), holder.getUserImageView(), x, y, true);
    }

    private void uploadReply(ViewHolder holder, DocumentSnapshot commentshot) {
        final CharSequence content = holder.getReplyContent();
        if(TextUtils.isEmpty(content)) return;

        commentListener.addCommentReply(commentshot, content);
        holder.getContentEditText().getText().clear();
    }

    private void showListPopupWindow(ViewHolder holder) {
        //LayoutInflater inflater = LayoutInflater.from(context);
        //View view = inflater.inflate(R.layout.popup_comment, holder.commentBinding.getRoot(), false);
        ListPopupWindow popupWindow = new ListPopupWindow(context);
        popupWindow.setAnchorView(holder.getOverflowView());
        popupWindow.setHeight(300);
        popupWindow.setContentWidth(200);
        popupWindow.setHorizontalOffset(-190);
        Drawable background = ContextCompat.getDrawable(context, android.R.drawable.editbox_background);
        popupWindow.setBackgroundDrawable(background);
        popupWindow.setModal(true);
        popupWindow.setOnItemClickListener((parent, view, i, l) -> {
            log.i("click");
        });
        popupWindow.setAdapter(arrayCommentAdapter);
        popupWindow.show();


        /*
        PopupWindow dropdown = new PopupWindow(view,
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        Drawable background = ContextCompat.getDrawable(context, android.R.drawable.editbox_background);
        dropdown.setBackgroundDrawable(background);
        dropdown.showAsDropDown(holder.getOverflowView(), -150, -10);
        dropdown.setOverlapAnchor(true);
        dropdown.setOutsideTouchable(true);
        dropdown.update();

         */

        /*
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

         */
    }

    private static class CommentReplyAdapter extends RecyclerView.Adapter<CommentReplyAdapter.ViewHolder> {
        private List<DocumentSnapshot> replyList;
        private ApplyImageResourceUtil imgutil;

        private static class InnerClazz {
            private static final CommentReplyAdapter sInstance = new CommentReplyAdapter();
        }

        public static CommentReplyAdapter getInstance() {
            return InnerClazz.sInstance;
        }


        public static class ViewHolder extends RecyclerView.ViewHolder {
            ItemviewBoardReplyBinding replyBinding;
            public ViewHolder(View replyView) {
                super(replyView);
                replyBinding = ItemviewBoardReplyBinding.bind(replyView);
            }

            /*
            ItemviewBoardReplyBinding replyBinding;
            public ViewHolder(ItemviewBoardReplyBinding replyBinding) {
                super(replyBinding.getRoot());
                this.replyBinding = replyBinding;
            }
            */

            ImageView getReplyUserImage() { return replyBinding.imgReplyUser; }

            void setReplyProfile(DocumentSnapshot doc) {
                replyBinding.tvUserName.setText(doc.getString("user_name"));
                replyBinding.tvReplyTimestamp.setText(String.valueOf(doc.getDate("timestamp")));
                replyBinding.tvReplyContent.setText(doc.getString("reply_content"));
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View replyView = LayoutInflater.from(parent.getContext()).inflate(R.layout.itemview_board_reply, parent, false);
            return new ViewHolder(replyView);
            /*
            final LayoutInflater inflater = LayoutInflater.from(parent.getContext()
            ItemviewBoardReplyBinding replyBinding = ItemviewBoardReplyBinding.inflate(inflater);
            imgutil = new ApplyImageResourceUtil(parent.getContext());
            return new CommentReplyAdapter.ViewHolder(replyBinding);

             */
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            final DocumentSnapshot doc = replyList.get(position);
            holder.setReplyProfile(doc);

            final String imgurl = (!TextUtils.isEmpty(doc.getString("user_pic")))?
                    doc.getString("user_pic") : Constants.imgPath + "ic_user_blank_gray";
            setReplyUserPic(holder, imgurl);
        }

        @Override
        public int getItemCount() {
            log.i("replyList size: %s", replyList.size());
            return replyList.size();
        }

        public void setImageUtl(ApplyImageResourceUtil imgutil) {
            this.imgutil = imgutil;
        }

        public void setCommentReplyList(DocumentReference commentRef) {
            replyList = new ArrayList<>();
            commentRef.collection("replies").get().addOnSuccessListener(replyShot -> {
                for(DocumentSnapshot doc : replyShot) {
                    replyList.add(doc);
                }
            });
        }

        private void setReplyUserPic(ViewHolder holder, String imgPath) {
            int x = holder.getReplyUserImage().getWidth();
            int y = holder.getReplyUserImage().getHeight();
            imgutil.applyGlideToImageView(Uri.parse(imgPath), holder.getReplyUserImage(), x, y, true);
        }

    }

}
