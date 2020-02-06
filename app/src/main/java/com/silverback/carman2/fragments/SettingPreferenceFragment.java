package com.silverback.carman2.fragments;


import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;

import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.google.android.material.snackbar.Snackbar;
import com.silverback.carman2.BaseActivity;
import com.silverback.carman2.R;
import com.silverback.carman2.SettingPreferenceActivity;
import com.silverback.carman2.database.CarmanDatabase;
import com.silverback.carman2.database.FavoriteProviderDao;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.FragmentSharedModel;
import com.silverback.carman2.utils.Constants;
import com.silverback.carman2.utils.EditImageHelper;
import com.silverback.carman2.views.NameDialogPreference;
import com.silverback.carman2.views.SpinnerDialogPreference;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;

/*
 * This fragment subclasses PreferernceFragmentCompat, which is a special fragment to display a
 * hierarchy of Preference objects, automatically persisting values in SharedPreferences.
 * Some preferences have custom dialog frragments which should implement the callbacks defined in
 * PreferenceManager.OnDisplayDialogPreferenceListener to pop up the dialog fragment, passing
 * params to the singleton constructor.
 */
public class SettingPreferenceFragment extends PreferenceFragmentCompat {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(SettingPreferenceFragment.class);

    // Objects
    private SharedPreferences mSettings;
    private Preference cropImagePreference;
    private String nickname;

    // Fields
    private String sigunCode;

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        // Set Preference hierarchy defined as XML and placed in res/xml directory.
        setPreferencesFromResource(R.xml.preferences, rootKey);

        CarmanDatabase mDB = CarmanDatabase.getDatabaseInstance(getContext().getApplicationContext());
        mSettings = ((SettingPreferenceActivity)getActivity()).getSettings();
        FragmentSharedModel sharedModel = new ViewModelProvider(getActivity()).get(FragmentSharedModel.class);

        // Custom preference which calls DialogFragment, not PreferenceDialogFragmentCompat,
        // in order to receive a user name which is verified to a new one by querying.
        NameDialogPreference namePref = findPreference(Constants.USER_NAME);
        String userName = mSettings.getString(Constants.USER_NAME, null);
        namePref.setSummary(userName);
        if(TextUtils.isEmpty(namePref.getSummary())) namePref.setSummary(getString(R.string.setting_null));
        if(userName != null) nickname = namePref.getSummary().toString();


        // Call SettingAutoFragment which has preferences of its own. On clicking the Up button in
        // the fragment, the preference values are notified here as the JSONString and reset the
        // preference summary.
        Preference autoPref = findPreference(Constants.VEHICLE);
        String autoMaker = mSettings.getString("pref_auto_maker", null);
        String autoModel = mSettings.getString("pref_auto_model", null);
        String autoYear = mSettings.getString("pref_auto_year", null);
        String autoType = mSettings.getString("pref_auto_type", null);
        autoPref.setSummary(String.format("%s, %s, %s, %s", autoMaker, autoType, autoModel, autoYear));

        sharedModel.getJsonAutoData().observe(getActivity(), data -> {
            try {
                JSONArray json = new JSONArray(data);
                autoPref.setSummary(String.format("%s, %s, %s, %s",
                        json.optString(0), json.optString(1), json.optString(2), json.optString(3)));
            } catch(JSONException e) {
                log.e("JSONException: %s", e.getMessage());
            }
        });

        // Preference for selecting a fuel out of gas, diesel, lpg and premium, which should be
        // improved with more energy source such as eletricity and hydrogene provided.
        ListPreference fuelList = findPreference(Constants.FUEL);
        if(fuelList != null) fuelList.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());

        // Custom SummaryProvider overriding provideSummary() with Lambda expression.
        // Otherwise, just set app:useSimpleSummaryProvider="true" in xml for EditTextPreference
        // and ListPreference.
        EditTextPreference etMileage = findPreference(Constants.ODOMETER);
        // Custom SummaryProvider overriding provideSummary() with Lambda expression.
        if(etMileage != null) {
            etMileage.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER));
            etMileage.setSummaryProvider(preference -> String.format("%s%3s", etMileage.getText(), "km"));
        }

        EditTextPreference etAvg = findPreference(Constants.AVERAGE);
        //etAvg.setSummaryProvider(EditTextPreference.SimpleSummaryProvider.getInstance());
        if(etAvg != null) {
            etAvg.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER));
            etAvg.setSummaryProvider(preference -> String.format("%s%3s", etAvg.getText(), "km"));
        }

        ListPreference searchingRadius = findPreference(Constants.SEARCHING_RADIUS);
        if(searchingRadius != null) {
            searchingRadius.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
        }


        // Retrieve the favorite gas station and the service station which are both set the placeholder
        // to 0 as the designated provider.
        Preference favorite = findPreference("pref_favorite_provider");
        mDB.favoriteModel().queryFirstSetFavorite().observe(this, data -> {
            String favoriteStn = getString(R.string.pref_no_favorite);
            String favoriteSvc = getString(R.string.pref_no_favorite);
            for(FavoriteProviderDao.FirstSetFavorite provider : data) {
                if(provider.category == Constants.GAS) favoriteStn = provider.favoriteName;
                else if(provider.category == Constants.SVC) favoriteSvc = provider.favoriteName;
            }

            favorite.setSummary(String.format("%-8s%s\n%-8s%s",
                    getString(R.string.pref_label_station), favoriteStn,
                    getString(R.string.pref_label_service), favoriteSvc));
        });

        Preference gasStation = findPreference("pref_favorite_gas");
        gasStation.setSummary(R.string.pref_summary_gas);
        Preference svcCenter = findPreference("pref_favorite_svc");
        svcCenter.setSummary(R.string.pref_summary_svc);

        // Set the standard of period between month and mileage.
        ListPreference svcPeriod = findPreference("pref_service_period");
        if(svcPeriod != null) {
            svcPeriod.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
        }

        // Custom preference to display the custom PreferenceDialogFragmentCompat which has dual
        // spinners to pick the district of Sido and Sigun based upon the given Sido. The default
        // district name and code is saved as JSONString.
        SpinnerDialogPreference spinnerPref = findPreference(Constants.DISTRICT);
        JSONArray json = BaseActivity.getDistrictJSONArray();
        if(json != null) {
            spinnerPref.setSummary(String.format("%s %s", json.optString(0), json.optString(1)));
            sigunCode = json.optString(2);
        }
        // When the default district changes in the spinners, a newly selected district will be
        // notified via ViewModel
        // and after coverting the values to JSONString, save it in SharedPreferences.
        sharedModel.getDefaultDistrict().observe(this, distList -> {
            sigunCode = distList.get(2);
            spinnerPref.setSummary(String.format("%s %s", distList.get(0), distList.get(1)));
            JSONArray jsonArray = new JSONArray(distList);
            mSettings.edit().putString(Constants.DISTRICT, jsonArray.toString()).apply();
        });

        // Preference for whether any notification is permiited to receive or not.
        SwitchPreferenceCompat switchPref = findPreference(Constants.LOCATION_UPDATE);

        // Image Editor which pops up the dialog to select which resource location to find an image.
        cropImagePreference = findPreference(Constants.IMAGES);
        cropImagePreference.setOnPreferenceClickListener(view -> {
            if(TextUtils.isEmpty(mSettings.getString(Constants.USER_NAME, null))) {
                Snackbar.make(getView(), R.string.pref_snackbar_edit_image, Snackbar.LENGTH_SHORT).show();
                return false;
            }

            DialogFragment dialogFragment = new CropImageDialogFragment();
            dialogFragment.show(getActivity().getSupportFragmentManager(), null);
            return true;
        });

        // Set the circle image to the icon by getting the image Uri which has been saved at
        // SharedPreferences defined in SettingPreverenceActivity.
        /*
        String imageUri = mSettings.getString(Constants.FILE_IMAGES, null);
        if(!TextUtils.isEmpty(imageUri)) {
            try {
                EditImageHelper cropHelper = new EditImageHelper(getContext());
                RoundedBitmapDrawable drawable = cropHelper.drawRoundedBitmap(Uri.parse(imageUri));
                cropImagePreference.setIcon(drawable);
            } catch (IOException e) {
                log.e("IOException: %s", e.getMessage());
            }
        }
         */
    }

    // Preferrence.OnDisplayPreferenceDialogListener is implemented by the following callback which
    // defines an action to pop up an custom DialogFragment when a preferenece clicks.
    // getFragmentManager() is deprecated as of API 28 and up. Instead, use FragmentActivity.
    // getSupportFragmentManager()
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
    // the profile image icon.
    public Preference getCropImagePreference() {
        return cropImagePreference;
    }

}