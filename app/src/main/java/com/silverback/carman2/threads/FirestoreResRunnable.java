package com.silverback.carman2.threads;

import android.content.Context;
import android.os.Process;
import android.util.Base64OutputStream;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;
import com.silverback.carman2.database.AutoDataEntity;
import com.silverback.carman2.database.CarmanDatabase;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.utils.Constants;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class FirestoreResRunnable implements Runnable {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(FirestoreResRunnable.class);

    // Constants
    static final int DOWNLOAD_COMPLETED = 1;
    static final int SAVE_COMPLETED = 2;
    static final int DOWNLAOD_FAILED = -1;


    // Objects
    private Context context;
    private FirebaseFirestore firestore;
    private FirestoreResMethods mCallback;
    private CarmanDatabase mDB;
    private AutoDataEntity entity;
    private List<AutoData> autoList;
    private AutoData autoData;

    // Fields
    private int rowId;

    // Interface
    public interface FirestoreResMethods {
        void setResourceThread(Thread thead);
        void handleState(int state);
    }

    // Constructor
    public FirestoreResRunnable(Context context, FirestoreResMethods callback) {
        this.context = context;
        firestore = FirebaseFirestore.getInstance();
        mDB = CarmanDatabase.getDatabaseInstance(context);
        mCallback = callback;
    }


    @SuppressWarnings("ConstantConditions")
    @Override
    public void run() {
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        mCallback.setResourceThread(Thread.currentThread());

        mDB.autoDataModel().deleteAllData();
        entity = new AutoDataEntity();

        autoList = new ArrayList<>();
        final List<String> autoMakerList = new ArrayList<>();
        final CollectionReference autoDataRef = firestore.collection("autodata");


        autoDataRef.get().continueWith(task -> {
            if(task.isSuccessful()) return task.getResult();
            else return task.getResult(IOException.class);

        }).continueWith(task -> {
            for(QueryDocumentSnapshot automaker : task.getResult())
                autoMakerList.add(automaker.getString("auto_maker"));

            return autoMakerList;

        }).addOnCompleteListener(automakerList -> {

            for(String autoMaker : automakerList.getResult()) {

                autoDataRef.whereEqualTo("auto_maker", autoMaker).get().continueWith(task -> {

                    for(QueryDocumentSnapshot doc : task.getResult()) {
                        doc.getReference().collection("auto_model").get().addOnSuccessListener(models -> {
                            for(QueryDocumentSnapshot model : models) {
                                log.i("auto model: %s", model.getString("model_name"));
                                entity = new AutoDataEntity();
                                entity.autoMaker = autoMaker;
                                entity.autoModel = doc.getString("model_name");
                                int row = mDB.autoDataModel().insertAutoData(entity);
                                log.i("insert data: %s", row);
                            }
                        });
                    }

                    return rowId;

                }).addOnCompleteListener(rowId -> log.i("Insert task done:%s", rowId));
            }
        });

        /*
        // Query auto makers and add them to autoMakerList.
        autoDataRef.get().addOnSuccessListener(snapshots -> {
            for (QueryDocumentSnapshot automaker : snapshots) {
                log.i("Auto Maker: %s", automaker.getString("auto_maker"));
                autoMakerList.add(automaker.getString("auto_maker"));
            }

            // Query auto models with
            for(String autoMaker : autoMakerList) {
                autoDataRef.whereEqualTo("auto_maker", autoMaker).get().addOnSuccessListener(makers -> {
                    for(QueryDocumentSnapshot document : makers) {
                        document.getReference().collection("auto_model").get().addOnSuccessListener(models -> {
                            for(QueryDocumentSnapshot doc : models) {
                                String model = doc.getString("model_name");
                                log.i("model name: %s", model);
                                autoData = new AutoData(autoMaker, model, 0);
                                autoData.setAutoMaker(autoMaker);
                                autoData.setAutoModel(model);
                                autoData.setAutoType(0);
                                autoList.add(autoData);

                                entity = new AutoDataEntity();
                                entity.autoMaker = autoMaker;
                                entity.autoModel = doc.getString("model_name");
                                if(doc.getLong("model_type") != null)
                                    entity.autoType =  doc.getLong("model_type").intValue();
                                log.i("Entity: %s, %s", entity.autoMaker, entity.autoModel, entity.autoType);

                                //rowId = mDB.autoDataModel().insertAutoData(entity);
                                log.i("rowId: %s", rowId);

                            }

                        }).addOnFailureListener(e -> {
                            mCallback.handleState(DOWNLAOD_FAILED);
                            log.i("Fail: %s", e.getMessage());
                        });
                    }
                });
            }

            if(rowId > 0) {
                mCallback.handleState(DOWNLOAD_COMPLETED);
                log.i("rowId: %s", rowId);

            }
            for(AutoData data : autoList) log.i("AutoData: %s, %s", data.getAutoMaker(), data.autoModel);
            saveAutoDataToFile();

        }).addOnFailureListener(e -> mCallback.handleState(DOWNLAOD_FAILED));
        */

    }

    // Serialize the auto data to save it in the file.
    class AutoData implements Serializable {
        private String autoMaker;
        private String autoModel;
        private int autoType;

        AutoData(String maker, String model, int type) {
            this.autoMaker = maker;
            this.autoModel = model;
            this.autoType = type;
        }

        String getAutoMaker() {
            return autoMaker;
        }

        void setAutoMaker(String autoMaker) {
            this.autoMaker = autoMaker;
        }

        String getAutoModel() {
            return autoModel;
        }

        void setAutoModel(String autoModel) {
            this.autoModel = autoModel;
        }

        public int getAutoType() {
            return autoType;
        }

        void setAutoType(int autoType) {
            this.autoType = autoType;
        }

        @NonNull
        @Override
        public String toString() {
            log.i("Auto Data: %s, %s", getAutoMaker(), getAutoModel());
            return getAutoMaker() + getAutoModel();
        }

    }


    private void saveAutoDataToFile() {
        File file = new File(context.getFilesDir(), Constants.AUTO_DATA);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try(FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream oos = new ObjectOutputStream(bos)){
            oos.writeObject(autoList);
            byte[] data = bos.toByteArray();
            bos.close();

            // Create a String from the object using Base64 encoding.
            bos = new ByteArrayOutputStream();
            Base64OutputStream b64 = new Base64OutputStream(bos, 0);
            b64.write(data);
            bos.close();

            fos.write(data);


        } catch(IOException e) {
            e.printStackTrace();
        }
    }

}
