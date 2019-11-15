package com.silverback.carman2.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class BoardRecyclerAdapter extends RecyclerView.Adapter<BoardRecyclerAdapter.BoardItemHolder> {
//public class BoardRecyclerAdapter extends RecyclerView.Adapter<BoardRecyclerAdapter.BaseViewHolder> {
    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(BoardRecyclerAdapter.class);

    // Objects
    private FirebaseFirestore firestore;
    private QuerySnapshot querySnapshot;
    private CardView cardView;
    private OnRecyclerItemClickListener mListener;

    // Interface for RecyclerView item click event
    public interface OnRecyclerItemClickListener {
        void onItemClicked(String postId);
    }

    // Constructor
    public BoardRecyclerAdapter(QuerySnapshot querySnapshot, OnRecyclerItemClickListener listener) {
        super();
        this.querySnapshot = querySnapshot;
        mListener = listener;
    }


    @NonNull
    @Override
    public BoardItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        cardView = (CardView)LayoutInflater.from(parent.getContext())
                .inflate(R.layout.cardview_board_title, parent, false);


        return new BoardItemHolder(cardView);
    }

    @Override
    public void onBindViewHolder(@NonNull BoardItemHolder holder, int position) {
        DocumentSnapshot document = querySnapshot.getDocuments().get(position);
        log.i("Snapshot title: %s", document.getString("title"));
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM.dd HH:mm", Locale.getDefault());

        holder.tvPostTitle.setText(document.getString("title"));
        holder.tvNumber.setText(String.valueOf(position + 1));
        holder.tvUserName.setText(document.getString("username"));
        holder.tvPostingDate.setText(dateFormat.format(document.getDate("timestamp")));

        // Set the listener for clicking the item with position
        holder.itemView.setOnClickListener(view -> {
            if(mListener != null) mListener.onItemClicked(document.getId());
        });
    }

    @Override
    public int getItemCount() {
        return querySnapshot.size();
    }

    @Override
    public int getItemViewType(int position) {
        return -1;
    }


    abstract class BaseViewHolder extends RecyclerView.ViewHolder {

        private int currentPos;

        public BaseViewHolder(View view) {
            super(view);
        }

        abstract void clear();
        void onBind(int position) {
            currentPos = position;
            clear();
        }

        int getCurrentPos() {
            return currentPos;
        }
    }

    class ItemViewHolder extends BaseViewHolder {
        TextView tvPostTitle;
        ItemViewHolder(CardView view) {
            super(view);
            tvPostTitle = view.findViewById(R.id.tv_post_title);
        }

        @Override
        void clear() {}
    }

    class ProgressHolder extends BaseViewHolder {
        ProgressHolder(View view) {
            super(view);
        }

        @Override
        void clear(){}
    }

    class BoardItemHolder extends RecyclerView.ViewHolder {

        TextView tvPostTitle;
        TextView tvUserName;
        TextView tvNumber;
        TextView tvPostingDate;

        BoardItemHolder(CardView cardview){
            super(cardview);
            tvNumber = cardview.findViewById(R.id.tv_number);
            tvPostTitle = cardview.findViewById(R.id.tv_post_title);
            tvPostingDate = cardview.findViewById(R.id.tv_posting_date);
            tvUserName = cardview.findViewById(R.id.tv_post_owner);

        }


    }

}
