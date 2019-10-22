package com.silverback.carman2.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.LoadImageViewModel;
import com.silverback.carman2.threads.DownloadImageTask;
import com.silverback.carman2.threads.ThreadManager;
import com.silverback.carman2.viewholders.CommentListHolder;
import com.silverback.carman2.viewholders.StationListHolder;
import com.squareup.picasso.Picasso;

import java.util.List;

public class CommentRecyclerAdapter extends RecyclerView.Adapter<CommentListHolder> {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(CommentRecyclerAdapter.class);

    // Objects
    private DownloadImageTask downloadTask;
    private Context context;
    private FirebaseStorage storage;
    private LoadImageViewModel viewModel;
    private StorageReference storageRef;
    private List<DocumentSnapshot> snapshotList;
    private RoundedBitmapDrawable drawable;

    // UIs
    private TextView tvComment;
    private ImageView imgProfile;

    // Constructor
    public CommentRecyclerAdapter(
            Context context, List<DocumentSnapshot> snapshotList, LoadImageViewModel viewModel){

        this.context = context;
        this.snapshotList = snapshotList;
        this.viewModel = viewModel;
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();
    }


    @NonNull
    @Override
    public CommentListHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        CardView cardView = (CardView)LayoutInflater.from(parent.getContext())
                .inflate(R.layout.cardview_comments, parent, false);

        imgProfile = cardView.findViewById(R.id.img_profile);

        return new CommentListHolder(cardView);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentListHolder holder, int position) {

        holder.bindToComments(snapshotList.get(position), drawable);

        final String userid = snapshotList.get(position).getId();
        storage.getReference().child("images/" + userid + "/profile.jpg").getDownloadUrl()
                .addOnSuccessListener(uri -> {
                    log.i("Image Uri: %s", uri);
                    ThreadManager.downloadImageTask(context, position, uri.toString(), viewModel);
                    //Picasso.with(context).load(uri).into(imgProfile);
                }).addOnFailureListener(e -> log.e("Download failed"));



    }

    @Override
    public void onBindViewHolder(@NonNull CommentListHolder holder, int position, @NonNull List<Object> payloads) {

        if(payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads);

        }else{
            for(Object obj : payloads) {
                log.i("Partial Binding");
                drawable = (RoundedBitmapDrawable)obj;
                holder.imgProfile.setImageDrawable((RoundedBitmapDrawable)obj);
            }
        }
    }

    @Override
    public int getItemCount() {
        log.i("snapshotlist: %s", snapshotList.size());
        return snapshotList.size();
    }

    public void setProfileImage(int position, RoundedBitmapDrawable drawable) {

    }
}
