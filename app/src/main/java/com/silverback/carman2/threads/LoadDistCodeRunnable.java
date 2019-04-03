package com.silverback.carman2.threads;

import android.content.Context;
import android.net.Uri;

import com.silverback.carman2.adapters.DistrictSpinnerAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Constants;
import com.silverback.carman2.models.Opinet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.List;

public class LoadDistCodeRunnable implements Runnable {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(LoadDistCodeRunnable.class);

    // Constants
    static final int SPINNER_DIST_CODE_COMPLETE = 1;
    static final int SPINNER_DIST_CODE_FAIL = -1;

    // Objects
    private Context context;
    private DistCodeMethods task;

    public interface DistCodeMethods {
        int getSidoCode();
        DistrictSpinnerAdapter getSpinnerAdapter();
        void setSpinnerDistCodeThread(Thread currentThread);
        void handleSpinnerDistCodeTask(int state);
    }

    LoadDistCodeRunnable(Context context, DistCodeMethods task) {
        this.context = context;
        this.task = task;
    }


    @SuppressWarnings("unchecked")
    @Override
    public void run() {

        task.setSpinnerDistCodeThread(Thread.currentThread());
        int code = task.getSidoCode();


        // Make int position to String sidoCode
        String sidoCode = convertCode(code);
        log.i("SidoCode: %s", sidoCode);

        //List<Opinet.DistrictCode> distCodeList = new ArrayList<>();
        DistrictSpinnerAdapter adapter = task.getSpinnerAdapter();
        if(adapter.getCount() > 0) adapter.removeAll();

        File file = new File(context.getFilesDir(), Constants.FILE_DISTRICT_CODE);
        Uri uri = Uri.fromFile(file);

        try(InputStream is = context.getContentResolver().openInputStream(uri);
            ObjectInputStream ois = new ObjectInputStream(is)) {

            for(Opinet.DistrictCode obj : (List<Opinet.DistrictCode>)ois.readObject()) {
                if(obj.getDistrictCode().substring(0, 2).equals(sidoCode)) {
                    //distCodeList.add(obj);
                    //adapter.addItem(obj.getDistrictName());
                    adapter.addItem(obj);
                }
            }

            task.handleSpinnerDistCodeTask(SPINNER_DIST_CODE_COMPLETE);

        } catch (IOException e) {
            log.w("IOException: %s", e.getMessage());
            task.handleSpinnerDistCodeTask(SPINNER_DIST_CODE_FAIL);
        } catch (ClassNotFoundException e) {
            log.w("ClassNotFoundException: %s", e.getMessage());
            task.handleSpinnerDistCodeTask(SPINNER_DIST_CODE_FAIL);
        }
    }

    // Converts a position set by SidoSpinner to a Sido string format.
    private String convertCode(int code) {
        String sidoCode;
        switch(code) {
            case 0: sidoCode = "01"; break; case 1: sidoCode = "02"; break;
            case 2: sidoCode = "03"; break; case 3: sidoCode = "04"; break;
            case 4: sidoCode = "05"; break; case 5: sidoCode = "06"; break;
            case 6: sidoCode = "07"; break; case 7: sidoCode = "08"; break;
            case 8: sidoCode = "09"; break; case 9: sidoCode = "10"; break;
            case 10: sidoCode = "11"; break; case 11: sidoCode = "14"; break;
            case 12: sidoCode = "15"; break; case 13: sidoCode = "16"; break;
            case 14: sidoCode = "17"; break; case 15: sidoCode = "18"; break;
            case 16: sidoCode = "19"; break; default: sidoCode = "01"; break;
        }

        return sidoCode;
    }
}