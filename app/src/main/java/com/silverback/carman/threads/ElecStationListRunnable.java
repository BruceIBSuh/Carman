package com.silverback.carman.threads;

import android.location.Location;
import android.os.Process;

import com.silverback.carman.coords.GeoPoint;
import com.silverback.carman.coords.GeoTrans;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class ElecStationListRunnable implements Runnable{

    private static final LoggingHelper log = LoggingHelperFactory.create(ElecStationListRunnable.class);
    private final String api = "http://apis.data.go.kr/B552584/EvCharger/getChargerStatus";
    private final String key = "Wd%2FkK0BbiWJlv1Rj9oR0Q7WA0aQ0UO3%2FY11uMkriK57e25VBUaNk1hQxQWv0svLZln5raxjA%2BFuCXzqm8pWu%2FQ%3D%3D";


    private ElecStationCallback callback;

    // Interface
    public interface ElecStationCallback {
        void setElecStationTaskThread(Thread thread);
        Location getElecStationLocation();
    }

    public ElecStationListRunnable(ElecStationCallback callback) {
        this.callback = callback;

    }

    @Override
    public void run() {
        callback.setElecStationTaskThread(Thread.currentThread());
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        log.i("EV stations thread: %s", Thread.currentThread());

        Location location = callback.getElecStationLocation();
        GeoPoint in_pt = new GeoPoint(location.getLongitude(), location.getLatitude());
        GeoPoint tm_pt = GeoTrans.convert(GeoTrans.GEO, GeoTrans.TM, in_pt);
        GeoPoint katec_pt = GeoTrans.convert(GeoTrans.TM, GeoTrans.KATEC, tm_pt);
        float x = (float) katec_pt.getX();
        float y = (float) katec_pt.getY();

        StringBuilder sb = new StringBuilder(api); /*URL*/
        try {
            sb.append("?").append(URLEncoder.encode("serviceKey", "UTF-8"));
            sb.append("=").append(key); /*Service Key*/
            sb.append("&").append(URLEncoder.encode("pageNo", "UTF-8")).append("=").append(URLEncoder.encode("1", "UTF-8")); /*페이지 번호*/
            sb.append("&").append(URLEncoder.encode("numOfRows", "UTF-8")).append("=").append(URLEncoder.encode("100", "UTF-8")); /*한 페이지 결과 수 (최소 10, 최대 9999)*/
            sb.append("&").append(URLEncoder.encode("period", "UTF-8")).append("=").append(URLEncoder.encode("5", "UTF-8")); /*상태갱신 조회 범위(분) (기본값 5, 최소 1, 최대 10)*/
            sb.append("&").append(URLEncoder.encode("zcode", "UTF-8")).append("=").append(URLEncoder.encode("11", "UTF-8")); /*시도 코드 (행정구역코드 앞 2자리)*/
            URL url = new URL(sb.toString());

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Content-type", "application/json");

            log.i("URL: %s", url);

        } catch(IOException e) {
            e.printStackTrace();
        }
    }
}
