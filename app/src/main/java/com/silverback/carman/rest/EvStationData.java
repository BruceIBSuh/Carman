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
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Query;

public class EvStationData {

    private static final LoggingHelper log = LoggingHelperFactory.create(EvStationData.class);

    public interface RetrofitApi {
        //@GET("getChargerStatus")
        @GET("getChargerStatus")
        @Headers({"Accept:application/xml"})
        Call<EvStationModel> getEvStationInfo (
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
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .addConverterFactory(TikXmlConverterFactory.create(new TikXml.Builder().exceptionOnUnreadXml(false).build()))
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

    }
}
