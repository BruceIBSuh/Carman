package com.silverback.carman2.threads;

import android.content.Context;
import android.os.Process;
import android.util.Base64OutputStream;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.silverback.carman2.database.AutoDataMakerEntity;
import com.silverback.carman2.database.AutoDataModelEntity;
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

public class AutoDataResourceRunnable implements Runnable {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(AutoDataResourceRunnable.class);

    // Constants
    static final int DOWNLOAD_COMPLETED = 1;
    static final int DOWNLOAD_FAILED = -1;


    // Objects
    private Context context;
    private FirebaseFirestore firestore;
    private FirestoreResMethods mCallback;
    private CarmanDatabase mDB;
    private AutoDataMakerEntity autoMakerEntity;
    private AutoDataModelEntity autoModelEntity;
    private List<AutoData> autoList;
    //private AutoData autoData;

    // Fields
    //private int rowId;

    // Interface
    public interface FirestoreResMethods {
        void setResourceThread(Thread thead);
        void handleState(int state);
    }

    // Constructor
    AutoDataResourceRunnable(Context context, FirestoreResMethods callback) {
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

        if(mDB.autoDataModel().getAutoDataModelNum() > 0) mDB.autoDataModel().deleteModelData();
        if(mDB.autoDataModel().getAutoDataMakerNum() > 0) mDB.autoDataModel().deleteMakerData();

        autoMakerEntity = new AutoDataMakerEntity();
        autoList = new ArrayList<>();
        final List<String> autoMakerList = new ArrayList<>();
        final CollectionReference autoDataRef = firestore.collection("autodata");


        // The query process mainly consists of two asynced parts.
        // First, retrieve all auto makers to contain in ArrayList.
        // Second, while looping the list, query the auto model based on a auto maker given by loop.
        // Third, with the auto model as a result of successful query, another query should be made
        // to retrieve the model name and other fields, which go to Room databse.
        // These procedures are sequentially and asyncronously done such that Continuation should be
        // applied. Otherwise, it may produce the final results as null.

        // Query the auto makers
        autoDataRef.get().continueWith(task -> {
            if(task.isSuccessful()) return task.getResult();
            else return task.getResult(IOException.class);

        }).continueWith(task -> {
            // Then, make the result turn into ArrayList
            for(QueryDocumentSnapshot automaker : task.getResult()) {
                autoMakerEntity = new AutoDataMakerEntity();
                autoMakerEntity._id = Integer.valueOf(automaker.getId());
                autoMakerEntity.autoMaker = automaker.getString("auto_maker");

                mDB.autoDataModel().insertAutoMaker(autoMakerEntity);
                autoMakerList.add(automaker.getString("auto_maker"));
            }

            return autoMakerList;

        }).addOnCompleteListener(automakerList -> {
            // Then, loop the list to query auto_model with each auto maker.
            for(String autoMaker : automakerList.getResult()) {
                autoDataRef.whereEqualTo("auto_maker", autoMaker).get().continueWith(task -> {
                    // Query results with an auto_maker matched
                    for(QueryDocumentSnapshot doc : task.getResult()) {
                        doc.getReference().collection("auto_model").get().addOnSuccessListener(models -> {
                            for(QueryDocumentSnapshot model : models) {
                                log.i("auto model: %s", model.getString("model_name"));
                                /*
                                autoData = new AutoData(autoMaker, doc.getString("model_name"), 0);
                                autoData.setAutoMaker(autoMaker);
                                autoData.setAutoModel(modelName);
                                autoData.setAutoType(0);
                                autoList.add(autoData);
                                */

                                // Insert the data into the DB.
                                autoModelEntity = new AutoDataModelEntity();
                                autoModelEntity.parentId = Integer.valueOf(doc.getId());
                                autoModelEntity.modelName = model.getString("model_name");

                                // NullPointerException occrrued!!
                                //autoModelEntity.autoType = doc.getLong("auto_type").intValue();
                                mDB.autoDataModel().insertModel(autoModelEntity);

                            }
                        });
                    }

                    return task.getResult();

                }).addOnCompleteListener(task -> {
                    log.i("Insert task done:%s", task.getResult());
                    if(task.isSuccessful()) mCallback.handleState(DOWNLOAD_COMPLETED);
                    else mCallback.handleState(DOWNLOAD_FAILED);
                });
            }
        });


    }

    // OPTION: Serialize the auto data and save it in the file.
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


}
