package com.silverback.carman.adapters;

import static android.content.Context.INPUT_METHOD_SERVICE;
import static com.silverback.carman.BoardActivity.PAGING_REPLY;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.collect.Lists;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.silverback.carman.R;
import com.silverback.carman.databinding.BoardItemviewCommentBinding;
import com.silverback.carman.databinding.PopupCommentOverflowBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.ApplyImageResourceUtil;
import com.silverback.carman.utils.Constants;
import com.silverback.carman.utils.PopupDropdownUtil;
import com.silverback.carman.utils.QueryPostPaginationUtil;
import com.silverback.carman.utils.RecyclerDividerUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class BoardCommentAdapter extends RecyclerView.Adapter<BoardCommentAdapter.ViewHolder> implements
        BoardReplyAdapter.ReplyCallback {

    private static final LoggingHelper log = LoggingHelperFactory.create(BoardCommentAdapter.class);

    private final CommentAdapterListener commentListener;
    private final FirebaseFirestore mDB;
    private QueryPostPaginationUtil pagingUtil;
    private final AsyncListDiffer<DocumentSnapshot> mDiffer;
    private final List<DocumentSnapshot> commentList;
    private final ApplyImageResourceUtil imageUtil;
    //private final PopupDropdownUtil popupDropdownUtil;
    private final RecyclerDividerUtil divider;
    private final SimpleDateFormat sdf;

    private final Context context;
    //private final Context styleWrapper; // for the PopupMenu
    //private final ArrayAdapter arrayCommentAdapter;
    private BoardReplyAdapter replyAdapter;
    //private ListPopupWindow popupWindow;
    private final String viewerId;
    private long cntReply;
    private int checkedPos;
    private boolean isReplyUploaded;

    public interface CommentAdapterListener {
        void deleteComment(DocumentSnapshot snapshot);
        void notifyNoData();
        void OnUploadReplyDone(int position, boolean isDone);
        void OnReplySwitchChecked(int checkedPos, int bindingPos);
        void OnReplyContentFocused(View view);
    }

    // Constructor
    public BoardCommentAdapter(Context context, List<DocumentSnapshot> commentList, String viewerId,
            CommentAdapterListener listener) {
        this.context = context;
        this.commentList = commentList;
        this.viewerId = viewerId;
        this.commentListener = listener;

        mDB = FirebaseFirestore.getInstance();
        mDiffer = new AsyncListDiffer<>(this, DIFF_CALLBACK_COMMENT);

        imageUtil = new ApplyImageResourceUtil(context);
        //popupDropdownUtil = PopupDropdownUtil.getInstance();
        divider = new RecyclerDividerUtil(Constants.DIVIDER_HEIGHT_POSTINGBOARD,
                0, ContextCompat.getColor(context, R.color.recyclerDivider));

        sdf = new SimpleDateFormat("MM.dd HH:mm", Locale.getDefault());

        // Intantiate BoardReplyAdapter
        //replyAdapter = new BoardReplyAdapter(context, viewerId, this);
        //replyAdapter = BoardReplyAdapter.getInstance();
        //replyAdapter.initReplyAdapter(viewerId);
        //replyAdapter.setReplyAdapterListener(listener);

    }

    // Update the adapter using AsyncListDiffer.ItemCallback<T>
    public void submitCommentList(List<DocumentSnapshot> snapshotList) {
        mDiffer.submitList(Lists.newArrayList(snapshotList));//, recyclerListener::onRecyclerUpdateDone);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        BoardItemviewCommentBinding commentBinding;
        public ViewHolder(BoardItemviewCommentBinding commentBinding) {
            super(commentBinding.getRoot());
            this.commentBinding = commentBinding;
        }

        TextView getUserNameView() { return commentBinding.tvCommentUser; }
        ImageView getUserImageView() { return commentBinding.imgCommentUser; }
        TextView getTimestampView() { return commentBinding.tvCommentTimestamp; }
        TextView getReplyCountView() { return commentBinding.headerReplyCnt; }
        TextView getCommentContentView() { return commentBinding.tvCommentContent; }
        ImageView getOverflowView() { return commentBinding.imgOverflow; }
        ImageView getSendReplyView() { return commentBinding.imgbtnSendReply; }
        RecyclerView getRecyclerReplyView() { return commentBinding.recyclerviewReply; }
        SwitchCompat getReplySwitchView() { return commentBinding.switchReply; }
        Button getLoadReplyButton() { return commentBinding.btnLoadReplies; }

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
        //View itemView = LayoutInflater.from(context).inflate(R.layout.itemview_board_comment, parent, false);
        BoardItemviewCommentBinding commentBinding = BoardItemviewCommentBinding.inflate(
                LayoutInflater.from(context), parent, false);
        commentBinding.etCommentReply.setOnFocusChangeListener((v, isFocused) -> {
            if(isFocused) commentListener.OnReplyContentFocused(v);
        });

        //return new ViewHolder(itemView);
        return new ViewHolder(commentBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DocumentSnapshot doc = mDiffer.getCurrentList().get(position);
        String commentId = doc.getString("user_id");
        setCommentUserProfile(commentId, holder.getUserNameView(), holder.getUserImageView());

        final Date date = doc.getDate("timestamp");
        if(date != null) holder.getTimestampView().setText(sdf.format(date));

        // Try-catch should be temporarily put here.
        try {
            long cntReply = Objects.requireNonNull(doc.getLong("cnt_reply"));
            holder.getReplyCountView().setText(String.valueOf(cntReply));
        } catch (NullPointerException e) {e.printStackTrace();}

        // Set the posting content
        holder.getCommentContentView().setText(doc.getString("comment"));

        holder.getOverflowView().setOnClickListener(v -> showCommentPopupWindow(holder, doc));
        holder.getSendReplyView().setOnClickListener(v -> uploadReplyToComment(holder, doc));
        holder.getLoadReplyButton().setOnClickListener(v -> loadNextReplies(doc));
        holder.getReplySwitchView().setOnCheckedChangeListener((v, isChecked) -> {
            showCommentReply(holder, doc, isChecked);
        });
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int pos, @NonNull List<Object> payloads){
        if(payloads.isEmpty()) super.onBindViewHolder(holder, pos, payloads);
        else {
            if(!holder.commentBinding.switchReply.isChecked()) return;
            if((boolean)payloads.get(0)) {
                holder.commentBinding.linearReply.setVisibility(View.GONE);
                holder.commentBinding.switchReply.setChecked(false);
            }
        }
    }

    @Override
    public int getItemCount() {
        return mDiffer.getCurrentList().size();
    }

    @Override
    public void OnDeleteReply(ViewHolder holder) {
        int count = Integer.parseInt(holder.getReplyCountView().getText().toString());
        count -= 1;
        holder.getReplyCountView().setText(String.valueOf(count));
    }

    private void setCommentUserProfile(String userId, TextView textView, ImageView imageView) {
        mDB.collection("users").document(userId).get().addOnSuccessListener(user -> {
            if(user.get("user_names") != null) {
                List<?> names = (List<?>)user.get("user_names");
                assert names != null;
                textView.setText((String)names.get(names.size() - 1));
            }

            Uri userImage = null;
            if(user.getString("user_pic") != null) userImage = Uri.parse(user.getString("user_pic"));
            Glide.with(context).load(userImage)
                    .placeholder(R.drawable.ic_user_blank_white)
                    .fitCenter().circleCrop().into(imageView);
        });
    }

    // Implement the reply switch button method.
    private void showCommentReply(ViewHolder holder, DocumentSnapshot doc, boolean isChecked) {
        if(isChecked) {
            // Create BoardReplyAdapter and set it to RecyclerView.
            replyAdapter = BoardReplyAdapter.getInstance();
            replyAdapter.initReplyAdapter(this, holder, viewerId);
            LinearLayoutManager layout = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false);
            holder.getRecyclerReplyView().setLayoutManager(layout);
            holder.getRecyclerReplyView().addItemDecoration(divider);
            holder.getRecyclerReplyView().setHasFixedSize(false);
            holder.getRecyclerReplyView().setItemAnimator(new DefaultItemAnimator());
            holder.getRecyclerReplyView().setAdapter(replyAdapter);
            // Set the visibility of the reply loading button
            holder.commentBinding.linearReply.setVisibility(View.VISIBLE);
            final long cntReply = Objects.requireNonNull(doc.getLong("cnt_reply"));
            int visible = (cntReply > PAGING_REPLY) ? View.VISIBLE : View.GONE;
            holder.commentBinding.btnLoadReplies.setVisibility(visible);
            if(cntReply > 0) replyAdapter.queryCommentReply(doc.getReference(), "timestamp");
            // When having a different switch checked, the current switch should be closed.
            if(checkedPos != holder.getBindingAdapterPosition()){
                commentListener.OnReplySwitchChecked(checkedPos, holder.getBindingAdapterPosition());
                checkedPos = holder.getBindingAdapterPosition();
            }
        } else {
            holder.commentBinding.linearReply.setVisibility(View.GONE);
            holder.commentBinding.btnLoadReplies.setVisibility(View.GONE);
            InputMethodManager imm = ((InputMethodManager)context.getSystemService(INPUT_METHOD_SERVICE));
            if(imm.isActive()) imm.hideSoftInputFromWindow(holder.commentBinding.getRoot().getWindowToken(), 0);
        }

    }

    private void loadNextReplies(DocumentSnapshot doc) {
        final int cntReply = Objects.requireNonNull(doc.getLong("cnt_reply")).intValue();
        if(cntReply > replyAdapter.getItemCount()) replyAdapter.queryNextReply();
        else commentListener.notifyNoData();
    }

    private void uploadReplyToComment(ViewHolder holder, DocumentSnapshot doc) {
        final DocumentReference commentRef = doc.getReference();
        final String content = holder.getReplyContent();
        if(TextUtils.isEmpty(content)) return;

        Map<String, Object> object = new HashMap<>();
        object.put("user_id", viewerId);
        object.put("timestamp", FieldValue.serverTimestamp());
        object.put("reply_content", content);

        commentRef.collection("replies").add(object).addOnSuccessListener(aVoid -> {
            commentRef.update("cnt_reply", FieldValue.increment(1)).addOnSuccessListener(Void -> {
                int count = Objects.requireNonNull(doc.getLong("cnt_reply")).intValue();
                count += 1;
                holder.getReplyCountView().setText(String.valueOf(count));
            });

            replyAdapter.queryCommentReply(doc.getReference(), "timestamp");
            holder.commentBinding.etCommentReply.clearFocus();
            holder.commentBinding.etCommentReply.getText().clear();
        });

        //commentListener.addCommentReply(replyAdapter, commentshot, content, position);

    }

    private void showCommentPopupWindow(ViewHolder holder, DocumentSnapshot doc) {
        ViewGroup rootView = holder.commentBinding.getRoot();
        LayoutInflater inflater = LayoutInflater.from(rootView.getContext());
        View view = inflater.inflate(R.layout.popup_comment_overflow, rootView, false);

        PopupDropdownUtil popupDropdownUtil = PopupDropdownUtil.getInstance();
        popupDropdownUtil.setInitParams(view, holder.getOverflowView(), doc);
        PopupWindow dropdown = popupDropdownUtil.createPopupWindow();

        PopupCommentOverflowBinding binding = PopupCommentOverflowBinding.bind(view);
        if(viewerId.equals(doc.getString("user_id"))) {
            binding.tvPopup1.setVisibility(View.VISIBLE);
            binding.tvPopup1.setOnClickListener(v -> {
                doc.getReference().delete().addOnSuccessListener(aVoid -> {
                    commentList.remove(holder.getBindingAdapterPosition());
                    submitCommentList(commentList);
                    commentListener.deleteComment(doc);
                });

                dropdown.dismiss();
            });
        }

        binding.tvPopup2.setOnClickListener(v -> {
            log.i("menu2");
            dropdown.dismiss();
        });
        binding.tvPopup3.setOnClickListener(v -> {
            log.i("menu3");
            dropdown.dismiss();
        });
    }

    private static final DiffUtil.ItemCallback<DocumentSnapshot> DIFF_CALLBACK_COMMENT =
            new DiffUtil.ItemCallback<DocumentSnapshot>() {

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

    public void hideReply(){
        log.i("checked reply position: %s", checkedPos);
        notifyItemChanged(checkedPos, true);
    }

    // RecyclerView Adapter for the comment reply.
    /*
    public static class CommentReplyAdapter extends RecyclerView.Adapter<CommentReplyAdapter.ViewHolder> {
        private CommentAdapterListener callback;
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
        
        public void setReplyAdapterListener(CommentAdapterListener callback) {
            this.callback = callback;
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

        private void setReplyUserPic(ViewHolder holder, DocumentSnapshot doc) {
            final String imgurl = (!TextUtils.isEmpty(doc.getString("user_pic")))?
                    doc.getString("user_pic") : Constants.imgPath + "ic_user_blank_gray";
            int x = holder.getReplyUserImage().getWidth();
            int y = holder.getReplyUserImage().getHeight();
            imgutil.applyGlideToImageView(Uri.parse(imgurl), holder.getReplyUserImage(), x, y, true);
        }

        private void showReplyPopupWindow(View view, ViewHolder holder, DocumentSnapshot doc, int pos){
            LayoutInflater inflater = LayoutInflater.from(view.getContext());
            View contentView = inflater.inflate(
                    R.layout.popup_comment_overflow, holder.replyBinding.getRoot(), false);
            popupDropdownUtil.setInitParams(contentView, holder.getOverflowView(), doc);
            PopupWindow dropdown = popupDropdownUtil.createPopupWindow();

            PopupCommentOverflowBinding popupBinding = PopupCommentOverflowBinding.bind(contentView);
            if(viewerId.equals(doc.getString("user_id"))) {
                popupBinding.tvPopup1.setVisibility(View.VISIBLE);
                popupBinding.tvPopup1.setOnClickListener(v -> {
                    callback.deleteCommentReply(this, commentRef.getId(), doc.getId(), pos);
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
    */

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

    /*
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

     */


}
