package com.silverback.carman2.adapters;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;
import com.google.firebase.storage.FirebaseStorage;
import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class BoardRecyclerAdapter extends RecyclerView.Adapter<BoardRecyclerAdapter.BoardItemHolder> {
//public class BoardRecyclerAdapter extends RecyclerView.Adapter<BoardRecyclerAdapter.BaseViewHolder> {
    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(BoardRecyclerAdapter.class);

    // Constants


    // Objects
    private Context context;
    private FirebaseFirestore firestore;
    private FirebaseStorage storage;
    private QuerySnapshot querySnapshot;
    private CardView cardView;
    private OnRecyclerItemClickListener mListener;
    private SimpleDateFormat sdf;

    // Interface for RecyclerView item click event
    public interface OnRecyclerItemClickListener {
        void onPostItemClicked(DocumentSnapshot snapshot);
    }

    // Constructor
    public BoardRecyclerAdapter(QuerySnapshot querySnapshot, OnRecyclerItemClickListener listener) {

        super();
        //this.context = context;
        this.querySnapshot = querySnapshot;
        mListener = listener;
        sdf = new SimpleDateFormat("MM.dd HH:mm", Locale.getDefault());
        //firestore = FirebaseFirestore.getInstance();
        //storage = FirebaseStorage.getInstance();
    }


    @NonNull
    @Override
    public BoardItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        cardView = (CardView)LayoutInflater.from(parent.getContext())
                .inflate(R.layout.cardview_board_title, parent, false);


        return new BoardItemHolder(cardView);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onBindViewHolder(@NonNull BoardItemHolder holder, int position) {

        // Retreive an board item queried in and passed from BoardPagerFragment
        DocumentSnapshot document = querySnapshot.getDocuments().get(position);

        holder.tvPostTitle.setText(document.getString("post_title"));
        holder.tvNumber.setText(String.valueOf(position + 1));
        holder.tvPostingDate.setText(sdf.format(document.getDate("timestamp")));
        holder.tvUserName.setText(document.getString("user_name"));
        holder.bindProfileImage(Uri.parse(document.getString("user_pic")));

        List<String> imgList = (List<String>)document.get("post_images");
        if(imgList != null && imgList.size() > 0) {
            log.i("imagList: %s", imgList.get(0));
            holder.bindAttachedImage(Uri.parse(imgList.get(0)));
        }

        // Set the listener for clicking the item with position
        holder.itemView.setOnClickListener(view -> {
            if(mListener != null) mListener.onPostItemClicked(document);
        });

    }


    @Override
    public void onBindViewHolder(@NonNull BoardItemHolder holder, int position, @NonNull List<Object> payloads) {

        if(payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads);
        } else {
            log.i("Partial Binding of BoardPosting");
        }
    }


    @Override
    public int getItemCount() {
        return querySnapshot.size();
    }

    @Override
    public int getItemViewType(int position) {
        return -1;
    }


    // ViewHolder class
    abstract class BaseViewHolder extends RecyclerView.ViewHolder {

        private int currentPos;

        BaseViewHolder(View view) {
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
        ImageView imgProfile;
        ImageView imgAttached;

        BoardItemHolder(CardView cardview){
            super(cardview);
            tvNumber = cardview.findViewById(R.id.tv_number);
            tvPostTitle = cardview.findViewById(R.id.tv_post_title);
            tvPostingDate = cardview.findViewById(R.id.tv_posting_date);
            tvUserName = cardview.findViewById(R.id.tv_post_owner);
            imgProfile = cardview.findViewById(R.id.img_user);
            imgAttached = cardview.findViewById(R.id.img_attached);

        }

        void bindProfileImage(Uri uri) {
            RequestOptions myOptions = new RequestOptions().fitCenter().override(30, 30).circleCrop();
            Glide.with(context)
                    .asBitmap()
                    .load(uri)
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .apply(myOptions)
                    .into(imgProfile);
        }

        void bindAttachedImage(Uri uri) {
            Glide.with(context)
                    .asBitmap()
                    .load(uri)
                    .centerCrop()
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .into(imgAttached);
        }


    }

}
