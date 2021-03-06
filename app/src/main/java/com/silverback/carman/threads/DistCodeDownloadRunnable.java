package com.silverback.carman.threads;

import android.content.Context;
import android.os.Process;

import com.silverback.carman.R;
import com.silverback.carman.utils.Constants;
import com.silverback.carman.viewmodels.Opinet;
import com.silverback.carman.viewmodels.XmlPullParserHandler;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/*
 * This class is to get the Sigun codes based on the Sido code defined in the string-array from the
 * Opinet. Once downloading the Sigun codes completes, it will be saved in the internal storage.
 */
public class DistCodeDownloadRunnable implements Runnable {

    // Logging
    //private final LoggingHelper log = LoggingHelperFactory.create(DistCodeDownloadRunnable.class);

    static final int TASK_COMPLETE = 1;
    static final int TASK_FAIL = -1;

    // Constants. API 29 and higher requires http to be https.
    private static final String API_KEY = "F186170711";
    private static final String OPINET = "https://www.opinet.co.kr/api/areaCode.do?out=xml&code=" + API_KEY;
    private static final String OPINET_AREA = OPINET + "&area=";

    // Objects
    private final Context context;
    private final OpinetDistCodeMethods mTask;

    // Interface
    public interface OpinetDistCodeMethods {
        void setDistCodeDownloadThread(Thread currentThread);
        void hasDistCodeSaved(boolean b);
        void handleDistCodeTask(int state);
    }

    // Constructor
    DistCodeDownloadRunnable(Context context, OpinetDistCodeMethods task) {
        this.context = context;
        mTask = task;

    }

    @Override
    public void run() {
        mTask.setDistCodeDownloadThread(Thread.currentThread());
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

        final String[] sido = context.getResources().getStringArray(R.array.sido_code);
        HttpURLConnection conn = null;
        BufferedInputStream bis = null;
        List<Opinet.DistrictCode> distCodeList = new ArrayList<>();
        XmlPullParserHandler xmlHandler = new XmlPullParserHandler(distCodeList);

        try{
            for(String sidoCode : sido) {
                if (Thread.interrupted()) throw new InterruptedException();
                final URL url = new URL(OPINET_AREA + sidoCode);

                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("Connection", "close");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.connect();

                bis = new BufferedInputStream(conn.getInputStream());
                distCodeList = xmlHandler.parseDistrictCode(bis);
            }

            // Save the list of Opinet.DistrictCode in the storage
            if(distCodeList.size() > 0) {
                boolean isSaved = saveDistCode(distCodeList);
                mTask.hasDistCodeSaved(isSaved);
                int state = (isSaved)? TASK_COMPLETE : TASK_FAIL;
                mTask.handleDistCodeTask(state);
            } else {
                mTask.hasDistCodeSaved(false);
                mTask.handleDistCodeTask(TASK_FAIL);
            }

        } catch(InterruptedException | IOException e) {
            e.printStackTrace();
        } finally {
            try {if (bis != null) bis.close();}
            catch(IOException e) { e.printStackTrace();}
            if(conn != null) conn.disconnect();
        }
    }

    // Method to save the district code downloaded from the Opinet in the internal storage
    private boolean saveDistCode(List<Opinet.DistrictCode> list) {
        File file = new File(context.getFilesDir(), Constants.FILE_DISTRICT_CODE);
        try(FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(list);
            return true;
        } catch (IOException e) { e.printStackTrace();}

        return false;
    }
}
