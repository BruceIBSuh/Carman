package com.silverback.carman.threads;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;
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

import java.io.Serializable;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
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
        //void setNearStationList(List<Opinet.GasStnParcelable> list);
        //void setCurrentStation(Opinet.GasStnParcelable station);
        void setNearStationList(List<Item> stationList);
        void setCurrentStation(Item station);
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
                log.i("ItemList: %s", itemList.size());
                for(Item item: itemList) log.i("Item: %s", item.getStnName());

                if(itemList.size() > 0) {
                    if(radius.matches(Constants.MIN_RADIUS)) {
                        mTask.setCurrentStation(itemList.get(0));
                        mTask.handleTaskState(StationGasTask.DOWNLOAD_CURRENT_STATION);
                    } else {
                        mTask.setNearStationList(itemList);
                        mTask.handleTaskState(StationGasTask.DOWNLOAD_NEAR_STATIONS);
                    }
                } else {
                    log.i("no station around");
                    if (radius.matches(Constants.MIN_RADIUS)) {
                        mTask.handleTaskState(StationGasTask.DOWNLOAD_CURRENT_STATION_FAIL);
                    } else mTask.handleTaskState(StationGasTask.DOWNLOAD_NEAR_STATIONS_FAIL);
                }
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
        );

    }

    private static class RetrofitClient {
        private final RetrofitApi retrofitApi;
        private RetrofitClient() {
            Gson gson = new GsonBuilder().setLenient().create(); //make it less strict
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(RetrofitApi.BASE_URL)
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

    public static class Item implements Serializable, Parcelable {
        @SerializedName("UNI_ID")
        @Expose
        String stnId;
        @SerializedName("OS_NM")
        @Expose
        private String stnName;
        @SerializedName("POLL_DIV_CD")
        @Expose
        private String stnCompany;
        @SerializedName("PRICE")
        @Expose
        private int gasPrice;
        @SerializedName("DISTANCE")
        @Expose
        private float distance;
        @SerializedName("GIS_X_COOR")
        @Expose
        private double x;
        @SerializedName("GIS_Y_COOR")
        @Expose
        private double y;

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

        public int getGasPrice() {
            return gasPrice;
        }
        public void setGasPrice(int gasPrice) {
            this.gasPrice = gasPrice;
        }

        public float getStnDistance() {
            return distance;
        }
        public void setStnDistance(float stnDistance) {
            this.distance = stnDistance;
        }

        public double getX() {
            return x;
        }
        public void setX(double x) {
            this.x = x;
        }

        public double getY() {
            return y;
        }
        public void setY(double y) {
            this.y = y;
        }

        public boolean isCarWash;
        public void setIsCarWash(boolean isCarWash) {
            this.isCarWash = isCarWash;
        }
        public boolean getIsCarWash() {
            return isCarWash;
        }

        public boolean isCVS;
        public void setIsCVS(boolean isCVS) { this.isCVS = isCVS; }
        public boolean getIsCVS() { return isCVS; }

        public boolean isService;
        public void setIsService(boolean isService) { this.isService = isService; }
        public boolean getIsService() { return isService; }

        public String addrsNew;
        public String getAddrsNew() { return addrsNew; }
        public void setAddrsNew(String addrsNew) { this.addrsNew = addrsNew;}

        public String addrsOld;
        public String getAddrsOld() { return addrsOld; }
        public void setAddrsOld(String addrsNew) { this.addrsOld = addrsOld;}

        // Parcelize
        protected Item(Parcel in) {
            stnId = in.readString();
            stnName = in.readString();
            stnCompany = in.readString();
            gasPrice = in.readInt();
            distance = in.readFloat();
            x = in.readDouble();
            y = in.readDouble();
            isCarWash = in.readByte() != 0;
            isCVS = in.readByte() != 0;
            isService = in.readByte() != 0;
            addrsNew = in.readString();
            addrsOld = in.readString();
        }

        public static final Creator<Item> CREATOR = new Creator<Item>() {
            @Override
            public Item createFromParcel(Parcel in) {
                return new Item(in);
            }

            @Override
            public Item[] newArray(int size) {
                return new Item[size];
            }
        };


        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeString(stnId);
            parcel.writeString(stnName);
            parcel.writeString(stnCompany);
            parcel.writeInt(gasPrice);
            parcel.writeFloat(distance);
            parcel.writeDouble(x);
            parcel.writeDouble(y);
            parcel.writeByte((byte)(isCarWash?1:0));
            parcel.writeByte((byte)(isCVS?1:0));
            parcel.writeByte((byte)(isService?1:0));
            parcel.writeString(addrsNew);
            parcel.writeString(addrsOld);
        }
    }

}