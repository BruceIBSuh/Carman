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
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.database.DataProviderContract;
import com.silverback.carman2.models.FragmentSharedModel;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import java.text.DecimalFormat;

public class ExpensePagerFragment extends Fragment {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(ExpensePagerFragment.class);

    // Constants
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

    private static DecimalFormat df = BaseActivity.getDecimalFormatInstance();


    // Objects
    private FragmentSharedModel viewModel;
    private Cursor cursor;

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
    public void onActivityCreated(Bundle bundle){
        super.onActivityCreated(bundle);
        //getActivity().getSupportLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        if(getActivity() == null) return;

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

            if(currentFragment instanceof GasManagerFragment) {
                projection = gasColumns;
                baseUri = DataProviderContract.GAS_TABLE_URI;

            } else if(currentFragment instanceof ServiceFragment) {
                projection = serviceColumns;
                baseUri = DataProviderContract.SERVICE_TABLE_URI;
            }

            cursor = getActivity().getContentResolver().query(baseUri, projection, null, null, null);

            // Retrieve the last data to get the current mileage passing over to GasManagerActivity
            // using the callback method.
            if(cursor.moveToLast()) {
                displayLastInfo(cursor);
                log.i("Last Info: %s", lastInfo);
            }

            //String tableName = getArguments().getString("table");
            int mPageNumber = (getArguments().getInt("page")) * (-1); //Set minus for moving backword
            if(!cursor.move(mPageNumber)) { //lastInfo = getResources().getString("no data in db");
                lastInfo = "no data in db";
            } else {
                // Make string last info retrieved from DB using displayLastInfo method which, in part,
                // converts numbers to decimal format with comma.
                try {
                    lastInfo = displayLastInfo(cursor);
                } catch (Exception e) {
                    lastInfo = "no_data_in db";//getResources().getString(R.string.err_viewpager_no_data);
                }
            }

            if(cursor != null) cursor.close();

            tvLastInfo.setText(lastInfo);
            tvPage.setText(String.valueOf(Math.abs(mPageNumber) + 1));

        });


        return localView;
    }



    //Display the last 5 info retrieved from SQLite DB in the ViewPager with 5 fragments
    @SuppressWarnings("ConstantConditions")
    private String displayLastInfo(Cursor cursor) {

        // The latest mileage should be retrieved from SharedPreferernces. Otherwise, the last mileage
        // be retrieved from DB because the mileage value column in GasManagerTable and
        String format = getContext().getResources().getString(R.string.date_format_1);
        String date = BaseActivity.formatMilliseconds(format, cursor.getLong(0));
        String won = getString(R.string.unit_won);
        String liter = getString(R.string.unit_liter);


        if(currentFragment instanceof GasManagerFragment) {
            String a = String.format("%-10s%s%s", getString(R.string.gas_label_date), date, "\n");
            String b = String.format("%-10s%s%s%s", getString(R.string.exp_label_odometer), df.format(cursor.getInt(1)), "km", "\n");
            String c = String.format("%-12s%s%s", getString(R.string.gas_label_station), cursor.getString(2), "\n");
            String d = String.format("%-12s%s%s%s", getString(R.string.gas_label_expense), df.format(cursor.getInt(3)), won, "\n");
            String e = String.format("%-12s%s%s", getString(R.string.gas_label_amount),df.format(cursor.getInt(4)), liter);
            return a + b + c + d + e;

        } else if(currentFragment instanceof ServiceFragment) {
            String a = String.format("%-8s%s%s", getString(R.string.svc_label_date), date,"\n");
            String b = String.format("%-8s%s%1s%s", getString(R.string.exp_label_odometer), df.format(cursor.getInt(1)), "km", "\n");
            String c = String.format("%-8s%s%s", getString(R.string.svc_label_provider), cursor.getString(2), "\n");
            String d = String.format("%-8s%s%1s%s", getString(R.string.svc_label_period), df.format(cursor.getInt(3)), won, "\n");

            return a + b + c + d;

        }

        return null;
    }

}
