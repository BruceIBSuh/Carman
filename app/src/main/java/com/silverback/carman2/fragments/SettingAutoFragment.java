package com.silverback.carman2.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;
import com.silverback.carman2.R;
import com.silverback.carman2.SettingPreferenceActivity;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.FragmentSharedModel;
import com.silverback.carman2.utils.Constants;

import org.json.JSONArray;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

/**
 * This fragment is a split screen PreferenceFragmentCompat which may display multiple preferences
 * on a separate screen with its own preference hierarch that is concerned with the auto data.
 * Firestore holds comprehensive data to download but special care is required for latency. Thus,
 * upon completion of auto colleciton all at once, transactions should be made with Source.Cache.
 *
 */
public class SettingAutoFragment extends PreferenceFragmentCompat {

    private static final LoggingHelper log = LoggingHelperFactory.create(SettingAutoFragment.class);

    // Constants
    private static final int LONGEVITY = 20;

    // Objects
    private Source source;
    private ListenerRegistration autoListener;
    private CollectionReference autoRef;
    private Task<QuerySnapshot> autoMakerTask;
    private QueryDocumentSnapshot autoMakerSnapshot;
    private FragmentSharedModel fragmentSharedModel;
    private SharedPreferences mSettings;
    private OnToolbarTitleListener mToolbarListener;
    private ListPreference autoMaker, autoType, autoModel, autoYear;
    private List<String> yearList;

    // fields
    private JSONArray jsonAutoArray;
    private String mAutoMaker;
    private int mRegNumber;
    private String[] mYearEntries;


    // Interface for reverting the actionbar title. Otherwise, the title in the parent activity should
    // be reset to the current tile.
    public interface OnToolbarTitleListener {
        void notifyResetTitle();
    }

    // Set the listener to the parent activity for reverting the toolbar title.
    public void addTitleListener(OnToolbarTitleListener titleListener) {
        mToolbarListener = titleListener;
    }

    // Constructor
    public SettingAutoFragment() {
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        setPreferencesFromResource(R.xml.pref_autodata, rootKey);
        setHasOptionsMenu(true);

        autoMaker = findPreference(Constants.AUTO_MAKER);
        autoType = findPreference(Constants.AUTO_TYPE);
        autoModel = findPreference(Constants.AUTO_MODEL);
        autoYear = findPreference(Constants.AUTO_YEAR);

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        mSettings = ((SettingPreferenceActivity)getActivity()).getSettings();
        fragmentSharedModel = new ViewModelProvider(getActivity()).get(FragmentSharedModel.class);
        yearList = new ArrayList<>();

        // Define CollectionReference of the auto data and attach the listener to it, which
        // indicates.
        autoRef = firestore.collection("autodata");
        autoListener = autoRef.addSnapshotListener((querySnapshot, e) -> {
            if(e != null) return;
            source = querySnapshot != null && querySnapshot.getMetadata().hasPendingWrites()?
                    Source.CACHE  : Source.SERVER ;
            log.i("Source: %s", source);
        });

        String[] arrAutoType = {"No Type", "Sedan", "SUV", "MPV", "Mini Bus", "Truck", "Bus"};
        autoType.setEntries(arrAutoType);
        autoType.setEntryValues(arrAutoType);

        createYearEntries();
        autoYear.setEntries(mYearEntries);
        autoYear.setEntryValues(mYearEntries);

        // Set the default summary and register number of the auto maker.
        setSummaryWithRegNumber(autoMaker, mSettings.getString(Constants.AUTO_MAKER, null));
        setSummaryWithRegNumber(autoType, mSettings.getString(Constants.AUTO_TYPE, null));
        setSummaryWithRegNumber(autoModel, mSettings.getString(Constants.AUTO_MODEL, null));

        // Initially, query all auto makers to set entry(values) to the auto maker preference.
        // useSimpleSummaryProvider does not work b/c every time the fragment is instantiated, the
        // entries is set which may disable to call the summary.
        autoRef.get(source).addOnSuccessListener(queries -> {
            List<String> autoMakerList = new ArrayList<>();
            for(QueryDocumentSnapshot snapshot : queries) {
                if(snapshot.exists()) autoMakerList.add(snapshot.getString("auto_maker"));
            }

            autoMaker.setEntries(autoMakerList.toArray(new CharSequence[queries.size()]));
            autoMaker.setEntryValues(autoMakerList.toArray(new CharSequence[queries.size()]));
        });


        // Initial sets for querying auto models with auto maker and auto type. As far as either of the fields
        // has the value, query auto models. Otherwise, the auto model preference is set to be
        // disabled.
        mAutoMaker = mSettings.getString(Constants.AUTO_MAKER, null);
        if(!TextUtils.isEmpty(mAutoMaker)) {
            String type = mSettings.getString(Constants.AUTO_TYPE, null);
            int typeIndex = Arrays.asList(autoType.getEntries()).indexOf(type);

            setAutoModelEntries(autoMaker.getValue(), typeIndex);
            //autoModel.setEnabled(true);

        } //else autoModel.setEnabled(false);

        // If the auto maker preference changes the value, query the registration number, setting
        // the entries to autoModel and the void summary to autoType and autoModel as well.
        // At the same time, increase the registration number of the current auto maker and decrease
        // the number of the previous auto maker, which can be retrieved by getValue();
        autoMaker.setOnPreferenceChangeListener((preference, maker)-> {
            log.i("source and value: %s, %s", source, autoMaker.getValue());
            // TEST CODING: pay attention to the position which is ahead of setSummary()
            setRegistrationNumber(autoMaker, mAutoMaker);

            mAutoMaker = maker.toString();
            setAutoModelEntries(mAutoMaker, -1);
            setSummaryWithRegNumber(autoMaker, maker.toString());



            autoModel.setSummary(getString(R.string.pref_entry_void));

            return true;
        });

        autoType.setOnPreferenceChangeListener((preference, type) -> {
            int typeNumber = autoType.findIndexOfValue(type.toString());
            setAutoModelEntries(mAutoMaker, typeNumber);
            autoModel.setSummary(getString(R.string.pref_entry_void));

            return true;
        });

        autoModel.setOnPreferenceChangeListener((preference, model) -> {
            setSummaryWithRegNumber(autoModel, model.toString());
            return true;
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        autoListener.remove();
    }

    // To make the Up button working in Fragment, it is required to invoke sethasOptionsMenu(true)
    // and the return value should be true in onOptionsItemSelected(). The values of each preference
    // is translated to List<String>, then converted to JSONString for transferring the json string
    // to SettingPerrenceFragment to invalidate the preference summary.
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        log.i("value compared: %s, %s", autoMaker.getValue(), autoMaker.getSummary());
        log.i("value compared: %s, %s", autoModel.getValue(), autoModel.getSummary());

        if(item.getItemId() == android.R.id.home) {
            // Invalidate the summary of the parent preference transferring the changed data to
            // as JSON string type.

            JSONArray autoData = new JSONArray(getAutoDataList());
            mSettings.edit().putString(Constants.AUTO_DATA, autoData.toString()).apply();

            // To pass the json string for setting the summary in SettingPreferenceActivity, then
            // upload the auto data.
            fragmentSharedModel.getJsonAutoData().setValue(autoData.toString());


            // Revert the toolbar title when leaving this fragment b/c SettingPreferenceFragment and
            // SettingAutoFragment share the toolbar under the same parent activity.
            mToolbarListener.notifyResetTitle();

            return true;
        }

        return false;
    }

    private void createYearEntries() {
        int year = Calendar.getInstance().get(Calendar.YEAR);
        for (int i = year; i >= (year - LONGEVITY); i--) yearList.add(String.valueOf(i));
        mYearEntries = yearList.toArray(new String[LONGEVITY]);
    }


    private List<String> getAutoDataList() {
        List<String> dataList = new ArrayList<>();

        dataList.add(autoMaker.getValue());
        dataList.add(autoType.getValue());
        dataList.add(autoModel.getValue());
        dataList.add(autoYear.getValue());

        return dataList;
    }

    // This method queries all auto models with auto maker and auto type. The auto maker is required
    // but the auto type condition may be null. Special care should be taken when async queries
    // are made. This method takes Continuation which queries auto maker first. On completion, the
    // next query is made with the integer value of auto type, which can be null.
    @SuppressWarnings("ConstantConditions")
    private void setAutoModelEntries(String maker, int type) {
        if(TextUtils.isEmpty(maker)) return;

        autoRef.whereEqualTo("auto_maker", maker).get(source).continueWith(makerTask -> {
            if(makerTask.isSuccessful()) return makerTask.getResult();
            else return makerTask.getResult(IOException.class);

        }).continueWith(makerTask -> {
            List<String> modelList = new ArrayList<>();
            for(QueryDocumentSnapshot model : makerTask.getResult()) {
                // Create query alternatives according to whether the auto type is empty or not.
                // The index 0 indicates no type that has to be excluded from query condition.
                final CollectionReference modelRef = model.getReference().collection("auto_model");
                Query modelQuery = (type > 0)? modelRef.whereEqualTo("auto_type", type) : modelRef;

                modelQuery.get(source).continueWith(modelTask -> {
                    if(modelTask.isSuccessful()) {
                        for (DocumentSnapshot document : modelTask.getResult()) {
                            log.i("Auto Model: %s", document.getString("model_name"));
                            modelList.add(document.getString("model_name"));
                        }

                        return modelTask.getResult();

                    } else return modelTask.getResult(IOException.class);

                }).addOnSuccessListener(querySnapshot -> {
                    final int size = modelList.size();
                    autoModel.setEntries(modelList.toArray(new CharSequence[size]));
                    autoModel.setEntryValues(modelList.toArray(new CharSequence[size]));
                });
            }

            return makerTask.getResult();

        });
    }


    @SuppressWarnings("ConstantConditions")
    private void setSummaryWithRegNumber(Preference pref, final String value) throws NullPointerException {

        if(TextUtils.isEmpty(value)) {
            pref.setSummary(getString(R.string.pref_entry_void));
            return;
        }

        // Instantiate the task which queries the auto maker with a value of the auto maker preference.
        // As far as the task is successful,
        autoRef.whereEqualTo("auto_maker", mAutoMaker).get(source).addOnSuccessListener(queries -> {
            for(QueryDocumentSnapshot snapshot : queries) {
                if(snapshot.exists()) {
                    //autoMakerSnapshot = snapshot;
                    log.i("AutoMaker snapshot: %s", snapshot);
                    switch(pref.getKey()) {
                        case Constants.AUTO_MAKER:
                            mRegNumber = snapshot.getLong("reg_number").intValue();
                            break;

                        case Constants.AUTO_TYPE:
                            CollectionReference typeRef = snapshot.getReference().collection("auto_model");

                            break;

                        case Constants.AUTO_MODEL:
                            CollectionReference modelRef = snapshot.getReference().collection("auto_model");
                            modelRef.whereEqualTo("model_name", value).get(source).addOnSuccessListener(models -> {
                                for(QueryDocumentSnapshot doc : models) {
                                    if(doc.exists()) {
                                        mRegNumber = doc.getLong("reg_number").intValue();
                                        break;
                                    }
                                }
                            }).addOnFailureListener(Throwable::printStackTrace);

                            break;

                        case Constants.AUTO_YEAR:
                            log.i("Auto year");
                            break;
                    }

                    break;
                }
            }
        }).addOnFailureListener(Throwable::printStackTrace);

        String result = String.format("%s%10s%s", value, getString(R.string.pref_auto_reg), mRegNumber);
        pref.setSummary(result);
    }

    private void setRegistrationNumber(Preference pref, String value) {
        Query prevQuery = autoRef.whereEqualTo("auto_maker", autoMaker.getValue());
        Query currentQuery = autoRef.whereEqualTo("auto_maker", mAutoMaker);

        currentQuery.get(source).addOnSuccessListener(queries -> {
            for(QueryDocumentSnapshot snapshot : queries) {
                if(snapshot.exists()) {
                    switch(pref.getKey()) {
                        case Constants.AUTO_MAKER:
                            log.i("values: %s", snapshot.getString("auto_data"));
                            snapshot.getReference().update("reg_number", FieldValue.increment(1));
                            break;

                        case Constants.AUTO_TYPE:
                            break;
                    }
                }
            }
        });
    }

}
