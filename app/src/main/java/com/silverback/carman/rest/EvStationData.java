package com.silverback.carman.rest;

import com.google.gson.annotations.SerializedName;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.tickaroo.tikxml.TikXml;
import com.tickaroo.tikxml.retrofit.TikXmlConverterFactory;

import java.util.List;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.simplexml.SimpleXmlConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Query;

public class EvStationData {

    private static final LoggingHelper log = LoggingHelperFactory.create(EvStationData.class);

    public interface RetrofitApi {
        //@GET("getChargerStatus")
        @GET("getChargerStatus")
        @Headers({"Accept:application/xml"})
        Call<List<EvStationModel>> getEvStationInfo (
                @Query(value="ServiceKey", encoded=true) String serviceKey,
                @Query("pageNo") int page,
                @Query("numOfRows") int rows,
                @Query("period") int period,
                @Query("zcode") String sido
        );
    }

    public static class RetrofitClient {
        private final RetrofitApi retrofitApi;

        private RetrofitClient() {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("http://apis.data.go.kr/B552584/EvCharger/")
                    //.addConverterFactory(GsonConverterFactory.create())
                    //.addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    //.addConverterFactory(TikXmlConverterFactory.create(new TikXml.Builder().exceptionOnUnreadXml(false).build()))
                    .addConverterFactory(TikXmlConverterFactory.create(new TikXml.Builder().build()))
                    .build();
            retrofitApi = retrofit.create(RetrofitApi.class);
        }

        // Bill-Pugh Singleton instance
        private static class LazyHolder {
            private static final RetrofitClient sInstance = new RetrofitClient();
        }
        public static RetrofitClient getIntance() {
            return LazyHolder.sInstance;
        }
        public RetrofitApi getRetrofitApi() {
            return retrofitApi;
        }
    }

    public static class EvStationModel {
        private String hydroName;
        private String hydrochgr;
        private String addrs;
        private String bizhour;
        private String price;
        private String phone;

        public String getHydroName() {
            return hydroName;
        }

        public void setHydroName(String hydroName) {
            this.hydroName = hydroName;
        }

        public String getHydrochgr() {
            return hydrochgr;
        }

        public void setHydrochgr(String hydrochgr) {
            this.hydrochgr = hydrochgr;
        }

        public String getAddrs() {
            return addrs;
        }

        public void setAddrs(String addrs) {
            this.addrs = addrs;
        }

        public String getBizhour() {
            return bizhour;
        }

        public void setBizhour(String bizhour) {
            this.bizhour = bizhour;
        }

        public String getPrice() {
            return price;
        }

        public void setPrice(String price) {
            this.price = price;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }



    }


    public static class EvStationList {
        @SerializedName("data")
        private final List<EvStationModel> evStationList;

        public EvStationList(List<EvStationModel> evStationList) {
            this.evStationList = evStationList;
        }

        public List<EvStationModel> getEvStationList() {
            return evStationList;
        }

    }
}
