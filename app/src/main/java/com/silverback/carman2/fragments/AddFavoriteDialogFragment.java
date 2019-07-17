package com.silverback.carman2.fragments;


import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.silverback.carman2.ManagementActivity;
import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

/**
 * A simple {@link Fragment} subclass.
 */
public class AddFavoriteDialogFragment extends DialogFragment {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(AddFavoriteDialogFragment.class);

    // Objects
    private static AddFavoriteDialogFragment favoriteDialog;
    private SharedPreferences mSettings;
    private String providerName;

    // Fields
    private int category;

    // Default constructor
    private AddFavoriteDialogFragment() {
        // Required empty public constructor
    }

    // Instantiate DialogFragment as a SingleTon
    public static AddFavoriteDialogFragment newInstance(String name, int category) {
        if(favoriteDialog == null) {
            favoriteDialog = new AddFavoriteDialogFragment();
            Bundle args = new Bundle();
            args.putString("favoriteName", name);
            args.putInt("category", category);
            favoriteDialog.setArguments(args);
        }

        return favoriteDialog;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSettings = ((ManagementActivity)getActivity()).getSettings();
        providerName = getArguments().getString("favoriteName");
        category = getArguments().getInt("category");
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_favorite_dialog, container, false);
    }


    /*
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        //View localView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_service_favorite, null);
        View localView = View.inflate(getContext(), R.layout.fragment_favorite_dialog, null);
        setCancelable(false);

        return null;
    }
    */

}
