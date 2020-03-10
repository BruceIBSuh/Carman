package com.silverback.carman2.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
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
import com.silverback.carman2.viewmodels.FragmentSharedModel;
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
public class SettingAutoFragment extends SettingBaseFragment implements Preference.OnPreferenceChangeListener {

    private static final LoggingHelper log = LoggingHelperFactory.create(SettingAutoFragment.class);

    // Constants
    private static final int LONGEVITY = 20;
    private String[] arrAutoType = {"Sedan", "SUV", "MPV", "Mini Bus", "Truck", "Bus"};

    // Objects
    private FirebaseFirestore firestore;
    private Source source;
    private ListenerRegistration autoListener;
    private CollectionReference autoRef;
    private DocumentReference autoDoc;
    private FragmentSharedModel fragmentSharedModel;
    private SharedPreferences mSettings;
    private OnToolbarTitleListener mToolbarListener;
    private ListPreference autoMaker, autoType, autoModel, autoYear;

    // fields
    private String mAutoMaker;
    private int typeId;
    private int regNum;




    // Interface for reverting the actionbar title. Otherwise, the title in the parent activity should
    // be reset to the current tile.
    public interface OnToolbarTitleListener {
        void notifyResetTitle();
    }

    // Set the listener to the parent activity for reverting the toolbar title.
    public void setTitleListener(OnToolbarTitleListener titleListener) {
        mToolbarListener = titleListener;
    }

    // Constructor
    public SettingAutoFragment() {}

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        setPreferencesFromResource(R.xml.pref_autodata, rootKey);
        setHasOptionsMenu(true);

        autoMaker = findPreference(Constants.AUTO_MAKER);
        autoType = findPreference(Constants.AUTO_TYPE);
        autoModel = findPreference(Constants.AUTO_MODEL);
        autoYear = findPreference(Constants.AUTO_YEAR);

        autoMaker.setOnPreferenceChangeListener(this);
        autoType.setOnPreferenceChangeListener(this);
        autoModel.setOnPreferenceChangeListener(this);

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        mSettings = ((SettingPreferenceActivity)getActivity()).getSettings();
        fragmentSharedModel = new ViewModelProvider(getActivity()).get(FragmentSharedModel.class);

        // Attach the sanpshot listener to the basic collection, which initially downloades all
        // the auto data from Firestore, then manage the data with Firestore cache framework
        // once the listener is removed.
        autoRef = firestore.collection("autodata");
        autoListener = autoRef.addSnapshotListener((querySnapshot, e) -> {
            if(e != null) return;
            source = (querySnapshot != null && querySnapshot.getMetadata().hasPendingWrites())?
                    Source.CACHE  : Source.SERVER ;
            log.i("Source: %s", source);

        });

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

            // Inital setting of the preference summaries, querying an auto maker with a name
            // fetched from SharedPreferences.
            initPreferences();
        });

        // Set the entries(values) to the auto type preference.
        autoType.setEntries(arrAutoType);
        autoType.setEntryValues(arrAutoType);

        // Set the entries(values) to the auto year preference dynamically.
        List<String> yearList = new ArrayList<>();
        int year = Calendar.getInstance().get(Calendar.YEAR);
        for (int i = year; i >= (year - LONGEVITY); i--) yearList.add(String.valueOf(i));
        String[] mYearEntries = yearList.toArray(new String[LONGEVITY]);
        autoYear.setEntries(mYearEntries);
        autoYear.setEntryValues(mYearEntries);
    }

    @Override
    public void onPause() {
        super.onPause();
        autoListener.remove();
    }

    // The autoType and autoModel preferenc depend on the autoMaker one in terms of setting entries
    // and values, which means that the autoMaker has a new value, the autoType gets no set and
    // the autoModey queries new entries with the new autoMaker value.
    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        // Set an initiall summary to the auto maker preference with tne number registered.
        // If successful, set the auto model preference to be enabled and set entries to it
        // queried with the maker and type id.
        final String name = value.toString();
        final String aVoid = getString(R.string.pref_entry_void);
        log.i("preference name: %s", name);

        switch(preference.getKey()) {
            // If the auto maker preference changes the value, query the registration number, setting
            // the entries to autoModel and the void summary to autoType and autoModel as well.
            // At the same time, increase the registration number of the current auto maker and decrease
            // the number of the previous auto maker, which can be retrieved by getValue();
            case Constants.AUTO_MAKER:

                queryAutoMaker(name);
                setFirestoreCompleteListener(new OnFirestoreTaskCompleteListener() {
                    @Override
                    public void setAutoMakerSnapshot(QueryDocumentSnapshot snapshot) {
                        log.i("automaker listener: %s",snapshot.getString("auto_maker"));
                    }
                });


                setAutoMakerPreference(name, -1).addOnCompleteListener(task -> {
                    log.i("setAutoMakerPreference task completed");
                    setAutoModelPreference(name);
                    autoType.setValue(autoType.getEntries()[0].toString());
                    autoType.setSummary(aVoid);
                    autoModel.setSummary(aVoid);

                });

                return true;

            case Constants.AUTO_TYPE:
                typeId = Arrays.asList(arrAutoType).indexOf(name);
                setAutoModelEntries(mAutoMaker, typeId);
                autoType.setSummary(value.toString());
                return true;

            case Constants.AUTO_MODEL:
                setAutoModelPreference(name);
                return true;

            default: return false;
        }

    }


    // To make the Up button working in Fragment, it is required to invoke sethasOptionsMenu(true)
    // and the return value should be true in onOptionsItemSelected(). The values of each preference
    // is translated to List<String>, then converted to JSONString for transferring the json string
    // to SettingPerrenceFragment to invalidate the preference summary.
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

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


    // Set an initiall summary to the auto maker preference with tne number registered.
    // If successful, set the auto model preference to be enabled and set entries to it
    // queried with the maker and type id.
    private void initPreferences() {
        String aVoid = getString(R.string.pref_entry_void);
        String makerName = mSettings.getString(Constants.AUTO_MAKER, null);
        String typeName = mSettings.getString(Constants.AUTO_TYPE, null);
        String modelName = mSettings.getString(Constants.AUTO_MODEL, null);
        typeId = Arrays.asList(arrAutoType).indexOf(typeName);

        log.i("Auto Maker name: %s", makerName);
        if(!TextUtils.isEmpty(makerName)) {
            Task<QuerySnapshot> task = setAutoMakerPreference(makerName, typeId);
            task.addOnCompleteListener(autoTask -> {
                setAutoModelPreference(modelName);
                autoType.setSummary(typeName);
            });

        // One shot code used before the autumaker preference hasn't be set.
        } else {
            autoModel.setEntries(new CharSequence[]{});
            autoModel.setEntryValues(new CharSequence[]{});
        }

    }

    @SuppressWarnings("ConstantConditions")
    private Task<QuerySnapshot> setAutoMakerPreference(String value, int typeId) {

        final String prevValue = autoMaker.getValue();
        final Task<QuerySnapshot> task = autoRef.whereEqualTo("auto_maker", value).get(source);

        task.continueWith(automakerTask -> {
            if(automakerTask.isSuccessful()) return automakerTask.getResult();
            else return automakerTask.getResult(IOException.class);
        });

        // Increase the registration number of a new auto maker and set summary with those combined.
        task.continueWith(automakerTask -> {
            for(QueryDocumentSnapshot makershot : automakerTask.getResult()) {
                if(makershot.exists()) {
                    mAutoMaker = makershot.getString("auto_maker");
                    autoDoc = makershot.getReference();

                    // Check the current and previous value for verifying that change has happended
                    // If so, increase the number and update it.
                    log.i("isValueChanged: %s, %s", prevValue, value);
                    //int num;
                    if(!value.equals(prevValue)) {
                        log.i("increase the current reg_number");
                        autoDoc.update("reg_number", FieldValue.increment(1)).addOnSuccessListener(aVoid -> {
                            log.i("reg. num updated");
                            regNum = makershot.getLong("reg_number").intValue() + 1;
                            autoMaker.setSummary(String.format("%s (%s)", value, regNum));
                        });

                    } else regNum = makershot.getLong("reg_number").intValue();
                    autoMaker.setSummary(String.format("%s (%s)", value, regNum));
                    setAutoModelEntries(mAutoMaker, typeId);

                    break;
                }
            }

            return automakerTask;

        });



        return task;
    }

    @SuppressWarnings("ConstantConditions")
    private void setAutoModelPreference(String value) {
        // Decrease the regsistration number with the previous model as far as the previous
        // model is different from the current one.
        boolean isModelChanged =
                (autoModel.getValue() == null && !TextUtils.isEmpty(value)) ||
                (autoModel.getValue() != null && autoModel.getValue().equals(value));

        if(isModelChanged) {
            autoDoc.collection("auto_model").whereEqualTo("model_name", autoModel.getValue())
                    .get(source).addOnSuccessListener(prevModels -> {
                        for (QueryDocumentSnapshot model : prevModels) {
                            if(model.exists()) {
                                model.getReference().update("reg_number", FieldValue.increment(-1));
                                break;
                            }
                        }
                    });
        }

        // Increase the registration number w/ a new auto model
        autoDoc.collection("auto_model").whereEqualTo("model_name", value).get(source)
                .addOnSuccessListener(curModels -> {
                    for(QueryDocumentSnapshot model : curModels) {
                        if(model.exists()) {
                            int num;
                            if(autoModel.getValue().equals(model.getString("model_name"))) {
                                model.getReference().update("reg_number", FieldValue.increment(1))
                                        .addOnSuccessListener(aVoid -> {
                                            log.i("update completed");
                                        }).addOnFailureListener(e -> log.e("update failed"));

                                num = model.getLong("reg_number").intValue() + 1;
                            } else num = model.getLong("reg_number").intValue();
                            autoModel.setSummary(String.format("%s (%s)", value, num));
                            break;
                        }
                    }
                });
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
                            //log.i("Auto Model: %s", document.getString("model_name"));
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

    private List<String> getAutoDataList() {
        List<String> dataList = new ArrayList<>();

        dataList.add(autoMaker.getValue());
        dataList.add(autoType.getValue());
        dataList.add(autoModel.getValue());
        dataList.add(autoYear.getValue());

        return dataList;
    }


}
