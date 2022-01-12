package com.silverback.carman.fragments;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentSnapshot;
import com.silverback.carman.BaseActivity;
import com.silverback.carman.R;
import com.silverback.carman.database.CarmanDatabase;
import com.silverback.carman.database.FavoriteProviderDao;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.Constants;
import com.silverback.carman.viewmodels.FragmentSharedModel;
import com.silverback.carman.viewmodels.ImageViewModel;
import com.silverback.carman.views.NameDialogPreference;
import com.silverback.carman.views.ProgressBarPreference;
import com.silverback.carman.views.SpinnerDialogPreference;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.Objects;

/*
 * This fragment subclasses PreferernceFragmentCompat, which is a special fragment to display a
 * hierarchy of preference objects, automatically persisting values in SharedPreferences.
 * Some preferences have a custom dialog fragment which should implement the callback defined in
 * PreferenceManager.OnDisplayDialogPreferenceListener to pop up the dialog fragment, passing params
 * to the singleton constructor.
 */
public class SettingPreferenceFragment extends SettingBaseFragment  {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(SettingPreferenceFragment.class);

    // Objects
    private SharedPreferences mSettings;
    private FragmentSharedModel fragmentModel;
    private Preference userImagePref;
    private String nickname;

    //private DecimalFormat df;

    // Custom preferences defined in views package
    private ProgressBarPreference autoPref; // custom preference to show the progressbar.
    private SpinnerDialogPreference spinnerPref;
    private Preference favorite;

    // Fields
    private JSONArray jsonDistrict;
    private String sigunCode;
    private String regMakerNum;

    //@SuppressWarnings("ConstantConditions")
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Set Preference hierarchy defined as XML and placed in res/xml directory.
        setPreferencesFromResource(R.xml.preferences, rootKey);
        String jsonString = requireArguments().getString("district");
        try {jsonDistrict = new JSONArray(jsonString);}
        catch(JSONException e) {e.printStackTrace();}

        CarmanDatabase mDB = CarmanDatabase.getDatabaseInstance(getContext());
        mSettings = ((BaseActivity) Objects.requireNonNull(requireActivity())).getSharedPreferernces();
        fragmentModel = new ViewModelProvider(requireActivity()).get(FragmentSharedModel.class);

        // Custom preference which calls for DialogFragment, not PreferenceDialogFragmentCompat,
        // in order to receive a user name which shouold be verified to a new one by querying.
        NameDialogPreference namePref = findPreference(Constants.USER_NAME);
        String userName = mSettings.getString(Constants.USER_NAME, null);
        Objects.requireNonNull(namePref).setSummary(userName);

        if(TextUtils.isEmpty(namePref.getSummary())) namePref.setSummary(getString(R.string.pref_entry_void));
        if(userName != null) nickname = namePref.getSummary().toString();

        // Call SettingAutoFragment which contains preferences to have car related data which are
        // used as filters for querying the posting board. On clicking the UP button, the preference
        // values are notified here as the JSONString and reset the preference summary.
        autoPref = findPreference(Constants.AUTO_DATA);
        makerName = mSettings.getString(Constants.AUTO_MAKER, null);
        modelName = mSettings.getString(Constants.AUTO_MODEL, null);

        // Set the void summary to the auto preference unless the auto maker name is given. Otherwise,
        // query the registration number of the automaker and the automodel, if the model name is given.
        // At the same time, show the progressbar until the number is queried.
        log.i("maker name: %s", makerName);
        if(TextUtils.isEmpty(makerName) || makerName.matches(getString(R.string.pref_entry_void))) {
            autoPref.setSummaryProvider(pref -> getString(R.string.pref_entry_void));
        } else {
            autoPref.showProgressBar(true);
            queryAutoMaker(makerName);
        }

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
        log.i("notification switch: %s", mSettings.getBoolean(Constants.NOTIFICATION_GEOFENCE, true));

        // Image Editor which pops up the dialog to select which resource location to find an image.
        // Consider to replace this with the custom preference defined as ProgressImagePreference.
        //ProgressImagePreference progImgPref = findPreference(Constants.USER_IMAGE);
        userImagePref = findPreference(Constants.USER_IMAGE);
        Objects.requireNonNull(userImagePref).setOnPreferenceClickListener(view -> {
            // Carmera permission check.
            if(TextUtils.isEmpty(mSettings.getString(Constants.USER_NAME, null))) {
                Snackbar.make(getView(), R.string.pref_snackbar_edit_image, Snackbar.LENGTH_SHORT).show();
                return false;
            }

            DialogFragment dialogFragment = new CropImageDialogFragment();
            dialogFragment.show(requireActivity().getSupportFragmentManager(), null);
            return true;
        });

        // LiveData from ApplyImageREsourceUtil to set a drawable to the icon. The viewmodel is
        // defined in onViewCreated().
        ImageViewModel imgModel = new ViewModelProvider(requireActivity()).get(ImageViewModel.class);
        imgModel.getGlideDrawableTarget().observe(requireActivity(), res -> userImagePref.setIcon(res));
    }

    /*
     * Called immediately after onCreateView() has returned, but before any saved state has been
     * restored in to the view. This should be appropriate to have ViewModel be created and observed
     * for sharing data b/w fragments by getting getViewLifecycleOwner().
     */
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Observe whether the auto data in SettingAutoFragment has changed.
        fragmentModel.getAutoData().observe(getViewLifecycleOwner(), jsonString -> {
            mSettings.edit().putString(Constants.AUTO_DATA, jsonString).apply();
            makerName = parseAutoData(jsonString).get(0);
            modelName = parseAutoData(jsonString).get(1);

            // The null value that JSONObject returns seems different than that of other regular
            // object. Thus, JSONObject.isNull(int) should be checked, then set the null value to it
            // if it is true. This is firmly at bug issue.
            if(!TextUtils.isEmpty(makerName)) {
                log.i("view created:%s", makerName);
                autoPref.setSummaryProvider(preference -> "Loading...");
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
        // Upon completion of querying the auto maker, seqduentially re-query the auto model
        // with the auto make id from the snapshot.
        regMakerNum = String.valueOf(makershot.getLong("reg_maker"));
        String summary = String.format("%s (%s)", makerName, regMakerNum);
        setSpannedAutoSummary(autoPref, summary);
        // Hide the progressbar in the preference
        autoPref.showProgressBar(false);

        // If the automodel exisits, keep querying the model registration number. If not, hide the
        // progressbar when the automaker queries the number.
        if(!TextUtils.isEmpty(modelName)) queryAutoModel(makershot.getId(), modelName);
        else autoPref.showProgressBar(false);

    }
    // queryAutoModel() defined in the parent fragment(SettingBaseFragment) queries the auto model,
    // the result of which implement the method to have the registration number of the auto model,
    // then set the summary.
    @Override
    public void queryAutoModelSnapshot(DocumentSnapshot modelshot) {
        // The auto preference summary depends on whether the model name is set because
        // queryAutoModel() would notify null to the listener w/o the model name.
        if(modelshot != null && modelshot.exists()) {
            String num = String.valueOf(modelshot.getLong("reg_model"));
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
    //@SuppressWarnings("ConstantConditions")
    @Override
    public void onDisplayPreferenceDialog(Preference pref) {
        if (pref instanceof SpinnerDialogPreference) {
            DialogFragment spinnerFragment = SettingSpinnerDlgFragment.newInstance(pref.getKey(), sigunCode);
            spinnerFragment.setTargetFragment(this, 0);
            spinnerFragment.show(requireActivity().getSupportFragmentManager(), null);

        } else if(pref instanceof NameDialogPreference) {
            DialogFragment nameFragment = SettingNameDlgFragment.newInstance(pref.getKey(), nickname);
            nameFragment.setTargetFragment(this, 1);
            nameFragment.show(requireActivity().getSupportFragmentManager(), null);

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