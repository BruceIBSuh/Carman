package com.silverback.carman2.fragments;


import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.silverback.carman2.R;
import com.silverback.carman2.adapters.BoardRecyclerAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import java.text.SimpleDateFormat;
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
    private BoardRecyclerAdapter recyclerAdapter;
    private SimpleDateFormat sdf;
    private int page;

    public BoardPagerFragment() {
        // Required empty public constructor
    }

    public static BoardPagerFragment newInstance(int page) {
        BoardPagerFragment fragment = new BoardPagerFragment();
        Bundle args = new Bundle();
        args.putInt("fragmentPage", page);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firestore = FirebaseFirestore.getInstance();
        if(getArguments() != null) page = getArguments().getInt("fragmentPage");
        log.i("Fragment page: %s", page);

        sdf = new SimpleDateFormat("MM.dd HH:mm", Locale.getDefault());


    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View localView = inflater.inflate(R.layout.fragment_billboard, container, false);
        RecyclerView recyclerView = localView.findViewById(R.id.recycler_billboard);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));


        // First tab page which is the recent posts
        if(page == 0) {
            Query firstQuery = firestore.collection("board_general")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(25);

            firstQuery.get()
                    .addOnSuccessListener(querySnapshot -> {
                        recyclerAdapter = new BoardRecyclerAdapter(querySnapshot, this);
                        recyclerView.setAdapter(recyclerAdapter);
                        DocumentSnapshot lastDoc = querySnapshot.getDocuments().get(querySnapshot.size() - 1);
                    });


        } else if(page == 1) {
            //recyclerView.setAdapter(new BoardRecyclerAdapter(null));
        }



        return localView;
    }




    @Override
    public void onItemClicked(String postId) {

        log.i("post id: %s", postId);
        FragmentManager fragmentManager = getFragmentManager();
        BoardPostDialogFragment postDialogFragment = new BoardPostDialogFragment();
        Bundle args = new Bundle();

        firestore.collection("board_general").document(postId).get()
                .addOnSuccessListener(snapshot -> {
                    args.putString("title", snapshot.getString("title"));
                    args.putString("body", snapshot.getString("body"));
                    args.putString("postDate", sdf.format(snapshot.getDate("timestamp")));
                    args.putString("userId", snapshot.getString("userid"));
                    postDialogFragment.setArguments(args);

                    // The device is smaller, so show the fragment fullscreen
                    FragmentTransaction transaction = fragmentManager.beginTransaction();
                    // For a little polish, specify a transition animation
                    //transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                    // To make it fullscreen, use the 'content' root view as the container
                    // for the fragment, which is always the root view for the activity
                    transaction.add(android.R.id.content, postDialogFragment)
                            .addToBackStack(null).commit();

                }).addOnFailureListener(e -> {});

    }
}
