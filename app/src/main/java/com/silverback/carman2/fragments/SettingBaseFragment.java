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
import com.google.firebase.firestore.Source;
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
    private OnFirestoreCompleteListener mListener;

    // fields
    private Preference autoPreference;
    private String makerName, modelName, typeName, year;
    private String makerNum, modelNum;

    public interface OnFirestoreCompleteListener {
        void setRegistrationNumber(String makerNum, String modelNum);
    }

    // Attach the listener
    void setFirestoreCompleteListener(OnFirestoreCompleteListener listener) {
        mListener = listener;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {}

    public SettingBaseFragment() {
        super();
        firestore = FirebaseFirestore.getInstance();
        autoRef = firestore.collection("autodata");

    }

    // Query the auto maker first. Upon completion, sequentially query the auto model with the queried
    // auto maker document reference.
    @SuppressWarnings("ConstantConditions")
    //void queryAutoRegistrationNums(String makerName, String modelName) {
    void queryAutoRegistrationNums(String name) {
        autoRef.whereEqualTo("auto_maker", makerName).get().addOnSuccessListener(makers -> {
            for(QueryDocumentSnapshot makershot : makers) {
                if(makershot.exists()) {
                    queryAutoModelNum(makershot, modelName);
                    /*
                    // Upon completion of querying the auto maker, query
                    makershot.getReference().collection("auto_model")
                            .whereEqualTo("model_name", modelName).get()
                            .addOnSuccessListener(models -> {
                                for(QueryDocumentSnapshot modelshot : models) {
                                    if(modelshot.exists()) {
                                        log.i("modelshot: %s", modelshot.getLong("reg_number"));
                                        mListener.setRegistrationNumber(
                                                makershot.getLong("reg_number").toString(),
                                                modelshot.getLong("reg_number").toString());

                                        break;
                                    }
                                }
                            });
                    */
                    break;
                }
            }
        }).addOnFailureListener(e -> {
            log.i("automaker queried failed");
        });
    }

    void queryAutoModelNum(QueryDocumentSnapshot snapshot, String model) {
        autoRef.document(snapshot.getId()).collection("auto_model").whereEqualTo("model_name", model)
                .get().addOnSuccessListener(query -> {
                    for(QueryDocumentSnapshot modelshot : query) {
                        if(modelshot.exists()) {
                            log.i("Query modelshot: %s", modelshot.getLong("reg_number"));
                            break;
                        }
                    }
                });
    }


    public void setSpannableAutoDataSummary(Preference pref, String summary) {


        SpannableString sb = new SpannableString(summary);
        String reg = "\\(\\d+\\)";
        Matcher m = Pattern.compile(reg).matcher(summary);
        while(m.find()) {
            sb.setSpan(new ForegroundColorSpan(Color.BLUE), m.start(), m.end(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        pref.setSummary(sb);
    }
}
