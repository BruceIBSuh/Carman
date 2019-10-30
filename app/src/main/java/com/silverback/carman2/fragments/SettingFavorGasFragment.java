package com.silverback.carman2.fragments;


import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.silverback.carman2.R;
import com.silverback.carman2.SettingPreferenceActivity;
import com.silverback.carman2.adapters.SettingFavoriteAdapter;
import com.silverback.carman2.database.CarmanDatabase;
import com.silverback.carman2.database.FavoriteProviderEntity;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.OpinetViewModel;
import com.silverback.carman2.threads.PriceFavoriteTask;
import com.silverback.carman2.threads.ThreadManager;
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
    private FirebaseFirestore firestore;
    private SettingFavoriteAdapter mAdapter;
    private SparseArray<DocumentSnapshot> snapshotList;
    private PriceFavoriteTask priceFavoriteTask;
    private OpinetViewModel priceViewModel;

    // Constructor
    public SettingFavorGasFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        firestore = FirebaseFirestore.getInstance();
        mDB = CarmanDatabase.getDatabaseInstance(getContext());
        priceViewModel = ViewModelProviders.of(this).get(OpinetViewModel.class);
        //firestoreViewModel = ViewModelProviders.of(this).get(FirestoreViewModel.class);
        snapshotList = new SparseArray<>();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View localView = inflater.inflate(R.layout.fragment_setting_favorite, container, false);
        RecyclerView recyclerView = localView.findViewById(R.id.recycler_favorite);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);

        // Query the favorite gas stations from FavoriteProviderEntity
        mDB.favoriteModel().queryFavoriteProvider(Constants.GAS).observe(this, favoriteList -> {

            mAdapter = new SettingFavoriteAdapter(favoriteList, snapshotList, this);

            ItemTouchHelperCallback callback = new ItemTouchHelperCallback(getContext(), mAdapter);
            ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
            itemTouchHelper.attachToRecyclerView(recyclerView);

            recyclerView.setAdapter(mAdapter);

            for(int i = 0; i < favoriteList.size(); i++) {
                final int pos = i;
                final String stnId = favoriteList.get(pos).providerId;

                firestore.collection("gas_eval").document(stnId).get().addOnCompleteListener(task -> {
                    if(task.isSuccessful()) {
                        DocumentSnapshot snapshot = task.getResult();
                        if(snapshot != null && snapshot.exists()) {
                            mAdapter.addSnapshotList(pos, snapshot);
                            mAdapter.notifyItemChanged(pos, snapshot);
                        }

                    }
                });
            }

        });

        // Inflate the layout for this fragment
        return localView;
    }

    @Override
    public void onPause() {
        super.onPause();
        if(priceFavoriteTask != null) priceFavoriteTask = null;
    }

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

    /*
    @Override
    public void changeFavorite(FavoriteProviderEntity entity) {
        log.i("Listener: Add Favorite - %s", entity.providerName);
        mDB.favoriteModel().insertFavoriteProvider(entity);
    }
    */

    // Callback invoked from SettingFwhen the first-set favorite station has changed in
    @Override
    public void changeFavorite(int category, String stnId) {
        if(category == Constants.GAS && !stnId.isEmpty()) {
            log.i("The favorite changed: %s", stnId);
            priceFavoriteTask = ThreadManager.startFavoritePriceTask(getContext(), priceViewModel, stnId, true);
        }
    }

    @Override
    public void deleteFavorite(FavoriteProviderEntity entity) {
        log.i("Listener: delete Favorite - %s", entity.providerName);
        mDB.favoriteModel().deleteProvider(entity);
    }
}
