package com.silverback.carman2.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.silverback.carman2.R;
import com.silverback.carman2.SettingPreferenceActivity;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.viewmodels.FragmentSharedModel;
import com.silverback.carman2.utils.Constants;

import org.json.JSONArray;

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
public class SettingAutoFragment extends SettingBaseFragment implements
        Preference.OnPreferenceChangeListener {

    private static final LoggingHelper log = LoggingHelperFactory.create(SettingAutoFragment.class);

    // Constants for setting year entries.
    private static final int LONGEVITY = 20;

    // Objects
    private String[] arrAutoType;
    private FragmentSharedModel fragmentModel;
    private OnToolbarTitleListener mToolbarListener;
    private ListPreference autoMakerPref, autoTypePref, autoModelPref, autoYearPref;

    // fields
    private String makerId, modelId;
    private String makerName, modelName, typeName, yearName;
    private int typeId;
    private boolean isMakerChanged, isModelChanged;


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

        log.i("autoListener: %s", autoListener);

        arrAutoType = new String[]{ getString(R.string.pref_entry_void),
                "Sedan", "SUV", "MPV", "Mini Bus", "Truck", "Bus"};
        mSettings = ((SettingPreferenceActivity)getActivity()).getSettings();
        fragmentModel = new ViewModelProvider(requireActivity()).get(FragmentSharedModel.class);

        autoMakerPref = findPreference(Constants.AUTO_MAKER);
        autoTypePref = findPreference(Constants.AUTO_TYPE);
        autoModelPref = findPreference(Constants.AUTO_MODEL);
        autoYearPref = findPreference(Constants.AUTO_YEAR);

        autoMakerPref.setOnPreferenceChangeListener(this);
        autoTypePref.setOnPreferenceChangeListener(this);
        autoModelPref.setOnPreferenceChangeListener(this);
        //addCompleteAutoQueryListener(this);


        // Initially, query all auto makers to set entry(values) to the auto maker preference.
        // useSimpleSummaryProvider does not work b/c every time the fragment is instantiated, the
        // entries is set which may disable to call the summary.
        autoRef.get(source).addOnSuccessListener(queries -> {
            List<String> autoMakerList = new ArrayList<>();
            for(QueryDocumentSnapshot snapshot : queries) {
                if(snapshot.exists()) autoMakerList.add(snapshot.getString("auto_maker"));
            }

            autoMakerPref.setEntries(autoMakerList.toArray(new CharSequence[queries.size()]));
            autoMakerPref.setEntryValues(autoMakerList.toArray(new CharSequence[queries.size()]));
        });

        // Attach the listener to be notified that the autodata query is made done.
        //addCompleteAutoQueryListener(this);

        // Set the entries(values) to the auto type preference.
        autoTypePref.setEntries(arrAutoType);
        autoTypePref.setEntryValues(arrAutoType);

        // Set the entries(values) to the auto year preference.
        List<String> yearList = new ArrayList<>();
        int year = Calendar.getInstance().get(Calendar.YEAR);
        for (int i = year; i >= (year - LONGEVITY); i--) yearList.add(String.valueOf(i));
        String[] mYearEntries = yearList.toArray(new String[LONGEVITY]);
        autoYearPref.setEntries(mYearEntries);
        autoYearPref.setEntryValues(mYearEntries);


        // Query the auto maker to retrieve the registration number, the query result of which is
        // notified to the OnCompleteAutoQueryListener. Sequentially, upon completion of the auto
        // maker query, make an auto model query to have the registration number notified to the
        // same listener, then set the initial values and summaries. In particular, the summaries
        // of the autoMakerPref and autoModelPref preferences include the queried registration numbers.
        makerName = mSettings.getString(Constants.AUTO_MAKER, null);
        modelName = mSettings.getString(Constants.AUTO_MODEL, null);
        typeName = mSettings.getString(Constants.AUTO_TYPE, null);
        yearName = mSettings.getString(Constants.AUTO_YEAR, null);
        log.i("Auto Preferences: %s, %s, %s", makerName, modelName, typeName);

        // Other than the initial value of the automaker which should be null or empty, query the
        // automaker with a maker name selected.
        if(TextUtils.isEmpty(makerName)) autoModelPref.setEnabled(false);
        else queryAutoMaker(makerName);
    }


    // The autoTypePref and autoModelPref preferenc depend on the autoMakerPref one in terms of setting entries
    // and values, which means that the autoMakerPref has a new value, the autoTypePref gets no set and
    // the autoModey queries new entries with the new autoMakerPref value.
    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        // Set an initiall summary to the auto maker preference with tne number registered.
        // If successful, set the auto model preference to be enabled and set entries to it
        // queried with the maker and type id.
        final String valueName = (String)value;

        switch(preference.getKey()) {
            // If the auto maker preference changes the value, query the registration number, setting
            // the entries to autoModelPref and the void summary to autoTypePref and autoModelPref as well.
            // At the same time, increase the registration number of the current auto maker and decrease
            // the number of the previous auto maker, which can be retrieved by getValue();
            case Constants.AUTO_MAKER:
                isMakerChanged = true;
                // The last auto maker decrease its reg number before a new maker comes in unless
                // its value is null, which occurs at the initial setting.
                if(!TextUtils.isEmpty(autoMakerPref.getValue())){
                    log.i("Decrease the previous automaker reg number: %s", autoMakerPref.getValue());
                    autoRef.document(makerId).update("reg_number", FieldValue.increment(-1));
                }


                // The reg number of the current auto model has to be decreased b/c change of
                // the auto maker makes the auto model set to null.
                if(!TextUtils.isEmpty(autoModelPref.getValue())) {
                    log.i("auto model: %s", autoModelPref.getValue());
                    autoRef.document(makerId).collection("auto_model").document(makerId)
                            .update("reg_number", FieldValue.increment(-1))
                            .addOnSuccessListener(aVoid -> log.i("decrease the reg number successfully"));
                }


                // Set the auto model preference summary to void and make it disabled until the
                // entries are prepared by setAutoModelEntries()
                autoModelPref.setValue(null);
                autoTypePref.setValue(null);
                autoModelPref.setSummary(getString(R.string.pref_entry_void));
                autoTypePref.setSummary(getString(R.string.pref_entry_void));
                autoModelPref.setEnabled(false);
                // Revert the auto type preference with the summary attached as void.

                // Make a new query for the auto maker, the result of which sets a new entries of
                // the auto models.
                queryAutoMaker(valueName);
                return true;

            case Constants.AUTO_TYPE:
                autoTypePref.setSummary(valueName);
                typeId = Arrays.asList(arrAutoType).indexOf(valueName);
                autoModelPref.setValue(null);
                autoModelPref.setSummary(getString(R.string.pref_entry_void));
                autoModelPref.setEnabled(false);

                log.i("maker and type ID : %s, %s", makerId, typeId);
                setAutoModelEntries(makerId, typeId);
                //queryAutoMaker(makerName);

                return true;

            case Constants.AUTO_MODEL:
                isModelChanged = true;
                if(!TextUtils.isEmpty(autoModelPref.getValue())) {
                    autoRef.document(makerId).collection("auto_model").document(modelId)
                            .update("reg_number", FieldValue.increment(-1));
                }

                queryAutoModel(makerId, valueName);
                return true;

            default: return false;
        }

    }

    // Implements SettingBaseFragment.OnCompleteAutoQueryListener which notifies that query to
    // retrieve the registration number of an auto maker or auto model has completed.
    @SuppressWarnings("ConstantConditions")
    @Override
    public void queryAutoMakerSnapshot(QueryDocumentSnapshot makershot) {
        // With the automaker id queried and the autotype id, query auto models and set them to
        // the model entry.
        makerId = makershot.getId();
        typeId = Arrays.asList(arrAutoType).indexOf(typeName);
        setAutoModelEntries(makerId, typeId);

        // When the auto maker changes, which means the previous automaker is not null, update
        // the current registration number to be increased.
        int makerNum = makershot.getLong("reg_number").intValue();
        if(isMakerChanged) {
            makerNum++;
            makershot.getReference().update("reg_number", FieldValue.increment(1));
        }

        String name = makershot.getString("auto_maker");
        autoMakerPref.setSummary(String.format("%s %s%s", name, getString(R.string.pref_auto_reg), makerNum));

        autoTypePref.setEnabled(true);
        // Continue to query the auto model with the automaker snapshot to retrieve the
        // auto model registration number and set it to the summary
        if(!TextUtils.isEmpty(modelName)) queryAutoModel(makerId, modelName);
        else setAutoModelEntries(makerId, typeId);
    }

    @Override
    public void queryAutoModelSnapshot(QueryDocumentSnapshot modelshot) {
        modelId = modelshot.getId();
        int modelNum = modelshot.getLong("reg_number").intValue();
        if(isModelChanged) {
            log.i("Increase the current model reg number");
            modelNum ++;
            autoRef.document(makerId).collection("auto_model").document(modelId)
                    .update("reg_number", FieldValue.increment(1))
                    .addOnSuccessListener(aVoid -> log.i("update regnum successfully"));
        }

        autoModelPref.setSummary(String.format("%s %s %s",
                modelshot.getString("model_name"), getString(R.string.pref_auto_reg), modelNum));
    }



    // To make the Up button working in Fragment, it is required to invoke sethasOptionsMenu(true)
    // and the return value should be true in onOptionsItemSelected(). The values of each preference
    // is translated to List<String>, then converted to JSONString for transferring the json string
    // to SettingPerrenceFragment to invalidate the preference summary.
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if(item.getItemId() == android.R.id.home) {
            List<String> dataList = new ArrayList<>();
            dataList.add(mSettings.getString(Constants.AUTO_MAKER, null));
            dataList.add(mSettings.getString(Constants.AUTO_MODEL, null));
            dataList.add(mSettings.getString(Constants.AUTO_TYPE, null));
            dataList.add(mSettings.getString(Constants.AUTO_YEAR, null));

            JSONArray json = new JSONArray(dataList);
            mSettings.edit().putString(Constants.AUTO_DATA, json.toString()).apply();

            // Update the registration numbers of the auto makers and models. If any change occurs,
            // the previous reg numbers should be decreased and the new reg numbers increased.
            // In case no auto data is set first time, throws NullPointerException.
            fragmentModel.getAutoData().setValue(json.toString());
            mToolbarListener.notifyResetTitle();

            return true;
        }


        return false;
    }


    // This method queries all auto models with auto maker and auto type as conditions. The auto maker
    // is required but the auto type condition may be null. Special care should be taken when async
    // queries are made. This method takes Continuation which queries auto maker first. On completion,
    // the next query is made with the integer value of auto type, which may be null.
    @SuppressWarnings("ConstantConditions")
    private void setAutoModelEntries(String id, int type) {
        if(TextUtils.isEmpty(id)) return;

        CollectionReference modelRef = autoRef.document(id).collection("auto_model");
        Query modelQuery = (type > 0) ? modelRef.whereEqualTo("auto_type", type) : modelRef;

        log.i("model query: %s, %s, %s", id, type, modelQuery);
        modelQuery.get(source).addOnSuccessListener(query -> {
            List<String> modelEntries = new ArrayList<>();
            for(QueryDocumentSnapshot modelshot : query) {
                if(modelshot.exists()) modelEntries.add(modelshot.getString("model_name"));
            }

            final int size = modelEntries.size();
            autoModelPref.setEntries(modelEntries.toArray(new CharSequence[size]));
            autoModelPref.setEntryValues(modelEntries.toArray(new CharSequence[size]));

            autoModelPref.setValue(null);
            autoModelPref.setEnabled(true);

        });
    }
}
