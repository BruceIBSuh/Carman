package com.silverback.carman.fragments;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.silverback.carman.R;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.Constants;
import com.silverback.carman.viewmodels.FragmentSharedModel;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * This fragment is a split screen PreferenceFragmentCompat which displays multiple preferences
 * on a separate screen with its own preference hierarchy that is concerned with the auto data.
 * Firestore holds comprehensive data to download but special care is required for latency.
 */
public class SettingAutoFragment extends SettingBaseFragment implements
        Preference.OnPreferenceChangeListener {

    private static final LoggingHelper log = LoggingHelperFactory.create(SettingAutoFragment.class);

    // Constants for setting year entries.
    private static final int StartYear = 20;

    private FragmentSharedModel fragmentModel;
    private ListPreference autoMaker, autoType, autoModel, engineType, autoYear;
    private EngineTypeDialogFragment engineTypeDialogFragment;
    private String makerId, modelId;
    private boolean isMakerChanged, isModelChanged, isAutoTypeChanged, isEngineTypeChanged;

    // Constructor
    public SettingAutoFragment() {
        super();
    }

    //@SuppressWarnings("ConstantConditions")
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pref_autodata, rootKey);
        setHasOptionsMenu(true);// necessary for the options menu feasible in fragment

        mSettings = PreferenceManager.getDefaultSharedPreferences(requireActivity());
        fragmentModel = new ViewModelProvider(requireActivity()).get(FragmentSharedModel.class);
        engineTypeDialogFragment = new EngineTypeDialogFragment(this);

        autoMaker = findPreference(Constants.AUTO_MAKER);
        autoType = findPreference(Constants.AUTO_TYPE);
        engineType = findPreference(Constants.ENGINE_TYPE);
        autoModel = findPreference(Constants.AUTO_MODEL);
        autoYear = findPreference(Constants.AUTO_YEAR);

        Objects.requireNonNull(autoYear).setSummaryProvider(preference -> {
            String value = ((ListPreference)preference).getValue();
            if(TextUtils.isEmpty(value)) return getString(R.string.pref_entry_void);
            else return value;
        });

        autoMaker.setOnPreferenceChangeListener(this);
        autoType.setOnPreferenceChangeListener(this);
        autoModel.setOnPreferenceChangeListener(this);
        engineType.setOnPreferenceChangeListener(this);

        // Query the auto maker to retrieve the registration number, the result of which is notified
        // to OnCompleteAutoQueryListener. Sequentially, upon completion of the auto maker query,
        // make an auto model query to have the registration number notified to the same listener,
        // then set the initial values and summaries. In particular, the summaries of the autoMaker
        // and autoModel preferences include the queried registration numbers.
        makerName = mSettings.getString(Constants.AUTO_MAKER, null);
        modelName = mSettings.getString(Constants.AUTO_MODEL, null);
        typeName = mSettings.getString(Constants.AUTO_TYPE, null);
        engineName = mSettings.getString(Constants.ENGINE_TYPE, null);
        yearName = mSettings.getString(Constants.AUTO_YEAR, null);

        // Initially, query all auto makers to set entries(values) to the auto maker preference.
        // useSimpleSummaryProvider does not work b/c every time the fragment is instantiated, the
        // entries are initially set, which may disable the summary to be called, thus zero-size
        // array should be used to initialize.
        autoRef.get().addOnSuccessListener(queries -> {
            if(queries.size() == 0) return;
            List<String> autoMakerList = new ArrayList<>();
            for(QueryDocumentSnapshot snapshot : queries) autoMakerList.add(snapshot.getId());
            autoMaker.setEntries(autoMakerList.toArray(new CharSequence[0]));
            autoMaker.setEntryValues(autoMakerList.toArray(new CharSequence[0]));
        });

        // Set the entries(values) to the auto year preference.
        List<String> yearList = new ArrayList<>();
        int year = Calendar.getInstance().get(Calendar.YEAR);
        for (int i = year + 1; i >= ((year + 1) - StartYear); i--) yearList.add(String.valueOf(i));
        String[] years = yearList.toArray(new String[0]); // zero-size array.
        autoYear.setEntries(years);
        autoYear.setEntryValues(years);

        // Given the automaker name, query the registration number of the automaker with the other
        // preferences set disabled until queryAutoMaker() completes.
        if(!TextUtils.isEmpty(makerName)) {
            queryAutoMaker(makerName);
        } else {
            autoType.setEnabled(false);
            engineType.setEnabled(false);
            autoModel.setEnabled(false);
            autoYear.setEnabled(false);
        }
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
        log.i("onPreferenceChange value: %s", valueName);
        switch(preference.getKey()) {
            // If the automaker preference changes, query the registration number, setting
            // the entries to autoModel and the void summary to autoType and autoModel as well.
            // At the same time, increase the registration number of the current automaker and
            // decrease the number of the previous auto maker, which can be retrieved by getValue();
            case Constants.AUTO_MAKER:
                isMakerChanged = true;
                autoType.setEnabled(false);
                engineType.setEnabled(false);
                autoModel.setEnabled(false);

                // Initialize the autotype and the enginetype value. These 2 preferences has the
                // void item in the list.
                typeName = getString(R.string.pref_entry_void);
                engineName = getString(R.string.pref_entry_void);

                // The previous auto maker decrease its registration number before a new maker comes
                // in unless its value is null, which usually occurs at the initial setting.
                if(!TextUtils.isEmpty(autoMaker.getValue())){
                    autoRef.document(makerId).update("reg_automaker", FieldValue.increment(-1));
                }

                // It's weirdo TextUtils.isEmpty() is not guaranteed to return false when the value
                // is null. Looks like Android team is already aware of this "We are aware that the
                // default behavior is problematic when using classes like Log or TextUtils and will
                // evaluate possible solutions in future releases."

                // The registration number of the current auto model has to be decreased b/c change
                // of the auto maker makes the auto model set to null.
                if(!TextUtils.isEmpty(autoModel.getValue())) {
                    autoRef.document(makerId).collection("autoModels").document(modelId)
                            .update("reg_model", FieldValue.increment(-1))
                            .addOnSuccessListener(aVoid -> log.i("decrease the reg number successfully"));
                }


                // Initialize the automodel only after the regit number decreases with the previous
                // automodel.
                autoModel.setValue(null);
                autoYear.setValue(null);

                // Retrieve the auto maker by a name selected from the list preference.
                queryAutoMaker(valueName);
                return true;

            case Constants.AUTO_TYPE:
                // Decrease the reg number if the autotype changes which makes the automodel void.
                if(!TextUtils.isEmpty(autoModel.getValue())) {
                    autoRef.document(makerId).collection("autoModels").document(modelId)
                            .update("reg_model", FieldValue.increment(-1));
                }

                isAutoTypeChanged = true;
                autoType.setValue(valueName);
                autoType.setSummaryProvider(autotypePref -> valueName);
                autoModel.setEnabled(false); //until query completes.

                log.i("autotype changed: %s, %s", valueName, engineType.getValue());
                setAutoModelEntries(valueName, engineType.getValue());
                return true;

            case Constants.ENGINE_TYPE:
                // Decrease the reg number if the autotype changes which makes the automodel void.
                if(!TextUtils.isEmpty(autoModel.getValue())) {
                    autoRef.document(makerId).collection("auto_model").document(modelId)
                            .update("reg_number", FieldValue.increment(-1));
                    //.addOnSuccessListener(aVoid -> log.i("decrease the reg number successfully"));
                }

                isEngineTypeChanged = true;
                engineType.setValue(valueName);
                engineType.setSummaryProvider(enginetypePref -> valueName);
                autoModel.setEnabled(false);

                log.i("enginetype changed: %s, %s", valueName, autoType.getValue());
                setAutoModelEntries(autoType.getValue(), valueName);
                return true;

            case Constants.AUTO_MODEL:
                log.i("automodel preference changed:%s, %s", modelId, autoModel.getValue());
                isModelChanged = true;
                if(!TextUtils.isEmpty(autoModel.getValue()) && !TextUtils.isEmpty(modelId)) {
                    autoRef.document(makerId).collection("autoModels").document(modelId)
                            .update("reg_model", FieldValue.increment(-1));
                }

                autoModel.setValue(valueName);
                queryAutoModel(makerId, valueName);

                // Once any automodel is selected, the autotype and enginetype should be disabled.
                //autoType.setEnabled(false);
                //engineType.setEnabled(false);
                return true;

            default: return false;
        }
    }

    // Implement the abstract method defined in SettingBaseFragment to
    //@SuppressWarnings({"ConstantConditions", "unchecked"})
    @Override
    public void queryAutoMakerSnapshot(DocumentSnapshot makershot) {
        // With the automaker id queried and the autotype id, query auto models and set them to
        // the model entry.
        makerId = makershot.getId();
        autoType.setEnabled(true);
        engineType.setEnabled(true);
        autoModel.setEnabled(true);
        autoYear.setEnabled(true);
        log.i("maker id: %s", makerId);
        // Retrieve the emblem url
        //emblem = makershot.getString("auto_emblem");

        // Use a custom object with @PropertyName annotaion to get a Map data, which would be uploaded
        // as a custom object.
        AutoData dataList = makershot.toObject(AutoData.class);
        if(makershot.get("auto_type") != null) {
            Map<String, Integer> autoTypeMap = Objects.requireNonNull(dataList).getAutoTypeMap();
            List<String> autoTypeList = new ArrayList<>(autoTypeMap.keySet());
            autoTypeList.add(0, getString(R.string.pref_entry_void));// add the void value into the first place.
            String[] arrAutoType = autoTypeList.toArray(new String[0]);

            //if(TextUtils.isEmpty(typeName)) typeName = getString(R.string.pref_entry_void);
            autoType.setEntries(arrAutoType);
            autoType.setEntryValues(arrAutoType);
            autoType.setValue(typeName);
            autoType.setSummaryProvider(autotypePref -> typeName);
        }

        if(makershot.get("engine_type") != null) {
            Map<String, Integer> engineTypeMap = Objects.requireNonNull(dataList).getEngineTypeMap();
            List<String> engineTypeList = new ArrayList<>(engineTypeMap.keySet());
            engineTypeList.add(0, getString(R.string.pref_entry_void));
            String[] arrEngineType = engineTypeList.toArray(new String[0]);

            // The engine name should be saved as null when it's not set b/c it is referenced in
            // the autofilter checkbox. To show the summary, however, the null value should be
            // the String value to select the list item and show the summary.
            engineType.setEntries(arrEngineType);
            engineType.setEntryValues(arrEngineType);
            engineType.setValue(engineName);
            engineType.setSummaryProvider(enginetypePref -> engineName);
        }

        // Retrieve the automodels by querying the automodel collection
        log.i("maker data: %s, %s, %s", makerId, typeName, engineName);
        setAutoModelEntries(typeName, engineName);

        // When the auto maker changes, which means the previous automaker is not null, update
        // the current registration number to be increased. The boolean value indicates whether
        // the query is initially made or by selecting another automaker; only the latter has to
        // increase the regit number.
        int regAutoMaker = Objects.requireNonNull(makershot.getLong("reg_automaker")).intValue();

        if(isMakerChanged) {
            regAutoMaker++;
            makershot.getReference().update("reg_automaker", FieldValue.increment(1));
            isMakerChanged = false;
        }


        // Set the summary with a spnned string.
        String makerSummary = String.format("%s%10s%s(%s)",
                //makershot.getString("auto_maker"),
                makershot.getId(),
                "",
                getString(R.string.pref_auto_reg), regAutoMaker);
        setSpannedAutoSummary(autoMaker, makerSummary);


        // If the automodel preference has a value which may get preference.getValue(), query the
        // automodel to fetch the regit number. Otherwise, set the summary
        if(!TextUtils.isEmpty(autoModel.getValue())) queryAutoModel(makerId, modelName);
        else autoModel.setSummaryProvider(preference -> getString(R.string.pref_entry_void));

        //if(isMakerChanged) isMakerChanged = false;

    }


    //@SuppressWarnings({"ConstantConditions", "unchecked"})
    @Override
    public void queryAutoModelSnapshot(DocumentSnapshot modelshot) {
        modelId = modelshot.getId();
        int regModel = Objects.requireNonNull(modelshot.getLong("reg_model")).intValue();

        // Reset the auto type value according to the auto model selected.
        /*
        String value = modelshot.getString("auto_type");
        if(!TextUtils.isEmpty((value))) {
            autoType.setValue(value);
            autoType.setSummaryProvider(autotypePref -> value);
        }
        */
        // Reset the engine type value according to the auto model selected. Typecasting issue.
        // Typecast Firestore array field to List
        AutoData data = modelshot.toObject(AutoData.class);
        Map<String, Integer> engineMap = Objects.requireNonNull(data).getEngineTypeMap();

        List<String> engineList = new ArrayList<>(engineMap.keySet());
        int engineValue = engineType.findIndexOfValue(engineType.getValue());
        if (engineList.size() == 1) {
            engineType.setValue(engineList.get(0));
            engineType.setSummaryProvider(enginetypePref -> engineList.get(0));
            //} else if (isModelChanged && dataList.size() > 1 && engineValue == 0) {
        } else if(isModelChanged && engineList.size() > 1) {
            CharSequence[] arrType = engineList.toArray(new CharSequence[0]);
            engineTypeDialogFragment.setEngineType(arrType);
            engineTypeDialogFragment.show(getChildFragmentManager(), "engineTypeFragment");
        }

        if(isModelChanged) {
            regModel++;
            autoRef.document(makerId).collection("autoModels").document(modelId)
                    .update("reg_model", FieldValue.increment(1)).addOnSuccessListener(aVoid -> {
                        log.i("update regnum successfully");
                        isModelChanged = false;
                    });

        }

        // Set the summary with a spanned string.
        String modelSummary = String.format("%s%10s%s(%s)",
                modelshot.getId(), "", getString(R.string.pref_auto_reg), regModel);
        setSpannedAutoSummary(autoModel, modelSummary);

        //if(isModelChanged) isModelChanged = false;
    }

    private void setAutoData(DocumentSnapshot snapshot) {
        // Use a custom object with @PropertyName annotaion to get a Map data, which would be uploaded
        // as a custom object.
        AutoData dataList = snapshot.toObject(AutoData.class);
        if(snapshot.get("auto_type") != null) {
            Map<String, Integer> autoTypeMap = Objects.requireNonNull(dataList).getAutoTypeMap();
            List<String> autoTypeList = new ArrayList<>(autoTypeMap.keySet());
            autoTypeList.add(0, getString(R.string.pref_entry_void));// add the void value into the first place.
            String[] arrAutoType = autoTypeList.toArray(new String[0]);

            //if(TextUtils.isEmpty(typeName)) typeName = getString(R.string.pref_entry_void);
            autoType.setEntries(arrAutoType);
            autoType.setEntryValues(arrAutoType);
            autoType.setValue(typeName);
            autoType.setSummaryProvider(autotypePref -> typeName);
        }

        if(snapshot.get("engine_type") != null) {
            Map<String, Integer> engineTypeMap = Objects.requireNonNull(dataList).getEngineTypeMap();
            List<String> engineTypeList = new ArrayList<>(engineTypeMap.keySet());
            engineTypeList.add(0, getString(R.string.pref_entry_void));
            String[] arrEngineType = engineTypeList.toArray(new String[0]);

            // The engine name should be saved as null when it's not set b/c it is referenced in
            // the autofilter checkbox. To show the summary, however, the null value should be
            // the String value to select the list item and show the summary.
            engineType.setEntries(arrEngineType);
            engineType.setEntryValues(arrEngineType);
            engineType.setValue(engineName);
            engineType.setSummaryProvider(enginetypePref -> engineName);
        }
    }

    // To make the Up button working in Fragment, it is required to invoke sethasOptionsMenu(true)
    // and the return value should be true in onOptionsItemSelected(). The values of each preference
    // is translated to List<String>, then converted to JSONString for transferring the json string
    // to SettingPerrenceFragment to invalidate the preference summary.
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            log.i("onOptionsitemSelected in SettingAutoFragment");
            // Crete JSONString which holds the preference values except the autotype. The autotype
            // is fully dependent on the auto model, thus no need for the json to contain the type.
            // The JSONString will be used to create the autofilter in the board which works as
            // the query conditions.
            List<String> dataList = new ArrayList<>();
            if(engineType.findIndexOfValue(engineType.getValue()) == 0) engineType.setValue(null);

            dataList.add(autoMaker.getValue());
            dataList.add(autoModel.getValue());
            dataList.add(engineType.getValue());
            dataList.add(autoYear.getValue());


            JSONArray json = new JSONArray(dataList);
            fragmentModel.getAutoData().setValue(json.toString());

            return true;
        }

        return false;
    }


    // This method queries auto models with automaker and autotypes as conditions. The auto maker
    // is required but the auto types may be null. Special care should be taken when async
    // queries are made. This method takes Continuation which queries auto maker first. On completion,
    // the next query is made with the integer value of auto type, which may be null.
    private void setAutoModelEntries(String type, String engine) {
        //if(TextUtils.isEmpty(makerId)) return;
        CollectionReference colRef = autoRef.document(makerId).collection("autoModels");
        Query query = colRef;

        if(!TextUtils.isEmpty(type) && autoType.findIndexOfValue(type) > 0)
            query = colRef.whereEqualTo("auto_type", type);
        /*
        if(!TextUtils.isEmpty(engine) && engineType.findIndexOfValue(engine) > 0)
            query = query.whereArrayContains("engine_type", engine);
        */
        query.get().addOnSuccessListener(queries -> {
            log.i("automodel queries: %s", queries.size());
            List<String> autoModelList = new ArrayList<>();
            for(QueryDocumentSnapshot modelshot : queries) autoModelList.add(modelshot.getId());

            autoModel.setEntries(autoModelList.toArray(new String[0]));
            autoModel.setEntryValues(autoModelList.toArray(new String[0]));
            autoModel.setEnabled(true);

            // Initialize the automodel value and summary when the autotype or enginetype has a new
            // value.
            if(isMakerChanged || isAutoTypeChanged || isEngineTypeChanged) {
                autoModel.setValue(null);
                autoModel.setSummaryProvider(preference -> getString(R.string.pref_entry_void));
            }
        });
    }

    // Static nested class to create AlertDialog to select an engine type if an model has multiple
    // engine types.
    public static class EngineTypeDialogFragment extends DialogFragment {
        private final SettingAutoFragment outerFragment;
        private CharSequence[] types;

        EngineTypeDialogFragment(SettingAutoFragment outerFragment) {
            this.outerFragment = outerFragment;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
            builder.setTitle("Engine Type")
                    .setItems(types, (dialog, which) -> {
                        outerFragment.engineType.setValue(types[which].toString());
                        outerFragment.engineType.setSummaryProvider(enginetypePref -> types[which]);
                    });

            return builder.create();
        }

        void setEngineType(CharSequence[] types) {
            this.types = types;
        }
    }


}
