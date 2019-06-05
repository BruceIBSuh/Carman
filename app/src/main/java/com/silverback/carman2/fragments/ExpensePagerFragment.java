package com.silverback.carman2.fragments;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.silverback.carman2.BaseActivity;
import com.silverback.carman2.R;
import com.silverback.carman2.database.CarmanDatabase;
import com.silverback.carman2.database.GasManager;
import com.silverback.carman2.database.ServiceManager;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.FragmentSharedModel;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import java.text.DecimalFormat;
import java.util.List;

public class ExpensePagerFragment extends Fragment {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(ExpensePagerFragment.class);

    // Constants
    /*
    private final String[] gasColumns = {
            DataProviderContract.DATE_TIME_COLUMN,
            DataProviderContract.MILEAGE_COLUMN,
            DataProviderContract.GAS_STATION_COLUMN,
            DataProviderContract.GAS_PAYMENT_COLUMN,
            DataProviderContract.GAS_AMOUNT_COLUMN
    };

    private final String[] serviceColumns = {
            DataProviderContract.DATE_TIME_COLUMN,
            DataProviderContract.MILEAGE_COLUMN,
            DataProviderContract.SERVICE_PROVIDER_COLUMN,
            DataProviderContract.SERVICE_TOTAL_PRICE_COLUMN
    };
    */

    private static DecimalFormat df = BaseActivity.getDecimalFormatInstance();


    // Objects
    private CarmanDatabase mDB;
    private GasManager gasManager;
    private ServiceManager serviceManager;
    private FragmentSharedModel viewModel;
    //private Cursor cursor;
    private List<GasManager> gasList;
    private List<ServiceManager> serviceList;

    // UIs
    private TextView tvDate, tvMileage, tvName, tvPayment, tvAmount;

    // Fields
    private Fragment currentFragment;
    private String lastInfo;
    private String[] projection;
    private Uri baseUri;

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
        viewModel = ViewModelProviders.of(getActivity()).get(FragmentSharedModel.class);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        //ViewGroup rootView = (ViewGroup)inflater.inflate(R.layout.fragment_viewpager_expense, container, false);
        View localView = inflater.inflate(R.layout.fragment_viewpager_expense, container, false);
        final TextView tvLastInfo = localView.findViewById(R.id.tv_lastInfo);
        final TextView tvPage = localView.findViewById(R.id.tv_page);

        viewModel.getCurrentFragment().observe(this, fragment -> {

            currentFragment = fragment;
            int mPageNumber = (getArguments().getInt("page"));

            if(currentFragment instanceof GasManagerFragment) {
                gasList = mDB.gasManagerModel().loadRecentGasData();
                lastInfo = (gasList.size() > mPageNumber)? displayLastInfo(mPageNumber) : null;

            } else if(currentFragment instanceof ServiceFragment) {
                serviceList = mDB.serviceManagerModel().loadRecentServiceData();
                lastInfo = (serviceList.size() > mPageNumber)? displayLastInfo(mPageNumber) : null;
            }

            tvLastInfo.setText(lastInfo);
            tvPage.setText(String.valueOf(Math.abs(mPageNumber) + 1));

        });


        return localView;
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
            String date = BaseActivity.formatMilliseconds(format, gasList.get(pos).dateTime);
            String a = String.format("%-10s%s%s", getString(R.string.gas_label_date), date, "\n");
            String b = String.format("%-10s%s%s%s", getString(R.string.exp_label_odometer), df.format(gasList.get(pos).mileage), "km", "\n");
            String c = String.format("%-12s%s%s", getString(R.string.gas_label_station), gasList.get(pos).stnName, "\n");
            String d = String.format("%-12s%s%s%s", getString(R.string.gas_label_expense), df.format(gasList.get(pos).gasPayment), won, "\n");
            String e = String.format("%-12s%s%s", getString(R.string.gas_label_amount),df.format(gasList.get(pos).gasAmount), liter);
            return a + b + c + d + e;

        } else if(currentFragment instanceof ServiceFragment) {
            String date = BaseActivity.formatMilliseconds(format, serviceList.get(pos).dateTime);
            String a = String.format("%-8s%s%s", getString(R.string.svc_label_date), date,"\n");
            String b = String.format("%-8s%s%1s%s", getString(R.string.exp_label_odometer), df.format(serviceList.get(pos).mileage), "km", "\n");
            String c = String.format("%-8s%s%s", getString(R.string.svc_label_provider), serviceList.get(pos).serviceCenter, "\n");
            String d = String.format("%-8s%s%1s%s", getString(R.string.svc_label_payment), df.format(serviceList.get(pos).totalPayment), won, "\n");

            return a + b + c + d;

        }

        return null;
    }

}
