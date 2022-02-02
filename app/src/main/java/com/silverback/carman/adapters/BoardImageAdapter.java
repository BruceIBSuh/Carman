package com.silverback.carman.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.silverback.carman.R;
import com.silverback.carman.databinding.GridviewBoardImagesBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;

import java.util.List;

public class BoardImageAdapter extends RecyclerView.Adapter<BoardImageAdapter.ViewHolder> {

    private static final LoggingHelper log = LoggingHelperFactory.create(BoardImageAdapter.class);

    private final List<Uri> uriImageList;
    private final OnBoardAttachImageListener mListener;
    private final Context context;

    // Interface to communicate w/ BoardWriteFragment
    public interface OnBoardAttachImageListener {
        void removeImage(int position);
        //void attachImage(Bitmap bmp, int pos);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final Button btnDelImage;
        private final ImageView thumbnail;
        //public ViewHolder(View view) {
        public ViewHolder(GridviewBoardImagesBinding binding){
            super(binding.getRoot());
            //GridviewBoardImagesBinding binding = GridviewBoardImagesBinding.inflate(LayoutInflater.from(view.getContext()));
            //this.binding = binding;
            btnDelImage = binding.btnDelImage;
            thumbnail = binding.imgThumbnail;
        }

        public ImageView getThumbnail() {
            return thumbnail;
        }
        public Button getDelButton() {
            return btnDelImage;
        }
    }
    // Constructor
    public BoardImageAdapter(Context context, List<Uri> uriList, OnBoardAttachImageListener listener){
        this.context = context;
        uriImageList = uriList;
        mListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //View itemView = LayoutInflater.from(context).inflate(R.layout.gridview_board_images, parent, false);
        GridviewBoardImagesBinding binding = GridviewBoardImagesBinding.inflate(LayoutInflater.from(context), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
        Uri uri = uriImageList.get(position);
        bindImageToHolder(holder, uri);
        // Invoke the callback method when clicking the image button in order to remove the clicked
        // image out of the list and notify the adapter of the position for invalidating.
        //log.i("image position: %s", position);
        holder.getDelButton().setOnClickListener(view -> mListener.removeImage(position));
    }

    // Adapter should not assume that the payload will always be passed to onBindViewHolder(),
    // e.g. when the view is not attached, the payload will be simply dropped,as is the case here.
    /*
    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder, final int position, @NonNull List<Object> payloads) {
        if(payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads);
        } else {
            String caption = "image_" + (position + 1);

        }
    }

     */

    @Override
    public int getItemCount() {
        return uriImageList.size();
    }

    private void bindImageToHolder(ViewHolder holder, Uri uri) {
        Glide.with(context).asBitmap().load(uri).override(100).into(new CustomTarget<Bitmap>(){
            @Override
            public void onResourceReady(@NonNull Bitmap resource,
                                        @Nullable Transition<? super Bitmap> transition) {
                holder.getThumbnail().setImageBitmap(resource);
            }

            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {
                log.i("onLoadCleared");
            }
        });
    }

}
