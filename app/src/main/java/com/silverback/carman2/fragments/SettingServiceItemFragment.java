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
import com.silverback.carman2.adapters.SettingServiceItemAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

/**
 * A simple {@link Fragment} subclass.
 */
public class SettingServiceItemFragment extends Fragment {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(SettingServiceItemFragment.class);

    // Objects
    private SettingServiceItemAdapter mAdapter;
    private SettingServiceDlgFragment dlgFragment;


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

        String[] arrServiceItems = getResources().getStringArray(R.array.service_item_list);
        mAdapter = new SettingServiceItemAdapter(arrServiceItems);
        dlgFragment = new SettingServiceDlgFragment();
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
