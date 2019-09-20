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
import com.silverback.carman2.adapters.SettingFavoriteRecyclerAdapter;
import com.silverback.carman2.database.CarmanDatabase;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

/**
 * A simple {@link Fragment} subclass.
 */
public class FavoriteGasPagerFragment extends Fragment {
        //implements LoaderManager.LoaderCallbacks<Cursor> {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(FavoriteGasPagerFragment.class);

    // Objects
    private CarmanDatabase mDB;
    private SettingFavoriteRecyclerAdapter adapter;

    // Constructor
    public FavoriteGasPagerFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mDB == null) mDB = CarmanDatabase.getDatabaseInstance(getContext());
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View localView = inflater.inflate(R.layout.fragment_pager_favorite_gas, container, false);
        RecyclerView recyclerView = localView.findViewById(R.id.recycler_favorite);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);

        mDB.favoriteModel().loadAllFavoriteProvider().observe(this, favorite -> {
            for(int i = 0; i < favorite.size(); i++) {
                log.i("Favorite: %s, %s", favorite.get(i).providerName, favorite.get(i).address);
            }
            adapter = new SettingFavoriteRecyclerAdapter(favorite);
            recyclerView.setAdapter(adapter);
        });

        // Inflate the layout for this fragment
        return localView;
    }
}
