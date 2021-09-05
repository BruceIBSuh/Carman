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
import com.silverback.carman.database.ExpenseBaseDao;
import com.silverback.carman.databinding.FragmentStatStmtsBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.RecyclerDividerUtil;
import com.silverback.carman.viewmodels.FragmentSharedModel;
import com.silverback.carman.utils.Constants;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class StatStmtsFragment extends Fragment implements AdapterView.OnItemSelectedListener{

    private static final LoggingHelper log = LoggingHelperFactory.create(StatStmtsFragment.class);
    private static final int TotalExpense = 0;
    private static final int GasExpense = 1;
    private static final int SvcExpense = 2;

    // Objects
    private FragmentStatStmtsBinding binding;
    private CarmanDatabase mDB;
    private ExpStatStmtsAdapter mAdapter;
    private FragmentSharedModel fragmentModel;
    private List<ExpenseBaseDao.ExpenseStatements> expList;

    public StatStmtsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDB = CarmanDatabase.getDatabaseInstance(requireActivity().getApplicationContext());
        fragmentModel = new ViewModelProvider(requireActivity()).get(FragmentSharedModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentStatStmtsBinding.inflate(inflater);


        // Create the spinner for selecting an expense category.
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.spinner_expense_stmts, R.layout.spinner_stat_stmts);
        spinnerAdapter.setDropDownViewResource(R.layout.spinner_stat_dropdown);
        binding.spinnerExpense.setAdapter(spinnerAdapter);
        binding.spinnerExpense.setOnItemSelectedListener(this);

        // Create the recyclerview to show the expense list sorted by category
        binding.recyclerStats.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerStats.setHasFixedSize(false);
        RecyclerDividerUtil divider = new RecyclerDividerUtil(Constants.DIVIDER_HEIGHT_EXPENSE, 0,
                ContextCompat.getColor(requireContext(), R.color.recyclerDivider));
        binding.recyclerStats.addItemDecoration(divider);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstancestate) {
        super.onViewCreated(view, savedInstancestate);
        log.i("onViewCreated");
        mDB.expenseBaseModel().loadExpenseByCategory(Constants.GAS, Constants.SVC)
                .observe(getViewLifecycleOwner(), data -> {
                    mAdapter = new ExpStatStmtsAdapter(data);
                    binding.recyclerStats.setAdapter(mAdapter);
                    binding.spinnerExpense.setSelection(0);
                });
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        log.i("onItemSelected: %s", position);
        switch(position) {
            case TotalExpense: queryExpenseByCategory(Constants.GAS, Constants.SVC); break;
            case GasExpense:queryExpenseByCategory(Constants.GAS, -1); break;
            case SvcExpense:queryExpenseByCategory(-1,  Constants.SVC); break;
        }

        //binding.recyclerStats.setAdapter(mAdapter);

        // A spinner-selected category should be shared with StatGraphFragmeht to redraw the graph
        // if any change is made.
        fragmentModel.getTotalExpenseByCategory().setValue(position);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        log.i("spinner nothing selected");
    }

    public void queryExpenseByCategory(int category1, int category2) {
        mDB.expenseBaseModel().loadExpenseByCategory(category1, category2).observe(
                getViewLifecycleOwner(), data -> {
                    mAdapter.setStatsStmtList(data);
                    mAdapter.notifyDataSetChanged();
                });
    }


}
