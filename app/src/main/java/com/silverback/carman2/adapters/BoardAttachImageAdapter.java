package com.silverback.carman2.adapters;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import java.util.List;

public class BoardAttachImageAdapter extends RecyclerView.Adapter<BoardAttachImageAdapter.ImageViewHolder> {

    private static final LoggingHelper log = LoggingHelperFactory.create(BoardAttachImageAdapter.class);

    // Objects
    private Context context;
    private List<Uri> uriImageList;
    private OnBoardWriteListener mListener;

    // Interface to communicate w/ BoardWriteFragment
    public interface OnBoardWriteListener {
        void removeGridImage(int position);
    }

    // Constructor
    public BoardAttachImageAdapter(List<Uri> uriList, OnBoardWriteListener listener) {
        uriImageList = uriList;
        mListener = listener;
    }


    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        ConstraintLayout layout = (ConstraintLayout)LayoutInflater.from(context)
                .inflate(R.layout.gridview_board_images, parent, false);

        return new ImageViewHolder(layout);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {

        log.i("Image Uri in Adapter: %s", uriImageList.get(position));
        Uri uri = uriImageList.get(position);
        holder.bindImageToHolder(uri);

        // Invoke the callback method when clicking the image button in order to remove the clicked
        // image out of the list and notify the adapter of the position for invalidating.
        holder.btnRemoveImage.setOnClickListener(view -> mListener.removeGridImage(position));
    }

    // Adapter should not assume that the payload will always be passed to onBindViewHolder(),
    // e.g. when the view is not attached, the payload will be simply dropped,as is the case here.
    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position, @NonNull List<Object> payloads) {

        if(payloads.isEmpty()) {
            log.i("zero payload: %s", payloads);
            super.onBindViewHolder(holder,position, payloads);
        }

        String caption = "image_" + (position + 1);
    }

    @Override
    public int getItemCount() {
        return uriImageList.size();
    }



    class ImageViewHolder extends RecyclerView.ViewHolder {

        ImageView thumbnail;
        Button btnRemoveImage;

        ImageViewHolder(View view) {
            super(view);
            thumbnail = view.findViewById(R.id.img_thumbnail);
            btnRemoveImage = view.findViewById(R.id.btn_del_image);
        }

        void bindImageToHolder(Uri uri) {
            Glide.with(context).load(uri).into(thumbnail);
        }

    }
}
