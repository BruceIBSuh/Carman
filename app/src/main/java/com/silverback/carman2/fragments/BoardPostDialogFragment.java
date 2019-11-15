package com.silverback.carman2.fragments;


import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * A simple {@link Fragment} subclass.
 */
public class BoardPostDialogFragment extends DialogFragment {

    private static final LoggingHelper log = LoggingHelperFactory.create(BoardPostDialogFragment.class);


    // Objects
    private FirebaseFirestore firestore;
    private SimpleDateFormat sdf;
    private String userId;

    public BoardPostDialogFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firestore = FirebaseFirestore.getInstance();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View localView = inflater.inflate(R.layout.dialog_board_post, container, false);
        ImageButton btn = localView.findViewById(R.id.imgbtn_dismiss);
        TextView tvTitle = localView.findViewById(R.id.tv_post_title);
        TextView tvUserName = localView.findViewById(R.id.tv_username);
        TextView tvDate = localView.findViewById(R.id.tv_posting_date);
        TextView tvContent = localView.findViewById(R.id.tv_posting_body);
        btn.setOnClickListener(view -> dismiss());

        if(getArguments() != null) {
            tvTitle.setText(getArguments().getString("title"));
            tvDate.setText(getArguments().getString("postDate"));
            tvContent.setText(getArguments().getString("body"));
            userId = getArguments().getString("userId");
        }

        // Query the username and auto info from the users collection with the user id
        firestore.collection("users").document(userId).get().addOnSuccessListener(snapshot -> {
            String username = snapshot.getString("user_name");
            tvUserName.setText(username);

        }).addOnFailureListener(e -> {});


        // Inflate the layout for this fragment
        return localView;
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

}
