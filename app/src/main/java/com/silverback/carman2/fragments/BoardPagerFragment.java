package com.silverback.carman2.fragments;


import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
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

import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class BoardPagerFragment extends Fragment {

    private static final LoggingHelper log = LoggingHelperFactory.create(BoardPagerFragment.class);

    // Objects
    private FirebaseFirestore firestore;
    private BoardRecyclerAdapter recyclerAdapter;
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
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View localView = inflater.inflate(R.layout.fragment_billboard, container, false);
        RecyclerView recyclerView = localView.findViewById(R.id.recycler_billboard);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));


        // First tab page which is the recent posts
        if(page == 0) {
            Query firstQuery = firestore.collection("board_general")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(25);

            firstQuery.get()
                    .addOnSuccessListener(querySnapshot -> {
                        recyclerAdapter = new BoardRecyclerAdapter(querySnapshot);
                        recyclerView.setAdapter(recyclerAdapter);
                        DocumentSnapshot lastDoc = querySnapshot.getDocuments().get(querySnapshot.size() - 1);
                    });


        } else if(page == 1) {
            //recyclerView.setAdapter(new BoardRecyclerAdapter(null));
        }



        return localView;
    }

}
