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

/*
 * This class is to get the Sigun codes based on the Sido code defined in the string-array from the
 * Opinet. Once downloading the Sigun codes completes, it will be saved in the internal storage.
 */
public class DistrictCodeRunnable implements Runnable {

    // Logging
    private final LoggingHelper log = LoggingHelperFactory.create(DistrictCodeRunnable.class);

    // Constants
    private static final String API_KEY = "F186170711";
    private static final String OPINET = "http://www.opinet.co.kr/api/areaCode.do?out=xml&code=" + API_KEY;
    private static final String OPINET_AREA = OPINET + "&area=";

    //static final int DOWNLOAD_DISTCODE_SUCCEED = 1;
    //static final int DOWNLOAD_DISTCODE_FAIL = -1;

    // Objects
    private Context context;
    private OpinetDistCodeMethods mTask;
    private List<Opinet.DistrictCode> distCodeList;

    // Interface
    public interface OpinetDistCodeMethods {
        void setDistCodeDownloadThread(Thread currentThread);
        void hasDistCodeSaved(boolean b);
        //void handleDistCodeTask(int state);
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

        for(String sidoCode : sido) {
            try {
                if (Thread.interrupted()) throw new InterruptedException();
                final URL url = new URL(OPINET_AREA + sidoCode);

                conn = (HttpURLConnection)url.openConnection();
                conn.setRequestProperty("Connection", "close");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.connect();
                bis = new BufferedInputStream(conn.getInputStream());
                distCodeList = xmlHandler.parseDistrictCode(bis);

            } catch(InterruptedException e) {
                log.e("Thread Interrupted: " + e);
            } catch(IOException e) {
                log.e("InputStream failed: " + e);
            } finally {
                try {
                    if (bis != null) bis.close();
                } catch(IOException e) {
                    e.printStackTrace();
                }
                if(conn != null) conn.disconnect();

                // Save the list of Opinet.DistrictCode in the internal file storage
                if(distCodeList != null) {
                    boolean isSaved = saveDistCode(distCodeList);
                    if(isSaved) mTask.hasDistCodeSaved(true);
                    else mTask.hasDistCodeSaved(false);
                }
            }



            /*
            if(saveDistCode(distCodeList)){
                log.d("Sigun Numbers: %d", distCodeList.size());
                for(Opinet.DistrictCode sigunCode : distCodeList)
                    log.i("Dist Code : %s, %s", sigunCode.getDistrictCode(), sigunCode.getDistrictName());
                mTask.hasDistCodeSaved(true);
            } else mTask.hasDistCodeSaved(false);
             */
        }
    }

    private boolean saveDistCode(List<Opinet.DistrictCode> list) {
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
