package com.silverback.carman2.fragments;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
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
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private FragmentSharedModel fragmentModel;
    private OnToolbarTitleListener mToolbarListener;
    private ListPreference autoMaker, autoType, autoModel, engineType, autoYear;

    // UIs
    private EngineTypeDialogFragment engineTypeDialogFragment;

    // fields
    private String makerId, modelId;
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

        mSettings = ((SettingPreferenceActivity)getActivity()).getSettings();
        fragmentModel = new ViewModelProvider(requireActivity()).get(FragmentSharedModel.class);
        engineTypeDialogFragment = new EngineTypeDialogFragment(this);

        autoMaker = findPreference(Constants.AUTO_MAKER);
        autoType = findPreference(Constants.AUTO_TYPE);
        engineType = findPreference(Constants.ENGINE_TYPE);
        autoModel = findPreference(Constants.AUTO_MODEL);
        autoYear = findPreference(Constants.AUTO_YEAR);

        // Set the custom summary provider for the autoyear preference.
        autoYear.setSummaryProvider(preference -> {
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

        // Initially, query all auto makers to set entry(values) to the auto maker preference.
        // useSimpleSummaryProvider does not work b/c every time the fragment is instantiated, the
        // entries is set which may disable to call the summary.
        autoRef.get().addOnSuccessListener(queries -> {
            List<String> autoMakerList = new ArrayList<>();
            for(QueryDocumentSnapshot snapshot : queries) autoMakerList.add(snapshot.getString("auto_maker"));
            autoMaker.setEntries(autoMakerList.toArray(new CharSequence[queries.size()]));
            autoMaker.setEntryValues(autoMakerList.toArray(new CharSequence[queries.size()]));
        });

        // Set the entries(values) to the auto year preference.
        List<String> yearList = new ArrayList<>();
        int year = Calendar.getInstance().get(Calendar.YEAR);
        for (int i = year; i >= (year - LONGEVITY); i--) yearList.add(String.valueOf(i));
        String[] mYearEntries = yearList.toArray(new String[LONGEVITY]);
        autoYear.setEntries(mYearEntries);
        autoYear.setEntryValues(mYearEntries);

        // As long as the automaker name is given, query the auto data to retrieve the reg. number
        // of that automaker and continue to query the auto model if an auto model name is provided.
        // Otherwise, dependent preferences should be disabled until an automaker is selected.
        if(!TextUtils.isEmpty(makerName)) {
            queryAutoMaker(makerName);
        } else {
            autoType.setEnabled(false);
            engineType.setEnabled(false);
            autoModel.setEnabled(false);
            autoYear.setEnabled(false);
        }
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

                autoType.setEnabled(false);
                engineType.setEnabled(false);
                autoModel.setEnabled(false);

                // Initialize the autotype and the enginetype value. These 2 preferences has the
                // void item in the list.
                typeName = getString(R.string.pref_entry_void);
                engineName = getString(R.string.pref_entry_void);

                // Initialize the automodel only after the regit number decreases with the previous
                // automodel.
                autoModel.setValue(null);
                autoYear.setValue(null);

                // The previous auto maker, if any, decrease its reg number before a new maker comes in
                // unless its value is null, which occurs at the initial setting.
                if(!TextUtils.isEmpty(autoMaker.getValue())){
                    log.i("autoMaker preferernce: %s", autoMaker.getValue());
                    autoRef.document(makerId).update("reg_number", FieldValue.increment(-1));
                }

                // It's weirdo TextUtils.isEmpty() is not guaranteed to return false when the value
                // is null. Looks like Android team is already aware of this "We are aware that the
                // default behavior is problematic when using classes like Log or TextUtils and will
                // evaluate possible solutions in future releases."

                // The reg number of the current auto model has to be decreased b/c change of
                // the auto maker makes the auto model set to null.
                log.i("autoModel value: %s", autoModel.getValue());
                if(!TextUtils.isEmpty(autoModel.getValue()) && !TextUtils.isEmpty(modelId)) {
                    autoRef.document(makerId).collection("auto_model").document(modelId)
                            .update("reg_number", FieldValue.increment(-1))
                            .addOnSuccessListener(aVoid -> log.i("decrease the reg number successfully"));
                }



                // Retrieve the auto maker by a name selected from the list preference.
                queryAutoMaker(valueName);
                return true;

            case Constants.AUTO_TYPE:
                autoType.setValue(valueName);
                autoType.setSummary(valueName);
                autoModel.setEnabled(false); //until query completes.

                setAutoModelEntries(makerId, valueName, engineType.getValue());
                return true;

            case Constants.ENGINE_TYPE:
                engineType.setValue(valueName);
                engineType.setSummary(valueName);
                autoModel.setEnabled(false);

                setAutoModelEntries(makerId, autoType.getValue(), valueName);
                return true;

            case Constants.AUTO_MODEL:
                isModelChanged = true;
                if(!TextUtils.isEmpty(autoModel.getValue()) && !TextUtils.isEmpty(modelId)) {
                    autoRef.document(makerId).collection("auto_model").document(modelId)
                            .update("reg_number", FieldValue.increment(-1));
                }

                autoModel.setValue(valueName);
                queryAutoModel(makerId, valueName);
                return true;

            default: return false;
        }

    }

    // Implement the abstract method defined in SettingBaseFragment to
    @SuppressWarnings({"ConstantConditions", "unchecked"})
    @Override
    public void queryAutoMakerSnapshot(DocumentSnapshot makershot) {
        // With the automaker id queried and the autotype id, query auto models and set them to
        // the model entry.
        makerId = makershot.getId();
        autoType.setEnabled(true);
        engineType.setEnabled(true);
        autoModel.setEnabled(true);
        autoYear.setEnabled(true);

        // Reset the auto type and the engine type to the initial state.
        if(makershot.get("auto_type") != null) {
            List<String> autoTypeList = (List<String>)makershot.get("auto_type");
            autoTypeList.add(0, getString(R.string.pref_entry_void));
            String[] arrAutoType = autoTypeList.toArray(new String[0]);
            autoType.setEntries(arrAutoType);
            autoType.setEntryValues(arrAutoType);
            autoType.setValue(typeName);
            autoType.setSummary(typeName);
        }

        if(makershot.get("engine_type") != null) {
            List<String> engineTypeList = (List<String>) makershot.get("engine_type");
            engineTypeList.add(0, getString(R.string.pref_entry_void));
            String[] arrEngineType = engineTypeList.toArray(new String[0]);
            engineType.setEntries(arrEngineType);
            engineType.setEntryValues(arrEngineType);
            engineType.setValue(engineName);
            engineType.setSummary(engineName);
        }


        setAutoModelEntries(makerId, typeName, engineName);

        // When the auto maker changes, which means the previous automaker is not null, update
        // the current registration number to be increased. The boolean value indicates whether
        // the query is initially made or by selecting another automaker; only the latter has to
        // increase the regit number.
        int makerRegitNum = makershot.getLong("reg_number").intValue();
        if(isMakerChanged) {
            makerRegitNum++;
            makershot.getReference().update("reg_number", FieldValue.increment(1));
            isMakerChanged = false;
        }

        // Set the summary with a spnned string.
        String makerSummary = String.format("%s%10s%s(%s)",
                makershot.getString("auto_maker"), "", getString(R.string.pref_auto_reg), makerRegitNum);
        setSpannedAutoSummary(autoMaker, makerSummary);


        // If the automodel preference has a value which may get preference.getValue(), query the
        // automodel to fetch the regit number. Otherwise, set the summary
        if(!TextUtils.isEmpty(autoModel.getValue())) {
            isModelChanged = false;
            queryAutoModel(makerId, modelName);
        } else autoModel.setSummary(getString(R.string.pref_entry_void));

    }


    @SuppressWarnings({"ConstantConditions", "unchecked"})
    @Override
    public void queryAutoModelSnapshot(DocumentSnapshot modelshot) {

        modelId = modelshot.getId();
        int modelRegitNum = modelshot.getLong("reg_number").intValue();

        // Reset the auto type value according to the auto model selected.
        String typeAuto = modelshot.getString("auto_type");
        if(!TextUtils.isEmpty((typeAuto))) {
            autoType.setValue(typeAuto);
            autoType.setSummary(typeAuto);
        }

        // Reset the engine type value according to the auto model selected. Typecasting issue.
        List<String> typeList = (List<String>) modelshot.get("engine_type");
        if (typeList.size() == 1) {
            engineType.setValue(typeList.get(0));
            engineType.setSummary(typeList.get(0));
        } else if (typeList.size() > 1 && isModelChanged) {
            CharSequence[] arrType = typeList.toArray(new CharSequence[0]);
            engineTypeDialogFragment.setEngineType(arrType);
            engineTypeDialogFragment.show(getChildFragmentManager(), "engineTypeFragment");
        }

        if(isModelChanged) {
            modelRegitNum++;
            autoRef.document(makerId).collection("auto_model").document(modelId)
                    .update("reg_number", FieldValue.increment(1))
                    .addOnSuccessListener(aVoid -> log.i("update regnum successfully"));
        }

        // Set the summary with a spanned string.
        String modelSummary = String.format("%s%10s%s(%s)",
                modelshot.getString("model_name"), "", getString(R.string.pref_auto_reg), modelRegitNum);
        setSpannedAutoSummary(autoModel, modelSummary);

        isModelChanged = false;
    }



    // To make the Up button working in Fragment, it is required to invoke sethasOptionsMenu(true)
    // and the return value should be true in onOptionsItemSelected(). The values of each preference
    // is translated to List<String>, then converted to JSONString for transferring the json string
    // to SettingPerrenceFragment to invalidate the preference summary.
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if(item.getItemId() == android.R.id.home) {

            // Crete JSONString which holds the preference values except the autotype. The autotype
            // is fully dependent on the auto model, thus no need for the json to contain the type.
            // The JSONString will be used to create the autofilter in the board which works as
            // the query conditions.
            List<String> dataList = new ArrayList<>();
            /*
            dataList.add(mSettings.getString(Constants.AUTO_MAKER, ""));
            dataList.add(mSettings.getString(Constants.AUTO_MODEL, ""));
            dataList.add(mSettings.getString(Constants.ENGINE_TYPE, ""));
            dataList.add(mSettings.getString(Constants.AUTO_YEAR, ""));
            */
            dataList.add(autoMaker.getValue());
            dataList.add(autoModel.getValue());
            dataList.add(engineType.getValue());
            dataList.add(autoYear.getValue());
            for(String data : dataList) log.i("data: %s", data);

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
    private void setAutoModelEntries(String id, String type, String engine) {
        if(TextUtils.isEmpty(id)) return;

        CollectionReference colRef = autoRef.document(id).collection("auto_model");
        Query query = colRef;

        if(!TextUtils.isEmpty(type) && autoType.findIndexOfValue(type) > 0)
            query = colRef.whereEqualTo("auto_type", type);

        if(!TextUtils.isEmpty(engine) && engineType.findIndexOfValue(engine) > 0)
            query = query.whereArrayContains("engine_type", engine);

        query.get().addOnSuccessListener(queries -> {
            List<String> autoModelList = new ArrayList<>();
            for(QueryDocumentSnapshot modelshot : queries)
                autoModelList.add(modelshot.getString("model_name"));

            autoModel.setEntries(autoModelList.toArray(new String[0]));
            autoModel.setEntryValues(autoModelList.toArray(new String[0]));
            autoModel.setEnabled(true);

            // Initialize the automodel value and summary when the autotype or enginetype has a new
            // value.
            autoModel.setValue(null);
            autoModel.setSummary(getString(R.string.pref_entry_void));
        });
    }

    // Static nested class to create AlertDialog to select an engine type if an model has multiple
    // engine type.
    @SuppressWarnings("ConstantConditions")
    public static class EngineTypeDialogFragment extends DialogFragment {
        private SettingAutoFragment outerFragment;
        private CharSequence[] types;

        EngineTypeDialogFragment(SettingAutoFragment outerFragment) {
            this.outerFragment = outerFragment;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Engine Type")
                    .setItems(types, (dialog, which) -> {
                        outerFragment.engineType.setValue(types[which].toString());
                        outerFragment.engineType.setSummary(types[which]);
                    });

            return builder.create();
        }

        void setEngineType(CharSequence[] types) {
            this.types = types;
        }
    }

}
