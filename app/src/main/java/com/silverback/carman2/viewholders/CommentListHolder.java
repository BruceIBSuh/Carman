package com.silverback.carman2.viewholders;

import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.silverback.carman2.R;

public class CommentListHolder extends RecyclerView.ViewHolder {

    // UIs
    private TextView tvNickname, tvComments, tvTimestamp;

    public CommentListHolder(CardView cardView) {
        super(cardView);
        tvNickname = cardView.findViewById(R.id.tv_nickname);
        tvComments = cardView.findViewById(R.id.tv_comments);
        tvTimestamp = cardView.findViewById(R.id.tv_timestamp);
    }

    @SuppressWarnings("ConstantConditions")
    public void bindToComments(DocumentSnapshot snapshot) {
        tvNickname.setText(snapshot.getString("name"));
        tvComments.setText(snapshot.getString("comments"));
        tvTimestamp.setText(snapshot.getTimestamp("timestamp").toDate().toString());
    }
}
