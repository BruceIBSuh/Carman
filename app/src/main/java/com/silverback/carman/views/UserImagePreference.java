package com.silverback.carman.views;

import static com.silverback.carman.SettingActivity.PREF_USER_IMAGE;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.silverback.carman.CropImageActivity;
import com.silverback.carman.R;
import com.silverback.carman.databinding.SettingPrefUserimgBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.ApplyImageResourceUtil;

public class UserImagePreference extends Preference {
    private static final LoggingHelper log = LoggingHelperFactory.create(UserImagePreference.class);

    private SettingPrefUserimgBinding binding;
    private ImageView userImage;
    private ProgressBar progbar;
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
        setLayoutResource(R.layout.setting_pref_userimg);
        //setIconSpaceReserved(true);
        binding = SettingPrefUserimgBinding.inflate(LayoutInflater.from(context));
        TypedArray ta = context.getTheme().obtainStyledAttributes(
                attrs, R.styleable.UserImagePreference, 0, 0);
        try {
            boolean isLoader = ta.getBoolean(R.styleable.UserImagePreference_showLoader, false);
            log.i("isLoader: %s", isLoader);
        } finally { ta.recycle();}
    }


    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder viewholder) {
        super.onBindViewHolder(viewholder);
        progbar = (ProgressBar) viewholder.findViewById(R.id.progbar_userImg);
        userImage = (ImageView)viewholder.findViewById(R.id.imgview_userpic);
        progbar.setVisibility(View.GONE);
    }


    public void setUserImageLoader(boolean isVisible) {
        int visibility = (isVisible)?View.VISIBLE : View.GONE;
        binding.progbarUserImg.setVisibility(visibility);
    }

    public ImageView getUserImageView() { return binding.imgviewUserpic; }



}
