package com.silverback.carman.fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.silverback.carman.R;
import com.silverback.carman.database.CarmanDatabase;
import com.silverback.carman.database.ExpenseBaseDao;
import com.silverback.carman.databinding.MainContentPager1Binding;
import com.silverback.carman.databinding.MainContentPager2Binding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.Constants;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MainExpPagerFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MainExpPagerFragment extends Fragment {
    private static final LoggingHelper log = LoggingHelperFactory.create(MainExpPagerFragment.class);
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";

    // TODO: Rename and change types of parameters
    private Calendar calendar;
    private DecimalFormat df;
    private CarmanDatabase mDB;
    private MainContentPager1Binding firstBinding;
    private MainContentPager2Binding expStmtsBinding;

    private int position;
    private int year, month;
    private int totalExpense;

    private MainExpPagerFragment() {
        // Required empty public constructor
    }

    public static MainExpPagerFragment newInstance(int position) {
        MainExpPagerFragment fragment = new MainExpPagerFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_PARAM1, position);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) position = getArguments().getInt(ARG_PARAM1);

        mDB = CarmanDatabase.getDatabaseInstance(requireActivity().getApplicationContext());
        calendar = Calendar.getInstance();

        df = (DecimalFormat)NumberFormat.getInstance();
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

                    df.applyPattern("#,###");
                    df.setDecimalSeparatorAlwaysShown(false);

                    //String total = String.valueOf(totalExpense);

                    //firstBinding.tvTotalExpense.setText(total);
                    animateExpenseCount(totalExpense);
                });
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