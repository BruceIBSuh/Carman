package com.silverback.carman.threads;

import android.content.Context;
import android.net.Uri;

import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.Constants;
import com.silverback.carman.viewmodels.Opinet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

public class DistCodeSpinnerRunnable implements Runnable {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(DistCodeSpinnerRunnable.class);

    // Constants
    static final int SPINNER_DIST_CODE_COMPLETE = 1;
    static final int SPINNER_DIST_CODE_FAIL = -1;

    // Objects
    private final Context context;
    private final DistCodeMethods task;

    public interface DistCodeMethods {
        int getSidoCode();
        void setSigunCode(List<Opinet.DistrictCode> distCode);
        void setSpinnerDistCodeThread(Thread currentThread);
        void handleDistCodeSpinnerTask(int state);
    }

    DistCodeSpinnerRunnable(Context context, DistCodeMethods task) {
        this.context = context;
        this.task = task;
    }

    @Override
    public void run() {
        task.setSpinnerDistCodeThread(Thread.currentThread());
        int code = task.getSidoCode();

        // Make int position to String sidoCode
        final String sidoCode = convertCode(code);
        List<Opinet.DistrictCode> distCodeList = new ArrayList<>();

        File file = new File(context.getFilesDir(), Constants.FILE_DISTRICT_CODE);
        Uri uri = Uri.fromFile(file);
        try(InputStream is = context.getContentResolver().openInputStream(uri);
            ObjectInputStream ois = new ObjectInputStream(is)) {
            // To prevent ObjectInputStream.readObject() from incurring unchecked cast when the object
            // is directly cast to List<Opinet.DistrictCode>, the object should be deserialized first.
            Object objList = ois.readObject();
            if(objList instanceof ArrayList<?>) {
                for(Object distcode : (ArrayList<?>)objList) {
                    if(distcode instanceof Opinet.DistrictCode) {
                        if(((Opinet.DistrictCode) distcode).getDistrictCode().substring(0, 2).equals(sidoCode)){
                            distCodeList.add((Opinet.DistrictCode) distcode);
                        }
                    }
                }
            }

            log.i("distcodelist: %s", distCodeList.size());
            task.setSigunCode(distCodeList);
            task.handleDistCodeSpinnerTask(SPINNER_DIST_CODE_COMPLETE);

            // Post(Set) value in SpinnerDistriceModel, which is notified to the parent fragment,
            // SettingSpinnerDlgFragment as LiveData.
            //task.getSpinnerDistrictModel().getSpinnerDataList().postValue(distCodeList);

        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            e.printStackTrace();
            task.handleDistCodeSpinnerTask(SPINNER_DIST_CODE_FAIL);
        }
    }

    // Converts a position set by SidoSpinner to a Sido string format to handle exceptions that the
    // item position is not identical with the Sido code.
    private String convertCode(int code) {
        String sidoCode = "01"; //default value in case the method fails to receive any code.
        switch(code) {
            case 0: sidoCode = "01"; break; case 1: sidoCode = "02"; break;
            case 2: sidoCode = "03"; break; case 3: sidoCode = "04"; break;
            case 4: sidoCode = "05"; break; case 5: sidoCode = "06"; break;
            case 6: sidoCode = "07"; break; case 7: sidoCode = "08"; break;
            case 8: sidoCode = "09"; break; case 9: sidoCode = "10"; break;
            case 10: sidoCode = "11"; break; case 11: sidoCode = "14"; break;
            case 12: sidoCode = "15"; break; case 13: sidoCode = "16"; break;
            case 14: sidoCode = "17"; break; case 15: sidoCode = "18"; break;
            case 16: sidoCode = "19"; break;
        }

        return sidoCode;
    }
}
