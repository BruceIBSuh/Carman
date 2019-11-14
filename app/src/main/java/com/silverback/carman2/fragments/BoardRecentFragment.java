package com.silverback.carman2.fragments;


import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.silverback.carman2.R;
import com.silverback.carman2.adapters.BillboardRecyclerAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class BoardRecentFragment extends Fragment {

    private static final LoggingHelper log = LoggingHelperFactory.create(BoardRecentFragment.class);

    // Objects
    private FirebaseFirestore firestore;
    private BillboardRecyclerAdapter recyclerAdapter;
    private RecyclerView recyclerView;


    public BoardRecentFragment() {
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

        View localView = inflater.inflate(R.layout.fragment_billboard_recent, container, false);
        recyclerView = localView.findViewById(R.id.vg_recycler_recent);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        firestore.collection("board_general").orderBy("timestamp", Query.Direction.DESCENDING).get()
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful() && task.getResult() != null) {
                        List<QueryDocumentSnapshot> dataList = new ArrayList<>();
                        for(QueryDocumentSnapshot snapshot : task.getResult()){
                            log.i("title: %s", snapshot.getString("title"));
                            dataList.add(snapshot);
                        }

                        recyclerAdapter = new BillboardRecyclerAdapter(dataList);
                        recyclerView.setAdapter(recyclerAdapter);
                        recyclerAdapter.notifyDataSetChanged();
                    }
                });

        // Inflate the layout for this fragment
        return localView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }

}
