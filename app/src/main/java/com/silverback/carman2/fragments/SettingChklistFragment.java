package com.silverback.carman2.fragments;


import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.silverback.carman2.R;
import com.silverback.carman2.adapters.SettingChklistAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

/**
 * A simple {@link Fragment} subclass.
 */
public class SettingChklistFragment extends Fragment {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(SettingChklistFragment.class);

    // Objects
    private MenuItem menuItem;
    private RecyclerView.Adapter chklistAdapter;
    private RecyclerView.LayoutManager layoutManager;

    public SettingChklistFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);


        String[] arrServiceItems = getResources().getStringArray(R.array.service_item_list);
        for(String item : arrServiceItems) {
            log.i("Service Item: %s", item);
        }

        layoutManager = new LinearLayoutManager(getActivity());
        chklistAdapter = new SettingChklistAdapter(arrServiceItems);


    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        log.i("SettingChklistFragment");

        View localView = inflater.inflate(R.layout.fragment_setting_chklist, container, false);
        RecyclerView recyclerView = localView.findViewById(R.id.recycler_chklist);

        //recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(chklistAdapter);

        // Inflate the layout for this fragment
        return localView;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_options_setting, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

}
