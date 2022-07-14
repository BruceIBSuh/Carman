package com.silverback.carman.threads;

import static com.silverback.carman.threads.StationGasTask.DOWNLOAD_STATION_INFO;
import static com.silverback.carman.threads.StationGasTask.TASK_FAILED;
import static com.silverback.carman.threads.StationInfoRunnable.RetrofitApi.BASE_URL;
import static com.silverback.carman.threads.ThreadTask.TASK_FAIL;

import android.os.Process;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class StationInfoRunnable implements Runnable {

   // Constants
   private static final LoggingHelper log = LoggingHelperFactory.create(StationInfoRunnable.class);

   private final StationInfoMethods callback;
   private final StationGasRunnable.Item gasStation;
   private final int index;



   public interface StationInfoMethods {
      void setStationTaskThread(Thread thread);
      void setStationInfo(int index, Info info);
      void handleTaskState(int state);

   }

   public StationInfoRunnable(int index, StationGasRunnable.Item station, StationInfoMethods callback) {
      this.callback = callback;
      this.index = index;
      this.gasStation = station;
   }

   @Override
   public void run() {
      log.i("station info thread: %s", Thread.currentThread());
      callback.setStationTaskThread(Thread.currentThread());
      android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

      try {
         if (Thread.interrupted()) throw new InterruptedException();
         //mStationList = callback.getNearStationList();
         log.i("index: %s", index);
         //for (int i = 0; i < mStationList.size(); i++) {
            //index = i;
            Call<StationInfoModel> call = RetrofitClient.getIntance()
                    .getRetrofitApi()
                    .getStationInfoModel("F186170711", gasStation.getStnId(), "json");
            call.enqueue(new Callback<StationInfoModel>() {
               @Override
               public void onResponse(@NonNull Call<StationInfoModel> call,
                                      @NonNull Response<StationInfoModel> response) {
                  StationInfoModel model = response.body();
                  assert model != null;
                  Info info = model.result.info.get(0);
                  callback.setStationInfo(index, info);

                  log.i("station info: %s ,%s, %s, %s, %s:", index, info.stnName, info.carWashYN, info.cvsYN, info.maintYN);

                  gasStation.setAddrsNew(info.addrsNew);
                  gasStation.setAddrsOld(info.addrsOld);
                  gasStation.setIsCarWash(Objects.equals(info.carWashYN, "Y"));
                  gasStation.setIsCVS(Objects.equals(info.cvsYN, "Y"));
                  gasStation.setIsService(Objects.equals(info.maintYN, "Y"));

                  callback.handleTaskState(DOWNLOAD_STATION_INFO);
               }

               @Override
               public void onFailure(@NonNull Call<StationInfoModel> call, @NonNull Throwable t) {
                  log.i("call failed: %s", t);
                  callback.handleTaskState(TASK_FAILED);
               }
            });
         //}

      } catch (InterruptedException e) { e.getLocalizedMessage(); }
   }

   public interface RetrofitApi {
      String BASE_URL = "https://www.opinet.co.kr/api/";
      @GET("detailById.do")
      Call<StationInfoModel> getStationInfoModel (
              @Query("code") String code,
              @Query("id") String id,
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
                 //.addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                 //.addConverterFactory(TikXmlConverterFactory.create(new TikXml.Builder().exceptionOnUnreadXml(false).build()))
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

   public static class StationInfoModel {
      @SerializedName("RESULT")
      @Expose
      public Result result;
   }

   public static class Result {
      @SerializedName("OIL")
      @Expose
      public List<Info> info;

   }

   public static class Info {
      @SerializedName("OS_NM")
      public String stnName;
      @SerializedName("VAN_ADR")
      @Expose
      public String addrsOld;

      @SerializedName("NEW_ADR")
      @Expose
      public String addrsNew;

      @SerializedName("CAR_WASH_YN")
      public String carWashYN;

      @SerializedName("CVS_YN")
      public String cvsYN;

      @SerializedName("MAINT_YN")
      public String maintYN;
   }


}
