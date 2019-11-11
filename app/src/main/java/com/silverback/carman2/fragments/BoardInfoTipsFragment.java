package com.silverback.carman2.fragments;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.silverback.carman2.R;
import com.silverback.carman2.adapters.BillboardRecyclerAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

/**
 * A simple {@link Fragment} subclass.
 */
public class BoardInfoTipsFragment extends Fragment {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(BoardInfoTipsFragment.class);

    // Objects
    private BillboardRecyclerAdapter mAdapter;
    private RecyclerView recyclerView;

    public BoardInfoTipsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View localView = inflater.inflate(R.layout.fragment_billboard, container, false);
        recyclerView = localView.findViewById(R.id.recycler_billboard);

        mAdapter = new BillboardRecyclerAdapter();
        recyclerView.setAdapter(mAdapter);

        return localView;
    }

}
