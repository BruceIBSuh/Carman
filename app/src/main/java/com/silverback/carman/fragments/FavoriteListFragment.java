package com.silverback.carman.fragments;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.silverback.carman.R;
import com.silverback.carman.adapters.FavoriteListAdapter;
import com.silverback.carman.database.CarmanDatabase;
import com.silverback.carman.database.FavoriteProviderEntity;
import com.silverback.carman.databinding.DialogFavoriteListBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.viewmodels.FragmentSharedModel;
import com.silverback.carman.utils.Constants;

import java.util.List;

public class FavoriteListFragment extends DialogFragment {

    private static final LoggingHelper log = LoggingHelperFactory.create(FavoriteListFragment.class);

    // Objects
    //private static FavoriteListFragment favoriteFragment;
    private DialogFavoriteListBinding binding;
    private FavoriteListAdapter mAdapter;
    private CarmanDatabase mDB;
    private FragmentSharedModel fragmentModel;
    private List<FavoriteProviderEntity> favoriteList;

    // Fields
    private String title;
    private int category;

    // private Constructor
    private FavoriteListFragment() {
        // Leave empty constructor which may be invoked by FragmentFactory.
    }

    private static class LazyHolder {
        private static final FavoriteListFragment INSTANCE = new FavoriteListFragment();
    }

    // Instantiate the fragment as Singleton(Bill Pugh Model)
    public static FavoriteListFragment newInstance(String title, int category) {
        Bundle args = new Bundle();
        args.putString("title", title);
        args.putInt("category", category);
        LazyHolder.INSTANCE.setArguments(args);
        return LazyHolder.INSTANCE;
    }

    //@SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDB = CarmanDatabase.getDatabaseInstance(getContext());
        fragmentModel = new ViewModelProvider(requireActivity()).get(FragmentSharedModel.class);

        if (getArguments() != null) {
            title = getArguments().getString("title");
            category = getArguments().getInt("category");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        binding = DialogFavoriteListBinding.inflate(getLayoutInflater());
        binding.tvFavoriteTitle.setText(title);

        mDB.favoriteModel().queryFavoriteProviders(category).observe(this, data -> {
            favoriteList = data;
            mAdapter = new FavoriteListAdapter(favoriteList);
            binding.lvFavorite.setAdapter(mAdapter);
            binding.lvFavorite.setOnItemClickListener((adapterView, v, pos, id) -> {
                FavoriteProviderEntity entity = (FavoriteProviderEntity)mAdapter.getItem(pos);
                switch(category) {
                    case Constants.GAS:
                        fragmentModel.getFavoriteGasEntity().setValue(entity);
                        break;
                    case Constants.SVC:
                        fragmentModel.getFavoriteSvcEntity().setValue(entity);
                        break;
                }

                dismiss();
            });
        });

        builder.setView(binding.getRoot()).setPositiveButton("CANCEL", (dialog, which) -> dismiss());
        return builder.create();
    }
}
