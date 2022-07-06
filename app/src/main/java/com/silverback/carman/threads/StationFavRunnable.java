package com.silverback.carman.threads;


import static com.silverback.carman.threads.StationFavRunnable.RetrofitApi.BASE_URL;

import android.content.Context;
import android.net.Uri;
import android.os.Process;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.Constants;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class StationFavRunnable implements Runnable {

    private static final LoggingHelper log = LoggingHelperFactory.create(StationFavRunnable.class);

    // Objects
    private final Context mContext;
    private final StationPriceMethods mCallback;
    private String stnId;


    // Interface
    interface StationPriceMethods {
        String getStationId();
        boolean getIsFirst();//if true, the station will be viewed in MainActivity w/ the price.
        void setStnPriceThread(Thread thread);
        void setFavStationInfo(Info info);
    }

    // Constructor
    StationFavRunnable(Context context, StationPriceMethods callback) {
        mContext = context;
        mCallback = callback;
    }

    @Override
    public void run() {
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        mCallback.setStnPriceThread(Thread.currentThread());

        stnId = mCallback.getStationId();
        try {
            if(Thread.interrupted()) throw new InterruptedException();

            // Fetch the first favorite station, the price info of which is shown in the price
            // viewpager in the main activity. Otherwise, just fetch the price info and show
            // it in the gas manager fragment in the expense activity.
            if(mCallback.getIsFirst()) {
                Call<FavStationModel> call = RetrofitClient.getIntance()
                        .getRetrofitApi()
                        .getStationInfoModel("F186170711", stnId, "json");
                call.enqueue(new Callback<FavStationModel>() {
                    @Override
                    public void onResponse(@NonNull Call<FavStationModel> call,
                                           @NonNull Response<FavStationModel> response) {

                        FavStationModel model = response.body();
                        assert model != null;

                        Info info = model.result.info.get(0);
                        //calculatePriceDiff(info);
                        mCallback.setFavStationInfo(info);
                    }

                    @Override
                    public void onFailure(@NonNull Call<FavStationModel> call,
                                          @NonNull Throwable t) {
                        // Exception handling required.
                    }
                });


            } else {
                // Other favorite stations
            }

        } catch(InterruptedException e){
            e.printStackTrace();
        }
    }

    public interface RetrofitApi {
        String BASE_URL = "https://www.opinet.co.kr/api/";
        @GET("detailById.do")
        Call<FavStationModel> getStationInfoModel (
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

    private static class FavStationModel {
        @SerializedName("RESULT")
        @Expose
        public Result result;
    }

    private static class Result {
        @SerializedName("OIL")
        @Expose
        public List<Info> info;

    }

    public static class Info implements Serializable {
        @SerializedName("UNI_ID")
        private String stationid;
        @SerializedName("OS_NM")
        private String stationName;
        @SerializedName("POLL_DIV_CD")
        private String company;
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
        @SerializedName("OIL_PRICE")
        private List<OilPrice> oilPriceList;
        @Expose
        private Map<String, Integer> priceDiff;

        public String getStationid() { return stationid; }
        public String getStationName() { return stationName; }
        public String getCompany() { return company; }
        public List<OilPrice> getOliPriceList() { return oilPriceList; }


        public void setPriceDiff(Map<String, Integer> priceDiff) {
            this.priceDiff = priceDiff;
        }
        public Map<String, Integer> getPriceDiff() {
            return priceDiff;
        }
    }

    public static class OilPrice implements Serializable {
        @SerializedName("PRODCD")
        private String oilCode;
        @SerializedName("PRICE")
        private int price;
        @SerializedName("TRADE_DT")
        private String tradeDate;
        @SerializedName("TRADE_TM")
        private String tradeTime;

        public String getOilCode() { return oilCode; }
        public int getPrice() { return price; }
        public String getTradeDate() { return tradeDate; }
        public String getTradeTime() { return tradeTime; }
    }



    private void calculatePriceDiff(Object obj) {
        final File file = new File(mContext.getFilesDir(), Constants.FILE_FAVORITE_PRICE);
        if(!file.exists()) {
            try(FileOutputStream fos = new FileOutputStream(file);
                ObjectOutputStream oos = new ObjectOutputStream(fos)) {
                oos.writeObject(obj);
            } catch (IOException e) { e.printStackTrace(); }

        } else {
            Uri uri = Uri.fromFile(file);
            try(InputStream is = mContext.getContentResolver().openInputStream(uri);
                ObjectInputStream ois = new ObjectInputStream(is)) {
                Info savedInfo = (Info)ois.readObject();
                if(savedInfo == null) {
                    log.i("saved info null");
                    return;
                }

                if(stnId.matches(savedInfo.getStationid())) {
                    Map<String, Integer> priceDiff = new HashMap<>();
                    Info currentInfo = (Info)obj;
                    for(OilPrice newInfo : currentInfo.oilPriceList) {
                        for(OilPrice oldInfo :savedInfo.oilPriceList) {
                            if(newInfo.oilCode.matches(oldInfo.oilCode)) {
                               priceDiff.put(newInfo.oilCode, (newInfo.price - oldInfo.price));
                            }
                        }
                    }

                    currentInfo.setPriceDiff(priceDiff);

                } else {
                    try(FileOutputStream fos = new FileOutputStream(file);
                        ObjectOutputStream oos = new ObjectOutputStream(fos)) {
                        oos.writeObject(obj);
                    } catch (IOException e) { e.printStackTrace(); }
                }
            } catch(IOException | ClassNotFoundException | NullPointerException e) {
                e.printStackTrace();
            }
        }
    }

}
