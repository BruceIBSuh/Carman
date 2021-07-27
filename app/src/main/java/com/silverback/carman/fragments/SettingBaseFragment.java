package com.silverback.carman.fragments;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This abstract fragment is
 */
public abstract class SettingBaseFragment extends PreferenceFragmentCompat {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(SettingBaseFragment.class);

    // Objects
    //private QueryDocumentSnapshot makershot, modelshot;
    //private OnCompleteAutoQueryListener mListener;

    // Package-private objects
    //ListenerRegistration autoListener;
    //Source source;
    SharedPreferences mSettings;
    CollectionReference autoRef;
    String makerName, modelName, typeName, engineName, yearName;

    // Constructor
    public SettingBaseFragment() {
        super();
        autoRef = FirebaseFirestore.getInstance().collection("autodata");
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


    // Query an auto maker with a name selected by the automaker preference. The query result is
    // passed as param to queryAutoMakerSnapshot(), an abstract method which should be implemented
    // either in SettingAutoFragment or SettingPreferenceFragment.
    void queryAutoMaker(String name) {
        autoRef.whereEqualTo("auto_maker", name).get().addOnSuccessListener(makers -> {
            for(DocumentSnapshot makershot : makers) {
                if(makershot.exists()) {
                    queryAutoMakerSnapshot(makershot);
                    break;
                }
            }
        }).addOnFailureListener(e -> {
            log.i("queryAutoMaker failed: %s", e.getMessage());
        });
    }

    // Once the automaker queyr completes, continue to query the automodel if an model name is given.
    // As like the automaker, the query result is passed as param to queryAutoModelSnapshot() an
    // abstract method which should be implemented either in SettingAutoFragment or in SettingPreference
    // Fragment.
    void queryAutoModel(String makerId, String modelName) {
        autoRef.document(makerId).collection("auto_model").whereEqualTo("model_name", modelName).get()
                .addOnSuccessListener(queries -> {
                    for(DocumentSnapshot modelshot : queries) {
                        if(modelshot.exists()) {
                            queryAutoModelSnapshot(modelshot);
                            break;
                        }
                    }
                }).addOnFailureListener(e -> {
                    log.i("queryAutoModel failed: %s", e.getMessage());
                });
    }



    // WARNING: Setting a summary with a String formatting marker is no longer supported.
    // You should use a SummaryProvider instead.
    void setSpannedAutoSummary(Preference pref, String summary) {
        SpannableString sb = new SpannableString(summary);
        String reg = "\\(\\d+\\)";
        Matcher m = Pattern.compile(reg).matcher(summary);
        while(m.find()) {
            sb.setSpan(new ForegroundColorSpan(Color.BLUE), m.start(), m.end(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        //pref.setSummary(sb);
        pref.setSummaryProvider(preference -> sb);
    }

    // AutoData is saved in SharedPreferences as JSON String which should be parsed into List<String>
    // Each names are inherited to the child views of SettingPreferenceFragment and SettiingAutoFragment.
    List<String> parseAutoData(String jsonString) {
        List<String> autoDataList = new ArrayList<>();
        try {
            JSONArray jsonObject = new JSONArray(jsonString);
            for(int i = 0; i < jsonObject.length(); i++) autoDataList.add(jsonObject.optString(i));

            // The null value that JSONObject returns seems different than that of other regular objects.
            // Thus, JSONObject.isNull(int) should be checked, then, if true,  set the null value to it .
            // This is firmly at bug issue.
            makerName = (jsonObject.isNull(0))? null : jsonObject.optString(0);
            modelName = (jsonObject.isNull(1))? null : jsonObject.optString(1);
            typeName = (jsonObject.isNull(2))? null : jsonObject.optString(2);
            yearName = (jsonObject.isNull(3))? null : jsonObject.optString(3);
        } catch(JSONException e) {e.printStackTrace();}

        return autoDataList;
    }

    // Abstract methods which should be implemented both in SettingPreferenceFragment and
    // SettingBaseFragment.
    protected abstract void queryAutoMakerSnapshot(DocumentSnapshot makershot);
    protected abstract void queryAutoModelSnapshot(DocumentSnapshot modelshot);
}