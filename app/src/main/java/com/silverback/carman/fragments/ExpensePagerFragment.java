package com.silverback.carman.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.silverback.carman.BaseActivity;
import com.silverback.carman.R;
import com.silverback.carman.database.CarmanDatabase;
import com.silverback.carman.database.GasManagerDao;
import com.silverback.carman.database.ServiceManagerDao;
import com.silverback.carman.databinding.FragmentPagerExpenseBinding;
import com.silverback.carman.utils.Constants;
import com.silverback.carman.viewmodels.FragmentSharedModel;

import java.text.DecimalFormat;
import java.util.List;

/**
 * This fragment displays last gas/service expenses in the ViewPager2.
 */
public class ExpensePagerFragment extends Fragment {

    // Logging
    //private static final LoggingHelper log = LoggingHelperFactory.create(ExpensePagerFragment.class);

    private final static DecimalFormat df = BaseActivity.getDecimalFormatInstance();
    // Objects
    private FragmentPagerExpenseBinding binding;
    private CarmanDatabase mDB;
    private FragmentSharedModel fragmentModel;
    private List<GasManagerDao.RecentGasData> gasDataList;
    private List<ServiceManagerDao.RecentServiceData> serviceList;

    // Fields
    private int page, category;
    private String lastInfo;

    private ExpensePagerFragment(){
        // Default Constructor. Leave this empty!
    }
    /*
    private static class FragmentHolder {
        private static final ExpensePagerFragment INSTANCE = new ExpensePagerFragment();
    }
    public static ExpensePagerFragment getInstance(int page) {
        log.i("static init:%s", page);
        Bundle args = new Bundle();
        args.putInt("page", page);
        FragmentHolder.INSTANCE.setArguments(args);
        return FragmentHolder.INSTANCE;
    }
    */

    // Instantiate Singleton of ExpensePagerFragment using Eager Initialization. Other type of
    // initialization should cause an error.
    public static ExpensePagerFragment getInstance(int pageNumber) {
        ExpensePagerFragment instance = new ExpensePagerFragment();
        Bundle args = new Bundle();
        args.putInt("page", pageNumber);
        instance.setArguments(args);
        return instance;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getArguments() != null) page = getArguments().getInt("page");
        // Instantiate CarmanDatabase as a type of singleton instance
        mDB = CarmanDatabase.getDatabaseInstance(requireActivity().getApplicationContext());
        fragmentModel = new ViewModelProvider(requireActivity()).get(FragmentSharedModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentPagerExpenseBinding.inflate(inflater);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // FragmentSharedModel observes if the fragment in the bottom viewpager changes b/w
        // ExpenseGasFragment and ExpenseServiceFragment.
        fragmentModel.getCurrentFragment().observe(getViewLifecycleOwner(), category -> {
            this.category = category;
            dispRecentExpensePager(category, page);
        });

    }

    public void dispRecentExpensePager(int category, int page) {
        // Query the recent data as the type of LiveData using Room(query on worker thread)
        this.category = category;

        if(category == Constants.GAS) {
            mDB.gasManagerModel().loadRecentGasData().observe(getViewLifecycleOwner(), data -> {
                gasDataList = data;
                lastInfo = (data.size() > page)?displayLastInfo(page):getString(R.string.toast_expense_no_data);
                binding.tvLastInfo.setText(lastInfo);
            });
        } else if(category == Constants.SVC) {
            mDB.serviceManagerModel().loadRecentServiceData().observe(getViewLifecycleOwner(), data -> {
                serviceList = data;
                lastInfo = (data.size() > page)?displayLastInfo(page):getString(R.string.toast_expense_no_data);
                binding.tvLastInfo.setText(lastInfo);
            });
        }
    }

    //Display the last 5 info retrieved from SQLite DB in the ViewPager with 5 fragments
    //@SuppressWarnings("ConstantConditions")
    private String displayLastInfo(int page) {
        // The latest mileage should be retrieved from SharedPreferernces. Otherwise, the last mileage
        // be retrieved from DB because the mileage value column in GasManagerTable and
        String format = requireContext().getResources().getString(R.string.date_format_1);
        String won = getString(R.string.unit_won);
        String liter = getString(R.string.unit_liter);

        if(category == Constants.GAS) {
            String date = BaseActivity.formatMilliseconds(format, gasDataList.get(page).dateTime);
            String a = String.format("%-10s%s%s", getString(R.string.gas_label_date), date, "\n");
            String b = String.format("%-10s%s%s%s", getString(R.string.exp_label_odometer), df.format(gasDataList.get(page).mileage), "km", "\n");
            String c = String.format("%-12s%s%s", getString(R.string.gas_label_station), gasDataList.get(page).stnName, "\n");
            String d = String.format("%-12s%s%s%s", getString(R.string.gas_label_expense), df.format(gasDataList.get(page).gasPayment), won, "\n");
            String e = String.format("%-12s%s%s", getString(R.string.gas_label_amount),df.format(gasDataList.get(page).gasAmount), liter);
            return a + b + c + d + e;

        } else if(category == Constants.SVC) {
            String date = BaseActivity.formatMilliseconds(format, serviceList.get(page).dateTime);
            String a = String.format("%-8s%s%s", getString(R.string.svc_label_date), date,"\n");
            String b = String.format("%-8s%s%s", getString(R.string.svc_label_provider), serviceList.get(page).svcName, "\n");
            String c = String.format("%-8s%s%1s%s", getString(R.string.exp_label_odometer), df.format(serviceList.get(page).mileage), "km", "\n");
            String d = String.format("%-8s%s%1s%s", getString(R.string.svc_label_payment), df.format(serviceList.get(page).totalExpense), won, "\n");

            return a + b + c + d;
        }

        return null;
    }
}
