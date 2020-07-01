package com.silverback.carman2;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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
import com.silverback.carman2.fragments.PermRationaleFragment;
import com.silverback.carman2.fragments.ProgbarDialogFragment;
import com.silverback.carman2.fragments.SettingAutoFragment;
import com.silverback.carman2.fragments.SettingFavorGasFragment;
import com.silverback.carman2.fragments.SettingFavorSvcFragment;
import com.silverback.carman2.fragments.SettingPrefFragment;
import com.silverback.carman2.fragments.SettingSvcItemFragment;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.viewmodels.FragmentSharedModel;
import com.silverback.carman2.viewmodels.ImageViewModel;
import com.silverback.carman2.viewmodels.OpinetViewModel;
import com.silverback.carman2.threads.GasPriceTask;
import com.silverback.carman2.threads.ThreadManager;
import com.silverback.carman2.utils.ApplyImageResourceUtil;
import com.silverback.carman2.utils.Constants;

import org.json.JSONArray;
import org.json.JSONException;

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
public class SettingPrefActivity extends BaseActivity implements
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback,
        CropImageDialogFragment.OnSelectImageMediumListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(SettingPrefActivity.class);


    // Constants
    private static final int REQUEST_CODE_GALLERY = 10;
    private static final int REQUEST_CODE_CAMERA = 11;
    private static final int REQUEST_CODE_CROP = 12;
    private static final int REQUEST_PERM_CAMERA = 1000;

    // Objects
    private FirebaseFirestore firestore;
    private FirebaseStorage storage;
    private ApplyImageResourceUtil applyImageResourceUtil;
    private ImageViewModel imgModel;
    //private OpinetViewModel opinetModel;
    private FragmentSharedModel fragmentModel;
    private SettingPrefFragment settingFragment;
    private GasPriceTask gasPriceTask;
    private Map<String, Object> uploadData;

    // UIs
    public Toolbar settingToolbar;
    private FrameLayout frameLayout;

    // Fields
    private String userId;
    private String distCode;
    private String userName;
    private String fuelCode;
    private String radius;
    private String userImage;
    private String jsonAutoData;
    private String permCamera;
    private Uri downloadUserImageUri;
    private int requestCode;


    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_general_setting);

        if(getIntent() != null) requestCode = getIntent().getIntExtra("requestCode", -1);

        // Permission check for CAMERA to get the user image.
        //checkPermissions(this, Manifest.permission.CAMERA);

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
        imgModel = new ViewModelProvider(this).get(ImageViewModel.class);
        //opinetModel = new ViewModelProvider(this).get(OpinetViewModel.class);
        FragmentSharedModel fragmentModel = new ViewModelProvider(this).get(FragmentSharedModel.class);
        uploadData = new HashMap<>();

        // Get the user id which is saved in the internal storage
        if(TextUtils.isEmpty(userId)) userId = getUserIdFromStorage(this);

        // Passes District Code(Sigun Code) and vehicle nickname to SettingPreferenceFragment for
        // setting the default spinner values in SpinnerDialogPrefernce and showing the summary
        // of the vehicle name respectively.
        JSONArray jsonDistArray = getDistrictJSONArray();
        //if(jsonDistArray == null) distCode = (getResources().getStringArray(R.array.default_district))[2];
        //else distCode = jsonDistArray.optString(2);

        // Attach SettingPreferencFragment in the FrameLayout
        settingFragment = new SettingPrefFragment();
        Bundle bundle = new Bundle();
        bundle.putString("district", jsonDistArray.toString());
        settingFragment.setArguments(bundle);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.frame_setting, settingFragment, "preferenceFragment")
                .addToBackStack(null)
                .commit();

        // Sync issue may occur.
        String imageUri = mSettings.getString(Constants.USER_IMAGE, null);
        if(!TextUtils.isEmpty(imageUri)) {
            applyImageResourceUtil.applyGlideToDrawable(imageUri, Constants.ICON_SIZE_PREFERENCE, imgModel);
            imgModel.getGlideDrawableTarget().observe(this, drawable ->
                settingFragment.getUserImagePreference().setIcon(drawable));
        }

        // On receiving the result of the educational dialogfragment showing the permission rationale,
        // which was invoked by shouldShowRequestPermissionRationale(), request permission again
        // if positive or show the message to tell the camera is disabled.
        fragmentModel.getPermission().observe(this, isPermitted -> {
            if(isPermitted) ActivityCompat.requestPermissions(this, new String[]{permCamera}, REQUEST_PERM_CAMERA);
            else Snackbar.make(frameLayout, getString(R.string.perm_msg_camera), Snackbar.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        //savedInstanceState.putString("userId", userId);//doubtful!!!
    }

    /*
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        //userId = savedInstanceState.getString("userId");//doubtful!!!
    }

     */

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

    /*
     * The return value should benefit when there are multiple fragments and they overrides the
     * OnOptionsItemSelected.
     * If the return value is true, the event will be consumed and won't fall through to functions
     * defined in onOptionsItemSelected overrided in other fragments. On the other hand, if the return
     * value is false, it may check that the menu id is identical and trace down to the callback
     * overrided in other fragments until it should be consumed.
     */
    @SuppressWarnings("ConstantConditions")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Check which fragment the parent activity contains, then if the fragment is SettingPreferenceFragment,
        // send back to MainActivity an intent holding preference changes when clicking the Up button.
        // Otherwise, if the parent activity contains any fragment other than SettingPreferenceFragment,
        // just pop the fragment off the back stack, which works like the Back command.
        Fragment targetFragment = getSupportFragmentManager().findFragmentById(R.id.frame_setting);

        if(item.getItemId() == android.R.id.home) {
            // The activity contains SettingPrefFragment
            if (targetFragment instanceof SettingPrefFragment) {
                // Upload user data to Firebase
                uploadUserDataToFirebase(uploadData);

                // Create Intent back to MainActivity which contains extras to notify the activity of
                // which have been changed.
                switch (requestCode) {
                    case Constants.REQUEST_MAIN_SETTING_GENERAL:
                        log.i("Back to MainActivity: %s", distCode);
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("district", distCode);
                        resultIntent.putExtra("userName", userName);
                        resultIntent.putExtra("fuelCode", fuelCode);
                        resultIntent.putExtra("radius", radius);
                        resultIntent.putExtra("userImage", userImage);
                        setResult(RESULT_OK, resultIntent);
                        break;

                    case Constants.REQUEST_BOARD_SETTING_AUTOCLUB:
                        Intent autoIntent = new Intent();
                        autoIntent.putExtra("jsonAutoData", jsonAutoData);
                        log.i("JSON Auto Data in Setting: %s", jsonAutoData);
                        setResult(RESULT_OK, autoIntent);
                        break;

                    case Constants.REQUEST_BOARD_SETTING_USERNAME:
                        Intent userIntent = new Intent();
                        userIntent.putExtra("userName", userName);
                        setResult(RESULT_OK, userIntent);
                        break;

                    default: break;
                }

                finish();
                return true;

            } else {
                // The return value must be false to make optionsItemSelected() in SettingAutoFragment
                // feasible.
                getSupportFragmentManager().popBackStack();
                getSupportActionBar().setTitle(getString(R.string.setting_toolbar_title));
                return false;
            }

        } else return item.getItemId() != R.id.menu_add_service_item;

    }

    /*
     * Invoked when a preference which has an associated (dialog)fragment is tapped. If you do not
     * implement onPreferenceStartFragment(), a fallback implementation is used instead. While this works
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

        // Chagne the toolbar title according to the fragment the parent activity contains. When
        // returning to the SettingPrefFragment, the title
        String title = null;
        if(fragment instanceof SettingAutoFragment) title = getString(R.string.pref_auto_title);
        else if(fragment instanceof SettingFavorGasFragment) title = getString(R.string.pref_favorite_gas);
        else if(fragment instanceof SettingFavorSvcFragment) title = getString(R.string.pref_favorite_svc);
        else if(fragment instanceof SettingSvcItemFragment) title = getString(R.string.pref_service_chklist);

        getSupportActionBar().setTitle(title);
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
                userName = mSettings.getString(key, null);
                // Check first if the user id file exists. If so, set the user data or update the
                // data, otherwise.
                //if(userName != null) {
                // TextUtils.isEmpty(str) indicates str == null || str.length = 0;
                if(!TextUtils.isEmpty(userName)) uploadData.put("user_name", userName);
                break;

            case Constants.AUTO_DATA:
                String jsonAutoData = mSettings.getString(Constants.AUTO_DATA, null);
                // Auto data should be saved both in SharedPreferences and Firestore for a statistical
                // use.
                if(jsonAutoData != null && !jsonAutoData.isEmpty()) {
                    //Map<String, Object> autoData = new HashMap<>();
                    //autoData.put("auto_data", jsonAutoData);
                    //uploadUserDataToFirebase(autoData);
                    uploadData.put("user_club", jsonAutoData);

                    // send the result bact to BoardActivity for startActivityForResult()
                    log.i("JSON AutoData: %s", jsonAutoData);
                    this.jsonAutoData = jsonAutoData;

                }

                break;

            case Constants.FUEL:
                fuelCode = mSettings.getString(key, null);
                break;

            case Constants.DISTRICT:
                try {
                    String jsonDist = mSettings.getString(key, null);
                    JSONArray jsonDistArray = new JSONArray(jsonDist);
                    distCode = jsonDistArray.optString(2);

                } catch(JSONException e) {e.printStackTrace();}
                break;

            case Constants.SEARCHING_RADIUS:
                radius = mSettings.getString(key, null);
                break;

            case Constants.USER_IMAGE:
                userImage = mSettings.getString(key, null);
                log.i("userimage changed: %s", userImage);
                break;
        }


    }

    // Implements OnSelectImageMediumListener defined in CropImageDialogFragment
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

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    permCamera = Manifest.permission.CAMERA;
                    checkCameraPermission();
                } else {
                    Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    //Intent chooser = Intent.createChooser(cameraIntent, "Choose camera");
                    if (cameraIntent.resolveActivity(getPackageManager()) != null) {
                        startActivityForResult(cameraIntent, REQUEST_CODE_CAMERA);
                    }
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
                        uploadUserImageToFirebase(null);
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
                if(croppedImageUri != null) {
                    // Upload the cropped user image to Firestore with the user id fetched
                    mSettings.edit().putString(Constants.USER_IMAGE, null).apply();
                    uploadUserImageToFirebase(croppedImageUri);
                }

                break;
        }
    }

    // Upload the data to Firestore.
    private void uploadUserDataToFirebase(Map<String, Object> data) {
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
    private void uploadUserImageToFirebase(Uri uri) {
        log.i("user id of Image upaloading: %s", userId);
        // Popup the progressbar displaying dialogfragment.
        ProgbarDialogFragment progbarFragment = new ProgbarDialogFragment();
        String msg = (uri == null)?
                getString(R.string.setting_msg_remove_image):
                getString(R.string.setting_msg_upload_image);

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

    // Get SharedPreferences which is referenced by child fragments
    public SharedPreferences getSettings() {
        return mSettings;
    }

    // Check if the camera permission is granted.
    private void checkCameraPermission() {
        if(ContextCompat.checkSelfPermission(this, permCamera) == PackageManager.PERMISSION_GRANTED) {
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            //Intent chooser = Intent.createChooser(cameraIntent, "Choose camera");
            if (cameraIntent.resolveActivity(getPackageManager()) != null)
                startActivityForResult(cameraIntent, REQUEST_CODE_CAMERA);

        } else if(ActivityCompat.shouldShowRequestPermissionRationale(this, permCamera)) {
            PermRationaleFragment permDialog = new PermRationaleFragment();
            permDialog.setContents("Hell", "World");
            permDialog.show(getSupportFragmentManager(), null);

        } else {
            ActivityCompat.requestPermissions(this, new String[]{permCamera}, REQUEST_CODE_CAMERA);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permission, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERM_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                //Intent chooser = Intent.createChooser(cameraIntent, "Choose camera");
                if (cameraIntent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(cameraIntent, REQUEST_CODE_CAMERA);
                }
            } else {
                Snackbar.make(frameLayout, getString(R.string.perm_msg_camera), Snackbar.LENGTH_SHORT).show();
            }
        }
    }
}