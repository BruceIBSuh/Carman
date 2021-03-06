package com.silverback.carman.threads;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Process;

import androidx.annotation.NonNull;

import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.rest.EvRetrofitTikXml;
import com.silverback.carman.utils.CustomPostingObject;
import com.tickaroo.tikxml.TikXml;
import com.tickaroo.tikxml.annotation.Element;
import com.tickaroo.tikxml.annotation.Path;
import com.tickaroo.tikxml.annotation.PropertyElement;
import com.tickaroo.tikxml.annotation.Xml;
import com.tickaroo.tikxml.retrofit.TikXmlConverterFactory;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Query;

public class StationEvRunnable implements Runnable {

    private static final LoggingHelper log = LoggingHelperFactory.create(StationEvRunnable.class);
    private static final String endPoint = "http://apis.data.go.kr/B552584/EvCharger/";
    private final String encodingKey = "Wd%2FkK0BbiWJlv1Rj9oR0Q7WA0aQ0UO3%2FY11uMkriK57e25VBUaNk1hQxQWv0svLZln5raxjA%2BFuCXzqm8pWu%2FQ%3D%3D";
    //private final String key ="Wd/kK0BbiWJlv1Rj9oR0Q7WA0aQ0UO3/Y11uMkriK57e25VBUaNk1hQxQWv0svLZln5raxjA+FuCXzqm8pWu/Q==";

    private final Geocoder geocoder;
    private final ElecStationCallback callback;

    //private EvStationModel model
    private final int queryPage;


    // Interface
    public interface ElecStationCallback {
        void setElecStationTaskThread(Thread thread);
        Location getElecStationLocation();
        void setEvStationList(List<Item> evList);
        void handleTaskState(int state);
        void notifyEvStationError(Exception e);
    }

    public StationEvRunnable(Context context, int queryPage, ElecStationCallback callback) {
        this.callback = callback;
        geocoder = new Geocoder(context, Locale.KOREAN);
        this.queryPage = queryPage;

        log.i("current page: %s", this.queryPage);
    }

    @Override
    public void run() {
        callback.setElecStationTaskThread(Thread.currentThread());
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        Location location = callback.getElecStationLocation();
        /*
        GeoPoint in_pt = new GeoPoint(location.getLongitude(), location.getLatitude());
        GeoPoint tm_pt = GeoTrans.convert(GeoTrans.GEO, GeoTrans.TM, in_pt);
        GeoPoint katec_pt = GeoTrans.convert(GeoTrans.TM, GeoTrans.KATEC, tm_pt);
        float x = (float) katec_pt.getX();
        float y = (float) katec_pt.getY();
        */
        // Get the sido code based on the current location using reverse Geocoding to narrow the
        // querying scope.
        int sido = getAddressfromLocation(location.getLatitude(), location.getLongitude());
        String sidoCode = String.valueOf(sido);

        Call<EvStationModel> call = RetrofitClient.getIntance()
                .getRetrofitApi()
                .getEvStationInfo(encodingKey, queryPage, 9999, 5, sidoCode);
                //.getEvStationInfo(encodingKey, queryPage, 9999, 5);

        call.enqueue(new Callback<EvStationModel>() {
            @Override
            public void onResponse(@NonNull Call<EvStationModel> call,
                                   @NonNull Response<EvStationModel> response) {

                final EvStationModel model = response.body();
                assert model != null;
                //final Header header = model.header;
                //int totalCount = header.totalCount;

                // Exclude an item if it is out of the distance or include an item within the distance
                List<Item> itemList = model.itemList;
                float[] results = new float[3];
                if(itemList != null && itemList.size() > 0) {
                    for (int i = itemList.size() - 1; i >= 0; i--) {
                        Location.distanceBetween(location.getLatitude(), location.getLongitude(),
                                itemList.get(i).lat, itemList.get(i).lng, results);
                        int distance = (int) results[0];

                        if (distance > 1000) itemList.remove(i);
                        else itemList.get(i).setDistance(distance);
                    }

                    log.i("ev runnable itemList: %s", itemList.size());
                }


                callback.setEvStationList(itemList);
            }

            @Override
            public void onFailure(@NonNull Call<EvStationModel> call, @NonNull Throwable t) {
                log.e("response failed: %s", t);
                callback.notifyEvStationError(new Exception(t));
                //callback.handleTaskState(EV_TASK_FAIL);
            }
        });
        //callback.handleTaskState(EV_TASK_SUCCESS);
    }

    private interface RetrofitApi {
        @GET("getChargerInfo")
        @Headers({"Accept:application/xml"})
        Call<EvStationModel> getEvStationInfo (
                @Query(value="serviceKey", encoded=true) String serviceKey,
                @Query(value="pageNo", encoded=true) int page,
                @Query(value="numOfRows", encoded=true) int rows,
                @Query(value="period", encoded=true) int period,
                @Query(value="zcode", encoded=true) String sidoCode
        );
    }

    private static class RetrofitClient {
        private final RetrofitApi retrofitApi;
        private RetrofitClient() {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(endPoint)
                    //.addConverterFactory(GsonConverterFactory.create())
                    //.addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .addConverterFactory(TikXmlConverterFactory.create(
                            new TikXml.Builder().exceptionOnUnreadXml(false).build()))
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


    @Xml(name="response")
    public static class EvStationModel {
        @Path("body/items")
        @Element
        List<Item> itemList;
    }

    /*
    @Xml(name="response")
    public static class EvStationModel {
        @Element Header header;
        @Element Body body;
    }

    @Xml(name="header")
    static class Header {
        @PropertyElement String resultCode;
        @PropertyElement String resultMsg;
        @PropertyElement int totalCount;
        @PropertyElement int pageNo;
        @PropertyElement int numOfRows;
    }

    @Xml(name="body")
    static class Body {
        @Element(name="items")
        Items items;
    }
    */

    @Xml
    public static class Items {
        @Element(name="item")
        List<Item> itemList;
    }

    @Xml
    public static class Item { //implements Parcelable, Serializable{
        @PropertyElement(name="statNm") String stdNm;
        @PropertyElement(name="statId") String stdId;
        @PropertyElement(name="chgerId") String chgerId;
        @PropertyElement(name="chgerType") String chgerType;
        @PropertyElement(name="addr") String addr;
        @PropertyElement(name="location") String location;
        @PropertyElement(name="lat") double lat;
        @PropertyElement(name="lng") double lng;
        //@PropertyElement(name="useTime") String useTime;
        //@PropertyElement(name="busiId") String busiId;
        //@PropertyElement(name="bnm") String bnm;
        //@PropertyElement(name="busiNm") String busiNm;
        //@PropertyElement(name="busiCall") String busiCall;
        @PropertyElement(name="stat") int stat;
        //@PropertyElement(name="statUpdDt") String statUpdDt;
        //@PropertyElement(name="lastTsdt") String lastTsdt;
        //@PropertyElement(name="lastTedt") String lastTedt;
        //@PropertyElement(name="nowTsdt") String nowTsdt;
        //@PropertyElement(name="powerType") String powerType;
        //@PropertyElement(name="output") String output;
        //@PropertyElement(name="method") String method;
        @PropertyElement(name="zcode") String zcode;
        //@PropertyElement(name="parkingFree") boolean parkingFree;
        //@PropertyElement(name="note") String node;
        //@PropertyElement(name="limitYn") boolean limitYn;
        @PropertyElement(name="limitDetail") String limitDetail;

        public String getStdNm() {return stdNm;}
        public void setStdNm(String name) { this.stdNm = name; }
        public String getStdId() {return stdId;}
        public String getChgerId() {return chgerId;}
        public String getChgerType() {return convChargerType(chgerType);}
        public String getAddr() {return addr;}
        public String getLocation() { return location; }
        public double getLat() {return lat;}
        public double getLng() {return lng;}
        //public String getUseTime() {return useTime;}
        //public String getBusiId() {return busiId;}
        //public String getBnm() {return bnm;}
        //public String getBusiNm() { return busiNm; }
        //public String getBusiCall() { return busiCall; }
        public int getStat() { return stat; }
        //public String getStatUpdDt() { return statUpdDt; }
        //public String getLastTsdt() { return lastTsdt; }
        //public String getLastTedt() { return lastTedt; }
        //public String getNowTsdt() { return nowTsdt; }
        //public String getPowerType() { return powerType; }
        //public String getOutput() { return output; }
        //public String getMethod() { return method; }
        public String getZcode() { return zcode;}
        //public boolean isParkingFree() { return parkingFree; }
        //public String getNode() { return node; }
        //public boolean isLimitYn() { return limitYn; }
        public String getLimitDetail() { return limitDetail; }

        private int distance;
        public int getDistance() { return distance; }
        public void setDistance(int distance) { this.distance = distance; }

        private int cntCharger;
        public void setCntCharger(int cnt) { this.cntCharger = cnt;}
        public int getCntCharger() { return cntCharger; }

        private boolean isAnyChargerOpen;
        public void setIsAnyChargerOpen(boolean isAnyChargerOpen) {
            this.isAnyChargerOpen = isAnyChargerOpen;
        }
        public boolean getIsAnyChargerOpen() {
            return isAnyChargerOpen;
        }

        // Empty constructor
        /*
        public Item() {}
        // Parcelize the object
        protected Item(Parcel in) {
            stdNm = in.readString();
            chgerId = in.readString();
            chgerType = in.readString();
            addr = in.readString();
            location = in.readString();
            lat = in.readDouble();
            lng = in.readDouble();
            stat = in.readInt();
            zcode = in.readString();
            limitDetail = in.readString();
            distance = in.readInt();
            cntCharger = in.readInt();
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
            parcel.writeString(stdNm);
            parcel.writeString(chgerId);
            parcel.writeString(chgerType);
            parcel.writeString(addr);
            parcel.writeString(location);
            parcel.writeDouble(lat);
            parcel.writeDouble(lng);
            parcel.writeInt(stat);
            parcel.writeString(zcode);
            parcel.writeString(limitDetail);
            parcel.writeInt(distance);
            parcel.writeInt(cntCharger);
        }

         */
    }

    // Refactor required as of Android13(Tiramisu), which has added the listener for getting the
    // address done.
    private int getAddressfromLocation(double lat, double lng) {
        int sidoCode = -1;
        // Remove less than the third decimal place.
        /*
        double lat = Math.round(latitude * 1000) / 1000.0;
        double lng = Math.round(longitude * 1000) / 1000.0;
        log.i("Geocoding: %s, %s, %s", lat, lng, sidoCode);
        */
        try {
            List<Address> addressList = geocoder.getFromLocation(lat, lng, 1);
            for(Address addrs : addressList) {
                if(addrs != null) {
                    String address = addrs.getAddressLine(0).substring(5);
                    String sido = address.substring(0, address.indexOf(" "));
                    sidoCode = convSidoCode(sido);
                    break;
                }
            }

        } catch(IOException e) { e.printStackTrace(); }

        return sidoCode;
    }

    private int convSidoCode(String sido) {
        switch(sido) {
            case "???????????????": return 11; case "???????????????": return 26; case "???????????????": return 27;
            case "???????????????": return 28; case "???????????????": return 29; case "???????????????": return 30;
            case "???????????????": return 31; case "?????????????????????": return 36; case "?????????": return 41;
            case "?????????": return 42; case "????????????": return 43; case "????????????": return 44;
            case "????????????": return 45; case "????????????": return 46; case "????????????": return 47;
            case "????????????": return 48; case "?????????????????????": return 50;
            default: return -1;
        }
    }

    private static String convChargerType(String code) {
        switch(code) {
            case "01" : return "DC??? ??????";
            case "02" : return "AC ??????";
            case "03" : return "DC?????????+AC3???";
            case "04" : return "DC??????";
            case "05" : return "DC?????????+DC??????";
            case "06" : return "DC?????????+AC3???+DC??????";
            case "07" : return "AC3???";
            default : return "N/A";
        }
    }

    private static String convChargerStatus(String code) {
        switch(code) {
            case "1" : return "????????????";
            case "2" : return "????????????";
            case "3" : return "?????????";
            case "4" : return "????????????";
            case "5" : return "?????????";
            case "9" : return "???????????????";
            default: return "N/A";
        }
    }
}
