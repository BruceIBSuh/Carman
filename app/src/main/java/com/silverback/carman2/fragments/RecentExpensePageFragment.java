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

import java.text.DecimalFormat;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

public class RecentExpensePageFragment extends Fragment {

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

    // Object References
    //private static DecimalFormat df = BaseActivity.getDecimalFormatInstance();

    // Fields
    private String lastInfo;
    private String[] projection;
    private Uri baseUri;

    // Define Interface to pass over the current mileage to GasManagerActivity
    /*
    public interface OnLastMileageInfoFragmentListener {
        void getLastMileageInfo(int arg);
    }
    */

    public RecentExpensePageFragment(){
        // Default Constructor. Leave this empty!
    }

    // Construct ViewPager fragment as static used in ViewPager Adapter
    public static RecentExpensePageFragment create(int pageNumber) {
        //Instantiate SharedPreferences for getting tableName set as default
        RecentExpensePageFragment fragment = new RecentExpensePageFragment();
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

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        //String tableName = getArguments().getString("table");
        int mPageNumber = (getArguments().getInt("page")) * (-1); //Set minus for moving backword

        /*
        if(getActivity() instanceof GasManagerActivity) {
            projection = gasColumns;
            baseUri = DataProviderContract.GAS_TABLE_URI;
        } else if(getActivity() instanceof ServiceManagerActivity) {
            projection = serviceColumns;
            baseUri = DataProviderContract.SERVICE_TABLE_URI;
        }
        */

        /*
        Cursor cursor = getActivity().getContentResolver().query(baseUri, projection, null, null, null);

        // Retrieve the last data to get the current mileage passing over to GasManagerActivity
        // using the callback method.
        if(cursor.moveToLast()) {
            // Pass the last mileage data over to the parent activity by calling callback method.
            //lastInfo = displayLastInfo(cursor);
        }


        if(!cursor.move(mPageNumber)) lastInfo = getResources().getString(R.string.err_viewpager_no_data);
        else {
            // Make string last info retrieved from DB using displayLastInfo method which, in part,
            // converts numbers to decimal format with comma.
            try {
                //lastInfo = displayLastInfo(cursor);
            } catch (Exception e) {
                //lastInfo = getResources().getString(R.string.err_viewpager_no_data);
            }
        }


        if(cursor != null) cursor.close();

        */

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        //ViewGroup rootView = (ViewGroup)inflater.inflate(R.layout.fragment_viewpager, container, false);
        View localView = inflater.inflate(R.layout.fragment_viewpager, container, false);
        TextView tvLastInfo = localView.findViewById(R.id.tv_last_info);
        tvLastInfo.setText(lastInfo);

        return localView;
    }


    /*
    //Display the last 5 info retrieved from SQLite DB in the ViewPager with 5 fragments
    private String displayLastInfo(Cursor cursor) {

        // The latest mileage should be retrieved from SharedPreferernces. Otherwise, the last mileage
        // be retrieved from DB because the mileage value column in GasManagerTable and
        String format = getContext().getResources().getString(R.string.date_format_1);
        String date = BaseActivity.formatMilliseconds(format, cursor.getLong(0));
        String won = getString(R.string.currency_won);
        String liter = getString(R.string.unit_liter);


        if(getActivity() instanceof GasManagerActivity) {
            String a = String.format("%-8s%s%1s", getString(R.string.expense_gas_date), date, "\n");
            String b = String.format("%-8s%s%1s%s", getString(R.string.expense_ordometer), df.format(cursor.getInt(1)), "km", "\n");
            String c = String.format("%-8s%s%s", getString(R.string.expense_gas_station), cursor.getString(2), "\n");
            String d = String.format("%-8s%s%1s%s", getString(R.string.expense_gas_price), df.format(cursor.getInt(3)), won, "\n");
            String e = String.format("%-11s%s%1s", getString(R.string.expense_gas_amount),df.format(cursor.getInt(4)), liter);
            return a + b + c + d + e;
        } else if(getActivity() instanceof ServiceManagerActivity) {
            String a = String.format("%-8s%s%s", getString(R.string.expense_service_date), date,"\n");
            String b = String.format("%-8s%s%1s%s", getString(R.string.expense_ordometer), df.format(cursor.getInt(1)), "km", "\n");
            String c = String.format("%-8s%s%s", getString(R.string.expense_service_provider), cursor.getString(2), "\n");
            String d = String.format("%-8s%s%1s%s", getString(R.string.expense_service_price), df.format(cursor.getInt(3)), won, "\n");

            return a + b + c + d;
        }

        return null;
    }
    */
}
