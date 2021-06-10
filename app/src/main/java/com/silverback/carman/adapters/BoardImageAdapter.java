package com.silverback.carman.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.silverback.carman.R;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;

import java.util.List;

public class BoardImageAdapter extends RecyclerView.Adapter<BoardImageAdapter.ImageViewHolder> {

    private static final LoggingHelper log = LoggingHelperFactory.create(BoardImageAdapter.class);

    // Objects
    private Context context;
    private List<Uri> uriImageList;
    private OnBoardAttachImageListener mListener;

    // Interface to communicate w/ BoardWriteFragment
    public interface OnBoardAttachImageListener {
        void removeImage(int position);
        //void attachImage(Bitmap bmp, int pos);
    }

    // Constructor
    public BoardImageAdapter(List<Uri> uriList, OnBoardAttachImageListener listener) {
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

        Uri uri = uriImageList.get(position);
        holder.bindImageToHolder(uri, position);

        // Invoke the callback method when clicking the image button in order to remove the clicked
        // image out of the list and notify the adapter of the position for invalidating.
        //log.i("image position: %s", position);
        holder.btnDel.setOnClickListener(view -> mListener.removeImage(position));
    }

    // Adapter should not assume that the payload will always be passed to onBindViewHolder(),
    // e.g. when the view is not attached, the payload will be simply dropped,as is the case here.
    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position, @NonNull List<Object> payloads) {
        if(payloads.isEmpty()) {
            //log.i("zero payload: %s", payloads);
            super.onBindViewHolder(holder, position, payloads);
        } else {
            String caption = "image_" + (position + 1);
            //log.i("notifyItemRemoved: %s, %s", payloads.get(0), caption);
        }
    }

    @Override
    public int getItemCount() {
        return uriImageList.size();
    }

    class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView thumbnail;
        Button btnDel;

        ImageViewHolder(View view) {
            super(view);
            thumbnail = view.findViewById(R.id.img_thumbnail);
            btnDel = view.findViewById(R.id.btn_del_image);
        }

        void bindImageToHolder(Uri uri, int pos) {
            Glide.with(context).asBitmap().load(uri).override(100).into(new CustomTarget<Bitmap>(){
                @Override
                public void onResourceReady(@NonNull Bitmap resource,
                                            @Nullable Transition<? super Bitmap> transition) {
                    thumbnail.setImageBitmap(resource);
                    //mListener.attachImage(resource, pos);
                }

                @Override
                public void onLoadCleared(@Nullable Drawable placeholder) {
                    log.i("onLoadCleared");
                }
            });
        }

    }
}
