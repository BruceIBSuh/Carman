package com.silverback.carman2.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.SparseArray;

import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.viewmodels.FirestoreViewModel;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.prefs.PreferenceChangeListener;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SettingBaseFragment extends PreferenceFragmentCompat {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(SettingBaseFragment.class);

    // Objects
    private FirebaseFirestore firestore;
    private CollectionReference autoRef;
    private SharedPreferences mSettings;
    private QueryDocumentSnapshot makershot, modelshot;
    private OnFirestoreTaskCompleteListener mListener;

    // fields
    private Preference autoPreference;
    private String makerName, modelName, typeName, year;
    private String makerNum, modelNum;

    public interface OnFirestoreTaskCompleteListener {
        void setAutoMakerSnapshot(QueryDocumentSnapshot snapshot);
        void setAutoModelSnapshot(QueryDocumentSnapshot snapshot);
    }

    // Attach the listener
    void setFirestoreCompleteListener(OnFirestoreTaskCompleteListener listener) {
        mListener = listener;
    }

    public SettingBaseFragment() {
        super();
        firestore = FirebaseFirestore.getInstance();
        autoRef = firestore.collection("autodata");

    }

    class QueryAutoData implements OnFirestoreTaskCompleteListener {

        String makerName, modelName;
        QueryDocumentSnapshot makershot, modelshot;

        public QueryAutoData() {}



        @Override
        public void setAutoMakerSnapshot(QueryDocumentSnapshot snapshot) {
            makerName = snapshot.getString("auto_maker");
            makershot = snapshot;
        }

        @Override
        public void setAutoModelSnapshot(QueryDocumentSnapshot snapshot) {
            modelshot = snapshot;
        }


        public void queryMaker(String name) {
            autoRef.whereEqualTo("auto_maker", name).get().addOnSuccessListener(query -> {
                for(QueryDocumentSnapshot makershot : query) {
                    if(makershot.exists()) {
                        mListener.setAutoMakerSnapshot(makershot);
                        break;
                    }
                }
            });
        }

        public void queryModelNum(String name) {
            makershot.getReference().collection("auto_model").whereEqualTo("model_name", name).get()
                    .addOnSuccessListener(query -> {
                        for(QueryDocumentSnapshot snapshot : query) {
                            if(snapshot.exists()) {
                                mListener.setAutoModelSnapshot(snapshot);
                                break;
                            }
                        }
                    });

        }


    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {}

    // Query the QueryDocumentSnapshot of an auto maker. Upon completion of querying the snapshot,
    // notify it to the callee of SettingAutoFragment
    void queryAutoMaker(String name) {
        autoRef.whereEqualTo("auto_maker", name).get().addOnSuccessListener(query -> {
            for(QueryDocumentSnapshot makershot : query) {
                if(makershot.exists()) {
                    mListener.setAutoMakerSnapshot(makershot);
                    break;
                }
            }
        });
    }


    @SuppressWarnings("ConstantConditions")
    void setAutoDataNumAndSummary(Preference pref, String json) {

        try {
            JSONArray jsonArray = new JSONArray(json);
            makerName = jsonArray.optString(0);
            modelName = jsonArray.optString(2);
            typeName = jsonArray.optString(1);
            year = jsonArray.optString(3);
            log.i("Auto Data: %s, %s, %s, %s", makerName, modelName, typeName, year);

        } catch(JSONException e) {
            e.printStackTrace();
        }

        final Task<QuerySnapshot> makerTask = autoRef.whereEqualTo("auto_maker", makerName).get();

        makerTask.continueWith(task -> {
            if(task.isSuccessful()) return task.getResult();
            else return task.getResult(IOException.class);

        }).continueWith(task -> {
            for (QueryDocumentSnapshot makerShot : task.getResult()) {
                if (makerShot.exists()) {
                    makerNum = makerShot.getLong("reg_number").toString();
                    makerName = makerShot.getString("auto_maker");
                    log.i("makerNum: %s", makerNum);
                    return makerShot;
                }
            }
            return null;

        }).addOnCompleteListener(task -> {
            if(task.isSuccessful()) {
                String autoMaker = task.getResult().getString("auto_maker");
                log.i("autoMaker: %s", autoMaker);
                setAutoModelNum(pref, task.getResult(), modelName);
            }
        });
    }

    @SuppressWarnings("ConstantConditions")
    private void setAutoModelNum(Preference pref, QueryDocumentSnapshot snapshot, String model) {

        Task<QuerySnapshot> modelTask = snapshot.getReference().collection("auto_model")
                .whereEqualTo("model_name", model).get();

        modelTask.continueWith(task -> {
            if(task.isSuccessful()) return task.getResult();
            else return task.getResult(IOException.class);

        }).continueWith(task -> {
            for(QueryDocumentSnapshot modelShot : task.getResult()) {
                if(modelShot.exists()) {
                    modelNum = modelShot.getLong("reg_number").toString();
                    modelName = modelShot.getString("model_name");
                    log.i("model: %s, %s", modelName, modelNum);
                    break;
                }
            }

            return modelshot;

        }).addOnCompleteListener(task -> {
            if(task.isSuccessful()) setAutoDataSummary(pref, typeName, year);

        });

    }

    private void setAutoDataSummary(Preference pref, String typeName, String year) {
        String summary = String.format(
                "%s(%s), %s(%s), %s, %s", makerName, makerNum, modelName, modelNum, typeName, year);

        SpannableString sb = new SpannableString(summary);
        String reg = "\\(\\d+\\)";
        Matcher m = Pattern.compile(reg).matcher(summary);
        while(m.find()) {
            sb.setSpan(new ForegroundColorSpan(Color.BLUE), m.start(), m.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        pref.setSummary(sb);

    }

    public String[] getAutoDataNums() {
        return new String[]{makerNum, modelNum};

    }
}
