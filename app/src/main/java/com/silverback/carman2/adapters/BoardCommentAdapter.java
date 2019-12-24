package com.silverback.carman2.adapters;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import java.util.List;

public class BoardCommentAdapter extends RecyclerView.Adapter<BoardCommentAdapter.CommentViewHolder> {

    private static final LoggingHelper log = LoggingHelperFactory.create(BoardCommentAdapter.class);

    // Objects
    private List<DocumentSnapshot> snapshotList;
    private FirebaseFirestore firestore;

    // Constructor
    public BoardCommentAdapter(List<DocumentSnapshot> snapshotList) {

        this.snapshotList = snapshotList;
        log.i("Comments: %s", snapshotList.size());
    }
    @NonNull
    @Override
    public BoardCommentAdapter.CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        CardView cardview = (CardView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.cardview_board_comment, parent, false);

        return new CommentViewHolder(cardview);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        DocumentSnapshot document = snapshotList.get(position);
        //holder.tvCommentUser.setText(document.getString("user"));
        holder.tvTimestamp.setText(String.valueOf(document.getDate("timestamp")));
        holder.tvCommentContent.setText(document.getString("comment"));

        // Retrieve the user name from the "users" collection.
        String userId = document.getString("user");
        if(userId != null && !userId.isEmpty()) {
            FirebaseFirestore.getInstance().collection("users").document(userId).get()
                    .addOnCompleteListener(task -> {
                        if(task.isSuccessful()) {
                            DocumentSnapshot doc = task.getResult();
                            holder.tvCommentUser.setText(doc.getString("user_name"));
                        }
                    });
        }

    }


    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position, List<Object> payloads) {
        if(payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads);
        } else {
            DocumentSnapshot snapshot = (DocumentSnapshot)payloads.get(0);
            log.i("Partial Binding: %s", snapshot.getString("user"));
        }
    }

    @Override
    public int getItemCount() {
        return snapshotList.size();
    }


    class CommentViewHolder extends RecyclerView.ViewHolder {

        TextView tvCommentUser, tvTimestamp;
        TextView tvCommentContent;

        CommentViewHolder(CardView cardview) {
            super(cardview);
            tvCommentUser = cardview.findViewById(R.id.tv_comment_user);
            tvTimestamp = cardview.findViewById(R.id.tv_comment_timestamp);
            tvCommentContent = cardview.findViewById(R.id.tv_comment_content);
        }
    }
}
