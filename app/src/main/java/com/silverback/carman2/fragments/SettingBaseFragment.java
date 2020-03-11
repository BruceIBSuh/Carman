package com.silverback.carman2.fragments;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

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
        //void setRegistrationNumber(String makerNum, String modelNum);
        void queryAutoMakerSnapshot(QueryDocumentSnapshot makershot);
        void queryAutoModelSnapshot(QueryDocumentSnapshot modelshot);
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

    // Query the auto maker first. Upon completion, notify the listener of the automaker snapshot
    // to continue another query to retrieve auto models.
    void queryAutoMaker(String name) {
        autoRef.whereEqualTo("auto_maker", name).get().addOnSuccessListener(makers -> {
            for(QueryDocumentSnapshot makershot : makers) {
                if(makershot.exists()) {
                    mListener.queryAutoMakerSnapshot(makershot);
                    break;
                }
            }
        }).addOnFailureListener(e -> {
            log.i("automaker queried failed");
        });
    }

    // On completion of the auto maker query, make a sequential query of auto models with the
    // automaker snapshot id, then notify the listener of queried snapshot
    void queryAutoModel(String id,  String model) {
        log.i("Model name: %s", model);
        if(TextUtils.isEmpty(model)) mListener.queryAutoModelSnapshot(null);
        autoRef.document(id).collection("auto_model").whereEqualTo("model_name", model)
                .get().addOnSuccessListener(query -> {
                    for(QueryDocumentSnapshot modelshot : query) {
                        if(modelshot.exists()) {
                            log.i("Query modelshot: %s", modelshot.getLong("reg_number"));
                            mListener.queryAutoModelSnapshot(modelshot);
                            break;
                        }
                    }
                }).addOnFailureListener(e -> {
                    log.i("query failed");
                    mListener.queryAutoModelSnapshot(null);
                });
    }


    void setSpannableAutoDataSummary(Preference pref, String summary) {
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
