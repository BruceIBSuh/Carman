package com.silverback.carman2.fragments;


import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.silverback.carman2.R;
import com.silverback.carman2.adapters.SettingFavoriteRecyclerAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.DataProviderContract;

/**
 * A simple {@link Fragment} subclass.
 */
public class FavoriteGasPagerFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(FavoriteGasPagerFragment.class);

    // Objects
    private Context context;

    // UIs
    private RecyclerView recyclerView;

    // Constructor
    public FavoriteGasPagerFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        this.context = getContext();
        LoaderManager loaderManager = LoaderManager.getInstance(this);
        loaderManager.initLoader(1, null, this);

        View localView = inflater.inflate(R.layout.fragment_pager_favorite_gas, container, false);

        recyclerView = localView.findViewById(R.id.recycler_favorite);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);

        // Inflate the layout for this fragment
        return localView;
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        log.i("Loader id: %s", id);
        Uri uriFavorite = DataProviderContract.FAVORITE_TABLE_URI;

        final String[] projection = {
                DataProviderContract.FAVORITE_PROVIDER_NAME,
                DataProviderContract.FAVORITE_PROVIDER_CODE,
                DataProviderContract.FAVORITE_PROVIDER_ADDRS
        };

        String selection = DataProviderContract.FAVORITE_PROVIDER_CATEGORY + " = '" + id + "';";

        return new CursorLoader(context, uriFavorite, projection, selection, null, null);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
        log.i("Loaded");
        SettingFavoriteRecyclerAdapter adapter = new SettingFavoriteRecyclerAdapter(cursor);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        log.d("Loader reset");
    }
}
