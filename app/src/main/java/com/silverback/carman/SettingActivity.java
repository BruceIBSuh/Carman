package com.silverback.carman;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
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
import com.silverback.carman.databinding.ActivitySettingsBinding;
import com.silverback.carman.fragments.CropImageDialogFragment;
import com.silverback.carman.fragments.PermRationaleFragment;
import com.silverback.carman.fragments.ProgressBarDialogFragment;
import com.silverback.carman.fragments.SettingAutoFragment;
import com.silverback.carman.fragments.SettingFavorGasFragment;
import com.silverback.carman.fragments.SettingFavorSvcFragment;
import com.silverback.carman.fragments.SettingPreferenceFragment;
import com.silverback.carman.fragments.SettingSvcItemFragment;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.threads.GasPriceTask;
import com.silverback.carman.utils.ApplyImageResourceUtil;
import com.silverback.carman.utils.Constants;
import com.silverback.carman.viewmodels.FragmentSharedModel;
import com.silverback.carman.viewmodels.ImageViewModel;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * This activity contains PreferenceFragmentCompat which is a special fragment to persist values
 * of preferences in SharedPreferences. Any value that has changed will be back to MainActivty
 * b/c this activity has been started by startActivityForResult() in MainActivity.
 *
 * CropImageDialogFragment is to select which media to use out of Gallery or Camera, the result of
 * which returns by the attached listener.
 */

public class SettingActivity extends BaseActivity implements
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback,
        CropImageDialogFragment.OnSelectImageMediumListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(SettingActivity.class);


    // Constants
    private static final int REQUEST_CODE_GALLERY = 10;
    private static final int REQUEST_CODE_CAMERA = 11;
    private static final int REQUEST_CODE_CROP = 12;
    private static final int REQUEST_PERM_CAMERA = 1000;

    // Objects
    private Intent resultIntent;

    private FirebaseFirestore firestore;
    private FirebaseStorage storage;
    private ApplyImageResourceUtil applyImageResourceUtil;
    private ImageViewModel imgModel;
    //private OpinetViewModel opinetModel;
    private FragmentSharedModel fragmentModel;
    private SettingPreferenceFragment settingFragment;
    private GasPriceTask gasPriceTask;
    private Map<String, Object> uploadData;


    // UIs
    private ActivitySettingsBinding binding;
    public Toolbar settingToolbar;
    private FrameLayout frameLayout;
    private Fragment childFragment;

    // Fields
    private String userId, distCode, userName, gasCode, radius, userImage, jsonAutoData, permCamera;
    //private String distCode;
    //private String userName;
    //private String gasCode;
    //private String radius;
    //private String userImage;
    //private String jsonAutoData;
    //private String permCamera;
    private Uri downloadUserImageUri;
    private int requestCode;

    private final ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), this::getActivityResultCallback);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // UI's
        frameLayout = binding.frameSetting;
        setSupportActionBar(binding.settingToolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getString(R.string.setting_toolbar_title));



        // get Intent data, if any.
        if(getIntent() != null) requestCode = getIntent().getIntExtra("caller", -1);
        log.i("request code: %s", requestCode);


        // Permission check for CAMERA to get the user image.
        //checkPermissions(this, Manifest.permission.CAMERA);
        //settingToolbar = findViewById(R.id.setting_toolbar);

        // Instantiate objects
        resultIntent = new Intent();
        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        applyImageResourceUtil = new ApplyImageResourceUtil(this);
        uploadData = new HashMap<>();

        // ViewModels
        FragmentSharedModel fragmentModel = new ViewModelProvider(this).get(FragmentSharedModel.class);
        imgModel = new ViewModelProvider(this).get(ImageViewModel.class);
        //opinetModel = new ViewModelProvider(this).get(OpinetViewModel.class);

        // Get the user id which is saved in the internal storage
        if(TextUtils.isEmpty(userId)) userId = getUserIdFromStorage(this);

        // Passes District Code(Sigun Code) and vehicle nickname to SettingPreferenceFragment for
        // setting the default spinner values in SpinnerDialogPrefernce and showing the summary
        // of the vehicle name respectively.
        JSONArray jsonDistArray = getDistrictJSONArray();
        //if(jsonDistArray == null) distCode = (getResources().getStringArray(R.array.default_district))[2];
        //else distCode = jsonDistArray.optString(2);
        // Attach SettingPreferencFragment in the FrameLayout
        settingFragment = new SettingPreferenceFragment();
        Bundle bundle = new Bundle();
        bundle.putString("district", jsonDistArray.toString());
        settingFragment.setArguments(bundle);
        addPreferenceFragment(settingFragment, getSupportFragmentManager());

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
            else Snackbar.make(binding.getRoot(), getString(R.string.perm_msg_camera), Snackbar.LENGTH_SHORT).show();
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
    public void onBackPressed() {
        log.i("onBackPressed in the parent activity");
    }
    /*
     * The return value should benefit when there are multiple fragments and they overrides the
     * OnOptionsItemSelected.
     * If the return value is true, the event will be consumed and won't fall through to functions
     * defined in onOptionsItemSelected overrided in other fragments. On the other hand, if the return
     * value is false, it may check that the menu id is identical and trace down to the callback
     * overrided in other fragments until it should be consumed.
     */
    //@SuppressWarnings("ConstantConditions")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Check which fragment the parent activity contains, then if the fragment is SettingPreferenceFragment,
        // send back to MainActivity an intent holding preference changes when clicking the Up button.
        // Otherwise, if the parent activity contains any fragment other than SettingPreferenceFragment,
        // just pop the fragment off the back stack, which works like the Back command.

        //childFragment = getSupportFragmentManager().findFragmentById(R.id.frame_setting);
        childFragment = getSupportFragmentManager().findFragmentByTag("settingGeneral");
        log.i("Fragment in the framelayout: %s", childFragment);

        if (item.getItemId() == android.R.id.home) {
            log.i("setting update");
            if(childFragment instanceof SettingPreferenceFragment) {
                switch(requestCode) {
                    case Constants.REQUEST_MAIN_SETTING_GENERAL:
                        log.i("result back to main");
                        setResult(requestCode, resultIntent);
                        break;
                    case Constants.REQUEST_BOARD_SETTING_AUTOCLUB:
                        setResult(requestCode, resultIntent);
                        break;
                }

                finish();
                return true;

            } else {
                getSupportFragmentManager().popBackStack();
                Objects.requireNonNull(getSupportActionBar()).setTitle(getString(R.string.setting_toolbar_title));
                return false;
            }

        // The return value should be false when it comes to the menu that adds a new service item,
        // which means this method will be handled in the SettingSvcItemFragment.
        } else return item.getItemId() != R.id.menu_add_service_item;
    }
    /*
        if(item.getItemId() == android.R.id.home) {
            uploadUserDataToFirebase(uploadData);
            Intent resultIntent = new Intent();
            resultIntent.putExtra("district", distCode);
            resultIntent.putExtra("userName", userName);
            resultIntent.putExtra("fuelCode", gasCode);
            resultIntent.putExtra("radius", radius);
            resultIntent.putExtra("userImage", userImage);
            log.i("resut data: %s, %s, %s, %s, %s", distCode, userName, gasCode, radius, userImage);
            setResult(RESULT_OK, resultIntent);
            finish();
            return true;

        } else super.onOptionsItemSelected(item);

        // Use FragmentManager.findFragmentByTag as far as a fragment is dynamically added.
        Fragment fragment = getSupportFragmentManager().findFragmentByTag("settingGeneral");
        log.i("Fragment in the framelayout: %s", fragment);

        if(item.getItemId() == android.R.id.home) {
            // The activity contains SettingPrefFragment
            if(fragment instanceof SettingPreferenceFragment) {
                // Upload user data to Firebase
                uploadUserDataToFirebase(uploadData);
                // Create Intent back to MainActivity which contains extras to notify the activity of
                // which have been changed.
                switch (requestCode) {
                    case Constants.REQUEST_MAIN_SETTING_GENERAL:
                        log.i("Back to MainActivity: %s", distCode);

                        //Intent resultIntent = new Intent();
                        resultIntent.putExtra("district", distCode);
                        resultIntent.putExtra("userName", userName);
                        resultIntent.putExtra("fuelCode", gasCode);
                        resultIntent.putExtra("radius", radius);
                        resultIntent.putExtra("userImage", userImage);

                        setResult(RESULT_OK, resultIntent);
                        break;

                    case Constants.REQUEST_BOARD_SETTING_AUTOCLUB:

                        Intent autoIntent = new Intent();
                        autoIntent.putExtra("jsonAutoData", jsonAutoData);
                        log.i("JSON Auto Data in Setting: %s", jsonAutoData);
                        setResult(requestCode, resultIntent);
                        break;

                    case Constants.REQUEST_BOARD_SETTING_USERNAME:
                        Intent userIntent = new Intent();
                        userIntent.putExtra("userName", userName);
                        setResult(requestCode, userIntent);
                        break;

                    default: break;
                }

                finish();
                return true;

            } else {
                // The return value must be false to make optionsItemSelected() in SettingAutoFragment
                // feasible.
                getSupportFragmentManager().popBackStack();
                Objects.requireNonNull(getSupportActionBar()).setTitle(getString(R.string.setting_toolbar_title));
                return false;
            }

        // The return value should be false when it comes to the menu that adds a new service item,
        // which means this method will be handled in the SettingSvcItemFragment.
        } else return item.getItemId() != R.id.menu_add_service_item;

    }

     */

    /*
     * Invoked when a preference which has an associated custom (dialog)fragment is tapped. If you do not
     * implement onPreferenceStartFragment(), a fallback implementation is used instead. While this works
     * in most cases, it is strongly recommend to implement this method, thereby you can fully configure
     * transitions b/w Fragment objects and update the title in the toolbar, if applicable.
     */
    //@SuppressWarnings("ConstantConditions")
    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
        log.i("onPreferenceStartFragment: %s, %s", caller.getId(), pref);
        final Bundle args = pref.getExtras();
        final Fragment fragment = getSupportFragmentManager()
                .getFragmentFactory()
                .instantiate(getClassLoader(), pref.getFragment());
        fragment.setArguments(args);

        getSupportFragmentManager().setFragmentResultListener("autodata", this, (requestKey, result) -> {
            log.i("FragmentResultListener: %s, %s", requestKey, result);
        });


        // Chagne the toolbar title according to the fragment the parent activity contains. When
        // returning to the SettingPrefFragment, the title
        String title = null;
        if(fragment instanceof SettingAutoFragment) title = getString(R.string.pref_auto_title);
        else if(fragment instanceof SettingFavorGasFragment) title = getString(R.string.pref_favorite_gas);
        else if(fragment instanceof SettingFavorSvcFragment) title = getString(R.string.pref_favorite_svc);
        else if(fragment instanceof SettingSvcItemFragment) title = getString(R.string.pref_service_chklist);

        Objects.requireNonNull(getSupportActionBar()).setTitle(title);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.frame_setting, fragment)
                .addToBackStack(null)
                .commit();

        return true;
    }

    // Implement SharedPreferences.OnSharedPreferenceChangeListener
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch(key) {
            case Constants.USER_NAME:
                userName = mSettings.getString(key, null);
                // Check first if the user id file exists. If so, set the user data or update the
                // data, otherwise.
                //if(userName != null) {
                // TextUtils.isEmpty(str) indicates str == null || str.length = 0;
                if(!TextUtils.isEmpty(userName)) {
                    uploadData.put("user_name", userName);
                    resultIntent.putExtra("userName", userName);
                }
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
                log.i("gas code changed");
                gasCode = mSettings.getString(key, null);
                resultIntent.putExtra("gasCode", gasCode);
                break;

            case Constants.DISTRICT:
                log.i("district code changed");
                try {
                    String jsonDist = mSettings.getString(key, null);
                    JSONArray jsonDistArray = new JSONArray(jsonDist);
                    distCode = jsonDistArray.optString(2);
                    resultIntent.putExtra("distCode", distCode);
                } catch(JSONException e) {e.printStackTrace();}
                break;

            case Constants.SEARCHING_RADIUS:
                radius = mSettings.getString(key, null);
                log.i("searching radius changed: %s", radius);
                resultIntent.putExtra("searchRadius", radius);
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
                galleryIntent.putExtra("requestCode", REQUEST_CODE_GALLERY);
                /*
                Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
                if (galleryIntent.resolveActivity(getPackageManager()) != null) {
                    log.i("galleryIntent: %s", galleryIntent);
                    //galleryIntent.putExtra(MediaStore.EXTRA_OUTPUT, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
                }
                */
                //startActivityForResult(galleryIntent, REQUEST_CODE_GALLERY);
                //startActivityForResult(Intent.createChooser(galleryIntent, "Select Image"), REQUEST_CODE_GALLERY);
                activityResultLauncher.launch(galleryIntent);
                break;

            case 1: // Camera
                // Check if the carmear is available for the device.
                if(!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) return;
                //if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    permCamera = Manifest.permission.CAMERA;
                    checkCameraPermission();
                //} else {
                    Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    //Intent chooser = Intent.createChooser(cameraIntent, "Choose camera");
                    if (cameraIntent.resolveActivity(getPackageManager()) != null) {
                        //startActivityForResult(cameraIntent, REQUEST_CODE_CAMERA);
                        activityResultLauncher.launch(cameraIntent);
                    }
                //}
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

    // Callback method for ActivityResultLauncher
    private void getActivityResultCallback(ActivityResult result) {
        if(result.getData() == null) return;

        ApplyImageResourceUtil cropHelper = new ApplyImageResourceUtil(this);
        //Uri imageUri = result.getData().getData();
        Intent intent = result.getData();
        int orientation;

        switch(result.getResultCode()) {
            case REQUEST_CODE_GALLERY:
                Uri galleryUri = Objects.requireNonNull(intent.getData());
                // Get the image orinetation and check if it is 0 degree. Otherwise, the image
                // requires to be rotated.
                orientation = cropHelper.getImageOrientation(galleryUri);
                if(orientation != 0) galleryUri = cropHelper.rotateBitmapUri(galleryUri, orientation);

                Intent galleryIntent = new Intent(this, CropImageActivity.class);
                galleryIntent.setData(galleryUri);
                galleryIntent.putExtra("requestCrop", REQUEST_CODE_CROP);
                //startActivityForResult(galleryIntent, REQUEST_CODE_CROP);
                activityResultLauncher.launch(galleryIntent);
                break;
            case REQUEST_CODE_CAMERA:
                Uri cameraUri = Objects.requireNonNull(intent.getData());
                // Retrieve the image orientation and rotate it unless it is 0 by applying matrix
                orientation = cropHelper.getImageOrientation(cameraUri);
                if(orientation != 0) cameraUri = cropHelper.rotateBitmapUri(cameraUri, orientation);

                Intent cameraIntent = new Intent(this, CropImageActivity.class);
                cameraIntent.setData(cameraUri);
                cameraIntent.putExtra("requestCrop", REQUEST_CODE_CROP);
                //startActivityForResult(cameraIntent, REQUEST_CODE_CROP);
                activityResultLauncher.launch(cameraIntent);

                break;
            case REQUEST_CODE_CROP:
                final Uri croppedImageUri = Objects.requireNonNull(intent.getData());
                // Upload the cropped user image to Firestore with the user id fetched
                mSettings.edit().putString(Constants.USER_IMAGE, null).apply();
                uploadUserImageToFirebase(croppedImageUri);

                break;
        }
    }

    private void addPreferenceFragment(PreferenceFragmentCompat fragment, FragmentManager fm) {
        fm.beginTransaction().replace(R.id.frame_setting, fragment, "settingGeneral")
                .addToBackStack(null)
                .commit();
        fm.registerFragmentLifecycleCallbacks(new FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentAttached(
                    @NonNull FragmentManager fm, @NonNull Fragment f, @NonNull Context context) {
                super.onFragmentAttached(fm, f, context);
                log.i("onFragmentAttched");
            }

            @Override
            public void onFragmentViewCreated(
                    @NonNull FragmentManager fm, @NonNull Fragment fragment, @NonNull View v,
                    @Nullable Bundle savedInstanceState) {
                super.onFragmentViewCreated(fm, fragment, v, savedInstanceState);
                log.i("onViewCreated");
                // To prevent SettingAutoFragment from invoking the method, which leads to cause
                // NullPointerException due to no autoRef reference.
                if(fragment instanceof SettingPreferenceFragment){
                    if(requestCode != -1) markupPreference((PreferenceFragmentCompat)fragment, requestCode);
                }

            }}, false);
    }

    // Display the indicator for which preferrence should be set as long as the activity is invoked
    // by ActivityResultLauncher.
    private void markupPreference(PreferenceFragmentCompat fragment, int caller) {
        switch(caller) {
            case Constants.REQUEST_BOARD_SETTING_USERNAME:
                Preference namePref = fragment.findPreference(Constants.USER_NAME);
                log.i("namePref: %s", namePref);
                Objects.requireNonNull(namePref).setIcon(R.drawable.setting_arrow_indicator);
                break;

            case Constants.REQUEST_BOARD_SETTING_AUTOCLUB:
                Preference autoPref = fragment.findPreference(Constants.AUTO_DATA);
                log.i("auto pref: %s", autoPref);
                Objects.requireNonNull(autoPref).setIcon(R.drawable.setting_arrow_indicator);
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
        ProgressBarDialogFragment progbarFragment = new ProgressBarDialogFragment();
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
                //startActivityForResult(cameraIntent, REQUEST_CODE_CAMERA);
                activityResultLauncher.launch(cameraIntent);

        } else if(ActivityCompat.shouldShowRequestPermissionRationale(this, permCamera)) {
            PermRationaleFragment permDialog = new PermRationaleFragment();
            permDialog.setContents("Hell", "World");
            permDialog.show(getSupportFragmentManager(), null);

        } else {
            ActivityCompat.requestPermissions(this, new String[]{permCamera}, REQUEST_CODE_CAMERA);
        }
    }
    /*
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

     */
}