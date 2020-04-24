package com.silverback.carman2.fragments;


import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.FieldValue;
import com.silverback.carman2.BoardActivity;
import com.silverback.carman2.R;
import com.silverback.carman2.adapters.BoardImageAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.threads.ThreadManager;
import com.silverback.carman2.threads.UploadBitmapTask;
import com.silverback.carman2.threads.UploadPostTask;
import com.silverback.carman2.utils.ApplyImageResourceUtil;
import com.silverback.carman2.utils.BoardImageSpanHandler;
import com.silverback.carman2.utils.Constants;
import com.silverback.carman2.viewmodels.FragmentSharedModel;
import com.silverback.carman2.viewmodels.ImageViewModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.content.Context.INPUT_METHOD_SERVICE;

/**
 * A simple {@link Fragment} subclass.
 * This fragment is to upload any writing to post in the board with images attached.
 * Special care should be taken when thumbnail image and images in the recyclerview
 * Different processes are applied to add and remove the imagespan. Adding
 */
public class BoardWriteFragment extends DialogFragment implements
        BoardActivity.OnFilterCheckBoxListener,
        BoardImageSpanHandler.OnImageSpanListener,
        BoardImageAdapter.OnBoardAttachImageListener {

    private static final LoggingHelper log = LoggingHelperFactory.create(BoardWriteFragment.class);

    // Constants
    static final int GALLERY = 1;
    static final int CAMERA = 2;

    // Objects
    private BoardImageSpanHandler spanHandler;
    private ProgbarDialogFragment pbFragment;
    private ApplyImageResourceUtil applyImageResourceUtil;
    private FragmentSharedModel fragmentModel;
    private Observer<Integer> chooserObserver;
    private ImageViewModel imgViewModel;
    private BoardImageAdapter imageAdapter;
    private List<Uri> uriImgList;
    //private List<ImageSpan> spanList;
    //private ImageSpan imageSpan;
    private SparseArray<String> downloadImages;
    private UploadBitmapTask bitmapTask;
    private UploadPostTask postTask;

    // UIs
    private View localView;
    private EditText etPostTitle, etPostBody;

    // Fields
    private Uri imgUri;
    private ArrayList<CharSequence> autofilter;
    private boolean isGeneralPost;
    private String[] arrAutoData;
    private String userId;
    private int tabPage;
    private int chooser;


    // Constructor
    public BoardWriteFragment() {
        // Required empty public constructor
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the soft input mode, which seems not working.
        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        if(getArguments() != null) {
            userId = getArguments().getString("userId");
            tabPage = getArguments().getInt("tabPage");
        }

        // Set the listener only if the current page is set to the auto club.
        if(tabPage == Constants.BOARD_AUTOCLUB) ((BoardActivity)getActivity()).setAutoFilterListener(this);
        isGeneralPost = true;

        applyImageResourceUtil = new ApplyImageResourceUtil(getContext());
        uriImgList = new ArrayList<>();
        downloadImages = new SparseArray<>();

        fragmentModel = new ViewModelProvider(requireActivity()).get(FragmentSharedModel.class);
        imgViewModel = new ViewModelProvider(this).get(ImageViewModel.class);

        // Special attention should be paid when using ViewModel with requireActivity() as its
        // viewlifecyclerowner because ViewModel.observe() retains the existing value even after
        // the frament is removed from the parent activity.
        chooserObserver = integer -> {};
        fragmentModel.getImageChooser().observe(requireActivity(), chooserObserver);
    }

    @SuppressWarnings({"ConstantConditions", "ClickableViewAccessibility"})
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        localView = inflater.inflate(R.layout.fragment_board_write, container, false);

        etPostTitle = localView.findViewById(R.id.et_board_write_title);
        etPostBody = localView.findViewById(R.id.et_board_content);
        RecyclerView recyclerImageView = localView.findViewById(R.id.vg_recycler_images);
        Button btnAttach = localView.findViewById(R.id.btn_attach_image);

        // Create RecyclerView for holding attched pictures which are handled in onActivityResult()
        LinearLayoutManager linearLayout = new LinearLayoutManager(getContext());
        linearLayout.setOrientation(LinearLayoutManager.HORIZONTAL);
        recyclerImageView.setLayoutManager(linearLayout);
        //recyclerImageView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        //recyclerImageView.setHasFixedSize(true);//DO NOT SET THIS as far as notifyItemInserted may work.
        imageAdapter = new BoardImageAdapter(uriImgList, this);
        recyclerImageView.setAdapter(imageAdapter);

        // To scroll edittext inside (nested)scrollview. More research should be done.
        etPostBody.setOnTouchListener((view, event) ->{
            if(etPostBody.hasFocus()) {
                view.getParent().requestDisallowInterceptTouchEvent(true);
                switch(event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_SCROLL:
                        view.getParent().requestDisallowInterceptTouchEvent(false);
                        return true;
                }
            }
            return false;
        });

        // Call the gallery or camera to capture images, the URIs of which are sent to an intent
        // of onActivityResult(int, int, Intent)
        btnAttach.setOnClickListener(btn -> {
            ((InputMethodManager)(getActivity().getSystemService(INPUT_METHOD_SERVICE)))
                    .hideSoftInputFromWindow(localView.getWindowToken(), 0);

            // Pop up the dialog as far as the num of attached pics are no more than 6.
            if(uriImgList.size() > Constants.MAX_ATTACHED_IMAGE_NUMS) {
                log.i("Image count: %s", uriImgList.size());
                Snackbar.make(localView, getString(R.string.board_msg_image), Snackbar.LENGTH_SHORT).show();

            } else {
                log.i("BoardChooserDlgFragment initiated");
                // Pop up the dialog to select which media to use bewteen the camera and gallery, then
                // create an intent by the selection.
                DialogFragment dialog = new BoardChooserDlgFragment();
                // BUGS FREQUENTLY OCCURRED!!!!
                //java.lang.IllegalStateException: Fragment BoardWriteFragment{984f0c5}
                // (4b73b12d-9e70-4e32-95bb-52f209a6b8a1)} not attached to Activity
                //dialog.show(getChildFragmentManager(), "@null");
                //dialog.show(getParentFragmentManager(), "chooserDialog");
                dialog.show(getActivity().getSupportFragmentManager(), "chooser");

                /*
                 * This works for both, inserting a text at the current position and replacing
                 * any text which is selected by the user. The Math.max() is necessary in the first
                 * and second line because, if there is no selection or focus in the edittext,
                 * getSelectionStart() and getSelectionEnd() will both return -1. On the other hand,
                 * The Math.min() and Math.max() in the third line is necessary because the user
                 * could have selected the text backwards and thus start would have a higher value
                 * than the end value which is not allowed for Editable.replace().
                 */
                // Put linefeeder into the edittext when the image interleaves b/w the lines
                //int start = Math.max(etPostBody.getSelectionStart(), 0);
                //int end = Math.max(etPostBody.getSelectionEnd(), 0);
                int start = etPostBody.getSelectionStart();
                int end = etPostBody.getSelectionEnd();
                log.i("insert image: %s, %s, %s", etPostBody.getText(), start, end);
                etPostBody.getText().replace(start, end, "\n");
            }
        });


        // Create BoardImageSpanHandler implementing SpanWatcher, which is a helper class to manage
        // SpannableStringBuilder in order to protect imagespans from deletion while editing.
        spanHandler = new BoardImageSpanHandler(etPostBody, this);

        return localView;
    }

    // Handling  ViewModels
    // When fragments finish their lifecycle, the instance of a viewmodel exists in ViewModelStore,
    // until destruction of the parent activity. ViewModelStore has clear() method but it clears
    // all viewmodels in it.
    @SuppressWarnings("ConstantConditions")
    @Override
    public void onActivityCreated(Bundle bundle) {

        super.onActivityCreated(bundle);
        // Notified of which media(camera or gallery) to select in BoardChooserDlgFragment, according
        // to which startActivityForResult() is invoked by the parent activity and the result will be
        // notified to the activity and it is, in turn, sent back here by calling
        fragmentModel.getImageChooser().observe(getViewLifecycleOwner(), chooser -> {
            log.i("FragmentViwModel: %s", fragmentModel);
            ((BoardActivity)getActivity()).getImageFromChooser(chooser);
        });

        /*
        // The result of startActivityForResult() invoked in the parent activity should be notified
        // to the activity and it is, in turn, sent back here via a viewmodel livedata with the image
        // uri, with which the image span and the recyclerview are displayed with a new image.
        imgViewModel.getUriFromImageChooser().observe(getViewLifecycleOwner(), imgUri -> {
            // Glide creates a processed bitmap with the uri which the result intent from MediaStore
            // contains and as the process completes, the bitmap is sent to ImageViewModel for putting
            // it to the imagespan, which is defined in getGlideBitmapTarget() of onActivityCreated().
            int x = Constants.IMAGESPAN_THUMBNAIL_SIZE;
            int y = Constants.IMAGESPAN_THUMBNAIL_SIZE;
            applyImageResourceUtil.applyGlideToImageSpan(imgUri, x, y, imgViewModel);

            // Partial binding to show the image. RecyclerView.setHasFixedSize() is allowed to make
            // additional pics.
            uriImgList.add(imgUri);
            final int position = uriImgList.size() - 1;
            //imageAdapter.notifyItemInserted(position);
            imageAdapter.notifyItemChanged(position);
        });
         */

        // The imgUri received as a result of startActivityForResult() is applied to applyGlideToImageSpan().
        // This util method translates an image to an appropriate extent for fitting the imagespan and
        // the result is provided
        imgViewModel.getGlideBitmapTarget().observe(requireActivity(), bitmap -> {
            log.i("Bitmap received: %s, %s", uriImgList.size(), bitmap);
            ImageSpan imgSpan = new ImageSpan(getContext(), bitmap);
            // Manage the image spans using BoardImageSpanHandler helper class.
            //this.imageSpan = imgSpan;
            //spanHandler.setImageSpanToPost(imgSpan);
            spanHandler.setImageSpan(imgSpan);
        });


        /*
         * UPLOAD PROCESS
         * The process of uploading the post consists of three steps.
         * First, upload attached images to Firebase Storage, if any.
         * Second, check whether the attached images safely completes uploading.
         * Third, start to upload the post to FireStore, then on notifying completion, dismiss.
         */
        // As UploadBitmapTask has completed to optimize an attched image and upload it to Stroage,
        // the result is notified as SparseArray which indicates the position and uriString of image.
        imgViewModel.getDownloadBitmapUri().observe(requireActivity(), sparseArray -> {
            // Check if the number of attached images equals to the number of uris that are down
            // loaded from Storage.
            downloadImages.put(sparseArray.keyAt(0), sparseArray.valueAt(0).toString());
            if(uriImgList.size() == downloadImages.size()) {
                // On completing optimization of attached images, start uploading a post.
                uploadPostToFirestore();
            }

        });

        // On completion of uploading a post to Firestore, dismiss ProgbarDialogFragment and add the
        // viewpager which contains BoardPagerFragment to the frame of the parent activity. Not only
        // this, this viewmodel notifies BoardPagerFragment of the completion for making a query.
        fragmentModel.getNewPosting().observe(requireActivity(), docId -> {
            log.i("New Posting in BoardWRiteFragment: %s, %s", docId, TextUtils.isEmpty(docId));
            if(pbFragment != null) pbFragment.dismiss();
            ((BoardActivity)getActivity()).addViewPager();
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        //if(spanHandler != null) spanHandler = null;
        if(bitmapTask != null) bitmapTask = null;
        if(postTask != null) postTask = null;
    }

    // Special care should be paid to handling ViewModel and Fragment life cycle.
    @Override
    public void onDestroyView() {
        log.i("onDestroy");
        fragmentModel.getImageChooser().setValue(-1);
        fragmentModel.getImageChooser().removeObserver(chooserObserver);
        imageAdapter = null;
        super.onDestroyView();
    }


    // Implement BoardActivity.OnFilterCheckedListener which notifies any change of the checkbox
    // values
    @Override
    public void onCheckBoxValueChange(ArrayList<CharSequence> autofilter) {
        for(CharSequence filter : autofilter) log.i("filter: %s", filter);
        this.autofilter = autofilter;
    }
    @Override
    public void onGeneralPost(boolean b) {
        log.i("isGeneralPost: %s", b);
        isGeneralPost = b;
    }

    // Implement BoardImageSpanHandler.OnImageSpanListener which notifies that an new ImageSpan
    // has been added or removed. The position param is fetched from the markup using the regular
    // expression.
    @Override
    public void notifyAddImageSpan(ImageSpan imgSpan, int position) {
        uriImgList.add(position, imgUri);
        imageAdapter.notifyDataSetChanged();
    }

    @Override
    public void notifyRemovedImageSpan(int position) {
        uriImgList.remove(position);
        imageAdapter.notifyDataSetChanged();
    }

    // Implement BoardImageAdapter.OnBoardAttachImageListener when an image is removed from the
    // recyclerview.
    @Override
    public void removeImage(int position) {
        spanHandler.removeImageSpan(position);
    }

    /*
    @Override
    public void attachImage(Bitmap bmp, int pos) {
        log.i("Bitmap received: %s, %s", bmp, pos);
    }

     */


    // Invokde by OnActivityResult() in the parent activity that passes an intent data(URI) as to
    // an image picked in the media which has been selected by BoardChooserDlgFragment
    public void setUriFromImageChooser(Uri uri) {
        //log.d("setUriFromImageChooser");
        int x = Constants.IMAGESPAN_THUMBNAIL_SIZE;
        int y = Constants.IMAGESPAN_THUMBNAIL_SIZE;
        applyImageResourceUtil.applyGlideToImageSpan(uri, x, y, imgViewModel);
        // Partial binding to show the image. RecyclerView.setHasFixedSize() is allowed to make
        // additional pics.
        imgUri = uri;
    }

    // Invoked when the upload menu in the toolbar is pressed.
    @SuppressWarnings("ConstantConditions")
    public void initUploadPost() {

        ((InputMethodManager)getActivity().getSystemService(INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(localView.getWindowToken(), 0);

        if(!doEmptyCheck()) return;

        // Instantiate the fragment to display the progressbar.
        pbFragment = new ProgbarDialogFragment();
        pbFragment.setProgressMsg(getString(R.string.board_msg_uploading));

        getActivity().getSupportFragmentManager().beginTransaction()
                .add(android.R.id.content, pbFragment).commit();
        //getChildFragmentManager().beginTransaction().add(android.R.id.content, pbFragment).commit();
        // No image posting makes an immediate uploading but postings with images attached
        // should take the uploading process that images starts uploading first and image
        // URIs would be sent back if uploading images is successful,then uploading gets
        // started with image URIs, adding it to a document of Firestore.
        if(uriImgList.size() == 0) uploadPostToFirestore();
        else {
            // Image Attachment button that starts UploadBitmapTask as many as the number of
            // images. In case the task starts with UploadBitmapTask multi-threading which
            // runs in ThreadManager, thread contentions may occur, replacing one with the
            // other which makes last image cover the other ones.
            // A download url from Storage each time when an attached image is successfully
            // downsized and scaled down, then uploaded to Storage is transferred via
            // ImageViewModel.getDownloadBitmapUri() as a live data of SparseArray.
            for(int i = 0; i < uriImgList.size(); i++) {
                bitmapTask = ThreadManager.startBitmapUploadTask(getContext(),
                        uriImgList.get(i), i, imgViewModel);
            }
        }
    }


    @SuppressWarnings("ConstantConditions")
    private void uploadPostToFirestore() {
        //if(!doEmptyCheck()) return;
        // UserId should be passed from the parent activity. If not, the process should end here.
        if(TextUtils.isEmpty(userId)) return;

        // Cast SparseArray containing download urls from Storage to String array
        // Something wrong around here because a posting item contains an image despite no image
        // attached.
        List<String> downloadUriList = null;
        if(downloadImages.size() > 0) {
            downloadUriList = new ArrayList<>(downloadImages.size());
            for (int i = 0; i < downloadImages.size(); i++) {
                downloadUriList.add(downloadImages.keyAt(i), downloadImages.valueAt(i));
            }
        }

        Map<String, Object> post = new HashMap<>();
        post.put("user_id", userId);
        post.put("post_title", etPostTitle.getText().toString());
        post.put("timestamp", FieldValue.serverTimestamp());
        post.put("cnt_comment", 0);
        post.put("cnt_compathy", 0);
        post.put("cnt_view", 0);
        post.put("post_content", etPostBody.getText().toString());
        if(downloadImages.size() > 0) post.put("post_images",  downloadUriList);
        log.i("tab page: %s", tabPage);

        // To show or hide a post of the autoclub depends on the value of isGeneralPost. The default
        // value is set to true such that any post, no matter what is autoclub or general post,
        // it would be shown in every board. However, as long as an autoclub post set isGeneral value
        // to false, it would not be shown in the general board; only in the autoclub.
        if(tabPage == Constants.BOARD_AUTOCLUB) {
            //isGeneralPost = ((BoardActivity)getActivity()).checkPostGenenral();// initially true.
            autofilter = ((BoardActivity)getActivity()).getAutoFilterValues();
            post.put("auto_club", autofilter);
        }

        log.i("isGeneralPost: %s", isGeneralPost);
        post.put("post_general", isGeneralPost);

        // When uploading completes, the result is sent to BoardPagerFragment and the  notifes
        // BoardPagerFragment of a new posting. At the same time, the fragment dismisses.
        postTask = ThreadManager.startUploadPostTask(getContext(), post, fragmentModel);
    }


    private boolean doEmptyCheck() {
        log.i("Title: %s", etPostTitle.getText());
        if(TextUtils.isEmpty(etPostTitle.getText())) {
            Snackbar.make(localView, getString(R.string.board_msg_no_title), Snackbar.LENGTH_SHORT).show();
            return false;
        } else if(TextUtils.isEmpty(etPostBody.getText())){
            Snackbar.make(localView, getString(R.string.board_msg_no_content), Snackbar.LENGTH_SHORT).show();
            return false;
        } else return true;

    }




}
