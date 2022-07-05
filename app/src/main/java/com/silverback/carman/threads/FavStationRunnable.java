package com.silverback.carman.threads;


import static com.silverback.carman.threads.FavStationRunnable.RetrofitApi.BASE_URL;

import android.content.Context;
import android.os.Process;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.viewmodels.XmlPullParserHandler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class FavStationRunnable implements Runnable {

    private static final LoggingHelper log = LoggingHelperFactory.create(FavStationRunnable.class);

    //private static final String API_KEY = "F186170711";
    //private static final String OPINET = "https://www.opinet.co.kr/api/";
    //private static final String URLStn = OPINET + "detailById.do?out=xml&code="+ API_KEY + "&id=";

    // Objects
    private final Context mContext;
    private final StationPriceMethods mCallback;
    private final XmlPullParserHandler xmlHandler;

    // Interface
    interface StationPriceMethods {
        String getStationId();
        boolean getIsFirst();//if true, the station will be viewed in MainActivity w/ the price.
        void setStnPriceThread(Thread thread);
        void setFavoritePrice(Map<String, Float> data);
        void savePriceDiff();
    }

    // Constructor
    FavStationRunnable(Context context, StationPriceMethods callback) {
        mContext = context;
        mCallback = callback;
        xmlHandler = new XmlPullParserHandler();
    }

    @Override
    public void run() {
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        mCallback.setStnPriceThread(Thread.currentThread());

        final String stationId = mCallback.getStationId();
        try {
            if(Thread.interrupted()) throw new InterruptedException();
            if(mCallback.getIsFirst()) {
                Call<FavStationModel> call = RetrofitClient.getIntance()
                        .getRetrofitApi()
                        .getStationInfoModel("F186170711", stationId, "json");
                call.enqueue(new Callback<FavStationModel>() {
                    @Override
                    public void onResponse(@NonNull Call<FavStationModel> call,
                                           @NonNull Response<FavStationModel> response) {

                    }

                    @Override
                    public void onFailure(@NonNull Call<FavStationModel> call,
                                          @NonNull Throwable t) {
                        log.i("call failed: %s", t);
                    }
                });

            } else {

            }



            /*
            URL url = new URL(URLStn + stationId);
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            try(InputStream in = conn.getInputStream()) {
                Opinet.StationPrice currentStation = xmlHandler.parseStationPrice(in);
                final String stnName = currentStation.getStnName();
                if(mCallback.getIsFirst()) {
                    final File file = new File(mContext.getFilesDir(), Constants.FILE_FAVORITE_PRICE);
                    if(!file.exists()) {
                        log.i("favorite station file not exists");
                        savePriceInfo(file, currentStation);
                        return;
                    }
                    // Read the saved station and compare the saved price w/ the current price if
                    Uri uri = Uri.fromFile(file);
                    try(InputStream is = mContext.getContentResolver().openInputStream(uri);
                        ObjectInputStream ois = new ObjectInputStream(is)) {
                        Opinet.StationPrice savedStation = (Opinet.StationPrice)ois.readObject();
                        log.i("compare price: %s, %s", savedStation.getStnName(), stnName);
                        if(Objects.equals(savedStation.getStnName(), stnName)){
                            log.i("get the price difference");
                            Map<String, Float> current = currentStation.getStnPrice();
                            Map<String, Float> prev = savedStation.getStnPrice();
                            Map<String, Float> diffPrice = new HashMap<>();

                            for (String key : current.keySet()) {
                                log.i("price key: %s", key);
                                Float currentPrice = current.get(key);
                                Float savedPrice = prev.get(key);
                                if (currentPrice == null) throw new NullPointerException();
                                if (savedPrice == null) throw new NullPointerException();
                                diffPrice.put(key, currentPrice - savedPrice);
                                log.i("price diff: %s",  currentPrice - savedPrice);
                                currentStation.setPriceDiff(diffPrice);
                            }
                        }

                        savePriceInfo(file, currentStation);

                    } catch(IOException | ClassNotFoundException | NullPointerException e) {
                        e.printStackTrace();
                    }

                } else {
                    log.i("favorite station price: %s", currentStation.getStnPrice());
                    mCallback.setFavoritePrice(currentStation.getStnPrice());
                }

            } finally { if(conn != null) conn.disconnect(); }

             */

        } catch(InterruptedException e){e.printStackTrace(); }
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

    private static class FavStationModel {
        @SerializedName("RESULT")
        @Expose
        public Result result;
    }

    public static class Result {
        @SerializedName("OIL")
        @Expose
        public List<StationInfoRunnable.Info> info;

    }

    public static class Info {
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

    private void savePriceInfo(File file, Object obj) {
        log.i("save price info");
        try(FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(obj);
            mCallback.savePriceDiff();
        } catch (IOException e) { e.printStackTrace(); }
    }

}
