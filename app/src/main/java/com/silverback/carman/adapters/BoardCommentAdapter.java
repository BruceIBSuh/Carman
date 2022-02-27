package com.silverback.carman.adapters;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.silverback.carman.R;
import com.silverback.carman.databinding.ItemviewBoardCommentBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.ApplyImageResourceUtil;
import com.silverback.carman.utils.Constants;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BoardCommentAdapter extends RecyclerView.Adapter<BoardCommentAdapter.ViewHolder> {

    private static final LoggingHelper log = LoggingHelperFactory.create(BoardCommentAdapter.class);

    private final FirebaseFirestore firestore;
    private final List<DocumentSnapshot> snapshotList;
    private final SimpleDateFormat sdf;
    private final ApplyImageResourceUtil imageUtil;
    private final Context context;

    // Constructor
    public BoardCommentAdapter(Context context, List<DocumentSnapshot> snapshotList) {
        this.context = context;
        this.snapshotList = snapshotList;

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

        TextView getCommentContentView() {
            return commentBinding.tvCommentContent;
        }
        TextView getCommentTimestampView() {
            return commentBinding.tvCommentTimestamp;
        }
        TextView getCommentUserView() {
            return commentBinding.tvCommentUser;
        }
        ImageView getUserImageView() {
            return commentBinding.imgCommentUser;
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
        DocumentSnapshot document = snapshotList.get(position);
        holder.getCommentContentView().setText(document.getString("comment"));

        final Date date = document.getDate("timestamp");
        if(date != null) holder.getCommentTimestampView().setText(sdf.format(date));

        final String userId = document.getString("userId");
        if(userId != null && !TextUtils.isEmpty(userId)) {
            firestore.collection("users").document(userId).get().addOnCompleteListener(task -> {
                if(task.isSuccessful()) {
                    DocumentSnapshot doc = task.getResult();
                    log.i("user name: %s", doc.getString("user_name"));
                    holder.getCommentUserView().setText(doc.getString("user_name"));
                    if(!TextUtils.isEmpty(doc.getString("user_pic")))
                        setUserImage(holder, doc.getString("user_pic"));
                    else setUserImage(holder, Constants.imgPath + "ic_user_blank_gray");
                }
            });
        }
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
        return snapshotList.size();
    }

    private void setUserImage(ViewHolder holder, String imgPath) {
        int x = holder.getUserImageView().getWidth();
        int y = holder.getUserImageView().getHeight();
        imageUtil.applyGlideToImageView(Uri.parse(imgPath), holder.getUserImageView(), x, y, true);
    }
}
