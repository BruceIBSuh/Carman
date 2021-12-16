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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
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
    private static final int SINCE = 20;

    private FragmentSharedModel fragmentModel;
    private ListPreference autoMaker, autoType, autoModel, engineType, autoYear;
    private EngineTypeDialogFragment engineTypeDialogFragment;
    private String makerId, modelId;
    //private DocumentSnapshot makershot;
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

        autoMaker.setOnPreferenceChangeListener(this);
        autoType.setOnPreferenceChangeListener(this);
        autoModel.setOnPreferenceChangeListener(this);
        engineType.setOnPreferenceChangeListener(this);

        // Query the automakers and set the entries to the autoMaker preference.
        autoRef.get().addOnSuccessListener(queries -> {
            if(queries.size() == 0) return;
            List<String> autoMakerList = new ArrayList<>();
            autoMakerList.add(0, getString(R.string.pref_entry_void));
            for(QueryDocumentSnapshot snapshot : queries) autoMakerList.add(snapshot.getId());
            autoMaker.setEntries(autoMakerList.toArray(new CharSequence[0]));
            autoMaker.setEntryValues(autoMakerList.toArray(new CharSequence[0]));

            final String maker = autoMaker.getValue();
            if(autoMaker.findIndexOfValue(maker) > 0) {
                autoMaker.setValue(maker);
                queryAutoMaker(maker);
            } else {
                setAutoPreferenceSummary(autoMaker, 0);
                autoMaker.setValue(null);
            }
        });

        // Create the year list and set the entries to the autoYear preference.
        List<String> yearList = new ArrayList<>();
        yearList.add(0, getString(R.string.pref_entry_void));
        int year = Calendar.getInstance().get(Calendar.YEAR);
        for (int i = year + 1; i >= ((year + 1) - SINCE); i--) yearList.add(String.valueOf(i));
        String[] years = yearList.toArray(new String[0]); // zero-size array.
        autoYear.setEntries(years);
        autoYear.setEntryValues(years);
    }


    // The autoType and autoModel preference depend on the autoMaker one in terms of setting entries
    // and values, which means that the autoMaker has a new value, the autoType gets no set and
    // the autoModel queries new entries with the new autoMaker value.
    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        // Set an initiall summary to the auto maker preference with tne number registered.
        // If successful, set the auto model preference to be enabled and set entries to it
        // queried with the maker and type id.
        String entryValue = (String)value;
        switch(preference.getKey()) {
            // If the automaker preference changes, query the registration number, setting
            // the entries to autoModel and the void summary to autoType and autoModel as well.
            // At the same time, increase the registration number of the current automaker and
            // decrease the number of the previous auto maker, which can be retrieved by getValue();
            case Constants.AUTO_MAKER:
                if(entryValue.equals(autoMaker.getValue())) return false;

                isMakerChanged = true;
                ListPreference[] arrPref = {autoType, engineType, autoModel};
                resetAutoPreferences(arrPref);

                if(autoMaker.findIndexOfValue(entryValue) > 0) queryAutoMaker(entryValue);
                else setAutoPreferenceSummary(autoMaker, 0);

                /*
                // Decrease the previous automaier reg number
                if(autoMaker.findIndexOfValue(autoMaker.getValue()) > 0) {
                    log.i("Decrease the number");
                    autoRef.document(autoMaker.getValue()).update("reg_automaker", FieldValue.increment(-1));
                }

                if(autoMaker.findIndexOfValue(entryValue) > 0) {
                    autoRef.document(entryValue).update("reg_automaker", FieldValue.increment(1));
                }
                 */
                updateRegFields(autoMaker, entryValue);

                return true;

            case Constants.AUTO_TYPE:
                if(entryValue.equals(autoType.getValue())) return true;
                isAutoTypeChanged = true;
                autoType.setValue(entryValue);
                autoType.setSummaryProvider(autotypePref -> entryValue);
                autoModel.setEnabled(false); //until query completes.

                log.i("autotype changed: %s, %s", entryValue, engineType.getValue());
                setAutoModelEntries(entryValue, engineType.getValue());
                return true;

            case Constants.ENGINE_TYPE:
                if(entryValue.equals(engineType.getValue())) return true;

                isEngineTypeChanged = true;
                engineType.setValue(entryValue);
                engineType.setSummaryProvider(enginetypePref -> entryValue);
                autoModel.setEnabled(false);

                setAutoModelEntries(autoType.getValue(), entryValue);
                return true;

            case Constants.AUTO_MODEL:
                if(entryValue.equals(autoModel.getValue())) return true;
                isModelChanged = true;
                /*
                if(!TextUtils.isEmpty(autoModel.getValue()) && !TextUtils.isEmpty(modelId)) {
                    autoRef.document(makerId).collection("automodels").document(modelId)
                            .update("reg_model", FieldValue.increment(-1));
                }
                */
                log.i("automodel: %s, %s", entryValue, autoModel.getValue());
                if(autoModel.findIndexOfValue(entryValue) > 0) queryAutoModel(makerId, entryValue);
                else setAutoPreferenceSummary(autoModel, 0);

                updateRegFields(autoModel, entryValue);

                /*
                if(autoModel.findIndexOfValue(autoModel.getValue()) > 0) {
                    autoRef.document(makerId).collection("automodels").document(autoModel.getValue())
                            .update("reg_model", FieldValue.increment(-1));
                }

                if(autoModel.findIndexOfValue(entryValue) > 0) {
                    autoRef.document(makerId).collection("automodels").document(entryValue)
                            .update("reg_model", FieldValue.increment(1));
                }

                 */

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
        makerId = makershot.getId();
        int num = Objects.requireNonNull(makershot.getLong("reg_automaker")).intValue();
        setAutoPreferenceSummary(autoMaker, num);

        String typeValue = autoType.getValue();
        String engineValue = engineType.getValue();
        // Retrieve the auto_type and engine_type fields as Map object, typecasting them to
        // ArrayList and String arrays.
        ObjectAutoData dataList = makershot.toObject(ObjectAutoData.class);
        if(makershot.get("auto_type") != null) {
            Map<String, Integer> autotypeMap = Objects.requireNonNull(dataList).getAutoTypeMap();
            List<String> autotypeList = new ArrayList<>(autotypeMap.keySet());
            autotypeList.add(0, getString(R.string.pref_entry_void));
            String[] arrAutotype = autotypeList.toArray(new String[0]);

            autoType.setEntries(arrAutotype);
            autoType.setEntryValues(arrAutotype);
            autoType.setEnabled(true);

            autoType.setValue(typeValue);
            if(TextUtils.isEmpty(typeValue)) {
                autoType.setValueIndex(0);
                setAutoPreferenceSummary(autoType, 0);
            }

        }
        // Set the enes to the engineType preference
        if(makershot.get("engine_type") != null) {
            Map<String, Integer> enginetypeMap = Objects.requireNonNull(dataList).getEngineTypeMap();
            List<String> enginetypeList = new ArrayList<>(enginetypeMap.keySet());
            enginetypeList.add(0, getString(R.string.pref_entry_void));
            String[] arrEngineType = enginetypeList.toArray(new String[0]);

            engineType.setEntries(arrEngineType);
            engineType.setEntryValues(arrEngineType);
            engineType.setEnabled(true);

            engineType.setValue(engineValue);
            if(TextUtils.isEmpty(engineValue)) {
                engineType.setValueIndex(0);
                setAutoPreferenceSummary(engineType, 0);
            }

        }

        // Retrieve the automodels by querying the automodel collection
        autoYear.setEnabled(true);
        if(TextUtils.isEmpty(autoYear.getValue())) {
            autoYear.setValue(null);
            setAutoPreferenceSummary(autoYear, 0);
        }

        setAutoModelEntries(typeValue, engineValue);

        // If the automodel preference has a value which may get preference.getValue(), query the
        // automodel to fetch the regit number. Otherwise, set the summary
        //if(!TextUtils.isEmpty(autoModel.getValue())) queryAutoModel(makerId, autoModel.getValue());
        /*
        if(autoModel.findIndexOfValue(autoModel.getValue()) > 0) {
            queryAutoModel(makerId, autoModel.getValue());
        } else setAutoPreferenceSummary(autoModel, 0);
        */

        // When the auto maker changes, which means the previous automaker is not null, update
        // the current registration number to be increased. The boolean value indicates whether
        // the query is initially made or by selecting another automaker; only the latter has to
        // increase the regit number.
        /*
        int regAutoMaker = Objects.requireNonNull(makershot.getLong("reg_automaker")).intValue();
        if(isMakerChanged) {
            regAutoMaker++;
            makershot.getReference().update("reg_automaker", FieldValue.increment(1));
            isMakerChanged = false;
        }



        // Set the summary with a spnned string.
        String makerSummary = String.format("%s%10s%s(%s)",
                makershot.getId(),
                "",
                getString(R.string.pref_auto_reg), regAutoMaker);
        setSpannedAutoSummary(autoMaker, makerSummary);
        */



    }

    //@SuppressWarnings({"ConstantConditions", "unchecked"})
    @Override
    public void queryAutoModelSnapshot(DocumentSnapshot modelshot) {
        modelId = modelshot.getId();
        int regModel = Objects.requireNonNull(modelshot.getLong("reg_model")).intValue();
        setAutoPreferenceSummary(autoModel, regModel);
        log.i("automodel id: %s", modelId);

        final String autotype = modelshot.getString("auto_type");
        //if(!TextUtils.isEmpty(autotype)) {
        if(autoType.findIndexOfValue(autotype) > 0) {
            autoType.setValue(autotype);
            //autoType.setSummaryProvider(pref -> autotype);
            setAutoPreferenceSummary(autoType, 0);
        }

        if(modelshot.get("engine_type") != null) {
            ArrayAutoData autodata = modelshot.toObject(ArrayAutoData.class);
            List<String> enginetypeList = Objects.requireNonNull(autodata).getEngineTypeList();
            log.i("engine type: %s", enginetypeList.size());
            if (enginetypeList.size() == 1) {
                engineType.setValue(enginetypeList.get(0));
                //engineType.setSummaryProvider(enginetypePref -> enginetypeList.get(0));
                setAutoPreferenceSummary(engineType, 0);
            } else if(isModelChanged && enginetypeList.size() > 1) {
                CharSequence[] arrType = enginetypeList.toArray(new CharSequence[0]);
                engineTypeDialogFragment.setEngineType(arrType);
                engineTypeDialogFragment.show(getChildFragmentManager(), "engineFragment");
            }
        }

        /*
        if(isModelChanged) {
            regModel++;
            autoRef.document(makerId).collection("automodels").document(modelId)
                    .update("reg_model", FieldValue.increment(1)).addOnSuccessListener(aVoid -> {
                        log.i("update regnum successfully");
                        isModelChanged = false;
                    });
        }

         */

        // Set the summary with a spanned string.
        /*
        String modelSummary = String.format("%s%10s%s(%s)",
                modelshot.getId(), "", getString(R.string.pref_auto_reg), regModel);
        setSpannedAutoSummary(autoModel, modelSummary);
        */

        //setAutoPreferenceSummary(autoModel, 0);
        //if(isModelChanged) isModelChanged = false;
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
            //if(autoMaker.findIndexOfValue(autoMaker.getValue()) == 0) autoMaker.setValue(null);
            //if(engineType.findIndexOfValue(engineType.getValue()) == 0) engineType.setValue(null);

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

    private void resetAutoPreferences(ListPreference... preferences) {
        for(ListPreference preference : preferences) {
            preference.setValue(null);
            preference.setEntries(new CharSequence[0]);
            preference.setEntryValues(null);
            setAutoPreferenceSummary(preference, 0);
        }
    }
    // Method for setting the initial default summary to each preferences.
    // List/Edit)preference.setSummaryProvider(SummaryProvider(I))
    private void setAutoPreferenceSummary(ListPreference preference, int num) {
        preference.setSummaryProvider(pref -> {
            String value = preference.getValue();
            String output = (num == 0)? value : value + " [" + num + "]";
            return (TextUtils.isEmpty(value))? getString(R.string.pref_entry_void): output;
        });

    }

    private void updateRegFields(ListPreference pref, String value) {
        log.i("model id: %s", modelId);
        if(pref.equals(autoMaker)) {
            if(pref.findIndexOfValue(pref.getValue()) > 0) {
                autoRef.document(pref.getValue()).update("reg_automaker", FieldValue.increment(-1));
            }
            if(pref.findIndexOfValue(value) > 0) {
                autoRef.document(value).update("reg_automaker", FieldValue.increment(1));
            }

            if(!TextUtils.isEmpty(modelId)) {
                log.i("required to update model reg");
            }

        } else if(pref.equals(autoModel)) {
            /*
            if(pref.findIndexOfValue(pref.getValue()) > 0) {
                autoRef.document(pref.getValue()).collection("automodels").document(modelId)
                        .update("reg_model", FieldValue.increment(-1));
            }
            if(pref.findIndexOfValue(value) > 0) {
                autoRef.document(makerId).collection("automodels").document(value)
                        .update("reg_model", FieldValue.increment(1));
            }

             */

        }

    }


    // This method queries auto models with automaker and autotypes as conditions. The auto maker
    // is required but the auto types may be null. Special care should be taken when async
    // queries are made. This method takes Continuation which queries auto maker first. On completion,
    // the next query is made with the integer value of auto type, which may be null.
    private void setAutoModelEntries(String autotype, String enginetype) {
        List<String> automodelList = new ArrayList<>();
        automodelList.add(0, getString(R.string.pref_entry_void));

        CollectionReference modelColRef = autoRef.document(makerId).collection("automodels");
        Query query = modelColRef;
        if(!TextUtils.isEmpty(autotype) && autoType.findIndexOfValue(autotype) > 0) {
            query = modelColRef.whereEqualTo("auto_type", autotype);
        }
        if(!TextUtils.isEmpty(enginetype) && engineType.findIndexOfValue(enginetype) > 0) {
            query = query.whereArrayContains("engine_type", enginetype);
        }

        query.get().addOnSuccessListener(queries -> {
            for(QueryDocumentSnapshot modelshot : queries) automodelList.add(modelshot.getId());
            autoModel.setEntries(automodelList.toArray(new String[0]));
            autoModel.setEntryValues(automodelList.toArray(new String[0]));
            autoModel.setEnabled(true);

            // Initialize the automodel value and summary when the autotype or enginetype has a new
            // value.
            //if(isMakerChanged || isAutoTypeChanged || isEngineTypeChanged) {
            if(isAutoTypeChanged || isEngineTypeChanged){
                autoModel.setValue(null);
                autoModel.setSummaryProvider(preference -> getString(R.string.pref_entry_void));
            }
        });
    }

    // Static nested class to create AlertDialog to select an engine type if an model has multiple
    // engine types.
    public static class EngineTypeDialogFragment extends DialogFragment {
        private final SettingAutoFragment autoFragment;
        private CharSequence[] types;
        private int which;

        EngineTypeDialogFragment(SettingAutoFragment autoFragment) {
            this.autoFragment = autoFragment;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
            builder.setTitle("Engine Type")
                    .setItems(types, (dialog, which) -> {
                        this.which = which;
                        autoFragment.engineType.setValue(types[which].toString());
                        autoFragment.engineType.setSummaryProvider(pref -> types[which]);
                    });

            return builder.create();
        }

        void setEngineType(CharSequence[] types) {
            this.types = types;
        }
        int getSelectedEngineType() {
            return which;
        }

    }


}
