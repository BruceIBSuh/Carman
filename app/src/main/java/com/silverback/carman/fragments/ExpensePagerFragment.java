package com.silverback.carman.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.silverback.carman.BaseActivity;
import com.silverback.carman.R;
import com.silverback.carman.database.CarmanDatabase;
import com.silverback.carman.database.GasManagerDao;
import com.silverback.carman.database.ServiceManagerDao;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.viewmodels.FragmentSharedModel;

import java.text.DecimalFormat;
import java.util.List;

/**
 * This fragment displays last gas/service expenses in the ViewPager2.
 */
public class ExpensePagerFragment extends Fragment {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(ExpensePagerFragment.class);
    private final static DecimalFormat df = BaseActivity.getDecimalFormatInstance();

    // Objects
    private CarmanDatabase mDB;
    private FragmentSharedModel fragmentModel;
    private Fragment currentFragment;
    private List<GasManagerDao.RecentGasData> gasDataList;
    private List<ServiceManagerDao.RecentServiceData> serviceList;
    // UIs
    private TextView tvLastInfo, tvPage;

    // Fields
    private int numPage;
    private String lastInfo;

    // Define Interface to pass over the current mileage to GasManagerActivity
    /*
    public interface OnLastMileageInfoFragmentListener {
        void getLastMileageInfo(int arg);
    }
    */

    private ExpensePagerFragment(){
        // Default Constructor. Leave this empty!
    }

    // Instantiate Singleton of ExpensePagerFragment
    public static ExpensePagerFragment create(int pageNumber) {
        //Instantiate SharedPreferences for getting tableName set as default
        ExpensePagerFragment fragment = new ExpensePagerFragment();
        Bundle args = new Bundle();
        args.putInt("page", pageNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getActivity() == null) return;
        if(getArguments() != null) numPage = getArguments().getInt("page");
        // Instantiate CarmanDatabase as a type of singleton instance
        mDB = CarmanDatabase.getDatabaseInstance(getActivity().getApplicationContext());
        fragmentModel = new ViewModelProvider(requireActivity()).get(FragmentSharedModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        //ViewGroup rootView = (ViewGroup)inflater.inflate(R.layout.fragment_pager_expense, container, false);
        View localView = inflater.inflate(R.layout.fragment_pager_expense, container, false);
        tvLastInfo = localView.findViewById(R.id.tv_lastInfo);
        tvPage = localView.findViewById(R.id.tv_page);

        return localView;
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Observe whether the current fragment changes via ViewModel and find what is the current
        // fragment attached in order to separately do actions according to the fragment.
        fragmentModel.getCurrentFragment().observe(getViewLifecycleOwner(), fragment -> {
            currentFragment = fragment;
            // Query the recent data as the type of LiveData using Room(query on worker thread)
            if(currentFragment instanceof GasManagerFragment) {
                log.i("current fragment invoked");
                mDB.gasManagerModel().loadRecentGasData().observe(getViewLifecycleOwner(), data -> {
                    gasDataList = data;
                    lastInfo = (data.size() > numPage) ? displayLastInfo(numPage) : getString(R.string.toast_expense_no_data);
                    tvLastInfo.setText(lastInfo);
                    tvPage.setText(String.valueOf(Math.abs(numPage) + 1));
                });

            } else if(currentFragment instanceof ServiceManagerFragment) {
                log.i("current fragment invoked");
                mDB.serviceManagerModel().loadRecentServiceData().observe(getViewLifecycleOwner(), data -> {
                    serviceList = data;
                    lastInfo = (data.size() > numPage) ? displayLastInfo(numPage) : getString(R.string.toast_expense_no_data);
                    tvLastInfo.setText(lastInfo);
                    tvPage.setText(String.valueOf(Math.abs(numPage) + 1));
                });

            }
        });

    }

    //Display the last 5 info retrieved from SQLite DB in the ViewPager with 5 fragments
    @SuppressWarnings("ConstantConditions")
    private String displayLastInfo(int pos) {
        // The latest mileage should be retrieved from SharedPreferernces. Otherwise, the last mileage
        // be retrieved from DB because the mileage value column in GasManagerTable and
        String format = getContext().getResources().getString(R.string.date_format_1);
        String won = getString(R.string.unit_won);
        String liter = getString(R.string.unit_liter);

        if(currentFragment instanceof GasManagerFragment) {
            String date = BaseActivity.formatMilliseconds(format, gasDataList.get(pos).dateTime);
            String a = String.format("%-10s%s%s", getString(R.string.gas_label_date), date, "\n");
            String b = String.format("%-10s%s%s%s", getString(R.string.exp_label_odometer), df.format(gasDataList.get(pos).mileage), "km", "\n");
            String c = String.format("%-12s%s%s", getString(R.string.gas_label_station), gasDataList.get(pos).stnName, "\n");
            String d = String.format("%-12s%s%s%s", getString(R.string.gas_label_expense), df.format(gasDataList.get(pos).gasPayment), won, "\n");
            String e = String.format("%-12s%s%s", getString(R.string.gas_label_amount),df.format(gasDataList.get(pos).gasAmount), liter);
            return a + b + c + d + e;

        } else if(currentFragment instanceof ServiceManagerFragment) {
            String date = BaseActivity.formatMilliseconds(format, serviceList.get(pos).dateTime);
            String a = String.format("%-8s%s%s", getString(R.string.svc_label_date), date,"\n");
            String b = String.format("%-8s%s%1s%s", getString(R.string.exp_label_odometer), df.format(serviceList.get(pos).mileage), "km", "\n");
            String c = String.format("%-8s%s%s", getString(R.string.svc_label_provider), serviceList.get(pos).svcName, "\n");
            String d = String.format("%-8s%s%1s%s", getString(R.string.svc_label_payment), df.format(serviceList.get(pos).totalExpense), won, "\n");

            return a + b + c + d;

        }

        return null;
    }

}
