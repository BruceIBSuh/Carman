package com.silverback.carman2;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.FrameLayout;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.silverback.carman2.fragments.CropImageDialogFragment;
import com.silverback.carman2.fragments.ProgbarDialogFragment;
import com.silverback.carman2.fragments.SettingAutoFragment;
import com.silverback.carman2.fragments.SettingPreferenceFragment;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.ImageViewModel;
import com.silverback.carman2.models.OpinetViewModel;
import com.silverback.carman2.threads.GasPriceTask;
import com.silverback.carman2.threads.ThreadManager;
import com.silverback.carman2.utils.ApplyImageResourceUtil;
import com.silverback.carman2.utils.Constants;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * This activity contains PreferenceFragmentCompat which is a special fragment to persist values
 * of preferences in SharedPreferences. Any value that has changed will be back to MainActivty
 * b/c this activity has been started by startActivityForResult() in MainActivity.
 *
 * CropImageDialogFragment is to select which media to use out of Gallery or Camera, the result of
 * which returns by the attached listener.
 */
public class SettingPreferenceActivity extends BaseActivity implements
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback,
        SettingAutoFragment.OnToolbarTitleListener, //when leaving SettingAutoFragment, reset the toolbar title
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
    private ApplyImageResourceUtil applyImageResourceUtil;
    private OpinetViewModel priceModel;
    private ImageViewModel imgModel;
    private SettingPreferenceFragment settingFragment;
    private GasPriceTask gasPriceTask;
    private Map<String, Object> uploadData;

    // UIs
    public Toolbar settingToolbar;
    private FrameLayout frameLayout;

    // Fields
    private String userId;
    private boolean isDistrictReset;
    private String distCode;
    private String userName;
    private String fuelCode;
    private String radius;
    private String userImage;
    private Uri downloadUserImageUri;


    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_general_setting);

        userId = getUserId();
        log.i("user id: %s", userId);

        settingToolbar = findViewById(R.id.toolbar_setting);
        setSupportActionBar(settingToolbar);
        // Get a support ActionBar corresponding to this toolbar
        //ActionBar ab = getSupportActionBar();
        // Enable the Up button which enables it as an action button such that when the user presses
        // it, the parent activity receives a call to onOptionsItemSelected().
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getString(R.string.setting_toolbar_title));

        frameLayout = findViewById(R.id.frame_setting);

        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        applyImageResourceUtil = new ApplyImageResourceUtil(this);
        priceModel = new ViewModelProvider(this).get(OpinetViewModel.class);
        imgModel = new ViewModelProvider(this).get(ImageViewModel.class);
        uploadData = new HashMap<>();

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
                .replace(R.id.frame_setting, settingFragment, "preferenceFragment")
                .addToBackStack(null)
                .commit();

        // Set the user image to the custom preference icon using ApplyImageResourceUtil
        // .applyGlideToDrawable() and receive a drawable as a LiveData that Glide transforms
        // the user image for fitting to a given size.
        String imageUri = mSettings.getString(Constants.USER_IMAGE, null);
        applyImageResourceUtil.applyGlideToDrawable(imageUri, Constants.ICON_SIZE_PREFERENCE, imgModel);

        // ViewModel listener to have drawable for the user icon processed by Glide each time when
        // the activity is created or the icon image is chaged or removed.
        imgModel.getGlideDrawableTarget().observe(this, drawable -> {
            //settingFragment.setUserImageIcon(drawable);
            settingFragment.getUserImagePreference().setIcon(drawable);
        });
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
    public void onBackPressed() {
        log.i("onBackPressed");
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Check which fragment the parent activity contains at first, then if the fragment is
        // SettingPreferenceFragment, send back an intent holding preference changes to MainActivity
        // when clicking the Up button. Otherwise, if the parent activity contains any fragment other
        // than SettingPreferenceFragment, just pop this fragment off the back stack, which works
        // like the Back command.
        Fragment targetFragment = getSupportFragmentManager().findFragmentById(R.id.frame_setting);

        if(targetFragment instanceof SettingPreferenceFragment) {
            // Upload user data to Firebase
            uploadUserData(uploadData);
            // Create Intent back to MainActivity which contains extras to notify the activity of
            // which have been changed.
            Intent resultIntent = new Intent();
            resultIntent.putExtra("isDistrictReset", isDistrictReset);
            resultIntent.putExtra("userName", userName);
            resultIntent.putExtra("fuelCode", fuelCode);
            resultIntent.putExtra("radius", radius);
            resultIntent.putExtra("userImage", userImage);
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
     * Invoked when a preference with an associated (dialog)fragment is tabbed. If you do not implement
     * onPreferenceStartFragment(), a fallback implementation is used instead. While this works
     * in most cases, it is strongly recommend to implement this method, thereby you can fully configure
     * transitions b/w Fragment objects and update the title in the toolbar, if applicable.
     */
    @SuppressWarnings("ConstantConditions")
    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
        final Bundle args = pref.getExtras();
        Fragment fragment = getSupportFragmentManager().getFragmentFactory()
                .instantiate(getClassLoader(), pref.getFragment());

        fragment.setArguments(args);
        fragment.setTargetFragment(caller, 0);

        // In case a preference calls PreferenceFragmentCompat and newly set the title again, it makes
        // the activity toolbar title changed as well.
        if(fragment instanceof SettingAutoFragment) {
            getSupportActionBar().setTitle(getString(R.string.pref_fragment_auto_title));
            ((SettingAutoFragment) fragment).addTitleListener(this);
        }
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.frame_setting, fragment)
                .addToBackStack(null)
                .commit();

        return true;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void notifyResetTitle() {
        getSupportActionBar().setTitle(getString(R.string.setting_toolbar_title));
    }

    // SharedPreferences.OnSharedPreferenceChangeListener invokes this callback method if and only if
    // any preference has changed.
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        switch(key) {
            case Constants.USER_NAME:
                userName = mSettings.getString(Constants.USER_NAME, null);
                // Check first if the user id file exists. If so, set the user data or update the
                // data, otherwise.
                if(userName != null) {
                    //Map<String, Object> data = new HashMap<>();
                    //data.put("user_name", userName);
                    //uploadUserData(data);
                    uploadData.put("user_name", userName);
                }
                break;

            case Constants.AUTO_DATA:
                String jsonAutoData = mSettings.getString(Constants.AUTO_DATA, null);
                // Auto data should be saved both in SharedPreferences and Firestore for a statistical
                // use.
                if(jsonAutoData != null && !jsonAutoData.isEmpty()) {
                    //Map<String, Object> autoData = new HashMap<>();
                    //autoData.put("auto_data", jsonAutoData);
                    //uploadUserData(autoData);
                    uploadData.put("auto_data", jsonAutoData);
                }

                break;

            case Constants.FUEL:
                fuelCode = mSettings.getString(key, null);
                log.i("SharedPreferences: %s", sharedPreferences.getString(key, null));
                break;

            case Constants.DISTRICT:
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

            case Constants.USER_IMAGE:
                log.i("user image changed");
                userImage = mSettings.getString(key, null);
                break;
        }


    }

    // Implement the callback of CropImageDialogFragment.OnSelectImageMediumListener to have the
    // image mediastore selected in the dialog.
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
                // Check if the carmear is available for the device.
                if(!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) return;

                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                //Intent chooser = Intent.createChooser(cameraIntent, "Choose camera");

                if(cameraIntent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(cameraIntent, REQUEST_CODE_CAMERA);
                }
                break;

            case 2: // Remove image
                // Bugs: to remove the image, the image should be removed not only from SahredPreferences
                // but also FireStorage and FireStore. Here the image is removed only from Shared.
                String uriString = mSettings.getString(Constants.USER_IMAGE, null);
                if(!TextUtils.isEmpty(uriString)) {
                    // Actually, delete the file provided as the contentUri in FileProvider.
                    // Consider that the cropped image file remains in the storage while  the uri
                    // saved in SharedPreferences should be deleted.
                    int delete = getContentResolver().delete(Uri.parse(uriString), null, null);
                    if(delete > 0) {
                        // Delete the file in SharedPreferences
                        log.i("delete image file: %s, %s", uriString, delete);
                        settingFragment.getUserImagePreference().setIcon(null);
                        mSettings.edit().putString(Constants.USER_IMAGE, null).apply();
                        userImage = null;

                        // Delete the file in Firebase.
                        uploadUserImage(null);
                    }
                }

                break;

        }
    }

    // Callback by startActivityForResult() defined in onSelectImageMedia(), receiving the uri of
    // a selected image back from Gallery or Camera w/ each request code, then creating an intent
    // w/ the Uri to instantiate CropImageActivity to edit the image. The result is, in turn, sent
    // back here once again.
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode != RESULT_OK) return;

        ApplyImageResourceUtil cropHelper = new ApplyImageResourceUtil(this);
        Uri imageUri;
        int orientation;

        switch(requestCode) {

            case REQUEST_CODE_GALLERY:
                //Uri galleryUri = data.getData();
                imageUri = data.getData();
                if(imageUri == null) return;

                // Get the image orinetation and check if it is 0 degree. Otherwise, the image
                // requires to be rotated.
                orientation = cropHelper.getImageOrientation(imageUri);
                if(orientation != 0) imageUri = cropHelper.rotateBitmapUri(imageUri, orientation);
                log.i("galleryUri: %s, %s", orientation, imageUri);

                Intent galleryIntent = new Intent(this, CropImageActivity.class);
                galleryIntent.setData(imageUri);
                startActivityForResult(galleryIntent, REQUEST_CODE_CROP);

                /*
                File tmpRotated = new File(imageUri.getPath());
                log.i("tmpRoated file Path: %s", tmpRotated);
                if(tmpRotated.exists() && tmpRotated.delete()) log.i("Deleted");
                */
                break;

            case REQUEST_CODE_CAMERA:
                imageUri = data.getData();
                if(imageUri == null) return;

                // Retrieve the image orientation and rotate it unless it is 0 by applying matrix
                orientation = cropHelper.getImageOrientation(imageUri);
                if(orientation != 0) imageUri = cropHelper.rotateBitmapUri(imageUri, orientation);

                Intent cameraIntent = new Intent(this, CropImageActivity.class);
                cameraIntent.setData(imageUri);
                startActivityForResult(cameraIntent, REQUEST_CODE_CROP);

                break;

            // Result from CropImageActivity with a cropped image uri and set the image to
            case REQUEST_CODE_CROP:
                final Uri croppedImageUri = data.getData();
                log.i("croppedImageUri: %s", croppedImageUri);
                if(croppedImageUri != null) {
                    // Upload the cropped user image to Firestore with the user id fetched
                    uploadUserImage(croppedImageUri);
                }

                break;
        }
    }

    // Upload the data to Firestore.
    private void uploadUserData(Map<String, Object> data) {
        // Read the user id containing file which is saved in the internal storage.
        final DocumentReference docRef = firestore.collection("users").document(userId);
        docRef.set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> log.i("Successful"))
                .addOnFailureListener(e -> log.e("Failed"));
    }


    // Upload the cropped user image, the uri of which is saved in the internal storage, to Firebase
    // storage, then move on to get the download url. The url, in turn, is uploaded to "user" collection
    // of Firesotre. When the uploading process completes, put the uri in SharedPreferenes.
    // At the same time, the new uri has to be uploaded in the documents written by the user.
    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    private void uploadUserImage(Uri uri) {
        // Popup the progressbar displaying dialogfragment.
        ProgbarDialogFragment progbarFragment = new ProgbarDialogFragment();
        String msg = (uri == null)?
                getString(R.string.setting_msg_remove_image):getString(R.string.setting_msg_upload_image);

        progbarFragment.setProgressMsg(msg);
        getSupportFragmentManager().beginTransaction().add(android.R.id.content, progbarFragment).commit();

        // Instantiate Firebase Storage.
        final StorageReference storageRef = storage.getReference();
        final StorageReference userImgRef = storageRef.child("user_pic/" + userId + ".jpg");

        // Delete the file from FireStorage and, if successful, it goes to FireStore to delete the
        // Url of the image.
        if(uri == null) {
            userImgRef.delete().addOnSuccessListener(aVoid -> {
                // Delete(update) the document with null value.
                DocumentReference docref = firestore.collection("users").document(userId);
                docref.update("user_pic", null).addOnSuccessListener(bVoid -> {
                    Snackbar.make(frameLayout, getString(R.string.pref_snackbar_image_deleted),
                            Snackbar.LENGTH_SHORT).show();

                    // Dismiss the progbar dialogfragment
                    progbarFragment.dismiss();
                }).addOnFailureListener(Throwable::printStackTrace);

            }).addOnFailureListener(Throwable::printStackTrace);

            return;
        }

        // Upload a new user image file to Firebase.Storage.
        UploadTask uploadTask = userImgRef.putFile(uri);
        uploadTask.addOnProgressListener(listener -> log.i("progresslistener"))
                .addOnSuccessListener(taskSnapshot -> log.i("task succeeded"))
                .addOnFailureListener(e -> log.e("Upload failed"));

        // On completing upload, return the download url which goes to Firebase.Firestore
        uploadTask.continueWithTask(task -> {
            if(!task.isSuccessful()) task.getException();
            return userImgRef.getDownloadUrl();

        }).addOnCompleteListener(task -> {
            if(task.isSuccessful()) {
                downloadUserImageUri = task.getResult();
                log.i("Download uri: %s", downloadUserImageUri);

                // Receive the image uri from Firebase Storage and upload the uri to FireStore to
                // reference the user image.
                Map<String, String> uriUserImage = new HashMap<>();
                uriUserImage.put("user_pic", downloadUserImageUri.toString());

                // Update the "user_pic" field of the document in the "users" collection
                firestore.collection("users").document(userId).set(uriUserImage, SetOptions.merge())
                        .addOnCompleteListener(userimgTask -> {
                            if(userimgTask.isSuccessful()) {
                                log.i("user image update in Firestore");
                                // On compeleting the upload, save the uri in SharedPreferences and
                                // set the drawable to the preferernce icon.
                                // ProgressBar should be come in here!!!
                                mSettings.edit().putString(Constants.USER_IMAGE, uri.toString()).apply();
                                applyImageResourceUtil.applyGlideToDrawable(
                                        uri.toString(), Constants.ICON_SIZE_PREFERENCE, imgModel);

                                // Dismiss the prgbar dialogfragment
                                progbarFragment.dismiss();
                            }
                        });

                // Update the user image in the posting items wrtiiten by the user.
                // Consider that all documents should be updated. Otherwise, limit condition would
                // be added.
                firestore.collection("board_general").whereEqualTo("user_id", userId).get()
                        .addOnCompleteListener(postTask -> {
                            if(postTask.isSuccessful() && postTask.getResult() != null) {
                                for(QueryDocumentSnapshot document : postTask.getResult()) {
                                    DocumentReference docRef = document.getReference();
                                    docRef.update("user_pic", downloadUserImageUri.toString());
                                }

                            }
                        });


            } //else log.w("No uri fetched");
        });
    }


    // Get the user id
    private String getUserId() {
        try(FileInputStream fis = openFileInput("userId");
            BufferedReader br = new BufferedReader(new InputStreamReader(fis))) {
            log.i("user id: %s", br.readLine());
            return br.readLine();
        } catch(IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    // Custom method that fragments herein may refer to SharedPreferences inherited from BaseActivity.
    public SharedPreferences getSettings() {
        return mSettings;
    }


}