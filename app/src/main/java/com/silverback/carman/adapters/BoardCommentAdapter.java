package com.silverback.carman.adapters;

import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.silverback.carman.R;
import com.silverback.carman.utils.ApplyImageResourceUtil;
import com.silverback.carman.utils.Constants;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BoardCommentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    //private static final LoggingHelper log = LoggingHelperFactory.create(BoardCommentAdapter.class);

    // Objects
    private ApplyImageResourceUtil imgUtil;
    private List<DocumentSnapshot> snapshotList;
    private FirebaseFirestore firestore;
    private SimpleDateFormat sdf;

    // Constructor
    public BoardCommentAdapter(List<DocumentSnapshot> snapshotList) {
        this.snapshotList = snapshotList;
        firestore = FirebaseFirestore.getInstance();
        sdf = new SimpleDateFormat("MM.dd HH:mm", Locale.getDefault());
    }
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        CardView cardview = (CardView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.cardview_board_comment, parent, false);

        imgUtil = new ApplyImageResourceUtil(parent.getContext());
        return new CommentViewHolder(cardview);
    }


    @SuppressWarnings("ConstantConditions")
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        DocumentSnapshot document = snapshotList.get(position);
        //holder.tvCommentUser.setText(document.getString("user"));
        if(document.getDate("timestamp") != null) {
            Date timestamp = document.getDate("timestamp");
            ((CommentViewHolder)holder).tvTimestamp.setText(sdf.format(timestamp));
        }

        ((CommentViewHolder)holder).tvCommentContent.setText(document.getString("comment"));

        // Retrieve the user name from the "users" collection.
        String userId = document.getString("userId");
        //if(userId != null && !userId.isEmpty()) {
        if(!TextUtils.isEmpty(userId)) {
            firestore.collection("users").document(userId).get().addOnCompleteListener(task -> {
                if(task.isSuccessful()) {
                    DocumentSnapshot doc = task.getResult();
                    ((CommentViewHolder)holder).tvCommentUser.setText(doc.getString("user_name"));
                    // Check if the user_pic field exists. If so, attach the user image. Otherwise,
                    // attach the default image.
                    if(!TextUtils.isEmpty(doc.getString("user_pic")))
                        ((CommentViewHolder)holder).bindUserImage(Uri.parse(doc.getString("user_pic")));
                    else ((CommentViewHolder)holder).bindUserImage(Uri.parse(Constants.imgPath + "ic_user_blank_gray"));
                }
            });
        }
    }


    @Override
    public void onBindViewHolder(
            @NonNull RecyclerView.ViewHolder holder, int position, @NonNull List<Object> payloads) {
        if(payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads);
        } else {
            DocumentSnapshot snapshot = (DocumentSnapshot)payloads.get(0);
            //log.i("Partial Binding: %s", snapshot.getString("user"));
        }

    }

    @Override
    public int getItemCount() {
        return snapshotList.size();
    }


    class CommentViewHolder extends RecyclerView.ViewHolder {
        ImageView imgUserPic;
        TextView tvCommentUser, tvTimestamp;
        TextView tvCommentContent;

        CommentViewHolder(CardView cardview) {
            super(cardview);
            imgUserPic = cardview.findViewById(R.id.img_comment_user);
            tvCommentUser = cardview.findViewById(R.id.tv_comment_user);
            tvTimestamp = cardview.findViewById(R.id.tv_comment_timestamp);
            tvCommentContent = cardview.findViewById(R.id.tv_comment_content);
        }

        void bindUserImage(Uri uri) {
            int x = imgUserPic.getWidth();
            int y = imgUserPic.getHeight();
            imgUtil.applyGlideToImageView(uri, imgUserPic, x, y, true);
        }
    }
}
