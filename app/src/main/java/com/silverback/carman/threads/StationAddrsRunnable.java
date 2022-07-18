package com.silverback.carman.threads;

import static com.silverback.carman.threads.StationEvTask.EV_ADDRS_TASK_FAIL;
import static com.silverback.carman.threads.StationEvTask.EV_ADDRS_TASK_SUCCESS;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Process;

import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class StationAddrsRunnable implements Runnable {

    private static final LoggingHelper log = LoggingHelperFactory.create(StationAddrsRunnable.class);

    private final Geocoder geoCoder;
    private final StationAddrsCallback callback;
    private EvSidoCode evSidoCode;


    public interface StationAddrsCallback {
        Location getEvStationLocation();
        void setStationAddrsThread(Thread thread);
        void setEnumEvSidoCode(EvSidoCode evSidoCode);
        void handleTaskState(int state);
    }


    public StationAddrsRunnable(Context context, StationAddrsCallback callback) {
        this.callback = callback;
        geoCoder = new Geocoder(context, Locale.ENGLISH);

    }

    @Override
    public void run() {
        callback.setStationAddrsThread(Thread.currentThread());
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

        Location location = callback.getEvStationLocation();
        EvSidoCode code = getAddressfromLocation(location.getLatitude(), location.getLongitude());
        if(code != null) {
            callback.setEnumEvSidoCode(code);
            callback.handleTaskState(EV_ADDRS_TASK_SUCCESS);
        } else callback.handleTaskState(EV_ADDRS_TASK_FAIL);
    }

    // Refactor required as of Android13(Tiramisu), which has added the listener for getting the
    // address done.
    private EvSidoCode getAddressfromLocation(double lat, double lng) {
        try {
            List<Address> addressList = geoCoder.getFromLocation(lat, lng, 1);
            if(addressList.size() > 0) {
                String sido = addressList.get(0).getAdminArea().replaceAll("[\\s\\-]", "");
                return EvSidoCode.valueOf(sido);
            }
        } catch(IOException e) { e.printStackTrace(); }
        return null;
    }

    public enum EvSidoCode {
        Seoul(11, 23059), Busan(26, 6467), Daegu(27, 1984), Incheon(28, 5358), Gwangju(29, 3860),
        Daejeon(30, 3321), Ulsan(31, 1984), Sejong(36, 5519),
        Gyeonggi(41, 31202),
        Chungcheongbukdo(43, 3937), Chungcheongnamdo(44, 5519),
        Gyeongsangbukdo(47, 7155), Gyeongsangnamdo(48, 6862),
        Jeollabukdo(45, 4261), Jeollanamdo(46, 4139),
        Jejudo(50, 5249);

        private final int code;
        private final int number;
        EvSidoCode(int code, int number) {
            this.code = code;
            this.number = number;
        }

        public int getCode() { return code; }
        public int getEvNumber() { return number; }

    }
}
