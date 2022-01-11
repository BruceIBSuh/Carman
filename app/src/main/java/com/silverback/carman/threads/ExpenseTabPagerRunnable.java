package com.silverback.carman.threads;

import android.content.Context;
import android.os.Bundle;
import android.os.Process;

import androidx.fragment.app.FragmentManager;

import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.Constants;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class ExpenseTabPagerRunnable implements Runnable {

    private static final LoggingHelper log = LoggingHelperFactory.create(ExpenseTabPagerRunnable.class);

    // Objects
    private final Context context;
    private final TabPagerMethods task;

    public interface TabPagerMethods {
        void setTabPagerTaskThread(Thread thread);
        //void setTabPagerAdapter(ExpTabPagerAdapter adapter);
        String[] getDefaults();
        String getJsonDistrict();
        FragmentManager getFragmentManager();
    }

    // Constructor
    public ExpenseTabPagerRunnable(Context context, TabPagerMethods task) {
        this.context = context;
        this.task = task;
    }

    @Override
    public void run() {
        task.setTabPagerTaskThread(Thread.currentThread());
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

        try (FileInputStream fis = context.openFileInput("userId");
             BufferedReader br = new BufferedReader(new InputStreamReader(fis))) {
            // Retrieve the user id saved in the internal stoage.
            final String userId = br.readLine();

            // Create the adapter
            //ExpTabPagerAdapter pagerAdapter = new ExpTabPagerAdapter(task.getFragmentManager(), context.get);

            // Set args to ExpenseGasFragment
            task.getDefaults()[1] = Constants.MIN_RADIUS;
            Bundle gasArgs = new Bundle();
            gasArgs.putStringArray("defaultParams", task.getDefaults());
            gasArgs.putString("userId", userId);
            //pagerAdapter.getItem(0).setArguments(gasArgs);

            // Set args to ExpenseServiceFragment
            JSONArray jsonArray = new JSONArray(task.getJsonDistrict());
            String distCode = (String)jsonArray.get(2);
            Bundle svcArgs = new Bundle();
            svcArgs.putString("distCode", distCode);
            svcArgs.putString("userId", userId);
            //pagerAdapter.getItem(1).setArguments(svcArgs);

            //task.setTabPagerAdapter(pagerAdapter);

        } catch(IOException e) {
            log.e("IOException: %s", e.getMessage());
        } catch(JSONException e) {
            log.e("JSONException: %s", e.getMessage());
        }

    }
}
