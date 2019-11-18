package com.silverback.carman2.fragments;


import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.silverback.carman2.BoardPostingActivity;
import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import static android.content.Context.INPUT_METHOD_SERVICE;
import static androidx.core.content.ContextCompat.getSystemService;

/**
 * A simple {@link Fragment} subclass.
 */
public class BoardWriteFragment extends DialogFragment implements CheckBox.OnCheckedChangeListener {

    private static final LoggingHelper log = LoggingHelperFactory.create(BoardWriteFragment.class);

    private SharedPreferences mSettings;
    private HorizontalScrollView hScrollView;
    private ConstraintLayout statusLayout;
    private RecyclerView recyclerImageView;
    private EditText etPostTitle;

    public BoardWriteFragment() {
        // Required empty public constructor
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSettings = ((BoardPostingActivity)getActivity()).getSettings();

    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View localView = inflater.inflate(R.layout.dialog_board_write, container, false);

        hScrollView = localView.findViewById(R.id.scrollview_horizontal);
        statusLayout = localView.findViewById(R.id.vg_constraint_status);

        CheckBox chkboxGeneral = localView.findViewById(R.id.chkbox_general);
        CheckBox chkboxMaker = localView.findViewById(R.id.chkbox_maker);
        CheckBox chkboxType = localView.findViewById(R.id.chkbox_type);
        CheckBox chkboxModel = localView.findViewById(R.id.chkbox_model);
        CheckBox chkboxYear = localView.findViewById(R.id.chkbox_year);

        etPostTitle = localView.findViewById(R.id.et_board_title);

        recyclerImageView = localView.findViewById(R.id.vg_recycler_images);
        Button btnAttach = localView.findViewById(R.id.btn_attach_image);


        chkboxGeneral.setText("일반");
        chkboxMaker.setText(mSettings.getString("pref_auto_maker", null));
        chkboxType.setText(mSettings.getString("pref_auto_type", null));
        chkboxModel.setText(mSettings.getString("pref_auto_model", null));
        chkboxYear.setText(mSettings.getString("pref_auto_year", null));

        // Set the event listener to the checkboxes
        chkboxGeneral.setOnCheckedChangeListener(this);
        chkboxMaker.setOnCheckedChangeListener(this);
        chkboxType.setOnCheckedChangeListener(this);
        chkboxModel.setOnCheckedChangeListener(this);
        chkboxYear.setOnCheckedChangeListener(this);

        statusLayout.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            float statusHeight = statusLayout.getHeight();
            log.i("statuslayout height: %s", statusHeight);
        });


        TypedValue typedValue = new TypedValue();
        if(getActivity().getTheme().resolveAttribute(android.R.attr.actionBarSize, typedValue, true)) {
            float actionBarHeight = TypedValue.complexToDimensionPixelSize(
                    typedValue.data, getResources().getDisplayMetrics());

            animateStatusTitleViews(actionBarHeight);
        }

        ImageButton btnDismiss = localView.findViewById(R.id.btn_dismiss);
        ImageButton btnUpload = localView.findViewById(R.id.btn_upload);
        btnDismiss.setOnClickListener(btn -> dismiss());

        recyclerImageView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        recyclerImageView.setHasFixedSize(true);

        // Call the gallery or camera to capture images, the URIs of which are sent to an intent
        // of onActivityResult(int, int, Intent)
        btnAttach.setOnClickListener(view -> {
            ((InputMethodManager)(getActivity().getSystemService(INPUT_METHOD_SERVICE)))
                    .hideSoftInputFromWindow(etPostTitle.getWindowToken(), 0);

            log.i("Phone maker: %s, %s", Build.MANUFACTURER, Build.MODEL);

            // Pop up the dialog to select which media to use bewteen the camera and gallery, then
            // create an intent by the selection.
            DialogFragment dialog = new BoardChooserDlgFragment();
            dialog.show(getChildFragmentManager(), "@null");
        });


        return localView;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    private void animateStatusTitleViews(float height) {
        ObjectAnimator animStatusView = ObjectAnimator.ofFloat(hScrollView, "Y", height);
        animStatusView.setDuration(1000);
        animStatusView.start();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

    }
}
