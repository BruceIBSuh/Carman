package com.silverback.carman.threads;

import android.location.Location;
import android.os.Process;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import com.silverback.carman.coords.GeoPoint;
import com.silverback.carman.coords.GeoTrans;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.Constants;
import com.silverback.carman.viewmodels.Opinet;
import com.silverback.carman.viewmodels.XmlPullParserHandler;
import com.tickaroo.tikxml.TikXml;
import com.tickaroo.tikxml.retrofit.TikXmlConverterFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Path;
import retrofit2.http.Query;

public class StationGasRunnable implements Runnable{

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(StationGasRunnable.class);


    private static final String OPINET = "https://www.opinet.co.kr/api/aroundAll.do?code=F186170711&out=xml";



    // Objects
    private FirebaseFirestore fireStore;
    private List<Opinet.GasStnParcelable> mStationList;
    private final StationListMethod mTask;

    // Interface
    public interface StationListMethod {
        String[] getDefaultParam();
        Location getStationLocation();
        void setStationTaskThread(Thread thread);
        void setNearStationList(List<Opinet.GasStnParcelable> list);
        void setCurrentStation(Opinet.GasStnParcelable station);
        void notifyException(String msg);
        void handleTaskState(int state);
    }

    // Constructor
    public StationGasRunnable(StationListMethod task) {
        if(fireStore == null) fireStore = FirebaseFirestore.getInstance();
        mStationList = null;
        mTask = task;
    }

    @Override
    public void run() {
        mTask.setStationTaskThread(Thread.currentThread());
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        log.i("statinlistrunnable thread: %s", Thread.currentThread());

        String[] defaultParams = mTask.getDefaultParam();
        Location location = mTask.getStationLocation();

        // Get the default params and location passed over here from MainActivity
        String fuelCode = defaultParams[0];
        String radius = defaultParams[1];
        String sort = defaultParams[2];

        // Convert longitute and latitude-based location to TM(Transverse Mercator), then again to
        // Katec location using the coords package, which is distributed over internet^^.
        GeoPoint in_pt = new GeoPoint(location.getLongitude(), location.getLatitude());
        GeoPoint tm_pt = GeoTrans.convert(GeoTrans.GEO, GeoTrans.TM, in_pt);
        GeoPoint katec_pt = GeoTrans.convert(GeoTrans.TM, GeoTrans.KATEC, tm_pt);
        float x = (float) katec_pt.getX();
        float y = (float) katec_pt.getY();

        /*
        // Complete the OPINET_ARUND URL w/ the given requests
        final String OPINET_AROUND = OPINET
                + "&x=" + x
                + "&y=" + y
                + "&radius=" + radius
                + "&sort=" + sort // 1: price 2: distance
                + "&prodcd=" + fuelCode;

        try {
            if(Thread.interrupted()) throw new InterruptedException();

            final URL url = new URL(OPINET_AROUND);
            XmlPullParserHandler xmlHandler = new XmlPullParserHandler();
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setRequestProperty("Connection", "close");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.connect();

            try(InputStream is = new BufferedInputStream(conn.getInputStream())) {
                mStationList = xmlHandler.parseStationListParcelable(is);
                // Get near stations which may be the current station if MIN_RADIUS is given  or
                // it should be near stations located within SEARCHING_RADIUS.
                if(mStationList.size() > 0) {
                    if(radius.matches(Constants.MIN_RADIUS)) {
                        mTask.setCurrentStation(mStationList.get(0));
                        mTask.handleTaskState(StationGasTask.DOWNLOAD_CURRENT_STATION);
                    } else {
                        mTask.setNearStationList(mStationList);
                        mTask.handleTaskState(StationGasTask.DOWNLOAD_NEAR_STATIONS);
                    }
                } else {
                    if(radius.matches(Constants.MIN_RADIUS)) {
                        mTask.handleTaskState(StationGasTask.DOWNLOAD_CURRENT_STATION_FAIL);
                    } else mTask.handleTaskState(StationGasTask.DOWNLOAD_NEAR_STATIONS_FAIL);
                }
            } finally { conn.disconnect(); }

        } catch (IOException | InterruptedException e) {
            mTask.notifyException(e.getLocalizedMessage());
            mTask.handleTaskState(StationGasTask.DOWNLOAD_NEAR_STATIONS_FAIL);
        }
        */

        int rad = Integer.parseInt(defaultParams[1]);
        int order = Integer.parseInt(defaultParams[2]);
        Call<Object> call = RetrofitClient.getIntance()
                .getRetrofitApi()
                .getGasStationAroundModel("F186170711", x, y, rad, order, fuelCode, "json");

        call.enqueue(new Callback<Object>() {
            @Override
            public void onResponse(@NonNull Call<Object> call,
                                   @NonNull Response<Object> response) {

               log.i("response: %s", response.body().toString());


            }

            @Override
            public void onFailure(@NonNull Call<Object> call, @NonNull Throwable t) {
                log.e("retrofit failed: %s, %s", call, t);
            }
        });
    }


    public interface RetrofitApi {
        String BASE_URL = "https://www.opinet.co.kr/api/";
        @GET("aroundAll.do")
        Call<Object> getGasStationAroundModel (
                @Query(value="code", encoded=true) String serviceKey,
                @Query(value="x", encoded=true) float x,
                @Query(value="y", encoded=true) float y,
                @Query(value="radius", encoded=true) int radius,
                @Query(value="sort", encoded=true) int sort,
                @Query(value="prodcd", encoded=true) String prodCd,
                @Query(value="out") String format
        );

    }

    private static class RetrofitClient {
        private final RetrofitApi retrofitApi;
        private RetrofitClient() {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(RetrofitApi.BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
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

    private static class GasStationAroundModel {

        //@SerializedName("RESULT")
        //private List<GasStationAroundModel> GasStationAroundModelList;
        @SerializedName("UNI_ID")
        private String stnId;
        @SerializedName("POLL_DIV_CO")
        private String stnCompany;
        @SerializedName("OS_NM")
        private String stnName;
        @SerializedName("PRICE")
        private int gasPrice;
        @SerializedName("DISTANCE")
        private float stnDistance;
        @SerializedName("GIS_X_COOR")
        private double xCoords;
        @SerializedName("GIS_Y_COOR")
        private double yCoords;

        public GasStationAroundModel() {}
        public GasStationAroundModel(String id, String company, String name,
                                     int price, float distance, double x, double y) {
            this.stnId = id;
            this.stnCompany = company;
            this.stnName = name;
            this.gasPrice = price;
            this.stnDistance = distance;
            this.xCoords = x;
            this.yCoords = y;
        }

        public String getStnId() {
            return stnId;
        }

        public String getStnCompany() {
            return stnCompany;
        }

        public String getStnName() {
            return stnName;
        }

        public int getGasPrice() {
            return gasPrice;
        }

        public float getStnDistance() {
            return stnDistance;
        }

        public double getxCoords() {
            return xCoords;
        }

        public double getyCoords() {
            return yCoords;
        }

    }

}