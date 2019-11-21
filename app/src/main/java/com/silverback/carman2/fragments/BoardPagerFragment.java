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
import com.silverback.carman2.threads.ThreadManager;

import java.text.SimpleDateFormat;
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

            case 0:
                Query firstQuery = firestore.collection("board_general")
                        .orderBy("timestamp", Query.Direction.DESCENDING)
                        .limit(25);

                firstQuery.get().addOnSuccessListener(querySnapshot -> {
                    recyclerAdapter = new BoardRecyclerAdapter(querySnapshot, this);
                    log.i("querysnapshot: %s", querySnapshot);
                    recyclerView.setAdapter(recyclerAdapter);
                    //DocumentSnapshot lastDoc = querySnapshot.getDocuments().get(querySnapshot.size() - 1);

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

            case 1:
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

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onItemClicked(String postId) {
        // Initiate the task to query the board collection and the user collection.
        // Show the dialog with the full screen. The container is android.R.id.content.
        BoardPostDialogFragment postDialogFragment = new BoardPostDialogFragment();
        getFragmentManager().beginTransaction()
                .add(android.R.id.content, postDialogFragment)
                .addToBackStack(null)
                .commit();


        /*
        log.i("post id: %s", postId);
        BoardPostDialogFragment postDialogFragment = new BoardPostDialogFragment();
        FragmentManager fragmentManager = getFragmentManager();
        Bundle args = new Bundle();

        firestore.collection("board_general").document(postId).get().addOnSuccessListener(snapshot -> {
            args.putString("title", snapshot.getString("title"));
            args.putString("body", snapshot.getString("body"));
            args.putString("postDate", sdf.format(snapshot.getDate("timestamp")));
            args.putString("userId", snapshot.getString("userid"));
            postDialogFragment.setArguments(args);

            firestore.collection("users").document(snapshot.getString("userid")).get().addOnCompleteListener(task -> {
                if(task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    log.i("user name: %s", document.getString("user_name"));

                }
            });


            // The device is smaller, so show the fragment fullscreen
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            // For a little polish, specify a transition animation
            //transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            // To make it fullscreen, use the 'content' root view as the container
            // for the fragment, which is always the root view for the activity
            transaction.add(android.R.id.content, postDialogFragment)
                            .addToBackStack(null).commit();

        }).addOnFailureListener(e -> {});
         */
    }
}
