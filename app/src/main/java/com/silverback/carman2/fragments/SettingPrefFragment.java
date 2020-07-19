package com.silverback.carman2.fragments;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentSnapshot;
import com.silverback.carman2.BaseActivity;
import com.silverback.carman2.R;
import com.silverback.carman2.database.CarmanDatabase;
import com.silverback.carman2.database.FavoriteProviderDao;
import com.silverback.carman2.utils.Constants;
import com.silverback.carman2.viewmodels.FragmentSharedModel;
import com.silverback.carman2.viewmodels.ImageViewModel;
import com.silverback.carman2.views.NameDialogPreference;
import com.silverback.carman2.views.ProgressBarPreference;
import com.silverback.carman2.views.SpinnerDialogPreference;

import org.json.JSONArray;
import org.json.JSONException;

/*
 * This fragment subclasses PreferernceFragmentCompat, which is a special fragment to display a
 * hierarchy of Preference objects, automatically persisting values in SharedPreferences.
 * Some preferences have custom dialog frragments which should implement the callbacks defined in
 * PreferenceManager.OnDisplayDialogPreferenceListener to pop up the dialog fragment, passing params
 * to the singleton constructor.
 */
public class SettingPrefFragment extends SettingBaseFragment  {

    // Logging
    //private static final LoggingHelper log = LoggingHelperFactory.create(SettingPrefFragment.class);

    // Objects
    private SharedPreferences mSettings;
    private Preference userImagePref;
    private String nickname;
    //private DecimalFormat df;

    // UIs
    private ProgressBarPreference autoPref;
    private SpinnerDialogPreference spinnerPref;
    private Preference favorite;

    // Fields
    private JSONArray jsonDistrict;
    private String sigunCode;
    private String regMakerNum;

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        // Set Preference hierarchy defined as XML and placed in res/xml directory.
        setPreferencesFromResource(R.xml.preferences, rootKey);
        String jsonString = getArguments().getString("district");
        try {jsonDistrict = new JSONArray(jsonString);}
        catch(JSONException e) {e.printStackTrace();}

        CarmanDatabase mDB = CarmanDatabase.getDatabaseInstance(getContext());
        //mSettings = ((SettingPrefActivity)getActivity()).getSettings();
        mSettings = ((BaseActivity)getActivity()).getSharedPreferernces();

        //df = BaseActivity.getDecimalFormatInstance();

        // Custom preference which calls DialogFragment, not PreferenceDialogFragmentCompat,
        // in order to receive a user name which is verified to a new one by querying.
        NameDialogPreference namePref = findPreference(Constants.USER_NAME);
        String userName = mSettings.getString(Constants.USER_NAME, null);
        namePref.setSummary(userName);
        if(TextUtils.isEmpty(namePref.getSummary())) namePref.setSummary(getString(R.string.pref_entry_void));
        if(userName != null) nickname = namePref.getSummary().toString();

        // Call SettingAutoFragment which contains the preferences to have the auto data which will
        // be used as filter for querying the posting board. On clicking the Up button, the preference
        // values are notified here as the JSONString and reset the preference summary.
        autoPref = findPreference(Constants.AUTO_DATA);
        //autoPref.showProgressBar(true);
        makerName = mSettings.getString(Constants.AUTO_MAKER, null);
        modelName = mSettings.getString(Constants.AUTO_MODEL, null);
        //typeName = mSettings.getString(Constants.AUTO_TYPE, null);
        //String yearName = mSettings.getString(Constants.AUTO_YEAR, null);
        //String jsonData = mSettings.getString(Constants.AUTO_DATA, null);
        //if(!TextUtils.isEmpty(jsonData)) parseAutoData(jsonData);
        // set the void summary to the auto preference unless the auto maker name is given. Otherwise,
        // query the registration number of the auto maker and model with the make name, notifying
        // the listener
        // Invalidate the summary of the autodata preference as far as any preference value of
        // SettingAutoFragment have been changed.
        if(TextUtils.isEmpty(makerName)) autoPref.setSummary(getString(R.string.pref_entry_void));
        else queryAutoMaker(makerName);

        // Preference for selecting a fuel out of gas, diesel, lpg and premium, which should be
        // improved with more energy source such as eletricity and hydrogene provided.
        ListPreference fuelList = findPreference(Constants.FUEL);
        if(fuelList != null) fuelList.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
        // Custom SummaryProvider overriding provideSummary() with Lambda expression.
        // Otherwise, just set app:useSimpleSummaryProvider="true" in xml for EditTextPreference
        // and ListPreference.
        EditTextPreference etMileage = findPreference(Constants.ODOMETER);
        if(etMileage != null) {
            etMileage.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER));
            etMileage.setSummaryProvider(pref -> {
                String summary = ((EditTextPreference)pref).getText();
                return String.format("%s%3s", summary, "km");
            });
        }

        // Average miles per year
        EditTextPreference etAvg = findPreference(Constants.AVERAGE);
        if(etAvg != null) {
            etAvg.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER));
            etAvg.setSummaryProvider(preference -> {
                //String summary = df.format(Integer.parseInt(((EditTextPreference)preference).getText()));
                String summary = ((EditTextPreference)preference).getText();
                return String.format("%s%3s", summary, "km");
            });
        }

        // Custom preference to display the custom PreferenceDialogFragmentCompat which has dual
        // spinners to pick the district of Sido and Sigun based upon the given Sido. The default
        // district name and code is saved as JSONString.
        spinnerPref = findPreference(Constants.DISTRICT);
        if(jsonDistrict != null) {
            spinnerPref.setSummaryProvider(preference ->
                    String.format("%s %s", jsonDistrict.optString(0), jsonDistrict.optString(1)));
            sigunCode = jsonDistrict.optString(2);
        }

        // Set the searching radius within which near stations should be located.
        ListPreference searchingRadius = findPreference(Constants.SEARCHING_RADIUS);
        if(searchingRadius != null) {
            searchingRadius.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
        }

        // Retrieve the favorite gas station and the service station which are both set the placeholder
        // to 0 as the designated provider.
        favorite = findPreference(Constants.FAVORITE);
        mDB.favoriteModel().queryFirstSetFavorite().observe(this, data -> {
            String favoriteStn = getString(R.string.pref_no_favorite);
            String favoriteSvc = getString(R.string.pref_no_favorite);
            for(FavoriteProviderDao.FirstSetFavorite provider : data) {
                if(provider.category == Constants.GAS) favoriteStn = provider.favoriteName;
                else if(provider.category == Constants.SVC) favoriteSvc = provider.favoriteName;
            }

            String summary = String.format("%-8s%s\n%-8s%s",
                    getString(R.string.pref_label_station), favoriteStn,
                    getString(R.string.pref_label_service), favoriteSvc);
            favorite.setSummaryProvider(preference -> summary);
        });

        Preference gasStation = findPreference(Constants.FAVORITE_GAS);
        gasStation.setSummaryProvider(preference -> getString(R.string.pref_summary_gas));
        Preference svcCenter = findPreference(Constants.FAVORITE_SVC);
        svcCenter.setSummaryProvider(preference -> getString(R.string.pref_summary_svc));

        // Set the standard of period between month and mileage.
        ListPreference svcPeriod = findPreference("pref_service_period");
        if(svcPeriod != null) {
            svcPeriod.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
        }

        // Preference for whether the geofence notification is permiited to receive or not.
        SwitchPreferenceCompat switchGeofencePref = findPreference(Constants.NOTIFICATION_GEOFENCE);
        switchGeofencePref.setChecked(true);

        // Image Editor which pops up the dialog to select which resource location to find an image.
        // Consider to replace this with the custom preference defined as ProgressImagePreference.
        //ProgressImagePreference progImgPref = findPreference(Constants.USER_IMAGE);
        userImagePref = findPreference(Constants.USER_IMAGE);
        userImagePref.setOnPreferenceClickListener(view -> {
            // Carmera permission check.
            if(TextUtils.isEmpty(mSettings.getString(Constants.USER_NAME, null))) {
                Snackbar.make(getView(), R.string.pref_snackbar_edit_image, Snackbar.LENGTH_SHORT).show();
                return false;
            }

            DialogFragment dialogFragment = new CropImageDialogFragment();
            dialogFragment.show(getActivity().getSupportFragmentManager(), null);
            return true;
        });

        // LiveData from ApplyImageREsourceUtil to set a drawable to the icon. The viewmodel is
        // defined in onViewCreated().
        ImageViewModel imgModel = new ViewModelProvider(requireActivity()).get(ImageViewModel.class);
        imgModel.getGlideDrawableTarget().observe(getActivity(), res -> userImagePref.setIcon(res));
    }

    /*
     * Called immediately after onCreateView() has returned, but before any saved state has been
     * restored in to the view. This should be appropriate to have ViewModel be created and observed
     * for sharing data b/w fragments by getting getViewLifecycleOwner().
     */
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        FragmentSharedModel fragmentModel = new ViewModelProvider(requireActivity()).get(FragmentSharedModel.class);

        // Observe whether the auto data in SettingAutoFragment has changed.
        fragmentModel.getAutoData().observe(getViewLifecycleOwner(), jsonString -> {
            makerName = parseAutoData(jsonString).get(0);
            modelName = parseAutoData(jsonString).get(1);
            mSettings.edit().putString(Constants.AUTO_DATA, jsonString).apply();

            if(!TextUtils.isEmpty(makerName)) {
                queryAutoMaker(makerName);
                autoPref.showProgressBar(true);
            }
        });

        // Observe whether the district has changed in the custom spinner list view. If any change
        // has been made, set summary and save it as JSONArray string in SharedPreferences.
        fragmentModel.getDefaultDistrict().observe(getViewLifecycleOwner(), distList -> {
            sigunCode = distList.get(2);
            spinnerPref.setSummaryProvider(preference -> String.format("%s %s", distList.get(0), distList.get(1)));
            JSONArray jsonArray = new JSONArray(distList);
            mSettings.edit().putString(Constants.DISTRICT, jsonArray.toString()).apply();
        });
    }

    // queryAutoMaker() defined in the parent fragment(SettingBaseFragment) queries the auto maker,
    // the result of which implements this to get the registration number of the auto
    // maker and continues to call queryAutoModel() if an auto model exists. Otherwise, ends with
    // setting the summary.
    @Override
    public void queryAutoMakerSnapshot(DocumentSnapshot makershot) {
        // Upon completion of querying the auto maker, sequentially re-query the auto model
        // with the auto make id from the snapshot.
        regMakerNum = String.valueOf(makershot.getLong("reg_number"));
        //String automaker = makershot.getString("auto_maker");
        //log.i("emblem: %s", makershot.getString("auto_emblem"));

        if(!TextUtils.isEmpty(modelName)) {
            queryAutoModel(makershot.getId(), modelName);
        } else {
            String summary = String.format("%s (%s)", makerName, regMakerNum);
            setSpannedAutoSummary(autoPref, summary);

            // Hide the progressbar in the preference
            autoPref.showProgressBar(false);
        }
    }
    // queryAutoModel() defined in the parent fragment(SettingBaseFragment) queries the auto model,
    // the result of which implement the method to have the registration number of the auto model,
    // then set the summary.
    @Override
    public void queryAutoModelSnapshot(DocumentSnapshot modelshot) {
        // The auto preference summary depends on whether the model name is set because
        // queryAutoModel() would notify null to the listener w/o the model name.
        if(modelshot != null && modelshot.exists()) {
            String num = String.valueOf(modelshot.getLong("reg_number"));
            String summary = String.format("%s(%s)  %s(%s)", makerName, regMakerNum, modelName, num);
            setSpannedAutoSummary(autoPref, summary);

            // Hide the progressbar in the autodata preference when the auto data are succeessfully
            // retrieved.
            autoPref.showProgressBar(false);
        }
    }


    // Implement the callback of Preferrence.OnDisplayPreferenceDialogListener, which defines an
    // action to pop up an CUSTOM PreferenceDialogFragmnetCompat when a preferenece clicks.
    // getFragmentManager() is deprecated as of API 28 and up. Instead, use FragmentActivity.
    @SuppressWarnings("ConstantConditions")
    @Override
    public void onDisplayPreferenceDialog(Preference pref) {
        if (pref instanceof SpinnerDialogPreference) {
            DialogFragment spinnerFragment = SettingSpinnerDlgFragment.newInstance(pref.getKey(), sigunCode);
            spinnerFragment.setTargetFragment(this, 0);
            spinnerFragment.show(getActivity().getSupportFragmentManager(), null);

        } else if(pref instanceof NameDialogPreference) {
            DialogFragment nameFragment = SettingNameDlgFragment.newInstance(pref.getKey(), nickname);
            nameFragment.setTargetFragment(this, 1);
            nameFragment.show(getActivity().getSupportFragmentManager(), null);

        } else {
            super.onDisplayPreferenceDialog(pref);
        }

    }

    // Referenced by OnSelectImageMedia callback when selecting the deletion in order to remove
    // the profile image icon
    public Preference getUserImagePreference() {
        return userImagePref;
    }
}