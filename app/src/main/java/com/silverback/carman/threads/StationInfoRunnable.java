package com.silverback.carman.threads;

import static com.silverback.carman.threads.StationInfoRunnable.RetrofitApi.BASE_URL;

import android.os.Process;
import android.util.SparseArray;

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
   private List<StationGasRunnable.Item> mStationList;
   private List<Info> stationInfoList;
   private SparseArray<Info> sparseArray;
   private int count, index;



   public interface StationInfoMethods {
      List<StationGasRunnable.Item> getNearStationList();
      void setStationTaskThread(Thread thread);
      void setStationInfoArray(SparseArray<Info> sparseArray);
      void setStationInfoList(List<StationGasRunnable.Item> info);
   }

   public StationInfoRunnable(StationInfoMethods callback) {
      this.callback = callback;
      stationInfoList = new ArrayList<>();
      sparseArray = new SparseArray<>(1);

   }

   @Override
   public void run() {
      log.i("station info thread: %s", Thread.currentThread());
      callback.setStationTaskThread(Thread.currentThread());
      android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

      try {
         if (Thread.interrupted()) throw new InterruptedException();
         mStationList = callback.getNearStationList();
         for (int i = 0; i < mStationList.size(); i++) {
            index = i;
            Call<StationInfoModel> call = RetrofitClient.getIntance()
                    .getRetrofitApi()
                    .getStationInfoModel("F186170711", mStationList.get(i).getStnId(), "json");

            call.enqueue(new Callback<StationInfoModel>() {
               @Override
               public void onResponse(@NonNull Call<StationInfoModel> call,
                                      @NonNull Response<StationInfoModel> response) {
                  StationInfoModel model = response.body();
                  assert model != null;
                  Info info = model.result.info.get(0);
                  sparseArray.append(index, info);
                  callback.setStationInfoArray(sparseArray);
                  /*
                  mStationList.get(index).setAddrsNew(info.addrsNew);
                  mStationList.get(index).setAddrsOld(info.addrsOld);
                  mStationList.get(index).setIsCarWash(Objects.equals(info.carWashYN, "Y"));
                  mStationList.get(index).setIsCVS(Objects.equals(info.cvsYN, "Y"));
                  mStationList.get(index).setIsService(Objects.equals(info.maintYN, "Y"));

                   */
               }

               @Override
               public void onFailure(@NonNull Call<StationInfoModel> call, @NonNull Throwable t) {
                  log.i("call failed: %s", t);
               }
            });
         }

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
      private String stnName;
      @SerializedName("VAN_ADR")
      @Expose
      private String addrsOld;

      @SerializedName("NEW_ADR")
      @Expose
      private String addrsNew;

      @SerializedName("CAR_WASH_YN")
      private String carWashYN;

      @SerializedName("CVS_YN")
      private String cvsYN;

      @SerializedName("MAINT_YN")
      private String maintYN;
   }


}
