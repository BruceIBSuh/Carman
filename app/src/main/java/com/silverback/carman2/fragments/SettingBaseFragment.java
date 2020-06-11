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

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
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

    // Package-private objects
    //ListenerRegistration autoListener;
    //Source source;
    SharedPreferences mSettings;
    CollectionReference autoRef;
    String makerName, modelName, typeName, engineName, yearName;

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
        }).addOnFailureListener(Throwable::printStackTrace);
    }

    // Once the automaker queyr completes, continue to query the automodel if an model name is given.
    // As like the automaker, the query result is passed as param to queryAutoModelSnapshot() an
    // abstract method which should be implemented either in SettingAutoFragment or in SettingPreference
    // Fragment.
    void queryAutoModel(String makerId, String modelName) {
        log.i("Model name: %s", modelName);
        //if(TextUtils.isEmpty(model)) mListener.queryAutoModelSnapshot(null);
        autoRef.document(makerId).collection("auto_model").whereEqualTo("model_name", modelName).get()
                .addOnSuccessListener(queries -> {
                    for(DocumentSnapshot modelshot : queries) {
                        if(modelshot.exists()) {
                            queryAutoModelSnapshot(modelshot);
                            break;
                        }
                    }
                }).addOnFailureListener(Exception::printStackTrace);
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
    protected abstract void queryAutoMakerSnapshot(DocumentSnapshot makershot);
    protected abstract void queryAutoModelSnapshot(DocumentSnapshot modelshot);
}
