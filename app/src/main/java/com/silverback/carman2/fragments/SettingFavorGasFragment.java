package com.silverback.carman2.fragments;


import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.silverback.carman2.R;
import com.silverback.carman2.SettingPreferenceActivity;
import com.silverback.carman2.adapters.SettingFavoriteAdapter;
import com.silverback.carman2.database.CarmanDatabase;
import com.silverback.carman2.database.FavoriteProviderEntity;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.FragmentSharedModel;
import com.silverback.carman2.utils.Constants;
import com.silverback.carman2.utils.ItemTouchHelperCallback;

import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class SettingFavorGasFragment extends Fragment implements
        SettingFavoriteAdapter.OnFavoriteAdapterListener{

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(SettingFavorGasFragment.class);

    // Objects
    private CarmanDatabase mDB;
    private SettingFavoriteAdapter mAdapter;

    // Constructor
    public SettingFavorGasFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (mDB == null) mDB = CarmanDatabase.getDatabaseInstance(getContext());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View localView = inflater.inflate(R.layout.fragment_setting_favorite, container, false);
        RecyclerView recyclerView = localView.findViewById(R.id.recycler_favorite);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);

        mDB.favoriteModel().queryFavoriteProvider(Constants.GAS).observe(this, favoriteList -> {

            log.i("FavoriteList: %s", favoriteList.size());

            mAdapter = new SettingFavoriteAdapter(favoriteList, this);
            ItemTouchHelperCallback callback = new ItemTouchHelperCallback(getContext(), mAdapter);
            ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
            itemTouchHelper.attachToRecyclerView(recyclerView);

            recyclerView.setAdapter(mAdapter);
        });

        // Inflate the layout for this fragment
        return localView;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {

        if(menuItem.getItemId() == android.R.id.home) {

            List<FavoriteProviderEntity> favoriteList = mAdapter.getFavoriteList();
            int position = 0;

            // Update the placeholder in FavoriteProviderEntity accroding to the position of
            // the edited fasvorte list.
            for(FavoriteProviderEntity entity : favoriteList) {
                entity.placeHolder = position;
                position++;
            }

            mDB.favoriteModel().updatePlaceHolder(favoriteList);
            startActivity(new Intent(getActivity(), SettingPreferenceActivity.class));

            return true;
        }

        return false;
    }

    @Override
    public void addFavorite(FavoriteProviderEntity entity) {
        log.i("Listener: Add Favorite - %s", entity.providerName);
        mDB.favoriteModel().insertFavoriteProvider(entity);
    }

    @Override
    public void deleteFavorite(FavoriteProviderEntity entity) {
        log.i("Listener: delete Favorite - %s", entity.providerName);
        mDB.favoriteModel().deleteProvider(entity);
    }
}
