package com.silverback.carman2.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.silverback.carman2.R;

import java.util.List;

public class BoardCommentAdapter extends RecyclerView.Adapter<BoardCommentAdapter.CommentViewHolder> {


    private List<DocumentSnapshot> snapshotList;

    public BoardCommentAdapter(List<DocumentSnapshot> list) {
        snapshotList = list;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        CardView cardview = (CardView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.cardview_board_comment, null);

        return new CommentViewHolder(cardview);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {

    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position, List<Object> payloads) {
        if(payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads);
        }
    }

    @Override
    public int getItemCount() {
        return 0;
    }

    class CommentViewHolder extends RecyclerView.ViewHolder {

        public CommentViewHolder(CardView cardview) {
            super(cardview);
        }

    }
}
