package com.silverback.carman.threads;

import static com.silverback.carman.threads.StationInfoRunnable.RetrofitApi.BASE_URL;

import android.os.Process;
import android.util.SparseBooleanArray;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

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
   private List<StationGasRunnable.Item> mStationList;
   private List<Info> stationInfoList;
   private int index = 0;



   public interface StationInfoMethods {
      void setStationTaskThread(Thread thread);
      List<StationGasRunnable.Item> getNearStationList();
      void setStationInfoList(List<Info> infoList);
   }

   public StationInfoRunnable(StationInfoMethods callback) {
      this.callback = callback;
      stationInfoList = new ArrayList<>();
   }

   @Override
   public void run() {
      callback.setStationTaskThread(Thread.currentThread());
      android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

      mStationList = callback.getNearStationList();
      for(int i = 0; i < mStationList.size(); i++) {

         Call<StationInfoModel> call = RetrofitClient.getIntance()
                 .getRetrofitApi()
                 .getStationInfoModel("F186170711", mStationList.get(i).getStnId(), "json");


         call.enqueue(new Callback<StationInfoModel>() {
            @Override
            public void onResponse(@NonNull Call<StationInfoModel> call,
                                   @NonNull Response<StationInfoModel> response) {

               StationInfoModel model = response.body();
               assert model != null;
               //log.i("model: %s", model.result);
               //List<Info> detailList = model.result.detail;
               stationInfoList.add(model.result.info.get(0));
            }

            @Override
            public void onFailure(@NonNull Call<StationInfoModel> call,
                                  @NonNull Throwable t) {
               log.i("call failed: %s", t);
            }
         });

      }

      callback.setStationInfoList(stationInfoList);

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
      @SerializedName("CAR_WASH_YN")
      private String carWashYN;

      public String getCarWashYN() {
         return carWashYN;
      }
   }


}
