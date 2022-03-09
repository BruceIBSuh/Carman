package com.silverback.carman.adapters;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.PopupWindow;

import androidx.annotation.NonNull;
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
import com.silverback.carman.databinding.PopupCommentOverflowBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.ApplyImageResourceUtil;
import com.silverback.carman.utils.Constants;
import com.silverback.carman.utils.PopupDropdownUtil;
import com.silverback.carman.utils.RecyclerDividerUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class BoardCommentAdapter extends RecyclerView.Adapter<BoardCommentAdapter.ViewHolder> {

    private static final LoggingHelper log = LoggingHelperFactory.create(BoardCommentAdapter.class);

    private final PopupMenuListener commentListener;
    private final FirebaseFirestore firestore;

    private final List<DocumentSnapshot> commentList;
    private final ApplyImageResourceUtil imageUtil;
    private final PopupDropdownUtil popupDropdownUtil;

    private final Context context;
    //private final Context styleWrapper; // for the PopupMenu

    //private final ArrayAdapter arrayCommentAdapter;
    private final CommentReplyAdapter replyAdapter;
    private final RecyclerDividerUtil divider;

    //private ListPopupWindow popupWindow;

    private final String viewerId;
    private List<DocumentSnapshot> replyList;

    public interface PopupMenuListener {
        void deleteComment(String commentId);
        void addCommentReply(DocumentSnapshot commentshot, String content);
        void test();
    }

    // Constructor
    public BoardCommentAdapter(
            Context context, List<DocumentSnapshot> commentList, String viewerId,
            PopupMenuListener listener) {
        this.context = context;
        this.commentList = commentList;
        this.viewerId = viewerId;
        this.commentListener = listener;

        //styleWrapper = new ContextThemeWrapper(context, R.style.CarmanPopupMenu);
        firestore = FirebaseFirestore.getInstance();
        imageUtil = new ApplyImageResourceUtil(context);
        popupDropdownUtil = PopupDropdownUtil.getInstance();
        divider = new RecyclerDividerUtil(Constants.DIVIDER_HEIGHT_POSTINGBOARD,
                0, ContextCompat.getColor(context, R.color.recyclerDivider));

        // Intantiate CommentReplyAdapter
        replyAdapter = CommentReplyAdapter.getInstance();
        replyAdapter.setReplyInitParams(context, popupDropdownUtil, imageUtil, viewerId);
        replyAdapter.setListener(listener);

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

        String getReplyContent() {
            String content = commentBinding.etCommentReply.getText().toString();
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
        DocumentSnapshot doc = commentList.get(position);
        holder.setCommentProfile(doc);
        holder.setReplyVisibility();

        setCommentUserPic(holder, doc);
        holder.getOverflowView().setOnClickListener(view -> showCommentPopupWindow(holder, doc));
        holder.getSendReplyView().setOnClickListener(view -> uploadReply(holder, doc));

        if(Objects.requireNonNull(doc.getLong("cnt_reply")) > 0) {
            replyAdapter.setCommentReplyList(doc.getReference());
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

    private void setCommentUserPic(ViewHolder holder, DocumentSnapshot doc) {
        final String imgurl = (!TextUtils.isEmpty(doc.getString("user_pic")))?
                doc.getString("user_pic") : Constants.imgPath + "ic_user_blank_gray";

        int x = holder.getUserImageView().getWidth();
        int y = holder.getUserImageView().getHeight();
        imageUtil.applyGlideToImageView(Uri.parse(imgurl), holder.getUserImageView(), x, y, true);
    }


    private void uploadReply(ViewHolder holder, DocumentSnapshot commentshot) {
        final String content = holder.getReplyContent();
        if(TextUtils.isEmpty(content)) return;

        //commentListener.addCommentReply(commentshot, content);
        holder.getContentEditText().getText().clear();
    }

    private void showCommentPopupWindow(ViewHolder holder, DocumentSnapshot doc) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.popup_comment_overflow, holder.commentBinding.getRoot(), false);
        popupDropdownUtil.setInitParams(view, holder.getOverflowView(), doc);
        PopupWindow dropdown = popupDropdownUtil.createPopupWindow();
        PopupCommentOverflowBinding binding = PopupCommentOverflowBinding.bind(view);
        if(viewerId.equals(doc.getString("user_id"))) {
            log.i("remove document");
            binding.tvPopup1.setVisibility(View.VISIBLE);
            binding.tvPopup1.setOnClickListener(v -> {
                log.i("remove listener");
                commentListener.deleteComment(doc.getId());
                dropdown.dismiss();
            });
        }

        binding.tvPopup2.setOnClickListener(v -> {
            log.i("menu2");
            commentListener.test();
            dropdown.dismiss();
        });
        binding.tvPopup3.setOnClickListener(v -> {
            log.i("menu3");
            dropdown.dismiss();
        });


    }

    // RecyclerView Adapter for the comment reply.
    private static class CommentReplyAdapter extends RecyclerView.Adapter<CommentReplyAdapter.ViewHolder> {
        //private Context context;
        private PopupMenuListener callback;
        private List<DocumentSnapshot> replyList;
        private ApplyImageResourceUtil imgutil;
        private PopupDropdownUtil popupDropdownUtil;
        private String viewerId;

        private CommentReplyAdapter(){ }

        private static class InnerClazz {
            private static final CommentReplyAdapter sInstance = new CommentReplyAdapter();
        }

        public static CommentReplyAdapter getInstance() {
            return InnerClazz.sInstance;
        }

        public void setReplyInitParams(
                Context context, PopupDropdownUtil dropdownUtil,
                ApplyImageResourceUtil imgutil, String viewerId) {
            //this.context = context;
            this.popupDropdownUtil = dropdownUtil;
            this.imgutil = imgutil;
            this.viewerId = viewerId;
        }
        
        public void setListener(PopupMenuListener listener) {
            this.callback = listener;
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

            holder.getOverflowView().setOnClickListener(view -> {
                LayoutInflater inflater = LayoutInflater.from(view.getContext());
                View contentView = inflater.inflate(
                        R.layout.popup_comment_overflow, holder.replyBinding.getRoot(), false);
                popupDropdownUtil.setInitParams(contentView, holder.getOverflowView(), doc);
                PopupWindow dropdown = popupDropdownUtil.createPopupWindow();

                PopupCommentOverflowBinding popupBinding = PopupCommentOverflowBinding.bind(contentView);
                if(viewerId.equals(doc.getString("user_id"))) {
                    log.i("remove document");
                    popupBinding.tvPopup1.setVisibility(View.VISIBLE);
                    popupBinding.tvPopup1.setOnClickListener(v -> {
                        log.i("remove listener");
                        callback.deleteComment(doc.getId());
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



            });


        }

        @Override
        public int getItemCount() {
            return replyList.size();
        }

        public void setCommentReplyList(DocumentReference commentRef) {
            replyList = new ArrayList<>();
            commentRef.collection("replies").get().addOnSuccessListener(replyShot -> {
                for(DocumentSnapshot doc : replyShot) replyList.add(doc);
            });
        }

        private void setReplyUserPic(ViewHolder holder, DocumentSnapshot doc) {
            final String imgurl = (!TextUtils.isEmpty(doc.getString("user_pic")))?
                    doc.getString("user_pic") : Constants.imgPath + "ic_user_blank_gray";
            int x = holder.getReplyUserImage().getWidth();
            int y = holder.getReplyUserImage().getHeight();
            imgutil.applyGlideToImageView(Uri.parse(imgurl), holder.getReplyUserImage(), x, y, true);
        }

    }



    // The comment and reply overflow event handler
    /*
    private void showListPopupWindow(ViewHolder holder, DocumentSnapshot doc) {
        //ListPopupWindow
        int[] size = measurePopupContentSize(arrayCommentAdapter);
        log.i("content size: %s, %s", size[0], size[1]);
        popupWindow = new ListPopupWindow(context);
        popupWindow.setAnchorView(holder.getOverflowView());
        popupWindow.setHeight(220);
        popupWindow.setContentWidth(180);
        popupWindow.setHorizontalOffset(-160);
        Drawable background = ContextCompat.getDrawable(context, android.R.drawable.editbox_background);
        popupWindow.setBackgroundDrawable(background);
        popupWindow.setModal(true);
        popupWindow.setOnItemClickListener((parent, view, i, l) -> {
            log.i("click");
        });
        popupWindow.setAdapter(arrayCommentAdapter);
        popupWindow.show();

        // PopupWindow
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.popup_comment_overflow, holder.commentBinding.getRoot(), false);
        PopupWindow dropdown = new PopupWindow(view,
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        Drawable background = ContextCompat.getDrawable(context, android.R.drawable.editbox_background);
        dropdown.setBackgroundDrawable(background);
        dropdown.showAsDropDown(holder.getOverflowView(), -120, -20);
        dropdown.setOverlapAnchor(true);
        dropdown.setOutsideTouchable(true);
        dropdown.update();

        // TextView event Listener
        PopupCommentOverflowBinding popupBinding = PopupCommentOverflowBinding.bind(view);
        if(viewerId.equals(doc.getString("user_id"))) {
            log.i("remove document");
            popupBinding.tvPopup1.setVisibility(View.VISIBLE);
            popupBinding.tvPopup1.setOnClickListener(v -> {
                log.i("remove listener");
                commentListener.deleteComment(doc.getId());
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

        // PopupMenu
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

     */


    private int[] measurePopupContentSize(ListAdapter adapter) {
        ViewGroup mMeasureParent = null;
        View itemView = null;
        int maxWidth = 0;
        int maxHeight = 0;
        int itemType = 0;
        int totalHeight = 0;
        final int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        final int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        final int num = adapter.getCount();

        for(int i = 0; i < num; i++) {
            final int positionType = adapter.getItemViewType(i);
            if(positionType != itemType) {
                itemType = positionType;
                itemView = null;
            }

            if(mMeasureParent == null) mMeasureParent = new FrameLayout(context);

            itemView = adapter.getView(i, itemView, mMeasureParent);
            itemView.measure(widthMeasureSpec, heightMeasureSpec);
            final int itemWidth = itemView.getMeasuredWidth();
            final int itemHeight = itemView.getMeasuredHeight();
            if(itemWidth > maxWidth) maxWidth = itemWidth;
            if(itemHeight > maxHeight) maxHeight = itemHeight;

            totalHeight += itemHeight;
        }
        log.i("total Height: %s", totalHeight);
        return new int[] {maxWidth, maxHeight * adapter.getCount()};
    }


}
