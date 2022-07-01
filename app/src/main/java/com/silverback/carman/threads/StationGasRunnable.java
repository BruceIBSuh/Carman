package com.silverback.carman.threads;

import android.location.Location;
import android.os.Process;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.silverback.carman.coords.GeoPoint;
import com.silverback.carman.coords.GeoTrans;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.Constants;
import com.silverback.carman.viewmodels.Opinet;
import com.squareup.okhttp.OkHttpClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.Result;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Field;
import retrofit2.http.GET;
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
        Call<StationAroundModel> call = RetrofitClient.getIntance()
                .getRetrofitApi()
                //.getGasStationAroundModel("F186170711", "B027", "json");
                .getGasStationAroundModel("F186170711", x, y, rad, order, fuelCode, "json");

        call.enqueue(new Callback<StationAroundModel>() {
            @Override
            public void onResponse(@NonNull Call<StationAroundModel> call,
                                   @NonNull Response<StationAroundModel> response) {

                StationAroundModel model = response.body();
                assert model != null;
                List<Item> itemList = model.result.oilList;
                for(Item item: itemList) log.i("Item: %s", item.getStnName());
                /*
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

                 */
            }

            @Override
            public void onFailure(@NonNull Call<StationAroundModel> call, @NonNull Throwable t) {
                log.e("retrofit failed: %s, %s", call, t);
            }
        });
    }


    public interface RetrofitApi {
        String BASE_URL = "https://www.opinet.co.kr/api/";
        @GET("aroundAll.do")
        Call<StationAroundModel> getGasStationAroundModel (

                @Query("code") String code,
                @Query("x") float x,
                @Query("y") float y,
                @Query("radius") int radius,
                @Query("sort") int sort,
                @Query("prodcd") String prodCd,
                @Query("out") String out
                /*
                @Query("code") String code,
                @Query("prodcd") String prodcd,
                @Query("out") String out

                 */
        );

    }

    private static class RetrofitClient {
        private final RetrofitApi retrofitApi;


        private RetrofitClient() {
            Gson gson = new GsonBuilder().setLenient().create();
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(RetrofitApi.BASE_URL)
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

    public static class StationAroundModel {
        @SerializedName("RESULT")
        @Expose
        public Result result;

    }

    public static class Result {
        @SerializedName("OIL")
        @Expose
        public List<Item> oilList;
    }

    public static class Item {
        @SerializedName("UNI_ID")
        @Expose
        private String stnId;
        @SerializedName("POLL_DIV_CO")
        @Expose
        private String stnCompany;
        @SerializedName("OS_NM")
        @Expose
        private String stnName;
        @SerializedName("PRICE")
        @Expose
        private Integer gasPrice;
        @SerializedName("DISTANCE")
        @Expose
        private Float stnDistance;
        @SerializedName("GIS_X_COOR")
        @Expose
        private Double xCoords;
        @SerializedName("GIS_Y_COOR")
        @Expose
        private Double yCoords;

        public String getStnId() {
            return stnId;
        }

        public void setStnId(String stnId) {
            this.stnId = stnId;
        }

        public String getStnCompany() {
            return stnCompany;
        }

        public void setStnCompany(String stnCompany) {
            this.stnCompany = stnCompany;
        }

        public String getStnName() {
            return stnName;
        }

        public void setStnName(String stnName) {
            this.stnName = stnName;
        }

        public Integer getGasPrice() {
            return gasPrice;
        }

        public void setGasPrice(Integer gasPrice) {
            this.gasPrice = gasPrice;
        }

        public Float getStnDistance() {
            return stnDistance;
        }

        public void setStnDistance(Float stnDistance) {
            this.stnDistance = stnDistance;
        }

        public Double getxCoords() {
            return xCoords;
        }

        public void setxCoords(Double xCoords) {
            this.xCoords = xCoords;
        }

        public Double getyCoords() {
            return yCoords;
        }

        public void setyCoords(Double yCoords) {
            this.yCoords = yCoords;
        }





    }


    /*
    private static class StationAroundModel {


        @SerializedName("UNI_ID")
        private String stnId;
        @SerializedName("POLL_DIV_CO")
        private String stnCompany;
        @SerializedName("OS_NM")
        private String stnName;
        @SerializedName("PRICE")
        private Integer gasPrice;

        @SerializedName("DISTANCE")
        private Float stnDistance;
        @SerializedName("GIS_X_COOR")
        private Double xCoords;
        @SerializedName("GIS_Y_COOR")
        private Double yCoords;

        public void setResult(HashMap<String, Object> result) {
            this.result = result;
        }
        public Object getResult() {
            return result;
        }


        public void setOilList(HashMap<String, List<Object>> oilList) {
            this.oilList = oilList;
        }



        public HashMap<String, List<Object>> getOiList() {
            return oilList;
        }

        /*
        public void setStnId(String stnId) {
            this.stnId = stnId;
        }

        public void setStnCompany(String stnCompany) {
            this.stnCompany = stnCompany;
        }

        public void setStnName(String stnName) {
            this.stnName = stnName;
        }

        public void setGasPrice(Integer gasPrice) {
            this.gasPrice = gasPrice;
        }

        public void setStnDistance(Float stnDistance) {
            this.stnDistance = stnDistance;
        }

        public void setxCoords(Double xCoords) {
            this.xCoords = xCoords;
        }

        public void setyCoords(Double yCoords) {
            this.yCoords = yCoords;
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

        public Integer getGasPrice() {
            return gasPrice;
        }

        public Float getStnDistance() {
            return stnDistance;
        }

        public Double getxCoords() {
            return xCoords;
        }

        public Double getyCoords() {
            return yCoords;
        }

         */


}