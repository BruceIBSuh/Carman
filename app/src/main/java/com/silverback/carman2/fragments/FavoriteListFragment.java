package com.silverback.carman2.fragments;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.silverback.carman2.ExpenseActivity;
import com.silverback.carman2.R;
import com.silverback.carman2.adapters.FavoriteListAdapter;
import com.silverback.carman2.database.CarmanDatabase;
import com.silverback.carman2.database.FavoriteProviderEntity;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.FragmentSharedModel;
import com.silverback.carman2.utils.Constants;

import java.util.List;

public class FavoriteListFragment extends DialogFragment {

    private static final LoggingHelper log = LoggingHelperFactory.create(FavoriteListFragment.class);

    // Objects
    private static FavoriteListFragment favoriteFragment;
    private FavoriteListAdapter mAdapter;
    private CarmanDatabase mDB;
    private FragmentSharedModel fragmentModel;
    private List<FavoriteProviderEntity> favoriteList;


    // Fields
    private String title;
    private int category;


    // private Constructor
    private FavoriteListFragment () {}

    // Instantiate the fragment as Singleton
    static FavoriteListFragment newInstance(String title, int category) {
        if(favoriteFragment == null) favoriteFragment = new FavoriteListFragment();
        Bundle args = new Bundle();
        args.putString("title", title);
        args.putInt("category", category);
        favoriteFragment.setArguments(args);

        return favoriteFragment;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDB = CarmanDatabase.getDatabaseInstance(getContext());
        fragmentModel = ((ExpenseActivity)getActivity()).getFragmentSharedModel();
    }

    @SuppressWarnings("ConstantConditions")
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        View localView = View.inflate(getContext(), R.layout.dialog_favorite_list, null);

        if (getArguments() != null) {
            title = getArguments().getString("title");
            category = getArguments().getInt("category");
        }

        ListView listView = localView.findViewById(R.id.lv_favorite);
        TextView tvTitle = localView.findViewById(R.id.tv_favorite_title);
        tvTitle.setText(title);

        mDB = CarmanDatabase.getDatabaseInstance(getContext());

        mDB.favoriteModel().queryFavoriteProvider(category).observe(this, data -> {
            favoriteList = data;
            mAdapter = new FavoriteListAdapter(favoriteList);
            listView.setAdapter(mAdapter);

        });


        // ListView item click event handler
        listView.setOnItemClickListener((parent, view, position, id) -> {
            log.i("Click event: %s, %s, %s, %s", parent, view, position, id);
            FavoriteProviderEntity entity = (FavoriteProviderEntity)mAdapter.getItem(position);
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

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(localView)
                .setNegativeButton("CANCEL", null);

        return builder.create();
    }
}
