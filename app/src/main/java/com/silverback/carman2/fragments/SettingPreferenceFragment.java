package com.silverback.carman2.fragments;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;

import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.silverback.carman2.BaseActivity;
import com.silverback.carman2.R;
import com.silverback.carman2.SettingPreferenceActivity;
import com.silverback.carman2.database.CarmanDatabase;
import com.silverback.carman2.database.FavoriteProviderDao;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.viewmodels.FragmentSharedModel;
import com.silverback.carman2.utils.Constants;
import com.silverback.carman2.views.NameDialogPreference;
import com.silverback.carman2.views.SpinnerDialogPreference;

import org.json.JSONArray;
import org.json.JSONException;

/*
 * This fragment subclasses PreferernceFragmentCompat, which is a special fragment to display a
 * hierarchy of Preference objects, automatically persisting values in SharedPreferences.
 * Some preferences have custom dialog frragments which should implement the callbacks defined in
 * PreferenceManager.OnDisplayDialogPreferenceListener to pop up the dialog fragment, passing
 * params to the singleton constructor.
 */
public class SettingPreferenceFragment extends SettingBaseFragment {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(SettingPreferenceFragment.class);

    // Objects
    private FirebaseFirestore firestore;
    private SharedPreferences mSettings;
    private Preference userImagePref;
    private String nickname;

    // UIs
    private Preference autoPref;

    // Fields
    private String sigunCode;
    private String makerName, modelName, typeName, year;

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        // Set Preference hierarchy defined as XML and placed in res/xml directory.
        setPreferencesFromResource(R.xml.preferences, rootKey);

        firestore = FirebaseFirestore.getInstance();
        CarmanDatabase mDB = CarmanDatabase.getDatabaseInstance(getContext());
        mSettings = ((SettingPreferenceActivity)getActivity()).getSettings();
        FragmentSharedModel sharedModel = new ViewModelProvider(getActivity()).get(FragmentSharedModel.class);
        //ImageViewModel imgModel = new ViewModelProvider(getActivity()).get(ImageViewModel.class);

        // Custom preference which calls DialogFragment, not PreferenceDialogFragmentCompat,
        // in order to receive a user name which is verified to a new one by querying.
        NameDialogPreference namePref = findPreference(Constants.USER_NAME);
        String userName = mSettings.getString(Constants.USER_NAME, null);
        namePref.setSummary(userName);
        if(TextUtils.isEmpty(namePref.getSummary())) namePref.setSummary(getString(R.string.setting_null));
        if(userName != null) nickname = namePref.getSummary().toString();

        // Call SettingAutoFragment which contains the preferences to have the auto data which will
        // be used as filter for querying the board. On clicking the Up button, the preference values
        // are notified here as the JSONString and reset the preference summary.
        autoPref = findPreference(Constants.AUTO_DATA);
        String aVoid = getString(R.string.pref_entry_void);
        makerName = mSettings.getString(Constants.AUTO_MAKER, aVoid);
        modelName = mSettings.getString(Constants.AUTO_MODEL, aVoid);
        typeName = mSettings.getString(Constants.AUTO_TYPE, aVoid);
        year = mSettings.getString(Constants.AUTO_YEAR, aVoid);

        //queryAutoRegistrationNums(makerName, modelName);
        log.i("auto maker: %s", makerName);
        queryAutoRegistrationNums(makerName);
        setFirestoreCompleteListener((makerNum, modelNum) -> {
            log.i("Numbers: %s, %s", makerNum, modelNum);
            //String autoData = mSettings.getString(Constants.AUTO_DATA, aVoid);
            String summary = String.format("%s(%s), %s(%s), %s, %s",
                    makerName, makerNum, modelName, modelNum, typeName, year);

            setSpannableAutoDataSummary(autoPref, summary);
        });

        // Share the auto data which have ben seleted in SettingAutoFragment and put them to the
        // summary simultaneously.
        sharedModel.getJsonAutoData().observe(getActivity(), data -> {
            try {
                log.i("new auto data");
                JSONArray json = new JSONArray(data);
                makerName = json.optString(0);
                modelName = json.optString(2);
                typeName = json.optString(1);
                year = json.optString(3);
                log.i("new autodata: %s, %s", json.optString(0), json.optString(2));
                queryAutoRegistrationNums(makerName);
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
        // Consider to replace this with the custom preference defined as ProgressImagePreference.
        //ProgressImagePreference progImgPref = findPreference(Constants.USER_IMAGE);
        userImagePref = findPreference(Constants.USER_IMAGE);
        userImagePref.setOnPreferenceClickListener(view -> {
            log.i("ProgressImagePreference clicked");
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
                ApplyImageResourceUtil cropHelper = new ApplyImageResourceUtil(getContext());
                RoundedBitmapDrawable drawable = cropHelper.drawRoundedBitmap(Uri.parse(imageUri));
                userImagePreference.setIcon(drawable);
            } catch (IOException e) {
                log.e("IOException: %s", e.getMessage());
            }
        }
         */
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