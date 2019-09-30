package com.silverback.carman2.fragments;


import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.silverback.carman2.R;
import com.silverback.carman2.adapters.SettingFavoriteAdapter;
import com.silverback.carman2.database.CarmanDatabase;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.utils.Constants;
import com.silverback.carman2.utils.ItemTouchHelperCallback;

/**
 * A simple {@link Fragment} subclass.
 */
public class SettingFavorSvcFragment extends Fragment {

    // Constants
    private static final LoggingHelper log = LoggingHelperFactory.create(SettingFavorSvcFragment.class);

    // Objects
    private CarmanDatabase mDB;
    private SettingFavoriteAdapter mAdapter;


    public SettingFavorSvcFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (mDB == null) mDB = CarmanDatabase.getDatabaseInstance(getContext());
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View localView = inflater.inflate(R.layout.fragment_setting_favorite, container, false);
        RecyclerView recyclerView = localView.findViewById(R.id.recycler_favorite);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);

        mDB.favoriteModel().queryFavoriteProvider(Constants.SVC).observe(this, favoriteList -> {
            for(int i = 0; i < favoriteList.size(); i++) {
                log.i("Favorite: %s, %s", favoriteList.get(i).providerName, favoriteList.get(i).address);
            }
            mAdapter = new SettingFavoriteAdapter(favoriteList);
            // Make the item drag by invoking ItemTouchHelperCallback
            mAdapter = new SettingFavoriteAdapter(favoriteList);
            ItemTouchHelperCallback callback = new ItemTouchHelperCallback(mAdapter);
            ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
            itemTouchHelper.attachToRecyclerView(recyclerView);

            recyclerView.setAdapter(mAdapter);
        });

        return localView;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if(menuItem.getItemId() == android.R.id.home) {
            getActivity().onBackPressed();
            return true;
        }

        return false;
    }
}
