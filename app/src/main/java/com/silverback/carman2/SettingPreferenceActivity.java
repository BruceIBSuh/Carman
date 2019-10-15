package com.silverback.carman2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.FrameLayout;

import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.material.snackbar.Snackbar;
import com.silverback.carman2.fragments.CropImageDialogFragment;
import com.silverback.carman2.fragments.SettingPreferenceFragment;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.FragmentSharedModel;
import com.silverback.carman2.models.OpinetPriceViewModel;
import com.silverback.carman2.threads.PriceRegionalTask;
import com.silverback.carman2.threads.ThreadManager;
import com.silverback.carman2.utils.Constants;
import com.silverback.carman2.utils.CropImageHelper;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;


public class SettingPreferenceActivity extends BaseActivity implements
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback,
        CropImageDialogFragment.OnSelectImageMediumListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(SettingPreferenceActivity.class);

    // Constants
    private static final int REQUEST_CODE_GALLERY = 10;
    private static final int REQUEST_CODE_CAMERA = 11;
    private static final int REQUEST_CODE_CROP = 12;

    // Objects
    private OpinetPriceViewModel priceModel;
    private FragmentSharedModel sharedModel;

    private MenuItem menuEdit, menuAdd;
    private PreferenceFragmentCompat caller;
    private SettingPreferenceFragment settingFragment;
    private PriceRegionalTask priceRegionalTask;
    private String distCode;
    private DecimalFormat df;

    // UIs
    private FrameLayout frameLayout;


    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_general_setting);

        Toolbar settingToolbar = findViewById(R.id.toolbar_setting);
        setSupportActionBar(settingToolbar);
        // Get a support ActionBar corresponding to this toolbar
        //ActionBar ab = getSupportActionBar();
        // Enable the Up button which enables it as an action button such that when the user presses
        // it, the parent activity receives a call to onOptionsItemSelected().
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        frameLayout = findViewById(R.id.frame_setting);

        priceModel = ViewModelProviders.of(this).get(OpinetPriceViewModel.class);
        sharedModel = ViewModelProviders.of(this).get(FragmentSharedModel.class);
        // DecimalFormat singleton instance from BaseActivity
        df = getDecimalFormatInstance();


        // Passes District Code(Sigun Code) and vehicle nickname to SettingPreferenceFragment for
        // setting the default spinner values in SpinnerDialogPrefernce and showing the summary
        // of the vehicle name respectively.
        List<String> district = convJSONArrayToList();
        if(district == null) distCode = "0101";
        else distCode = district.get(2);
        String vehicleName = mSettings.getString(Constants.VEHICLE_NAME, null);

        Bundle args = new Bundle();
        args.putStringArray("district", convJSONArrayToList().toArray(new String[3]));
        //args.putString("distCode", convJSONArrayToList().get(2));
        args.putString("name", vehicleName);
        //args.putString(Constants.ODOMETER, mileage);
        settingFragment = new SettingPreferenceFragment();
        settingFragment.setArguments(args);

        // Attach SettingPreferencFragment in the FrameLayout
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.frame_setting, settingFragment)
                .addToBackStack(null)
                .commit();
    }


    @Override
    public void onResume(){
        super.onResume();
        mSettings.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        mSettings.unregisterOnSharedPreferenceChangeListener(this);
        if(priceRegionalTask != null) priceRegionalTask = null;
    }




    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    /*
     * Invoked when a Preference with an associated Fragment is tabbed.
     * If you do not implement on PreferenceStartFragment(), a fallback implementation is used instead.
     * While this works i most cases, we strongly recommend implementing this method so you can fully
     * configure transitions b/w Fragment objects and update the title in the toolbar, if applicable.
     */
    @SuppressWarnings("ConstantConditions")
    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {

        log.i("Preference tabbed: %s", pref);
        final Bundle args = pref.getExtras();
        Fragment fragment = getSupportFragmentManager().getFragmentFactory()
                .instantiate(getClassLoader(), pref.getFragment());

        fragment.setArguments(args);
        fragment.setTargetFragment(caller, 0);
        getSupportActionBar().setTitle(pref.getTitle());
        getSupportActionBar().setDisplayShowHomeEnabled(false);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.frame_setting, fragment)
                .addToBackStack(null)
                .commit();

        return true;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        switch(key) {
            case Constants.VEHICLE_NAME:
                EditTextPreference pref = settingFragment.findPreference(key);
                log.i("EditTextPref: %s", pref.getText());
                if(!TextUtils.isEmpty(pref.getText())) {
                    //pref.setSummary(pref.getText());
                    //mSettings.edit().putString(Constants.VEHICLE_NAME, pref.getText()).apply();
                }
                break;

            case Constants.ODOMETER:
                /*
                EditTextPreference mileage = settingFragment.findPreference(key);
                log.i("EditTextPref: %s", mileage.getText());
                if(!TextUtils.isEmpty(mileage.getText())) {
                    //mileage.setSummary(mileage.getText() + "km");
                    //mSettings.edit().putString(Constants.ODOMETER, mileage.getText()).apply();
                }
                */
                break;

            case Constants.DISTRICT:
                log.i("District changed");
                distCode = convJSONArrayToList().get(2);
                priceRegionalTask = ThreadManager.startRegionalPriceTask(this, priceModel, distCode, null);
                mSettings.edit().putLong(Constants.OPINET_LAST_UPDATE, System.currentTimeMillis()).apply();
                break;

            case "pref_location_autoupdate":
                break;

            case "pref_favorite_provider":
                log.i("Favorite Provider changed");
                break;

            case "pref_edit_image":
                log.i("EditImage");
                break;
        }

    }

    @Override
    public void onSelectImageMedia(int which) {

        switch(which) {
            case 0: // Gallery
                Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
                if (galleryIntent.resolveActivity(getPackageManager()) != null) {
                    log.i("galleryIntent: %s", galleryIntent);
                    //galleryIntent.putExtra(MediaStore.EXTRA_OUTPUT, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
                }

                startActivityForResult(galleryIntent, REQUEST_CODE_GALLERY);
                break;

            case 1: // Camera
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                //Intent chooser = Intent.createChooser(cameraIntent, "Choose camera");

                if(cameraIntent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(cameraIntent, REQUEST_CODE_CAMERA);
                }
                break;

            case 2: // Delete
                String uriString = mSettings.getString("croppedImageUri", null);
                log.i("Delete: %s", uriString);

                // BUG: it can't found the file.
                if(!TextUtils.isEmpty(uriString)) {
                    File file = new File(Uri.parse(uriString).getPath());
                    if(file.exists()) {
                        log.i("File exists");
                        mSettings.edit().putString("croppedImageUri", null).apply();

                    }

                    settingFragment.getCropImagePreference().setIcon(null);
                    if(file.delete()) Snackbar.make(frameLayout, "Deleted!", Snackbar.LENGTH_SHORT).show();
                }

                break;

        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode != RESULT_OK) return;

        CropImageHelper imageHelper = new CropImageHelper(this);
        int orientation;

        switch(requestCode) {

            case REQUEST_CODE_GALLERY:

                Uri galleryUri = data.getData();
                if(galleryUri == null) return;

                orientation = imageHelper.getImageOrientation(galleryUri);
                if(orientation != 0) galleryUri = imageHelper.rotateBitmapUri(galleryUri, orientation);

                Intent galleryIntent = new Intent(this, CropImageActivity.class);
                galleryIntent.setData(galleryUri);
                startActivityForResult(galleryIntent, REQUEST_CODE_CROP);


                break;

            case REQUEST_CODE_CAMERA:
                Uri cameraUri = data.getData();
                if(cameraUri == null) return;

                // Retrieve the image orientation and rotate it unless it is 0 by applying matrix
                orientation = imageHelper.getImageOrientation(cameraUri);
                if(orientation != 0) cameraUri = imageHelper.rotateBitmapUri(cameraUri, orientation);

                Intent cameraIntent = new Intent(this, CropImageActivity.class);
                cameraIntent.setData(cameraUri);
                startActivityForResult(cameraIntent, REQUEST_CODE_CROP);

                break;


            case REQUEST_CODE_CROP:

                final Uri croppedImageUri = data.getData();
                if(croppedImageUri != null)
                    mSettings.edit().putString("croppedImageUri", croppedImageUri.toString()).apply();

                //imageHelper.applyGlideForCroppedImage(this, croppedImageUri, null, imageView);

                // Create the bitmap based on the Uri which is passed from CropImageActivity.
                try {

                    RoundedBitmapDrawable roundedBitmap = drawRoundedBitmap(croppedImageUri);
                    settingFragment.getCropImagePreference().setIcon(roundedBitmap);
                    // Encode the bitmap to String based on Base64 format
                    //String encodedBitmap = imageHelper.encodeBitmapToBase64(croppedBitmap);


                } catch(IOException e) {
                    //Log.e(LOG_TAG, "IOException e: " + e.getMessage());
                }

                break;


        }


    }

    public RoundedBitmapDrawable drawRoundedBitmap(Uri uri) throws IOException {
        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
        RoundedBitmapDrawable roundedBitmap = RoundedBitmapDrawableFactory.create(getResources(), bitmap);
        roundedBitmap.setCircular(true);

        return roundedBitmap;
    }

    // Custom method that fragments herein may refer to SharedPreferences inherited from BaseActivity.
    public SharedPreferences getSettings() {
        return mSettings;
    }



}