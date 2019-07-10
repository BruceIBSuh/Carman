package com.silverback.carman2.fragments;


import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.silverback.carman2.R;
import com.silverback.carman2.adapters.SettingServiceItemAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

/**
 * A simple {@link Fragment} subclass.
 */
public class SettingServiceListFragment extends Fragment {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(SettingServiceListFragment.class);

    // Objects
    private String[] arrServiceItems;


    public SettingServiceListFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        arrServiceItems = getResources().getStringArray(R.array.service_item_list);

    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        log.i("SettingServiceListFragment");

        View localView = inflater.inflate(R.layout.fragment_setting_service_list, container, false);
        RecyclerView recyclerView = localView.findViewById(R.id.recycler_service_items);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);

        SettingServiceItemAdapter recyclerAdapter = new SettingServiceItemAdapter(arrServiceItems);
        recyclerView.setAdapter(recyclerAdapter);

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_setting_service_list, container, false);
    }

}
