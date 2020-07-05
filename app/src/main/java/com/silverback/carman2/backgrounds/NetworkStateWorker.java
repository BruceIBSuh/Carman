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

public class NetworkStateWorker extends Worker {

    private static final LoggingHelper log = LoggingHelperFactory.create(NetworkStateWorker.class);

    private Context context;

    public NetworkStateWorker(@NonNull Context context, @NonNull WorkerParameters params) {
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
        if(isConnected) {
            Data outputData = new Data.Builder().putBoolean("network", isConnected).build();
            return Result.success(outputData);
        } else return Result.failure();
    }
}
