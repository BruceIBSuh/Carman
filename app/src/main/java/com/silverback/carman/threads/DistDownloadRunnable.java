package com.silverback.carman.threads;

import static com.silverback.carman.threads.DistDownloadRunnable.RetrofitApi.BASE_URL;

import android.content.Context;
import android.os.Process;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.silverback.carman.R;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.Constants;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

/*
 * This class is to get the Sigun codes based on the Sido code defined in the string-array from the
 * Opinet. Once downloading the Sigun codes completes, it will be saved in the internal storage.
 */
public class DistDownloadRunnable implements Runnable {

    // Logging
    private final LoggingHelper log = LoggingHelperFactory.create(DistDownloadRunnable.class);

    static final int TASK_COMPLETE = 1;
    static final int TASK_FAIL = -1;


    // Objects
    private final Context context;
    private final OpinetDistCodeMethods mTask;

    private int index;

    // Interface
    public interface OpinetDistCodeMethods {
        void setDistCodeDownloadThread(Thread currentThread);
        void hasDistCodeSaved(boolean b);
        void handleDistCodeTask(int state);
    }

    // Constructor
    DistDownloadRunnable(Context context, OpinetDistCodeMethods task) {
        this.context = context;
        mTask = task;

    }

    @Override
    public void run() {
        mTask.setDistCodeDownloadThread(Thread.currentThread());
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

        final String[] sido = context.getResources().getStringArray(R.array.sido_code);
        List<Area> areaList = new ArrayList<>();
        index = 0;
        for(String sidocode : sido) {
            Call<AreaCodeModel> call = RetrofitClient.getIntance()
                    .getRetrofitApi()
                    .getAreaCodeModel("F186170711", sidocode, "json");

            call.enqueue(new Callback<AreaCodeModel>() {
                @Override
                public void onResponse(@NonNull Call<AreaCodeModel> call,
                                       @NonNull Response<AreaCodeModel> response) {
                   AreaCodeModel model = response.body();
                   assert model != null;
                   areaList.addAll(model.result.areaList);
                   if(index == sido.length - 1) {
                       boolean isSaved = saveDistCode(areaList);
                       mTask.hasDistCodeSaved(isSaved);
                   }
                   index++;
                }

                @Override
                public void onFailure(@NonNull Call<AreaCodeModel> call, @NonNull Throwable t) {
                    // Exception handling required.
                }
            });
        }


    }

    // Retrofit API.
    public interface RetrofitApi {
        // Constants. API 29 and higher requires http to be https.
        String BASE_URL = "https://www.opinet.co.kr/api/";
        @GET("areaCode.do")
        Call<AreaCodeModel> getAreaCodeModel (
                @Query("code") String code,
                @Query("area") String area,
                @Query("out") String out
        );

    }

    private static class RetrofitClient {
        private final RetrofitApi retrofitApi;
        private RetrofitClient() {
            Gson gson = new GsonBuilder().setLenient().create(); //make it less strict
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();

            retrofitApi = retrofit.create(RetrofitApi.class);
        }
        // Bill-Pugh Singleton instance
        private static class LazyHolder {
            private static final RetrofitClient sInstance = new RetrofitClient();
        }
        public static RetrofitClient getIntance() {
            return RetrofitClient.LazyHolder.sInstance;
        }
        public RetrofitApi getRetrofitApi() {
            return retrofitApi;
        }
    }

    private static class AreaCodeModel {
        @SerializedName("RESULT")
        public Result result;
    }

    private static class Result implements Serializable {
        @SerializedName("OIL")
        public List<Area> areaList;
    }

    public static class Area implements Serializable {
        @SerializedName("AREA_CD")
        public String areaCd;
        @SerializedName("AREA_NM")
        public String areaName;

        public String getAreaCd() {
            return areaCd;
        }
        public String getAreaName() {
            return areaName;
        }
    }
    // Method to save the district code downloaded from the Opinet in the internal storage
    private boolean saveDistCode(List<Area> list) {
        File file = new File(context.getFilesDir(), Constants.FILE_DISTRICT_CODE);
        try(FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(list);
            return true;
        } catch (IOException e) { e.printStackTrace();}

        return false;
    }
}
