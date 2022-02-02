package com.silverback.carman.fragments;

import static com.silverback.carman.BoardActivity.AUTOCLUB;

import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.silverback.carman.R;
import com.silverback.carman.databinding.FragmentBoardWriteBinding;
import com.silverback.carman.databinding.FragmentBoardWriteTempBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;

public class BoardWriteDlgFragment extends DialogFragment {

    private static final LoggingHelper log = LoggingHelperFactory.create(BoardWriteDlgFragment.class);

    private FragmentBoardWriteTempBinding binding;

    private String userId;
    private String userName;
    private int page;

    public BoardWriteDlgFragment() {
        // empty constructor which might be referenced by FragmentFactory
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        if(getArguments() != null) {
            userId = getArguments().getString("userId");
            userName = getArguments().getString("userName");
            page = getArguments().getInt("page");
        }

        log.i("page: %s, ", page);
        //requireActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        binding = FragmentBoardWriteTempBinding.inflate(inflater);
        binding.toolbarBoardWrite.setTitle("POST WRITING");
        binding.toolbarBoardWrite.setNavigationOnClickListener(view -> dismiss());
        createPostWriteMenu();
        if(page == AUTOCLUB) animAutoFilter();
        else binding.scrollviewAutofilter.setVisibility(View.GONE);

        return binding.getRoot();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // If current page is AUTOCLUB, animate the autofilter down

    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        log.i("onDismiss");
    }

    private void createPostWriteMenu() {
        binding.toolbarBoardWrite.inflateMenu(R.menu.options_board_write);
        binding.toolbarBoardWrite.setOnMenuItemClickListener(item -> {
            return true;
        });
    }

    private void animAutoFilter() {
        binding.scrollviewAutofilter.setVisibility(View.VISIBLE);
        ObjectAnimator animTab = ObjectAnimator.ofFloat(
                binding.scrollviewAutofilter, "translationY", 150);
        animTab.setDuration(1000);
        animTab.start();
    }
}
