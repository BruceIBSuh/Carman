package com.silverback.carman.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.silverback.carman.R;
import com.silverback.carman.databinding.MapStationCommentBinding;
import com.silverback.carman.databinding.MapStationDetailBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.threads.StationGasRunnable;

import java.util.List;
import java.util.Objects;

public class StationMapAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(StationMapAdapter.class);

    // Define Types
    private final int TYPE_HEADER = 0;
    private final int TYPE_ITEM = 1;
    private final int TYPE_FOOTER = 2;

    //private final DocumentReference docRef;
    private final List<DocumentSnapshot> snapshotList;
    private final StationGasRunnable.Item stationDetail;


    /*
    public StationMapAdapter(String stnId, List<DocumentSnapshot> snapshotList) {
        this.snapshotList = snapshotList;
        docRef = FirebaseFirestore.getInstance().collection("gas_station").document(stnId);
    }

     */
    public StationMapAdapter(StationGasRunnable.Item item, List<DocumentSnapshot> snapshots) {
        this.snapshotList = snapshots;
        this.stationDetail = item;
    }

    // ViewHolder
    public static class CommentViewHolder extends RecyclerView.ViewHolder {
        private final MapStationCommentBinding binding;
        public CommentViewHolder(View itemView) {
            super(itemView);
            binding =MapStationCommentBinding.bind(itemView);
        }

        ImageView getUserImageView() { return binding.imgUserpic; }
        TextView getUserNameView() { return binding.tvNickname; }
        TextView getTimeView() { return binding.tvCommentTimestamp; }
        TextView getCommentView() { return binding.tvComments; }
        RatingBar getCommentRatingView() { return binding.rbCommentsRating; }
    }

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final MapStationDetailBinding binding;
        public HeaderViewHolder(View headerView){
            super(headerView);
            binding = MapStationDetailBinding.bind(headerView);
        }

        TextView getStationNameView() { return binding.tvStnName; }
        TextView getStationAddrsView() { return binding.tvStnAddrs; }
        TextView getCarWashView() { return binding.tvWash; }
        TextView getCvsView() { return binding.tvCvs; }
        TextView getServiceView() { return binding.tvService; }
        ImageView getStationImageView() { return binding.imgStation; }
    }


    public static class FooterViewHolder extends RecyclerView.ViewHolder {
        public FooterViewHolder(View footerView){
            super(footerView);
        }
    }




    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Objects
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        switch(viewType) {
            case TYPE_HEADER:
                View headerView = inflater.inflate(R.layout.map_station_detail, parent, false);
                return new HeaderViewHolder(headerView);
            case TYPE_ITEM:
            default:
                View commentView = inflater.inflate(R.layout.map_station_comment, parent, false);
                return new CommentViewHolder(commentView);
        }

        /*
        RecyclerView.ViewHolder holder;

        if(viewType == TYPE_HEADER) {
            //if(viewType == 0) {
            log.i("Station Info");
            stnBinding = InclMapStninfoBinding.inflate(inflater, parent, false);
            holder = new HeaderViewHolder(stnBinding.getRoot());

        } else {
            commentBinding = CardviewCommentsBinding.inflate(inflater, parent, false);
            holder = new ItemViewHolder(commentBinding.getRoot());
        }

        return holder;
        */
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        switch(position) {
            case TYPE_HEADER:
                HeaderViewHolder header = (HeaderViewHolder)holder;
                header.getStationNameView().setText(stationDetail.getStnName());
                header.getStationAddrsView().setText(stationDetail.getAddrsNew());
                header.getCarWashView().setText(String.valueOf(stationDetail.isCarWash));
                header.getCvsView().setText(String.valueOf(stationDetail.isCVS));
                header.getServiceView().setText(String.valueOf(stationDetail.isService));
                break;
            case TYPE_FOOTER:
                break;
            default:
                CommentViewHolder comment = (CommentViewHolder)holder;
                DocumentSnapshot doc = snapshotList.get(position - 1);
                comment.getUserNameView().setText(doc.getString("name"));
                comment.getTimeView().setText(Objects.requireNonNull(doc.getTimestamp("timestamp")).toDate().toString());
                comment.getCommentView().setText(doc.getString("comments"));
                Long rating = doc.getLong("rating");
                if(rating != null) comment.getCommentRatingView().setRating((float)rating);

                break;
        }
        /*
        if(position == 0) dispStationInfo();
        else bindCommentToView(snapshotList.get(position));

        final String userId = snapshotList.get(position).getId();
        firestore.collection("users").document(userId).get().addOnSuccessListener(snapshot -> {
            if(snapshot != null && snapshot.exists()) {
                String strUserPic = snapshot.getString("user_pic");
                if (!TextUtils.isEmpty(strUserPic)) holder.bindImage(Uri.parse(strUserPic));
            }
        }).addOnFailureListener(e -> {});

        holder.bindToComments(snapshotList.get(position));

        if(holder instanceof HeaderViewHolder) dispStationInfo();
        //else if(holder instanceof FooterViewHolder) dispStationInfo();
        else bindCommentToView(snapshotList.get(position - 1));

         */
    }

    @Override
    public void onBindViewHolder(
            @NonNull RecyclerView.ViewHolder holder, int position, @NonNull List<Object> payloads) {
        if(payloads.isEmpty()) super.onBindViewHolder(holder, position, payloads);

        /*
        if(holder instanceof HeaderViewHolder) {
            dispStationInfo();
        } else if(holder instanceof ItemViewHolder) {
            //super.onBindViewHolder(holder, position, payloads);
            if (payloads.isEmpty()) {
                super.onBindViewHolder(holder, position, payloads);
            } else {
                for (Object obj : payloads) {
                    //drawable = (RoundedBitmapDrawable)obj;
                    //holder.imgProfile.setImageDrawable((RoundedBitmapDrawable)obj);
                }
            }
        }

         */
    }

    @Override
    public int getItemCount() {
        return snapshotList.size() + 1;
    }

    @Override
    public int getItemViewType(int position) {
        if(position == 0) return TYPE_HEADER;
        else if(position == snapshotList.size() + 1) return TYPE_FOOTER;
        else return TYPE_ITEM;
    }

    // ViewHolder class
    /*
    class CommentListHolder extends RecyclerView.ViewHolder {
        TextView tvNickname, tvComments, tvTimestamp;
        ImageView imgProfile;
        RatingBar ratingBar;

        CommentListHolder(CardView cardView) {
            super(cardView);
            imgProfile = cardView.findViewById(R.id.img_userpic);
            tvNickname = cardView.findViewById(R.id.tv_nickname);
            tvComments = cardView.findViewById(R.id.tv_comments);
            tvTimestamp = cardView.findViewById(R.id.tv_comment_timestamp);
            ratingBar = cardView.findViewById(R.id.rb_comments_rating);
            log.i("viewholder");

        }

        @SuppressWarnings("ConstantConditions")
        void bindToComments(DocumentSnapshot snapshot) {
            log.i("document: %s", snapshot);
            tvNickname.setText(snapshot.getString("name"));
            tvComments.setText(snapshot.getString("comments"));
            tvTimestamp.setText(snapshot.getTimestamp("timestamp").toDate().toString());

            float rating = (float)snapshot.getLong("rating");
            ratingBar.setRating(rating);
        }

        // Required to make caching in Glide!!
        void bindImage(Uri uri) {
            // Apply Glide with options.
            RequestOptions myOptions = new RequestOptions().fitCenter().override(50, 50).circleCrop();
            Glide.with(context)
                    .asBitmap()
                    .load(uri)
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .apply(myOptions)
                    .into(imgProfile);

        }
    }

     */
    /*
    @SuppressWarnings("ConstantConditions")
    private void bindCommentToView(DocumentSnapshot doc) {
        log.i("bind comment");
        commentBinding.tvNickname.setText(doc.getString("name"));
        commentBinding.tvComments.setText(doc.getString("comments"));
        commentBinding.tvCommentTimestamp.setText(doc.getTimestamp("timestamp").toDate().toString());
        float rating = (float)doc.getLong("rating");
        commentBinding.rbCommentsRating.setRating(rating);

        final String userId = snapshotList.get(position).getId();
        firestore.collection("users").document(userId).get().addOnSuccessListener(snapshot -> {
            if(snapshot != null && snapshot.exists()) {
                String strUserPic = snapshot.getString("user_pic");
                if (!TextUtils.isEmpty(strUserPic)) {
                    Uri imgUri = Uri.parse(strUserPic);
                    RequestOptions options = new RequestOptions().fitCenter().override(50, 50).circleCrop();
                    Glide.with(context)
                            .asBitmap()
                            .load(imgUri)
                            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                            .apply(options)
                            .into(commentBinding.imgUserpic);
                }
            }
        }).addOnFailureListener(e -> {});
    }

     */

    //@SuppressWarnings("ConstantConditions")
    /*
    private void dispStationInfo() throws NullPointerException {
        docRef.get().addOnCompleteListener(task -> {
            if(task.isSuccessful()) {
                DocumentSnapshot doc = task.getResult();
                stnBinding.tvStnName.setText(doc.get("stn_name", String.class));
                stnBinding.tvStnAddrs.setText(doc.get("new_addrs", String.class));
                boolean isWash = (boolean)doc.get("carwash");
                boolean isCvs = (boolean)doc.get("cvs");
                boolean isSvc = (boolean)doc.get("service");
                String wash = (isWash)?context.getString(R.string.map_value_ok):context.getString(R.string.map_value_not_ok);
                String cvs = (isCvs)?context.getString(R.string.map_value_yes):context.getString(R.string.map_value_no);
                String svc = (isSvc)?context.getString(R.string.map_value_yes):context.getString(R.string.map_value_no);
                stnBinding.tvWash.setText(wash);
                stnBinding.tvService.setText(cvs);
                stnBinding.tvCvs.setText(svc);
            }
        });
    }

     */
}
