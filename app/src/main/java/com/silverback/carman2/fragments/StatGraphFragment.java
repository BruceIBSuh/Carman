package com.silverback.carman2.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.silverback.carman2.R;
import com.silverback.carman2.database.ExpenseBaseEntity;
import com.silverback.carman2.database.CarmanDatabase;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.views.StatGraphView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class StatGraphFragment extends Fragment implements View.OnClickListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(StatGraphFragment.class);

    // Object References
    private CarmanDatabase mDB;
    private Calendar calendar;
    private SimpleDateFormat sdf;
    private int currentYear;

    // GraphView
    private static StatGraphView graph;
    //private static BarGraphSeries<DataPoint> series;
    //private static DataPoint[] dataPoint;
    //private static int[] monthlyTotalExpense;

    // UI's
    private TextView tvYear;



    // Constructor
    public StatGraphFragment() {
        // Required empty public constructor
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle bundle) {

        super.onCreate(bundle);

        mDB = CarmanDatabase.getDatabaseInstance(getActivity().getApplicationContext());
        calendar = Calendar.getInstance();
        sdf = new SimpleDateFormat("MM", Locale.getDefault());

        //dataPoint = new DataPoint[12];
        //series = new BarGraphSeries<>(dataPoint);
        //monthlyTotalExpense = new int[12];

        currentYear = calendar.get(Calendar.YEAR);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_stat_graph, container, false);

        // GraphView
        graph = view.findViewById(R.id.graphView);
        //graph.getViewport().setXAxisBoundsManual(true);
        //graph.getViewport().setMaxX(12);
        //series.setDrawValuesOnTop(true);

        // UI's
        tvYear = view.findViewById(R.id.tv_year);
        tvYear.setText(String.valueOf(calendar.get(Calendar.YEAR)));

        // ImageButton
        Button leftArrow = view.findViewById(R.id.btn_arrow_left);
        Button rightArrow = view.findViewById(R.id.btn_arrow_right);
        leftArrow.setOnClickListener(this);
        rightArrow.setOnClickListener(this);

        loadTotalExpense(currentYear);

        // Creates an instance of AsyncTaskk with this year given as params for doInBackground.
        //new StatGraphViewTask(mDB, graph, calendar).execute(calendar.get(Calendar.YEAR));


        return view;
    }


    @Override
    public void onClick(View view) {

        switch(view.getId()) {

            case R.id.btn_arrow_left:
                calendar.add(Calendar.YEAR, -1);
                tvYear.setText(String.valueOf(calendar.get(Calendar.YEAR)));

                break;

            case R.id.btn_arrow_right:
                if(calendar.get(Calendar.YEAR) < currentYear) {
                    calendar.add(Calendar.YEAR, 1);
                    tvYear.setText(String.valueOf(calendar.get(Calendar.YEAR)));
                }

                break;

        }

        loadTotalExpense(calendar.get(Calendar.YEAR));
        //new StatGraphViewTask(mDB, graph, calendar).execute(calendar.get(Calendar.YEAR));
    }


    private void loadTotalExpense(int year) {
        calendar.set(year, 0, 1, 0, 0);
        long start = calendar.getTimeInMillis();
        calendar.set(year, 11, 31, 23, 59, 59);
        long end = calendar.getTimeInMillis();

        mDB.expenseBaseMdoel().loadExpenseLiveData(start, end).observe(this, entities -> {
            //monthlyTotalExpense = calculateMonthlyExpense(entities);
            graph.setGraphData(calculateMonthlyExpense(entities));
        });
    }

    /*
    private static class StatGraphViewTask extends AsyncTask<Integer, Void, Cursor> {

        // Create WeakReference to prevent the outer class reference from memory leaking.
        //WeakReference<Activity> weakActivity;
        WeakReference<CarmanDatabase> weakDB;
        WeakReference<StatGraphView> weakGraphView;
        WeakReference<Calendar> weakCalendar;

        // Constructor
        StatGraphViewTask(CarmanDatabase db, StatGraphView graphView, Calendar calendar) {

            weakGraphView = new WeakReference<>(graphView);
            weakCalendar = new WeakReference<>(calendar);
            monthlyTotalExpense = new int[12];

        }

        @Override
        protected Cursor doInBackground(Integer... params) {

            // Array to set conditions to ?(wildcard) of WHERE condition clause to fetch the expense
            // data in each year.
            String[] conds = new String[4];

            // Set the Calendar to the Frist day of a given year(passed from param), then convert it
            // to Miliiseconds to fetch data from the tables to match the column type(long milliseconds)
            weakCalendar.get().set(params[0], 0, 1, 0, 0, 0); //set(year, month, date, hourOfDay, minute)
            //Log.d(TAG, "Calendar: "+ calendar);
            //conds[0] = String.valueOf(calendar.getTimeInMillis()); //First date of year for gasTable
            //conds[2] = String.valueOf(calendar.getTimeInMillis()); //First date of year for serviceTable
            long start = weakCalendar.get().getTimeInMillis();

            // Set the Calendar tO the last day of a given year.
            weakCalendar.get().set(params[0], 11, 31, 23, 59, 59);
            //conds[1] = String.valueOf(calendar.getTimeInMillis()); //Last date of year to gasTable
            //conds[3] = String.valueOf(calendar.getTimeInMillis()); //Last date of year to serviceTable
            long end = weakCalendar.get().getTimeInMillis();


            String graphDataSql = gasData + " UNION " + serviceData
                    + " ORDER BY " + DataProviderContract.DATE_TIME_COLUMN + " DESC ";




            // conds: First day and last day of each year represented by milliseconds to fetch the
            // expenses during the year.
            return mDB.rawQuery(graphDataSql, conds);

        }

        @Override
        protected void onPostExecute(Cursor cursor) {

            monthlyTotalExpense = calculateMonthlyExpense(cursor);
            weakGraphView.get().setGraphData(monthlyTotalExpense);

            if(weakActivity != null) {
                weakActivity.clear();
                weakActivity = null;
            }



            if(weakGraphView != null) {
                weakGraphView.clear();
                weakGraphView = null;
            }

        }

    }
    */



    private int[] calculateMonthlyExpense(List<ExpenseBaseEntity> entityList) {

        int[] monthlyTotalExpense = new int[12];

        for (ExpenseBaseEntity entity : entityList) {
            int month = Integer.valueOf(sdf.format(entity.dateTime));
            log.i("Month: %s", month);
            switch (month) {
                case 1: monthlyTotalExpense[0] += entity.totalExpense; break;
                case 2: monthlyTotalExpense[1] += entity.totalExpense; break;
                case 3: monthlyTotalExpense[2] += entity.totalExpense; break;
                case 4: monthlyTotalExpense[3] += entity.totalExpense; break;
                case 5: monthlyTotalExpense[4] += entity.totalExpense; break;
                case 6: monthlyTotalExpense[5] += entity.totalExpense; break;
                case 7: monthlyTotalExpense[6] += entity.totalExpense; break;
                case 8: monthlyTotalExpense[7] += entity.totalExpense; break;
                case 9: monthlyTotalExpense[8] += entity.totalExpense; break;
                case 10: monthlyTotalExpense[9] += entity.totalExpense; break;
                case 11: monthlyTotalExpense[10] += entity.totalExpense; break;
                case 12: monthlyTotalExpense[11] += entity.totalExpense; break;
                default: break;
            }
        }

        return monthlyTotalExpense;
    }
}
