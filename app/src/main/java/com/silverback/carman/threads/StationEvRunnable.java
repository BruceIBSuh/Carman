package com.silverback.carman.threads;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Process;

import androidx.annotation.NonNull;

import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class StationEvRunnable implements Runnable{

    private static final LoggingHelper log = LoggingHelperFactory.create(StationEvRunnable.class);
    private final String evUrl = "http://apis.data.go.kr/B552584/EvCharger";

    private final String encodingKey = "Wd%2FkK0BbiWJlv1Rj9oR0Q7WA0aQ0UO3%2FY11uMkriK57e25VBUaNk1hQxQWv0svLZln5raxjA%2BFuCXzqm8pWu%2FQ%3D%3D";
    private final String key ="Wd/kK0BbiWJlv1Rj9oR0Q7WA0aQ0UO3/Y11uMkriK57e25VBUaNk1hQxQWv0svLZln5raxjA+FuCXzqm8pWu/Q==";

    private final Geocoder geocoder;
    private final ElecStationCallback callback;

    private EvStationModel model;

    private int queryPage;

    // Interface
    public interface ElecStationCallback {
        void setElecStationTaskThread(Thread thread);
        Location getElecStationLocation();
        int getCurrentPage();
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

        //int sido = getAddressfromLocation(location.getLatitude(), location.getLongitude());
        //String sidoCode = String.valueOf(sido);

        //for(int page = 1; page <= 5; page++) {
        //synchronized (this) {
            Call<EvStationModel> call = RetrofitClient.getIntance()
                    .getRetrofitApi()
                    //.getEvStationInfo(encodingKey, page, 9999, 5, sidoCode);
                    .getEvStationInfo(encodingKey, queryPage, 9999, 5);

            call.enqueue(new Callback<EvStationModel>() {
                @Override
                public void onResponse(@NonNull Call<EvStationModel> call,
                                       @NonNull Response<EvStationModel> response) {

                    final EvStationModel model = response.body();
                    assert model != null;
                    // Exclude an item if it is out of the distance or include an item within the distance
                    List<Item> itemList = model.itemList;
                    if(itemList != null && itemList.size() > 0) {
                        log.i("ItemList: %s", model.itemList.size());
                        for (int i = model.itemList.size() - 1; i >= 0; i--) {
                            float[] results = new float[3];
                            Location.distanceBetween(location.getLatitude(), location.getLongitude(),
                                    model.itemList.get(i).lat, model.itemList.get(i).lng, results);
                            int distance = (int) results[0];

                            if (distance > 1000) model.itemList.remove(i);
                            else model.itemList.get(i).setDistance(distance);
                        }
                    }

                    callback.setEvStationList(model.itemList);
                }

                @Override
                public void onFailure(@NonNull Call<EvStationModel> call, @NonNull Throwable t) {
                    log.e("response failed: %s", t);
                    callback.notifyEvStationError(new Exception(t));
                    //callback.handleTaskState(EV_TASK_FAIL);
                }
            });
        //}
        //callback.handleTaskState(EV_TASK_SUCCESS);
    }


    public interface RetrofitApi {
        @GET("getChargerInfo")
        //@Headers({"Accept:application/xml"})
        Call<EvStationModel> getEvStationInfo (
                @Query(value="serviceKey", encoded=true) String serviceKey,
                @Query(value="pageNo", encoded=true) int page,
                @Query(value="numOfRows", encoded=true) int rows,
                @Query(value="period", encoded=true) int period
                //@Query(value="zcode", encoded=true) String sido
        );
    }

    public static class RetrofitClient {
        private final RetrofitApi retrofitApi;

        private RetrofitClient() {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("http://apis.data.go.kr/B552584/EvCharger/")
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
    static class EvStationModel {
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

    @Xml
    public static class Items {
        @Element(name="item")
        List<Item> item;
    }
    */
    @Xml(name="header")
    static class Header {
        @PropertyElement String resultCode;
        @PropertyElement String resultMsg;
        @PropertyElement int totalCount;
        @PropertyElement int pageNo;
        @PropertyElement int numOfRows;
    }

    @Xml
    public static class Item {
        @PropertyElement(name="statNm") String stdNm;
        @PropertyElement(name="statId") String stdId;
        @PropertyElement(name="chgerId") String chgerId;
        @PropertyElement(name="chgerType") String chgerType;
        @PropertyElement(name="addr") String addr;
        @PropertyElement(name="location") String location;
        @PropertyElement(name="lat") double lat;
        @PropertyElement(name="lng") double lng;
        @PropertyElement(name="useTime") String useTime;
        @PropertyElement(name="busiId") String busiId;
        @PropertyElement(name="bnm") String bnm;
        @PropertyElement(name="busiNm") String busiNm;
        @PropertyElement(name="busiCall") String busiCall;
        @PropertyElement(name="stat") int stat;
        @PropertyElement(name="statUpdDt") String statUpdDt;
        @PropertyElement(name="lastTsdt") String lastTsdt;
        @PropertyElement(name="lastTedt") String lastTedt;
        @PropertyElement(name="nowTsdt") String nowTsdt;
        @PropertyElement(name="powerType") String powerType;
        @PropertyElement(name="output") String output;
        @PropertyElement(name="method") String method;
        @PropertyElement(name="zcode") String zcode;
        @PropertyElement(name="parkingFree") boolean parkingFree;
        @PropertyElement(name="note") String node;
        @PropertyElement(name="limitYn") boolean limitYn;
        @PropertyElement(name="limitDetail") String limitDetail;

        public String getStdNm() {
            return stdNm;
        }

        public String getStdId() {
            return stdId;
        }

        public String getChgerId() {
            return chgerId;
        }

        public String getChgerType() {
            return convChargerType(chgerType);
        }

        public String getAddr() {
            return addr;
        }

        public String getLocation() {
            return location;
        }

        public double getLat() {
            return lat;
        }

        public double getLng() {
            return lng;
        }

        public String getUseTime() {
            return useTime;
        }

        public String getBusiId() {
            return busiId;
        }

        public String getBnm() {
            return bnm;
        }

        public String getBusiNm() {
            return busiNm;
        }

        public String getBusiCall() {
            return busiCall;
        }

        public int getStat() {
            return stat;
        }

        public String getStatUpdDt() {
            return statUpdDt;
        }

        public String getLastTsdt() {
            return lastTsdt;
        }

        public String getLastTedt() {
            return lastTedt;
        }

        public String getNowTsdt() {
            return nowTsdt;
        }

        public String getPowerType() {
            return powerType;
        }

        public String getOutput() {
            return output;
        }

        public String getMethod() {
            return method;
        }

        public String getZcode() {
            return zcode;
        }

        public boolean isParkingFree() {
            return parkingFree;
        }

        public String getNode() {
            return node;
        }

        public boolean isLimitYn() {
            return limitYn;
        }

        public String getLimitDetail() {
            return limitDetail;
        }



        public int getDistance() {
            return distance;
        }
        public void setDistance(int distance) {
            this.distance = distance;
        }

        private int distance;


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

    public static class XmlEvPullParserHandler {
        public XmlEvPullParserHandler() {
            //default constructor left empty
        }
        public List<EvStationInfo> parseEvStationInfo(InputStream is) {
            List<EvStationInfo> evList = new ArrayList<>();
            try (InputStream inputStream = is) {
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                XmlPullParser parser = factory.newPullParser();
                parser.setInput(inputStream, "utf-8");
                int eventType = parser.getEventType();

                String tagName = "";
                EvStationInfo evInfo = null;

                while(eventType != XmlPullParser.END_DOCUMENT) {
                    switch(eventType) {
                        case XmlPullParser.START_TAG:
                            tagName = parser.getName();
                            if(tagName.equalsIgnoreCase("item")) evInfo = new EvStationInfo();
                            break;

                        case XmlPullParser.END_TAG:
                            if(parser.getName().equalsIgnoreCase("item")) evList.add(evInfo);
                            break;

                        case XmlPullParser.TEXT:
                            if(evInfo == null) break;
                            switch(tagName) {
                                case "statNm":
                                    evInfo.setEvName(parser.getText());
                                    break;
                                case "lat":
                                    evInfo.setLat(Double.parseDouble(parser.getText()));
                                    break;
                                case "lng":
                                    evInfo.setLng(Double.parseDouble(parser.getText()));
                                    break;
                                case "chgerId":
                                    evInfo.setChargerId(parser.getText());
                                    break;
                                case "chgerType":
                                    evInfo.setChargerType(parser.getText());
                                    break;
                                case "stat":
                                    evInfo.setChargerStatus(parser.getText());
                                    break;
                                case "limitYn":
                                    evInfo.setIsPublic(parser.getText());
                                    break;
                                case "limitDetail":
                                    evInfo.setLimitDetail(parser.getText());
                                    break;
                            }
                            break;
                    }

                    eventType = parser.next();
                }
            } catch (XmlPullParserException | IOException e) { e.printStackTrace();}

            return evList;
        }
    }

    public static class EvStationInfo {
        private String evName;
        private String chargerId;
        private double lat;
        private double lng;
        private int distance;
        private String chargerType;
        private String chargerStatus;
        private String isPublic;
        private String limitDetail;

        public EvStationInfo() {}

        public double getLat() {
            return lat;
        }
        public void setLat(double lat) {
            this.lat = lat;
        }

        public double getLng() {
            return lng;
        }
        public void setLng(double lng) {
            this.lng = lng;
        }

        public String getEvName() {
            return evName;
        }
        public void setEvName(String evName) {
            this.evName = evName;
        }

        public int getDistance() {return distance;}
        public void setDistance(int distance) {this.distance = distance;}

        public String getChargerId() {
            return chargerId;
        }
        public void setChargerId(String chargerId) {
            this.chargerId = chargerId;
        }

        public String getChargerType() {
            return chargerType;
        }
        public void setChargerType(String chgrType) {
            this.chargerType = convChargerType(chgrType);
        }

        public String getChargerStatus() {
            return chargerStatus;
        }
        public void setChargerStatus(String code) {
            this.chargerStatus = convChargerStatus(code);
        }

        public String getIsPublic() {
            return isPublic;
        }
        public void setIsPublic(String isPublic) {
            this.isPublic = isPublic;
        }

        public String getLimitDetail() {
            log.i("getLimitDetail: %s", this.limitDetail);
            return limitDetail;
        }

        public void setLimitDetail(String limitDetail) {
            this.limitDetail = limitDetail;
        }
    }

    private int convSidoCode(String sido) {
        switch(sido) {
            case "서울특별시": return 11; case "부산광역시": return 26; case "대구광역시": return 27;
            case "인천광역시": return 28; case "광주광역시": return 29; case "대전광역시": return 30;
            case "울산광역시": return 31; case "세종특별자치시": return 36; case "경기도": return 41;
            case "강원도": return 42; case "충청북도": return 43; case "충청남도": return 44;
            case "전라북도": return 45; case "전라남도": return 46; case "경상북도": return 47;
            case "경상남도": return 48; case "제주특별자치도": return 50;
            default: return -1;
        }
    }

    private static String convChargerType(String code) {
        switch(code) {
            case "01" : return "DC차 데모";
            case "02" : return "AC 완속";
            case "03" : return "DC차데모+AC3상";
            case "04" : return "DC콤보";
            case "05" : return "DC차데모+DC콤보";
            case "06" : return "DC차데모+AC3상+DC콤보";
            case "07" : return "AC3상";
            default : return "N/A";
        }
    }

    private static String convChargerStatus(String code) {
        switch(code) {
            case "1" : return "통신이상";
            case "2" : return "충전대기";
            case "3" : return "충전중";
            case "4" : return "운영중지";
            case "5" : return "점검중";
            case "9" : return "상태미확인";
            default: return "N/A";
        }
    }










}
