package com.silverback.carman.fragments;


import android.os.Bundle;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.silverback.carman.adapters.SettingFavAdapter;
import com.silverback.carman.database.CarmanDatabase;
import com.silverback.carman.database.FavoriteProviderEntity;
import com.silverback.carman.databinding.FragmentSettingFavoriteBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.threads.StationFavTask;
import com.silverback.carman.utils.Constants;
import com.silverback.carman.utils.ItemTouchHelperCallback;

import java.util.List;

/**
 * This fragment is to show favorite gas statoins which are retrieved from CarmanDatabase and passed
 * to SettingFavAdapter extending RecyclerView.Adapter.
 *
 * Drag and drop action is set to the RecyclerView using ItemTouchHelperCallback, the util class
 * extending ItemTouchHelper.Callback, which defines the interface of RecyclerItemMoveListener when
 * an item is moving up and down or deleted by dragging and drop.
 *
 * The callbacks of the interface are overrided in SettingFavAdapter, invoking onDragItem() and
 * onDeleteItem(), notify the adapter of which item moves up and down or is deleted. The adapter, in
 * turn, notifies this fragment of initiating StationFavTask to get the price data or
 */
public class SettingFavorGasFragment extends Fragment implements
        SettingFavAdapter.OnFavoriteAdapterListener{

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(SettingFavorGasFragment.class);

    // Objects
    private FragmentSettingFavoriteBinding binding;
    private CarmanDatabase mDB;
    private FirebaseFirestore firestore;
    private SettingFavAdapter mAdapter;
    private SparseArray<DocumentSnapshot> sparseSnapshotArray;
    private StationFavTask favStationTaskk;
    private List<FavoriteProviderEntity> favList;

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
        sparseSnapshotArray = new SparseArray<>();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentSettingFavoriteBinding.inflate(LayoutInflater.from(getContext()), container, false);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        binding.recyclerFavorite.setLayoutManager(layoutManager);

        // Query the favorite gas stations from FavoriteProviderEntity
        mDB.favoriteModel().queryFavoriteProviders(Constants.GAS).observe(getViewLifecycleOwner(), favList -> {
            this.favList = favList;
            mAdapter = new SettingFavAdapter(favList, sparseSnapshotArray, this);

            ItemTouchHelperCallback callback = new ItemTouchHelperCallback(getContext(), mAdapter);
            ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
            itemTouchHelper.attachToRecyclerView(binding.recyclerFavorite);
            binding.recyclerFavorite.setAdapter(mAdapter);

            // Retrieve the evaluation of favroite stations from Firestore, add it to the
            // SparseArray, then make the partial binding of recyclerview items.
            for(int i = 0; i < favList.size(); i++) {
                final int pos = i;
                final String stnId = favList.get(pos).providerId;

                firestore.collection("gas_eval").document(stnId).get().addOnCompleteListener(task -> {
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

        // Inflate the layout for this fragment
        return binding.getRoot();
    }


    @Override
    public void onPause() {
        super.onPause();
        if(favStationTaskk != null) favStationTaskk = null;
    }


    // To make onOptionsItemSelected() working in Fragment,  setHasOptionsMenu defined in onCreate()
    // is set to true and the return value must be true. This callback concurrently invokes the same
    // method defined in the parent Activity, in which the return value must be false in order to
    // prevent invoking sequentially.
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem menuItem) {
        if(menuItem.getItemId() == android.R.id.home) {
            // Update the placeholder in FavoriteProviderEntity accroding to the position of
            // the edited fasvorte list.
            int position = 0;
            for(FavoriteProviderEntity entity : favList) {
                log.i("Favorite placeholder: %s, %s", entity.providerName, entity.placeHolder);
                entity.placeHolder = position;
                position++;
            }
            mDB.favoriteModel().updatePlaceHolder(favList);
        }

        return true;
    }

    // If an item moves up to the first placeholder, initiate the task to fetch the price data from
    // the Opinet server and save it in the cache storage.
    @Override
    public void setFirstPlaceholder(int category, String stnId) {
        if(category == Constants.GAS && !stnId.isEmpty()) {
            log.i("The favorite changed: %s", stnId);
            //favStationTaskk = ThreadManager.startFavoritePriceTask(getContext(), null, stnId, true);
        }
    }

    @Override
    public void deleteFavorite(int category, int position) {
        mDB.favoriteModel().deleteProvider(favList.get(position));
        favList.remove(position);
        mAdapter.notifyItemRemoved(position);
    }


}
