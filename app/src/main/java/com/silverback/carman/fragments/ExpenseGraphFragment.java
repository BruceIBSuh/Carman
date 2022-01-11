package com.silverback.carman.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.silverback.carman.R;
import com.silverback.carman.database.CarmanDatabase;
import com.silverback.carman.database.ExpenseBaseDao;
import com.silverback.carman.databinding.FragmentStatGraphBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.Constants;
import com.silverback.carman.viewmodels.FragmentSharedModel;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ExpenseGraphFragment extends Fragment implements View.OnClickListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(ExpenseGraphFragment.class);

    // Object References
    private FragmentStatGraphBinding binding;
    private CarmanDatabase mDB;
    private FragmentSharedModel fragmentSharedModel;
    private Calendar calendar;
    private SimpleDateFormat sdf;

    // GraphView
    //private StatGraphView graph;
    //private static BarGraphSeries<DataPoint> series;
    //private static DataPoint[] dataPoint;
    //private static int[] monthlyTotalExpense;

    // UI's
    private TextView tvYear;

    // Fields
    private int currentYear, targetYear;
    private int gasCategory, svcCategory;

    // Constructor
    public ExpenseGraphFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        mDB = CarmanDatabase.getDatabaseInstance(requireActivity().getApplicationContext());
        fragmentSharedModel = new ViewModelProvider(requireActivity()).get(FragmentSharedModel.class);
        calendar = Calendar.getInstance(Locale.getDefault());
        sdf = new SimpleDateFormat("MM", Locale.getDefault());

        //dataPoint = new DataPoint[12];
        //series = new BarGraphSeries<>(dataPoint);
        //monthlyTotalExpense = new int[12];
        currentYear = calendar.get(Calendar.YEAR);
        log.i("current year: %s", currentYear);
        targetYear = currentYear;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        //View view = inflater.inflate(R.layout.fragment_stat_graph, container, false);
        binding = FragmentStatGraphBinding.inflate(inflater);

        // GraphView
        //graph = view.findViewById(R.id.graphView);
        //graph.getViewport().setXAxisBoundsManual(true);
        //graph.getViewport().setMaxX(12);
        //series.setDrawValuesOnTop(true);

        // UI's
        binding.tvYear.setText(String.valueOf(calendar.get(Calendar.YEAR)));

        // ImageButton
        binding.btnArrowLeft.setOnClickListener(this);
        binding.btnArrowRight.setOnClickListener(this);

        loadTotalExpense(currentYear, Constants.GAS, Constants.SVC);

        // Creates an instance of AsyncTaskk with this year given as params for doInBackground.
        //new StatGraphViewTask(mDB, graph, calendar).execute(calendar.get(Calendar.YEAR));
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstancestate) {
        super.onViewCreated(view, savedInstancestate);

        // Get the selected item position of the spinner defined in ExpenseStmtsFragment for querying
        // the expense by category
        fragmentSharedModel.getExpenseCategory().observe(getViewLifecycleOwner(), category -> {
            switch(category) {
                case 0: // all
                    gasCategory = Constants.GAS;
                    svcCategory = Constants.SVC;
                    break;
                case 1: // gas
                    gasCategory = Constants.GAS;
                    svcCategory = -1;
                    break;
                case 2: // service
                    gasCategory = -1;
                    svcCategory = Constants.SVC;
                    break;
            }

            loadTotalExpense(targetYear, gasCategory, svcCategory);
        });
    }


    @Override
    public void onClick(View view) {
        if(view.getId() == R.id.btn_arrow_left) {
            calendar.add(Calendar.YEAR, -1);
            binding.tvYear.setText(String.valueOf(calendar.get(Calendar.YEAR)));

        } else if(view.getId() == R.id.btn_arrow_right) {
            if(calendar.get(Calendar.YEAR) < currentYear) {
                calendar.add(Calendar.YEAR, 1);
                binding.tvYear.setText(String.valueOf(calendar.get(Calendar.YEAR)));
            }
        }

        targetYear = calendar.get(Calendar.YEAR);
        loadTotalExpense(targetYear, gasCategory, svcCategory);
    }

    private void loadTotalExpense(int year, int gas, int service) {
        calendar.set(year, 0, 1, 0, 0);
        long start = calendar.getTimeInMillis();
        calendar.set(year, 11, 31, 23, 59, 59);
        long end = calendar.getTimeInMillis();

        mDB.expenseBaseModel().loadMonthlyExpense(gas, service, start, end).observe(
                getViewLifecycleOwner(), data -> {
                    log.i("monthly data: %s", data);
                    binding.graphView.setGraphData(calcMonthlyExpense(data));
                });
    }

    private int[] calcMonthlyExpense(List<ExpenseBaseDao.ExpenseByMonth> data) {
        int[] monthlyTotal = new int[12];
        for (ExpenseBaseDao.ExpenseByMonth monthlyExpense : data) {
            int month = Integer.parseInt(sdf.format(monthlyExpense.dateTime));
            switch (month) {
                case 1: monthlyTotal[0] += monthlyExpense.totalExpense; break;
                case 2: monthlyTotal[1] += monthlyExpense.totalExpense; break;
                case 3: monthlyTotal[2] += monthlyExpense.totalExpense; break;
                case 4: monthlyTotal[3] += monthlyExpense.totalExpense; break;
                case 5: monthlyTotal[4] += monthlyExpense.totalExpense; break;
                case 6: monthlyTotal[5] += monthlyExpense.totalExpense; break;
                case 7: monthlyTotal[6] += monthlyExpense.totalExpense; break;
                case 8: monthlyTotal[7] += monthlyExpense.totalExpense; break;
                case 9: monthlyTotal[8] += monthlyExpense.totalExpense; break;
                case 10: monthlyTotal[9] += monthlyExpense.totalExpense; break;
                case 11: monthlyTotal[10] += monthlyExpense.totalExpense; break;
                case 12: monthlyTotal[11] += monthlyExpense.totalExpense; break;
                default: break;
            }
        }
        return monthlyTotal;
    }
}
