package com.silverback.carman2.fragments;


import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.silverback.carman2.R;
import com.silverback.carman2.adapters.SettingFavoriteRecyclerAdapter;
import com.silverback.carman2.database.CarmanDatabase;
import com.silverback.carman2.database.FavoriteProvider;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class FavoritePagerGasFragment extends Fragment {
        //implements LoaderManager.LoaderCallbacks<Cursor> {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(FavoritePagerGasFragment.class);

    // Objects
    private CarmanDatabase mDB;
    private LiveData<List<FavoriteProvider>> liveData;
    private SettingFavoriteRecyclerAdapter adapter;

    // Constructor
    public FavoritePagerGasFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        if(getActivity() != null) {
            mDB = CarmanDatabase.getDatabaseInstance(getActivity().getApplicationContext());
            liveData = mDB.favoriteModel().loadAllFavoriteProvider();
        }

        View localView = inflater.inflate(R.layout.fragment_pager_favorite_gas, container, false);
        RecyclerView recyclerView = localView.findViewById(R.id.recycler_favorite);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);

        liveData.observe(getActivity(), observer -> {
            log.i("LiveData changed");
            List<FavoriteProvider> list = liveData.getValue();
            adapter = new SettingFavoriteRecyclerAdapter(list);
            recyclerView.setAdapter(adapter);
        });


        // Inflate the layout for this fragment
        return localView;
    }
}
