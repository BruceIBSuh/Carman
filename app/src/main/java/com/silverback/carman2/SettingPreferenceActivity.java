package com.silverback.carman2;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.FrameLayout;

import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NavUtils;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
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
import com.silverback.carman2.fragments.SettingAutoFragment;
import com.silverback.carman2.fragments.SettingPreferenceFragment;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.OpinetViewModel;
import com.silverback.carman2.threads.PriceDistrictTask;
import com.silverback.carman2.threads.ThreadManager;
import com.silverback.carman2.utils.Constants;
import com.silverback.carman2.utils.CropImageHelper;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
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
    private PriceDistrictTask priceDistrictTask;
    private String distCode;

    // UIs
    private FrameLayout frameLayout;

    // Fields
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

        priceModel = ViewModelProviders.of(this).get(OpinetViewModel.class);

        // Passes District Code(Sigun Code) and vehicle nickname to SettingPreferenceFragment for
        // setting the default spinner values in SpinnerDialogPrefernce and showing the summary
        // of the vehicle name respectively.
        List<String> district = convJSONArrayToList();
        if(district == null) distCode = "0101";
        else distCode = district.get(2);
        String autoName = mSettings.getString(Constants.USER_NAME, null);

        /*
        Bundle args = new Bundle();
        args.putStringArray("district", convJSONArrayToList().toArray(new String[3]));
        //args.putString("distCode", convJSONArrayToList().get(2));
        args.putString("name", autoName);
        //args.putString(Constants.ODOMETER, mileage);

         */
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
        if(priceDistrictTask != null) priceDistrictTask = null;
    }


    @Override
    public void onBackPressed() {
        NavUtils.navigateUpFromSameTask(this);
        //finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if(getSupportFragmentManager().findFragmentById(R.id.frame_setting) instanceof SettingAutoFragment) {
            log.i("SettingAutoFragment");
            //getSupportFragmentManager().popBackStack();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.frame_setting, new SettingPreferenceFragment())
                    .addToBackStack(null)
                    .commit();
            return true;

        } else return super.onOptionsItemSelected(item);
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
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.frame_setting, fragment)
                .addToBackStack(null)
                .commit();

        return true;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        //final File userIdFile = new File(getFilesDir(), "user_id");

        switch(key) {

            case Constants.USER_NAME:
                // Change the nickname after verifying it, then upload it to the Firestore.
                String username = mSettings.getString(key, null);
                if(username == null || username.isEmpty()) return;
                // Check first if the user id file exists. If so, set the user data or update the
                // data, otherwise.
                Map<String, Object> data = new HashMap<>();
                data.put("user_name", username);

                uploadAutoDataToFirestore(data);

                break;

            case Constants.VEHICLE:
                log.i("Auto data changed:");
                String jsonAutoData = mSettings.getString(Constants.VEHICLE, null);
                if(jsonAutoData == null) return;

                Map<String, Object> autoData = new HashMap<>();
                try {
                    JSONArray jsonArray = new JSONArray(jsonAutoData);
                    autoData.put("auto_maker", jsonArray.optString(0));
                    autoData.put("auto_type", jsonArray.optString(1));
                    autoData.put("auto_model", jsonArray.optString(2));
                    autoData.put("auto_year", Integer.valueOf(jsonArray.optString(3)));
                } catch(JSONException e) {
                    log.e("JSONException: %s", e.getMessage());
                }

                uploadAutoDataToFirestore(autoData);

                break;

            case Constants.EDIT_IMAGE:
                log.i("Edit image");
                break;

            case Constants.DISTRICT:
                log.i("District changed");
                distCode = convJSONArrayToList().get(2);
                priceDistrictTask = ThreadManager.startPriceDistrictTask(this, priceModel, distCode, null);
                mSettings.edit().putLong(Constants.OPINET_LAST_UPDATE, System.currentTimeMillis()).apply();
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

        CropImageHelper cropHelper = new CropImageHelper(this);
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
                    try (FileInputStream fis = openFileInput("user_id");
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
    private void uploadAutoDataToFirestore(Map<String, Object> data) {
        //Map<String, Object> userData = new HashMap<>();
        //userData.put(fieldName, data);
        final File fileUserId = new File(getFilesDir(), "user_id");

        if(!fileUserId.exists()) {

            firestore.collection("users").add(data).addOnSuccessListener(docref -> {

                final String id = docref.getId();
                // Get the document id generated by Firestore and save it in the internal storage.
                try (final FileOutputStream fos = openFileOutput("user_id", Context.MODE_PRIVATE)) {
                    fos.write(id.getBytes());
                } catch (IOException e) {
                    log.e("IOException: %s", e.getMessage());
                }

            }).addOnFailureListener(e -> log.e("Add user failed: %s", e.getMessage()));

        } else {

            try (FileInputStream fis = openFileInput("user_id");
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
    }

    /*
    private void updateUserAutoToFirestore(String field, Object data) {
        try (FileInputStream fis = openFileInput("user_id");
             BufferedReader br = new BufferedReader(new InputStreamReader(fis))) {

            final String id = br.readLine();
            final DocumentReference docRef = firestore.collection("users").document(id);

            firestore.runTransaction(transaction -> {
                DocumentSnapshot snapshot = transaction.get(docRef);
                if(snapshot.getString(field) == null) {
                    log.i("Set merge");
                    Map<String, Object> autoData = new HashMap<>();
                    autoData.put(field, data);
                    transaction.set(docRef, autoData, SetOptions.merge());
                } else {
                    log.i("Update");
                    transaction.update(docRef, field, data);
                }

                return null;

            }).addOnSuccessListener(aVoid -> log.i("Transaction successful")
            ).addOnFailureListener(e -> log.e("Exception: %s", e.getMessage()));

        } catch(IOException e) {
            log.e("IOException occrurred when updating: %s", e.getMessage());
        }
    }
    */

    // Upload the edited user image, the uri of which is saved in the internal storage, to the
    // firebase storage, then move on to get the download url from there.
    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    private void loadUserImageToFirebase(Uri uriSource, String userId) {

        final StorageReference storageRef = storage.getReference();
        final StorageReference imageRef = storageRef.child("images");
        final StorageReference profileRef = imageRef.child(userId + "/profile.jpg");
        log.i("Image Path: %s", profileRef);

        UploadTask uploadTask = profileRef.putFile(uriSource);
        uploadTask.addOnProgressListener(listener -> {
            // Indicate loading progress in the icon.
            // settingFragment.getCropImagePreference().setIcon()

        }).addOnSuccessListener(taskSnapshot -> log.i("task succeeded")
        ).addOnFailureListener(e -> log.e("Upload failed"));

        /*
        uploadTask.continueWithTask(task -> {
            if(!task.isSuccessful()) task.getException();
            return profileRef.getDownloadUrl();

        }).addOnCompleteListener(task -> {
            if(task.isSuccessful()) {
                downloadUserImageUri = task.getResult();
                log.i("Download uri: %s", downloadUserImageUri);

                Map<String, String> uriUserImage = new HashMap<>();
                uriUserImage.put("user_image", downloadUserImageUri.toString());

                firestore.collection("users").document(userId)
                        .set(uriUserImage, SetOptions.merge())
                        .addOnCompleteListener(listener -> {
                            if(listener.isSuccessful()) log.i("upload the image uri to Firestore");
                        });

            } else log.w("No uri fetched");
        });

        */
    }


    // Custom method that fragments herein may refer to SharedPreferences inherited from BaseActivity.
    public SharedPreferences getSettings() {
        return mSettings;
    }
}