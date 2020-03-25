package com.silverback.carman2.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.utils.ApplyImageResourceUtil;
import com.silverback.carman2.utils.Constants;
import com.silverback.carman2.viewmodels.ImageViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


/**
 * This RecyclerView.Adapter is to display posting items of
 */
public class BoardPostingAdapter extends RecyclerView.Adapter<BoardPostingAdapter.BoardItemHolder> {

    private static final LoggingHelper log = LoggingHelperFactory.create(BoardPostingAdapter.class);

    // Constants

    // Objects
    private Context context;
    private List<DocumentSnapshot> snapshotList;
    private SparseArray<String> sparseThumbArray;
    private OnRecyclerItemClickListener mListener;
    private SimpleDateFormat sdf;
    private ApplyImageResourceUtil imgUtil;
    private ImageViewModel imgModel;

    // Fields
    private int type;

    // Interface to notify BoardPagerFragment of pressing a recyclerview item.
    public interface OnRecyclerItemClickListener {
        void onPostItemClicked(DocumentSnapshot snapshot, int position);
    }

    // Constructor
    public BoardPostingAdapter(List<DocumentSnapshot> snapshots, OnRecyclerItemClickListener listener) {
        super();

        log.i("Posting adapter initiated");
        mListener = listener;
        snapshotList = snapshots;
        sdf = new SimpleDateFormat("MM.dd HH:mm", Locale.getDefault());
        sparseThumbArray = new SparseArray<>();

    }

    @NonNull
    @Override
    public BoardItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        imgUtil = new ApplyImageResourceUtil(context);


        CardView cardView = (CardView)LayoutInflater.from(context)
                .inflate(R.layout.cardview_board_post, parent, false);

        return new BoardItemHolder(cardView);
    }

    @SuppressWarnings({"unchecked", "ConstantConditions"})
    @Override
    public void onBindViewHolder(@NonNull BoardItemHolder holder, final int position) {

        // Retreive an board item queried in and passed from BoardPagerFragment
        //DocumentSnapshot document = querySnapshot.getDocuments().get(position);
        //DocumentSnapshot document = snapshotList.get(position);
        final DocumentSnapshot snapshot = snapshotList.get(position);
        log.i("Snapshot: %s", snapshot.getString("post_title"));

        holder.tvPostTitle.setText(snapshot.getString("post_title"));
        holder.tvNumber.setText(String.valueOf(position + 1));
        holder.tvPostingDate.setText(sdf.format(snapshot.getDate("timestamp")));
        holder.tvUserName.setText(snapshot.getString("user_name"));
        holder.tvViewCount.setText(String.valueOf(snapshot.getLong("cnt_view")));
        holder.tvCommentCount.setText(String.valueOf(snapshot.getLong("cnt_comment")));


        if(!TextUtils.isEmpty(snapshot.getString("user_pic"))) {
            holder.bindProfileImage(Uri.parse(snapshot.getString("user_pic")));
        } else holder.bindProfileImage(null);

        if(snapshot.get("post_images") != null) {
            String thumb = ((ArrayList<String>)snapshot.get("post_images")).get(0);
            holder.bindAttachedImage(Uri.parse(thumb));

        } else {
            Glide.with(context).clear(holder.imgAttached);
            holder.imgAttached.setImageDrawable(null);
        }

        // Set the listener for clicking the item with position
        holder.itemView.setOnClickListener(view -> {
            log.i("position: %s, %s", position, holder.getAdapterPosition());
            if(mListener != null) mListener.onPostItemClicked(snapshot, position);
        });

    }


    @Override
    public void onBindViewHolder(
            @NonNull BoardItemHolder holder, int position, @NonNull List<Object> payloads) {

        if(payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads);
        } else {
            for(Object obj : payloads) log.i("payloads: %s", obj);
            holder.tvViewCount.setText(String.valueOf(payloads.get(0)));
            holder.tvCommentCount.setText(String.valueOf(payloads.get(1)));
        }
    }


    @Override
    public int getItemCount() {
        //return snapshotList == null ? 0 : snapshotList.size();
        return snapshotList.size();
    }

    // Guess this will be useful to apply plug-in ads.
    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    // ViewHolders
    class BoardItemHolder extends RecyclerView.ViewHolder {

        TextView tvPostTitle, tvUserName, tvNumber, tvViewCount, tvCommentCount, tvPostingDate;
        ImageView imgProfile;
        ImageView imgAttached;

        BoardItemHolder(CardView cardview){
            super(cardview);
            tvNumber = cardview.findViewById(R.id.tv_number);
            tvPostTitle = cardview.findViewById(R.id.tv_post_title);
            tvPostingDate = cardview.findViewById(R.id.tv_posting_date);
            tvUserName = cardview.findViewById(R.id.tv_post_owner);
            tvViewCount = cardview.findViewById(R.id.tv_count_views);
            tvCommentCount = cardview.findViewById(R.id.tv_count_comment);
            imgProfile = cardview.findViewById(R.id.img_user);
            imgAttached = cardview.findViewById(R.id.img_attached);

        }

        // Null check of the uri shouldn't be needed b/c Glide handles it on its own.
        void bindProfileImage(Uri uri) {
            RequestOptions myOptions = new RequestOptions()
                    .fitCenter()
                    .override(Constants.ICON_SIZE_POSTING_LIST, Constants.ICON_SIZE_POSTING_LIST)
                    .circleCrop();

            if(uri == null) {
                Glide.with(context).load(R.drawable.ic_user_blank_white)
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        .apply(myOptions)
                        .into(imgProfile);
            } else {
                Glide.with(context).asBitmap()
                        //.placeholder(new ColorDrawable(Color.BLUE))
                        .load(uri)
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        .apply(myOptions)
                        .into(imgProfile);
            }
        }

        void bindAttachedImage(Uri uri) {
            int x = imgAttached.getWidth();
            int y = imgAttached.getHeight();
            imgUtil.applyGlideToThumbnail(uri, x, y, imgAttached, false);
        }

    }


}
