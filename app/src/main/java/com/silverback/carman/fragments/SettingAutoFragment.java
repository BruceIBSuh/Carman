package com.silverback.carman.fragments;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;

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
import com.google.firebase.firestore.FieldPath;
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

/*
 * This fragment is a split screen PreferenceFragmentCompat which displays multiple preferences
 * on a separate screen with its own preference hierarchy that is concerned with the auto data.
 * Firestore holds comprehensive data to download but special care is required for latency.
 *
 *
 */
public class SettingAutoFragment extends SettingBaseFragment implements
        Preference.OnPreferenceChangeListener {

    private static final LoggingHelper log = LoggingHelperFactory.create(SettingAutoFragment.class);

    // Constants for setting year entries.
    private static final int SINCE = 20;

    private FragmentSharedModel fragmentModel;
    private ListPreference autoMaker, autoType, autoModel, engineType, autoYear;
    private EngineDialogFragment engineDialogFragment;
    private DocumentSnapshot makershot, modelshot;
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
        engineDialogFragment = new EngineDialogFragment(fragmentModel);

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
        //setAutoMakerEntries();
        autoRef.get().addOnSuccessListener(queries -> {
            List<String> autoMakerList = new ArrayList<>();
            autoMakerList.add(0, getString(R.string.pref_entry_void));
            for(QueryDocumentSnapshot snapshot : queries) autoMakerList.add(snapshot.getId());
            autoMaker.setEntries(autoMakerList.toArray(new CharSequence[0]));
            autoMaker.setEntryValues(autoMakerList.toArray(new CharSequence[0]));

            if(autoMaker.findIndexOfValue(autoMaker.getValue()) > 0) {
                queryAutoMaker(autoMaker.getValue());
            } else {
                ListPreference[] arrPrefs = {autoMaker, autoType, autoModel, engineType, autoYear};
                for(ListPreference pref : arrPrefs) setAutoPreferenceSummary(pref, 0);
            }
        });

        // Create the year list and set the entries to the autoYear preference.
        List<String> yearList = new ArrayList<>();
        yearList.add(0, getString(R.string.pref_entry_void));
        int year = Calendar.getInstance().get(Calendar.YEAR);
        for (int i = year + 1; i >= ((year + 1) - SINCE); i--) yearList.add(String.valueOf(i));
        CharSequence[] years = yearList.toArray(new CharSequence[0]); // zero-size array.
        autoYear.setEntries(years);
        autoYear.setEntryValues(years);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        fragmentModel.getEngineSelected().observe(getViewLifecycleOwner(), engine -> {
            engineType.setValue(engine.toString());
            setAutoPreferenceSummary(engineType, 0);
            updateRegField(engineType, true);
        });
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        String newValue = (String)value;
        // Prevent repeating click on the same pref.
        if(newValue.equals(((ListPreference)preference).getValue())) return false;

        switch(preference.getKey()) {
            case Constants.AUTO_MAKER:
                isMakerChanged = true;
                // Make the automaker preference set to default
                ListPreference[] arrPrefs = {autoModel, autoType, engineType, autoYear};
                for(ListPreference pref : arrPrefs) pref.setEnabled(false);

                if(autoMaker.findIndexOfValue(autoMaker.getValue()) > 0) {
                    updateRegField(autoMaker, false);
                }

                if(autoMaker.findIndexOfValue(newValue) > 0) queryAutoMaker(newValue);
                else setAutoPreferenceSummary(autoMaker, 0);

                return true;

            case Constants.AUTO_TYPE:
                isAutoTypeChanged = true;
                if(autoModel.findIndexOfValue(autoModel.getValue()) > 0) {
                    updateRegField(autoModel, false);
                }

                autoType.setValue(newValue);
                setAutoPreferenceSummary(autoType, 0);

                setAutoModelEntries(newValue, engineType.getValue());

                return true;

            case Constants.ENGINE_TYPE:
                isEngineTypeChanged = true;

                if(autoModel.findIndexOfValue(autoModel.getValue()) > 0) {
                    updateRegField(autoModel, false);
                }

                engineType.setValue(newValue);
                setAutoPreferenceSummary(engineType, 0);

                setAutoModelEntries(autoType.getValue(), newValue);
                return true;

            case Constants.AUTO_MODEL:
                isModelChanged = true;
                if(autoModel.findIndexOfValue(autoModel.getValue()) > 0) {
                    updateRegField(autoModel, false);
                }

                if(autoModel.findIndexOfValue(newValue) > 0) {
                    queryAutoModel(makershot.getId(), newValue);
                } else {
                    //setAutoPreferenceSummary(autoModel, 0);
                    ListPreference[] arrPref = {autoModel, autoType, engineType, autoYear};
                    for(ListPreference pref : arrPref) {
                        pref.setValue(null);
                        pref.setValueIndex(0);
                        setAutoPreferenceSummary(pref, 0);
                    }
                }


                return true;

            default: return false;
        }
    }

    // Implement the abstract method defined in SettingBaseFragment to
    //@SuppressWarnings({"ConstantConditions", "unchecked"})
    @Override
    public void queryAutoMakerSnapshot(DocumentSnapshot makershot) {
        this.makershot = makershot;
        // Retrieve the auto_type and engine_type fields as Map object, typecasting them to
        // ArrayList and String arrays.
        String typeValue = autoType.getValue();
        String engineValue = engineType.getValue();

        ObjectAutoData dataList = makershot.toObject(ObjectAutoData.class);
        if(makershot.get("auto_type") != null) {
            Map<String, Integer> autotypeMap = Objects.requireNonNull(dataList).getAutoTypeMap();
            List<String> autotypeList = new ArrayList<>(autotypeMap.keySet());
            autotypeList.add(0, getString(R.string.pref_entry_void));
            String[] arrAutotype = autotypeList.toArray(new String[0]);

            autoType.setEntries(arrAutotype);
            autoType.setEntryValues(arrAutotype);
            if(!autoType.isEnabled()) autoType.setEnabled(true);

        }
        // Set the enes to the engineType preference
        if(makershot.get("engine_type") != null) {
            Map<String, Integer> enginetypeMap = Objects.requireNonNull(dataList).getEngineTypeMap();
            List<String> enginetypeList = new ArrayList<>(enginetypeMap.keySet());
            enginetypeList.add(0, getString(R.string.pref_entry_void));
            String[] arrEngineType = enginetypeList.toArray(new String[0]);

            engineType.setEntries(arrEngineType);
            engineType.setEntryValues(arrEngineType);
            if(!engineType.isEnabled()) engineType.setEnabled(true);
        }

        // Set the automodel entries(entryValues). In this case, the params should be null, which
        // means no conditioned query will be made.
        autoModel.setEnabled(true);
        setAutoModelEntries(typeValue, engineValue);

        autoYear.setEnabled(true);
        setAutoPreferenceSummary(autoYear, 0);


        // Set or update the registration number according to whether the automaker preference
        // has changed.
        if(isMakerChanged) {
            updateRegField(autoMaker, true);
            isMakerChanged = false;
        } else {
            int num = Objects.requireNonNull(makershot.getLong("reg_maker")).intValue();
            setAutoPreferenceSummary(autoMaker, num);
        }
    }

    //@SuppressWarnings({"ConstantConditions", "unchecked"})
    @Override
    public void queryAutoModelSnapshot(DocumentSnapshot modelshot) {
        this.modelshot = modelshot;

        final String autotype = modelshot.getString("model_type");
        if(!TextUtils.isEmpty(autotype)) autoType.setValue(autotype);
        else setAutoPreferenceSummary(autoType, 0);

        if(modelshot.get("model_engine") != null) {
            ArrayAutoData autodata = modelshot.toObject(ArrayAutoData.class);
            List<String> engineList = Objects.requireNonNull(autodata).getEngineTypeList();
            if(engineList.size() == 1) {
                engineType.setValue(engineList.get(0));
                //updateRegField(engineType, engineType.getValue(), true);
                setAutoPreferenceSummary(engineType, 0);
            } else if(isModelChanged && engineList.size() > 1) {
                CharSequence[] arrType = engineList.toArray(new CharSequence[0]);
                engineDialogFragment.setEngineTypes(arrType);
                engineDialogFragment.show(getChildFragmentManager(), "engineFragment");
            }
        }

        if(isModelChanged) {
            updateRegField(autoModel, true);
            isModelChanged = false;
        } else {
            int num = Objects.requireNonNull(modelshot.getLong("reg_model")).intValue();
            setAutoPreferenceSummary(autoModel, num);
            //setAutoPreferenceSummary(autoType, 0);
            //setAutoPreferenceSummary(engineType, 0);
            //setAutoPreferenceSummary(autoYear, 0);
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
            if(autoMaker.findIndexOfValue(autoMaker.getValue()) == 0) autoMaker.setValue(null);
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

    // This method queries auto models with automaker and autotypes as conditions. The auto maker
    // is required but the auto types may be null. Special care should be taken when async
    // queries are made. This method takes Continuation which queries auto maker first. On completion,
    // the next query is made with the integer value of auto type, which may be null.
    private void setAutoModelEntries(String autotype, String enginetype) {
        List<String> automodelList = new ArrayList<>();
        automodelList.add(0, getString(R.string.pref_entry_void));

        Query query = makershot.getReference().collection("automodels");
        if(autoType.findIndexOfValue(autotype) > 0) {
            query = query.whereEqualTo("model_type", autotype);
        }
        if(engineType.findIndexOfValue(enginetype) > 0) {
            query = query.whereArrayContains("model_engine", enginetype);
        }

        query.get().addOnSuccessListener(queries -> {
            for(QueryDocumentSnapshot modelshot : queries) automodelList.add(modelshot.getId());
            autoModel.setEntries(automodelList.toArray(new String[0]));
            autoModel.setEntryValues(automodelList.toArray(new String[0]));

            if(isAutoTypeChanged || isEngineTypeChanged) {
                setAutoPreferenceSummary(autoModel, 0);
                autoModel.setValue(null);
                isAutoTypeChanged = false;
                isEngineTypeChanged = false;

                return;
            }

            if(autoModel.findIndexOfValue(autoModel.getValue()) > 0) {
                queryAutoModel(makershot.getId(), autoModel.getValue());
            }
        });
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

    private void updateRegField(ListPreference pref, boolean isIncrement){
        int inc = (isIncrement) ? 1 : -1;
        final DocumentReference makerRef = autoRef.document(makershot.getId());

        if(pref.equals(autoMaker)) {
            FirebaseFirestore.getInstance().runTransaction(transaction -> {
                transaction.update(makerRef, "reg_maker", FieldValue.increment(inc));
                final String model = autoModel.getValue();
                if (autoModel.findIndexOfValue(model) > 0) {
                    DocumentReference modelRef = makerRef.collection("automodels").document(model);
                    FieldPath regEnginePath = FieldPath.of("reg_engine", engineType.getValue());
                    FieldPath engineTypePath = FieldPath.of("engine_type", engineType.getValue());
                    FieldPath autoTypePath = FieldPath.of("auto_type", autoType.getValue());

                    transaction.update(modelRef, "reg_model", FieldValue.increment(inc));
                    transaction.update(modelRef, regEnginePath, FieldValue.increment(inc));
                    transaction.update(makerRef, engineTypePath, FieldValue.increment(inc));
                    transaction.update(makerRef, autoTypePath, FieldValue.increment(inc));
                }

                return null;

            }).addOnSuccessListener(aVoid -> {
                if(isIncrement) {
                    makerRef.get().addOnSuccessListener(doc -> {
                        int num = Objects.requireNonNull(doc.getLong("reg_maker")).intValue();
                        setAutoPreferenceSummary(autoMaker, num);
                    });
                }

                ListPreference[] arrPrefs = {autoModel, autoType, engineType, autoYear};
                for(ListPreference preference : arrPrefs) {
                    preference.setValueIndex(0);
                    preference.setValue(null);
                    setAutoPreferenceSummary(preference, 0);
                }

            }).addOnFailureListener(e -> log.e("update automaker field failed:%s", e));


        } else if(pref.equals(autoModel)) {
            DocumentReference modelRef = makerRef.collection("automodels").document(modelshot.getId());
            FirebaseFirestore.getInstance().runTransaction(transaction -> {
                DocumentSnapshot snapshot = transaction.get(modelRef);
                FieldPath autotypePath = FieldPath.of("auto_type", snapshot.getString("model_type"));
                FieldPath enginePath = FieldPath.of("engine_type", engineType.getValue());
                FieldPath regEngine = FieldPath.of("reg_engine", engineType.getValue());

                transaction.update(makerRef, autotypePath, FieldValue.increment(inc));
                transaction.update(modelRef, "reg_model", FieldValue.increment(inc));
                if (!isIncrement) {
                    transaction.update(makerRef, enginePath, FieldValue.increment(inc));
                    transaction.update(modelRef, regEngine, FieldValue.increment(inc));
                }
                return null;
            }).addOnSuccessListener(aVoid -> {
                if(isIncrement) {
                    modelRef.get().addOnSuccessListener(doc -> {
                        int num = Objects.requireNonNull(doc.getLong("reg_model")).intValue();
                        setAutoPreferenceSummary(autoModel, num);
                    });
                } else {
                    log.i("automodel changed: %s", autoModel.getValue());
                }

            }).addOnFailureListener(e -> log.e("update transaction failed: %s", e.getMessage()));


        } else if(pref.equals(engineType)) {
            DocumentReference modelRef = makerRef.collection("automodels").document(modelshot.getId());
            FirebaseFirestore.getInstance().runTransaction(transaction -> {
                final String engine = engineType.getValue();
                transaction.update(makerRef, FieldPath.of("engine_type", engine), FieldValue.increment(inc));
                transaction.update(modelRef, FieldPath.of("reg_engine", engine), FieldValue.increment(inc));
                return null;
            });
        }
    }




    // Static nested class to create AlertDialog to select an engine type if an model has multiple
    // engine types.
    public static class EngineDialogFragment extends DialogFragment {
        private final FragmentSharedModel fragmentModel;
        private CharSequence[] types;

        EngineDialogFragment(FragmentSharedModel model) {
            this.fragmentModel = model;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
            builder.setTitle("Engine Type").setItems(types, (dialogInterface, index) ->
                    fragmentModel.getEngineSelected().setValue(types[index]));

            return builder.create();
        }

        void setEngineTypes(CharSequence[] types) {
            this.types = types;
        }
    }


}
