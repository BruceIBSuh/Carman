package com.silverback.carman2.fragments;


import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
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
import com.silverback.carman2.utils.Constants;
import com.silverback.carman2.utils.ItemTouchHelperCallback;

import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class SettingFavorSvcFragment extends Fragment implements
        SettingFavoriteAdapter.OnFavoriteAdapterListener{

    // Constants
    private static final LoggingHelper log = LoggingHelperFactory.create(SettingFavorSvcFragment.class);

    // Objects
    private CarmanDatabase mDB;
    private FirebaseFirestore firestore;
    private List<FavoriteProviderEntity> favoriteEntityList;
    private SparseArray<DocumentSnapshot> snapshotArray;
    private SettingFavoriteAdapter mAdapter;


    public SettingFavorSvcFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mDB = CarmanDatabase.getDatabaseInstance(getContext());
        firestore = FirebaseFirestore.getInstance();
        snapshotArray = new SparseArray<>();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View localView = inflater.inflate(R.layout.fragment_setting_favorite, container, false);
        RecyclerView recyclerView = localView.findViewById(R.id.recycler_favorite);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);

        mDB.favoriteModel().queryFavoriteProviders(Constants.SVC).observe(this, favoriteList -> {
            for(int i = 0; i < favoriteList.size(); i++) {
                log.i("Favorite: %s, %s", favoriteList.get(i).providerName, favoriteList.get(i).address);
            }

            favoriteEntityList = favoriteList;
            // Make the item drag by invoking ItemTouchHelperCallback
            mAdapter = new SettingFavoriteAdapter(favoriteList, snapshotArray, this);
            ItemTouchHelperCallback callback = new ItemTouchHelperCallback(getContext(), mAdapter);
            ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
            itemTouchHelper.attachToRecyclerView(recyclerView);

            recyclerView.setAdapter(mAdapter);

            for(int i = 0; i < favoriteList.size(); i++) {

                final int pos = i;
                final String stnId = favoriteList.get(pos).providerId;
                log.i("Station ID: %s", stnId);

                firestore.collection("svc_eval").document(stnId).get().addOnCompleteListener(task -> {
                    if(task.isSuccessful()) {
                        DocumentSnapshot snapshot = task.getResult();
                        if(snapshot != null && snapshot.exists()) {
                            mAdapter.addSparseSnapshotArray(pos, snapshot);
                            mAdapter.notifyItemChanged(pos, snapshot);
                        }

                    }
                });
            }
        });

        return localView;
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

            //getActivity().onBackPressed();
            //startActivity(new Intent(getActivity(), SettingPreferenceActivity.class));
            return true;
        } else return false;
    }

    /*
    @Override
    public void setFirstPlaceholder(FavoriteProviderEntity entity) {
        log.i("Listener: Add Favorite - %s", entity.providerName);
        mDB.favoriteModel().insertFavoriteProvider(entity);
    }
    */

    @Override
    public void setFirstPlaceholder(int category, String svcId) {

    }

    @Override
    public void deleteFavorite(int category, int position) {
        log.i("Listener: delete Favorite - %s", favoriteEntityList.get(position).providerName);
        mDB.favoriteModel().deleteProvider(favoriteEntityList.get(position));
    }
}
