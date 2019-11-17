package com.silverback.carman2.fragments;


import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

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
import com.silverback.carman2.models.FirestoreViewModel;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * A simple {@link Fragment} subclass.
 */
public class BoardPostDialogFragment extends DialogFragment {

    private static final LoggingHelper log = LoggingHelperFactory.create(BoardPostDialogFragment.class);


    // Objects
    private FirebaseFirestore firestore;
    private FirestoreViewModel firestoreModel;
    private SimpleDateFormat sdf;
    private String userId;

    public BoardPostDialogFragment() {
        // Required empty public constructor
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firestore = FirebaseFirestore.getInstance();
        firestoreModel = ViewModelProviders.of(getActivity()).get(FirestoreViewModel.class);
        sdf = new SimpleDateFormat("MM.dd HH:mm", Locale.getDefault());
    }


    @SuppressWarnings("ConstantConditions")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View localView = inflater.inflate(R.layout.dialog_board_post, container, false);
        ImageButton btn = localView.findViewById(R.id.imgbtn_dismiss);
        TextView tvTitle = localView.findViewById(R.id.tv_post_title);
        TextView tvUserName = localView.findViewById(R.id.tv_username);
        TextView tvAutoInfo = localView.findViewById(R.id.tv_autoinfo);
        TextView tvDate = localView.findViewById(R.id.tv_posting_date);
        TextView tvContent = localView.findViewById(R.id.tv_posting_body);
        btn.setOnClickListener(view -> dismiss());

        // Get the post result queried from the board_general collection in Firestore
        firestoreModel.getPostSnapshot().observe(getViewLifecycleOwner(), snapshot -> {
            log.i("FirestoreViewModel snapshot: %s", snapshot.getString("title"));
            tvTitle.setText(snapshot.getString("title"));
            tvContent.setText(snapshot.getString("body"));
            tvDate.setText(sdf.format(snapshot.getDate("timestamp")));
        });

        // Get the user result queried from the "users" collection in Firestore.
        firestoreModel.getUserSnapshot().observe(getViewLifecycleOwner(), snapshot -> {
            tvUserName.setText(snapshot.getString("user_name"));
            tvAutoInfo.setText(snapshot.getString("auto_maker"));
        });

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
