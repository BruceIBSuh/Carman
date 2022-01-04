package com.silverback.carman.fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;

import com.silverback.carman.database.CarmanDatabase;
import com.silverback.carman.database.ExpenseBaseDao;
import com.silverback.carman.database.GasManagerDao;
import com.silverback.carman.databinding.MainContentPagerConfigBinding;
import com.silverback.carman.databinding.MainContentPagerTotalBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.Constants;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MainExpPagerFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MainExpPagerFragment extends Fragment {
    private static final LoggingHelper log = LoggingHelperFactory.create(MainExpPagerFragment.class);

    private static final int NumOfPrevMonths = 3;
    private RecentMonthlyExpense monthlyExpense;


    private Calendar calendar;
    private DecimalFormat df;
    private CarmanDatabase mDB;
    private MainContentPagerTotalBinding totalBinding;
    private MainContentPagerConfigBinding expConfigBinding;
    private int position;

    private MainExpPagerFragment() {
        // Required empty public constructor
    }

    public static MainExpPagerFragment newInstance(int position) {
        MainExpPagerFragment fragment = new MainExpPagerFragment();
        Bundle args = new Bundle();
        args.putInt("position", position);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) position = getArguments().getInt("position");

        mDB = CarmanDatabase.getDatabaseInstance(requireActivity().getApplicationContext());
        calendar = Calendar.getInstance(Locale.getDefault());
        //SimpleDateFormat sdf = new SimpleDateFormat(getString(R.string.date_format_1), Locale.getDefault());
        //SimpleDateFormat sdf2 = new SimpleDateFormat(getString(R.string.date_format_6), Locale.getDefault());
        df = (DecimalFormat)NumberFormat.getInstance();
        df.applyPattern("#,###");


    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        switch(position){
            case 0:
                totalBinding = MainContentPagerTotalBinding.inflate(inflater, container, false);
                return totalBinding.getRoot();

            case 1:
                expConfigBinding = MainContentPagerConfigBinding.inflate(inflater, container, false);
                return expConfigBinding.getRoot();
        }


        return null;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Must instantiate the object here to have getViewLifecycleOwner();
        monthlyExpense = new RecentMonthlyExpense();
        switch(position) {
            case 0:
                String month = String.valueOf(calendar.get(Calendar.MONTH) + 1);
                totalBinding.tvSubtitleMonth.setText(month);
                monthlyExpense.queryThisMonthExpense();
                break;
            case 1:
                monthlyExpense.setMonthlyExpenseConfig();
                break;

        }

    }


    // Animate the number of expense in this month.
    private void animateExpenseCount(int targetExpense) {
        ValueAnimator animator = ValueAnimator.ofInt(0, targetExpense);
        animator.setDuration(1000);
        animator.setInterpolator(new DecelerateInterpolator());

        animator.addUpdateListener(animation -> {
            int currentNum = (int)animation.getAnimatedValue();
            String total = df.format(currentNum);
            totalBinding.tvTotalExpense.setText(total);
        });

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                monthlyExpense.queryPrevMonthExpense();
                //totalBinding.recentGraphView.setExpenseData(totalExpense, getViewLifecycleOwner());
            }
        });

        animator.start();
    }


    // Class for setting a month and query the expenses
    private class RecentMonthlyExpense {
        int[] arrExpense;
        int[] arrConfig;
        int totalExpense, count;
        int gasTotal, svcTotal;

        RecentMonthlyExpense() {
            arrExpense = new int[NumOfPrevMonths];
            arrConfig = new int[4];
            totalExpense = 0;
            count = 1;
        }

        long setThisMonth() {
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            return calendar.getTimeInMillis();
        }

        long setPreviousMonth(boolean isStart) {
            if(isStart) {
                calendar.add(Calendar.MONTH, -1);
                calendar.set(Calendar.DAY_OF_MONTH, 1);
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
            } else {
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
                calendar.set(Calendar.HOUR_OF_DAY, 23);
                calendar.set(Calendar.MINUTE, 59);
                calendar.set(Calendar.SECOND, 59);
            }
            return calendar.getTimeInMillis();
        }

        LiveData<List<ExpenseBaseDao.ExpenseByMonth>> queryMonthlyExpense(long start, long end) {
            return mDB.expenseBaseModel().loadTotalExpenseByMonth(start, end);
        }

        // Retrieve the total expense of this month, putting it in the array for the graph and
        // animate the number.
        void queryThisMonthExpense() {
            //calendar.set(Calendar.DAY_OF_MONTH, 1);
            long start = setThisMonth();
            long end = System.currentTimeMillis();
            queryMonthlyExpense(start, end).observe(getViewLifecycleOwner(), results -> {
                totalExpense = 0;
                for(ExpenseBaseDao.ExpenseByMonth expense : results) totalExpense += expense.totalExpense;
                arrExpense[0] = totalExpense;
                df.setDecimalSeparatorAlwaysShown(false);
                animateExpenseCount(totalExpense);
            });
        }

        // Query the expense data of previous months except the current one.
        void queryPrevMonthExpense() {
            for(int i = 1; i < NumOfPrevMonths; i++) {
                final int index = i;
                long start = setPreviousMonth(true);
                long end = setPreviousMonth(false);
                queryMonthlyExpense(start, end).observe(
                        getViewLifecycleOwner(), data -> calcPrevExpense(index, data));
            }
        }

        // Calculate each total expense of last 2 months, putting them in the array and animate
        // the graph.
        void calcPrevExpense(final int index, List<ExpenseBaseDao.ExpenseByMonth> data) {
            count++;
            for(ExpenseBaseDao.ExpenseByMonth expense : data) {
                switch(index) {
                    case 1: arrExpense[1] += expense.totalExpense;break; //last month
                    case 2: arrExpense[2] += expense.totalExpense;break; //two months ago
                    default: break;
                }
            }

            if(count == NumOfPrevMonths) totalBinding.recentGraphView.setGraphData(arrExpense);
        }

        void setMonthlyExpenseConfig() {
            long start = setThisMonth();
            long end = System.currentTimeMillis();

            // First, query the gas and serviee expense in the current month from ExpenseBaseEntity, then
            // retrieve the wash expense from GasManagerEntity.
            mDB.expenseBaseModel().queryExpenseConfig(start, end).observe(getViewLifecycleOwner(), configs -> {
                gasTotal = 0;
                svcTotal = 0;
                for(ExpenseBaseDao.ExpenseConfig config : configs) {
                    if(config.category == Constants.GAS) gasTotal += config.totalExpense;
                    else if(config.category == Constants.SVC) svcTotal += config.totalExpense;
                }

                mDB.gasManagerModel().queryMiscExpense(start, end).observe(getViewLifecycleOwner(), expenses -> {
                    int washTotal = 0;
                    int miscTotal = 0;
                    for(GasManagerDao.MiscExpense miscExpense : expenses) {
                        washTotal += miscExpense.washPayment;
                        miscTotal += miscExpense.extraPayment;
                    }

                    // Display this month's expense by Category only after querying gas, svc, and
                    // wash done.
                    int gasOnly = gasTotal - washTotal - miscTotal;
                    expConfigBinding.tvExpenseGas.setText(df.format(gasOnly));
                    expConfigBinding.tvExpenseSvc.setText(df.format(svcTotal));
                    expConfigBinding.tvExpenseWash.setText(df.format(washTotal));
                    expConfigBinding.tvExpenseMisc.setText(df.format(miscTotal));

                    // Pass the data to the custom graph view.
                    int[] data = {gasOnly, svcTotal, washTotal, miscTotal};
                    expConfigBinding.expenseConfigView.setExpenseConfigData(data);
                });
            });
        }
    }
}