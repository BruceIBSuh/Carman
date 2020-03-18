package com.silverback.carman2.fragments;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.Source;
import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class SettingBaseFragment extends PreferenceFragmentCompat {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(SettingBaseFragment.class);

    // Objects
    protected FirebaseFirestore firestore;
    private QueryDocumentSnapshot makershot, modelshot;
    //private OnCompleteAutoQueryListener mListener;

    ListenerRegistration autoListener;
    SharedPreferences mSettings;
    Source source;
    CollectionReference autoRef;
    String makerName, modelName, typeName, yearName;

    /*
    public interface OnCompleteAutoQueryListener {
        void queryAutoMakerSnapshot(QueryDocumentSnapshot makershot);
        void queryAutoModelSnapshot(QueryDocumentSnapshot modelshot);
    }

    // Attach the listener
    void addCompleteAutoQueryListener(OnCompleteAutoQueryListener listener) {
        mListener = listener;
    }

     */


    // Constructor
    public SettingBaseFragment() {
        super();
        firestore = FirebaseFirestore.getInstance();
        autoRef = firestore.collection("autodata");
        // Attach the sanpshot listener to the basic collection, which initially downloades all
        // the auto data from Firestore, then manage the data with Firestore cache framework
        // once the listener is removed.
        /*
        autoListener = autoRef.addSnapshotListener((querySnapshot, e) -> {
            if(e != null) return;
            source = (querySnapshot != null && querySnapshot.getMetadata().hasPendingWrites())?
                    Source.CACHE  : Source.SERVER ;
            log.i("Source: %s", source);

        });
        */
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {}


    // Query the auto maker first. Upon completion, notify the listener of the automaker snapshot
    // to continue another query to retrieve auto models.
    void queryAutoMaker(String name) {
        autoRef.whereEqualTo("auto_maker", name).get().addOnSuccessListener(makers -> {
            for(QueryDocumentSnapshot makershot : makers) {
                if(makershot.exists()) {
                    //mListener.queryAutoMakerSnapshot(makershot);
                    queryAutoMakerSnapshot(makershot);
                    break;
                }
            }
        }).addOnFailureListener(Throwable::printStackTrace);
    }

    // On completion of the auto maker query, make a sequential query of auto models with the
    // automaker snapshot id, then notify the listener of queried snapshot
    void queryAutoModel(String id,  String model) {
        log.i("Model name: %s", model);
        //if(TextUtils.isEmpty(model)) mListener.queryAutoModelSnapshot(null);
        autoRef.document(id).collection("auto_model").whereEqualTo("model_name", model)
                .get().addOnSuccessListener(query -> {
                    for(QueryDocumentSnapshot modelshot : query) {
                        if(modelshot.exists()) {
                            log.i("Query modelshot: %s", modelshot.getLong("reg_number"));
                            //mListener.queryAutoModelSnapshot(modelshot);
                            queryAutoModelSnapshot(modelshot);
                            break;
                        }
                    }
                }).addOnFailureListener(e -> {
                    log.i("query failed");
                    //mListener.queryAutoModelSnapshot(null);
                    queryAutoModelSnapshot(null);
                });
    }


    void setSpannedAutoSummary(Preference pref, String summary) {
        SpannableString sb = new SpannableString(summary);
        String reg = "\\(\\d+\\)";
        Matcher m = Pattern.compile(reg).matcher(summary);
        while(m.find()) {
            sb.setSpan(new ForegroundColorSpan(Color.BLUE), m.start(), m.end(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        pref.setSummary(sb);
    }


    List<String> parseAutoData(String jsonString) {
        List<String> autoDataList = new ArrayList<>();
        try {
            JSONArray json = new JSONArray(jsonString);
            for(int i = 0; i < json.length(); i++) autoDataList.add(json.optString(i));
            makerName = json.optString(0);
            modelName = json.optString(1);
            typeName = json.optString(2);
            yearName = json.optString(3);
        } catch(JSONException e) {
            e.printStackTrace();
        }

        return autoDataList;
    }

    // Abstract methods which should be implemented both in SettingPreferenceFragment and
    // SettingBaseFragment.
    protected abstract void queryAutoMakerSnapshot(QueryDocumentSnapshot makershot);
    protected abstract void queryAutoModelSnapshot(QueryDocumentSnapshot modelshot);
}
