package com.silverback.carman2.backgrounds;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

public class FirestoreQueryWorker extends Worker {

    private static final LoggingHelper log = LoggingHelperFactory.create(FirestoreQueryWorker.class);

    private Context context;

    public FirestoreQueryWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        // Deprecated as of Android 10(API 29)
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        Data outputData = new Data.Builder().putBoolean("network", isConnected).build();
        return Result.success(outputData);
    }
}
