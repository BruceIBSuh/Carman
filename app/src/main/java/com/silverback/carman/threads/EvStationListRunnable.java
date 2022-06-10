package com.silverback.carman.threads;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Process;
import android.text.TextUtils;

import com.silverback.carman.adapters.GasStationListAdapter;
import com.silverback.carman.coords.GeoPoint;
import com.silverback.carman.coords.GeoTrans;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.Constants;
import com.silverback.carman.viewmodels.Opinet;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class EvStationListRunnable implements Runnable{

    private static final LoggingHelper log = LoggingHelperFactory.create(EvStationListRunnable.class);
    private final String evUrl = "http://apis.data.go.kr/B552584/EvCharger";

    private final String evStatus = "http://apis.data.go.kr/B552584/EvCharger/getChargerStatus";
    private final String evInfo = "http://apis.data.go.kr/B552584/EvCharger/getChargerInfo";
    private final String key = "Wd%2FkK0BbiWJlv1Rj9oR0Q7WA0aQ0UO3%2FY11uMkriK57e25VBUaNk1hQxQWv0svLZln5raxjA%2BFuCXzqm8pWu%2FQ%3D%3D";
    private final String code ="Wd/kK0BbiWJlv1Rj9oR0Q7WA0aQ0UO3/Y11uMkriK57e25VBUaNk1hQxQWv0svLZln5raxjA+FuCXzqm8pWu/Q==";

    private final Geocoder geocoder;
    private final ElecStationCallback callback;

    // Interface
    public interface ElecStationCallback {
        void setElecStationTaskThread(Thread thread);
        Location getElecStationLocation();
        void setEvStationList(List<EvStationInfo> evList);
        void notifyEvStationError(Exception e);
    }

    public EvStationListRunnable(Context context, ElecStationCallback callback) {
        this.callback = callback;
        geocoder = new Geocoder(context, Locale.KOREAN);
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
        StringBuilder sb = new StringBuilder(evInfo); /*URL*/
        try {
            sb.append("?").append(URLEncoder.encode("serviceKey", "UTF-8"));
            sb.append("=").append(URLEncoder.encode(code, "UTF-8")); /*Service Key*/
            sb.append("&").append(URLEncoder.encode("pageNo", "UTF-8"));
            sb.append("=").append(URLEncoder.encode("1", "UTF-8")); /*페이지 번호*/
            sb.append("&").append(URLEncoder.encode("numOfRows", "UTF-8"));
            sb.append("=").append(URLEncoder.encode("9999", "UTF-8")); /*한 페이지 결과 수 (최소 10, 최대 9999)*/
            sb.append("&").append(URLEncoder.encode("period", "UTF-8"));
            sb.append("=").append(URLEncoder.encode("5", "UTF-8")); /*상태갱신 조회 범위(분) (기본값 5, 최소 1, 최대 10)*/
            sb.append("&").append(URLEncoder.encode("zcode", "UTF-8"));
            sb.append("=").append(URLEncoder.encode(sidoCode, "UTF-8")); /*시도 코드 (행정구역코드 앞 2자리)*/

            XmlEvPullParserHandler xmlHandler = new XmlEvPullParserHandler();
            URL url = new URL(sb.toString());
            log.i("EV url: %s", url);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            //conn.setRequestProperty("Content-type", "application/json");
            conn.setRequestProperty("Connection", "close");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.connect();

            List<EvStationInfo> evStationList = new ArrayList<>();
            try(InputStream is = new BufferedInputStream(conn.getInputStream())) {
                for(EvStationInfo info : xmlHandler.parseEvStationInfo(is)) {
                    float[] results = new float[3];
                    Location.distanceBetween(location.getLatitude(), location.getLongitude(),
                            info.getLat(), info.getLng(), results);

                    int distance = (int) results[0];
                    // Get EV stations within 3000m out of retrieved ones.
                    if (distance < 2500) {
                        info.setDistance(distance);
                        evStationList.add(info);
                    }
                }

                // Sort EvList in the distance-descending order
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                    Collections.sort(evStationList, Comparator.comparingInt(t -> (int) t.getDistance()));
                else Collections.sort(evStationList, (t1, t2) ->
                            Integer.compare((int)t1.getDistance(), (int)t2.getDistance()));

                callback.setEvStationList(evStationList);

            } catch(IOException e) {
                callback.notifyEvStationError(e);
                e.printStackTrace();

            } finally {conn.disconnect();}

        } catch(IOException e) {
            e.getLocalizedMessage();
        }
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
