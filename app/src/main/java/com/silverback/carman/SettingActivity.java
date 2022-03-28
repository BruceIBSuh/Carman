package com.silverback.carman;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
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
import com.silverback.carman.fragments.SettingAutoFragment;
import com.silverback.carman.fragments.SettingFavorGasFragment;
import com.silverback.carman.fragments.SettingFavorSvcFragment;
import com.silverback.carman.fragments.SettingPrefFragment;
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

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * This activity contains PreferenceFragmentCompat which is a special fragment to persist values
 * of preferences in SharedPreferences. Any value that has changed will be back to MainActivty
 * b/c this activity has been started by startActivityForResult() in MainActivity.
 */

public class SettingActivity extends BaseActivity implements
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback,
        SharedPreferences.OnSharedPreferenceChangeListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(SettingActivity.class);

    public static final String PREF_USERNAME_TAG = "carman_pref_nickname";
    public static final String PREF_DISTRICT_TAG ="carman_pref_district";
    public static final String PREF_AUTODATA_TAG = "carman_pref_autodata";
    public static final String PREF_FUEL_TAG = "carman_pref_ls_fuel";
    public static final String PREF_SEARCH_RADIOUS_TAG = "carman_pref_searching_radius";
    public static final String PREF_USERIMG_TAG = "carman_pref_userpic";

    //private static final int REQUEST_CODE_GALLERY = 10;
    //private static final int REQUEST_CODE_CAMERA = 11;
    private static final int REQUEST_CODE_CROP = 12;
    private static final int REQUEST_PERM_CAMERA = 1000;

    // Objects
    private Intent resultIntent; //back results to MainActivity
    private FirebaseFirestore mDB;
    private FirebaseStorage storage;
    private ApplyImageResourceUtil applyImageResourceUtil;
    private ImageViewModel imgModel;
    private SettingPrefFragment settingFragment;
    private GasPriceTask gasPriceTask;


    // UIs
    private ActivitySettingsBinding binding;
    private FrameLayout frameLayout;

    // Fields
    private String userId;
    private String userImage;
    private String permCamera;
    private Uri downloadUserImageUri;
    private int requestCode;

    // StratActivityForResult with ActivityResultcoracts and ActvityResultCallback.
    // Calling CropImageActivity right after getting the image uri.
    private final ActivityResultLauncher<Intent> cropImageResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), this::getActivityResultCallback);
    // Gettng Uri from Gallery
    private final ActivityResultLauncher<String> mGetContent = registerForActivityResult(
            new ActivityResultContracts.GetContent(), this::getAttachedImageUriCallback);
    // Getting Uri from Camera
    private final ActivityResultLauncher<Uri> mTakePicture = registerForActivityResult(
            new ActivityResultContracts.TakePicture(), this::getCameraImage);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // get Intent data, if any.
        if(getIntent() != null) requestCode = getIntent().getIntExtra("caller", -1);

        frameLayout = binding.frameSetting;
        setSupportActionBar(binding.settingToolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getString(R.string.setting_toolbar_title));

        // Instantiate objects
        resultIntent = new Intent();
        mDB = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        applyImageResourceUtil = new ApplyImageResourceUtil(this);

        // ViewModels
        FragmentSharedModel fragmentModel = new ViewModelProvider(this).get(FragmentSharedModel.class);
        imgModel = new ViewModelProvider(this).get(ImageViewModel.class);
        
        if(TextUtils.isEmpty(userId)) userId = getUserIdFromStorage(this);
        JSONArray jsonDistArray = getDistrictJSONArray();

        settingFragment = new SettingPrefFragment();
        Bundle bundle = new Bundle();
        bundle.putString("district", jsonDistArray.toString());
        settingFragment.setArguments(bundle);

        addPreferenceFragment(getSupportFragmentManager(), settingFragment);
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
            if(isPermitted) ActivityCompat.requestPermissions(
                    this, new String[]{permCamera}, REQUEST_PERM_CAMERA);
            else {
                final String msg = getString(R.string.perm_msg_camera);
                Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_SHORT).show();
            }
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
    public void getPermissionResult(Boolean isPermitted) {

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
        // Check which fragment the parent activity contains, then if the fragment is SettingPrefFragment,
        // send back to MainActivity an intent holding preference changes when clicking the Up button.
        // Otherwise, if the parent activity contains any fragment other than SettingPrefFragment,
        // just pop the fragment off the back stack, which works like the Back command.
        if (item.getItemId() == android.R.id.home) {
            Fragment childFragment = getSupportFragmentManager().findFragmentById(R.id.frame_setting);
            //childFragment = getSupportFragmentManager().findFragmentByTag("settingGeneral"); seems not working
            if(childFragment instanceof SettingPrefFragment) {
                setResult(requestCode, resultIntent);
                /*
                switch(requestCode) {
                    case Constants.REQUEST_MAIN_SETTING_GENERAL:
                        setResult(requestCode, resultIntent);
                        break;
                    case Constants.REQUEST_BOARD_SETTING_AUTOCLUB:
                        log.i("result back to BoardActivity: %s", jsonAutoData);
                        Intent autoIntent = new Intent();
                        autoIntent.putExtra("autodata", jsonAutoData);
                        setResult(requestCode, autoIntent);
                        break;
                    case Constants.REQUEST_BOARD_SETTING_USERNAME:
                        log.i("result back w/ username to BoardActivity: %s", userName);
                        Intent userIntent = new Intent();
                        userIntent.putExtra("username", userName);
                        setResult(requestCode, userIntent);
                        break;
                }
                 */
                finish();
                return true;

            } else {
                log.i("Back to SettingPrefFragment");
                getSupportFragmentManager().popBackStack();
                Objects.requireNonNull(getSupportActionBar()).setTitle(getString(R.string.setting_toolbar_title));
                return false;
            }

        // The return value should be false when it comes to the menu that adds a new service item,
        // which means this method will be handled in the SettingSvcItemFragment.
        } else return item.getItemId() != R.id.menu_add_service_item;
    }
    /*
     * Invoked when a preference which has an associated custom (dialog)fragment is tapped. If you do not
     * implement onPreferenceStartFragment(), a fallback implementation is used instead. While this works
     * in most cases, it is strongly recommend to implement this method, thereby you can fully configure
     * transitions b/w Fragment objects and update the title in the toolbar, if applicable.
     */
    //@SuppressWarnings("ConstantConditions")
    @Override
    public boolean onPreferenceStartFragment(@NonNull PreferenceFragmentCompat caller, Preference pref) {
        final Bundle args = pref.getExtras();
        final Fragment fragment = getSupportFragmentManager()
                .getFragmentFactory()
                .instantiate(getClassLoader(), Objects.requireNonNull(pref.getFragment()));
        fragment.setArguments(args);

        getSupportFragmentManager().setFragmentResultListener("autodata", this,
                (requestKey, result) -> {});

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
            case PREF_USERNAME_TAG:
                String userName = mSettings.getString(key, null);
                if(!TextUtils.isEmpty(userName)) resultIntent.putExtra("userName", userName);
                break;

            case PREF_AUTODATA_TAG:
                String jsonAutoData = mSettings.getString(Constants.AUTO_DATA, null);
                // Auto data should be saved both in SharedPreferences and Firestore for a statistical
                // use.
                if(jsonAutoData != null && !jsonAutoData.isEmpty()) {
                    resultIntent.putExtra("autodata", jsonAutoData);
                    final DocumentReference docref = mDB.collection("users").document(userId);
                    docref.update("auto_data", jsonAutoData).addOnFailureListener(Throwable::printStackTrace);
                }
                break;

            case PREF_FUEL_TAG:
                String gasCode = mSettings.getString(key, null);
                resultIntent.putExtra("gasCode", gasCode);
                break;

            case PREF_DISTRICT_TAG:
                try {
                    String jsonDist = mSettings.getString(key, null);
                    JSONArray jsonDistArray = new JSONArray(jsonDist);
                    String distCode = jsonDistArray.optString(2);
                    resultIntent.putExtra("distCode", distCode);
                } catch(JSONException e) {e.printStackTrace();}
                break;

            case PREF_SEARCH_RADIOUS_TAG:
                String radius = mSettings.getString(key, null);
                resultIntent.putExtra("searchRadius", radius);
                break;

            case PREF_USERIMG_TAG:
                userImage = mSettings.getString(key, null);
                break;
        }


    }

    // Referenced from the fragment to pass the media which has been decided in the dialog fragment.
    public void selectImageMedia(int media) {
        switch(media) {
            case 1: //gallery
                mGetContent.launch("image/*");
                break;

            case 2: //camera
                final String perm = Manifest.permission.CAMERA;
                final String rationale = "permission required to use Camera";
                checkRuntimePermission(binding.getRoot(), perm, rationale, () -> {
                    File tmpFile = new File(getCacheDir(), new SimpleDateFormat(
                            "yyyyMMdd_HHmmss", Locale.US ).format(new Date( )) + ".jpg" );
                    Uri photoUri = FileProvider.getUriForFile(this, Constants.FILE_IMAGES, tmpFile);
                    mTakePicture.launch(photoUri);
                });
                break;

            default:
                break;
        }

    }

    // ActivityResult callback to pass the uri of an attached image, then get the image orientation
    // using the util class.
    private void getAttachedImageUriCallback(Uri uri) {
        if(uri == null) return;

        ApplyImageResourceUtil cropHelper = new ApplyImageResourceUtil(this);
        int orientation = cropHelper.getImageOrientation(uri);
        if(orientation != 0) uri = cropHelper.rotateBitmapUri(uri, orientation);

        Intent intent = new Intent(this, CropImageActivity.class);
        intent.setData(uri);
        intent.putExtra("requestCrop", REQUEST_CODE_CROP);
        //startActivityForResult(galleryIntent, REQUEST_CODE_CROP);
        cropImageResultLauncher.launch(intent);
    }
    private void getCameraImage(boolean isTaken) {
        if(isTaken) mGetContent.launch("image/*");
    }

    // Callback method for ActivityResultLauncher
    private void getActivityResultCallback(ActivityResult result) {
        if(result.getData() == null) return;
        Uri croppedImageUri = Objects.requireNonNull(result.getData().getData());
        mSettings.edit().putString(Constants.USER_IMAGE, croppedImageUri.toString()).apply();
        uploadUserImageToFirebase(croppedImageUri);
    }

    // Add the setting fragmnet and regster the fragment lifecycle listener.
    private void addPreferenceFragment(FragmentManager fm, PreferenceFragmentCompat fragment) {
        fm.beginTransaction().replace(R.id.frame_setting, fragment, "settingGeneral")
                .addToBackStack(null)
                .commit();

        fm.registerFragmentLifecycleCallbacks(new FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentAttached(
                    @NonNull FragmentManager fm, @NonNull Fragment f, @NonNull Context context) {
                super.onFragmentAttached(fm, f, context);
            }

            @Override
            public void onFragmentViewCreated(
                    @NonNull FragmentManager fm, @NonNull Fragment fragment, @NonNull View v,
                    @Nullable Bundle savedInstanceState) {
                super.onFragmentViewCreated(fm, fragment, v, savedInstanceState);
                // To prevent SettingAutoFragment from invoking the method, which leads to cause
                // NullPointerException due to no autoRef reference.
                if(fragment instanceof SettingPrefFragment){
                    if(requestCode != -1)
                        markupPreference((PreferenceFragmentCompat)fragment, requestCode);
                }

            }}, false);
    }

    // Display the indicator for which preferrence should be set as long as the activity is invoked
    // by ActivityResultLauncher.
    private void markupPreference(PreferenceFragmentCompat fragment, int caller) {
        switch(caller) {
            case Constants.REQUEST_BOARD_SETTING_USERNAME:
                Preference namePref = fragment.findPreference(Constants.USER_NAME);
                Objects.requireNonNull(namePref).setIcon(R.drawable.setting_arrow_indicator);
                break;

            case Constants.REQUEST_BOARD_SETTING_AUTOCLUB:
                Preference autoPref = fragment.findPreference(Constants.AUTO_DATA);
                Objects.requireNonNull(autoPref).setIcon(R.drawable.setting_arrow_indicator);
                break;

        }
    }

    // Upload the cropped user image, the uri of which is saved in the internal storage, to Firebase
    // storage, then move on to get the download url. The url, in turn, is uploaded to "user" collection
    // of Firesotre. When the uploading process completes, put the uri in SharedPreferenes.
    // At the same time, the new uri has to be uploaded in the documents written by the user.
    //@SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    private void uploadUserImageToFirebase(Uri uri) {
        log.i("user id of Image upaloading: %s", userId);
        // Popup the progressbar displaying dialogfragment.
        //ProgressBarDialogFragment progbarFragment = new ProgressBarDialogFragment();
        String msg = (uri == null)?
                getString(R.string.setting_msg_remove_image):
                getString(R.string.setting_msg_upload_image);

        //progbarFragment.setProgressMsg(msg);
        //getSupportFragmentManager().beginTransaction().add(android.R.id.content, progbarFragment).commit();

        // Instantiate Firebase Storage.
        final StorageReference userImgRef = storage.getReference().child("user_pic/" + userId + ".png");
        // Delete the file from FireStorage and, if successful, it goes to FireStore to delete the
        // Url of the image.
        if(uri == null) {
            userImgRef.delete().addOnSuccessListener(aVoid -> {
                // Delete(update) the document with null value.
                DocumentReference docref = mDB.collection("users").document(userId);
                docref.update("user_pic", null).addOnSuccessListener(bVoid -> {
                    Snackbar.make(frameLayout, getString(R.string.pref_snackbar_image_deleted),
                            Snackbar.LENGTH_SHORT).show();

                    // Dismiss the progbar dialogfragment
                    //progbarFragment.dismiss();
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
            //if(!task.isSuccessful()) task.getException();
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
                mDB.collection("users").document(userId).set(uriUserImage, SetOptions.merge())
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
                                //progbarFragment.dismiss();
                            }
                        });

                // Update the user image in the posting items wrtiiten by the user.
                // Consider that all documents should be updated. Otherwise, limit condition would
                // be added.
                mDB.collection("user_post").whereEqualTo("user_id", userId).get()
                        .addOnCompleteListener(postTask -> {
                            if(postTask.isSuccessful() && postTask.getResult() != null) {
                                for(QueryDocumentSnapshot document : postTask.getResult()) {
                                    DocumentReference docRef = document.getReference();
                                    docRef.update("user_pic", downloadUserImageUri.toString());
                                }
                            }
                        });
            }
        });
    }

}