package com.silverback.carman2.adapters;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import java.util.List;

public class AttachImageAdapter extends RecyclerView.Adapter<AttachImageAdapter.ImageViewHolder> {

    private static final LoggingHelper log = LoggingHelperFactory.create(AttachImageAdapter.class);

    // Objects
    private Context context;
    private List<Uri> uriImageList;

    // Constructor
    public AttachImageAdapter(List<Uri> uriList) {
        uriImageList = uriList;
    }


    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        RelativeLayout layout = (RelativeLayout)LayoutInflater.from(context)
                .inflate(R.layout.gridview_board_images, parent, false);

        return new ImageViewHolder(layout);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {

        log.i("Image Uri in Adapter: %s", uriImageList.get(position));
        Uri uri = uriImageList.get(position);
        holder.bindImageToHolder(uri);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position, @NonNull List<Object> payloads) {
        log.i("Partial Binding");
        holder.bindImageToHolder(uriImageList.get(position));
    }

    @Override
    public int getItemCount() {
        return uriImageList.size();
    }


    class ImageViewHolder extends RecyclerView.ViewHolder {

        ImageView imgView;

        ImageViewHolder(View view) {
            super(view);
            imgView = view.findViewById(R.id.img_board_grid);
        }

        void bindImageToHolder(Uri uri) {
            Glide.with(context).load(uri).into(imgView);

        }


    }
}
