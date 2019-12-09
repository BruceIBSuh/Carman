package com.silverback.carman2.fragments;


import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.silverback.carman2.R;
import com.silverback.carman2.adapters.BoardRecyclerAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.FirestoreViewModel;
import com.silverback.carman2.models.FragmentSharedModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A simple {@link Fragment} subclass.
 */
public class BoardPagerFragment extends Fragment implements
        BoardRecyclerAdapter.OnRecyclerItemClickListener {

    private static final LoggingHelper log = LoggingHelperFactory.create(BoardPagerFragment.class);

    // Objects
    private FirebaseFirestore firestore;
    private FirestoreViewModel fireModel;
    private FragmentSharedModel fragmentModel;
    private BoardRecyclerAdapter recyclerAdapter;
    private SimpleDateFormat sdf;
    private int page;

    public BoardPagerFragment() {
        // Required empty public constructor
    }

    public static BoardPagerFragment newInstance(int page) {
        BoardPagerFragment fragment = new BoardPagerFragment();
        Bundle args = new Bundle();
        args.putInt("fragment", page);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getActivity() == null) return;

        firestore = FirebaseFirestore.getInstance();
        //fireModel = ViewModelProviders.of(getActivity()).get(FirestoreViewModel.class);
        fragmentModel = ViewModelProviders.of(getActivity()).get(FragmentSharedModel.class);
        if(getArguments() != null) page = getArguments().getInt("fragment");

        sdf = new SimpleDateFormat("MM.dd HH:mm", Locale.getDefault());


    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View localView = inflater.inflate(R.layout.fragment_billboard, container, false);
        RecyclerView recyclerView = localView.findViewById(R.id.recycler_billboard);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        switch(page) {

            case 0: // Recent post
                // Pagination should be programmed.
                Query firstQuery = firestore.collection("board_general")
                        .orderBy("timestamp", Query.Direction.DESCENDING)
                        .limit(10);

                firstQuery.get().addOnSuccessListener(querySnapshot -> {
                    recyclerAdapter = new BoardRecyclerAdapter(querySnapshot, this);
                    recyclerView.setAdapter(recyclerAdapter);
                    //DocumentSnapshot lastDoc = querySnapshot.getDocuments().get(querySnapshot.size() - 1);

                    // Seems not working to update a new posting in the list.
                    fragmentModel.getNewPosting().observe(getActivity(), postId -> {
                        log.i("new positing: %s", postId);
                        firestore.collection("board_general").document(postId).get()
                                .addOnSuccessListener(snapshot -> {
                                    log.i("Update");
                                    querySnapshot.getDocuments().add(0, snapshot);
                                    for(DocumentSnapshot doc : querySnapshot) {
                                        log.i("document: %s", doc.getString("post_title"));
                                    }
                                    recyclerAdapter.notifyItemInserted(0);
                                });

                    });
                });

                break;

            case 1: // Popular post
                break;

            case 2:
                break;

            case 3:
                break;

            default:
                break;
        }

        return localView;
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
    }

    // Callback invoked by BoardRecyclerAdapter.OnRecyclerItemClickListener when an item is clicked.
    @SuppressWarnings("ConstantConditions")
    @Override
    public void onPostItemClicked(DocumentSnapshot snapshot) {
        log.i("Post item clicked");
        // Initiate the task to query the board collection and the user collection.
        // Show the dialog with the full screen. The container is android.R.id.content.
        BoardReadDlgFragment postDialogFragment = new BoardReadDlgFragment();

        Bundle bundle = new Bundle();
        bundle.putString("postTitle", snapshot.getString("post_title"));
        bundle.putString("userName", snapshot.getString("user_name"));
        bundle.putString("userPic", snapshot.getString("user_pic"));
        bundle.putString("postContent", snapshot.getString("post_content"));
        bundle.putStringArrayList("imageUriList", (ArrayList<String>)snapshot.get("post_images"));
        bundle.putString("timestamp", sdf.format(snapshot.getDate("timestamp")));
        bundle.putString("userId", snapshot.getString("user_id"));

        postDialogFragment.setArguments(bundle);

        getFragmentManager().beginTransaction()
                .add(android.R.id.content, postDialogFragment)
                .addToBackStack(null)
                .commit();


        // Auto information is retrived from Firestore based upon the user id and put it to Bundle,
        // then call the dialog.
        /*
        firestore.collection("users").document(snapshot.getString("user_id")).get()
                .addOnSuccessListener(document -> {
                    bundle.putString("autoData", document.getString("auto_data"));
                    postDialogFragment.setArguments(bundle);

                    getFragmentManager().beginTransaction()
                            .add(android.R.id.content, postDialogFragment)
                            .addToBackStack(null)
                            .commit();

                });

         */
    }
}
