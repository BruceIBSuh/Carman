package com.silverback.carman2.fragments;


import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;

import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProviders;
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
import java.util.Arrays;

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
    private FragmentSharedModel fragmentSharedModel;

    // Fields
    private String sigunCode;

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        // Set Preference hierarchy defined as XML and placed in res/xml directory.
        setPreferencesFromResource(R.xml.preferences, rootKey);

        // Indicates that the fragment may initialize the contents of the Activity's standard options menu.
        //setHasOptionsMenu(true);

        CarmanDatabase mDB = CarmanDatabase.getDatabaseInstance(getContext().getApplicationContext());
        //df = BaseActivity.getDecimalFormatInstance();
        mSettings = ((SettingPreferenceActivity)getActivity()).getSettings();
        fragmentSharedModel = ViewModelProviders.of(getActivity()).get(FragmentSharedModel.class);

        // Custom preference which calls DialogFragment, not PreferenceDialogFragmentCompat,
        // in order to receive a user name which is verified to a new one by querying.
        NameDialogPreference namePref = findPreference(Constants.USER_NAME);
        namePref.setSummary(mSettings.getString(Constants.USER_NAME, null));
        if(TextUtils.isEmpty(namePref.getSummary())) namePref.setSummary(getString(R.string.setting_null));
        if(mSettings.getString(Constants.USER_NAME, null) != null)
            nickname = namePref.getSummary().toString();

        Preference autoPref = findPreference(Constants.VEHICLE);
        String autoMaker = mSettings.getString("pref_auto_maker", null);
        String autoModel = mSettings.getString("pref_auto_model", null);
        String autoYear = mSettings.getString("pref_auto_year", null);
        String autoType = mSettings.getString("pref_auto_type", null);

        String[] autoProfile = new String[] {autoMaker, autoType, autoModel, autoYear};
        String jsonAutoData = new JSONArray(Arrays.asList(autoProfile)).toString();
        // ShredPreferences doesn't supprot the string array.
        mSettings.edit().putString(Constants.VEHICLE, jsonAutoData).apply();
        autoPref.setSummary(String.format("%s, %s, %s, %s", autoMaker, autoType, autoModel, autoYear));

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

        ListPreference searchingRadius = findPreference(Constants.RADIUS);
        if(searchingRadius != null) {
            searchingRadius.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
        }


        // Retrieve the favorite gas station and the service station which are both set the placeholder
        // to 0 as the designated provider.
        Preference favorite = findPreference("pref_favorite_provider");
        mDB.favoriteModel().queryFirstSetFavorite().observe(this, data -> {
            String station = getString(R.string.pref_no_favorite);
            String service = getString(R.string.pref_no_favorite);
            for(FavoriteProviderDao.FirstSetFavorite provider : data) {
                if(provider.category == Constants.GAS) station = provider.favoriteName;
                else if(provider.category == Constants.SVC) service = provider.favoriteName;
            }
            favorite.setSummary(String.format("%s / %s", station, service));
        });

        Preference gasStation = findPreference("pref_favorite_gas");
        gasStation.setSummary(R.string.pref_summary_gas);
        Preference svcCenter = findPreference("pref_favorite_svc");
        svcCenter.setSummary(R.string.pref_summary_svc);

        ListPreference svcPeriod = findPreference("pref_service_period");
        if(svcPeriod != null) {
            svcPeriod.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
        }


        //JSONArray jsonDistrict = getDistrictJSONArray();
        SpinnerDialogPreference spinnerPref = findPreference(Constants.DISTRICT);
        JSONArray json = BaseActivity.getDistrictNameCode();
        if(json != null) {
            spinnerPref.setSummary(String.format("%s %s", json.optString(0), json.optString(1)));
            sigunCode = json.optString(2);
        }
        // When the district changes, get the values via FragmentSharedModel from SettingSpinnerDlgFragment
        // and after coverting the values to JSONString, save it in SharedPreferences.
        fragmentSharedModel.getDefaultDistNames().observe(this, name -> {
            sigunCode = name[2];
            spinnerPref.setSummary(String.format("%s %s", name[0], name[1]));
            JSONArray jsonArray = new JSONArray(Arrays.asList(name[0], name[1], name[2]));
            mSettings.edit().putString(Constants.DISTRICT, jsonArray.toString()).apply();
        });


        SwitchPreferenceCompat switchPref = findPreference(Constants.LOCATION_UPDATE);

        // Image Editor which pops up the dialog to select which resource location to find an image.
        cropImagePreference = findPreference("pref_edit_image");
        cropImagePreference.setOnPreferenceClickListener(view -> {
            if(TextUtils.isEmpty(mSettings.getString(Constants.USER_NAME, null))) {
                Snackbar.make(getView(), R.string.pref_snackbar_edit_image, Snackbar.LENGTH_SHORT).show();
                return false;
            }

            DialogFragment dialogFragment = new CropImageDialogFragment();
            dialogFragment.show(getFragmentManager(), null);
            return true;
        });

        // Set the circle image to the icon by getting the image Uri which has been saved at
        // SharedPreferences defined in SettingPreverenceActivity.
        String imageUri = mSettings.getString("croppedImageUri", null);
        if(!TextUtils.isEmpty(imageUri)) {
            try {
                EditImageHelper cropHelper = new EditImageHelper(getContext());
                RoundedBitmapDrawable drawable = cropHelper.drawRoundedBitmap(Uri.parse(imageUri));
                cropImagePreference.setIcon(drawable);
            } catch (IOException e) {
                log.e("IOException: %s", e.getMessage());
            }
        }
    }

    // Preferrence.OnDisplayPreferenceDialogListener is implemented by the following callback which
    // defines an action to pop up an custom DialogFragment when a preferenece clicks.
    @SuppressWarnings("ConstantConditions")
    @Override
    public void onDisplayPreferenceDialog(Preference pref) {
        if (pref instanceof SpinnerDialogPreference) {
            DialogFragment spinnerFragment = SettingSpinnerDlgFragment.newInstance(pref.getKey(), sigunCode);
            spinnerFragment.setTargetFragment(this, 0);
            spinnerFragment.show(getFragmentManager(), null);

        } else if(pref instanceof NameDialogPreference) {
            DialogFragment nameFragment = SettingNameDlgFragment.newInstance(pref.getKey(), nickname);
            nameFragment.setTargetFragment(this, 1);
            nameFragment.show(getFragmentManager(), null);

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