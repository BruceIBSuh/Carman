package com.silverback.carman.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.silverback.carman.R;
import com.silverback.carman.databinding.SettingPrefUserimgBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.Constants;

public class UserImagePreference extends Preference {
    private static final LoggingHelper log = LoggingHelperFactory.create(UserImagePreference.class);

    private ImageView userImageView;
    private ProgressBar progbar;
    private Uri uri;
    // Constructors
    public UserImagePreference(Context context) {
        super(context);
    }
    public UserImagePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        getAttributes(context, attrs);
    }
    public UserImagePreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        getAttributes(context, attrs);
    }


    protected void getAttributes(Context context, AttributeSet attrs) {
        //setLayoutResource(R.layout.setting_pref_userimg);
        //setIconSpaceReserved(true);
        SettingPrefUserimgBinding binding = SettingPrefUserimgBinding.inflate(LayoutInflater.from(context));
        progbar = binding.progbarUserImg;
        userImageView = binding.imgviewUserpic;

        TypedArray ta = context.getTheme().obtainStyledAttributes(
                attrs, R.styleable.UserImagePreference, 0, 0);
        try {

        } finally { ta.recycle();}
    }


    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder viewholder) {
        super.onBindViewHolder(viewholder);
        progbar = (ProgressBar)viewholder.itemView.findViewById(R.id.progbar_userImg);
        userImageView = (ImageView)viewholder.itemView.findViewById(R.id.imgview_userpic);
        progbar.setVisibility(View.GONE);

        final float scale = getContext().getResources().getDisplayMetrics().density;
        final int size = (int)(Constants.ICON_SIZE_PREFERENCE * scale + 0.5f);
        Glide.with(getContext()).load(uri).override(size)
                .placeholder(R.drawable.ic_user_blank_white)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .fitCenter()
                .circleCrop()
                .into(userImageView);
    }

    public void setUserIcon(Uri uri) {
        this.uri = uri;
        notifyChanged();
    }

    public void setProgressBarVisibility(int visible) {
        progbar.setVisibility(visible);
    }



}
