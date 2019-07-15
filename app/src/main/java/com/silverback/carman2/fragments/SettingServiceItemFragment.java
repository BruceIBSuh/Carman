package com.silverback.carman2.fragments;


import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.silverback.carman2.R;
import com.silverback.carman2.adapters.SettingServiceItemAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.FragmentSharedModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class SettingServiceItemFragment extends Fragment {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(SettingServiceItemFragment.class);

    // Objects
    private SettingServiceItemAdapter mAdapter;
    private SettingServiceDlgFragment dlgFragment;
    private FragmentSharedModel sharedModel;
    private List<String> svcItemList;


    // Fields
    private boolean bEditMode = false;

    public SettingServiceItemFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Indicate the fragment has the option menu, invoking onCreateOptionsMenu()
        setHasOptionsMenu(true);

        // List.add() does not work if List is create by Arrays.asList().
        String[] arrServiceItems = getResources().getStringArray(R.array.service_item_list);
        svcItemList = new ArrayList<>();
        for(String item : arrServiceItems) {
            svcItemList.add(item);
        }
        //svcItemList = Arrays.asList(getResources().getStringArray(R.array.service_item_list));
        mAdapter = new SettingServiceItemAdapter(svcItemList);

        dlgFragment = new SettingServiceDlgFragment();

        sharedModel = ViewModelProviders.of(getActivity()).get(FragmentSharedModel.class);
        sharedModel.getServiceItem().observe(this, data -> {
            String itemName = data.get(0);
            String itemKm = data.get(1);
            String itemMonth = data.get(2);

            svcItemList.add(itemName);
            mAdapter.notifyDataSetChanged();

        });
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View localView = inflater.inflate(R.layout.fragment_setting_chklist, container, false);
        RecyclerView recyclerView = localView.findViewById(R.id.recycler_chklist);

        //recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(mAdapter);

        // Inflate the layout for this fragment
        return localView;
    }

    // Create the Toolbar option menu when the fragment is instantiated.
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_options_setting, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch(item.getItemId()) {
            case R.id.menu_add:
                if(getFragmentManager() != null)
                    dlgFragment.show(getFragmentManager(), null);

                return true;

            case R.id.menu_edit:
                bEditMode = !bEditMode;
                mAdapter.setEditMode(bEditMode);
                mAdapter.notifyDataSetChanged();
                return true;
        }

        return false;
    }

}
