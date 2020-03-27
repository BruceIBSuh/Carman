package com.silverback.carman2.fragments;

import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BoardBaseFragment extends Fragment {


    // Constructor
    public BoardBaseFragment(){}


    // Display the view of contents and images in ConstraintLayout which is dynamically created
    // using ConstraintSet. Images are managed by Glide.
    // The regular expression makes text and images split with the markup which was made when images
    // were inserted. While looping the content, split parts of text and image are conntected to
    // ConstraintSets which are applied to the parent ConstraintLayout.
    // The recyclerview which displays comments at the bottom should be coordinated according to
    // whether the content has images or not.
    private void createEditView(ConstraintLayout parent, View view, String content, List<Uri> images) {
        // When an image is attached as the post writes, the line separator is supposed to put in at
        // before and after the image. That's why the regex contains the line separator in order to
        // get the right end position.


    }
}
