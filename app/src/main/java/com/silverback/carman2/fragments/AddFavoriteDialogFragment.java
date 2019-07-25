package com.silverback.carman2.fragments;


import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.silverback.carman2.ExpenseActivity;
import com.silverback.carman2.R;
import com.silverback.carman2.adapters.DistrictSpinnerAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.threads.LoadDistCodeTask;

/**
 * A simple {@link Fragment} subclass.
 */
public class AddFavoriteDialogFragment extends DialogFragment implements
        AdapterView.OnItemSelectedListener{

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(AddFavoriteDialogFragment.class);

    // Objects
    private LoadDistCodeTask spinnerTask;
    private ArrayAdapter sidoAdapter;
    private DistrictSpinnerAdapter sigunAdapter;
    private SharedPreferences mSettings;

    private String providerName;

    // UIs
    private Spinner sidoSpinner, sigunSpinner;

    // Fields
    private int category;

    // Default constructor
    private AddFavoriteDialogFragment() {
        // Required empty public constructor
    }

    // Instantiate DialogFragment as a SingleTon
    public static AddFavoriteDialogFragment newInstance(String name, int category) {
        AddFavoriteDialogFragment favoriteDialog = new AddFavoriteDialogFragment();
        Bundle args = new Bundle();
        args.putString("favoriteName", name);
        args.putInt("category", category);
        favoriteDialog.setArguments(args);

        return favoriteDialog;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSettings = ((ExpenseActivity)getActivity()).getSettings();
        providerName = getArguments().getString("favoriteName");
        category = getArguments().getInt("category");
    }

    @SuppressWarnings("ConstantConditions")
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        //LayoutInflater inflater = requireActivity().getLayoutInflater();
        View localView = View.inflate(getContext(), R.layout.fragment_favorite_dialog, null);
        //View localView = inflater.inflate(R.layout.fragment_favorite_dialog, null);
        setCancelable(false);

        TextView tvTitle = localView.findViewById(R.id.tv_title);
        sidoSpinner = localView.findViewById(R.id.spinner_sido);
        sigunSpinner= localView.findViewById(R.id.spinner_sigun);

        sidoSpinner.setOnItemSelectedListener(this);
        sigunSpinner.setOnItemSelectedListener(this);

        tvTitle.setText(getArguments().getString("favoriteName"));
        // sidoSpinner and adapter
        sidoAdapter = ArrayAdapter.createFromResource(
                getContext(), R.array.sido_name, android.R.layout.simple_spinner_item);
        sidoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sidoSpinner.setAdapter(sidoAdapter);
        // Sigun Spinner and Adapter
        sigunAdapter = new DistrictSpinnerAdapter(getContext());
        sigunSpinner.setAdapter(sigunAdapter);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(localView)
                .setPositiveButton(R.string.dialog_btn_confirm, (dialog, which) -> {})
                .setNegativeButton(R.string.dialog_btn_cancel, (dialog, which) -> {});

        return builder.create();
    }


    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if(parent == sidoSpinner) {
            //spinnerTask = ThreadManager.loadSpinnerDistCodeTask(this, position);

        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
