package com.silverback.carman2.fragments;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.silverback.carman2.ExpenseActivity;
import com.silverback.carman2.R;
import com.silverback.carman2.adapters.ExpenseStatRecyclerAdapter;
import com.silverback.carman2.database.CarmanDatabase;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * A simple {@link Fragment} subclass.
 */
public class StatStmtsFragment extends Fragment {

    private static final LoggingHelper log = LoggingHelperFactory.create(StatStmtsFragment.class);

    // Objects
    private SharedPreferences mSettings;
    private CarmanDatabase mDB;

    public StatStmtsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(getActivity() == null) return;
        mSettings = ((ExpenseActivity)getActivity()).getSettings();
        mDB = CarmanDatabase.getDatabaseInstance(getActivity().getApplicationContext());

        mDB.expenseBaseMdoel().loadAllExpenses().observe(this, data ->
                log.i("data: %s, %s", data.category, data.totalExpense));

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View localView = inflater.inflate(R.layout.fragment_stat_stmts, container, false);

        RecyclerView recyclerExpense = localView.findViewById(R.id.recycler_stats);
        recyclerExpense.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerExpense.setHasFixedSize(true);
        recyclerExpense.setAdapter(new ExpenseStatRecyclerAdapter());

        return localView;
    }

}
