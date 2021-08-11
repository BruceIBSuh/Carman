package com.silverback.carman.fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.silverback.carman.BaseActivity;
import com.silverback.carman.R;
import com.silverback.carman.database.CarmanDatabase;
import com.silverback.carman.database.ExpenseBaseDao;
import com.silverback.carman.database.GasManagerDao;
import com.silverback.carman.databinding.MainContentPager1Binding;
import com.silverback.carman.databinding.MainContentPager2Binding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.Constants;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MainContentPagerFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MainContentPagerFragment extends Fragment {
    private static final LoggingHelper log = LoggingHelperFactory.create(MainContentPagerFragment.class);

    // TODO: Rename and change types of parameters
    private Calendar calendar;
    private DecimalFormat df;
    private SimpleDateFormat sdf;
    private CarmanDatabase mDB;
    private MainContentPager1Binding firstBinding;
    private MainContentPager2Binding expStmtsBinding;

    private int position;
    private int totalExpense, gasExpense, svcExpense;

    private MainContentPagerFragment() {
        // Required empty public constructor
    }

    public static MainContentPagerFragment newInstance(int position) {
        MainContentPagerFragment fragment = new MainContentPagerFragment();
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
        sdf = new SimpleDateFormat();
        df = (DecimalFormat)NumberFormat.getInstance();
        df.applyPattern("#,###");
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        switch(position){
            case 0:
                firstBinding = MainContentPager1Binding.inflate(inflater, container,false);
                String month = String.valueOf(calendar.get(Calendar.MONTH) + 1);
                firstBinding.tvSubtitleMonth.setText(month);
                displayTotalExpense();
                return firstBinding.getRoot();
            case 1:
                expStmtsBinding = MainContentPager2Binding.inflate(inflater, container, false);
                expStmtsBinding.tvSubtitleExpense.setText(R.string.main_subtitle_gas);
                mDB.gasManagerModel().loadLatestGasData().observe(getViewLifecycleOwner(), gasData -> {
                    expStmtsBinding.tvGasExpense.setText(df.format(gasData.gasPayment));

                    calendar.setTimeInMillis(gasData.dateTime);
                    sdf.applyPattern(getString(R.string.date_format_1));
                    expStmtsBinding.tvGasDate.setText(sdf.format(calendar.getTime()));

                    expStmtsBinding.tvGasStation.setText(gasData.stnName);
                    expStmtsBinding.tvAmount.setText(String.valueOf(gasData.gasAmount));
                });

                return expStmtsBinding.getRoot();
            case 2:
                expStmtsBinding = MainContentPager2Binding.inflate(inflater, container, false);
                expStmtsBinding.tvSubtitleExpense.setText(R.string.main_subtitle_svc);
                return expStmtsBinding.getRoot();
        }

        return null;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    private void displayTotalExpense() {
        final int year = calendar.get(Calendar.YEAR);
        final int month = calendar.get(Calendar.MONTH);

        calendar.set(year, month, 1, 0, 0);
        long start = calendar.getTimeInMillis();
        calendar.set(year, month, 31, 23, 59, 59);
        long end = calendar.getTimeInMillis();

        mDB.expenseBaseModel().loadMonthlyExpense(Constants.GAS, Constants.SVC, start, end)
                .observe(getViewLifecycleOwner(), expList -> {
                    for(ExpenseBaseDao.ExpenseByMonth expense : expList)
                        totalExpense += expense.totalExpense;

                    df.setDecimalSeparatorAlwaysShown(false);
                    animateExpenseCount(totalExpense);
                });
    }

    private void dispGasExpense() {

    }

    private void animateExpenseCount(int end) {
        log.i("animate expense");
        ValueAnimator animator = ValueAnimator.ofInt(0, end);
        animator.setDuration(1000);
        //animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            int currentNum = (int)animation.getAnimatedValue();
            String total = df.format(currentNum);
            firstBinding.tvTotalExpense.setText(total);
        });

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                log.i("animation ended");
            }
        });

        animator.start();
    }
}