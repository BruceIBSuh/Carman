package com.silverback.carman2.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.PropertyName;
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
    private String[] arrAutoType, arrEngineType;
    private FragmentSharedModel fragmentModel;
    private OnToolbarTitleListener mToolbarListener;
    private ListPreference autoMaker, autoType, autoModel, engineType, autoYear;

    // fields
    private String makerId, modelId;
    //private String makerName, modelName, typeName;
    private int autoTypeId;
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

        arrAutoType = new String[]{ getString(R.string.pref_entry_void),
                "Sedan", "SUV", "MPV", "Mini Bus", "Truck", "Bus"};

        arrEngineType = new String[] {getString(R.string.pref_entry_void),
                "Gas", "Diesel", "LPG", "Hybrid", "Electric", "Hydrogen", "Plug-in Hybrid" };

        mSettings = ((SettingPreferenceActivity)getActivity()).getSettings();
        fragmentModel = new ViewModelProvider(requireActivity()).get(FragmentSharedModel.class);

        autoMaker = findPreference(Constants.AUTO_MAKER);
        autoType = findPreference(Constants.AUTO_TYPE);
        engineType = findPreference(Constants.ENGINE_TYPE);
        autoModel = findPreference(Constants.AUTO_MODEL);
        autoYear = findPreference(Constants.AUTO_YEAR);

        autoMaker.setOnPreferenceChangeListener(this);
        autoType.setOnPreferenceChangeListener(this);
        autoModel.setOnPreferenceChangeListener(this);
        engineType.setOnPreferenceChangeListener(this);


        // Initially, query all auto makers to set entry(values) to the auto maker preference.
        // useSimpleSummaryProvider does not work b/c every time the fragment is instantiated, the
        // entries is set which may disable to call the summary.
        autoRef.get().addOnSuccessListener(queries -> {
            List<String> autoMakerList = new ArrayList<>();
            for(QueryDocumentSnapshot snapshot : queries) {
                if(snapshot.exists()) autoMakerList.add(snapshot.getString("auto_maker"));
            }

            autoMaker.setEntries(autoMakerList.toArray(new CharSequence[queries.size()]));
            autoMaker.setEntryValues(autoMakerList.toArray(new CharSequence[queries.size()]));
            autoMaker.setValue(makerName);
        });

        // Assign the names to each preference
        String jsonAutoData = mSettings.getString(Constants.AUTO_DATA, null);
        if(!TextUtils.isEmpty(jsonAutoData)) parseAutoData(jsonAutoData);
        log.i("Preference names: %s, %s, %s, %s", makerName, modelName, typeName, yearName);

        // Set the entries(values) to the auto type preference.
        autoType.setEntries(arrAutoType);
        autoType.setEntryValues(arrAutoType);
        String typeSummary = (!TextUtils.isEmpty(typeName))? typeName : getString(R.string.pref_entry_void);
        autoType.setSummary(typeSummary);

        // Set the engine type entries
        engineType.setEntries(arrEngineType);
        engineType.setEntryValues(arrEngineType);

        // Set the entries(values) to the auto year preference.
        List<String> yearList = new ArrayList<>();
        int year = Calendar.getInstance().get(Calendar.YEAR);
        for (int i = year; i >= (year - LONGEVITY); i--) yearList.add(String.valueOf(i));
        String[] mYearEntries = yearList.toArray(new String[LONGEVITY]);
        autoYear.setEntries(mYearEntries);
        autoYear.setEntryValues(mYearEntries);

        // Query the auto maker to retrieve the registration number, the result of which is notified
        // to OnCompleteAutoQueryListener. Sequentially, upon completion of the auto maker query,
        // make an auto model query to have the registration number notified to the same listener,
        // then set the initial values and summaries. In particular, the summaries of the autoMaker
        // and autoModel preferences include the queried registration numbers.
        /*
        makerName = mSettings.getString(Constants.AUTO_MAKER, null);
        modelName = mSettings.getString(Constants.AUTO_MODEL, null);
        typeName = mSettings.getString(Constants.AUTO_TYPE, null);
        String yearName = mSettings.getString(Constants.AUTO_YEAR, null);
        log.i("auto data in SharedPreferences: %s, %s, %s, %s", makerName, modelName, typeName, yearName);
        */

        // Other than the initial value of the automaker which should be null or empty, query the
        // automaker with a maker name selected.
        if(TextUtils.isEmpty(makerName)) autoModel.setEnabled(false);
        else queryAutoMaker(makerName);
    }

    @Override
    public void onPause() {
        super.onPause();
        //autoListener.remove();
    }


    // The autoType and autoModel preference depend on the autoMaker one in terms of setting entries
    // and values, which means that the autoMaker has a new value, the autoType gets no set and
    // the autoModel queries new entries with the new autoMaker value.
    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        // Set an initiall summary to the auto maker preference with tne number registered.
        // If successful, set the auto model preference to be enabled and set entries to it
        // queried with the maker and type id.
        final String valueName = (String)value;

        switch(preference.getKey()) {
            // If the auto maker preference changes, query the registration number, setting
            // the entries to autoModel and the void summary to autoType and autoModel as well.
            // At the same time, increase the registration number of the current auto maker and decrease
            // the number of the previous auto maker, which can be retrieved by getValue();
            case Constants.AUTO_MAKER:
                isMakerChanged = true;

                // The previous auto maker, if any, decrease its reg number before a new maker comes in
                // unless its value is null, which occurs at the initial setting.
                if(!TextUtils.isEmpty(autoMaker.getValue())){
                    log.i("autoMaker preferernce: %s", autoMaker.getValue());
                    autoRef.document(makerId).update("reg_number", FieldValue.increment(-1));
                }

                // The reg number of the current auto model has to be decreased b/c change of
                // the auto maker makes the auto model set to null.
                if(!TextUtils.isEmpty(autoModel.getValue())) {
                    log.i("automodel regit number: %s, %s", autoModel.getValue(), modelId);
                    autoRef.document(makerId).collection("auto_model").document(modelId)
                            .update("reg_number", FieldValue.increment(-1))
                            .addOnSuccessListener(aVoid -> log.i("decrease the reg number successfully"));
                }


                // If the automaker preference changes, the automodeel and autotype values return to
                // the default values.
                autoModel.setValue(null);
                autoModel.setEnabled(false);

                autoModel.setSummary(getString(R.string.pref_entry_void));
                autoType.setSummary(getString(R.string.pref_entry_void));
                engineType.setSummary(getString(R.string.pref_entry_void));

                // Make a new query with a new automaker name selected in the list preference.
                queryAutoMaker(valueName);
                return true;

            case Constants.AUTO_TYPE:
                autoType.setSummary(valueName);
                autoTypeId = Arrays.asList(arrAutoType).indexOf(valueName);

                autoModel.setValue(null);
                autoModel.setSummary(getString(R.string.pref_entry_void));
                autoModel.setEnabled(false); //until query completes.

                setAutoModelEntries(makerId, autoTypeId);
                return true;

            case Constants.AUTO_MODEL:
                isModelChanged = true;
                if(!TextUtils.isEmpty(autoModel.getValue())) {
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
    @SuppressWarnings({"ConstantConditions", "unchecked"})
    @Override
    public void queryAutoMakerSnapshot(DocumentSnapshot makershot) {
        // With the automaker id queried and the autotype id, query auto models and set them to
        // the model entry.
        makerId = makershot.getId();
        List<String> autoTypeList = (List<String>)makershot.get("auto_type");
        String[] arrAutoType = autoTypeList.toArray(new String[0]);
        autoType.setEntries(arrAutoType);
        autoType.setEntryValues(arrAutoType);

        // In case the auto preference changes, the auto type is set to the default value to show
        // all models
        autoTypeId = (isMakerChanged)? 0 : Arrays.asList(arrAutoType).indexOf(typeName);
        setAutoModelEntries(makerId, autoTypeId);

        // When the auto maker changes, which means the previous automaker is not null, update
        // the current registration number to be increased.
        int makerNum = makershot.getLong("reg_number").intValue();
        if(isMakerChanged) {
            makerNum++;
            makershot.getReference().update("reg_number", FieldValue.increment(1));
        }

        // Set the summary with a spnned string.
        String makerSummary = String.format("%s%10s%s(%s)",
                makershot.getString("auto_maker"), "", getString(R.string.pref_auto_reg), makerNum);
        setSpannedAutoSummary(autoMaker, makerSummary);

        autoType.setEnabled(true);// Check it out!!!
        // Continue to query the auto model with the automaker snapshot to retrieve the
        // auto model registration number and set it to the summary
        if(!TextUtils.isEmpty(modelName)) queryAutoModel(makerId, modelName);
        else setAutoModelEntries(makerId, autoTypeId);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void queryAutoModelSnapshot(QueryDocumentSnapshot modelshot) {
        modelId = modelshot.getId();
        int modelNum = modelshot.getLong("reg_number").intValue();
        // Set the auto type to be fit to the auto model
        int type = modelshot.getLong("auto_type").intValue();
        if(autoTypeId != type) {
            autoType.setValue(autoType.getEntryValues()[type].toString());
            autoType.setSummary(autoType.getEntryValues()[type].toString());
        }

        if(isModelChanged) {
            modelNum ++;
            autoRef.document(makerId).collection("auto_model").document(modelId)
                    .update("reg_number", FieldValue.increment(1))
                    .addOnSuccessListener(aVoid -> log.i("update regnum successfully"));
        }

        // Set the summary with a spanned string.
        String modelSummary = String.format("%s%10s%s(%s)",
                modelshot.getString("model_name"), "", getString(R.string.pref_auto_reg), modelNum);
        setSpannedAutoSummary(autoModel, modelSummary);
    }



    // To make the Up button working in Fragment, it is required to invoke sethasOptionsMenu(true)
    // and the return value should be true in onOptionsItemSelected(). The values of each preference
    // is translated to List<String>, then converted to JSONString for transferring the json string
    // to SettingPerrenceFragment to invalidate the preference summary.
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if(item.getItemId() == android.R.id.home) {
            List<String> dataList = new ArrayList<>();
            dataList.add(mSettings.getString(Constants.AUTO_MAKER, ""));
            dataList.add(mSettings.getString(Constants.AUTO_MODEL, ""));
            dataList.add(mSettings.getString(Constants.AUTO_TYPE, ""));
            dataList.add(mSettings.getString(Constants.AUTO_YEAR, ""));

            JSONArray json = new JSONArray(dataList);
            mSettings.edit().putString(Constants.AUTO_DATA, json.toString()).apply();

            // Update the registration numbers of the auto makers and models. If any change occurs,
            // the previous reg numbers should be decreased and the new reg numbers increased.
            // In case no auto data is set first time, throws NullPointerException.
            fragmentModel.getAutoData().setValue(json.toString());
            mToolbarListener.notifyResetTitle();


            log.i("AutoFragment preference values: %s, %s, %s, %s", autoMaker.getValue(), autoModel.getValue(), autoType.getValue(), autoYear.getValue());

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
        log.i("maker and type ids: %s, %s", id, type);
        if(TextUtils.isEmpty(id)) return;
        Query modelQuery = (type > 0) ?
                autoRef.document(id).collection("auto_model").whereEqualTo("auto_type", type) :
                autoRef.document(id).collection("auto_model");

        log.i("query starts");
        modelQuery.get().addOnSuccessListener(query -> {
            List<String> modelEntries = new ArrayList<>();

            for(QueryDocumentSnapshot modelshot : query) {
                if(modelshot.exists()) {
                    log.i("model: %s", modelshot.getString("model_name"));
                    modelEntries.add(modelshot.getString("model_name"));
                }
            }

            final int size = modelEntries.size();
            autoModel.setEntries(modelEntries.toArray(new CharSequence[size]));
            autoModel.setEntryValues(modelEntries.toArray(new CharSequence[size]));

            autoModel.setValue(null);
            autoModel.setEnabled(true);

        });
    }

}
