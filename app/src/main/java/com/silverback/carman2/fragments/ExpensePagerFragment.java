package com.silverback.carman2.fragments;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.silverback.carman2.BaseActivity;
import com.silverback.carman2.R;
import com.silverback.carman2.database.CarmanDatabase;
import com.silverback.carman2.database.GasManagerDao;
import com.silverback.carman2.database.GasManagerEntity;
import com.silverback.carman2.database.ServiceManagerDao;
import com.silverback.carman2.database.ServiceManagerEntity;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.FragmentSharedModel;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProviders;

import java.text.DecimalFormat;
import java.util.List;

public class ExpensePagerFragment extends Fragment {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(ExpensePagerFragment.class);
    private final static DecimalFormat df = BaseActivity.getDecimalFormatInstance();

    // Objects
    private CarmanDatabase mDB;
    private FragmentSharedModel fragmentSharedModel;
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

    public ExpensePagerFragment(){
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

        // Instantiate CarmanDatabase as a type of singleton instance
        mDB = CarmanDatabase.getDatabaseInstance(getActivity().getApplicationContext());

        // Create ViewModel to get data of which fragment is attached in the tab-linked ViewPager
        // from the viewpager-containing fragments.
        fragmentSharedModel = ViewModelProviders.of(getActivity()).get(FragmentSharedModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        //ViewGroup rootView = (ViewGroup)inflater.inflate(R.layout.fragment_pager_expense, container, false);
        View localView = inflater.inflate(R.layout.fragment_pager_expense, container, false);
        tvLastInfo = localView.findViewById(R.id.tv_lastInfo);
        tvPage = localView.findViewById(R.id.tv_page);

        // Observe whether the current fragment changes via ViewModel and find what is the current
        // fragment attached in order to separately do actions according to the fragment.
        fragmentSharedModel.getCurrentFragment().observe(this, fragment -> {

            currentFragment = fragment;
            log.i("Current Fragment in ExpenseViewPager: %s, %s", currentFragment, numPage);
            if(getArguments() != null) numPage = (getArguments().getInt("page"));

            // Query the recent data as the type of LiveData using Room(query on worker thread)
            if(currentFragment instanceof GasManagerFragment) {
                mDB.gasManagerModel().loadRecentGasData().observe(this, data -> {
                    gasDataList = data;
                    lastInfo = (data.size() > numPage)? displayLastInfo(numPage) : null;
                    tvLastInfo.setText(lastInfo);
                    tvPage.setText(String.valueOf(Math.abs(numPage) + 1));
                });

            }else if(currentFragment instanceof ServiceManagerFragment) {
                mDB.serviceManagerModel().loadRecentServiceData().observe(this, data -> {
                    serviceList = data;
                    lastInfo = (data.size() > numPage)?displayLastInfo(numPage) : "";
                    tvLastInfo.setText(lastInfo);
                    tvPage.setText(String.valueOf(Math.abs(numPage) + 1));
                });

            }


        });

        return localView;
    }

    @Override
    public void onResume() {
        super.onResume();
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
