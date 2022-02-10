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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.PropertyName;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    void queryAutoMaker(String automaker) {
        autoRef.document(automaker).get().addOnSuccessListener(snapshot -> {
            if(snapshot.exists()) queryAutoMakerSnapshot(snapshot);
        }).addOnFailureListener(e -> log.e("No automaker queried:%s", e.getMessage()));
    }

    // Once the automaker queyr completes, continue to query the automodel if an model name is given.
    // As like the automaker, the query result is passed as param to queryAutoModelSnapshot() an
    // abstract method which should be implemented either in SettingAutoFragment or in SettingPreference
    // Fragment.
    void queryAutoModel(String automaker, String modelName) {
        autoRef.document(automaker).collection("automodels").document(modelName).get()
                .addOnSuccessListener(snapshot -> {
                    if(snapshot.exists()) queryAutoModelSnapshot(snapshot);
                });
    }



    // WARNING!!!: Setting a summary with a String formatting marker is no longer supported.
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

    // ObjectAutoData is saved in SharedPreferences as JSON String which should be parsed into List<String>
    // Each names are inherited to the child views of SettingPreferenceFragment and SettiingAutoFragment.
    List<String> parseAutoData(String jsonString) {
        List<String> autoDataList = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(jsonString);
            for(int i = 0; i < jsonArray.length(); i++) autoDataList.add(jsonArray.optString(i));

            // The null value that JSONObject returns seems different than that of other regular objects.
            // Thus, JSONObject.isNull(int) should be checked, then, if true,  set the null value to it .
            // This is firmly at bug issue.
            makerName = (jsonArray.isNull(0))? null : jsonArray.optString(0);
            modelName = (jsonArray.isNull(1))? null : jsonArray.optString(1);
            typeName = (jsonArray.isNull(2))? null : jsonArray.optString(2);
            engineName = jsonArray.optString(3);
            yearName = (jsonArray.isNull(4))? null : jsonArray.optString(3);
            log.i("AutoData parsed: %s, %s, %s, %s, %s", makerName, modelName, typeName, engineName, yearName);

        } catch(JSONException e) {e.printStackTrace();}

        return autoDataList;
    }

    // Abstract methods which should be implemented both in SettingPreferenceFragment and
    // SettingBaseFragment.
    protected abstract void queryAutoMakerSnapshot(DocumentSnapshot makershot);
    protected abstract void queryAutoModelSnapshot(DocumentSnapshot modelshot);

    // POJO in order to typecast any Firestore array field to List. Use the annotation "@Keep"
    // so proguard will not delete any methods from this class.
    static class ObjectAutoData {
        @PropertyName("auto_type")
        private Map<String, Integer> autoTypeMap;
        @PropertyName("engine_type")
        private Map<String, Integer> engineTypeMap;

        public ObjectAutoData() {
            // Must have a public no-argument constructor
        }
        // Initialize all fields
        public ObjectAutoData(Map<String, Integer> autoTypeMap, Map<String, Integer> engineTypeMap) {
            //this.autoTypeList = autoTypeList;
            //this.engineTypeList = engineTypeList;
            this.autoTypeMap = autoTypeMap;
            this.engineTypeMap = engineTypeMap;
        }

        @PropertyName("auto_type")
        public Map<String, Integer> getAutoTypeMap() {
            return autoTypeMap;
        }
        @PropertyName("engine_type")
        public Map<String, Integer> getEngineTypeMap() {
            return engineTypeMap;
        }

        /*
        @PropertyName("auto_type")
        public void setAutoTypeMap(Map<String, Integer> autoTypeMap) {
            this.autoTypeMap = autoTypeMap;
        }
        @PropertyName("engine_type")
        public void setEngineTypeMap(Map<String, Integer> engineTypeMap) {
            this.engineTypeMap = engineTypeMap;
        }
         */
    }

    static class ArrayAutoData {
        @PropertyName("model_engine")
        private List<String> enginetypeList;
        public ArrayAutoData(){
            //must have a public no-argument contructor;
        }
        public ArrayAutoData(List<String> enginetypeList) {
            this.enginetypeList = enginetypeList;
        }
        @PropertyName("model_engine")
        public List<String> getEngineTypeList() {return enginetypeList;}
        @PropertyName("model_engine")
        public void setEngineTypeList(List<String> enginetypeList) {
            this.enginetypeList = enginetypeList;
        }

    }
}
