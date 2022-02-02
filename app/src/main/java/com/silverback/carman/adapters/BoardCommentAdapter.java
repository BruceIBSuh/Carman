package com.silverback.carman.adapters;

import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.silverback.carman.R;
import com.silverback.carman.databinding.CardviewBoardCommentBinding;
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
    private final List<DocumentSnapshot> snapshotList;
    private final SimpleDateFormat sdf;

    // Constructor
    public BoardCommentAdapter(List<DocumentSnapshot> snapshotList) {
        this.snapshotList = snapshotList;
        sdf = new SimpleDateFormat("MM.dd HH:mm", Locale.getDefault());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        CardView cardview = (CardView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.cardview_board_comment, parent, false);
        ApplyImageResourceUtil imgUtil = new ApplyImageResourceUtil(parent.getContext());
        return new ViewHolder(cardview, imgUtil);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DocumentSnapshot document = snapshotList.get(position);
        holder.bindComment(document, sdf);

        String userId = document.getString("userId");
        holder.bindUserProfile(userId);
    }


    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull List<Object> payloads) {
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

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final CardviewBoardCommentBinding binding;
        private final FirebaseFirestore firestore;
        private final ApplyImageResourceUtil imgUtil;

        public ViewHolder(@NonNull View itemView, ApplyImageResourceUtil imgUtil) {
            super(itemView);
            binding = CardviewBoardCommentBinding.bind(itemView);
            firestore = FirebaseFirestore.getInstance();
            this.imgUtil = imgUtil;
        }

        public void bindComment(DocumentSnapshot document, SimpleDateFormat sdf) {
            if(document.getDate("timestamp") != null) {
                Date date = document.getDate("timestamp");
                if(date != null) binding.tvCommentTimestamp.setText(sdf.format(date));
            }
            binding.tvCommentContent.setText(document.getString("comment"));
        }

        public void bindUserProfile(String userId) {
            if(!TextUtils.isEmpty(userId)) {
                firestore.collection("users").document(userId).get().addOnCompleteListener(task -> {
                    if(task.isSuccessful()) {
                        DocumentSnapshot doc = task.getResult();
                        binding.tvCommentUser.setText(doc.getString("user_name"));

                        if(!TextUtils.isEmpty(doc.getString("user_pic"))) {
                            setUserImage(Uri.parse(doc.getString("user_pic")));
                        } else {
                            setUserImage(Uri.parse(Constants.imgPath + "ic_user_blank_gray"));
                        }
                    }
                });
            }
        }

        private void setUserImage(Uri uri) {
            int x = binding.imgCommentUser.getWidth();
            int y = binding.imgCommentUser.getHeight();
            imgUtil.applyGlideToImageView(uri, binding.imgCommentUser, x, y, true);
        }
    }
}
