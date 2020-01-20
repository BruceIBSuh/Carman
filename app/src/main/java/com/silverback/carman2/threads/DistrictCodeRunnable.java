package com.silverback.carman2.threads;

import android.content.Context;
import android.os.Process;

import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.utils.Constants;
import com.silverback.carman2.models.Opinet;
import com.silverback.carman2.models.XmlPullParserHandler;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public class DistrictCodeRunnable implements Runnable {

    // Logging
    private final LoggingHelper log = LoggingHelperFactory.create(DistrictCodeRunnable.class);

    // Constants
    private static final String API_KEY = "F186170711";
    private static final String OPINET = "http://www.opinet.co.kr/api/areaCode.do?out=xml&code=" + API_KEY;
    private static final String OPINET_AREA = OPINET + "&area=";

    static final int DOWNLOAD_DISTCODE_SUCCEED = 1;
    static final int DOWNLOAD_DISTCODE_FAIL = -1;

    // Objects
    private Context context;
    private OpinetDistCodeMethods mTask;
    private List<Opinet.DistrictCode> distCodeList;

    // Interface
    public interface OpinetDistCodeMethods {
        void setDistCodeDownloadThread(Thread currentThread);
        void hasDistCodeSaved(boolean b);
        void handleDistCodeTask(int state);
    }

    // Constructor
    DistrictCodeRunnable(Context context, OpinetDistCodeMethods task) {
        this.context = context;
        mTask = task;

    }

    @Override
    public void run() {
        mTask.setDistCodeDownloadThread(Thread.currentThread());
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        // Get the District Sido codes from the resource.
        final String[] sido = context.getResources().getStringArray(R.array.sido_code);

        HttpURLConnection conn = null;
        BufferedInputStream bis = null;
        XmlPullParserHandler xmlHandler = new XmlPullParserHandler();

        try {
            // Get all siguncodes at a time with all sido codes given
            for(String code : sido) {
                if(Thread.interrupted()) {
                    throw new InterruptedException();
                }

                final URL url = new URL(OPINET_AREA.concat(code));
                conn = (HttpURLConnection)url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Content-type", "text/xml");
                conn.setDoOutput(true);
                conn.setDoInput(true);
                conn.setRequestProperty("Connection", "close");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.connect();
                bis = new BufferedInputStream(conn.getInputStream());
                //bis = new BufferedInputStream(url.openStream());
                distCodeList = xmlHandler.parseDistrictCode(bis);
            }

            if(saveDistCode(distCodeList)){
                log.d("Sigun Numbers: %d", distCodeList.size());
                mTask.hasDistCodeSaved(true);
                //mTask.handleDistCodeTask(DOWNLOAD_DISTCODE_SUCCEED);

            } else mTask.hasDistCodeSaved(false);

        } catch (IOException e) {
            log.e("InputStream failed: " + e);
        } catch (InterruptedException e) {
            log.e("Thread Interrupted: " + e);
        } finally {
            try {
                if (bis != null) bis.close();
            } catch(IOException e) {
                e.printStackTrace();
            }

            if(conn != null) conn.disconnect();
        }

    }

    private boolean saveDistCode(List<Opinet.DistrictCode> list) {
        log.i("saveDistCode: %s", list.size());
        File file = new File(context.getFilesDir(), Constants.FILE_DISTRICT_CODE);
        try(FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(list);
            return true;
        } catch (FileNotFoundException e) {
            log.e("FileNotFoundException: %s", e.getMessage());
        } catch (IOException e) {
            log.e("IOException: %s", e.getMessage());
        }

        return false;
    }
}
