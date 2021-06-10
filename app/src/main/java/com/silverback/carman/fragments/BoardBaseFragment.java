package com.silverback.carman.fragments;

import android.net.Uri;
import android.view.View;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import java.util.List;

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
