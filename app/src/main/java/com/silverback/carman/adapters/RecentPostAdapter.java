package com.silverback.carman.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.silverback.carman.R;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;

import java.util.ArrayList;
import java.util.List;

public class RecentPostAdapter extends RecyclerView.Adapter<RecentPostAdapter.ViewHolder> {
    //private static LoggingHelper log = LoggingHelperFactory.create(RecentPostAdapter.class);
    QuerySnapshot querySnapshot;
    List<String> titleList;

    // Custom ViewHolder
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTitle;
        public ViewHolder(View view) {
            super(view);
            tvTitle = view.findViewById(R.id.tv_post_title);
        }

        public TextView getTextView() {
            return tvTitle;
        }
    }

    // Constructor
    public RecentPostAdapter(QuerySnapshot querySnapshot) {
        this.querySnapshot = querySnapshot;
        titleList = new ArrayList<>();
        for(QueryDocumentSnapshot doc : querySnapshot) {
            titleList.add(doc.getString("post_title"));
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ViewGroup postView = (LinearLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.itemview_main_recent_post, parent, false);

        return new ViewHolder(postView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
        //log.i("position:%d, %s", position, titleList.get(position));
        holder.getTextView().setText(titleList.get(position));
    }


    @Override
    public int getItemCount() {
        return titleList.size();
    }
}