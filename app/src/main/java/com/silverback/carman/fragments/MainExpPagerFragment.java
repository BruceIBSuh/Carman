package com.silverback.carman.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.silverback.carman.R;
import com.silverback.carman.databinding.MainContentPager1Binding;
import com.silverback.carman.databinding.MainContentPager2Binding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

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
    private MainContentPager2Binding expStmtsBinding;
    private String thisMonth;
    private int position;

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


        Date currentTime = Calendar.getInstance().getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("MM", Locale.getDefault());
        thisMonth = sdf.format(currentTime);
        log.i("month: %s", thisMonth);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        switch(position){
            case 0:
                MainContentPager1Binding firstBinding = MainContentPager1Binding.inflate(inflater, container,false);
                firstBinding.tvSubtitleMonth.setText(thisMonth);
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
}