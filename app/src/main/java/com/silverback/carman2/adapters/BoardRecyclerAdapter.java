package com.silverback.carman2.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import java.util.List;

public class BoardRecyclerAdapter extends RecyclerView.Adapter<BoardRecyclerAdapter.BillboardHolder> {
//public class BoardRecyclerAdapter extends RecyclerView.Adapter<BoardRecyclerAdapter.BaseViewHolder> {
    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(BoardRecyclerAdapter.class);

    // Objects
    private FirebaseFirestore firestore;
    private QuerySnapshot querySnapshot;
    private CardView cardView;

    // Constructor
    public BoardRecyclerAdapter(QuerySnapshot querySnapshot) {
        super();
        this.querySnapshot = querySnapshot;
    }


    @NonNull
    @Override
    public BillboardHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        cardView = (CardView)LayoutInflater.from(parent.getContext())
                .inflate(R.layout.cardview_billbaord, parent, false);


        return new BillboardHolder(cardView);
    }

    @Override
    public void onBindViewHolder(@NonNull BillboardHolder holder, int position) {
        DocumentSnapshot document = querySnapshot.getDocuments().get(position);
        log.i("Snapshot title: %s", document.getString("title"));
        holder.tvPostTitle.setText(document.getString("title"));
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

    class BillboardHolder extends RecyclerView.ViewHolder {

        TextView tvPostTitle;

        BillboardHolder(CardView view){
            super(view);
            tvPostTitle = view.findViewById(R.id.tv_post_title);
        }
    }

}
