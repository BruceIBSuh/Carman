package com.silverback.carman.fragments;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.silverback.carman.R;
import com.silverback.carman.adapters.ExpStatStmtsAdapter;
import com.silverback.carman.database.CarmanDatabase;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.viewmodels.FragmentSharedModel;
import com.silverback.carman.utils.Constants;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * A simple {@link Fragment} subclass.
 */
public class StatStmtsFragment extends Fragment implements AdapterView.OnItemSelectedListener{

    private static final LoggingHelper log = LoggingHelperFactory.create(StatStmtsFragment.class);
    private static final int TotalExpense = 0;
    private static final int GasExpense = 1;
    private static final int SvcExpense = 2;

    // Objects₩
    //private SharedPreferences mSettings;
    private CarmanDatabase mDB;
    private RecyclerView recyclerExpense;
    private FragmentSharedModel fragmentSharedModel;

    public StatStmtsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getActivity() == null) return;

        //mSettings = ((ExpenseActivity)getActivity()).getSettings();
        mDB = CarmanDatabase.getDatabaseInstance(getActivity().getApplicationContext());
        fragmentSharedModel = new ViewModelProvider(getActivity()).get(FragmentSharedModel.class);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View localView = inflater.inflate(R.layout.fragment_stat_stmts, container, false);
        Spinner spinner = localView.findViewById(R.id.spinner_expense);
        recyclerExpense = localView.findViewById(R.id.recycler_stats);

        spinner.setOnItemSelectedListener(this);

        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(
                getContext(), R.array.spinner_expense_stmts, R.layout.spinner_stat_stmts);
        spinnerAdapter.setDropDownViewResource(R.layout.spinner_stat_dropdown);
        spinner.setAdapter(spinnerAdapter);

        recyclerExpense.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerExpense.setHasFixedSize(true);

        return localView;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        switch(position) {
            case TotalExpense:
                mDB.expenseBaseModel().loadExpenseByCategory(Constants.GAS, Constants.SVC).observe(getViewLifecycleOwner(),
                        data -> recyclerExpense.setAdapter(new ExpStatStmtsAdapter(data)));
                break;
            case GasExpense:
                mDB.expenseBaseModel().loadExpenseByCategory(Constants.GAS, -1).observe(getViewLifecycleOwner(),
                        data -> recyclerExpense.setAdapter(new ExpStatStmtsAdapter(data)));
                break;
            case SvcExpense:
                mDB.expenseBaseModel().loadExpenseByCategory(-1, Constants.SVC).observe(getViewLifecycleOwner(),
                        data -> recyclerExpense.setAdapter(new ExpStatStmtsAdapter(data)));
                break;
        }
        // Queried expense of the category selected by the spinner is shared with StatGraphFragmeht
        // which is another component of StatAc
        fragmentSharedModel.getExpenseCategory().setValue(position);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        log.i("spinner nothing selected");
    }

    // Invoked from onPageScrollStateChange in ExpenseActivity in order to load the fragment
    // w/o the recyclerview and call the recyclerview when the fragment shows up in the tabpager
    public void queryExpense() {
        mDB.expenseBaseModel().loadExpenseByCategory(1, 2).observe(this, data -> {
            log.i("All Expenses: %s", data.size());
            recyclerExpense.setAdapter(new ExpStatStmtsAdapter(data));
        });
    }


}
