package com.silverback.carman2.fragments;

import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.silverback.carman2.R;
import com.silverback.carman2.models.CarmanSQLiteOpenHelper;
import com.silverback.carman2.models.DataProviderContract;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.BarGraphSeries;
import com.jjoe64.graphview.series.DataPoint;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class StatGraphFragment extends Fragment implements View.OnClickListener {

    private static final String gasData = "SELECT " + DataProviderContract.GAS_ID + ", "
            + DataProviderContract.DATE_TIME_COLUMN + ", "
            + DataProviderContract.GAS_PAYMENT_COLUMN + " "
            + "FROM " + DataProviderContract.GAS_TABLE_NAME + " "
            + "WHERE " + DataProviderContract.DATE_TIME_COLUMN + " "
            + "BETWEEN ? AND ?";
    private static final String serviceData = "SELECT " + DataProviderContract.SERVICE_ID + ", "
            + DataProviderContract.DATE_TIME_COLUMN + ", "
            + DataProviderContract.SERVICE_TOTAL_PRICE_COLUMN + " "
            + "FROM " + DataProviderContract.SERVICE_TABLE_NAME + " "
            + "WHERE " + DataProviderContract.DATE_TIME_COLUMN + " "
            + "BETWEEN ? AND ?";


    // Object References
    private static SQLiteDatabase mDB;
    private static Calendar calendar;
    private static SimpleDateFormat sdf;
    private int currentYear;

    // GraphView
    private static GraphView graph;
    private static BarGraphSeries<DataPoint> series;
    private static DataPoint[] dataPoint;

    // UI's
    private TextView tvYear;

    // Initializes static objects
    static {
        calendar = Calendar.getInstance();
        sdf = new SimpleDateFormat("MM", Locale.getDefault());
        dataPoint = new DataPoint[12];
        series = new BarGraphSeries<>(dataPoint);
    }


    // Constructor
    public StatGraphFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle bundle) {

        super.onCreate(bundle);
        mDB = CarmanSQLiteOpenHelper.getInstance(getActivity()).getReadableDatabase();

        currentYear = calendar.get(Calendar.YEAR);

        // Creates an instance of AsyncTaskk with this year given as params for doInBackground.
        new GraphTask(getActivity()).execute(calendar.get(Calendar.YEAR));

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.view_stat_graph, container, false);

        // GraphView
        /*
        graph = view.findViewById(R.id.graphView);
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMaxX(12);
        //series.setDrawValuesOnTop(true);

        // UI's
        tvYear = view.findViewById(R.id.tv_year);
        tvYear.setText(String.valueOf(calendar.get(Calendar.YEAR)));

        // ImageButton
        Button leftArrow = view.findViewById(R.id.btn_arrow_left);
        Button rightArrow = view.findViewById(R.id.btn_arrow_right);
        leftArrow.setOnClickListener(this);
        rightArrow.setOnClickListener(this);
        */
        return view;
    }


    @Override
    public void onClick(View view) {

        switch(view.getId()) {
            /*
            case R.id.btn_arrow_left:
                calendar.add(Calendar.YEAR, -1);
                tvYear.setText(String.valueOf(calendar.get(Calendar.YEAR)));
                new GraphTask(getActivity()).execute(calendar.get(Calendar.YEAR));

                break;

            case R.id.btn_arrow_right:
                if(calendar.get(Calendar.YEAR) < currentYear) {
                    calendar.add(Calendar.YEAR, 1);
                    tvYear.setText(String.valueOf(calendar.get(Calendar.YEAR)));
                    new GraphTask(getActivity()).execute(calendar.get(Calendar.YEAR));
                }
                break;
           */
        }
    }


    private static class GraphTask extends AsyncTask<Integer, Void, DataPoint[]> {

        WeakReference<Activity> weakActivityRef;
        int[] months;



        // Constructor
        GraphTask(Activity activity) {
            weakActivityRef = new WeakReference<>(activity);
            months = new int[12];
        }

        @Override
        protected DataPoint[] doInBackground(Integer... params) {

            // Array to set conditions to ?(wildcard) of WHERE condition clause to fetch the expense
            // data in each year.
            String[] conds = new String[4];

            calendar.set(params[0], 0, 1, 0, 0, 0);
            conds[0] = String.valueOf(calendar.getTimeInMillis()); // First date of year to gasTable
            conds[2] = String.valueOf(calendar.getTimeInMillis()); // First date of year to serviceTable

            calendar.set(params[0], 11, 31, 23, 59, 59);
            conds[1] = String.valueOf(calendar.getTimeInMillis()); // Last date of year to gasTable
            conds[3] = String.valueOf(calendar.getTimeInMillis()); // Last date of year to serviceTable

            String graphDataSql = gasData + " UNION " + serviceData
                    + " ORDER BY " + DataProviderContract.DATE_TIME_COLUMN + " DESC ";

            // conds: First day and last day of each year represented by milliseconds to fetch the
            // expenses during the year.
            Cursor cursor = mDB.rawQuery(graphDataSql, conds);
            return calculateMonthlyExpense(months, cursor);

        }

        @Override
        protected void onPostExecute(DataPoint[] data) {

            series.resetData(data);
            graph.addSeries(series);

            if(weakActivityRef != null) {
                weakActivityRef.clear();
                weakActivityRef = null;
            }

        }

    }

    private static DataPoint[] calculateMonthlyExpense(int[] months, Cursor cursor) throws SQLiteException {

        try {

            if(cursor.moveToFirst()) {
                // Looping while the rows belonging to the same month are sorted and sums up the
                // payment column
                do {
                    // Converts Milliseconds fetched from 'DateTime' into Integer Type using SimpleDateFormat,
                    // then applies it to each case statment.
                    int month = Integer.valueOf(sdf.format(cursor.getLong(1)));

                    switch(month) {
                        case 1: months[0] += cursor.getInt(2); break;
                        case 2: months[1] += cursor.getInt(2); break;
                        case 3: months[2] += cursor.getInt(2); break;
                        case 4: months[3] += cursor.getInt(2); break;
                        case 5: months[4] += cursor.getInt(2); break;
                        case 6: months[5] += cursor.getInt(2); break;
                        case 7: months[6] += cursor.getInt(2); break;
                        case 8: months[7] += cursor.getInt(2); break;
                        case 9: months[8] += cursor.getInt(2); break;
                        case 10: months[9] += cursor.getInt(2); break;
                        case 11: months[10] += cursor.getInt(2); break;
                        case 12: months[11] += cursor.getInt(2); break;
                        default: break;
                    }

                } while (cursor.moveToNext());
            }

        } finally {
            cursor.close();
        }

        // Creates DataPoint for each month with month and total expenses in that month and inserts
        // it int DataPoint[] array to pass it to Series class for graph display.
        for(int i = 0; i < months.length; i++){
            DataPoint v = new DataPoint(i, months[i]);
            dataPoint[i] = v;
        }

        return dataPoint;

    }
}
