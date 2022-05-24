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
import com.silverback.carman.utils.Constants;
import com.silverback.carman.utils.PopupDropdownUtil;
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
    private final AsyncListDiffer<DocumentSnapshot> mDiffer;
    private final List<DocumentSnapshot> commentList;
    //private final PopupDropdownUtil popupDropdownUtil;
    private final RecyclerDividerUtil divider;
    private final SimpleDateFormat sdf;
    private final InputMethodManager imm;

    private final Context context;
    //private final Context styleWrapper; // for the PopupMenu
    //private final ArrayAdapter arrayCommentAdapter;
    private BoardReplyAdapter replyAdapter;
    //private ListPopupWindow popupWindow;
    private final String viewerId;
    private int checkedPos;

    public interface CommentAdapterListener {
        void deleteComment(DocumentSnapshot snapshot);
        void notifyNoReply();
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
        //popupDropdownUtil = PopupDropdownUtil.getInstance();
        divider = new RecyclerDividerUtil(Constants.DIVIDER_HEIGHT_POSTINGBOARD,
                0, ContextCompat.getColor(context, R.color.recyclerDivider));

        sdf = new SimpleDateFormat("MM.dd HH:mm", Locale.getDefault());
        imm = ((InputMethodManager)context.getSystemService(INPUT_METHOD_SERVICE));
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
        BoardItemviewCommentBinding commentBinding = BoardItemviewCommentBinding.inflate(
                LayoutInflater.from(context), parent, false);
        commentBinding.etCommentReply.setOnFocusChangeListener((v, isFocused) -> {
            // Check if the name exists, the method of which is defined in BoardReadFragment.
            if(isFocused) commentListener.OnReplyContentFocused(v);
        });

        commentBinding.switchReply.setOnCheckedChangeListener((v, isChecked) -> {
            int visible = (isChecked) ? View.VISIBLE : View.GONE;
            commentBinding.linearReply.setVisibility(visible);
        });


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
        int cntReply = Objects.requireNonNull(doc.getLong("cnt_reply")).intValue();
        holder.getReplyCountView().setText(String.valueOf(cntReply));

        // Set the posting content
        holder.getCommentContentView().setText(doc.getString("comment"));

        holder.getOverflowView().setOnClickListener(v -> showCommentPopupWindow(holder, doc));
        holder.getSendReplyView().setOnClickListener(v -> uploadReplyToComment(holder, doc));
        holder.getLoadReplyButton().setOnClickListener(v -> loadNextReplies(doc));
        holder.getReplySwitchView().setOnCheckedChangeListener((v, isChecked) -> showCommentReply(holder, doc, isChecked));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int pos, @NonNull List<Object> payloads){
        if(payloads.isEmpty()) super.onBindViewHolder(holder, pos, payloads);
        else {
            if(payloads.get(0) instanceof Boolean) {
                //if(!holder.commentBinding.switchReply.isChecked()) return;
                //holder.commentBinding.linearReply.setVisibility(View.GONE);
                holder.commentBinding.switchReply.setChecked(false);
            } else if(payloads.get(0) instanceof Integer) {
                holder.getReplyCountView().setText(String.valueOf(payloads.get(0)));
            }
        }
    }

    @Override
    public int getItemCount() {
        return mDiffer.getCurrentList().size();
    }

    // Implement BoardReplyAdapter.ReplyCallback to update the number of replies.
    @Override
    public void OnReplyRemoved(ViewHolder holder) {
        int cntReply = Integer.parseInt(holder.getReplyCountView().getText().toString());
        cntReply--;
        holder.getReplyCountView().setText(String.valueOf(cntReply));
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
            if(cntReply > 0) replyAdapter.queryCommentReply(doc.getReference(), "timestamp");

            int visible = (cntReply > PAGING_REPLY) ? View.VISIBLE : View.GONE;
            holder.commentBinding.btnLoadReplies.setVisibility(visible);

            // One switch on, the other swich should be off.
            if(checkedPos != holder.getBindingAdapterPosition()){
                notifyItemChanged(checkedPos, false);
                commentListener.OnReplySwitchChecked(checkedPos, holder.getBindingAdapterPosition());
                checkedPos = holder.getBindingAdapterPosition();
            }

        } else {
            holder.commentBinding.linearReply.setVisibility(View.GONE);
            holder.commentBinding.btnLoadReplies.setVisibility(View.GONE);
            if(imm.isActive()) imm.hideSoftInputFromWindow(holder.commentBinding.getRoot().getWindowToken(), 0);
        }

    }

    private void loadNextReplies(DocumentSnapshot doc) {
        final int cntReply = Objects.requireNonNull(doc.getLong("cnt_reply")).intValue();
        if(cntReply > replyAdapter.getItemCount()) replyAdapter.queryNextReply();
        else commentListener.notifyNoReply();
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
            replyAdapter.queryCommentReply(doc.getReference(), "timestamp");
            commentRef.update("cnt_reply", FieldValue.increment(1)).addOnSuccessListener(Void -> {
                int cntReply = Integer.parseInt(holder.getReplyCountView().getText().toString());
                cntReply++;
                holder.getReplyCountView().setText(String.valueOf(cntReply));
            });

            holder.commentBinding.etCommentReply.clearFocus();
            holder.commentBinding.etCommentReply.getText().clear();
            imm.hideSoftInputFromWindow(holder.commentBinding.getRoot().getWindowToken(), 0);
        });
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

    // Turn off the reply switch, hiding the reply list, which is invoked when the comment switch is off
    // and defined in BoardReadFeedAdapter.
    public void switchCommentReplyOff(){
        notifyItemChanged(checkedPos, true);
    }
}
