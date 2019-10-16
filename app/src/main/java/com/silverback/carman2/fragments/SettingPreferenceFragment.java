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

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.silverback.carman2.R;
import com.silverback.carman2.SettingPreferenceActivity;
import com.silverback.carman2.database.CarmanDatabase;
import com.silverback.carman2.database.FavoriteProviderDao;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.FragmentSharedModel;
import com.silverback.carman2.threads.LoadDistCodeTask;
import com.silverback.carman2.utils.Constants;
import com.silverback.carman2.utils.CropImageHelper;
import com.silverback.carman2.views.SpinnerDialogPreference;

import java.io.IOException;

public class SettingPreferenceFragment extends PreferenceFragmentCompat {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(SettingPreferenceFragment.class);

    // Objects
    private CarmanDatabase mDB;
    private SharedPreferences mSettings;
    private FragmentSharedModel sharedModel;
    private LoadDistCodeTask mTask;

    private Preference cropImagePreference;

    private String sigunCode;

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        // Set Preference hierarchy defined as XML and placed in res/xml directory.
        setPreferencesFromResource(R.xml.preferences, rootKey);

        // Indicates that the fragment may initialize the contents of the Activity's standard options menu.
        setHasOptionsMenu(true);

        mDB = CarmanDatabase.getDatabaseInstance(getContext().getApplicationContext());
        //df = BaseActivity.getDecimalFormatInstance();
        mSettings = ((SettingPreferenceActivity)getActivity()).getSettings();

        // Retrvie the district info saved in SharedPreferences from the parent activity as a type
        // of JSONArray
        String[] district = getArguments().getStringArray("district");
        sigunCode = district[2];

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

            log.i("favorite change: %s", station);
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

        SpinnerDialogPreference spinnerPref = findPreference(Constants.DISTRICT);
        spinnerPref.setSummary(String.format("%s %s", district[0], district[1]));

        SwitchPreferenceCompat switchPref = findPreference(Constants.LOCATION_UPDATE);

        // Image Editor which pops up the dialog to select which resource location to find an image.
        cropImagePreference = findPreference("pref_edit_image");
        cropImagePreference.setOnPreferenceClickListener(view -> {
            log.i("Edit Image Preference clicked");
            DialogFragment dialogFragment = new CropImageDialogFragment();
            dialogFragment.show(getFragmentManager(), null);

            return true;
        });

        // Set the circle image for the icon by getting the image Uri which has been saved at
        // SharedPreferences defined in SettingPreverenceActivity.
        String imageUri = mSettings.getString("croppedImageUri", null);

        if(!TextUtils.isEmpty(imageUri)) {
            try {
                CropImageHelper cropHelper = new CropImageHelper(getContext());
                RoundedBitmapDrawable drawable = cropHelper.drawRoundedBitmap(Uri.parse(imageUri));
                cropImagePreference.setIcon(drawable);
            } catch (IOException e) {
                log.e("IOException: %s", e.getMessage());
            }
        }


    }

    @Override
    public void onPause() {
        super.onPause();
        if (mTask != null) mTask = null;
    }


    // Callback from PreferenceManager.OnDisplayPreferenceDialogListener when a Preference
    // requests to display a dialog.
    @SuppressWarnings("ConstantConditions")
    @Override
    public void onDisplayPreferenceDialog(Preference pref) {

        log.i("onDisplayPreferenceDialog");
        
        if (pref instanceof SpinnerDialogPreference) {
            DialogFragment dlgFragment = SettingSpinnerDlgFragment.newInstance(pref.getKey(), sigunCode);
            dlgFragment.setTargetFragment(this, 0);
            dlgFragment.show(getFragmentManager(), null);

        } else {

            super.onDisplayPreferenceDialog(pref);
        }

    }

    public Preference getCropImagePreference() {
        return cropImagePreference;
    }

    /*
    private void cropProfileImage(Uri uri) {
        Intent intent = new Intent(this, CropImageActivity.class);
        intent.setData(uri);
        //intent.putExtra("orientation", orientation);
        startActivityForResult(intent, REQUEST_CODE_CROP);
    }
    */
}