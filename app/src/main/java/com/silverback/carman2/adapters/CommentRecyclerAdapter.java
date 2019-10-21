package com.silverback.carman2.adapters;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.viewholders.CommentListHolder;

import java.util.List;

public class CommentRecyclerAdapter extends RecyclerView.Adapter<CommentListHolder> {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(CommentRecyclerAdapter.class);

    // Objects
    private List<DocumentSnapshot> snapshotList;
    private List<Uri> imgList;

    // UIs
    private TextView tvComment;
    private ImageView imgProfile;

    // Constructor
    public CommentRecyclerAdapter(List<DocumentSnapshot> snapshotList) {
        this.snapshotList = snapshotList;

        for(DocumentSnapshot snapshot : snapshotList) {
            log.i("Comment Adapter snapshot: %s, %s", snapshot.getString("comments"), snapshot.getString("name"));
        }


    }


    @NonNull
    @Override
    public CommentListHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        CardView cardView = (CardView)LayoutInflater.from(parent.getContext())
                .inflate(R.layout.cardview_comments, parent, false);

        //tvComment = cardView.findViewById(R.id.tv_comments);
        imgProfile = cardView.findViewById(R.id.img_profile);

        return new CommentListHolder(cardView);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentListHolder holder, int position) {
        holder.bindToComments(snapshotList.get(position), imgList.get(position));

    }

    @Override
    public int getItemCount() {
        log.i("snapshotlist: %s", snapshotList.size());
        return snapshotList.size();
    }
}
