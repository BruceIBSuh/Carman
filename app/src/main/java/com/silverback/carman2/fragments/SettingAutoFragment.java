package com.silverback.carman2.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.Source;
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
public class SettingAutoFragment extends SettingBaseFragment implements Preference.OnPreferenceChangeListener {

    private static final LoggingHelper log = LoggingHelperFactory.create(SettingAutoFragment.class);

    // Constants for setting year entries.
    private static final int LONGEVITY = 20;

    // Objects
    private String[] arrAutoType;
    private Source source;
    private ListenerRegistration autoListener;
    private CollectionReference autoRef;
    private FragmentSharedModel fragmentModel;
    private SharedPreferences mSettings;
    private OnToolbarTitleListener mToolbarListener;
    private ListPreference autoMaker, autoType, autoModel, autoYear;

    // fields
    private String makerId, modelId;
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

        arrAutoType = new String[]{
                getString(R.string.pref_entry_void), "Sedan", "SUV", "MPV", "Mini Bus", "Truck", "Bus"};

        autoMaker = findPreference(Constants.AUTO_MAKER);
        autoType = findPreference(Constants.AUTO_TYPE);
        autoModel = findPreference(Constants.AUTO_MODEL);
        autoYear = findPreference(Constants.AUTO_YEAR);

        autoMaker.setOnPreferenceChangeListener(this);
        autoType.setOnPreferenceChangeListener(this);
        autoModel.setOnPreferenceChangeListener(this);

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        mSettings = ((SettingPreferenceActivity)getActivity()).getSettings();
        fragmentModel = new ViewModelProvider(getActivity()).get(FragmentSharedModel.class);

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
            //initPreferences();
        });

        // Set the entries(values) to the auto type preference.
        autoType.setEntries(arrAutoType);
        autoType.setEntryValues(arrAutoType);

        // Set the entries(values) to the auto year preference.
        List<String> yearList = new ArrayList<>();
        int year = Calendar.getInstance().get(Calendar.YEAR);
        for (int i = year; i >= (year - LONGEVITY); i--) yearList.add(String.valueOf(i));
        String[] mYearEntries = yearList.toArray(new String[LONGEVITY]);
        autoYear.setEntries(mYearEntries);
        autoYear.setEntryValues(mYearEntries);


        // Query the auto maker to retrieve the registration number, the query result of which is
        // notified to the OnFirestoreCompleteListener. Sequentially, upon completion of the auto
        // maker query, make an auto model query to have the registration number notified to the
        // same listener, then set the initial values and summaries. In particular, the summaries
        // of the autoMaker and autoModel preferences include the queried registration numbers.
        String makerName = mSettings.getString(Constants.AUTO_MAKER, null);
        String modelName = mSettings.getString(Constants.AUTO_MODEL, null);
        String typeName = mSettings.getString(Constants.AUTO_TYPE, null);
        if(!TextUtils.isEmpty(typeName)) autoType.setSummary(typeName);
        log.i("Auto Preferences: %s, %s, %s", makerName, modelName, typeName);

        // Just in case of initail setting first time.
        if(TextUtils.isEmpty(makerName)) autoModel.setEnabled(false);
        else queryAutoMaker(makerName);

        // Attach OnFirestoreCompleteListener which is defined in the parent fragmnet(SettingBaseFragment)
        // and notified of Firestore query result when querying the auto maker and auto model snapshots.
        setFirestoreCompleteListener(new OnFirestoreCompleteListener() {

            @Override
            public void queryAutoMakerSnapshot(QueryDocumentSnapshot makershot) {
                // Field to share a automaker document id until the current maker changes.
                makerId = makershot.getId();
                // On completion of the automaker query, set entries of the auto model preference
                typeId = Arrays.asList(arrAutoType).indexOf(typeName);
                setAutoModelEntries(makerId, typeId);
                // When the auto maker changes, which means the prevMaker value is not null, update
                // the current registration number to be increased.
                int makerNum = makershot.getLong("reg_number").intValue();
                if(isMakerChanged) {
                    makerNum++;
                    makershot.getReference().update("reg_number", FieldValue.increment(1));
                }

                String name = makershot.getString("auto_maker");
                autoMaker.setSummary(String.format("%s %s %s", name, "reg:", makerNum));
                // Continue to query the auto model with the automaker snapshot to retrieve the
                // auto model registration number and set it to the summary
                if(!TextUtils.isEmpty(modelName)) queryAutoModel(makerId, modelName);
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

                autoModel.setSummary(String.format("%s %s %s",
                        modelshot.getString("model_name"), "reg:", modelNum));
            }


        });

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
        final String valueName = (String)value;

        switch(preference.getKey()) {
            // If the auto maker preference changes the value, query the registration number, setting
            // the entries to autoModel and the void summary to autoType and autoModel as well.
            // At the same time, increase the registration number of the current auto maker and decrease
            // the number of the previous auto maker, which can be retrieved by getValue();
            case Constants.AUTO_MAKER:
                // Set the flag to true, which indicates the auto make has changed.
                isMakerChanged = true;
                // The last auto maker decrease its reg number before a new maker comes in unless
                // its value is null, which occurs at the initial setting.
                if(!TextUtils.isEmpty(autoMaker.getValue())){
                    log.i("Decrease the previous automaker reg number");
                    autoRef.document(makerId).update("reg_number", FieldValue.increment(-1));
                }

                // Set the auto model preference summary to void and make it disabled until the
                // entries are prepared by setAutoModelEntries()
                autoModel.setEnabled(false);
                autoModel.setValue(null);
                autoModel.setSummary(getString(R.string.pref_entry_void));
                // The reg number of the current auto model has to be decreased b/c change of
                // the auto maker makes the auto model set to null.
                if(!TextUtils.isEmpty(autoModel.getValue())) {
                    autoRef.document(makerId).collection("auto_model").document(modelId)
                            .update("reg_number", FieldValue.increment(-1))
                            .addOnSuccessListener(aVoid -> log.i("decrease the reg number successfully"));
                }

                // Revert the auto type preference with the summary attached as void.
                autoType.setValue(null);
                autoType.setSummary(getString(R.string.pref_entry_void));

                // Make a new query for the auto maker, the result of which sets a new entries of
                // the auto models.
                queryAutoMaker(valueName);

                return true;

            case Constants.AUTO_TYPE:
                autoType.setSummary(value.toString());
                typeId = Arrays.asList(arrAutoType).indexOf(valueName);
                log.i("Type ID changed: %s", typeId);
                setAutoModelEntries(makerId, typeId);

                return true;

            case Constants.AUTO_MODEL:
                isModelChanged = true;
                if(!TextUtils.isEmpty(autoModel.getValue())) {
                    autoRef.document(makerId).collection("auto_model").document(modelId)
                            .update("reg_number", FieldValue.increment(-1));
                }

                // Set the flag to true, which indicates the auto model has changed.
                queryAutoModel(makerId, valueName);
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

            List<String> dataList = new ArrayList<>();
            dataList.add(mSettings.getString(Constants.AUTO_MAKER, null));
            dataList.add(mSettings.getString(Constants.AUTO_MODEL, null));
            dataList.add(mSettings.getString(Constants.AUTO_TYPE, null));
            dataList.add(mSettings.getString(Constants.AUTO_YEAR, null));
            // Invalidate the summary of the parent preference transferring the changed data to
            // as JSON string type.
            /*
            JSONArray autoData = new JSONArray(dataList);
            mSettings.edit().putString(Constants.AUTO_DATA, autoData.toString()).apply();
            // To pass the json string for setting the summary in SettingPreferenceActivity, then
            // upload the auto data.
            fragmentModel.getJsonAutoData().setValue(true);
             */
            fragmentModel.getAutoDataList().setValue(dataList);

            // Revert the toolbar title when leaving this fragment b/c SettingPreferenceFragment and
            // SettingAutoFragment share the toolbar under the same parent activity.
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

        final CollectionReference modelRef = autoRef.document(id).collection("auto_model");
        Query modelQuery = (type > 0) ? modelRef.whereEqualTo("auto_type", type) : modelRef;

        modelQuery.get().addOnSuccessListener(query -> {
            List<String> modelEntries = new ArrayList<>();
            for(QueryDocumentSnapshot modelshot : query) {
                if(modelshot.exists()) modelEntries.add(modelshot.getString("model_name"));
            }

            final int size = modelEntries.size();
            autoModel.setEntries(modelEntries.toArray(new CharSequence[size]));
            autoModel.setEntryValues(modelEntries.toArray(new CharSequence[size]));

            autoModel.setValue(null);
            autoModel.setEnabled(true);

        });
    }
}
