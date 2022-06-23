package com.silverback.carman.rest;

import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.threads.StationEvRunnable;
import com.tickaroo.tikxml.TikXml;
import com.tickaroo.tikxml.annotation.Element;
import com.tickaroo.tikxml.annotation.PropertyElement;
import com.tickaroo.tikxml.annotation.Xml;
import com.tickaroo.tikxml.retrofit.TikXmlConverterFactory;

import java.util.List;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Query;

public class EvRetrofitTikXml {

    private static final LoggingHelper log = LoggingHelperFactory.create(EvRetrofitTikXml.class);
    private static final String baseUrl = "http://apis.data.go.kr/B552584/EvCharger/";

    private final RetrofitApi retrofitApi;

    public interface RetrofitApi {
        @GET("getChargerStatus")
        @Headers({"Accept:application/xml"})
        Call<EvModel> getEvStationInfo (
                @Query(value="ServiceKey", encoded=true) String serviceKey,
                @Query("pageNo") int page,
                @Query("numOfRows") int rows,
                @Query("period") int period,
                @Query("zcode") String sido
        );
    }


    private EvRetrofitTikXml() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(TikXmlConverterFactory.create (
                        new TikXml.Builder().exceptionOnUnreadXml(false).build()))
                .build();

        retrofitApi = retrofit.create(RetrofitApi.class);
    }

    private static class LazyHolder {
        private static final EvRetrofitTikXml sInstance = new EvRetrofitTikXml();
    }

    public static EvRetrofitTikXml getIntance() {
        return LazyHolder.sInstance;
    }

    public RetrofitApi getRetrofitApi() {
        return retrofitApi;
    }

    @Xml(name="response")
    public static class EvModel {
        @Element
        public Header header;
        @Element
        public Body body;
    }

    @Xml(name="header")
    public static class Header {
        @PropertyElement
        public String resultCode;
        @PropertyElement
        public String resultMsg;
        @PropertyElement
        public int totalCount;
        @PropertyElement
        public int pageNo;
        @PropertyElement
        public int numOfRows;
    }

    @Xml(name="body")
    public static class Body {
        @Element(name="items")
        public Items items;
    }

    @Xml
    public static class Items {
        @Element(name="item")
        public List<Item> itemList;
    }

    @Xml
    public static class Item {
        @PropertyElement(name="statNm")
        String stdNm;
        @PropertyElement(name="statId")
        public String stdId;
        @PropertyElement(name="chgerId")
        public String chgerId;
        @PropertyElement(name="chgerType")
        public String chgerType;
        @PropertyElement(name="addr")
        public String addr;
        @PropertyElement(name="location")
        public String location;
        @PropertyElement(name="lat")
        public double lat;
        @PropertyElement(name="lng")
        public double lng;
        @PropertyElement(name="useTime")
        public String useTime;
        @PropertyElement(name="busiId")
        public String busiId;
        @PropertyElement(name="bnm")
        public String bnm;
        @PropertyElement(name="busiNm")
        public String busiNm;
        @PropertyElement(name="busiCall")
        public String busiCall;
        @PropertyElement(name="stat")
        public int stat;
        @PropertyElement(name="statUpdDt")
        public String statUpdDt;
        @PropertyElement(name="lastTsdt")
        public String lastTsdt;
        @PropertyElement(name="lastTedt") public String lastTedt;
        @PropertyElement(name="nowTsdt") public String nowTsdt;
        @PropertyElement(name="powerType") public String powerType;
        @PropertyElement(name="output") public String output;
        @PropertyElement(name="method") public String method;
        @PropertyElement(name="zcode") public String zcode;
        @PropertyElement(name="parkingFree") public boolean parkingFree;
        @PropertyElement(name="note") public String node;
        @PropertyElement(name="limitYn") public boolean limitYn;
        @PropertyElement(name="limitDetail") public String limitDetail;

        public String getStdNm() {return stdNm;}
        public String getStdId() {return stdId;}
        public String getChgerId() {return chgerId;}
        public String getChgerType() {return chgerType;}
        public String getAddr() {return addr;}
        public String getLocation() { return location; }
        public double getLat() {return lat;}
        public double getLng() {return lng;}
        public String getUseTime() {return useTime;}
        public String getBusiId() {return busiId;}
        public String getBnm() {return bnm;}
        public String getBusiNm() { return busiNm; }
        public String getBusiCall() { return busiCall; }
        public int getStat() { return stat; }
        public String getStatUpdDt() { return statUpdDt; }
        public String getLastTsdt() { return lastTsdt; }
        public String getLastTedt() { return lastTedt; }
        public String getNowTsdt() { return nowTsdt; }
        public String getPowerType() { return powerType; }
        public String getOutput() { return output; }
        public String getMethod() { return method; }
        public String getZcode() { return zcode;}
        public boolean isParkingFree() { return parkingFree; }
        public String getNode() { return node; }
        public boolean isLimitYn() { return limitYn; }
        public String getLimitDetail() { return limitDetail; }

        public int distance;
        public int getDistance() { return distance; }
        public void setDistance(int distance) { this.distance = distance; }
    }
}
