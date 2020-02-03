package com.silverback.carman2;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.FrameLayout;

import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.silverback.carman2.fragments.CropImageDialogFragment;
import com.silverback.carman2.fragments.SettingPreferenceFragment;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.OpinetViewModel;
import com.silverback.carman2.threads.GasPriceTask;
import com.silverback.carman2.threads.ThreadManager;
import com.silverback.carman2.utils.Constants;
import com.silverback.carman2.utils.EditImageHelper;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;


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
    private FirebaseFirestore firestore;
    private FirebaseStorage storage;
    private OpinetViewModel priceModel;
    private SettingPreferenceFragment settingFragment;
    private GasPriceTask gasPriceTask;
    private String distCode;
    private String username;
    private String fuelCode;
    private String radius;

    // UIs
    private FrameLayout frameLayout;

    // Fields
    private boolean isDistrictReset;
    private Uri downloadUserImageUri;


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

        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        priceModel = new ViewModelProvider(this).get(OpinetViewModel.class);

        // Passes District Code(Sigun Code) and vehicle nickname to SettingPreferenceFragment for
        // setting the default spinner values in SpinnerDialogPrefernce and showing the summary
        // of the vehicle name respectively.
        JSONArray jsonDistArray = BaseActivity.getDistrictJSONArray();
        if(jsonDistArray == null) distCode = "0101";
        else distCode = jsonDistArray.optString(2);

        settingFragment = new SettingPreferenceFragment();
        //settingFragment.setArguments(args);

        // Attach SettingPreferencFragment in the FrameLayout
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.frame_setting, settingFragment)
                //.addToBackStack(null)
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
        if(gasPriceTask != null) gasPriceTask = null;
    }


    @Override
    public void onBackPressed() {}


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Check which fragment the parent activity contains at first, then if the fragment is
        // SettingPreferenceFragment, send back an intent holding preference changes to MainActivity
        // when clicking the Up button. Otherwise, if the parent activity contains any fragment other
        // than SettingPreferenceFragment, just pop this fragment off the back stack, which works
        // like the Back command.
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.frame_setting);
        if(fragment instanceof SettingPreferenceFragment) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("isDistrictReset", isDistrictReset);
            resultIntent.putExtra("username", username);
            resultIntent.putExtra("fuelCode", fuelCode);
            resultIntent.putExtra("radius", radius);
            setResult(Activity.RESULT_OK, resultIntent);
            finish();
            return true;

        } else {
            //
            if(item.getItemId() == android.R.id.home) {
                getSupportFragmentManager().popBackStack();
                return false;
            }
        }

        return false;
    }

    /*
     * Invoked when a Preference with an associated Fragment is tabbed. If you do not implement
     * onPreferenceStartFragment(), a fallback implementation is used instead.
     * While this works in most cases, it is strongly recommend to implement this method, thereby
     * you can fully configure transitions b/w Fragment objects and update the title in the toolbar,
     * if applicable.
     */
    @SuppressWarnings("ConstantConditions")
    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
        final Bundle args = pref.getExtras();
        Fragment fragment = getSupportFragmentManager().getFragmentFactory()
                .instantiate(getClassLoader(), pref.getFragment());

        fragment.setArguments(args);
        fragment.setTargetFragment(caller, 0);

        getSupportActionBar().setTitle(pref.getTitle());
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.frame_setting, fragment)
                .addToBackStack(null)
                .commit();

        return true;
    }

    // SharedPreferences.OnSharedPreferenceChangeListener invokes this callback method if and only if
    // any preference has changed.
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch(key) {
            case Constants.USER_NAME:
                log.i("nickname changed: %s", mSettings.getString(key, null));
                // Change the nickname after verifying it, then upload it to the Firestore.
                username = mSettings.getString(Constants.USER_NAME, null);
                // Check first if the user id file exists. If so, set the user data or update the
                // data, otherwise.
                if(username != null) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("user_name", username);
                    uploadUserDataToFirestore(data);
                }
                break;

            case Constants.VEHICLE:
                log.i("Auto data changed:");
                String jsonAutoData = mSettings.getString(Constants.VEHICLE, null);

                // Auto data should be saved both in SharedPreferences and Firestore for a statistical
                // use.
                if(jsonAutoData != null && !jsonAutoData.isEmpty()) {
                    Map<String, Object> autoData = new HashMap<>();
                    autoData.put("auto_data", jsonAutoData);
                    uploadUserDataToFirestore(autoData);
                }

                break;

            case Constants.FUEL:
                log.i("Fuel: %s", mSettings.getString(Constants.FUEL, null));
                fuelCode = mSettings.getString(key, null);
                break;

            case Constants.DISTRICT:
                log.i("District changed");
                JSONArray jsonDistArray = BaseActivity.getDistrictJSONArray();
                if(jsonDistArray != null) {
                    distCode = BaseActivity.getDistrictJSONArray().optString(2);
                    log.i("District Code : %s", distCode);
                    gasPriceTask = ThreadManager.startGasPriceTask(this, priceModel, distCode, null);
                    mSettings.edit().putLong(Constants.OPINET_LAST_UPDATE, System.currentTimeMillis()).apply();
                    isDistrictReset = true;
                }
                break;

            case Constants.SEARCHING_RADIUS:
                log.i("Radius changed");
                radius = mSettings.getString(key, null);
                break;

            case Constants.EDIT_IMAGE:
                log.i("Edit image");
                break;


        }


    }

    // Callback by CropImageDialogFragment.OnSelectImageMediumListener to notify which
    // image media to select in the dialog.
    @Override
    public void onSelectImageMedia(int which) {

        switch(which) {
            case 0: // Gallery
                Intent galleryIntent = new Intent();
                galleryIntent.setType("image/*");
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                /*
                Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
                if (galleryIntent.resolveActivity(getPackageManager()) != null) {
                    log.i("galleryIntent: %s", galleryIntent);
                    //galleryIntent.putExtra(MediaStore.EXTRA_OUTPUT, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
                }
                */
                startActivityForResult(galleryIntent, REQUEST_CODE_GALLERY);
                //startActivityForResult(Intent.createChooser(galleryIntent, "Select Image"), REQUEST_CODE_GALLERY);
                break;

            case 1: // Camera
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                Intent chooser = Intent.createChooser(cameraIntent, "Choose camera");

                if(cameraIntent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(chooser, REQUEST_CODE_CAMERA);
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

    // Callback by startActivityForResult defined in OnSelectImageMedia, receiving the uri of
    // a selected image back from Gallery or Camera with REQUEST_CODE_GALLERY OR REQUEST_CODE_CAMERA,
    // then creating an intent w/ the Uri to instantiate CropImageActivity to edit the image,
    // the result of which is sent to REQUEST_CROP_IMAGE.
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode != RESULT_OK) return;

        EditImageHelper cropHelper = new EditImageHelper(this);
        int orientation;

        switch(requestCode) {

            case REQUEST_CODE_GALLERY:
                Uri galleryUri = data.getData();
                if(galleryUri == null) return;

                orientation = cropHelper.getImageOrientation(galleryUri);
                if(orientation != 0) galleryUri = cropHelper.rotateBitmapUri(galleryUri, orientation);

                log.i("galleryUri: %s", galleryUri);

                Intent galleryIntent = new Intent(this, CropImageActivity.class);
                galleryIntent.setData(galleryUri);
                startActivityForResult(galleryIntent, REQUEST_CODE_CROP);

                File tmpRotated = new File(galleryUri.getPath());
                log.i("tmpRoated file Path: %s", tmpRotated);
                if(tmpRotated.exists() && tmpRotated.delete()) log.i("Deleted");


                break;

            case REQUEST_CODE_CAMERA:
                Uri cameraUri = data.getData();
                if(cameraUri == null) return;

                // Retrieve the image orientation and rotate it unless it is 0 by applying matrix
                orientation = cropHelper.getImageOrientation(cameraUri);
                if(orientation != 0) cameraUri = cropHelper.rotateBitmapUri(cameraUri, orientation);

                Intent cameraIntent = new Intent(this, CropImageActivity.class);
                cameraIntent.setData(cameraUri);
                startActivityForResult(cameraIntent, REQUEST_CODE_CROP);

                break;


            case REQUEST_CODE_CROP:

                final Uri croppedImageUri = data.getData();
                if(croppedImageUri != null) {

                    // Save the image uri in SharedPreferenes.
                    mSettings.edit().putString("croppedImageUri", croppedImageUri.toString()).apply();
                    // Fetch the uid generated by Firestore and saved in the internal storage.
                    try (FileInputStream fis = openFileInput("userId");
                         BufferedReader br = new BufferedReader(new InputStreamReader(fis))) {

                        final String userId = br.readLine();
                        loadUserImageToFirebase(croppedImageUri, userId);

                    } catch(IOException e) {
                        log.e("IOException: %s", e.getMessage());
                    }


                }

                // Create the bitmap based on the Uri which is passed from CropImageActivity.
                try {
                    RoundedBitmapDrawable roundedBitmap = cropHelper.drawRoundedBitmap(croppedImageUri);
                    settingFragment.getCropImagePreference().setIcon(roundedBitmap);

                } catch(IOException e) {
                    log.e("IOException: %s", e.getMessage());
                }

                break;
        }
    }

    // Upload the data to Firestore.
    private void uploadUserDataToFirestore(Map<String, Object> data) {
        // Read the user id containing file which is saved in the internal storage.
        try (FileInputStream fis = openFileInput("userId");
             BufferedReader br = new BufferedReader(new InputStreamReader(fis))) {

            final String id = br.readLine();
            final DocumentReference docRef = firestore.collection("users").document(id);
            docRef.set(data, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> log.i("Successful"))
                    .addOnFailureListener(e -> log.e("Failed"));

        } catch(IOException e) {
            log.e("IOException: %s", e.getMessage());
        }
    }


    // Upload the edited user image, the uri of which is saved in the internal storage, to the
    // firebase storage, then move on to get the download url from there.
    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    private void loadUserImageToFirebase(Uri uriSource, String userId) {

        final StorageReference storageRef = storage.getReference();
        final StorageReference userImgRef = storageRef.child("user_pic/" + userId + ".jpg");

        UploadTask uploadTask = userImgRef.putFile(uriSource);
        uploadTask.addOnProgressListener(listener -> log.i("progresslistener")
        ).addOnSuccessListener(taskSnapshot -> log.i("task succeeded")
        ).addOnFailureListener(e -> log.e("Upload failed"));

        // Fetch the download uri of the uploaded image and upload the uri to "users" collection
        uploadTask.continueWithTask(task -> {
            if(!task.isSuccessful()) task.getException();
            return userImgRef.getDownloadUrl();

        }).addOnCompleteListener(task -> {
            if(task.isSuccessful()) {
                downloadUserImageUri = task.getResult();
                log.i("Download uri: %s", downloadUserImageUri);

                Map<String, String> uriUserImage = new HashMap<>();
                uriUserImage.put("user_pic", downloadUserImageUri.toString());

                firestore.collection("users").document(userId)
                        .set(uriUserImage, SetOptions.merge())
                        .addOnCompleteListener(listener -> {
                            if(listener.isSuccessful()) log.i("upload the image uri to Firestore");
                        });
            } else log.w("No uri fetched");
        });
    }


    // Custom method that fragments herein may refer to SharedPreferences inherited from BaseActivity.
    public SharedPreferences getSettings() {
        return mSettings;
    }
}