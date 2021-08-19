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

import com.silverback.carman.R;
import com.silverback.carman.database.CarmanDatabase;
import com.silverback.carman.database.ExpenseBaseDao;
import com.silverback.carman.databinding.MainContentPagerCarwashBinding;
import com.silverback.carman.databinding.MainContentPagerExtraBinding;
import com.silverback.carman.databinding.MainContentPagerGasBinding;
import com.silverback.carman.databinding.MainContentPagerSvcBinding;
import com.silverback.carman.databinding.MainContentPagerTotalBinding;
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
    private MainContentPagerTotalBinding totalBinding;
    private MainContentPagerGasBinding gasBinding;
    private MainContentPagerSvcBinding svcBinding;
    private MainContentPagerCarwashBinding washBinding;
    private MainContentPagerExtraBinding extraBinding;

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
        sdf = new SimpleDateFormat(getString(R.string.date_format_1), Locale.getDefault());
        df = (DecimalFormat)NumberFormat.getInstance();
        df.applyPattern("#,###");
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        switch(position){
            case 0:
                totalBinding = MainContentPagerTotalBinding.inflate(inflater, container,false);
                String month = String.valueOf(calendar.get(Calendar.MONTH) + 1);
                totalBinding.tvSubtitleMonth.setText(month);
                displayTotalExpense();
                return totalBinding.getRoot();
            case 1:
                gasBinding = MainContentPagerGasBinding.inflate(inflater, container, false);
                displayGasExpense();
                return gasBinding.getRoot();
            case 2:
                washBinding = MainContentPagerCarwashBinding.inflate(inflater, container, false);
                return washBinding.getRoot();
            case 3:
                svcBinding = MainContentPagerSvcBinding.inflate(inflater, container, false);
                displayServiceExpense();
                return svcBinding.getRoot();
            case 4:
                extraBinding = MainContentPagerExtraBinding.inflate(inflater, container, false);
                return extraBinding.getRoot();
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

    private void displayGasExpense() {
        mDB.gasManagerModel().loadLatestGasData().observe(getViewLifecycleOwner(), gasData -> {
            gasBinding.tvGasExpense.setText(df.format(gasData.gasPayment));

            calendar.setTimeInMillis(gasData.dateTime);
            gasBinding.tvGasDate.setText(sdf.format(calendar.getTime()));

            gasBinding.tvGasStation.setText(gasData.stnName);
            gasBinding.tvAmount.setText(df.format(gasData.gasAmount));
            gasBinding.tvGasMileage.setText(df.format(gasData.mileage));
        });
    }

    private void displayWashExpense() {

    }

    private void displayServiceExpense() {
        mDB.serviceManagerModel().loadLatestSvcData().observe(getViewLifecycleOwner(), svcData -> {
            if(svcData == null) return;
            svcBinding.tvSvcDate.setText(sdf.format(calendar.getTime()));
            svcBinding.tvSvcStation.setText(svcData.svcName);
            svcBinding.tvSvcExpense.setText(df.format(svcData.totalExpense));
            svcBinding.tvSvcMileage.setText(df.format(svcData.mileage));
        });
    }

    private void animateExpenseCount(int end) {
        ValueAnimator animator = ValueAnimator.ofInt(0, end);
        animator.setDuration(1000);
        //animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            int currentNum = (int)animation.getAnimatedValue();
            String total = df.format(currentNum);
            totalBinding.tvTotalExpense.setText(total);
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